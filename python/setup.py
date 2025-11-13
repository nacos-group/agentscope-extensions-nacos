from setuptools import setup, find_packages
import os

# Read README from the same directory as setup.py
here = os.path.abspath(os.path.dirname(__file__))
try:
    with open(os.path.join(here, "README.md"), "r", encoding="utf-8") as fh:
        long_description = fh.read()
except FileNotFoundError:
    long_description = "Nacos extensions component for agentscope - Python SDK"

setup(
    name="agentscope-extensions-nacos",
    version="1.0.0",
    author="AgentScope Team",
    description="Nacos extensions component for agentscope - Python SDK",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/nacos-group/agentscope-extensions-nacos",
    packages=find_packages(),
    classifiers=[
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3.7",
        "Programming Language :: Python :: 3.8",
        "Programming Language :: Python :: 3.9",
        "Programming Language :: Python :: 3.10",
        "Programming Language :: Python :: 3.11",
        "License :: OSI Approved :: Apache Software License",
        "Operating System :: OS Independent",
    ],
    python_requires=">=3.7",
    install_requires=[
        "nacos-sdk-python>=3.0.0",
    ],
)
