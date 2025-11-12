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

package io.agentscope.extensions.runtime.a2a.nacos.registry;

import io.a2a.spec.AgentCard;
import io.agentscope.extensions.nacos.a2a.registry.NacosA2aRegistry;
import io.agentscope.extensions.nacos.a2a.registry.NacosA2aRegistryProperties;
import io.agentscope.extensions.runtime.a2a.nacos.properties.NacosA2aProperties;
import io.agentscope.extensions.runtime.a2a.registry.AgentRegistry;
import io.agentscope.runtime.autoconfigure.DeployProperties;
import io.agentscope.runtime.protocol.a2a.NetworkUtils;

/**
 * The Agent registry for Nacos.
 *
 * @author xiweng.yy
 */
public class NacosAgentRegistry implements AgentRegistry {
    
    /**
     * AgentScope export a2a message with fixed path: "/a2a/"
     */
    private static final String DEFAULT_ENDPOINT_PATH = "/a2a/";
    
    private final NacosA2aRegistry nacosA2aRegistry;
    
    private final NacosA2aProperties nacosA2aProperties;
    
    public NacosAgentRegistry(NacosA2aRegistry nacosA2aRegistry, NacosA2aProperties nacosA2aProperties) {
        this.nacosA2aRegistry = nacosA2aRegistry;
        this.nacosA2aProperties = nacosA2aProperties;
    }
    
    @Override
    public String registryName() {
        return "Nacos";
    }
    
    @Override
    public void register(AgentCard agentCard, DeployProperties deployProperties) {
        NetworkUtils networkUtils = new NetworkUtils(deployProperties);
        NacosA2aRegistryProperties properties = new NacosA2aRegistryProperties(nacosA2aProperties.isRegisterAsLatest(),
                networkUtils.getServerIpAddress(), networkUtils.getServerPort(), DEFAULT_ENDPOINT_PATH);
        nacosA2aRegistry.registerAgent(agentCard, properties);
    }
    
}
