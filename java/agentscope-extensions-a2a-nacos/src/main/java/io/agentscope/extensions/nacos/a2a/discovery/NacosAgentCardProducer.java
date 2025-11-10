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
