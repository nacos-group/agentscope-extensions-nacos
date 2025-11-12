package io.agentscope.extensions.runtime.a2a.nacos.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * A2a properties for Nacos A2A Registry.
 *
 * @author xiweng.yy
 */
@ConfigurationProperties(prefix = NacosA2aProperties.PREFIX)
public class NacosA2aProperties {
    
    public static final String PREFIX = NacosServerProperties.PREFIX + ".registry";
    
    private boolean registerAsLatest;
    
    public boolean isRegisterAsLatest() {
        return registerAsLatest;
    }
    
    public void setRegisterAsLatest(boolean registerAsLatest) {
        this.registerAsLatest = registerAsLatest;
    }
}
