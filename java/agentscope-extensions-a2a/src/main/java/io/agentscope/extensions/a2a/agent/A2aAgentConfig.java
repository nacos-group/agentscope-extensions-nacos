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

package io.agentscope.extensions.a2a.agent;

import io.a2a.client.transport.spi.ClientTransport;
import io.a2a.client.transport.spi.ClientTransportConfig;
import io.a2a.spec.AgentCard;
import io.agentscope.extensions.a2a.agent.card.AgentCardProducer;
import io.agentscope.extensions.a2a.agent.card.FixedAgentCardProducer;

import java.util.HashMap;
import java.util.Map;

/**
 * Config of A2A Agent.
 *
 * @author xiweng.yy
 */
public record A2aAgentConfig(AgentCardProducer agentCardProducer, boolean adaptOldVersionA2aDateTimeSerialization,
                             Map<Class, ClientTransportConfig> clientTransports) {
    
    public static class A2aAgentConfigBuilder {
        
        private AgentCardProducer agentCardProducer;
        
        private boolean adaptOldVersionA2aDateTimeSerialization;
        
        private Map<Class, ClientTransportConfig> clientTransports;
        
        public A2aAgentConfigBuilder() {
            clientTransports = new HashMap<>();
        }
        
        /**
         * Fast build {@link FixedAgentCardProducer} and register to this config.
         *
         * @param agentCard agent card of target remote A2A Agent
         * @return the current {@link A2aAgentConfigBuilder} instance for chaining calls
         * @see #agentCardProducer()
         */
        public A2aAgentConfigBuilder agentCard(AgentCard agentCard) {
            this.agentCardProducer = FixedAgentCardProducer.builder().agentCard(agentCard).build();
            return this;
        }
        
        /**
         * Set {@link AgentCardProducer} to this config which will be used to generate {@link AgentCard} to
         * {@link io.a2a.client.Client}.
         *
         * <p> It can be extended to support more ways to generate {@link AgentCard}.
         *
         * @param agentCardProducer agent card producer of target remote A2A Agent
         * @return the current {@link A2aAgentConfigBuilder} instance for chaining calls
         */
        public A2aAgentConfigBuilder agentCardProducer(AgentCardProducer agentCardProducer) {
            this.agentCardProducer = agentCardProducer;
            return this;
        }
        
        /**
         * The old version of A2A server might use {@link java.time.LocalDateTime} and new version use
         * {@link java.time.OffsetDateTime}, which will cause deserialization error for timestamp.
         *
         * <p>If you want to use the old version of A2A server, you need to set this flag to true.
         *
         * @param adaptOldVersionA2aDateTimeSerialization true if you want to use the old version of A2A server, false
         *                                                otherwise.
         * @return the current {@link A2aAgentConfigBuilder} instance for chaining calls
         */
        public A2aAgentConfigBuilder adaptOldVersionA2aDateTimeSerialization(
                boolean adaptOldVersionA2aDateTimeSerialization) {
            this.adaptOldVersionA2aDateTimeSerialization = adaptOldVersionA2aDateTimeSerialization;
            return this;
        }
        
        /**
         * Add client transport configuration which will be used to
         * {@link io.a2a.client.ClientBuilder#withTransport(Class, ClientTransportConfig)}.
         *
         * @param clazz  the client transport implementation class
         * @param config the client transport configuration
         * @param <T>    the subtype of ClientTransport
         * @return the current {@link A2aAgentConfigBuilder} instance for chaining calls
         */
        public <T extends ClientTransport> A2aAgentConfigBuilder withTransport(Class<T> clazz,
                ClientTransportConfig<T> config) {
            this.clientTransports.put(clazz, config);
            return this;
        }
        
        public A2aAgentConfig build() {
            return new A2aAgentConfig(agentCardProducer, adaptOldVersionA2aDateTimeSerialization,
                    this.clientTransports);
        }
    }
}
