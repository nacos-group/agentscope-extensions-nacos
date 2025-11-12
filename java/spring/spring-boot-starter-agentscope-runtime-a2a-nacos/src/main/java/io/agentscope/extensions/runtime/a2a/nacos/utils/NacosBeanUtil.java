package io.agentscope.extensions.runtime.a2a.nacos.utils;

import io.agentscope.extensions.runtime.a2a.nacos.NacosA2aProtocolConfig;
import io.agentscope.runtime.protocol.ProtocolConfig;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Utils for Nacos Spring Beans.
 *
 * @author xiweng.yy
 */
public class NacosBeanUtil {
    /**
     * Get NacosA2aProtocolConfig from ObjectProvider of ProtocolConfig.
     * This method filters the protocol configs to find the first one that is assignable from NacosA2aProtocolConfig.
     *
     * @param protocolConfigs the ObjectProvider of ProtocolConfig to search from
     * @return the NacosA2aProtocolConfig instance if found, otherwise null
     */
    public static NacosA2aProtocolConfig getNacosA2aProtocolConfig(ObjectProvider<ProtocolConfig> protocolConfigs) {
        return protocolConfigs.stream()
                .filter(protocolConfig -> NacosA2aProtocolConfig.class.isAssignableFrom(protocolConfig.getClass()))
                .map(protocolConfig -> (NacosA2aProtocolConfig) protocolConfig).findFirst().orElse(null);
    }
}
