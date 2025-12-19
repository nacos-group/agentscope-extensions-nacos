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

package io.agentscope.extensions.nacos.example.config;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.runtime.adapters.agentscope.AgentScopeAgentHandler;
import io.agentscope.runtime.engine.schemas.AgentRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.List;

@Configuration
public class AgentScopeAgentConfiguration {
    
    @Bean
    public ReActAgent.Builder agentBuilder(DashScopeChatModel model) {
        return ReActAgent.builder().model(model).name("agentscope-a2a-example-agent").sysPrompt(
                "You are an example of A2A(Agent2Agent) Protocol Agent. You can answer some simple question according to your knowledge.");
    }
    
    @Bean
    @SuppressWarnings("unchecked")
    public AgentScopeAgentHandler agent(ReActAgent.Builder builder) {
        return new AgentScopeAgentHandler() {
            
            @Override
            public boolean isHealthy() {
                return true;
            }
            
            @Override
            public Flux<?> streamQuery(AgentRequest request, Object messages) {
                ReActAgent agent = builder.build();
                StreamOptions streamOptions = StreamOptions.builder()
                        .eventTypes(EventType.REASONING, EventType.TOOL_RESULT).incremental(true).build();
                if (messages instanceof List<?>) {
                    return agent.stream((List<Msg>) messages, streamOptions);
                } else if (messages instanceof Msg) {
                    return agent.stream((Msg) messages, streamOptions);
                } else {
                    Msg msg = Msg.builder().role(MsgRole.USER).build();
                    return agent.stream(msg, streamOptions);
                }
            }
            
            @Override
            public String getName() {
                return builder.build().getName();
            }
            
            @Override
            public String getDescription() {
                return builder.build().getDescription();
            }
        };
    }
}
