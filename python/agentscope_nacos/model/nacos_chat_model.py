import asyncio
import json
import logging
from typing import Any, AsyncGenerator, Optional

from agentscope.formatter import FormatterBase, DashScopeChatFormatter, \
	GeminiChatFormatter, OllamaChatFormatter, AnthropicChatFormatter, \
	OpenAIChatFormatter, DashScopeMultiAgentFormatter, \
	GeminiMultiAgentFormatter, OllamaMultiAgentFormatter, \
	AnthropicMultiAgentFormatter, OpenAIMultiAgentFormatter
from agentscope.model import ChatModelBase, OpenAIChatModel, AnthropicChatModel, \
	OllamaChatModel, GeminiChatModel, DashScopeChatModel, ChatResponse
from v2.nacos import ClientConfig, NacosConfigService, ConfigParam
from watchfiles import awatch

from agentscope_nacos.utils import AsyncRWLock, validate_agent_name
from agentscope_nacos.nacos_service_manager import NacosServiceManager

# Initialize logger
logger = logging.getLogger(__name__)


class NacosChatModel(ChatModelBase):
	"""Nacos-integrated chat model with dynamic configuration support.
	
	Supports automatic model configuration updates via Nacos configuration service.
	Implements lazy initialization pattern for better resource management.
	
	Features:
		- Dynamic model configuration from Nacos
		- Automatic configuration change detection
		- Thread-safe lazy initialization
		- Multiple model provider support (OpenAI, Anthropic, Ollama, Gemini, DashScope)
		- Backup model fallback mechanism
	
	Args:
		agent_name: Name of the agent
		nacos_client_config: Nacos client configuration. If not provided, uses global config
		stream: Whether to enable streaming mode
		client_args: Additional client arguments
		backup_model: Backup model to use when primary model fails
	
	Example:
		```python
		model = NacosChatModel(
			agent_name="my_agent",
			stream=True,
			backup_model=OpenAIChatModel(...)
		)
		response = await model(messages)
		```
	"""

	def __init__(
		self,
		agent_name: str,
		nacos_client_config: Optional[ClientConfig] = None,
		stream: bool = True,
		client_args: dict | None = None,
		backup_model: ChatModelBase | None = None,
	):
		# Lazy initialization state flags
		self._initialized = False
		self._initializing = False
		self._init_lock = asyncio.Lock()
		
		self.client_args = client_args or {}
		self.agent_name = validate_agent_name(agent_name)
		super().__init__(agent_name, stream=stream)
		self._nacos_client_config: Optional[ClientConfig] = nacos_client_config
		self.nacos_config_service: NacosConfigService | None = None
		self.chat_model: ChatModelBase | None = None
		self.model_lock = AsyncRWLock()

		self.api_key: str | None = None
		self.args: dict = {}
		self.model_name = ""
		self.model_provider = "openai"
		self.base_url = ""
		self._backup_model: ChatModelBase | None = backup_model
		
		logger.debug(f"[{self.__class__.__name__}] Initialized for agent: {agent_name}")

	async def _ensure_initialized(self):
		"""Ensure ChatModel is initialized (thread-safe lazy initialization).
		
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
		
		Loads model configuration from Nacos and sets up configuration listeners.
		"""
		# Use NacosServiceManager to get service (automatically reuses connections)
		manager = NacosServiceManager()
		self.nacos_config_service = await manager.get_config_service(
			self._nacos_client_config)
		logger.debug(f"[{self.__class__.__name__}] Obtained Nacos config service for agent: {self.agent_name}")

		user_model_config_group_name = f"ai-agent-{self.agent_name}"
		user_model_config_data_id = "model.json"
		user_model_config = await self.nacos_config_service.get_config(
				ConfigParam(
						data_id=user_model_config_data_id,
						group=user_model_config_group_name,
				))
		if user_model_config is None:
			logger.error(f"[{self.__class__.__name__}] No model config found for agent {self.agent_name}")
			raise Exception(
					f"No model config found for agent {self.agent_name}")

		model_config = json.loads(user_model_config)
		self.model_name = model_config["modelName"]
		self.api_key = model_config.get("apiKey", "")
		self.model_provider = model_config.get("modelProvider", "openai")
		self.base_url = model_config.get("baseUrl", "")
		self.args = model_config.get("args", {})

		logger.info(f"[{self.__class__.__name__}] Loaded model spec - name: {self.model_name}, provider: {self.model_provider}, base_url :{self.base_url}")
		
		try:
			await self.set_chat_model(self.generate_chat_model())
			logger.info(f"[{self.__class__.__name__}] Chat model created successfully")
		except Exception as e:
			logger.error(f"[{self.__class__.__name__}] Failed to create chat model: {e}")
			if self._backup_model is not None:
				logger.info(f"[{self.__class__.__name__}] Falling back to backup model")
				await self.set_chat_model(self._backup_model)
			else:
				raise Exception(
						f"Failed to create chat model for agent {self.agent_name}: {e}")

		async def user_model_config_listener(tenant, data_id, group, content):
			"""Listener for user model configuration changes"""
			logger.info(f"[{self.__class__.__name__}] User model config changed - data_id: {data_id}, group: {group}")
			try:
				_model_config = json.loads(content)
				self.model_name = _model_config["modelName"]
				self.api_key = _model_config.get("apiKey", "")
				self.model_provider = _model_config.get("modelProvider",
													   "openai")
				self.base_url = _model_config.get("baseUrl", "")
				self.args = _model_config.get("args", {})
				await self.set_chat_model(self.generate_chat_model())
				logger.info(f"[{self.__class__.__name__}] Model configuration updated successfully")
			except Exception as e:
				logger.error(f"[{self.__class__.__name__}] Failed to update model from config change: {e}")
				if self._backup_model is not None:
					logger.info(f"[{self.__class__.__name__}] Falling back to backup model")
					await self.set_chat_model(self._backup_model)
				else:
					raise Exception(
						f"Failed to create chat model for agent {self.agent_name}: {e}")


		await self.nacos_config_service.add_listener(
				data_id=user_model_config_data_id,
				group=user_model_config_group_name,
				listener=user_model_config_listener)
		logger.debug(f"[{self.__class__.__name__}] Registered user model config listener")
	
	async def initialize(self):
		"""Public initialization method (maintains backward compatibility).
		
		Callers can explicitly call this method for initialization,
		or skip it (will auto-initialize lazily when needed).
		"""
		await self._ensure_initialized()

	def generate_chat_model(self) -> ChatModelBase:
		"""Generate chat model instance based on current configuration.
		
		Returns:
			ChatModelBase: Configured chat model instance
			
		Raises:
			Exception: If model provider is unknown
		"""
		_client_args = self.client_args.copy()
		if self.base_url is not None and len(self.base_url) > 0:
			_client_args["base_url"] = self.base_url
		if self.model_provider == "anthropic":
			chat_model = AnthropicChatModel(model_name=self.model_name,
											api_key=self.api_key,
											stream=self.stream,
											client_args=_client_args,
											**self.args)
		elif self.model_provider == "ollama":
			chat_model = OllamaChatModel(model_name=self.model_name,
										 stream=self.stream,
										 host=self.base_url,
										 **self.args)
		elif self.model_provider == "gemini":
			chat_model = GeminiChatModel(model_name=self.model_name,
										 api_key=self.api_key,
										 stream=self.stream,
										 client_args=_client_args,
										 **self.args)
		elif self.model_provider == "dashscope":
			chat_model = DashScopeChatModel(model_name=self.model_name,
											api_key=self.api_key,
											stream=self.stream,
											generate_kwargs=self.args,
											enable_thinking=self.args.get(
													"enable_thinking", False))
		elif self.model_provider == "openai":
			chat_model = OpenAIChatModel(model_name=self.model_name,
										 api_key=self.api_key,
										 stream=self.stream,
										 client_args=_client_args,
										 **self.args)
		else:
			raise Exception(f"Unknown model provider {self.model_provider}")
		return chat_model

	async def set_chat_model(self, chat_model: ChatModelBase):
		"""Set the chat model (thread-safe).
		
		Args:
			chat_model: The chat model instance to set
		"""
		async with self.model_lock.write_lock():
			self.chat_model = chat_model
			logger.debug(f"[{self.__class__.__name__}] Chat model updated")

	def set_backup_model(self, backup_model: ChatModelBase):
		"""Set the backup model to use when primary model fails.
		
		Args:
			backup_model: The backup chat model instance
		"""
		if backup_model is not self:
			self._backup_model = backup_model
			logger.debug(f"[{self.__class__.__name__}] Backup model set")

	async def get_chat_model(self) -> ChatModelBase:
		"""Get the chat model instance.
		
		Ensures initialization before returning the model.
		
		Returns:
			ChatModelBase: The current chat model instance
		"""
		await self._ensure_initialized()
		async with self.model_lock.read_lock():
			return self.chat_model
	
	# ============================================================================
	# Override key methods to implement lazy initialization
	# ============================================================================

	async def __call__(
			self,
			*args: Any,
			**kwargs: Any,
	) -> ChatResponse | AsyncGenerator[ChatResponse, None]:
		"""Override __call__ method to ensure initialization before invocation.
		
		This is the most commonly called method of ChatModel.
		
		Args:
			*args: Positional arguments for the chat model
			**kwargs: Keyword arguments for the chat model
			
		Returns:
			ChatResponse or AsyncGenerator: Model response
		"""
		await self._ensure_initialized()
		# Directly access chat_model to avoid calling _ensure_initialized again in get_chat_model
		async with self.model_lock.read_lock():
			return await self.chat_model(*args, **kwargs)

	async def close(self):
		"""Close connection and clean up resources"""
		if self.nacos_config_service:
			logger.info(f"[{self.__class__.__name__}] Closing Nacos config service for agent: {self.agent_name}")
			await self.nacos_config_service.shutdown()
			logger.debug(f"[{self.__class__.__name__}] Nacos config service closed")


class AutoFormatter(FormatterBase):
	"""Automatic formatter selector based on model provider.
	
	Automatically selects the appropriate formatter based on the chat model's provider
	and whether multi-agent mode is enabled.
	
	Args:
		if_multi_agent: Whether to use multi-agent formatters
		chat_model: The NacosChatModel instance to format messages for
	
	Example:
		```python
		formatter = AutoFormatter(if_multi_agent=False, chat_model=model)
		formatted_msgs = await formatter.format(messages)
		```
	"""

	def __init__(self, if_multi_agent: bool = False,
			chat_model: NacosChatModel | None = None):
		self.formatter_dict: dict[str, dict[str, FormatterBase]] = {}
		self.if_multi_agent = if_multi_agent
		self.chat_model = chat_model

		self.formatter_dict[str(False)] = {}
		self.formatter_dict[str(True)] = {}

		self.formatter_dict[str(False)]["dashScope"] = DashScopeChatFormatter()
		self.formatter_dict[str(False)]["gemini"] = GeminiChatFormatter()
		self.formatter_dict[str(False)]["ollama"] = OllamaChatFormatter()
		self.formatter_dict[str(False)]["anthropic"] = AnthropicChatFormatter()
		self.formatter_dict[str(False)]["openai"] = OpenAIChatFormatter()

		self.formatter_dict[str(True)][
			"dashScope"] = DashScopeMultiAgentFormatter()
		self.formatter_dict[str(True)]["gemini"] = GeminiMultiAgentFormatter()
		self.formatter_dict[str(True)]["ollama"] = OllamaMultiAgentFormatter()
		self.formatter_dict[str(True)][
			"anthropic"] = AnthropicMultiAgentFormatter()
		self.formatter_dict[str(True)]["openai"] = OpenAIMultiAgentFormatter()

	async def format(self, *args: Any, **kwargs: Any) -> list[dict[str, Any]]:
		return await self.get_formatter().format(*args, **kwargs)

	def get_formatter(self) -> FormatterBase:
		"""Get the appropriate formatter based on model provider.
		
		Returns:
			FormatterBase: The formatter for the current model provider
		"""
		return self.formatter_dict[str(self.if_multi_agent)].get(
			self.chat_model.model_provider,
			self.formatter_dict[str(self.if_multi_agent)]["openai"])
