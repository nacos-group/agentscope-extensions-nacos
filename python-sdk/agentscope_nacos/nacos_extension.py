"""
Nacos Extension for AgentScope
This module provides integration between AgentScope and Nacos service discovery.
"""


class NacosExtension:
    """
    Nacos Extension for AgentScope
    """

    def __init__(self, server_addr: str, namespace: str):
        """
        Initialize NacosExtension

        Args:
            server_addr (str): Nacos server address
            namespace (str): Nacos namespace
        """
        self.server_addr = server_addr
        self.namespace = namespace

    def get_server_addr(self) -> str:
        """
        Get server address

        Returns:
            str: Server address
        """
        return self.server_addr

    def get_namespace(self) -> str:
        """
        Get namespace

        Returns:
            str: Namespace
        """
        return self.namespace
