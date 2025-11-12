package io.agentscope.extensions.nacos.a2a.registry;

/**
 * Properties for A2A AgentCard and Endpoint registry to Nacos.
 *
 * @author xiweng.yy
 */
public record NacosA2aRegistryProperties(boolean isSetAsLatest, String endpointAddress, int endpointPort, String endpointPath) {

}
