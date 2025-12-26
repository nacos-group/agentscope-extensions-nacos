# -*- coding: utf-8 -*-
"""\nA2A Protocol with Nacos Service Discovery Example

This example demonstrates A2A protocol with Nacos service discovery
using AgentCardWithRuntimeConfig - a flat configuration approach that
extends AgentCard with runtime-specific fields.

Key Features:
- Flat configuration: All AgentCard fields + runtime fields in one object
- AgentCard inheritance: Automatically inherits all A2A protocol fields
- Runtime fields: registry, transports, task_timeout, etc.
- No nested config objects needed

Prerequisites:
1. Start Nacos server first:
   - Download: https://github.com/alibaba/nacos/releases
   - Start: sh bin/startup.sh -m standalone (macOS/Linux)
   - Or: cmd bin/startup.cmd -m standalone (Windows)
   - Web UI: http://localhost:8848/nacos (default: nacos/nacos)

2. Configure authentication in .env file:
   - If Nacos doesn't require auth (default standalone mode):
     Leave all auth variables empty or commented out
   - If Nacos requires username/password auth:
     Set NACOS_USERNAME and NACOS_PASSWORD in .env file
   - If Nacos requires access key auth (e.g., MSE Nacos):
     Set NACOS_ACCESS_KEY and NACOS_SECRET_KEY in .env file
   - Both authentication methods can be configured together if needed

Usage:
    python app_nacos_a2a.py

Environment Variables:
    DASHSCOPE_API_KEY: API key for DashScope model
    NACOS_SERVER_ADDR: Nacos server address (default: localhost:8848)
    NACOS_USERNAME: Nacos username (optional, for username/password auth)
    NACOS_PASSWORD: Nacos password (optional, for username/password auth)
    NACOS_ACCESS_KEY: Nacos access key (optional, for access key auth, e.g., MSE Nacos)
    NACOS_SECRET_KEY: Nacos secret key (optional, for access key auth, e.g., MSE Nacos)
    NACOS_NAMESPACE_ID: Nacos namespace ID (optional)
"""
import asyncio
import logging
import os
import sys
import time
import traceback
from pathlib import Path
from typing import List

from dotenv import load_dotenv
from agentscope.agent import ReActAgent
from agentscope.formatter import DashScopeChatFormatter
from agentscope.model import DashScopeChatModel
from agentscope.pipeline import stream_printing_messages
from agentscope.tool import Toolkit, execute_python_code
from a2a.types import AgentSkill, AgentCapabilities
from v2.nacos import ClientConfigBuilder

from agentscope_runtime.adapters.agentscope.memory import (
    AgentScopeSessionHistoryMemory,
)
from agentscope_runtime.engine.app import AgentApp
# pylint: disable=no-name-in-module
from agentscope_runtime.engine.deployers.adapter.a2a import (
    A2ARegistry,
    AgentCardWithRuntimeConfig,
    NacosRegistry,
)
from agentscope_runtime.engine.deployers.local_deployer import (
    LocalDeployManager,
)
from agentscope_runtime.engine.schemas.agent_schemas import (
    AgentRequest,
)
from agentscope_runtime.engine.services.agent_state import (
    InMemoryStateService,
)
from agentscope_runtime.engine.services.session_history import (
    InMemorySessionHistoryService,
)

# Setup project paths
_file_path = Path(__file__).resolve()
# example/a2a/app_nacos_a2a.py -> example/a2a -> example -> python
project_root = _file_path.parent.parent.parent

# Load environment variables from .env file
env_file = project_root / ".env"
print(f"Looking for .env at: {env_file}")
if env_file.exists():
    load_dotenv(env_file, override=True)
    print(f"✓ Loaded .env from: {env_file}")
    # Verify loaded values
    print(f"  NACOS_SERVER_ADDR={os.getenv('NACOS_SERVER_ADDR', 'NOT SET')}")
    auth_methods = []
    if os.getenv('NACOS_USERNAME') and os.getenv('NACOS_PASSWORD'):
        auth_methods.append("Username/Password")
    if os.getenv('NACOS_ACCESS_KEY') and os.getenv('NACOS_SECRET_KEY'):
        auth_methods.append("Access Key/Secret Key")
    if auth_methods:
        print(f"  Auth: {', '.join(auth_methods)} configured")
    else:
        print("  Auth: Not configured")
else:
    print(f"✗ Warning: .env file not found at {env_file}")
    print("  Will use default values or system environment variables")

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)

# Configuration constants
DEFAULT_PORT = 8099
DEFAULT_HOST = "0.0.0.0"
AGENT_NAME = "Friday"
AGENT_DESCRIPTION = "A helpful assistant"


def create_agent_skills() -> List[AgentSkill]:
    """Create and return list of agent skills.

    Returns:
        List of AgentSkill objects
    """
    return [
        AgentSkill(
            id="dialog",
            name="Natural Language Dialog Skill",
            description=(
                "Enables natural language conversation "
                "and dialogue with users"
            ),
            tags=["natural language", "dialog", "conversation"],
            examples=[
                "Hello, how are you?",
                "Can you help me with something?",
            ],
        ),
        AgentSkill(
            id="code_execution",
            name="Code Execution Skill",
            description="Can execute Python code to solve problems",
            tags=["code", "python", "execution"],
            examples=[
                "Calculate 2+2",
                "Write a function to sort a list",
            ],
        ),
    ]


# Create NacosRegistry with explicit configuration
# Read from environment variables loaded from .env file
nacos_server_addr = os.getenv("NACOS_SERVER_ADDR", "localhost:8848")
nacos_username = os.getenv("NACOS_USERNAME")
nacos_password = os.getenv("NACOS_PASSWORD")
nacos_namespace_id = os.getenv("NACOS_NAMESPACE_ID")
nacos_access_key = os.getenv("NACOS_ACCESS_KEY")
nacos_secret_key = os.getenv("NACOS_SECRET_KEY")

# Build Nacos client config
builder = ClientConfigBuilder().server_address(nacos_server_addr)
if nacos_namespace_id:
    builder.namespace_id(nacos_namespace_id)

# Configure authentication
auth_methods = []
if nacos_username and nacos_password:
    builder.username(nacos_username).password(nacos_password)
    auth_methods.append("Username/Password")
if nacos_access_key and nacos_secret_key:
    builder.access_key(nacos_access_key).secret_key(nacos_secret_key)
    auth_methods.append("Access Key/Secret Key")

# Print configuration summary
print("Nacos configuration:")
print(f"  Server: {nacos_server_addr}")
print(f"  Namespace: {nacos_namespace_id if nacos_namespace_id else 'default'}")
print(f"  Auth: {', '.join(auth_methods) if auth_methods else 'Disabled'}")

nacos_client_config = builder.build()
nacos_registry = NacosRegistry(nacos_client_config=nacos_client_config)

# Prepare registry as List[A2ARegistry] using typing.List
registry_list: List[A2ARegistry] = [nacos_registry]

# Configure A2A protocol with AgentCardWithRuntimeConfig
# This is a flat configuration that combines:
# - AgentCard protocol fields (name, version, skills, etc.)
# - Runtime-specific fields (registry, task_timeout, etc.)
a2a_config = AgentCardWithRuntimeConfig(
    # === AgentCard Protocol Fields ===
    # These will be published to Nacos and exposed via wellknown endpoint
    name=AGENT_NAME,
    version="1.0.0",
    description=AGENT_DESCRIPTION,
    url=f"http://{DEFAULT_HOST}:{DEFAULT_PORT}/a2a",
    preferredTransport="JSONRPC",
    capabilities=AgentCapabilities(
        streaming=True,
        push_notifications=False,
    ),
    skills=create_agent_skills(),
    defaultInputModes=["text"],
    defaultOutputModes=["text"],
    provider={
        "organization": "AgentScope Runtime",
        "url": "https://runtime.agentscope.io",
    },
    documentUrl="https://runtime.agentscope.io",
    # === Runtime-Specific Fields ===
    # These will NOT be published - only used internally by runtime
    registry=registry_list,  # Must use List[A2ARegistry] from typing
    task_timeout=60,
    task_event_timeout=10,
    wellknown_path="/.wellknown/agent-card.json",
    base_url=f"http://{DEFAULT_HOST}:{DEFAULT_PORT}",
)

agent_app = AgentApp(
    app_name=AGENT_NAME,
    app_description=AGENT_DESCRIPTION,
    a2a_config=a2a_config,
)


@agent_app.init
async def init_func(self):
    """Initialize services."""
    self.state_service = InMemoryStateService()
    self.session_service = InMemorySessionHistoryService()

    await self.state_service.start()
    await self.session_service.start()


@agent_app.shutdown
async def shutdown_func(self):
    """Shutdown services."""
    await self.state_service.stop()
    await self.session_service.stop()


@agent_app.query(framework="agentscope")
async def query_func(
    self,
    msgs,
    request: AgentRequest = None,
    **kwargs,
):
    """Query handler for agent requests."""
    assert kwargs is not None, "kwargs is required for query_func"
    session_id = request.session_id
    user_id = request.user_id

    state = await self.state_service.export_state(
        session_id=session_id,
        user_id=user_id,
    )

    toolkit = Toolkit()
    toolkit.register_tool_function(execute_python_code)

    agent = ReActAgent(
        name=AGENT_NAME,
        model=DashScopeChatModel(
            "qwen-turbo",
            api_key=os.getenv("DASHSCOPE_API_KEY"),
            enable_thinking=True,
            stream=True,
        ),
        sys_prompt=f"You're a helpful assistant named {AGENT_NAME}.",
        toolkit=toolkit,
        memory=AgentScopeSessionHistoryMemory(
            service=self.session_service,
            session_id=session_id,
            user_id=user_id,
        ),
        formatter=DashScopeChatFormatter(),
    )

    if state:
        agent.load_state_dict(state)

    async for msg, last in stream_printing_messages(
        agents=[agent],
        coroutine_task=agent(msgs),
    ):
        yield msg, last

    state = agent.state_dict()
    await self.state_service.save_state(
        user_id=user_id,
        session_id=session_id,
        state=state,
    )


async def main():
    """Main deployment function."""
    # AgentApp is already initialized with a2a_runtime_config
    # The A2A adapter is automatically created by AgentApp
    # base_url is already set in AgentCardWithRuntimeConfig,
    # so no need to manually set it on the adapter

    deploy_manager = LocalDeployManager(
        host=DEFAULT_HOST,
        port=DEFAULT_PORT,
    )
    await agent_app.deploy(deploy_manager)


if __name__ == "__main__":
    BANNER = (
        "\n"
        + "=" * 60
        + "\n"
        + "A2A Protocol with Nacos Service Discovery Example\n"
        + "=" * 60
        + "\n"
        + "\nPrerequisites:\n"
        + "  1. Start Nacos server (standalone mode):\n"
        + "     sh bin/startup.sh -m standalone\n"
        + "     Web UI: http://localhost:8848/nacos\n"
        + "\n"
        + "    2. Configure .env file:\n"
     + "     DASHSCOPE_API_KEY: Required for LLM\n"
     + "     NACOS_SERVER_ADDR: Nacos server address\n"
     + "     NACOS_USERNAME: Optional, for username/password auth\n"
     + "     NACOS_PASSWORD: Optional, for username/password auth\n"
     + "     NACOS_ACCESS_KEY: Optional, for access key auth (e.g., MSE Nacos)\n"
     + "     NACOS_SECRET_KEY: Optional, for access key auth (e.g., MSE Nacos)\n"
     + "     NACOS_NAMESPACE_ID: Optional, namespace ID\n"
        + "=" * 60
        + "\n"
    )
    print(BANNER)

    try:
        asyncio.run(main())
        print("\nServer is running. Press Ctrl+C to stop...")
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("\nShutting down...")
    except Exception as e:
        print(f"\nError: {e}")
        traceback.print_exc()
        sys.exit(1)
