# AgentScope Extensions Nacos

English | [简体中文](./README_CN.md)

An extension component for the [AgentScope](https://github.com/modelscope/agentscope) framework that provides Nacos integration capabilities, supporting dynamic configuration management, MCP tool integration, and A2A agent communication.

## ✨ Key Features

- 🔄 **Dynamic Configuration Management**: Host agent configurations (prompts, model configs, tool lists, etc.) in Nacos for centralized management and real-time hot updates without restarting the application
- 🛠️ **MCP Tool Integration**: Automatically discover and register tool servers from the Nacos MCP Registry with dynamic tool list updates
- 🤝 **A2A Agent Communication**: Support standard A2A protocol for agent-to-agent interconnection
- 🎯 **Multi-Model Support**: Support for OpenAI, Anthropic, Ollama, Google Gemini, Alibaba Cloud Qwen, and more

## 📋 Prerequisites

- Python >= 3.9
- [AgentScope](https://github.com/modelscope/agentscope) >= 1.0.7
- Nacos Server >= 3.1.0
- [Nacos Python SDK](https://github.com/nacos-group/nacos-sdk-python) >= 3.0.0b1

## 📦 Installation

```bash
pip install agentscope-extension-nacos
```

Or install from source:

```bash
git clone https://github.com/nacos-group/agentscope-extensions-nacos.git
cd agentscope-extensions-nacos/python
pip install -e .
```

## 🔧 Configuring Nacos Connection

Before using this extension, you need to configure the Nacos connection information.

### Method 1: Environment Variables

```bash
# Nacos server address (required)
export NACOS_SERVER_ADDRESS=localhost:8848

# Nacos namespace (required)
export NACOS_NAMESPACE_ID=public

# Local Nacos authentication (optional)
export NACOS_USERNAME=nacos
export NACOS_PASSWORD=nacos

# Or use Alibaba Cloud MSE authentication (optional)
export NACOS_ACCESS_KEY=your-access-key
export NACOS_SECRET_KEY=your-secret-key
```

### Method 2: Code Configuration

```python
from v2.nacos import ClientConfigBuilder
from agentscope_extension_nacos.nacos_service_manager import NacosServiceManager

# Configure Nacos connection
client_config = (ClientConfigBuilder()
    .server_address("localhost:8848")
    .namespace_id("public")
    .username("nacos")
    .password("nacos")
    .build())

# Set as global configuration
NacosServiceManager.set_global_config(client_config)
```

---

## 🚀 Usage Scenarios

### Scenario 1: Model Configuration Hosting

Host model configuration in Nacos to enable dynamic model switching and parameter adjustment.

#### 1. Create Model Configuration in Nacos

Create the following configuration in the Nacos console:

**Group**: `ai-agent-{agent_name}` (e.g., `ai-agent-my-agent`)  
**DataId**: `model.json`  
**Format**: JSON

```json
{
  "modelName": "qwen-max",
  "modelProvider": "dashscope",
  "apiKey": "sk-your-api-key",
  "baseUrl": "https://dashscope.aliyuncs.com/compatible-mode/v1",
  "args": {
    "temperature": 0.7,
    "max_tokens": 2000
  }
}
```

**Supported Model Providers**:
- `openai` - OpenAI GPT series
- `anthropic` - Anthropic Claude series
- `ollama` - Ollama local models
- `gemini` - Google Gemini
- `dashscope` - Alibaba Cloud Qwen

#### 2. Use in Code

```python
import asyncio
from v2.nacos import ClientConfigBuilder
from agentscope_extension_nacos.nacos_service_manager import NacosServiceManager
from agentscope_extension_nacos.model.nacos_chat_model import NacosChatModel
from agentscope.agent import ReActAgent
from agentscope.formatter import OpenAIChatFormatter
from agentscope.memory import InMemoryMemory

async def main():
    # 1. Configure Nacos connection
    client_config = (ClientConfigBuilder()
        .server_address("localhost:8848")
        .namespace_id("public")
        .username("nacos")
        .password("nacos")
        .build())
    NacosServiceManager.set_global_config(client_config)
    
    # 2. Create Nacos-managed model
    model = NacosChatModel(
        agent_name="my-agent",  # Corresponds to the configuration in Nacos
        stream=True
    )
    
    # 3. Use in agent
    agent = ReActAgent(
        name="MyAgent",
        sys_prompt="You are an AI assistant",
        model=model,
        formatter=OpenAIChatFormatter(),
        memory=InMemoryMemory()
    )
    
    # 4. Use the agent
    from agentscope.message import Msg
    response = await agent(Msg(
        name="user",
        content="Hello",
        role="user"
    ))
    print(response.content)
    
    # 5. Cleanup resources
    await NacosServiceManager.cleanup()

if __name__ == "__main__":
    asyncio.run(main())
```

#### 3. Dynamic Model Configuration Updates

After modifying the `model.json` configuration in the Nacos console, the agent will automatically switch to the new model without restarting the application.

---

### Scenario 2: Complete Agent Hosting (Recommended)

Host all agent configurations (prompts, models, tools) in Nacos for unified management.

#### 1. Create Configurations in Nacos

**Configuration 1: Prompt Configuration**

**Group**: `ai-agent-{agent_name}` (e.g., `ai-agent-my-agent`)  
**DataId**: `prompt.json`  
**Format**: JSON

You can directly fill in the desired Prompt content in prompt.json:

```json
{
  "prompt": "You are a helpful AI assistant that can answer various questions."
}
```

Or reference an already created Prompt in the MSE Nacos Prompt Management module:

```json
{
  "promptRef": "{promptKey}.json"
}
```

Where `promptKey` is the corresponding Prompt name in the MSE Nacos Prompt Management module.

**Configuration 2: Model Configuration**

**Group**: `ai-agent-{agent_name}`  
**DataId**: `model.json`  
**Format**: JSON

```json
{
  "modelName": "qwen-max",
  "modelProvider": "dashscope",
  "apiKey": "sk-your-api-key",
  "baseUrl": "https://dashscope.aliyuncs.com/compatible-mode/v1",
  "args": {
    "temperature": 0.7,
    "max_tokens": 2000
  }
}
```

**Configuration 3: MCP Server Configuration (Optional)**

**Group**: `ai-agent-{agent_name}`  
**DataId**: `mcp-server.json`  
**Format**: JSON

```json
{
  "mcpServers": [
    {"mcpServerName": "weather-tools"},
    {"mcpServerName": "calculator-tools"}
  ]
}
```

> **Note**: MCP servers must be registered in the Nacos MCP Registry first.

#### 2. Use in Code

```python
import asyncio
from v2.nacos import ClientConfigBuilder
from agentscope_extension_nacos.nacos_service_manager import NacosServiceManager
from agentscope_extension_nacos.nacos_react_agent import (
    NacosAgentListener,
    NacosReActAgent
)
from agentscope.message import Msg

async def main():
    # 1. Configure Nacos connection
    client_config = (ClientConfigBuilder()
        .server_address("localhost:8848")
        .namespace_id("public")
        .username("nacos")
        .password("nacos")
        .build())
    NacosServiceManager.set_global_config(client_config)
    
    # 2. Create agent listener
    listener = NacosAgentListener(agent_name="my-agent")
    await listener.initialize()
    
    # 3. Create fully Nacos-managed agent
    agent = NacosReActAgent(
        nacos_agent_listener=listener,
        name="MyAgent"
    )
    
    # 4. Chat with the agent
    response = await agent(Msg(
        name="user",
        content="Hello, please introduce yourself",
        role="user"
    ))
    print(response.content)
    
    # 5. Cleanup resources
    await NacosServiceManager.cleanup()

if __name__ == "__main__":
    asyncio.run(main())
```

#### 3. Host Existing Agent

If you already have an AgentScope agent, you can host it in Nacos:

```python
import asyncio
from agentscope.agent import ReActAgent
from agentscope.model import OpenAIChatModel
from agentscope.formatter import OpenAIChatFormatter
from agentscope.memory import InMemoryMemory
from agentscope_extension_nacos.nacos_react_agent import NacosAgentListener

async def main():
    # 1. Create regular AgentScope agent
    agent = ReActAgent(
        name="MyAgent",
        sys_prompt="You are an AI assistant",
        model=OpenAIChatModel(
            model_name="gpt-3.5-turbo",
            api_key="sk-xxx"
        ),
        formatter=OpenAIChatFormatter(),
        memory=InMemoryMemory()
    )
    
    # 2. Create Nacos listener
    listener = NacosAgentListener(agent_name="my-agent")
    await listener.initialize()
    
    # 3. Attach agent to listener
    listener.attach_agent(agent)
    
    # Now the agent's configuration will be managed by Nacos
    # Configuration changes will automatically take effect
    
    # Use the agent...
    
    # 4. Detach agent (restore original configuration)
    listener.detach_agent()

if __name__ == "__main__":
    asyncio.run(main())
```

#### 4. Configuration Hot Updates

After modifying configurations in the Nacos console, the agent will automatically apply the new configuration:

- **Prompt Update**: Modify `prompt.json`, agent immediately uses the new prompt
- **Model Switch**: Modify `model.json`, agent automatically switches to the new model
- **Tool Update**: Modify `mcp-server.json`, tool list automatically syncs

---

### Scenario 3: MCP Tool Integration

Discover and use MCP tool servers from the Nacos MCP Registry.

#### 1. Ensure MCP Server is Registered

MCP servers must be registered in the Nacos MCP Registry first. After registration, they can be used directly in code.

#### 2. Use MCP Tools in Code

```python
import asyncio
from v2.nacos import ClientConfigBuilder
from agentscope_extension_nacos.nacos_service_manager import NacosServiceManager
from agentscope_extension_nacos.mcp.agentscope_nacos_mcp import (
    NacosHttpStatelessClient,
    NacosHttpStatefulClient
)
from agentscope_extension_nacos.mcp.agentscope_dynamic_toolkit import DynamicToolkit
from agentscope.agent import ReActAgent
from agentscope.model import OpenAIChatModel

async def main():
    # 1. Configure Nacos connection
    client_config = (ClientConfigBuilder()
        .server_address("localhost:8848")
        .namespace_id("public")
        .username("nacos")
        .password("nacos")
        .build())
    NacosServiceManager.set_global_config(client_config)
    
    # 2. Create MCP clients
    # Stateless client (suitable for low-frequency calls)
    stateless_client = NacosHttpStatelessClient(
        nacos_client_config=None,  # Use global configuration
        name="weather-tools"  # MCP server name
    )
    
    # Stateful client (suitable for high-frequency calls)
    stateful_client = NacosHttpStatefulClient(
        nacos_client_config=None,
        name="calculator-tools"
    )
    
    # 3. Create dynamic toolkit
    toolkit = DynamicToolkit()
    
    # 4. Register MCP clients
    await stateful_client.connect()
    await toolkit.register_mcp_client(stateless_client)
    await toolkit.register_mcp_client(stateful_client)
    
    # 5. Use toolkit in agent
    agent = ReActAgent(
        name="ToolAgent",
        sys_prompt="You are an AI assistant that can use tools",
        model=OpenAIChatModel(
            model_name="gpt-4",
            api_key="sk-xxx"
        ),
        toolkit=toolkit
    )
    
    # Tools will automatically sync with Nacos configuration changes
    # No manual refresh needed
    
    # 6. Cleanup resources
    await stateful_client.close()
    await NacosServiceManager.cleanup()

if __name__ == "__main__":
    asyncio.run(main())
```

#### 3. Dynamic Tool Updates

When MCP server tool configurations are updated in Nacos, `DynamicToolkit` will automatically sync the tool list, and the agent can immediately use the new tools.

---

### Scenario 4: A2A Agent Communication

Support two ways to use the A2A protocol:
1. **Consumer**: Connect and use remote A2A agents
2. **Provider**: Deploy local agents as A2A services and register them in Nacos

#### 1. Connect to Remote Agent from URL

```python
import asyncio
from agentscope_extension_nacos.a2a.a2a_agent import A2aAgent
from agentscope.message import Msg

async def main():
    # 1. Create A2A agent from Agent Card URL
    remote_agent = A2aAgent(
        agent_card_source="https://example.com/.well-known/agent.json"
    )
    
    # 2. Chat with remote agent
    response = await remote_agent.reply(Msg(
        name="user",
        content="Hello, how are you?",
        role="user"
    ))
    print(response.content)
    
    # 3. Multi-turn conversation (automatic session state management)
    response2 = await remote_agent.reply(Msg(
        name="user",
        content="What can you do?",
        role="user"
    ))
    print(response2.content)

if __name__ == "__main__":
    asyncio.run(main())
```

#### 2. Get Agent from Nacos A2A Registry

```python
import asyncio
from v2.nacos import ClientConfigBuilder
from agentscope_extension_nacos.nacos_service_manager import NacosServiceManager
from agentscope_extension_nacos.a2a.nacos.nacos_a2a_card_resolver import (
    NacosA2ACardResolver
)
from agentscope_extension_nacos.a2a.a2a_agent import A2aAgent

async def main():
    # 1. Configure Nacos connection
    client_config = (ClientConfigBuilder()
        .server_address("localhost:8848")
        .namespace_id("public")
        .username("nacos")
        .password("nacos")
        .build())
    NacosServiceManager.set_global_config(client_config)
    
    # 2. Create Nacos Agent Card resolver
    resolver = NacosA2ACardResolver(
        remote_agent_name="test-agent"
    )
    
    # 3. Create A2A agent
    agent = A2aAgent(
        agent_card_source=None,
        agent_card_resolver=resolver
    )
    
    # 4. Use the agent
    from agentscope.message import Msg
    response = await agent.reply(Msg(
        name="user",
        content="Hello!",
        role="user"
    ))
    print(response.content)
    
    # 5. Cleanup resources
    await NacosServiceManager.cleanup()

if __name__ == "__main__":
    asyncio.run(main())
```

#### 3. Deploy Agent as A2A Service

Use AgentScope Runtime to deploy an agent as an A2A service and automatically register it to the Nacos A2A Registry.

```python
import asyncio
import os
from contextlib import asynccontextmanager
from agentscope.agent import ReActAgent
from agentscope.model import OpenAIChatModel
from agentscope_runtime.engine import Runner, LocalDeployManager
from agentscope_runtime.engine.agents.agentscope_agent import AgentScopeAgent
from agentscope_runtime.engine.services.context_manager import ContextManager
from v2.nacos import ClientConfigBuilder
from agentscope_extension_nacos.a2a.nacos.nacos_a2a_adapter import (
    A2AFastAPINacosAdaptor
)

async def main():
    # 1. Configure Nacos connection
    client_config = (ClientConfigBuilder()
        .server_address("localhost:8848")
        .namespace_id("public")
        .username("nacos")
        .password("nacos")
        .build())
    
    # 2. Create AgentScope Agent
    agent = AgentScopeAgent(
        name="Friday",
        model=OpenAIChatModel(
            model_name="gpt-4",
            api_key=os.getenv("OPENAI_API_KEY")
        ),
        agent_config={
            "sys_prompt": "You're a helpful assistant named Friday."
        },
        agent_builder=ReActAgent
    )
    
    # 3. Create Runner
    async with Runner(
        agent=agent,
        context_manager=ContextManager()
    ) as runner:
        # 4. Create deployment manager
        deploy_manager = LocalDeployManager(
            host="localhost",
            port=8090
        )
        
        # 5. Create A2A Nacos adapter
        # This exposes the Agent via A2A protocol and registers it to Nacos
        nacos_a2a_adapter = A2AFastAPINacosAdaptor(
            nacos_client_config=client_config,
            agent=agent,
            host="localhost"
        )
        
        # 6. Deploy Agent
        deploy_result = await runner.deploy(
            deploy_manager=deploy_manager,
            endpoint_path="/process",
            protocol_adapters=[nacos_a2a_adapter],  # Use A2A adapter
            stream=True
        )
        
        print(f"🚀 Agent deployed successfully: {deploy_result}")
        print(f"🌐 Service URL: {deploy_manager.service_url}")
        print(f"💚 Health check: {deploy_manager.service_url}/health")
        print(f"📝 Agent registered to Nacos A2A Registry")
        
        # Keep service running
        await asyncio.sleep(3600)

if __name__ == "__main__":
    asyncio.run(main())
```

**Deployment Effects**:
- ✅ Agent serves externally via FastAPI with A2A protocol
- ✅ Agent Card automatically registered to Nacos A2A Registry
- ✅ Other clients can discover and connect to this Agent via Nacos
- ✅ Supports streaming responses and full A2A protocol features

**Client Access**:
After successful deployment, other clients can discover and use this Agent via Method 2 in Scenario 4 (from Nacos A2A Registry).

---

## 📚 More Examples

Check the [`example/`](./example/) directory for more complete examples:

- [`agent_example.py`](./example/agent_example.py) - Basic agent creation and usage
- [`model_example.py`](./example/model_example.py) - Model configuration and dynamic switching
- [`mcp_example.py`](./example/mcp_example.py) - MCP tool integration example
- [`runtime_example.py`](./example/runtime_example.py) - AgentScope Runtime deployment
- [`a2a/nacos_a2a_example.py`](./example/a2a/nacos_a2a_example.py) - Connect to A2A agent from Nacos
- [`a2a/runtime_nacos_a2a_example.py`](./example/a2a/runtime_nacos_a2a_example.py) - Deploy Agent as A2A service

## ⚙️ Advanced Configuration

### NacosAgentListener Options

Selectively listen to certain configurations:

```python
from agentscope_extension_nacos.nacos_react_agent import NacosAgentListener

# Only listen to prompt and model, not MCP server configuration
listener = NacosAgentListener(
    agent_name="my-agent",
    nacos_client_config=None,  # Use global configuration
    listen_prompt=True,        # Listen to prompt configuration
    listen_chat_model=True,    # Listen to model configuration
    listen_mcp_server=False    # Don't listen to MCP server configuration
)
```

### NacosChatModel Backup Model

Configure a backup model that automatically falls back when the primary model fails:

```python
from agentscope_extension_nacos.model.nacos_chat_model import NacosChatModel
from agentscope.model import OpenAIChatModel

# Create backup model
backup_model = OpenAIChatModel(
    model_name="gpt-3.5-turbo",
    api_key="sk-xxx"
)

# Create Nacos model (with backup)
model = NacosChatModel(
    agent_name="my-agent",
    nacos_client_config=None,
    stream=True,
    backup_model=backup_model  # Use backup model when primary fails
)
```

### Custom Nacos Configuration

Use different Nacos configurations for different components:

```python
from v2.nacos import ClientConfigBuilder

# Create independent configuration for specific components
custom_config = (ClientConfigBuilder()
    .server_address("another-nacos:8848")
    .namespace_id("test")
    .username("nacos")
    .password("nacos")
    .build())

# Use custom configuration
listener = NacosAgentListener(
    agent_name="my-agent",
    nacos_client_config=custom_config  # Use custom configuration
)
```

## ❓ FAQ

<details>
<summary><b>Q: How to verify successful Nacos connection?</b></summary>

Check the log output for messages like:
```
INFO - [NacosServiceManager] Loaded Nacos config from env (basic auth): localhost:8848
INFO - [NacosServiceManager] NacosServiceManager initialized (singleton)
```

Or verify in code:
```python
manager = NacosServiceManager()
assert manager.is_initialized()
```
</details>

<details>
<summary><b>Q: Agent not responding after configuration update?</b></summary>

1. Check if Nacos configuration Group and DataId are correct
2. Verify JSON configuration format is valid
3. Check logs for error messages
4. Confirm `NacosAgentListener` is properly initialized and attached
</details>

<details>
<summary><b>Q: MCP tools not available?</b></summary>

1. Confirm MCP server is registered in Nacos MCP Registry
2. Check if MCP server is running properly
3. Verify network connectivity
4. Check MCP client logs
</details>

<details>
<summary><b>Q: How to switch between different model providers?</b></summary>

Modify `model.json` configuration in Nacos:
```json
{
  "modelProvider": "openai",  // or "anthropic", "ollama", "gemini", "dashscope"
  "modelName": "gpt-4",
  "apiKey": "sk-xxx"
}
```
The configuration will automatically take effect, and the agent will use the new model provider.
</details>

<details>
<summary><b>Q: What are the naming conventions for agent_name?</b></summary>

agent_name is used to identify configuration groups in Nacos, with the following conventions:
- Only letters, numbers, `.`, `:`, `_`, `-` allowed
- Maximum length 128 characters
- Spaces automatically replaced with underscores
- Configuration Group format: `ai-agent-{agent_name}`
</details>

<details>
<summary><b>Q: How do A2A server and client collaborate?</b></summary>

**Server (Agent Provider)**:
1. Use `A2AFastAPINacosAdaptor` to deploy Agent as A2A service
2. Agent Card automatically registered to Nacos A2A Registry
3. Provide A2A protocol interface externally

**Client (Agent Consumer)**:
1. Use `NacosA2ACardResolver` to get Agent Card from Nacos
2. Connect and use remote Agent via `A2aAgent`
3. Automatically manage session state

The entire process enables agent servitization and interconnection.
</details>

## 🤝 Community & Support

- **Issue Reporting**: [GitHub Issues](https://github.com/nacos-group/agentscope-extensions-nacos/issues)
- **Discussions**: [GitHub Discussions](https://github.com/nacos-group/agentscope-extensions-nacos/discussions)
- **AgentScope Docs**: https://github.com/modelscope/agentscope
- **Nacos Docs**: https://nacos.io/docs/

## 📄 License

This project is open-sourced under the [Apache License 2.0](./LICENSE).

## 🙏 Acknowledgments

Thanks to the following projects and communities for their support:
- [AgentScope](https://github.com/modelscope/agentscope) - Powerful multi-agent framework
- [Nacos](https://nacos.io/) - Dynamic service discovery and configuration management platform
- [MCP Protocol](https://modelcontextprotocol.io/) - Model Context Protocol
- [A2A Protocol](https://a2a.dev/) - Agent-to-Agent communication protocol

---

**If this project helps you, please give us a ⭐️ Star!**
