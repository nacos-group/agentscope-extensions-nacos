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

import com.alibaba.nacos.api.ai.A2aService;
import com.alibaba.nacos.api.ai.AiFactory;
import com.alibaba.nacos.api.ai.constant.AiConstants;
import com.alibaba.nacos.api.ai.model.a2a.AgentCard;
import com.alibaba.nacos.api.ai.model.a2a.AgentEndpoint;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.exception.runtime.NacosRuntimeException;
import io.agentscope.extensions.nacos.a2a.utils.AgentCardConverterUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * AgentScope Extensions for Runtime Registry A2A AgentCard and A2A instance endpoint to Nacos.
 *
 * @author xiweng.yy
 */
public class NacosA2aRegistry {
    
    private static final Logger log = LoggerFactory.getLogger(NacosA2aRegistry.class);
    
    private final A2aService a2aService;
    
    public NacosA2aRegistry(Properties nacosProperties) throws NacosException {
        this(AiFactory.createAiService(nacosProperties));
    }
    
    public NacosA2aRegistry(A2aService a2aService) {
        this.a2aService = a2aService;
    }
    
    public void registerAgent(io.a2a.spec.AgentCard agentCard, NacosA2aRegistryProperties a2aProperties) {
        AgentCard nacosAgentCard = AgentCardConverterUtil.convertToNacosAgentCard(agentCard);
        try {
            tryReleaseAgentCard(nacosAgentCard, a2aProperties);
            registerEndpoint(nacosAgentCard, a2aProperties);
        } catch (NacosException e) {
            log.error("Register agent card {} to Nacos failed,", agentCard.name(), e);
            throw new NacosRuntimeException(e.getErrCode(), e.getErrMsg());
        }
    }
    
    private void tryReleaseAgentCard(AgentCard agentCard, NacosA2aRegistryProperties a2aProperties)
            throws NacosException {
        log.info("Register agent card {} to Nacos. ", agentCard.getName());
        a2aService.releaseAgentCard(agentCard, AiConstants.A2a.A2A_ENDPOINT_TYPE_SERVICE,
                a2aProperties.isSetAsLatest());
        log.info("Register agent card {} to Nacos successfully. ", agentCard.getName());
    }
    
    private void registerEndpoint(AgentCard agentCard, NacosA2aRegistryProperties a2aProperties) throws NacosException {
        AgentEndpoint endpoint = new AgentEndpoint();
        endpoint.setVersion(agentCard.getVersion());
        endpoint.setPath(a2aProperties.endpointPath());
        endpoint.setTransport(agentCard.getPreferredTransport());
        endpoint.setAddress(a2aProperties.endpointAddress());
        endpoint.setPort(a2aProperties.endpointPort());
        a2aService.registerAgentEndpoint(agentCard.getName(), endpoint);
    }
}
