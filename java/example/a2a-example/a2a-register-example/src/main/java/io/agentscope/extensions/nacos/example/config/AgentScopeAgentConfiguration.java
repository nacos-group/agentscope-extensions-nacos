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
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.runtime.engine.agents.agentscope.AgentScopeAgent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentScopeAgentConfiguration {
    
    @Bean
    public ReActAgent.Builder agentBuilder(DashScopeChatModel model) {
        return ReActAgent.builder()
                .model(model)
                .name("agentscope-a2a-example-agent")
                .sysPrompt("You are an example of A2A(Agent2Agent) Protocol Agent. You can answer some simple question according to your knowledge.");
    }
    
    @Bean
    public AgentScopeAgent agent(ReActAgent.Builder builder) {
        return AgentScopeAgent.builder().agent(builder).build();
    }
}
