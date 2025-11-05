# AgentScope Extensions Nacos - Python

Nacos extensions component for agentscope - Python

## Installation

```bash
pip install -e .
```

## Usage

```python
from agentscope_nacos import NacosExtension

# Create a Nacos extension instance
extension = NacosExtension(
    server_addr="localhost:8848",
    namespace="test-namespace"
)
```
