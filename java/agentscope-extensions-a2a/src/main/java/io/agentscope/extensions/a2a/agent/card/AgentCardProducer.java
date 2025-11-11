package io.agentscope.extensions.a2a.agent.card;

import io.a2a.spec.AgentCard;

/**
 * The producer for AgentCard of A2A.
 *
 * @author xiweng.yy
 */
public interface AgentCardProducer {
    
    /**
     * Produce AgentCard for target agent name.
     *
     * @param agentName name of agent
     * @return {@link AgentCard}
     */
    AgentCard produce(String agentName);
    
}
