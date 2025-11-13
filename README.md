# agentscope-extensions-nacos
Nacos extensions component for agentscope.

This project provides integration between AgentScope and Nacos service discovery, with implementations in both Java and Python.

## Project Structure

```
agentscope-extensions-nacos/
├── java/           # Java implementation (Maven)
│   ├── pom.xml
│   └── src/
│       ├── main/java/
│       └── test/java/
└── python/         # Python implementation
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

## Contributing

We welcome contributions! Please see our [CONTRIBUTING.md](CONTRIBUTING.md) guide for:
- Setting up your development environment
- Fork and branch workflow
- Code style guidelines
- Troubleshooting common issues (including 403 push errors)
- Submitting pull requests

## License

See LICENSE file for details.
