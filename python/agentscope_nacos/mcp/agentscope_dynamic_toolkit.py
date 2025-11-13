# -*- coding: utf-8 -*-
"""Dynamic Toolkit - Toolkit extension supporting Nacos MCP tool auto-update"""

import logging
from typing import Any, Callable, Literal, Type, AsyncGenerator

from agentscope.mcp import MCPClientBase
from agentscope.message import ToolUseBlock
from agentscope.tool import Toolkit, ToolResponse
from agentscope.types import ToolFunction, JSONSerializableObject
from pydantic import BaseModel

from .agentscope_nacos_mcp import NacosMCPClientBase

# Initialize logger
logger = logging.getLogger(__name__)


class DynamicToolkit(Toolkit):
	"""Dynamic Toolkit supporting automatic tool updates.
	
	When using NacosMCPClientBase clients, automatically establishes bidirectional binding:
	- Auto-attach to client when registering MCP client
	- Auto-detach from client when removing MCP client
	- Auto-sync to Toolkit when Nacos pushes tool changes
	
	Usage: Simply replace Toolkit with DynamicToolkit
	
	Features:
		- Automatic tool synchronization from Nacos
		- Weak reference-based client tracking
		- Seamless integration with existing AgentScope code
		- Support for multiple MCP client registration
	
	Example:
		```python
		from agentscope_dynamic_toolkit import DynamicToolkit
		
		toolkit = DynamicToolkit()  # Replace original Toolkit()
		await toolkit.register_mcp_client(nacos_client)
		# Tools automatically follow Nacos configuration changes
		```
	"""
	
	def __init__(self, toolkit: Toolkit | None = None) -> None:
		"""Initialize dynamic toolkit.
		
		Args:
		    toolkit: Optional Toolkit instance.
		            If provided, will use that instance;
		            If not provided, will create new Toolkit instance.
		"""
		
		# Initialize internal toolkit using object.__setattr__
		if toolkit is not None:
			if isinstance(toolkit, DynamicToolkit):
				object.__setattr__(self, '_toolkit', toolkit.get_inner_toolkit())
			else:
				object.__setattr__(self, '_toolkit', toolkit)
		else: 
			object.__setattr__(self, '_toolkit', Toolkit())
		
		# Initialize nacos clients dictionary
		object.__setattr__(self, '_nacos_clients', {})
		
		# Now safely call parent initialization
		super().__init__()
		
		logger.debug(f"[{self.__class__.__name__}] Initialized")

	def get_inner_toolkit(self):
		return self._toolkit

	def set_inner_toolkit(self, toolkit: Toolkit):
		if isinstance(toolkit, DynamicToolkit):
			self._toolkit = toolkit.get_inner_toolkit()
		else:
			self._toolkit = toolkit

	@property
	def tools(self):
		"""Proxy tools property to internal toolkit"""
		return self._toolkit.tools

	@tools.setter
	def tools(self, value):
		"""Set tools property to internal toolkit"""
		self._toolkit.tools = value

	@property
	def groups(self):
		"""Proxy groups property to internal toolkit"""
		return self._toolkit.groups

	@groups.setter
	def groups(self, value):
		"""Set groups property to internal toolkit"""
		self._toolkit.groups = value
	
	async def register_mcp_client(
		self,
		mcp_client: MCPClientBase,
		group_name: str = "basic",
		enable_funcs: list[str] | None = None,
		disable_funcs: list[str] | None = None,
		preset_kwargs_mapping: dict[str, dict[str, Any]] | None = None,
		postprocess_func: Callable[
			[ToolUseBlock, ToolResponse],
			ToolResponse | None,
		] | None = None,
	) -> None:
		"""Register MCP client and automatically establish dynamic update association.
		
		If mcp_client is an instance of NacosMCPClientBase, will automatically:
		1. Register tools to Toolkit
		2. Register Toolkit to client's observer list
		3. Auto-sync when tools change
		
		Args:
			mcp_client: MCP client instance
			group_name: Tool group name
			enable_funcs: List of enabled tool functions
			disable_funcs: List of disabled tool functions
			preset_kwargs_mapping: Preset kwargs mapping
			postprocess_func: Post-processing function
		"""
		# Call parent method to register tools
		await self._toolkit.register_mcp_client(
			mcp_client=mcp_client,
			group_name=group_name,
			enable_funcs=enable_funcs,
			disable_funcs=disable_funcs,
			preset_kwargs_mapping=preset_kwargs_mapping,
			postprocess_func=postprocess_func,
		)
		
		# If NacosMCPClientBase, auto-establish bidirectional binding
		if isinstance(mcp_client, NacosMCPClientBase):
			# If a client with same name was registered before, unbind the old one
			if mcp_client.name in self._nacos_clients:
				old_client = self._nacos_clients[mcp_client.name]
				if old_client is not mcp_client:
					old_client._detach_toolkit(self)
					logger.debug(f"[{self.__class__.__name__}] Detached from old client: {mcp_client.name}")
			
			# Register new client
			mcp_client._attach_toolkit(self)
			self._nacos_clients[mcp_client.name] = mcp_client
			
			logger.info(
				f"[{self.__class__.__name__}] Auto-attached to Nacos MCP client "
				f"'{mcp_client.name}' for automatic tool updates"
			)
	
	async def remove_mcp_clients(
		self,
		client_names: list[str],
	) -> None:
		"""Remove MCP clients and automatically unbind associations.
		
		If the removed client is an instance of NacosMCPClientBase, will automatically:
		1. Remove related tools from Toolkit
		2. Remove Toolkit from client's observer list
		
		Args:
			client_names: List of client names to remove
		"""
		# Ensure it is a list type
		if isinstance(client_names, str):
			client_names = [client_names]
		
		# First unbind Nacos clients
		for client_name in client_names:
			if client_name in self._nacos_clients:
				nacos_client = self._nacos_clients[client_name]
				nacos_client._detach_toolkit(self)
				del self._nacos_clients[client_name]
				logger.info(
					f"[{self.__class__.__name__}] Auto-detached from Nacos MCP client "
					f"'{client_name}'"
				)
		
		# Call parent method to remove tools
		await self._toolkit.remove_mcp_clients(client_names)
	
	def clear(self) -> None:
		"""Clear the toolkit and unbind all Nacos clients"""
		# Unbind all Nacos clients
		for nacos_client in list(self._nacos_clients.values()):
			nacos_client._detach_toolkit(self)
		
		self._nacos_clients.clear()
		
		# Call parent method to clear tools
		self._toolkit.clear()
		
		logger.info(f"[{self.__class__.__name__}] Cleared all tools and detached all clients")

	def create_tool_group(
			self,
			group_name: str,
			description: str,
			active: bool = False,
			notes: str | None = None,
	) -> None:
		self._toolkit.create_tool_group(
			group_name=group_name,
			description=description,
			active=active,
			notes=notes,
		)

	def update_tool_groups(self, group_names: list[str], active: bool) -> None:
		self._toolkit.update_tool_groups(group_names=group_names, active=active)

	def remove_tool_groups(self, group_names: str | list[str]) -> None:
		self._toolkit.remove_tool_groups(group_names=group_names)

	def register_tool_function(  # pylint: disable=too-many-branches
			self,
			tool_func: ToolFunction,
			group_name: str | Literal["basic"] = "basic",
			preset_kwargs: dict[str, JSONSerializableObject] | None = None,
			func_description: str | None = None,
			json_schema: dict | None = None,
			include_long_description: bool = True,
			include_var_positional: bool = False,
			include_var_keyword: bool = False,
			postprocess_func: Callable[
								  [
									  ToolUseBlock,
									  ToolResponse,
								  ],
								  ToolResponse | None,
							  ]
							  | None = None,
	) -> None:
		self._toolkit.register_tool_function(
			tool_func=tool_func,
			group_name=group_name,
			preset_kwargs=preset_kwargs,
			func_description=func_description,
			json_schema=json_schema,
			include_long_description=include_long_description,
			include_var_positional=include_var_positional,
			include_var_keyword=include_var_keyword,
			postprocess_func=postprocess_func,
		)

	def remove_tool_function(self, tool_name: str) -> None:
		self._toolkit.remove_tool_function(tool_name=tool_name)

	def get_json_schemas(
			self,
	) -> list[dict]:
		return self._toolkit.get_json_schemas()

	def set_extended_model(
			self,
			func_name: str,
			model: Type[BaseModel] | None,
	) -> None:
		self._toolkit.set_extended_model(func_name=func_name, model=model)

	async def call_tool_function(
			self,
			tool_call: ToolUseBlock,
	) -> AsyncGenerator[ToolResponse, None]:
		return await self._toolkit.call_tool_function(tool_call=tool_call)

	def state_dict(self) -> dict[str, Any]:
		return self._toolkit.state_dict()

	def load_state_dict(
			self,
			state_dict: dict[str, Any],
			strict: bool = True,
	) -> None:
		self._toolkit.load_state_dict(state_dict=state_dict, strict=strict)

	def get_activated_notes(self) -> str:
		return self._toolkit.get_activated_notes()

	def reset_equipped_tools(self, **kwargs: Any) -> ToolResponse:
		return self._toolkit.reset_equipped_tools(**kwargs)

	def _validate_tool_function(self, func_name: str) -> None:
		self._toolkit._validate_tool_function(func_name=func_name)

