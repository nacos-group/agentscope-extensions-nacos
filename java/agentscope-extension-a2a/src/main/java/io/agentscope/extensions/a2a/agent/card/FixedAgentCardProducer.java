package io.agentscope.extensions.a2a.agent.card;

import io.a2a.spec.AgentCard;

/**
 * Agent Card Producer for Fixed AgentCard.
 *
 * @author xiweng.yy
 */
public class FixedAgentCardProducer implements AgentCardProducer {
    
    private final AgentCard agentCard;
    
    private FixedAgentCardProducer(AgentCard agentCard) {
        this.agentCard = agentCard;
    }
    
    @Override
    public AgentCard produce(String agentName) {
        return agentCard;
    }
    
    public static FixedAgentCardProducerBuilder builder() {
        return new FixedAgentCardProducerBuilder();
    }
    
    public static class FixedAgentCardProducerBuilder {
        
        private AgentCard agentCard;
        
        public FixedAgentCardProducerBuilder agentCard(AgentCard agentCard) {
            this.agentCard = agentCard;
            return this;
        }
        
        public FixedAgentCardProducer build() {
            return new FixedAgentCardProducer(agentCard);
        }
    }
}
