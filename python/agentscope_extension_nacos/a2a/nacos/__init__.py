# -*- coding: utf-8 -*-
"""
A2A Nacos Integration Module

Provides deep integration between A2A protocol and Nacos:
- Automatic Agent Card registration to Nacos
- Agent Card resolution from Nacos
- Service endpoint auto-registration and discovery

Main Classes:
- A2AFastAPIExtensionAdapter: FastAPI adapter that exposes Agent as A2A service
                              and registers to Nacos (via NacosRegistry)
- NacosRegistry: Registry implementation for registering agents to Nacos
- NacosA2ACardResolver: Agent Card resolver that fetches from Nacos

Configuration Methods:
    1. Using .env file (recommended for development):
       Create a .env file with:
       A2A_REGISTRY_ENABLED=true
       A2A_REGISTRY_TYPE=nacos
       NACOS_SERVER_ADDR=localhost:8848
       NACOS_USERNAME=your_username  # optional
       NACOS_PASSWORD=your_password  # optional
    
    2. Explicitly provide configuration:
       from v2.nacos import ClientConfigBuilder
       config = ClientConfigBuilder().server_address("localhost:8848").build()
       registry = NacosRegistry(nacos_client_config=config)
       adapter = A2AFastAPIExtensionAdapter(..., registry=registry)

Usage Examples:
    >>> from agentscope_extension_nacos.a2a.nacos import (
    ...     A2AFastAPIExtensionAdapter,
    ...     NacosRegistry,
    ...     NacosA2ACardResolver,
    ... )
    >>> 
    >>> # Server side: Expose Agent as A2A service with .env configuration
    >>> adapter = A2AFastAPIExtensionAdapter(
    ...     agent_name="MyAgent",
    ...     agent_description="A helpful assistant",
    ...     # registry will be loaded from .env if A2A_REGISTRY_TYPE=nacos
    ... )
    >>> 
    >>> # Server side: Explicit registry configuration (alternative)
    >>> from v2.nacos import ClientConfigBuilder
    >>> config = ClientConfigBuilder().server_address("localhost:8848").build()
    >>> registry = NacosRegistry(nacos_client_config=config)
    >>> adapter = A2AFastAPIExtensionAdapter(
    ...     agent_name="MyAgent",
    ...     agent_description="A helpful assistant",
    ...     registry=registry,
    ... )
    >>> 
    >>> # Client side: Get Agent Card from Nacos with .env configuration
    >>> resolver = NacosA2ACardResolver(
    ...     remote_agent_name="remote_agent",
    ...     # nacos_client_config will be loaded from .env if not provided
    ... )
    >>> from agentscope_extension_nacos.a2a import A2aAgent
    >>> agent = A2aAgent(agent_card_resolver=resolver)
"""

from agentscope_extension_nacos.a2a.nacos.a2a_protocol_adapter import (
    A2AFastAPIExtensionAdapter,
)

from agentscope_extension_nacos.a2a.nacos.nacos_a2a_registry import (
    NacosRegistry,
)

from agentscope_extension_nacos.a2a.nacos.nacos_a2a_card_resolver import (
    NacosA2ACardResolver,
)

__all__ = [
    "A2AFastAPIExtensionAdapter",
    "NacosRegistry",
    "NacosA2ACardResolver",
]
