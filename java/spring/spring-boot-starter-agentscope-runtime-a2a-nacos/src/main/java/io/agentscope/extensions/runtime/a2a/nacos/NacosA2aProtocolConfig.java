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

package io.agentscope.extensions.runtime.a2a.nacos;

import io.agentscope.runtime.protocol.a2a.A2aProtocolConfig;
import io.agentscope.runtime.protocol.a2a.ConfigurableAgentCard;

import java.util.Properties;

/**
 * Extensions {@link io.agentscope.runtime.protocol.a2a.A2aProtocolConfig} for Nacos A2A Registry.
 *
 * @author xiweng.yy
 */
public class NacosA2aProtocolConfig extends A2aProtocolConfig {
    
    private final Properties nacosProperties;
    
    private final boolean registerAsLatest;
    
    private final boolean enabledRegisterEndpoint;
    
    /**
     * If setting with this property, the preferredTransport and url in agentCard will be overwritten.
     *
     * <p>
     * If not found the transport from agentscope and all properties, overwrite will be ignored.
     * </p>
     */
    private final String overwritePreferredTransport;
    
    public NacosA2aProtocolConfig(ConfigurableAgentCard agentCard, Properties nacosProperties, boolean registerAsLatest,
            boolean enabledRegisterEndpoint, String overwritePreferredTransport) {
        super(agentCard);
        this.nacosProperties = nacosProperties;
        this.registerAsLatest = registerAsLatest;
        this.enabledRegisterEndpoint = enabledRegisterEndpoint;
        this.overwritePreferredTransport = overwritePreferredTransport;
    }
    
    public Properties getNacosProperties() {
        return nacosProperties;
    }
    
    public boolean isRegisterAsLatest() {
        return registerAsLatest;
    }
    
    public boolean isEnabledRegisterEndpoint() {
        return enabledRegisterEndpoint;
    }
    
    public String getOverwritePreferredTransport() {
        return overwritePreferredTransport;
    }
    
    public static class Builder extends A2aProtocolConfig.Builder {
        
        private final Properties nacosProperties;
        
        private boolean registerAsLatest = true;
        
        private boolean enabledRegisterEndpoint = true;
        
        private String overwritePreferredTransport;
        
        public Builder(Properties nacosProperties) {
            this.nacosProperties = nacosProperties;
        }
        
        public Builder registerAsLatest(boolean registerAsLatest) {
            this.registerAsLatest = registerAsLatest;
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
        
        public NacosA2aProtocolConfig.Builder agentCard(ConfigurableAgentCard agentCard) {
            super.agentCard(agentCard);
            return this;
        }
        
        @Override
        public A2aProtocolConfig build() {
            if (null == nacosProperties) {
                throw new IllegalArgumentException("Nacos properties can not be null");
            }
            return new NacosA2aProtocolConfig(agentCard, nacosProperties, registerAsLatest, enabledRegisterEndpoint,
                    overwritePreferredTransport);
        }
    }
}
