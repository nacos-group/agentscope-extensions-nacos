# AgentScope Extensions Nacos

[English](./README.md) | 简体中文

为 [AgentScope](https://github.com/modelscope/agentscope) 框架提供 Nacos 集成能力的扩展组件，支持动态配置管理、MCP 工具集成和 A2A 智能体通信。

## ✨ 核心特性

- 🔄 **动态配置管理**：支持将 Agent 所需的配置（提示词、模型配置、工具列表等）托管至 Nacos，实现集中管理和实时热更新，无需重启应用
- 🛠️ **MCP 工具集成**：自动发现和注册 Nacos MCP Registry 中的工具服务器，工具列表动态更新
- 🤝 **A2A 智能体通信**：支持标准 A2A 协议，实现智能体间的互联互通
- 🎯 **多模型支持**：支持 OpenAI、Anthropic、Ollama、Google Gemini、阿里云通义千问等多种模型

## 📋 前置要求

- Python >= 3.7
- [AgentScope](https://github.com/modelscope/agentscope) >= 0.1.0
- Nacos Server >= 3.1.0
- [Nacos Python SDK](https://github.com/nacos-group/nacos-sdk-python) >= 3.0.0b1

## 📦 安装

> **注意**：本包尚未正式发布到 PyPI，敬请期待。

## 🔧 配置 Nacos 连接

在使用本扩展前，首先需要配置 Nacos 连接信息。

### 方式一：环境变量配置

```bash
# Nacos 服务器地址（必需）
export NACOS_SERVER_ADDRESS=localhost:8848

# Nacos 命名空间（必需）
export NACOS_NAMESPACE_ID=public

# 本地 Nacos 认证（可选）
export NACOS_USERNAME=nacos
export NACOS_PASSWORD=nacos

# 或使用阿里云 MSE 认证（可选）
export NACOS_ACCESS_KEY=your-access-key
export NACOS_SECRET_KEY=your-secret-key
```

### 方式二：代码配置

```python
from v2.nacos import ClientConfigBuilder
from agentscope_extension_nacos.nacos_service_manager import NacosServiceManager

# 配置 Nacos 连接
client_config = (ClientConfigBuilder()
    .server_address("localhost:8848")
    .namespace_id("public")
    .username("nacos")
    .password("nacos")
    .build())

# 设置为全局配置
NacosServiceManager.set_global_config(client_config)
```

---

## 🚀 使用场景

### 场景一：模型配置托管

将模型配置托管至 Nacos，实现模型的动态切换和参数调整。

#### 1. 在 Nacos 中创建模型配置

在 Nacos 控制台创建以下配置：

**Group**: `ai-agent-{agent_name}`（例如：`ai-agent-my-agent`）  
**DataId**: `model.json`  
**配置格式**: JSON

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

**支持的模型提供商**：
- `openai` - OpenAI GPT 系列
- `anthropic` - Anthropic Claude 系列
- `ollama` - Ollama 本地模型
- `gemini` - Google Gemini
- `dashscope` - 阿里云通义千问

#### 2. 在代码中使用

```python
import asyncio
from v2.nacos import ClientConfigBuilder
from agentscope_extension_nacos.nacos_service_manager import NacosServiceManager
from agentscope_extension_nacos.model.nacos_chat_model import NacosChatModel
from agentscope.agent import ReActAgent
from agentscope.formatter import OpenAIChatFormatter
from agentscope.memory import InMemoryMemory

async def main():
    # 1. 配置 Nacos 连接
    client_config = (ClientConfigBuilder()
        .server_address("localhost:8848")
        .namespace_id("public")
        .username("nacos")
        .password("nacos")
        .build())
    NacosServiceManager.set_global_config(client_config)
    
    # 2. 创建 Nacos 管理的模型
    model = NacosChatModel(
        agent_name="my-agent",  # 对应 Nacos 中的配置
        stream=True
    )
    
    # 3. 在智能体中使用
    agent = ReActAgent(
        name="MyAgent",
        sys_prompt="你是一个AI助手",
        model=model,
        formatter=OpenAIChatFormatter(),
        memory=InMemoryMemory()
    )
    
    # 4. 使用智能体
    from agentscope.message import Msg
    response = await agent(Msg(
        name="user",
        content="你好",
        role="user"
    ))
    print(response.content)
    
    # 5. 清理资源
    await NacosServiceManager.cleanup()

if __name__ == "__main__":
    asyncio.run(main())
```

#### 3. 动态更新模型配置

在 Nacos 控制台修改 `model.json` 配置后，智能体会自动切换到新的模型，无需重启应用。

---

### 场景二：完整智能体托管（推荐）

将智能体的所有配置（提示词、模型、工具）都托管至 Nacos，实现统一管理。

#### 1. 在 Nacos 中创建配置

**配置一：提示词配置**

**Group**: `ai-agent-{agent_name}`（例如：`ai-agent-my-agent`）  
**DataId**: `prompt.json`  
**配置格式**: JSON

```json
{
  "prompt": "你是一个有帮助的AI助手，可以回答各种问题。"
}
```

**配置二：模型配置**

**Group**: `ai-agent-{agent_name}`  
**DataId**: `model.json`  
**配置格式**: JSON

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

**配置三：MCP 服务器配置（可选）**

**Group**: `ai-agent-{agent_name}`  
**DataId**: `mcp-server.json`  
**配置格式**: JSON

```json
{
  "mcpServers": [
    {"mcpServerName": "weather-tools"},
    {"mcpServerName": "calculator-tools"}
  ]
}
```

> **说明**：MCP 服务器需要先在 Nacos MCP Registry 中注册。

#### 2. 在代码中使用

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
    # 1. 配置 Nacos 连接
    client_config = (ClientConfigBuilder()
        .server_address("localhost:8848")
        .namespace_id("public")
        .username("nacos")
        .password("nacos")
        .build())
    NacosServiceManager.set_global_config(client_config)
    
    # 2. 创建智能体监听器
    listener = NacosAgentListener(agent_name="my-agent")
    await listener.initialize()
    
    # 3. 创建完全由 Nacos 管理的智能体
    agent = NacosReActAgent(
        nacos_agent_listener=listener,
        name="MyAgent"
    )
    
    # 4. 与智能体对话
    response = await agent(Msg(
        name="user",
        content="你好，请介绍一下你自己",
        role="user"
    ))
    print(response.content)
    
    # 5. 清理资源
    await NacosServiceManager.cleanup()

if __name__ == "__main__":
    asyncio.run(main())
```

#### 3. 托管现有智能体

如果已有 AgentScope 智能体，可以将其托管到 Nacos：

```python
import asyncio
from agentscope.agent import ReActAgent
from agentscope.model import OpenAIChatModel
from agentscope.formatter import OpenAIChatFormatter
from agentscope.memory import InMemoryMemory
from agentscope_extension_nacos.nacos_react_agent import NacosAgentListener

async def main():
    # 1. 创建普通 AgentScope 智能体
    agent = ReActAgent(
        name="MyAgent",
        sys_prompt="你是一个AI助手",
        model=OpenAIChatModel(
            model_name="gpt-3.5-turbo",
            api_key="sk-xxx"
        ),
        formatter=OpenAIChatFormatter(),
        memory=InMemoryMemory()
    )
    
    # 2. 创建 Nacos 监听器
    listener = NacosAgentListener(agent_name="my-agent")
    await listener.initialize()
    
    # 3. 将智能体附加到监听器
    listener.attach_agent(agent)
    
    # 现在智能体的配置将由 Nacos 管理
    # 配置变更会自动生效
    
    # 使用智能体...
    
    # 4. 分离智能体（恢复原始配置）
    listener.detach_agent()

if __name__ == "__main__":
    asyncio.run(main())
```

#### 4. 配置热更新

在 Nacos 控制台修改配置后，智能体会自动应用新配置：

- **提示词更新**：修改 `prompt.json`，智能体立即使用新提示词
- **模型切换**：修改 `model.json`，智能体自动切换到新模型
- **工具更新**：修改 `mcp-server.json`，工具列表自动同步

---

### 场景三：MCP 工具集成

从 Nacos MCP Registry 中发现和使用 MCP 工具服务器。

#### 1. 确保 MCP 服务器已注册

MCP 服务器需要先在 Nacos MCP Registry 中注册。注册后，可以在代码中直接使用。

#### 2. 在代码中使用 MCP 工具

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
    # 1. 配置 Nacos 连接
    client_config = (ClientConfigBuilder()
        .server_address("localhost:8848")
        .namespace_id("public")
        .username("nacos")
        .password("nacos")
        .build())
    NacosServiceManager.set_global_config(client_config)
    
    # 2. 创建 MCP 客户端
    # 无状态客户端（适合低频调用）
    stateless_client = NacosHttpStatelessClient(
        nacos_client_config=None,  # 使用全局配置
        name="weather-tools"  # MCP 服务器名称
    )
    
    # 有状态客户端（适合高频调用）
    stateful_client = NacosHttpStatefulClient(
        nacos_client_config=None,
        name="calculator-tools"
    )
    
    # 3. 创建动态工具包
    toolkit = DynamicToolkit()
    
    # 4. 注册 MCP 客户端
    await stateful_client.connect()
    await toolkit.register_mcp_client(stateless_client)
    await toolkit.register_mcp_client(stateful_client)
    
    # 5. 在智能体中使用工具包
    agent = ReActAgent(
        name="ToolAgent",
        sys_prompt="你是一个可以使用工具的AI助手",
        model=OpenAIChatModel(
            model_name="gpt-4",
            api_key="sk-xxx"
        ),
        toolkit=toolkit
    )
    
    # 工具会自动同步 Nacos 的配置变更
    # 无需手动刷新
    
    # 6. 清理资源
    await stateful_client.close()
    await NacosServiceManager.cleanup()

if __name__ == "__main__":
    asyncio.run(main())
```

#### 3. 动态工具更新

当 MCP 服务器的工具配置在 Nacos 中更新时，`DynamicToolkit` 会自动同步工具列表，智能体可以立即使用新的工具。

---

### 场景四：A2A 智能体通信

支持两种方式使用 A2A 协议：
1. **消费端**：连接并使用远程 A2A 智能体
2. **服务端**：将本地智能体部署为 A2A 服务并注册到 Nacos

#### 1. 从 URL 连接远程智能体

```python
import asyncio
from agentscope_extension_nacos.a2a.a2a_agent import A2aAgent
from agentscope.message import Msg

async def main():
    # 1. 从 Agent Card URL 创建 A2A 智能体
    remote_agent = A2aAgent(
        agent_card_source="https://example.com/.well-known/agent.json"
    )
    
    # 2. 与远程智能体对话
    response = await remote_agent.reply(Msg(
        name="user",
        content="Hello, how are you?",
        role="user"
    ))
    print(response.content)
    
    # 3. 多轮对话（自动管理会话状态）
    response2 = await remote_agent.reply(Msg(
        name="user",
        content="What can you do?",
        role="user"
    ))
    print(response2.content)

if __name__ == "__main__":
    asyncio.run(main())
```

#### 2. 从 Nacos A2A Registry 获取智能体

```python
import asyncio
from v2.nacos import ClientConfigBuilder
from agentscope_extension_nacos.nacos_service_manager import NacosServiceManager
from agentscope_extension_nacos.a2a.nacos.nacos_a2a_card_resolver import (
    NacosA2ACardResolver
)
from agentscope_extension_nacos.a2a.a2a_agent import A2aAgent

async def main():
    # 1. 配置 Nacos 连接
    client_config = (ClientConfigBuilder()
        .server_address("localhost:8848")
        .namespace_id("public")
        .username("nacos")
        .password("nacos")
        .build())
    NacosServiceManager.set_global_config(client_config)
    
    # 2. 创建 Nacos Agent Card 解析器
    resolver = NacosA2ACardResolver(
        remote_agent_name="test-agent"
    )
    
    # 3. 创建 A2A 智能体
    agent = A2aAgent(
        agent_card_source=None,
        agent_card_resolver=resolver
    )
    
    # 4. 使用智能体
    from agentscope.message import Msg
    response = await agent.reply(Msg(
        name="user",
        content="Hello!",
        role="user"
    ))
    print(response.content)
    
    # 5. 清理资源
    await NacosServiceManager.cleanup()

if __name__ == "__main__":
    asyncio.run(main())
```

#### 3. 部署 Agent 为 A2A 服务

使用 AgentScope Runtime 将智能体部署为 A2A 服务，并自动注册到 Nacos A2A Registry。

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
    # 1. 配置 Nacos 连接
    client_config = (ClientConfigBuilder()
        .server_address("localhost:8848")
        .namespace_id("public")
        .username("nacos")
        .password("nacos")
        .build())
    
    # 2. 创建 AgentScope Agent
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
    
    # 3. 创建 Runner
    async with Runner(
        agent=agent,
        context_manager=ContextManager()
    ) as runner:
        # 4. 创建部署管理器
        deploy_manager = LocalDeployManager(
            host="localhost",
            port=8090
        )
        
        # 5. 创建 A2A Nacos 适配器
        # 这会将 Agent 以 A2A 协议暴露并注册到 Nacos
        nacos_a2a_adapter = A2AFastAPINacosAdaptor(
            nacos_client_config=client_config,
            agent=agent,
            host="localhost"
        )
        
        # 6. 部署 Agent
        deploy_result = await runner.deploy(
            deploy_manager=deploy_manager,
            endpoint_path="/process",
            protocol_adapters=[nacos_a2a_adapter],  # 使用 A2A 适配器
            stream=True
        )
        
        print(f"🚀 智能体部署成功: {deploy_result}")
        print(f"🌐 服务 URL: {deploy_manager.service_url}")
        print(f"💚 健康检查: {deploy_manager.service_url}/health")
        print(f"📝 Agent 已注册到 Nacos A2A Registry")
        
        # 保持服务运行
        await asyncio.sleep(3600)

if __name__ == "__main__":
    asyncio.run(main())
```

**部署后的效果**：
- ✅ Agent 通过 FastAPI 以 A2A 协议对外提供服务
- ✅ Agent Card 自动注册到 Nacos A2A Registry
- ✅ 其他客户端可以通过 Nacos 发现并连接此 Agent
- ✅ 支持流式响应和完整的 A2A 协议特性

**其他客户端访问**：
部署成功后，其他客户端可以通过场景四中的方式 2（从 Nacos A2A Registry 获取）来发现和使用这个 Agent。

---

## 📚 更多示例

查看 [`example/`](./example/) 目录获取更多完整示例：

- [`agent_example.py`](./example/agent_example.py) - 基本智能体创建和使用
- [`model_example.py`](./example/model_example.py) - 模型配置和动态切换
- [`mcp_example.py`](./example/mcp_example.py) - MCP 工具集成示例
- [`runtime_example.py`](./example/runtime_example.py) - AgentScope Runtime 部署
- [`a2a/nacos_a2a_example.py`](./example/a2a/nacos_a2a_example.py) - 从 Nacos 连接 A2A 智能体
- [`a2a/runtime_nacos_a2a_example.py`](./example/a2a/runtime_nacos_a2a_example.py) - 部署 Agent 为 A2A 服务

## ⚙️ 高级配置

### NacosAgentListener 选项

选择性监听某些配置：

```python
from agentscope_extension_nacos.nacos_react_agent import NacosAgentListener

# 只监听提示词和模型，不监听 MCP 服务器配置
listener = NacosAgentListener(
    agent_name="my-agent",
    nacos_client_config=None,  # 使用全局配置
    listen_prompt=True,        # 监听提示词配置
    listen_chat_model=True,    # 监听模型配置
    listen_mcp_server=False    # 不监听 MCP 服务器配置
)
```

### NacosChatModel 备用模型

配置备用模型，当主模型失败时自动降级：

```python
from agentscope_extension_nacos.model.nacos_chat_model import NacosChatModel
from agentscope.model import OpenAIChatModel

# 创建备用模型
backup_model = OpenAIChatModel(
    model_name="gpt-3.5-turbo",
    api_key="sk-xxx"
)

# 创建 Nacos 模型（带备用）
model = NacosChatModel(
    agent_name="my-agent",
    nacos_client_config=None,
    stream=True,
    backup_model=backup_model  # 主模型失败时使用备用模型
)
```

### 自定义 Nacos 配置

为不同组件使用不同的 Nacos 配置：

```python
from v2.nacos import ClientConfigBuilder

# 为特定组件创建独立配置
custom_config = (ClientConfigBuilder()
    .server_address("another-nacos:8848")
    .namespace_id("test")
    .username("nacos")
    .password("nacos")
    .build())

# 使用自定义配置
listener = NacosAgentListener(
    agent_name="my-agent",
    nacos_client_config=custom_config  # 使用自定义配置
)
```

## ❓ 常见问题

<details>
<summary><b>Q: 如何验证 Nacos 连接是否成功？</b></summary>

检查日志输出，应该看到类似以下信息：
```
INFO - [NacosServiceManager] Loaded Nacos config from env (basic auth): localhost:8848
INFO - [NacosServiceManager] NacosServiceManager initialized (singleton)
```

或者在代码中验证：
```python
manager = NacosServiceManager()
assert manager.is_initialized()
```
</details>

<details>
<summary><b>Q: 配置更新后智能体没有响应？</b></summary>

1. 检查 Nacos 配置的 Group 和 DataId 是否正确
2. 检查配置 JSON 格式是否正确
3. 查看日志是否有错误信息
4. 确认 `NacosAgentListener` 已正确初始化和附加
</details>

<details>
<summary><b>Q: MCP 工具不可用？</b></summary>

1. 确认 MCP 服务器已在 Nacos MCP Registry 中注册
2. 检查 MCP 服务器是否正常运行
3. 验证网络连接是否正常
4. 查看 MCP 客户端日志
</details>

<details>
<summary><b>Q: 如何切换不同的模型提供商？</b></summary>

在 Nacos 中修改 `model.json` 配置：
```json
{
  "modelProvider": "openai",  // 或 "anthropic", "ollama", "gemini", "dashscope"
  "modelName": "gpt-4",
  "apiKey": "sk-xxx"
}
```
配置会自动生效，智能体会使用新的模型提供商。
</details>

<details>
<summary><b>Q: agent_name 有什么命名规范？</b></summary>

agent_name 用于在 Nacos 中标识配置组，命名规范：
- 只能包含字母、数字、`.`、`:`、`_`、`-`
- 最大长度 128 字符
- 空格会自动替换为下划线
- 配置 Group 格式为：`ai-agent-{agent_name}`
</details>

<details>
<summary><b>Q: A2A 服务端和客户端如何协作？</b></summary>

**服务端（Agent 提供者）**：
1. 使用 `A2AFastAPINacosAdaptor` 将 Agent 部署为 A2A 服务
2. Agent Card 自动注册到 Nacos A2A Registry
3. 对外提供 A2A 协议接口

**客户端（Agent 消费者）**：
1. 使用 `NacosA2ACardResolver` 从 Nacos 获取 Agent Card
2. 通过 `A2aAgent` 连接并使用远程 Agent
3. 自动管理会话状态

整个流程实现了智能体的服务化和互联互通。
</details>

## 🤝 社区与支持

- **问题反馈**：[GitHub Issues](https://github.com/nacos-group/agentscope-extensions-nacos/issues)
- **讨论交流**：[GitHub Discussions](https://github.com/nacos-group/agentscope-extensions-nacos/discussions)
- **AgentScope 文档**：https://github.com/modelscope/agentscope
- **Nacos 文档**：https://nacos.io/docs/

## 📄 许可证

本项目基于 [Apache License 2.0](./LICENSE) 开源。

## 🙏 致谢

感谢以下项目和社区的支持：
- [AgentScope](https://github.com/modelscope/agentscope) - 强大的多智能体框架
- [Nacos](https://nacos.io/) - 动态服务发现和配置管理平台
- [MCP Protocol](https://modelcontextprotocol.io/) - 模型上下文协议
- [A2A Protocol](https://a2a.dev/) - 智能体间通信协议

---

**如果这个项目对您有帮助，请给我们一个 ⭐️ Star！**

