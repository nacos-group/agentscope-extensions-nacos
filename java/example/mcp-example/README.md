# AgentScope MCP Protocol Examples with Nacos

This directory contains examples demonstrating the Model Context Protocol (MCP) implementation with Nacos integration in AgentScope.

## Overview

The MCP (Model Context Protocol) enables standardized communication between AI models and tools. These examples show how to discover and use MCP tools registered in Nacos service registry.

## Example Structure

### mcp-discovery-example

This example demonstrates how to discover and use MCP tools registered with Nacos service registry.

Key components:
- Client application that discovers MCP tools via Nacos
- Two different approaches to integrate MCP tools with AgentScope
- Console-based interface for testing tool usage

## Prerequisites

- Java 17+
- Maven 3.6+
- Nacos Server 3.1+

## Quick Start

### 1. Register MCP Server in Nacos

Before running the example, you need to register an MCP server in Nacos:

1. Access the [IP Location MCP Server](https://mcp.nacos.io/server/server9016) in your browser
2. Sign in and click the `Create` button to generate a URL and API key
3. Copy the generated URL (e.g., `https://mcp.higress.ai/mcp-ip-query/.../sse`)
4. Access the Nacos Console (e.g., `http://localhost:8848`) and navigate to the `MCP List` page
5. Create a new MCP server with:
   - Name: `IP-Location`
   - Protocol Type: `sse`
   - HTTP to MCP: disabled
   - MCP Server Endpoint: URL from step 3
6. Click `Auto import from MCP Server` in the Tools block

### 2. Start Nacos Server

Make sure you have a Nacos server running. You can download and start Nacos by following the [official documentation](https://nacos.io/docs/latest/guide/start/quick-start/).

Default connection settings:
- Server Address: `127.0.0.1:8848`
- Username: `nacos`
- Password: `nacos`

These can be customized using environment variables:
- `NACOS_SERVER_ADDRESS`
- `NACOS_USERNAME`
- `NACOS_PASSWORD`

### 3. Configure DashScope API Key

The example uses DashScope as the AI model provider. Set your API key:

```bash
export AI_DASHSCOPE_API_KEY=your_api_key_here
```

### 4. Run the Example

Navigate to the example directory and run the application:

```bash
cd mcp-discovery-example
mvn compile exec:java -Dexec.mainClass="io.agentscope.extensions.nacos.example.McpDiscoveryWithNacosExample"
```

You can now interact with the agent through the console interface.

Type any message to send to the agent, or type `exit` or `quit` to terminate the program.

## Two Ways to Integrate MCP Tools

### Way 1: Using NacosToolkit (Recommended)

```java
private static Toolkit buildToolKitWay1(AiService aiService) {
    NacosMcpServerManager mcpServerManager = NacosMcpServerManager.from(aiService);
    NacosMcpClientWrapper mcpClientWrapper = NacosMcpClientBuilder.create("IP-Location", mcpServerManager).build();
    Toolkit toolkit = new NacosToolkit();
    toolkit.registerMcpClient(mcpClientWrapper).block();
    return toolkit;
}
```

In this approach, Nacos automatically rebuilds MCP tools when the MCP server or tool specifications change. The NacosToolkit will remove old MCP tools and fully rebuild them in the Toolkit.

### Way 2: Using NacosMcpTool

```java
private static Toolkit buildToolKitWay2(AiService aiService) {
    NacosMcpServerManager mcpServerManager = NacosMcpServerManager.from(aiService);
    NacosMcpClientWrapper mcpClientWrapper = NacosMcpClientBuilder.create("IP-Location", mcpServerManager).build();
    Toolkit toolkit = new Toolkit();
    NacosMcpToolBuilder.create(mcpClientWrapper).build().forEach(toolkit::registerTool);
    return toolkit;
}
```

In this approach, Nacos rebuilds MCP tools when the MCP server or tool specifications change. Unlike Way 1, this method only rebuilds and replaces metadata and the actual MCP server connection in the NacosMcpTool, without doing a full rebuild in the Toolkit.

## Environment Variables

The example supports the following environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `NACOS_SERVER_ADDRESS` | Nacos server address | `127.0.0.1:8848` |
| `NACOS_USERNAME` | Nacos username | `nacos` |
| `NACOS_PASSWORD` | Nacos password | `nacos` |
| `AI_DASHSCOPE_API_KEY` | DashScope API key | None (required) |

## How It Works

1. **Registration**: MCP servers and their tools are registered in Nacos through the Nacos Console
2. **Discovery**: The example application connects to Nacos and discovers available MCP tools by name
3. **Integration**: MCP tools are integrated into AgentScope Toolkit using one of the two methods
4. **Execution**: The agent can use the MCP tools to perform actions like IP location lookup

## Customization

To customize this example for your own use case:

1. Modify the MCP server name in the code
2. Change the agent prompt in [McpDiscoveryWithNacosExample.java](file:///Users/xiweng.yy/Documents/java/opensource/agentscope-extensions-nacos/java/example/mcp-example/mcp-discovery-example/src/main/java/io/agentscope/extensions/nacos/example/McpDiscoveryWithNacosExample.java)
3. Update the Nacos connection details via environment variables
4. Change the AI model settings as needed
5. Register your own MCP servers and tools in Nacos

## Troubleshooting

Common issues and solutions:

1. **Connection refused to Nacos**: Ensure Nacos server is running and accessible
2. **Authentication failed**: Check Nacos credentials
3. **API key error**: Verify DashScope API key is set correctly
4. **MCP tools not found**: Make sure the MCP server is properly registered in Nacos
5. **Tool execution failed**: Check that the MCP server endpoint is accessible

## Contributing

If you'd like to contribute to these examples or report issues, please submit a pull request or open an issue in the repository.