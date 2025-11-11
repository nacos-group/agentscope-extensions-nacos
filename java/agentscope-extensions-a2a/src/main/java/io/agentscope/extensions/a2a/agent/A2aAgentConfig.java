package io.agentscope.extensions.a2a.agent;

import io.a2a.spec.AgentCard;
import io.agentscope.extensions.a2a.agent.card.AgentCardProducer;
import io.agentscope.extensions.a2a.agent.card.FixedAgentCardProducer;

/**
 * Config of A2A Agent.
 *
 * @author xiweng.yy
 */
public record A2aAgentConfig(AgentCardProducer agentCardProducer, boolean adaptOldVersionA2aDateTimeSerialization) {
    
    public static class A2aAgentConfigBuilder {
        
        private AgentCardProducer agentCardProducer;
        
        private boolean adaptOldVersionA2aDateTimeSerialization;
        
        public A2aAgentConfigBuilder agentCard(AgentCard agentCard) {
            this.agentCardProducer = FixedAgentCardProducer.builder().agentCard(agentCard).build();
            return this;
        }
        
        public A2aAgentConfigBuilder agentCardProducer(AgentCardProducer agentCardProducer) {
            this.agentCardProducer = agentCardProducer;
            return this;
        }
        
        public A2aAgentConfigBuilder adaptOldVersionA2aDateTimeSerialization(
                boolean adaptOldVersionA2aDateTimeSerialization) {
            this.adaptOldVersionA2aDateTimeSerialization = adaptOldVersionA2aDateTimeSerialization;
            return this;
        }
        
        public A2aAgentConfig build() {
            return new A2aAgentConfig(agentCardProducer, adaptOldVersionA2aDateTimeSerialization);
        }
    }
}
