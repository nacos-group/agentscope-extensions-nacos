"""
AgentScope Extension Nacos - Setup Configuration

This setup.py is maintained for backward compatibility.
For modern Python packaging, see pyproject.toml.
"""

from setuptools import setup, find_packages
import os

# Read README from the same directory as setup.py
here = os.path.abspath(os.path.dirname(__file__))
try:
    with open(os.path.join(here, "README.md"), "r", encoding="utf-8") as fh:
        long_description = fh.read()
except FileNotFoundError:
    long_description = "Nacos extension component for AgentScope - Python SDK"

setup(
    name="agentscope-extension-nacos",
    version="0.2.1",
    author="AgentScope Team",
    description="Nacos extension component for AgentScope - Python SDK",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/nacos-group/agentscope-extensions-nacos",
    project_urls={
        "Homepage": "https://github.com/nacos-group/agentscope-extensions-nacos",
        "Documentation": "https://github.com/nacos-group/agentscope-extensions-nacos/blob/main/python/README.md",
        "Repository": "https://github.com/nacos-group/agentscope-extensions-nacos",
        "Issues": "https://github.com/nacos-group/agentscope-extensions-nacos/issues",
    },
    packages=find_packages(exclude=["tests", "tests.*", "example", "example.*"]),
    classifiers=[
        "Development Status :: 4 - Beta",
        "Intended Audience :: Developers",
        "License :: OSI Approved :: Apache Software License",
        "Operating System :: OS Independent",
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3.9",
        "Programming Language :: Python :: 3.10",
        "Programming Language :: Python :: 3.11",
        "Programming Language :: Python :: 3.12",
    ],
    python_requires=">=3.9",
    install_requires=[
        "nacos-sdk-python>=3.0.0",
        "agentscope>=1.0.7",
        "agentscope-runtime>=1.0.1",
        "httpx>=0.25.0",
        "pydantic>=2.0.0",
    ],
    include_package_data=True,
    zip_safe=False,
)
