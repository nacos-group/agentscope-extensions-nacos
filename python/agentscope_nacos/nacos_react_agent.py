import asyncio
import json
import logging
from typing import Literal, Optional

from v2.nacos import ClientConfig, NacosConfigService, ConfigParam
from v2.nacos.ai.nacos_ai_service import NacosAIService
from agentscope.agent import ReActAgent
from agentscope.memory import MemoryBase, LongTermMemoryBase
from agentscope.plan import PlanNotebook
from agentscope.rag import KnowledgeBase

from agentscope_nacos.mcp.agentscope_nacos_mcp import NacosHttpStatelessClient
from agentscope_nacos.model.nacos_chat_model import NacosChatModel, AutoFormatter
from agentscope_nacos.mcp.agentscope_dynamic_toolkit import DynamicToolkit
from agentscope_nacos.nacos_service_manager import NacosServiceManager
from agentscope.tool import Toolkit

# Initialize logger
logger = logging.getLogger(__name__)


class NacosAgentListener:
	"""Nacos Agent Listener - Supports lazy initialization and background pre-initialization.
	
	Responsibilities:
	1. Load Agent configuration from Nacos (Prompt, MCP Server, etc.)
	2. Listen for configuration changes and auto-update
	3. Manage Agent lifecycle
	
	Features:
	- Lazy initialization: Auto-initialize on first public method call
	- Background pre-initialization: Try to initialize early in background if event loop exists
	- Thread-safe: Use asyncio.Lock to ensure initialization runs only once
	
	Args:
		agent_name: Name of the agent
		nacos_client_config: Nacos client configuration. If not provided, uses global config
		listen_prompt: Whether to listen for prompt configuration changes
		listen_mcp_server: Whether to listen for MCP server configuration changes
		listen_chat_model: Whether to listen for chat model configuration changes
	
	Example:
		```python
		listener = NacosAgentListener(agent_name="my_agent")
		await listener.initialize()
		agent = NacosReActAgent(nacos_agent_listener=listener, name="my_agent")
		```
	"""
	
	def __init__(
		self,
		agent_name: str,
		nacos_client_config: Optional[ClientConfig] = None,
		listen_prompt: bool = True,
		listen_mcp_server: bool = True,
		listen_chat_model: bool = True,
	):
		# Configuration parameters
		self._nacos_client_config = nacos_client_config
		self.agent_name = agent_name
		self._listen_prompt = listen_prompt
		self._listen_mcp_server = listen_mcp_server
		self._listen_mcp_server = listen_mcp_server
		self._listen_chat_model = listen_chat_model
		
		# Nacos services (lazy initialization)
		self.nacos_config_service: NacosConfigService | None = None
		self.nacos_ai_service: NacosAIService | None = None
		
		# Agent components (lazy initialization)
		self.toolkit: DynamicToolkit | None = None
		self.chat_model: NacosChatModel | None = None
		self.formatter: AutoFormatter | None = None
		self.agent: ReActAgent | None = None
		
		# MCP server management
		self.mcp_servers: list[dict] = []
		self.mcp_server_clients: dict[str, NacosHttpStatelessClient] = {}
		
		# Prompt management
		self.user_prompt_ref: str = ""
		self.template: str = ""
		self.prompt: str = ""
		
		# Lazy initialization state
		self._initialized = False
		self._initializing = False
		self._init_lock = asyncio.Lock()
		self._init_task = None  # For storing pre-initialization task

		self._original_model = None
		self._original_formatter = None
		self._original_prompt = None
		self._original_toolkit = None
		
		logger.debug(f"[{self.__class__.__name__}] Initialized for agent: {agent_name}")
		
		# Try to start background pre-initialization
		self._try_start_background_init()
	
	def _try_start_background_init(self):
		"""Try to start background initialization (safe).
		
		Attempts to initialize in background if an event loop is running.
		Falls back to lazy initialization if no loop is available.
		"""
		try:
			# Check if there's a running event loop
			loop = asyncio.get_running_loop()
			# If yes, start pre-initialization
			self._init_task = loop.create_task(self._ensure_initialized())
			logger.debug(f"[{self.__class__.__name__}] Started background pre-initialization for agent: {self.agent_name}")
		except RuntimeError:
			# No running event loop, skip pre-initialization
			# This is normal, will use lazy initialization
			logger.debug(f"[{self.__class__.__name__}] No event loop available, will use lazy initialization for agent: {self.agent_name}")
			pass
	
	async def initialize(self):
		"""Public initialization method (maintains backward compatibility).
		
		Callers can explicitly call this method for initialization,
		or skip it (will auto-initialize lazily when needed).
		"""
		await self._ensure_initialized()


	def _set_prompt(self,prompt:str):
		if prompt is None or prompt == "" and self._original_prompt is not None:
			prompt = self._original_prompt
		self.prompt = prompt
		if self.agent is not None:
			self.agent._sys_prompt = prompt

	def is_initialized(self):
		return self._initialized

	async def _ensure_initialized(self):
		"""Ensure listener is initialized (thread-safe lazy initialization).
		
		Uses double-checked locking pattern to avoid race conditions.
		"""
		if self._initialized:
			return

		# If initializing, wait for completion
		if self._initializing:
			while self._initializing:
				await asyncio.sleep(0.01)
			return

		async with self._init_lock:
			# Double-check to avoid duplicate initialization
			if self._initialized:
				return

			self._initializing = True
			try:
				logger.info(f"[{self.__class__.__name__}] Starting initialization for agent: {self.agent_name}")
				await self._async_init()
				self._initialized = True
				logger.info(f"[{self.__class__.__name__}] Successfully initialized for agent: {self.agent_name}")
			except Exception as e:
				logger.error(f"[{self.__class__.__name__}] Initialization failed for agent {self.agent_name}: {e}", exc_info=True)
				raise
			finally:
				self._initializing = False

	async def _async_init(self):
		"""Internal async initialization logic.
		
		Loads agent configuration from Nacos and sets up listeners for:
		- Chat model configuration
		- Prompt configuration
		- MCP server configuration
		"""
		# Use NacosServiceManager to get services (automatically reuses connections)
		manager = NacosServiceManager()
		self.nacos_config_service = await manager.get_config_service(
			self._nacos_client_config)
		self.nacos_ai_service = await manager.get_ai_service(
			self._nacos_client_config)
		logger.debug(f"[{self.__class__.__name__}] Obtained Nacos services for agent: {self.agent_name}")

		if self._listen_chat_model:
			self.chat_model = NacosChatModel(
				nacos_client_config=self._nacos_client_config,
				agent_name=self.agent_name,
				stream=True,
			)
			await self.chat_model.initialize()

		
			self.formatter = AutoFormatter(if_multi_agent=False,
									  chat_model=self.chat_model)
			logger.debug(f"[{self.__class__.__name__}] Formatter and Chat model initialized")
		if self._listen_mcp_server:
			self.toolkit = DynamicToolkit()
			logger.debug(f"[{self.__class__.__name__}] Toolkit created")

			await self._init_listen_mcp_server()

		if self._listen_prompt:
			await self._init_listen_prompt()

		logger.info(f"[{self.__class__.__name__}] Listeners configured for agent: {self.agent_name}")


	async def _init_listen_prompt(self):
		"""Initialize prompt configuration and set up listeners.
		
		Loads prompt configuration from Nacos and registers listeners
		for both prompt content and prompt reference changes.
		"""

		async def user_prompt_listener(tenant, data_id, group, content):
			"""Listener for prompt content changes"""
			logger.info(
				f"[{self.__class__.__name__}] Prompt content changed - data_id: {data_id}")
			_user_prompt_dict = json.loads(content)
			self.template = _user_prompt_dict["template"]
			self._set_prompt(self.template)
			logger.debug(f"[{self.__class__.__name__}] Prompt updated")

		async def user_prompt_config_listener(tenant, data_id, group, content):
			"""Listener for prompt reference changes"""
			logger.info(
				f"[{self.__class__.__name__}] Prompt config changed - data_id: {data_id}")
			_user_prompt_config_dict = json.loads(content)
			if "promptRef" not in _user_prompt_config_dict or _user_prompt_config_dict["promptRef"] != self.user_prompt_ref:
				_old_prompt_ref = self.user_prompt_ref
				if "promptRef" in _user_prompt_config_dict:
					self.user_prompt_ref = _user_prompt_config_dict["promptRef"]
					logger.info(
						f"[{self.__class__.__name__}] Prompt ref changed from {_old_prompt_ref} to {self.user_prompt_ref}")

					_user_prompt = await self.nacos_config_service.get_config(
							ConfigParam(
									data_id=self.user_prompt_ref,
									group="nacos-ai-prompt"
							))
					_user_prompt_dict = json.loads(_user_prompt)
					self.template = _user_prompt_dict["template"]

					# Update listener to new prompt ref
					if len(_old_prompt_ref) != 0:
						await self.nacos_config_service.remove_listener(
								data_id=_old_prompt_ref,
								group="nacos-ai-prompt",
								listener=user_prompt_listener)
					await self.nacos_config_service.add_listener(
							data_id=self.user_prompt_ref,
							group="nacos-ai-prompt",
							listener=user_prompt_listener)
					logger.debug(
						f"[{self.__class__.__name__}] Updated prompt listener")
				if "promptRef" not in _user_prompt_config_dict:
					if len(_old_prompt_ref) != 0:
						await self.nacos_config_service.remove_listener(
								data_id=_old_prompt_ref,
								group="nacos-ai-prompt",
								listener=user_prompt_listener)
					self.user_prompt_ref = ""
					logger.info(
						f"[{self.__class__.__name__}] Prompt ref not found in config")
					if "prompt" in _user_prompt_config_dict:
						self.template = _user_prompt_config_dict["prompt"]
			self._set_prompt(self.template)


		user_config_group_name = f"ai-agent-{self.agent_name}"
		user_prompt_config_data_id = "prompt.json"
		user_prompt_config = await self.nacos_config_service.get_config(
				ConfigParam(
						data_id=user_prompt_config_data_id,
						group=user_config_group_name,
				)
		)
		if user_prompt_config is None or len(user_prompt_config) == 0:
			logger.info(f"[{self.__class__.__name__}] Prompt config not found for agent: {self.agent_name}")
			await self.nacos_config_service.add_listener(
					data_id=user_prompt_config_data_id,
					group=user_config_group_name,
					listener=user_prompt_config_listener
			)
			logger.debug(
				f"[{self.__class__.__name__}] Registered prompt config listener")
			return

		user_prompt_config_dict = json.loads(user_prompt_config)
		if "promptRef" in user_prompt_config_dict:
			logger.debug(f"[{self.__class__.__name__}] Prompt ref found in config")

			self.user_prompt_ref = user_prompt_config_dict["promptRef"]
			logger.debug(f"[{self.__class__.__name__}] Loaded prompt ref: {self.user_prompt_ref}")

			user_prompt = await self.nacos_config_service.get_config(ConfigParam(
					data_id=self.user_prompt_ref,
					group="nacos-ai-prompt"
			))

			if user_prompt is None or len(user_prompt) == 0:
				logger.error(f"[{self.__class__.__name__}] Prompt ref not found: {self.user_prompt_ref}")
				raise ValueError(f"Prompt ref not found: {self.user_prompt_ref}")
			user_prompt_dict = json.loads(user_prompt)
			self.template = user_prompt_dict["template"]
			self._set_prompt(self.template)
			logger.info(f"[{self.__class__.__name__}] Prompt loaded and set")

			# Register prompt listeners
			await self.nacos_config_service.add_listener(
					data_id=self.user_prompt_ref,
					group="nacos-ai-prompt",
					listener=user_prompt_listener
			)
			logger.debug(
				f"[{self.__class__.__name__}] Registered prompt content listener")

			await self.nacos_config_service.add_listener(
					data_id=user_prompt_config_data_id,
					group=user_config_group_name,
					listener=user_prompt_config_listener
			)
			logger.debug(
				f"[{self.__class__.__name__}] Registered prompt config listener")

		elif "prompt" in user_prompt_config_dict:
			self.user_prompt_ref = ""
			self.template = user_prompt_config_dict["prompt"]
			self._set_prompt(self.template)
			logger.info(f"[{self.__class__.__name__}] Prompt loaded and set")
			await self.nacos_config_service.add_listener(
					data_id=user_prompt_config_data_id,
					group=user_config_group_name,
					listener=user_prompt_config_listener
			)
			logger.debug(
				f"[{self.__class__.__name__}] Registered prompt config listener")
			return
		else:
			logger.error(f"[{self.__class__.__name__}] Invalid prompt config: {user_prompt_config}")
			raise ValueError(f"Invalid prompt config: {user_prompt_config}")


	def attach_agent(self, agent: ReActAgent):
		"""Attach Agent to Listener to receive Nacos configuration updates.
		
		Note: Listener must be initialized before calling this method.
		
		Args:
			agent: ReActAgent instance to attach
			
		Raises:
			RuntimeError: If listener is not initialized
		"""
		if not self.is_initialized():
			raise RuntimeError("NacosAgentListener not initialized. Call await listener.initialize() first.")
		
		self.agent = agent
		logger.info(f"[{self.__class__.__name__}] Attaching agent: {agent.name}")

		self._original_prompt = self.agent._sys_prompt
		self._original_toolkit = self.agent.toolkit
		self._original_model = self.agent.model
		self._original_formatter = self.agent.formatter
		
		if self._listen_prompt:
			self.agent._sys_prompt = self.prompt
			logger.debug(f"[{self.__class__.__name__}] Prompt configured for agent")
			
		if self._listen_mcp_server:
			if self.agent.toolkit is not None:
				self.toolkit.tools.update(self.agent.toolkit.tools)
				self.toolkit.groups.update(self.agent.toolkit.groups)
				self.agent.toolkit = self.toolkit
			logger.debug(f"[{self.__class__.__name__}] Toolkit configured for agent")
			
		if self._listen_chat_model:
			self.chat_model.set_backup_model(self._original_model)
			self.agent.model = self.chat_model
			self.agent.formatter = self.formatter
			logger.debug(f"[{self.__class__.__name__}] Chat model configured for agent")
		
		logger.info(f"[{self.__class__.__name__}] Agent '{agent.name}' successfully attached")

	def detach_agent(self):
		"""Detach Agent from Listener to stop receiving Nacos configuration updates.

		Note: Listener must be initialized before calling this method.

		Raises:
			RuntimeError: If listener is not initialized
		"""
		agent = self.agent
		self.agent = None
		if agent is not None:
			agent.model = self._original_model
			agent.formatter = self._original_formatter
			agent.toolkit = self._original_toolkit
			agent._sys_prompt = self._original_prompt

		self._original_model = None
		self._original_prompt = None
		self._original_toolkit = None
		self._original_formatter = None


	async def _init_listen_mcp_server(self):
		"""Initialize MCP server configuration and register clients.
		
		Loads MCP server list from Nacos and creates client instances.
		"""
		user_config_group_name = f"ai-agent-{self.agent_name}"
		user_mcp_server_config_data_id = "mcp-server.json"
		user_mcp_server_config = await self.nacos_config_service.get_config(
				ConfigParam(
						data_id=user_mcp_server_config_data_id,
						group=user_config_group_name,
				)
		)

		user_mcp_server_config_dict = json.loads(user_mcp_server_config)
		self.mcp_servers = user_mcp_server_config_dict["mcpServers"]
		logger.debug(f"[{self.__class__.__name__}] Loaded {len(self.mcp_servers)} MCP server(s) from config")

		for mcp_server_dict in self.mcp_servers:
			mcp_server_name = mcp_server_dict["mcpServerName"]
			logger.debug(f"[{self.__class__.__name__}] Initializing MCP client: {mcp_server_name}")
			
			mcp_stateless_client = NacosHttpStatelessClient(
					nacos_client_config=self._nacos_client_config,
					name=mcp_server_name
			)

			await mcp_stateless_client.initialize()
			await self.toolkit.register_mcp_client(mcp_stateless_client)
			self.mcp_server_clients[mcp_server_name] = mcp_stateless_client
			logger.info(f"[{self.__class__.__name__}] MCP client '{mcp_server_name}' registered")

	def get_model_and_formatter(self):
		"""Get the chat model and formatter.
		
		Returns:
			tuple: (NacosChatModel, AutoFormatter)
			
		Raises:
			RuntimeError: If listener is not initialized
		"""
		if not self.is_initialized():
			raise RuntimeError("NacosAgentListener not initialized. Call await listener.initialize() first.")
		return self.chat_model, self.formatter

	def get_toolkit(self):
		"""Get the dynamic toolkit.
		
		Returns:
			DynamicToolkit: The toolkit instance
			
		Raises:
			RuntimeError: If listener is not initialized
		"""
		if not self.is_initialized():
			raise RuntimeError("NacosAgentListener not initialized. Call await listener.initialize() first.")
		return self.toolkit

	def get_prompt(self):
		"""Get the current prompt.
		
		Returns:
			str: The prompt string
			
		Raises:
			RuntimeError: If listener is not initialized
		"""
		if not self.is_initialized():
			raise RuntimeError("NacosAgentListener not initialized. Call await listener.initialize() first.")
		return self.prompt


class NacosReActAgent(ReActAgent):
	"""ReActAgent with Nacos configuration integration.
	
	This agent automatically uses configuration from a NacosAgentListener,
	including chat model, prompt, and MCP tools from Nacos.
	
	Args:
		nacos_agent_listener: Initialized NacosAgentListener instance
		name: Agent name
		toolkit: Optional additional toolkit
		memory: Memory instance
		long_term_memory: Long-term memory instance
		long_term_memory_mode: Memory mode
		enable_meta_tool: Whether to enable meta tools
		parallel_tool_calls: Whether to enable parallel tool calls
		knowledge: Knowledge base(s)
		enable_rewrite_query: Whether to enable query rewriting
		plan_notebook: Plan notebook instance
		print_hint_msg: Whether to print hint messages
		max_iters: Maximum number of iterations
	
	Raises:
		RuntimeError: If NacosAgentListener is not initialized
	
	Example:
		```python
		listener = NacosAgentListener(agent_name="my_agent")
		await listener.initialize()
		agent = NacosReActAgent(nacos_agent_listener=listener, name="my_agent")
		response = await agent(message)
		```
	"""

	def __init__(
			self,
			nacos_agent_listener: NacosAgentListener,
			name: str,
			toolkit: Toolkit | None = None,
			memory: MemoryBase | None = None,
			long_term_memory: LongTermMemoryBase | None = None,
			long_term_memory_mode: Literal[
				"agent_control",
				"static_control",
				"both",
			] = "both",
			enable_meta_tool: bool = False,
			parallel_tool_calls: bool = False,
			knowledge: KnowledgeBase | list[KnowledgeBase] | None = None,
			enable_rewrite_query: bool = True,
			plan_notebook: PlanNotebook | None = None,
			print_hint_msg: bool = False,
			max_iters: int = 10,
	) -> None:

		self.nacos_agent_listener = nacos_agent_listener
		if not self.nacos_agent_listener.is_initialized():
			raise RuntimeError("Nacos agent listener is not initialized")

		logger.debug(f"[{self.__class__.__name__}] Initializing NacosReActAgent: {name}")

		super().__init__(name=name,
						 sys_prompt="",
						 model=self.nacos_agent_listener.chat_model,
						 formatter=self.nacos_agent_listener.formatter,
						 toolkit=toolkit,
						 memory=memory,
						 long_term_memory=long_term_memory,
						 long_term_memory_mode=long_term_memory_mode,
						 enable_meta_tool=enable_meta_tool,
						 parallel_tool_calls=parallel_tool_calls,
						 knowledge=knowledge,
						 enable_rewrite_query=enable_rewrite_query,
						 plan_notebook=plan_notebook,
						 print_hint_msg=print_hint_msg,
						 max_iters=max_iters,
						 )
						 
		self.nacos_agent_listener.attach_agent(self)
		logger.info(f"[{self.__class__.__name__}] NacosReActAgent '{name}' created and attached to listener")

