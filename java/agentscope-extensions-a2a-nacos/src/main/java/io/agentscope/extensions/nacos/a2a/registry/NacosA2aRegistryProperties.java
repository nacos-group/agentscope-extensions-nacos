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

package io.agentscope.extensions.nacos.a2a.registry;

import java.util.HashMap;
import java.util.Map;

/**
 * Properties for A2A AgentCard and Endpoint registry to Nacos.
 *
 * <p>TODO extract transportProperties out from NacosA2aRegistryProperties.
 *
 * @author xiweng.yy
 */
public record NacosA2aRegistryProperties(boolean isSetAsLatest, boolean enabledRegisterEndpoint, String overwritePreferredTransport,
                                         Map<String, NacosA2aRegistryTransportProperties> transportProperties) {
    
    public void addTransport(NacosA2aRegistryTransportProperties transport) {
        transportProperties.put(transport.transport(), transport);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        
        private final Map<String, NacosA2aRegistryTransportProperties> transportProperties;
        
        private boolean setAsLatest;
        
        private boolean enabledRegisterEndpoint;
        
        private String overwritePreferredTransport;
        
        private Builder() {
            transportProperties = new HashMap<>();
            enabledRegisterEndpoint = true;
        }
        
        public Builder setAsLatest(boolean setAsLatest) {
            this.setAsLatest = setAsLatest;
            return this;
        }
        
        public Builder enabledRegisterEndpoint(boolean enabledRegisterEndpoint) {
            this.enabledRegisterEndpoint = enabledRegisterEndpoint;
            return this;
        }
        
        public Builder overwritePreferredTransport(String overwritePreferredTransport) {
            this.overwritePreferredTransport = overwritePreferredTransport;
            return this;
        }
        
        public Builder addTransport(NacosA2aRegistryTransportProperties transport) {
            transportProperties.put(transport.transport(), transport);
            return this;
        }
        
        public NacosA2aRegistryProperties build() {
            return new NacosA2aRegistryProperties(setAsLatest, enabledRegisterEndpoint, overwritePreferredTransport,
                    transportProperties);
        }
    }
}
