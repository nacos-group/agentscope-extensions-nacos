"""
Test module for NacosExtension
"""

import unittest
from agentscope_nacos import NacosExtension


class TestNacosExtension(unittest.TestCase):
    """Test cases for NacosExtension"""

    def test_nacos_extension_creation(self):
        """Test creating a NacosExtension instance"""
        server_addr = "localhost:8848"
        namespace = "test-namespace"

        extension = NacosExtension(server_addr, namespace)

        self.assertEqual(extension.get_server_addr(), server_addr)
        self.assertEqual(extension.get_namespace(), namespace)


if __name__ == "__main__":
    unittest.main()
