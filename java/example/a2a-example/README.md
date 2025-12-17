# AgentScope A2A Protocol Examples

This directory contains examples demonstrating the Agent-to-Agent (A2A) protocol implementation with Nacos integration in AgentScope.

## Overview

The A2A (Agent-to-Agent) protocol enables communication between agents in a standardized way. These examples show how to register an agent with Nacos and discover/consume it from another application.

## Examples Structure

### a2a-register-runtime-example

This example demonstrates how to register an agent with Nacos service registry by agentscope-runtime-java.

Key components:
- Spring Boot application exposing an agent service
- Registration of the agent with Nacos using A2A protocol
- Configuration via `application.yaml`

### a2a-discovery-example

This example shows how to discover and communicate with agents registered in Nacos.

Key components:
- Client application that discovers agents via Nacos
- Direct interaction with registered agents
- Console-based interface for testing

## Prerequisites

- Java 17+
- Maven 3.6+
- Nacos Server 3.1+

## Quick Start

### 1. Start Nacos Server

Make sure you have a Nacos server running. You can download and start Nacos by following the [official documentation](https://nacos.io/docs/latest/guide/start/quick-start/).

Default connection settings:
- Server Address: `127.0.0.1:8848`
- Username: `nacos`
- Password: `nacos`

These can be customized using environment variables:
- `NACOS_SERVER_ADDRESS`
- `NACOS_USERNAME`
- `NACOS_PASSWORD`

### 2. Configure DashScope API Key

The example uses DashScope as the AI model provider. Set your API key:

```bash
export AI_DASHSCOPE_API_KEY=your_api_key_here
```

### 3. Run the Register Example

Navigate to the register example directory and start the application:

```bash
cd a2a-register-example
mvn spring-boot:run
```

This will register an agent named `agentscope-a2a-example-agent` with Nacos.

### 4. Run the Discovery Example

In another terminal, navigate to the discovery example directory:

```bash
cd a2a-dicovery-example
mvn compile exec:java -Dexec.mainClass="io.agentscope.extensions.nacos.example.A2aAgentCallerExample"
```

You can now interact with the registered agent through the console interface.

Type any message to send to the agent, or type `exit` or `quit` to terminate the program.

## Configuration

### Register Example Configuration

The registration example uses Spring Boot's `application.yaml` for configuration:

```yaml
agentscope:
  a2a:
    server:
      card:
        description: "Example of A2A(Agent2Agent) Protocol Agent"
        provider:
          organization: Alibaba Nacos
          url: https://nacos.io
      nacos:
        server-addr: ${NACOS_SERVER_ADDRESS:127.0.0.1:8848}
        username: ${NACOS_USERNAME:nacos}
        password: ${NACOS_PASSWORD:nacos}
```

### Environment Variables

Both examples support the following environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `NACOS_SERVER_ADDRESS` | Nacos server address | `127.0.0.1:8848` |
| `NACOS_USERNAME` | Nacos username | `nacos` |
| `NACOS_PASSWORD` | Nacos password | `nacos` |
| `AI_DASHSCOPE_API_KEY` | DashScope API key | None (required) |

## How It Works

1. **Registration**: The register example creates an agent and registers it with Nacos using the A2A protocol. The agent metadata includes its capabilities and connection information.

2. **Discovery**: The discovery example connects to Nacos and looks up available agents by name (`agentscope-a2a-example-agent`).

3. **Communication**: Once discovered, the client can send messages to the agent and receive streaming responses.

## Customization

To customize these examples for your own use case:

1. Modify the agent name in both examples
2. Adjust the agent prompt in `AgentScopeAgentConfiguration.java`
3. Update the Nacos connection details in `application.yaml`
4. Change the AI model settings as needed

## Troubleshooting

Common issues and solutions:

1. **Connection refused to Nacos**: Ensure Nacos server is running and accessible
2. **Authentication failed**: Check Nacos credentials
3. **API key error**: Verify DashScope API key is set correctly
4. **Agent not found**: Make sure the register example is running before starting the discovery example

## Contributing

If you'd like to contribute to these examples or report issues, please submit a pull request or open an issue in the repository.