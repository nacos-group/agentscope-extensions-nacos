# agentscope-extensions-nacos
Nacos extensions component for agentscope.

This project provides integration between AgentScope and Nacos service discovery, with implementations in both Java and Python.

## Project Structure

```
agentscope-extensions-nacos/
├── java-sdk/           # Java implementation (Maven)
│   ├── pom.xml
│   └── src/
│       ├── main/java/
│       └── test/java/
└── python-sdk/         # Python implementation
    ├── setup.py
    ├── agentscope_nacos/
    └── tests/
```

## Java SDK

The Java SDK is built with Maven and provides Nacos integration for Java-based AgentScope applications.

### Building

```bash
cd java-sdk
mvn clean install
```

### Testing

```bash
cd java-sdk
mvn test
```

## Python SDK

The Python SDK provides Nacos integration for Python-based AgentScope applications.

### Installation

```bash
cd python-sdk
pip install -e .
```

### Testing

```bash
cd python-sdk
python -m unittest discover -s tests
```

## Development

The project uses a comprehensive `.gitignore` file that covers:
- Java artifacts (compiled classes, Maven target directory, JAR files)
- Python artifacts (bytecode, virtual environments, distribution files)
- IDE files (IntelliJ IDEA configuration files)

## License

See LICENSE file for details.
