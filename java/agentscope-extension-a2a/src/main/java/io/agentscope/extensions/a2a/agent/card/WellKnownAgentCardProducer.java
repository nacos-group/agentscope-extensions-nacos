package io.agentscope.extensions.a2a.agent.card;

import io.a2a.A2A;
import io.a2a.spec.AgentCard;

import java.util.Map;

/**
 * Agent Card Producer from well known url.
 *
 * @author xiweng.yy
 */
public class WellKnownAgentCardProducer implements AgentCardProducer {
    
    private final String wellKnownUrl;
    
    private final String relativeCardPath;
    
    private final Map<String, String> authHeaders;
    
    public WellKnownAgentCardProducer(String wellKnownUrl, String relativeCardPath, Map<String, String> authHeaders) {
        this.wellKnownUrl = wellKnownUrl;
        this.relativeCardPath = relativeCardPath;
        this.authHeaders = authHeaders;
    }
    
    @Override
    public AgentCard produce(String agentName) {
        return A2A.getAgentCard(wellKnownUrl, relativeCardPath, authHeaders);
    }
}
