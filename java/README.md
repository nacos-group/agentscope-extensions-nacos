# AgentScope Extensions for Nacos - Java Implementation

This project provides integration between AgentScope and Nacos service discovery for Java-based applications. It enables automatic registration and discovery of agents using the A2A (Agent-to-Agent) and MCP (Model Context Protocol) protocols through Nacos.

## Project Structure

```
java/
├── agentscope-extensions-a2a                                   # Core A2A protocol implementation
├── agentscope-extensions-a2a-nacos                             # Nacos integration for A2A protocol
├── agentscope-extensions-mcp-nacos                             # Nacos integration for MCP protocol
├── example                                                     # Examples demonstrating usage
│   ├── a2a-example                                             # A2A protocol examples with Nacos
│   └── mcp-example                                             # MCP protocol examples with Nacos
└── spring                                                      # Spring Boot integration
    └── spring-boot-starter-agentscope-runtime-a2a-nacos        # Auto-configuration for Spring Boot
```

## Modules Overview

### agentscope-extensions-a2a

Core implementation of the A2A (Agent-to-Agent) protocol for AgentScope. This module provides the base classes for creating and managing A2A agents.

Key features:
- Implementation of AgentScope's AgentBase for A2A protocol
- Support for JSON-RPC transport
- Event handling for client interactions
- Task management and interruption handling

### agentscope-extensions-a2a-nacos

Nacos integration for A2A protocol. This module enables discovery of A2A agents registered in Nacos.

Key features:
- NacosAgentCardProducer: Retrieves agent cards from Nacos registry
- Automatic subscription to agent card updates
- Cache management for efficient agent discovery

### agentscope-extensions-mcp-nacos

Nacos integration for MCP (Model Context Protocol). This module enables discovery and management of MCP tools registered in Nacos.

Key features:
- NacosMcpServerManager: Manages MCP server discovery from Nacos
- NacosMcpClientWrapper: Wrapper for MCP clients with dynamic configuration
- Automatic refresh of MCP tools when server configurations change
- Support for multiple MCP protocol types

### spring-boot-starter-agentscope-runtime-a2a-nacos

Spring Boot starter that provides auto-configuration for A2A agent registration with Nacos.

Key features:
- Auto-configuration of Nacos registry properties
- Automatic agent registration on application startup
- Integration with Spring Boot's configuration system
- Conditional configuration based on application properties

## Examples

### A2A Protocol Examples

Located in [a2a-example](file:///Users/xiweng.yy/Documents/java/opensource/agentscope-extensions-nacos/java/example/a2a-example) directory:

1. **a2a-register-example**: Demonstrates how to register an agent with Nacos
2. **a2a-discovery-example**: Shows how to discover and communicate with agents registered in Nacos

### MCP Protocol Examples

Located in [mcp-example](file:///Users/xiweng.yy/Documents/java/opensource/agentscope-extensions-nacos/java/example/mcp-example) directory:

1. **mcp-discovery-example**: Demonstrates how to discover and use MCP tools registered with Nacos

## Building

To build the entire project:

```bash
cd java
mvn clean install
```

## Testing

To run tests:

```bash
cd java
mvn test
```

## Usage

### A2A Agent Registration (Spring Boot)

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>spring-boot-starter-agentscope-runtime-a2a-nacos</artifactId>
    <version>${agentscope-extensions-nacos.version}</version>
</dependency>
```

Configure your `application.yaml`:

```yaml
spring:
  agentscope:
    runtime:
      a2a:
        card:
          description: "Your agent description"
          provider:
            organization: Your Organization
            url: https://your-organization.com
        nacos:
          server-addr: ${NACOS_SERVER_ADDRESS:127.0.0.1:8848}
          username: ${NACOS_USERNAME:nacos}
          password: ${NACOS_PASSWORD:nacos}
```

### A2A Agent Discovery

To discover and communicate with A2A agents:

```java
Properties properties = new Properties();
properties.put("serverAddr", "localhost:8848");
NacosAgentCardProducer producer = new NacosAgentCardProducer(properties);
A2aAgentConfig config = A2aAgentConfig.builder()
    .agentCardProducer(producer)
    .build();
A2aAgent agent = new A2aAgent("agent-name", config);
```

### MCP Tool Discovery

To discover and use MCP tools:

```java
Properties properties = new Properties();
properties.put("serverAddr", "localhost:8848");
NacosMcpServerManager manager = NacosMcpServerManager.from(properties);
NacosMcpClientWrapper client = NacosMcpClientBuilder.create("mcp-server-name", manager).build();
Toolkit toolkit = new NacosToolkit();
toolkit.registerMcpClient(client).block();
```

## Prerequisites

- Java 17+
- Maven 3.6+
- Nacos Server 3.1+

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `NACOS_SERVER_ADDRESS` | Nacos server address | `127.0.0.1:8848` |
| `NACOS_USERNAME` | Nacos username | `nacos` |
| `NACOS_PASSWORD` | Nacos password | `nacos` |

## Contributing

We welcome contributions to this project! Please see our contributing guidelines for more details.

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](../../LICENSE) file for details.