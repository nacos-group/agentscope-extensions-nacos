"""
Example: A2A Agent from Nacos A2A Registry

This example demonstrates how to discover and connect to A2A agents
registered in the Nacos A2A Registry using NacosA2ACardResolver.

Prerequisites:
    - The target agent must be deployed and registered in Nacos A2A Registry
    - See runtime_nacos_a2a_example.py for how to deploy an agent

Features:
    - Automatic Agent Card discovery from Nacos
    - Connect to agents registered in Nacos A2A Registry
    - Multi-turn conversation with session management
"""

import asyncio

from agentscope.agent import UserAgent, UserInputBase, UserInputData
from agentscope.message import TextBlock
from v2.nacos import ClientConfigBuilder
from agentscope_extension_nacos.a2a.a2a_agent import A2aAgent
from agentscope_extension_nacos.a2a.nacos.nacos_a2a_card_resolver import \
    NacosA2ACardResolver
from agentscope_extension_nacos.nacos_service_manager import NacosServiceManager


async def creating_react_agent() -> None:
    """Create an A2A agent that connects to a remote agent from Nacos A2A Registry."""

    # Configure Nacos connection
    client_config = (
        ClientConfigBuilder()
        .server_address("localhost:8848")
        .namespace_id("public")
        .log_level("DEBUG")  # Set to DEBUG level for detailed logs
        .build()
    )

    # Set as global configuration
    NacosServiceManager.set_global_config(client_config)

    # Create Nacos Agent Card resolver
    # This will fetch the Agent Card from Nacos A2A Registry
    agent_card_resolver = NacosA2ACardResolver(
        remote_agent_name="Friday"
    )

    # Create A2A agent with Nacos resolver
    # The agent will automatically discover and connect to the remote agent
    jarvis = A2aAgent(agent_card_resolver=agent_card_resolver)

    # Custom user input handler that runs in thread pool to avoid blocking
    class ThreadedTerminalInput(UserInputBase):
        """Run input() in a thread pool to avoid blocking the event loop."""

        def __init__(self, input_hint: str = "User Input: ") -> None:
            self.input_hint = input_hint

        async def __call__(
            self, agent_id: str, agent_name: str, structured_model=None, *args, **kwargs
        ):
            loop = asyncio.get_event_loop()
            text_input = await loop.run_in_executor(None, input, self.input_hint)
            return UserInputData(
                blocks_input=[TextBlock(type="text", text=text_input)],
                structured_input=None,
            )

    # Create user agent with custom input handler
    user = UserAgent(name="user")
    user.override_instance_input_method(ThreadedTerminalInput())

    # Start conversation loop
    msg = None
    msg = await user(msg)

    while True:
        msg = await jarvis(msg)
        msg = await user(msg)
        if msg.get_text_content() == "exit":
            break


if __name__ == "__main__":
    asyncio.run(creating_react_agent())
