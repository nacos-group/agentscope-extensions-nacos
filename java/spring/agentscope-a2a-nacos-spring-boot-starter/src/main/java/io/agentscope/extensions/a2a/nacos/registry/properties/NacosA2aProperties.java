/*
 * Copyright 1999-2025 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.agentscope.extensions.a2a.nacos.registry.properties;

import io.agentscope.extensions.a2a.nacos.registry.constants.Constants;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * A2a properties for Nacos A2A Registry.
 *
 * @author xiweng.yy
 */
@ConfigurationProperties(prefix = Constants.REGISTRY_PREFIX)
public class NacosA2aProperties {
    
    private boolean registerAsLatest = true;
    
    private boolean enabledRegisterEndpoint = true;
    
    /**
     * If setting with this property, the preferredTransport and url in agentCard will be overwritten.
     *
     * <p>
     * If not found the transport from agentscope and all properties, overwrite will be ignored.
     * </p>
     */
    private String overwritePreferredTransport;
    
    private Map<String, NacosA2aTransportProperties> transports = new HashMap<>();
    
    public boolean isRegisterAsLatest() {
        return registerAsLatest;
    }
    
    public void setRegisterAsLatest(boolean registerAsLatest) {
        this.registerAsLatest = registerAsLatest;
    }
    
    public boolean isEnabledRegisterEndpoint() {
        return enabledRegisterEndpoint;
    }
    
    public void setEnabledRegisterEndpoint(boolean enabledRegisterEndpoint) {
        this.enabledRegisterEndpoint = enabledRegisterEndpoint;
    }
    
    public String getOverwritePreferredTransport() {
        return overwritePreferredTransport;
    }
    
    public void setOverwritePreferredTransport(String overwritePreferredTransport) {
        this.overwritePreferredTransport = overwritePreferredTransport;
    }
    
    public Map<String, NacosA2aTransportProperties> getTransports() {
        return transports;
    }
    
    public void setTransports(Map<String, NacosA2aTransportProperties> transports) {
        this.transports = transports;
    }
}
