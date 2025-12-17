# agentscope-extensions-nacos
Nacos extensions component for agentscope.

This project provides integration between AgentScope and Nacos service discovery, with implementations in both Java and Python.

## Project Structure

```
agentscope-extensions-nacos/
├── java/                                                           # Java Implementation (Maven)
│   ├── agentscope-extensions-a2a-nacos                             # Source Code of AgentScope Extension For A2A(Agent2Agent) Protocol Registry with Nacos (Discovery&Register)
│   ├── agentscope-extensions-mcp-nacos                             # Source Code of AgentScope Extension For MCP Protocol Registry with Nacos (Discovery)
│   ├── example                                                     # Example for AgentScope Extension Java Implementation
│   │   ├── a2a-example                                             # Example for AgentScope Extension A2A(Agent2Agent) Protocol with Nacos Regsitry
│   │   └── mcp-example                                             # Example for AgentScope Extension MCP Protocol with Nacos Regsitry
│   └── spring                                                      # AgentScope Extension For Spring Framework
│       └── spring-boot-starter-agentscope-runtime-a2a-nacos        # Source Code of Spring Boot Starter for AgentScope Runtime Extension to Auto Register A2A Protocol to Nacos.
└── python/                                                         # Python Implementation
    ├── setup.py                                                    # AgentScope Extension Python Implementation setup script
    ├── agentscope_nacos/                                           # Source Code of AgentScope Extension Python Implementation
    ├── example/                                                    # Example for AgentScope Extension Python Implementation
    └── tests/                                                      # Tests for AgentScope Extension Python Implementation
```

## Java

```
agentscope-extensions-a2a has been contribute into agentscope-java in 1.0.3 version, so remove in this extensions project.

new dependencies:

<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-extensions-a2a-agent</artifactId>
    <!-- upper than 1.0.3 -->
    <version>${agentscope.version}</version>
</dependency>
```

The Java implementation provides seamless integration between AgentScope and Nacos service discovery. It enables automatic registration and discovery of agents using the A2A (Agent-to-Agent) and MCP (Model Context Protocol) protocols through Nacos.

Key features:
- A2A protocol support for agent-to-agent communication
- MCP protocol integration for model context operations
- Nacos service discovery for automatic agent registration and discovery
- Spring Boot starter for easy integration with Spring applications
- Examples demonstrating both A2A and MCP protocol usage with Nacos

For detailed information about the Java implementation, please see [Java README](java/README.md).

## Python

The Python implementation offers Nacos integration for Python-based AgentScope applications. It provides essential functionality for registering and discovering agents in Nacos service registry.

Key features:
- Easy integration with AgentScope Python framework
- Nacos service discovery for agent registration and lookup
- Simple API for common Nacos operations
- Examples demonstrating usage patterns

For detailed information about the Python implementation, please see [Python README](python/README.md).

## Development

The project uses a comprehensive `.gitignore` file that covers:
- Java artifacts (compiled classes, Maven target directory, JAR files)
- Python artifacts (bytecode, virtual environments, distribution files)
- IDE files (IntelliJ IDEA configuration files)

## License

See LICENSE file for details.