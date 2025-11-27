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

import com.alibaba.nacos.common.utils.StringUtils;
import io.a2a.spec.AgentCard;
import io.agentscope.extensions.a2a.agent.utils.LoggerUtil;
import io.agentscope.extensions.nacos.a2a.registry.NacosA2aRegistry;
import io.agentscope.extensions.nacos.a2a.registry.NacosA2aRegistryProperties;
import io.agentscope.extensions.nacos.a2a.registry.NacosA2aRegistryTransportProperties;
import io.agentscope.extensions.runtime.a2a.nacos.properties.NacosA2aProperties;
import io.agentscope.extensions.runtime.a2a.nacos.properties.NacosA2aTransportProperties;
import io.agentscope.extensions.runtime.a2a.nacos.properties.NacosA2aTransportPropertiesEnvParser;
import io.agentscope.extensions.runtime.a2a.registry.AgentRegistry;
import io.agentscope.runtime.autoconfigure.DeployProperties;
import io.agentscope.runtime.protocol.a2a.NetworkUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * The Agent registry for Nacos.
 *
 * @author xiweng.yy
 */
public class NacosAgentRegistry implements AgentRegistry {
    
    private static final Logger log = LoggerFactory.getLogger(NacosAgentRegistry.class);
    
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
        NacosA2aRegistryProperties properties = new NacosA2aRegistryProperties(nacosA2aProperties.isRegisterAsLatest(),
                nacosA2aProperties.isEnabledRegisterEndpoint());
        buildTransportProperties(agentCard, deployProperties).forEach(properties::addTransport);
        nacosA2aRegistry.registerAgent(agentCard, properties);
    }
    
    private Collection<NacosA2aRegistryTransportProperties> buildTransportProperties(AgentCard agentCard,
            DeployProperties deployProperties) {
        Map<String, NacosA2aRegistryTransportProperties> result = parseTransportsFromDeploy(agentCard,
                deployProperties);
        getTransportProperties().forEach((transport, properties) -> result.compute(transport, (key, oldValue) -> {
            String targetTransport = key.toUpperCase();
            if (null == oldValue) {
                LoggerUtil.warn(log,
                        "Transport {} is not export by agentscope, it might cause agentCard include an unavailable endpoint.",
                        targetTransport);
                return overwriteAttributes(null, properties, targetTransport);
            }
            NacosA2aRegistryTransportProperties newValue = overwriteAttributes(oldValue, properties, targetTransport);
            LoggerUtil.debug(log, "Overwrite attributes for transport {} from {} to {}", targetTransport, oldValue,
                    newValue);
            return newValue;
        }));
        return result.values();
    }
    
    private Map<String, NacosA2aRegistryTransportProperties> parseTransportsFromDeploy(AgentCard agentCard,
            DeployProperties deployProperties) {
        Map<String, NacosA2aRegistryTransportProperties> result = new HashMap<>();
        NetworkUtils networkUtils = new NetworkUtils(deployProperties);
        // TODO Support parse multiple transport from agentCard or deploy properties.
        NacosA2aRegistryTransportProperties defaultTransport = NacosA2aRegistryTransportProperties.builder()
                .transport(agentCard.preferredTransport()).endpointAddress(networkUtils.getServerIpAddress())
                .endpointPort(networkUtils.getServerPort()).endpointPath(DEFAULT_ENDPOINT_PATH).build();
        result.put(defaultTransport.transport(), defaultTransport);
        return result;
    }
    
    private Map<String, NacosA2aTransportProperties> getTransportProperties() {
        Map<String, NacosA2aTransportProperties> result = new HashMap<>();
        nacosA2aProperties.getTransports()
                .forEach((transport, properties) -> result.put(transport.toUpperCase(), properties));
        new NacosA2aTransportPropertiesEnvParser().getTransportProperties().forEach((transport, properties) -> {
            result.compute(transport, (key, oldValue) -> {
                if (null == oldValue) {
                    return properties;
                }
                oldValue.merge(properties);
                return oldValue;
            });
        });
        return result;
    }
    
    private NacosA2aRegistryTransportProperties overwriteAttributes(NacosA2aRegistryTransportProperties oldValue,
            NacosA2aTransportProperties newValue, String transport) {
        NacosA2aRegistryTransportProperties.Builder builder = NacosA2aRegistryTransportProperties.builder();
        if (null != oldValue) {
            builder.endpointAddress(oldValue.endpointAddress()).endpointPort(oldValue.endpointPort())
                    .endpointPath(oldValue.endpointPath()).isSupportTls(oldValue.isSupportTls())
                    .endpointProtocol(oldValue.endpointProtocol()).endpointQuery(oldValue.endpointQuery());
        }
        if (StringUtils.isNotEmpty(newValue.getHost())) {
            builder.endpointAddress(newValue.getHost());
        }
        if (newValue.getPort() > 0) {
            builder.endpointPort(newValue.getPort());
        }
        if (StringUtils.isNotEmpty(newValue.getPath())) {
            builder.endpointPath(newValue.getPath());
        }
        if (StringUtils.isNotEmpty(newValue.getProtocol())) {
            builder.endpointProtocol(newValue.getProtocol());
        }
        if (StringUtils.isNotEmpty(newValue.getQuery())) {
            builder.endpointQuery(newValue.getQuery());
        }
        if (null != newValue.isSupportTls()) {
            builder.isSupportTls(newValue.isSupportTls());
        }
        builder.transport(transport);
        return builder.build();
    }
}
