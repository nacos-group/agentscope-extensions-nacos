package com.agentscope.nacos;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test class for NacosExtension
 */
public class NacosExtensionTest {
    
    @Test
    public void testNacosExtensionCreation() {
        String serverAddr = "localhost:8848";
        String namespace = "test-namespace";
        
        NacosExtension extension = new NacosExtension(serverAddr, namespace);
        
        assertEquals(serverAddr, extension.getServerAddr());
        assertEquals(namespace, extension.getNamespace());
    }
}
