from abc import ABC, abstractmethod
from typing import Any, Callable, Awaitable, List, Literal, TYPE_CHECKING, Optional
import asyncio
import weakref
import logging

import mcp
from agentscope.mcp import MCPToolFunction, MCPClientBase, StatefulClientBase
from agentscope.tool import ToolResponse, Toolkit
from mcp import ClientSession, StdioServerParameters, stdio_client
from mcp.client.sse import sse_client
from mcp.client.streamable_http import streamablehttp_client
from v2.nacos import ClientConfig
from v2.nacos.ai.model.ai_param import GetMcpServerParam, \
	SubscribeMcpServerParam
from v2.nacos.ai.model.mcp.mcp import McpServerDetailInfo
from v2.nacos.ai.nacos_ai_service import NacosAIService


from agentscope_nacos.nacos_service_manager import NacosServiceManager
from agentscope_nacos.utils import \
	random_generate_url_from_mcp_server_detail_info

if TYPE_CHECKING:
	from agentscope.tool import Toolkit

try:
	from contextlib import _AsyncGeneratorContextManager, AsyncExitStack
except ImportError:
	from collections.abc import AsyncGenerator as _AsyncGeneratorContextManager

# Initialize logger
logger = logging.getLogger(__name__)



class NacosMCPClientBase(MCPClientBase, ABC):
	"""Base class for Nacos-based MCP (Model Context Protocol) clients.
	
	This abstract class provides core functionality for connecting to MCP servers
	registered in Nacos, managing tools, and notifying registered toolkits of changes.
	Uses NacosServiceManager for connection pooling.
	
	Features:
		- Lazy initialization for better performance
		- Automatic MCP server discovery via Nacos
		- Tool list caching and auto-refresh
		- Weak reference-based toolkit notification to avoid memory leaks
		- Thread-safe operations
		
	Args:
		name: Name of the MCP server in Nacos
		nacos_client_config: Optional Nacos client config (uses global if None)
	"""

	def __init__(
		self,
		name: str,
		nacos_client_config: Optional[ClientConfig] = None,
	) -> None:
		super().__init__(name)
		self.name = name
		self._nacos_client_config = nacos_client_config
		self.nacos_ai_service: NacosAIService | None = None
		self.mcp_server_detail_info: McpServerDetailInfo | None = None
		self._tools: List[mcp.types.Tool] = []
		self._tools_meta = {}
		
		# Use weak reference set to store Toolkit observers, avoiding circular references
		self._toolkit_refs: weakref.WeakSet['Toolkit'] = weakref.WeakSet()
		self._update_lock = asyncio.Lock()
		self._is_updating = False  # Flag to prevent recursive updates
		
		# Lazy initialization state
		self._initialized = False
		self._initializing = False
		self._init_lock = asyncio.Lock()
		
		logger.debug(f"[{self.__class__.__name__}] Initialized for MCP server: {name}")


	async def _ensure_initialized(self):
		"""Ensure MCP Client is initialized (thread-safe lazy initialization).
		
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
				logger.info(f"[{self.__class__.__name__}] Initializing MCP client for: {self.name}")
				await self._async_init()
				self._initialized = True
				logger.info(f"[{self.__class__.__name__}] Initialization completed for: {self.name}")
			except Exception as e:
				logger.error(f"[{self.__class__.__name__}] Failed to initialize: {e}")
				raise
			finally:
				self._initializing = False
	
	async def _async_init(self):
		"""Internal async initialization logic.
		
		Fetches MCP server details from Nacos and subscribes to updates.
		Uses NacosServiceManager for connection pooling.
		"""
		# Get Nacos AI service with connection pooling
		manager = NacosServiceManager()
		self.nacos_ai_service = await manager.get_ai_service(
			self._nacos_client_config)
		
		# Fetch MCP server details from Nacos
		self.mcp_server_detail_info = await self.nacos_ai_service.get_mcp_server(
				GetMcpServerParam(
						mcp_name=self.name,
				))
		
		if self.mcp_server_detail_info is None or self.mcp_server_detail_info.frontProtocol not in self.get_supported_transport():
			logger.error(f"[{self.__class__.__name__}] Invalid MCP server detail info for: {self.name}")
			raise ValueError("MCP server detail info is None or unsupported transport")

		# Callback for MCP server updates from Nacos
		async def callback(mcp_id, namespace_id, mcp_name,
				mcp_server_detail_info: McpServerDetailInfo):
			"""Handle MCP server detail updates from Nacos."""
			logger.info(f"[{self.__class__.__name__}] MCP server updated: {mcp_name}")
			self.mcp_server_detail_info = mcp_server_detail_info
			changed = self.update_tools(mcp_server_detail_info)
			if changed and self._toolkit_refs:
				logger.debug(f"[{self.__class__.__name__}] Tools changed, notifying toolkits")
				asyncio.create_task(self._notify_toolkits())

		self.subscribe_param = SubscribeMcpServerParam(
				mcp_name=self.name,
				subscribe_callback=callback,
		)

		await self.nacos_ai_service.subscribe_mcp_server(
				self.subscribe_param
		)
		logger.debug(f"[{self.__class__.__name__}] Subscribed to MCP server updates for: {self.name}")
	
	async def initialize(self):
		"""Public initialization method (backward compatible).
		
		Callers can explicitly invoke this method for initialization,
		or skip it (lazy initialization will occur automatically).
		"""
		await self._ensure_initialized()

	@abstractmethod
	def get_supported_transport(
			self,
	) -> List[str]:
		"""Get list of supported transport protocols.
		
		Returns:
			List of supported transport protocol names (e.g., ["sse", "stdio"])
		"""
	
	# ============================================================================
	# Template Method Pattern: Abstract methods implemented by subclasses,
	# common logic handled in base class
	# ============================================================================
	
	@abstractmethod
	async def _list_tools_impl(self) -> List[mcp.types.Tool]:
		"""Subclass-specific tool list retrieval logic (low-level operation).
		
		This method is called after initialization is complete,
		no need to check initialization status.
		
		Returns:
			Raw tool list (unfiltered, uncached)
		"""
		pass
	
	@abstractmethod
	def _create_tool_function_impl(
			self,
			tool: mcp.types.Tool,
			wrap_tool_result: bool = True,
	) -> Callable[..., Awaitable[mcp.types.CallToolResult | ToolResponse]]:
		"""Subclass-specific tool function creation logic (low-level operation).
		
		Args:
			tool: Tool definition
			wrap_tool_result: Whether to wrap tool result
			
		Returns:
			Callable tool function
		"""
		pass
	
	# ============================================================================
	# Template Methods: Unified public flow implementation in base class
	# ============================================================================
	
	async def list_tools(self) -> List[mcp.types.Tool]:
		"""List all available tools (template method).
		
		Unified processing: init check → get tools → cache → update → filter
		
		Returns:
			List of enabled tools
		"""
		# 1. Ensure initialization
		await self._ensure_initialized()
		
		# 2. Call subclass implementation to get raw tool list
		tools = await self._list_tools_impl()
		logger.debug(f"[{self.__class__.__name__}] Fetched {len(tools)} tools from MCP server")
		
		# 3. Cache tools
		self._tools = tools
		
		# 4. Update tool metadata
		self.update_tools(self.mcp_server_detail_info)
		
		# 5. Return enabled tools only
		enabled_tools = [
			tool for tool in self._tools if
			self.is_tool_enabled(tool.name)
		]
		logger.info(f"[{self.__class__.__name__}] {len(enabled_tools)} tools enabled out of {len(tools)}")
		return enabled_tools
	
	async def get_callable_function(
			self,
			func_name: str,
			wrap_tool_result: bool = True,
	) -> Callable[..., Awaitable[mcp.types.CallToolResult | ToolResponse]]:
		"""
		Get a callable tool function (template method).
		Unified processing: initialization check → find tool → create function
		
		Args:
			func_name: Tool function name
			wrap_tool_result: Whether to wrap the tool result
			
		Returns:
			Callable tool function
			
		Raises:
			ValueError: If the tool does not exist or is not enabled
		"""
		# 1. Ensure initialization
		await self._ensure_initialized()
		
		# 2. If tool list is empty, fetch tools first
		if self._tools is None or len(self._tools) == 0:
			await self.list_tools()
		
		# 3. Find the target tool
		target_tool = None
		for tool in self._tools:
			if tool.name == func_name and self.is_tool_enabled(tool.name):
				target_tool = tool
				break
		
		if target_tool is None:
			raise ValueError(
				f"Tool '{func_name}' not found in the MCP server '{self.name}'"
			)
		
		# 4. Call subclass implementation to create function
		return self._create_tool_function_impl(target_tool, wrap_tool_result)
	
	def _attach_toolkit(self, toolkit: 'Toolkit') -> None:
		"""Internal method: Register Toolkit as an observer for automatic synchronization when tools are updated.
		
		Note: This method should be automatically called by DynamicToolkit; users do not need to call it manually.
		
		Args:
			toolkit: The Toolkit instance that needs automatic synchronization
		"""
		self._toolkit_refs.add(toolkit)
		logger.info(f"Toolkit attached to MCP client '{self.name}'")
	
	def _detach_toolkit(self, toolkit: 'Toolkit') -> None:
		"""Internal method: Remove Toolkit observer.
		
		Note: This method should be automatically called by DynamicToolkit; users do not need to call it manually.
		
		Args:
			toolkit: The Toolkit instance to be removed
		"""
		try:
			self._toolkit_refs.remove(toolkit)
			logger.info(f"Toolkit detached from MCP client '{self.name}'")
		except KeyError:
			pass  # Toolkit has already been removed or garbage collected
	
	async def _notify_toolkits(self) -> None:
		"""Notify all registered Toolkits and re-synchronize tools when tools are updated.
		
		Uses a flag to avoid recursive calls.
		"""
		# If updating, return directly to avoid recursion
		if self._is_updating:
			return
		
		# Get all current toolkit references (weak reference set may change during iteration)
		toolkits = list(self._toolkit_refs)
		
		if not toolkits:
			return
		
		async with self._update_lock:
			self._is_updating = True
			try:
				logger.info(
					f"Notifying {len(toolkits)} toolkit(s) about "
					f"tool changes in MCP client '{self.name}'"
				)
				
				for toolkit in toolkits:
					try:
						# Use parent Toolkit methods to avoid triggering DynamicToolkit's extra logic
						# First remove old tools
						await Toolkit.remove_mcp_clients(toolkit, [self.name])
						
						# Re-register new tools
						await Toolkit.register_mcp_client(toolkit, self)
						
						logger.info(
							f"Toolkit synchronized with MCP client '{self.name}'"
						)
					except Exception as e:
						logger.error(
							f"Failed to sync toolkit with MCP client "
							f"'{self.name}': {e}",
							exc_info=True
						)
			finally:
				self._is_updating = False
	
	def _check_tools_changed(
		self, 
		server_detail_info: McpServerDetailInfo
	) -> bool:
		"""Check if the tool list or tool metadata has changed.
		
		Returns:
			bool: Returns True if tools have changed, otherwise False
		"""
		if self._tools is None:
			return False
		
		tool_spec = server_detail_info.toolSpec
		if tool_spec is None:
			return False
		
		# Check if the number of tools has changed
		if tool_spec.tools is None:
			return False
		
		# Check if the enabled status of tools has changed
		if tool_spec.toolsMeta != self._tools_meta:
			logger.info(
				f"Tool metadata changed in MCP client '{self.name}'"
			)
			return True
		
		return False

	def update_tools(self, server_detail_info: McpServerDetailInfo) -> bool:
		"""Update tool information and automatically notify all registered Toolkits when tools change."""
		
		# Check if tools have changed (check before updating)
		tools_changed = self._check_tools_changed(server_detail_info)

		def update_args_description(_local_args: dict[str, Any],
				_nacos_args: dict[str, Any]):
			nonlocal tools_changed
			for key, value in _local_args.items():
				if key in _nacos_args and "description" in _nacos_args[key] and value["description"] != _nacos_args[key]["description"]:
					tools_changed = True
					_local_args[key]["description"] = _nacos_args[key][
						"description"]

		tool_spec = server_detail_info.toolSpec
		if tool_spec is None:
			return tools_changed
		if tool_spec.toolsMeta is None:
			self._tools_meta = {}
		else:
			self._tools_meta = tool_spec.toolsMeta
		if tool_spec.tools is None:
			return tools_changed
		for tool in tool_spec.tools:
			for self_tool in self._tools:
				if self_tool.name == tool.name:
					if tool.description is not None and self_tool.description != tool.description:
						self_tool.description = tool.description
						tools_changed = True
					local_args = self_tool.inputSchema["properties"]
					nacos_args = tool.inputSchema["properties"]
					update_args_description(local_args, nacos_args)
					break


		return tools_changed

	def is_tool_enabled(self, tool_name: str) -> bool:
		if self._tools_meta is None:
			return True
		if tool_name in self._tools_meta:
			mcp_tool_meta = self._tools_meta[tool_name]
			if mcp_tool_meta.enabled is not None:
				if not mcp_tool_meta.enabled:
					return False
		return True

	async def shutdown(self):
		pass


class NacosHttpStatelessClient(NacosMCPClientBase):
	"""Stateless HTTP MCP client.
	
	Creates a new connection for each tool invocation.
	Suitable for intermittent operations or low-frequency tool calls.
	Supports SSE and streamable HTTP transport protocols.
	
	Features:
		- No persistent connection overhead
		- Simple lifecycle management
		- Automatic cleanup after each operation
	"""
	
	stateful: bool = False

	def __init__(
			self,
			nacos_client_config: ClientConfig,
			name: str,
			headers: dict[str, str] | None = None,
			timeout: float = 30,
			sse_read_timeout: float = 60 * 5,
			**client_kwargs: Any,
	) -> None:
		super().__init__(name=name,
									nacos_client_config=nacos_client_config)

		self.client_config = {
			"headers": headers or {},
			"timeout": timeout,
			"sse_read_timeout": sse_read_timeout,
			**client_kwargs,
		}

	def get_supported_transport(self) -> List[str]:
		return ["mcp-sse", "mcp-streamable"]

	def get_client(self):
		"""
		The disposable MCP client object, which is a context manager.
		Note: This method is synchronous, but should be called after initialization
		"""
		if not self._initialized:
			raise RuntimeError(
				f"MCP client '{self.name}' not initialized. "
				f"Please ensure async methods are called first, "
				f"or call 'await client.initialize()' explicitly."
			)
		
		transport = self.mcp_server_detail_info.frontProtocol
		url = random_generate_url_from_mcp_server_detail_info(
				self.mcp_server_detail_info)
		config_with_url = {**self.client_config, "url": url}
		if transport == "mcp-sse":
			# Create a new dict with all contents of self.client_config and the new url key-value pair
			# Use a safer way to handle SSE clients to avoid async context issues
			try:
				return sse_client(**config_with_url)
			except Exception as e:
				# Log error but do not interrupt the program
				import logging
				logging.warning(f"Failed to create SSE client: {e}")
				raise

		if transport == "mcp-streamable":
			return streamablehttp_client(**config_with_url)

		raise ValueError(
				f"Unsupported transport type: {transport}. "
				"Supported types are 'sse' and 'streamable_http'.",
		)
	
	# ============================================================================
	# Implement abstract methods: Only responsible for low-level operations, not handling initialization and caching
	# ============================================================================
	
	async def _list_tools_impl(self) -> List[mcp.types.Tool]:
		"""
		Stateless client's tool list retrieval implementation.
		Creates a new connection each time to fetch the tool list
		"""
		async with self.get_client() as cli:
			read_stream, write_stream = cli[0], cli[1]
			async with ClientSession(read_stream, write_stream) as session:
				await session.initialize()
				res = await session.list_tools()
				return res.tools
	
	def _create_tool_function_impl(
			self,
			tool: mcp.types.Tool,
			wrap_tool_result: bool = True,
	) -> Callable[..., Awaitable[mcp.types.CallToolResult | ToolResponse]]:
		"""
		Stateless client's function creation implementation.
		Returns a tool function that uses client_gen
		"""
		return MCPToolFunction(
				mcp_name=self.name,
				tool=tool,
				wrap_tool_result=wrap_tool_result,
				client_gen=self.get_client,
		)


class NacosStatefulClientBase(NacosMCPClientBase, StatefulClientBase, ABC):
	"""Base class for stateful MCP clients.
	
	Maintains persistent connection with MCP server for better performance
	and lower latency in high-frequency tool invocations.
	
	Features:
		- Persistent connection for reduced latency
		- Connection lifecycle management (connect/disconnect)
		- Automatic reconnection on failure
		- Session state preservation
	"""

	def __init__(self, nacos_client_config: ClientConfig,
			name: str) -> None:

		StatefulClientBase.__init__(self, name=name)

		NacosMCPClientBase.__init__(self, name=name,
									nacos_client_config=nacos_client_config)

	async def connect(self):
		"""
		Connect to MCP server.
		Override this method to ensure initialization before connecting
		"""
		await self._ensure_initialized()
		await super().connect()
	
	# ============================================================================
	# Implement abstract methods: Only responsible for low-level operations, not handling initialization and caching
	# Note: _validate_connection() method is inherited from StatefulClientBase, no need to reimplement
	# ============================================================================
	
	async def _list_tools_impl(self) -> List[mcp.types.Tool]:
		"""
		Stateful client's tool list retrieval implementation.
		Uses the established persistent connection to fetch the tool list
		"""
		self._validate_connection()
		res = await self.session.list_tools()
		return res.tools
	
	def _create_tool_function_impl(
			self,
			tool: mcp.types.Tool,
			wrap_tool_result: bool = True,
	) -> MCPToolFunction:
		"""
		Stateful client's function creation implementation.
		Returns a tool function that uses the session
		"""
		self._validate_connection()
		return MCPToolFunction(
				mcp_name=self.name,
				tool=tool,
				wrap_tool_result=wrap_tool_result,
				session=self.session,
		)

	async def shutdown(self):
		await self.close()
		await NacosMCPClientBase.shutdown(self)


class NacosHttpStatefulClient(NacosStatefulClientBase):

	def __init__(self, nacos_client_config: ClientConfig,
			name: str,
			headers: dict[str, str] | None = None,
			timeout: float = 30,
			sse_read_timeout: float = 60 * 5,
			**client_kwargs: Any,
	) -> None:
		super().__init__(nacos_client_config=nacos_client_config,
						 name=name)
		self.client_config = {
			"headers": headers or {},
			"timeout": timeout,
			"sse_read_timeout": sse_read_timeout,
			**client_kwargs,
		}

	async def _async_init(self):
		"""
		Override parent's async initialization logic.
		Add HTTP client-specific initialization
		"""
		await super()._async_init()
		url = random_generate_url_from_mcp_server_detail_info(
				self.mcp_server_detail_info)
		config_with_url = {**self.client_config, "url": url}
		if self.mcp_server_detail_info.frontProtocol == "mcp-sse":
			self.client = sse_client(
					**config_with_url
			)
		else:
			self.client = streamablehttp_client(
					**config_with_url
			)

	def get_supported_transport(self) -> List[str]:
		return ["mcp-sse", "mcp-streamable"]


class NacosStdIOStatefulClient(NacosStatefulClientBase):

	def __init__(
			self,
			nacos_client_config: ClientConfig,
			name: str,
			env: dict[str, str] | None = None,
			cwd: str | None = None,
			encoding: str = "utf-8",
			encoding_error_handler: Literal[
				"strict",
				"ignore",
				"replace",
			] = "strict",
	) -> None:
		super().__init__(nacos_client_config=nacos_client_config,
						 name=name)

		self.env = env
		self.cwd = cwd
		self.encoding = encoding
		self.encoding_error_handler = encoding_error_handler

	async def _async_init(self):
		"""
		Override parent's async initialization logic.
		Add StdIO client-specific initialization
		"""
		await super()._async_init()
		local_server_config = self.mcp_server_detail_info.localServerConfig

		# Recursively find command and args keys
		def find_command_args(config_dict, command_key="command",
				args_key="args"):
			"""
			Recursively find command and args keys in nested dictionary
			"""
			if not isinstance(config_dict, dict):
				return None, None

			command_value = None
			args_value = None

			# Check if the current level has command or args keys
			if command_key in config_dict:
				command_value = config_dict[command_key]

			if args_key in config_dict:
				args_value = config_dict[args_key]

			# If both values are found at the current level, return directly
			if command_value is not None and args_value is not None:
				return command_value, args_value

			# Otherwise, recursively check each level
			for key, value in config_dict.items():
				if isinstance(value, dict):
					cmd, args = find_command_args(value, command_key, args_key)
					if cmd is not None and command_value is None:
						command_value = cmd
					if args is not None and args_value is None:
						args_value = args

					# If both are found, terminate early
					if command_value is not None and args_value is not None:
						break

			return command_value, args_value

		# Find command and args
		command, args = find_command_args(local_server_config)

		self.client = stdio_client(
				StdioServerParameters(
						command=command,
						args=args or [],
						env=self.env,
						cwd=self.cwd,
						encoding=self.encoding,
						encoding_error_handler=self.encoding_error_handler,
				),
		)

	def get_supported_transport(self) -> List[str]:
		return ["stdio"]
