package com.agentscope.nacos;

/**
 * Nacos Extension for AgentScope
 * This class provides integration between AgentScope and Nacos service discovery.
 */
public class NacosExtension {
    
    private String serverAddr;
    private String namespace;
    
    /**
     * Constructor for NacosExtension
     * @param serverAddr Nacos server address
     * @param namespace Nacos namespace
     */
    public NacosExtension(String serverAddr, String namespace) {
        this.serverAddr = serverAddr;
        this.namespace = namespace;
    }
    
    /**
     * Get server address
     * @return server address
     */
    public String getServerAddr() {
        return serverAddr;
    }
    
    /**
     * Get namespace
     * @return namespace
     */
    public String getNamespace() {
        return namespace;
    }
}
