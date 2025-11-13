# agentscope-extensions-nacos
Nacos extensions component for agentscope.

This project provides integration between AgentScope and Nacos service discovery, with implementations in both Java and Python.

## Project Structure

```
agentscope-extensions-nacos/
├── java/                                                           # Java Implementation (Maven)
│   ├── agentscope-extensions-a2a                                   # AgentScope Extension For A2A(Agent2Agent) Protocol Request
│   ├── agentscope-extensions-a2a-nacos                             # AgentScope Extension For A2A(Agent2Agent) Protocol Registry with Nacos (Discovery&Register)
│   ├── agentscope-extensions-mcp-nacos                             # AgentScope Extension For MCP Protocol Registry with Nacos (Discovery)
│   ├── example                                                     # Example for AgentScope Extension For Java Implementation
│   │   └── a2a-example                                             # Example for AgentScope Extension For A2A(Agent2Agent) Protocol with Nacos Regsitry
│   └── spring                                                      # AgentScope Extension For Spring Framework
│       └── spring-boot-starter-agentscope-runtime-a2a-nacos        # Spring Boot Starter for AgentScope Runtime Extension to Auto Register A2A Protocol to Nacos.
└── python/                                                         # Python Implementation
    ├── setup.py
    ├── agentscope_nacos/
    └── tests/
```

## Java

The Java SDK is built with Maven and provides Nacos integration for Java-based AgentScope applications.

### Building

```bash
cd java
mvn clean install
```

### Testing

```bash
cd java
mvn test
```

## Python

The Python SDK provides Nacos integration for Python-based AgentScope applications.

### Installation

```bash
cd python
pip install -e .
```

### Testing

```bash
cd python
python -m unittest discover -s tests
```

## Development

The project uses a comprehensive `.gitignore` file that covers:
- Java artifacts (compiled classes, Maven target directory, JAR files)
- Python artifacts (bytecode, virtual environments, distribution files)
- IDE files (IntelliJ IDEA configuration files)

## License

See LICENSE file for details.
