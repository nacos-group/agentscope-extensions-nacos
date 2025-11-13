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

package io.agentscope.extensions.nacos.a2a.discovery;

import com.alibaba.nacos.api.ai.AiFactory;
import com.alibaba.nacos.api.ai.AiService;
import com.alibaba.nacos.api.ai.listener.AbstractNacosAgentCardListener;
import com.alibaba.nacos.api.ai.listener.NacosAgentCardEvent;
import com.alibaba.nacos.api.ai.model.a2a.AgentCardDetailInfo;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.exception.runtime.NacosRuntimeException;
import io.a2a.spec.AgentCard;
import io.agentscope.extensions.a2a.agent.card.AgentCardProducer;
import io.agentscope.extensions.nacos.a2a.utils.AgentCardConverterUtil;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent Card Producer from Nacos A2A Registry.
 *
 * <p>Example Usage:
 * <pre>{@code
 *  Properties properties = new Properties();
 *  properties.put(PropertyKeyConst.SERVER_ADDR, "localhost:8848");
 *  // put other Nacos server properties
 *  // create NacosAgentCardProducer with new Nacos Client
 *  NacosAgentCardProducer nacosAgentCardProducer = new NacosAgentCardProducer(properties);
 *  // Equals like:
 *  // AiService a2aService = AiFactory.createAiService(properties);
 *  // NacosAgentCardProducer nacosAgentCardProducer = new NacosAgentCardProducer(a2aService)
 *
 *  // Or Reuse Nacos Client
 *  AiService a2aService = getAiServiceFromCacheOrOtherComponent();
 *  NacosAgentCardProducer nacosAgentCardProducer = new NacosAgentCardProducer(a2aService);
 *
 *  // Then new A2aAgent
 *  A2aAgentConfig a2aAgentConfig = A2aAgentConfig.builder().agentCardProducer(nacosAgentCardProducer).build()
 *  A2aAgent a2aAgent = new A2aAgent("remote-agent-name", a2aAgentConfig);
 * }</pre>
 *
 * @see io.agentscope.extensions.a2a.agent.A2aAgent
 * @see io.agentscope.extensions.a2a.agent.A2aAgentConfig.A2aAgentConfigBuilder
 * @author xiweng.yy
 */
public class NacosAgentCardProducer implements AgentCardProducer {
    
    private final AiService aiService;
    
    private final Map<String, AgentCard> agentCardCaches;
    
    private final Map<String, AgentCardUpdater> agentCardUpdaters;
    
    public NacosAgentCardProducer(Properties properties) throws NacosException {
        this(AiFactory.createAiService(properties));
    }
    
    public NacosAgentCardProducer(AiService aiService) {
        this.aiService = aiService;
        this.agentCardCaches = new ConcurrentHashMap<>(2);
        this.agentCardUpdaters = new ConcurrentHashMap<>(2);
    }
    
    @Override
    public AgentCard produce(String agentName) {
        if (agentCardCaches.containsKey(agentName)) {
            return agentCardCaches.get(agentName);
        }
        AgentCard result = getAndSubscribe(agentName);
        // If already put by listener, use listener put value
        return agentCardCaches.computeIfAbsent(agentName, name -> result);
    }
    
    private AgentCard getAndSubscribe(String agentName) {
        try {
            AgentCardUpdater updater = agentCardUpdaters.computeIfAbsent(agentName, name -> new AgentCardUpdater());
            AgentCardDetailInfo agentCardDetailInfo = aiService.subscribeAgentCard(agentName, updater);
            return AgentCardConverterUtil.convertToA2aAgentCard(agentCardDetailInfo);
        } catch (NacosException e) {
            throw new NacosRuntimeException(e.getErrCode(), e.getErrMsg(), e);
        }
    }
    
    private class AgentCardUpdater extends AbstractNacosAgentCardListener {
        
        @Override
        public void onEvent(NacosAgentCardEvent event) {
            agentCardCaches.put(event.getAgentName(),
                    AgentCardConverterUtil.convertToA2aAgentCard(event.getAgentCard()));
        }
    }
}
