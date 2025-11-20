# AgentScope Extensions Nacos - Python

Nacos extensions component for agentscope - Python

## Overview

The Python implementation of AgentScope Extensions for Nacos provides seamless integration between AgentScope and Nacos service discovery. It enables automatic registration and discovery of agents and tools using both A2A (Agent-to-Agent) and MCP (Model Context Protocol) protocols through Nacos.

## Key Features

- **A2A Protocol Support**: Enables agent-to-agent communication with automatic discovery through Nacos
- **MCP Protocol Integration**: Provides dynamic tool discovery and management via Nacos service registry
- **Nacos Service Discovery**: Automatic registration and lookup of agents and tools in Nacos
- **Reactive Agent Support**: Specialized agents that can dynamically adapt to configuration changes in Nacos
- **Multiple Transport Protocols**: Support for various communication protocols including HTTP/SSE and StdIO

## Installation

```bash
pip install -e .
```

## Modules

### A2A (Agent-to-Agent)

The A2A module enables communication between agents using the Agent-to-Agent protocol. It includes:

- `A2aAgent`: Main agent class that bridges AgentScope with A2A protocol-compliant agents
- `NacosA2ACardResolver`: Resolves agent cards from Nacos service registry with automatic updates
- `DefaultA2ACardResolver`: Resolves agent cards from URLs or local files

### MCP (Model Context Protocol)

The MCP module provides integration with the Model Context Protocol for dynamic tool management:

- `NacosMCPClientBase`: Base class for Nacos-based MCP clients
- `NacosHttpStatelessClient`: Stateless HTTP client for intermittent tool operations
- `NacosHttpStatefulClient`: Stateful HTTP client for persistent connections
- `NacosStdIOStatefulClient`: StdIO client for local tool execution
- `DynamicToolkit`: Toolkit that automatically synchronizes with MCP tools from Nacos

### Nacos React Agent

Specialized reactive agents that can dynamically adapt to configuration changes in Nacos:

- `NacosReActAgent`: ReAct agent that automatically uses configuration from Nacos
- `NacosAgentListener`: Listens for configuration changes and updates agent components

## Usage

### Basic A2A Agent Usage

```python
from agentscope_nacos.a2a.a2a_agent import A2aAgent, NacosA2ACardResolver

# Create an A2A agent that discovers remote agents through Nacos
resolver = NacosA2ACardResolver(remote_agent_name="test-agent")
agent = A2aAgent(agent_card=None, agent_card_resolver=resolver)

# Send a message to the remote agent
response = await agent.reply(message)
```

### MCP Tool Usage

```python
from agentscope_nacos.mcp.agentscope_nacos_mcp import NacosHttpStatelessClient
from agentscope.tool import Toolkit

# Create an MCP client that discovers tools through Nacos
mcp_client = NacosHttpStatelessClient(
    nacos_client_config=None,  # Uses global config
    name="my-mcp-server"
)

# Register the MCP client with a toolkit
toolkit = Toolkit()
await toolkit.register_mcp_client(mcp_client)
```

### Nacos React Agent Usage

```python
from agentscope_nacos.nacos_react_agent import NacosAgentListener, NacosReActAgent

# Create a listener that monitors Nacos for agent configuration changes
listener = NacosAgentListener(agent_name="my_agent")
await listener.initialize()

# Create a reactive agent that automatically adapts to Nacos configuration
agent = NacosReActAgent(nacos_agent_listener=listener, name="my_agent")

# Send a message - the agent will automatically use the latest config from Nacos
response = await agent(message)
```

## Examples

For detailed examples, see the [example directory](example/):

- [AgentScope Main Example](example/agent_scope_main.py)
- [MCP Server Example](example/mcp_server_example_and_model.py)
- [Test Runtime Example](example/test_runtime.py)

## Testing

To run tests:

```bash
python -m unittest discover -s tests
```