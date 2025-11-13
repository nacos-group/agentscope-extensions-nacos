import asyncio
import dataclasses
import json
import logging
from abc import abstractmethod
from pathlib import Path
from typing import Any, Union, Optional, Type
from urllib.parse import urlparse
from uuid import uuid4

import httpx
from a2a.client import A2ACardResolver, A2AClient, ClientEvent
from a2a.client.client import ClientConfig as A2AClientConfig
from a2a.client.client_factory import ClientFactory as A2AClientFactory
from a2a.types import (
	AgentCard, 
	A2ARequest, 
	Message as A2AMessage,
	Part,
	Role as A2ARole,
	Task as A2ATask,
	TaskState,
	TextPart,
)
from a2a.types import TransportProtocol as A2ATransport
from a2a.utils import AGENT_CARD_WELL_KNOWN_PATH
from agentscope.agent import AgentBase
from agentscope.message import Msg
from pydantic import BaseModel
from v2.nacos import ClientConfig
from v2.nacos.ai.model.ai_param import GetAgentCardParam, \
	SubscribeAgentCardParam

from agentscope_nacos.nacos_service_manager import NacosServiceManager


# Initialize logger
logger = logging.getLogger(__name__)


class A2ACardResolverBase:
	"""Base class for A2A Agent Card resolvers.
	
	This abstract class defines the interface for resolving agent cards
	from various sources (URL, file, Nacos, etc.).
	"""

	@abstractmethod
	async def get_agent_card(self):
		"""Get Agent Card from the configured source.
		
		Returns:
			AgentCard: The resolved agent card object
		"""


class DefaultA2ACardResolver(A2ACardResolverBase):
	"""Default implementation of A2A Agent Card resolver.
	
	Resolves agent cards from either HTTP/HTTPS URLs or local file paths.
	Supports lazy loading and caching of the resolved agent card.
	
	Args:
		agent_card_source: URL or file path to the agent card
		httpx_client: Async HTTP client for URL-based resolution
		agent_card_path: Path to append to base URL (default: well-known path)
	"""

	def __init__(
			self,
			agent_card_source: str,
			httpx_client: httpx.AsyncClient,
			agent_card_path: str = AGENT_CARD_WELL_KNOWN_PATH,
	) -> None:
		self._agent_card: Optional[AgentCard] = None
		self._agent_card_source = agent_card_source
		self._httpx_client = httpx_client
		self._agent_card_path = agent_card_path
		
		logger.debug(f"[{self.__class__.__name__}] Initialized with source: {agent_card_source}")


	async def get_agent_card(
        self,
        relative_card_path: str | None = None,
        http_kwargs: dict[str, Any] | None = None,
    ) -> AgentCard:
		"""Get agent card with lazy loading and caching.
		
		Returns:
			AgentCard: Cached or newly resolved agent card
		"""
		if self._agent_card is None:
			logger.info(f"[{self.__class__.__name__}] Resolving agent card from: {self._agent_card_source}")
			self._agent_card = await self._resolve_agent_card()
			logger.info(f"[{self.__class__.__name__}] Agent card resolved successfully: {self._agent_card.name}")
		return self._agent_card

	async def _resolve_agent_card(self) -> AgentCard:
		"""Resolve agent card from configured source.
		
		Automatically determines whether to resolve from URL or file
		based on the source format.
		
		Returns:
			AgentCard: Resolved agent card object
		"""
		# Determine if source is URL or file path
		if self._agent_card_source.startswith(("http://", "https://")):
			logger.debug(f"[{self.__class__.__name__}] Resolving agent card from URL")
			return await self._resolve_agent_card_from_url(
				self._agent_card_source)
		else:
			logger.debug(f"[{self.__class__.__name__}] Resolving agent card from file")
			return await self._resolve_agent_card_from_file(
				self._agent_card_source)

	async def _resolve_agent_card_from_url(self, url: str) -> AgentCard:
		"""Resolve agent card from HTTP/HTTPS URL.
		
		Args:
			url: Full URL to the agent card
			
		Returns:
			AgentCard: Resolved agent card object
			
		Raises:
			ValueError: If URL format is invalid
			RuntimeError: If resolution fails
		"""
		try:
			parsed_url = urlparse(url)
			if not parsed_url.scheme or not parsed_url.netloc:
				logger.error(f"[{self.__class__.__name__}] Invalid URL format: {url}")
				raise ValueError(f"Invalid URL format: {url}")

			base_url = f"{parsed_url.scheme}://{parsed_url.netloc}"
			relative_card_path = parsed_url.path

			if not self._httpx_client:
				self._http_client = httpx.AsyncClient(
						timeout=httpx.Timeout(timeout=600)
				)
			resolver = A2ACardResolver(
					httpx_client=self._httpx_client,
					base_url=base_url,
					agent_card_path=self._agent_card_path
			)
			return await resolver.get_agent_card(
					relative_card_path=relative_card_path
			)
		except Exception as e:
			logger.error(f"[{self.__class__.__name__}] Failed to resolve agent card from URL {url}: {e}")
			raise RuntimeError(
					f"Failed to resolve AgentCard from URL {url}: {e}"
			) from e

	async def _resolve_agent_card_from_file(self, file_path: str) -> AgentCard:
		"""Resolve agent card from local file path.
		
		Args:
			file_path: Path to the agent card JSON file
			
		Returns:
			AgentCard: Parsed agent card object
			
		Raises:
			FileNotFoundError: If file doesn't exist
			ValueError: If path is not a file
			RuntimeError: If JSON parsing fails
		"""
		try:
			path = Path(file_path)
			if not path.exists():
				logger.error(f"[{self.__class__.__name__}] Agent card file not found: {file_path}")
				raise FileNotFoundError(
					f"Agent card file not found: {file_path}")
			if not path.is_file():
				logger.error(f"[{self.__class__.__name__}] Path is not a file: {file_path}")
				raise ValueError(f"Path is not a file: {file_path}")

			with path.open("r", encoding="utf-8") as f:
				agent_json_data = json.load(f)
				return AgentCard(**agent_json_data)
		except json.JSONDecodeError as e:
			logger.error(f"[{self.__class__.__name__}] Invalid JSON in agent card file {file_path}: {e}")
			raise RuntimeError(
					f"Invalid JSON in agent card file {file_path}: {e}"
			) from e
		except Exception as e:
			logger.error(f"[{self.__class__.__name__}] Failed to resolve agent card from file {file_path}: {e}")
			raise RuntimeError(
					f"Failed to resolve AgentCard from file {file_path}: {e}"
			) from e


class NacosA2ACardResolver(A2ACardResolverBase):
	"""Nacos-based A2A Agent Card resolver.
	
	Resolves and subscribes to agent cards stored in Nacos service registry.
	Supports automatic updates when agent cards change in Nacos.
	Uses NacosServiceManager for connection pooling.
	
	Args:
		remote_agent_name: Name of the remote agent in Nacos
		nacos_client_config: Optional Nacos client config (uses global if None)
		version: Optional version constraint for the agent card
		
	Raises:
		ValueError: If remote_agent_name is empty
	"""
	
	def __init__(
			self,
			remote_agent_name: str,
			nacos_client_config: Optional[ClientConfig] = None,
			version: Optional[str] = None,
	) -> None:
		if not remote_agent_name:
			raise ValueError("remote_agent_name is required")

		self._nacos_client_config: Optional[ClientConfig] = nacos_client_config
		self._remote_agent_name: str = remote_agent_name
		self._version: Optional[str] = version

		# Lazy initialization state
		self._initialized = False
		self._initializing = False
		self._init_lock = asyncio.Lock()

		self._agent_card: AgentCard | None = None
		
		logger.debug(f"[{self.__class__.__name__}] Initialized for agent: {remote_agent_name}")


	async def get_agent_card(
        self,
        relative_card_path: str | None = None,
        http_kwargs: dict[str, Any] | None = None,
    ) -> AgentCard:
		"""Get agent card from Nacos with lazy initialization.
		
		Returns:
			AgentCard: Resolved agent card from Nacos
		"""
		await self._ensure_initialized()
		return self._agent_card

	async def initialize(self):
		"""Public method to trigger explicit initialization."""
		await self._ensure_initialized()


	async def _ensure_initialized(self):
		"""Ensure the resolver is initialized (thread-safe lazy initialization).
		
		Uses double-checked locking pattern to avoid multiple initializations.
		"""
		if self._initialized:
			return
			
		# Wait if initialization is in progress
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
			logger.info(f"[{self.__class__.__name__}] Initializing for agent: {self._remote_agent_name}")
			await self._async_init()
			self._initialized = True
			logger.info(f"[{self.__class__.__name__}] Initialization completed for agent: {self._remote_agent_name}")
		except Exception as e:
			logger.error(f"[{self.__class__.__name__}] Failed to initialize: {e}")
			raise
		finally:
			self._initializing = False

	async def _async_init(self):
		"""Internal async initialization logic.
		
		Resolves agent card from Nacos and subscribes to updates.
		Uses NacosServiceManager for connection pooling.
		"""
		# Get Nacos AI service with connection pooling
		manager = NacosServiceManager()
		self._nacos_ai_service = await manager.get_ai_service(
			self._nacos_client_config
		)
		
		# Fetch agent card from Nacos
		self._agent_card = await self._nacos_ai_service.get_agent_card(GetAgentCardParam(
				agent_name=self._remote_agent_name,
				version=self._version,
		))
		logger.info(f"[{self.__class__.__name__}] Agent card fetched from Nacos: {self._agent_card.name}")

		# Subscribe to agent card updates
		async def agent_card_subscriber(agent_name: str, agent_card: AgentCard):
			"""Callback for agent card updates from Nacos."""
			logger.info(f"[{self.__class__.__name__}] Agent card updated for {agent_name}: {agent_card.name}")
			self._agent_card = agent_card

		await self._nacos_ai_service.subscribe_agent_card(SubscribeAgentCardParam(
				agent_name=self._remote_agent_name,
				version=self._version,
				subscribe_callback=agent_card_subscriber
		))
		logger.debug(f"[{self.__class__.__name__}] Subscribed to agent card updates for: {self._remote_agent_name}")





class A2aAgent(AgentBase):
	"""AgentScope agent that communicates with remote A2A protocol agents.
	
	This agent acts as a bridge between AgentScope and A2A protocol-compliant
	agents. It handles message conversion, session management, and error handling.
	
	Features:
		- Lazy initialization for better performance
		- Session state management (context_id, task_id)
		- Automatic message format conversion (AgentScope <-> A2A)
		- Support for both streaming and non-streaming responses
		- Error handling and recovery
	
	Args:
		agent_card: AgentCard object, URL, or file path to the card
		httpx_client: Optional HTTP client for network requests
		a2a_client_factory: Optional custom A2A client factory
		agent_card_resolver: Optional custom agent card resolver
		agent_name: Optional custom name for the agent
		
	Raises:
		ValueError: If neither agent_card nor agent_card_resolver is provided
		TypeError: If agent_card has invalid type
	
	Example:
		>>> # From URL
		>>> agent = A2aAgent(agent_card="https://example.com/agent-card.json")
		>>> response = await agent.reply(msg)
		
		>>> # From Nacos
		>>> resolver = NacosA2ACardResolver(remote_agent_name="test-agent")
		>>> agent = A2aAgent(agent_card=None, agent_card_resolver=resolver)
		>>> response = await agent.reply(msg)
	"""

	def __init__(self, agent_card: Union[AgentCard, str] | None,
			httpx_client: Optional[httpx.AsyncClient] = None,
			a2a_client_factory: Optional[A2AClientFactory] = None,
			agent_card_resolver: A2ACardResolver | None = None,
			agent_name: Optional[str] = None):
		super().__init__()

		# Core components
		self._agent_card: Optional[AgentCard] = None
		self._agent_card_source: Optional[str] = None
		self._use_resolver = True
		self._a2a_client: Optional[A2AClient] = None
		self._httpx_client = httpx_client
		if a2a_client_factory and a2a_client_factory._config.httpx_client:
			self._httpx_client = a2a_client_factory._config.httpx_client
		self._a2a_client_factory: Optional[A2AClientFactory] = a2a_client_factory
		self._a2a_card_resolver = agent_card_resolver

		# Lazy initialization state
		self._initialized = False
		self._initializing = False
		self._init_lock = asyncio.Lock()

		# Session state management
		self._current_task_id: Optional[str] = None
		self._current_context_id: Optional[str] = None
		self._conversation_history: list[A2AMessage] = []
		
		# Agent name (priority: user-specified > agent card > default)
		self._user_specified_name = agent_name
		self.name = agent_name or "A2AAgent"  # Temporary default, updated after init

		# Validate input parameters
		if agent_card is None and agent_card_resolver is None:
			raise ValueError("agent_card or agent_card_resolver is required")

		# Process agent_card parameter
		if agent_card is not None:
			if isinstance(agent_card, AgentCard):
				self._agent_card = agent_card
				# Set name immediately if agent card is provided
				if not self._user_specified_name and agent_card.name:
					self.name = agent_card.name
				logger.debug(f"[{self.__class__.__name__}] Initialized with AgentCard: {agent_card.name}")
			elif isinstance(agent_card, str):
				if not agent_card.strip():
					raise ValueError("agent_card string cannot be empty")
				self._agent_card_source = agent_card.strip()
				logger.debug(f"[{self.__class__.__name__}] Initialized with card source: {agent_card}")
			else:
				raise TypeError(
						"agent_card must be AgentCard, URL string, or file path string, "
						f"got {type(agent_card)}"
				)
		else:
			logger.debug(f"[{self.__class__.__name__}] Initialized with card resolver")

	async def _ensure_initialized(self):
		"""Ensure agent is initialized (thread-safe lazy initialization).
		
		Uses double-checked locking pattern to avoid multiple initializations.
		Waits if initialization is already in progress.
		"""
		if self._initialized:
			return
			
		# Wait if initialization is in progress
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
			logger.info(f"[{self.__class__.__name__}] Initializing...")
			await self._async_init()
			self._initialized = True
			logger.info(f"[{self.__class__.__name__}] Initialization completed: {self.name}")
		except Exception as e:
			logger.error(f"[{self.__class__.__name__}] Failed to initialize: {e}")
			raise
		finally:
			self._initializing = False


	async def _ensure_http_client(self):
		"""Ensure HTTP client is initialized and configured.
		
		Creates a new HTTP client if not provided, and updates the A2A client
		factory configuration accordingly.
		"""
		if not self._httpx_client:
			self._httpx_client = httpx.AsyncClient(
					timeout=httpx.Timeout(timeout=600)
			)
			if self._a2a_client_factory:
				registry = self._a2a_client_factory._registry
				self._a2a_client_factory = A2AClientFactory(
						config=dataclasses.replace(
								self._a2a_client_factory._config,
								httpx_client=self._httpx_client,
						),
						consumers=self._a2a_client_factory._consumers,
				)
				for label, generator in registry.items():
					self._a2a_client_factory.register(label, generator)
		if not self._a2a_client_factory:
			client_config = A2AClientConfig(
					httpx_client=self._httpx_client,
					streaming=False,
					polling=False,
					supported_transports=[A2ATransport.jsonrpc],
			)
			self._a2a_client_factory = A2AClientFactory(config=client_config)
		return self._httpx_client

	async def _async_init(self):
		"""Internal async initialization logic.
		
		Performs the following steps:
		1. Ensure HTTP client is ready
		2. Create card resolver if needed
		3. Resolve agent card
		4. Validate agent card
		5. Set agent name
		"""
		# Ensure HTTP client is initialized
		await self._ensure_http_client()
		
		# Create default card resolver if using resolver and not provided
		if self._use_resolver and not self._a2a_card_resolver:
			logger.debug(f"[{self.__class__.__name__}] Creating default card resolver")
			self._a2a_card_resolver = DefaultA2ACardResolver(
					agent_card_source=self._agent_card_source,
					httpx_client=self._httpx_client,
			)

		# Resolve agent card from configured source
		if self._use_resolver:
			self._agent_card = await self._a2a_card_resolver.get_agent_card()

		# Validate the resolved agent card
		await self._validate_agent_card(self._agent_card)
		
		# Set agent name (priority: user-specified > agent card name > default)
		if not self._user_specified_name:
			if self._agent_card and self._agent_card.name:
				self.name = self._agent_card.name
				logger.debug(f"[{self.__class__.__name__}] Agent name set from card: {self.name}")
			else:
				self.name = "A2AAgent"

	async def _validate_agent_card(self, agent_card: AgentCard) -> None:
		"""Validate resolved agent card.
		
		Args:
			agent_card: The agent card to validate
			
		Raises:
			RuntimeError: If agent card is invalid
		"""
		if not agent_card.url:
			logger.error(f"[{self.__class__.__name__}] Agent card missing URL")
			raise RuntimeError(
					"Agent card must have a valid URL for RPC communication"
			)

		# Validate URL format
		try:
			parsed_url = urlparse(str(agent_card.url))
			if not parsed_url.scheme or not parsed_url.netloc:
				logger.error(f"[{self.__class__.__name__}] Invalid RPC URL format: {agent_card.url}")
				raise ValueError("Invalid RPC URL format")
		except Exception as e:
			logger.error(f"[{self.__class__.__name__}] Invalid RPC URL in agent card: {agent_card.url}, error: {e}")
			raise RuntimeError(
					f"Invalid RPC URL in agent card: {agent_card.url}, error: {e}"
			) from e


	async def _get_agent_card(self):
		if self._use_resolver:
			return await self._a2a_card_resolver.get_agent_card()
		else:
			return self._agent_card


	async def reply(self,
        msg: Msg | list[Msg] | None = None,
        structured_model: Type[BaseModel] | None = None,) -> Msg:
		"""Process message and return response from remote A2A agent.
		
		This method handles the complete message exchange lifecycle:
		1. Initialize agent if needed
		2. Convert AgentScope message to A2A format
		3. Attach session context (task_id, context_id)
		4. Send message to remote agent
		5. Process response (handle both Message and Task responses)
		6. Convert A2A response back to AgentScope format
		
		Args:
			msg: Input message (single Msg or list of Msg)
			structured_model: Optional structured output model (not yet supported)
			
		Returns:
			Msg: Agent's response message in AgentScope format
			
		Raises:
			RuntimeError: If remote service returns error or task fails
			ValueError: If message format is invalid
		"""
		# 1. Ensure agent is initialized
		await self._ensure_initialized()
		
		# 2. Preprocess message
		if msg is None:
			raise ValueError("msg cannot be None")
		
		if isinstance(msg, list):
			# If list, take the last message
			if len(msg) == 0:
				raise ValueError("msg list cannot be empty")
			msg = msg[-1]
		
		logger.debug(f"[{self.__class__.__name__}] Processing message from {msg.get('name', 'user')}")
		
		# 3. Convert to A2A Message format
		a2a_message = self._convert_msg_to_a2a_message(msg)
		
		# 4. Attach session context for multi-turn conversation
		if self._current_task_id:
			a2a_message.task_id = self._current_task_id
			logger.debug(f"[{self.__class__.__name__}] Attached task_id: {self._current_task_id}")
		if self._current_context_id:
			a2a_message.context_id = self._current_context_id
			logger.debug(f"[{self.__class__.__name__}] Attached context_id: {self._current_context_id}")
		
		# 5. Create A2A client and send message
		client = self._a2a_client_factory.create(
			card=await self._get_agent_card()
		)
		
		logger.info(f"[{self.__class__.__name__}] Sending message to remote agent: {self.name}")
		
		# 6. Process response stream
		response_msg = None
		
		try:
			async for item in client.send_message(a2a_message):
				if isinstance(item, A2AMessage):
					# Case 1: Direct Message response (non-task mode)
					# Extract context_id and task_id for session management
					if item.context_id:
						self._current_context_id = item.context_id
						logger.debug(f"[{self.__class__.__name__}] Updated context_id: {self._current_context_id}")
					if item.task_id:
						self._current_task_id = item.task_id
						logger.debug(f"[{self.__class__.__name__}] Updated task_id: {self._current_task_id}")
					
					response_msg = self._convert_a2a_message_to_msg(item)
					logger.info(f"[{self.__class__.__name__}] Received direct message response")
					break
					
				elif isinstance(item, tuple):
					# Case 2: (Task, UpdateEvent) tuple response
					task, update = item
					
					# Update session state
					self._current_task_id = task.id
					self._current_context_id = task.context_id
					logger.debug(f"[{self.__class__.__name__}] Task update: {task.status.state.value}, task_id: {task.id}")
					
					# Check task status
					if task.status.state == TaskState.completed:
						# Extract response from completed task
						response_msg = self._convert_task_to_msg(task)
						logger.info(f"[{self.__class__.__name__}] Task completed successfully: {task.id}")
						break
						
					elif task.status.state in [
						TaskState.failed, 
						TaskState.canceled, 
						TaskState.rejected
					]:
						error_msg = ""
						if task.status.message:
							error_msg = f": {self._extract_text_from_message(task.status.message)}"
						logger.error(f"[{self.__class__.__name__}] Task {task.id} {task.status.state.value}{error_msg}")
						raise RuntimeError(
							f"Task {task.id} {task.status.state.value}{error_msg}"
						)
					
					# Other states (working, submitted, input_required, etc.) - continue waiting
		
		except Exception as e:
			# Log error and re-raise
			logger.error(f"[{self.__class__.__name__}] Failed to get response from remote agent: {e}")
			raise RuntimeError(f"Failed to get response from remote agent: {e}") from e
		
		# 7. Return response
		if response_msg is None:
			logger.error(f"[{self.__class__.__name__}] No response received from remote agent")
			raise RuntimeError("No response received from remote agent")

		await self.print(response_msg, True)
		return response_msg
	
	def _convert_msg_to_a2a_message(self, msg: Msg) -> A2AMessage:
		"""Convert AgentScope Msg to A2A Message format.
		
		Extracts text content from AgentScope message and creates an A2A
		protocol-compliant message with appropriate role mapping.
		
		Args:
			msg: AgentScope message object
			
		Returns:
			A2AMessage: A2A protocol message object
		"""
		# 提取文本内容
		if isinstance(msg.content, str):
			text_content = msg.content
		else:
			# content 是 list[ContentBlock]
			text_blocks = msg.get_content_blocks("text")
			if text_blocks:
				text_content = " ".join([block.get("text", "") for block in text_blocks])
			else:
				text_content = ""
		
		# 角色映射：AgentScope -> A2A
		# "user" | "system" -> Role.user
		# "assistant" -> Role.agent
		if msg.role in ["user", "system"]:
			a2a_role = A2ARole.user
		elif msg.role == "assistant":
			a2a_role = A2ARole.agent
		else:
			# 默认使用 user
			a2a_role = A2ARole.user
		
		# 构建 A2A Message
		a2a_message = A2AMessage(
			message_id=str(uuid4()),
			role=a2a_role,
			parts=[Part(root=TextPart(text=text_content))],
			metadata=msg.metadata if msg.metadata else None,
		)
		
		return a2a_message
	
	def _convert_a2a_message_to_msg(self, a2a_msg: A2AMessage) -> Msg:
		"""
		将 A2A Message 转换为 AgentScope Msg
		
		Args:
			a2a_msg: A2A 协议的消息对象
			
		Returns:
			Msg: AgentScope 消息对象
		"""
		# 提取文本内容
		text_content = self._extract_text_from_parts(a2a_msg.parts)
		
		# 角色映射：A2A -> AgentScope
		# Role.user -> "user"
		# Role.agent -> "assistant"
		if a2a_msg.role == A2ARole.user:
			role = "user"
		elif a2a_msg.role == A2ARole.agent:
			role = "assistant"
		else:
			role = "assistant"  # 默认
		
		# 构建 AgentScope Msg
		# 保留 A2A 的元数据
		metadata = a2a_msg.metadata.copy() if a2a_msg.metadata else {}
		metadata["a2a_message_id"] = a2a_msg.message_id
		if a2a_msg.task_id:
			metadata["a2a_task_id"] = a2a_msg.task_id
		if a2a_msg.context_id:
			metadata["a2a_context_id"] = a2a_msg.context_id
		
		msg = Msg(
			name=self.name,
			content=text_content,
			role=role,
			metadata=metadata,
		)
		
		return msg
	
	def _convert_task_to_msg(self, task: A2ATask) -> Msg:
		"""
		从 A2A Task 中提取最终响应并转换为 Msg
		
		Args:
			task: A2A 协议的任务对象
			
		Returns:
			Msg: AgentScope 消息对象
		"""
		# 1. 从 Task 的历史消息中提取最后一条 agent 消息
		text_content = ""
		
		if task.history:
			# 从后往前找最后一条 agent 消息
			for msg in reversed(task.history):
				if msg.role == A2ARole.agent:
					text_content = self._extract_text_from_parts(msg.parts)
					break
		
		# 2. 如果历史中没有找到，尝试从 status.message 中提取
		if not text_content and task.status.message:
			text_content = self._extract_text_from_message(task.status.message)
		
		# 3. 提取 artifacts 中的文本内容
		if task.artifacts:
			artifacts_text = self._extract_text_from_artifacts(task.artifacts)
			if artifacts_text:
				if text_content:
					# 如果已有内容，追加 artifacts 文本
					text_content += "\n\n" + artifacts_text
				else:
					# 如果没有内容，使用 artifacts 文本
					text_content = artifacts_text
		
		# 4. 如果还是没有内容，使用默认消息
		if not text_content:
			text_content = f"Task {task.id} completed"
		
		# 5. 构建 AgentScope Msg 元数据
		metadata = {
			"a2a_task_id": task.id,
			"a2a_context_id": task.context_id,
			"a2a_task_state": task.status.state.value,
		}
		
		# 6. 处理 artifacts 的详细信息
		if task.artifacts:
			# 保存基本信息
			metadata["a2a_artifacts_count"] = len(task.artifacts)
			metadata["a2a_artifacts"] = [
				{
					"id": art.artifact_id,
					"name": art.name,
					"description": art.description,
				}
				for art in task.artifacts
			]
			
			# 提取并保存文件信息
			files = self._extract_files_from_artifacts(task.artifacts)
			if files:
				metadata["a2a_artifact_files"] = files
			
			# 提取并保存结构化数据
			data = self._extract_data_from_artifacts(task.artifacts)
			if data:
				metadata["a2a_artifact_data"] = data
		
		msg = Msg(
			name=self.name,
			content=text_content,
			role="assistant",
			metadata=metadata,
		)
		
		return msg
	
	def _extract_text_from_parts(self, parts: list[Part]) -> str:
		"""
		从 A2A Parts 中提取文本内容
		
		Args:
			parts: A2A Part 列表
			
		Returns:
			str: 提取的文本内容
		"""
		text_segments = []
		
		for part in parts:
			# Part 是一个 RootModel[TextPart | FilePart | DataPart]
			# 需要访问 part.root 来获取实际内容
			if hasattr(part, 'root'):
				actual_part = part.root
			else:
				actual_part = part
			
			# 检查是否是 TextPart
			if isinstance(actual_part, TextPart):
				text_segments.append(actual_part.text)
			elif hasattr(actual_part, 'kind') and actual_part.kind == 'text':
				text_segments.append(actual_part.text)
		
		return " ".join(text_segments) if text_segments else ""
	
	def _extract_text_from_message(self, message: A2AMessage) -> str:
		"""
		从 A2A Message 中提取文本内容
		
		Args:
			message: A2A 消息对象
			
		Returns:
			str: 提取的文本内容
		"""
		if message.parts:
			return self._extract_text_from_parts(message.parts)
		return ""
	
	def _extract_text_from_artifacts(self, artifacts: list) -> str:
		"""
		从 artifacts 中提取文本内容并格式化展示
		
		Args:
			artifacts: Artifact 对象列表
			
		Returns:
			str: 格式化后的文本内容
		"""
		sections = []
		
		for artifact in artifacts:
			# 提取文本内容
			text = self._extract_text_from_parts(artifact.parts)
			if text:
				# 格式化：添加标题和描述
				section = ""
				if artifact.name or artifact.description:
					section += "=" * 50 + "\n"
					if artifact.name:
						section += f"工件: {artifact.name}\n"
					if artifact.description:
						section += f"说明: {artifact.description}\n"
					section += "=" * 50 + "\n"
				section += text
				sections.append(section)
		
		return "\n\n".join(sections) if sections else ""
	
	def _extract_files_from_artifacts(self, artifacts: list) -> list[dict]:
		"""
		从 artifacts 中提取文件信息
		
		Args:
			artifacts: Artifact 对象列表
			
		Returns:
			list[dict]: 文件信息列表
		"""
		files = []
		
		for artifact in artifacts:
			for part in artifact.parts:
				actual_part = part.root if hasattr(part, 'root') else part
				
				# 检查是否是 FilePart
				if hasattr(actual_part, 'kind') and actual_part.kind == 'file':
					file_info = {
						"artifact_id": artifact.artifact_id,
						"artifact_name": artifact.name,
						"artifact_description": artifact.description,
						"file": actual_part.model_dump() if hasattr(actual_part, 'model_dump') else {}
					}
					files.append(file_info)
		
		return files
	
	def _extract_data_from_artifacts(self, artifacts: list) -> list[dict]:
		"""
		从 artifacts 中提取结构化数据
		
		Args:
			artifacts: Artifact 对象列表
			
		Returns:
			list[dict]: 结构化数据列表
		"""
		data_list = []
		
		for artifact in artifacts:
			for part in artifact.parts:
				actual_part = part.root if hasattr(part, 'root') else part
				
				# 检查是否是 DataPart
				if hasattr(actual_part, 'kind') and actual_part.kind == 'data':
					data_info = {
						"artifact_id": artifact.artifact_id,
						"artifact_name": artifact.name,
						"artifact_description": artifact.description,
						"data": actual_part.data if hasattr(actual_part, 'data') else {}
					}
					data_list.append(data_info)
		
		return data_list
