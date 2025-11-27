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

import com.alibaba.nacos.api.ai.constant.AiConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * Properties for A2A AgentCard and Endpoint registry to Nacos.
 *
 * @author xiweng.yy
 */
public record NacosA2aRegistryProperties(boolean isSetAsLatest, boolean enabledRegisterEndpoint,
                                         Map<String, NacosA2aRegistryTransportProperties> transportProperties) {
    
    public NacosA2aRegistryProperties(boolean isSetAsLatest, boolean enabledRegisterEndpoint) {
        this(isSetAsLatest, enabledRegisterEndpoint, new HashMap<>());
    }
    
    /**
     * For support old version which only support DEFAULT transport.
     *
     * @deprecated in 1.0.0, please use {@link NacosA2aRegistryTransportProperties#builder()} and
     * {@link #addTransport(NacosA2aRegistryTransportProperties)}
     */
    @Deprecated
    public NacosA2aRegistryProperties(boolean isSetAsLatest, String endpointAddress, int endpointPort,
            String endpointPath) {
        this(isSetAsLatest, true);
        NacosA2aRegistryTransportProperties defaultJsonRpcTransport = NacosA2aRegistryTransportProperties.builder()
                .endpointAddress(endpointAddress).endpointPort(endpointPort).endpointPath(endpointPath)
                .transport(AiConstants.A2a.A2A_ENDPOINT_DEFAULT_TRANSPORT).build();
        transportProperties.put(AiConstants.A2a.A2A_ENDPOINT_DEFAULT_TRANSPORT, defaultJsonRpcTransport);
    }
    
    public void addTransport(NacosA2aRegistryTransportProperties transport) {
        transportProperties.put(transport.transport(), transport);
    }
}
