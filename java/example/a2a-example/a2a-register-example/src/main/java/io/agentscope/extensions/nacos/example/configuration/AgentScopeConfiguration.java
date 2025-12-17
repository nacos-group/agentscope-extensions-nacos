/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.agentscope.extensions.nacos.example.configuration;

import io.a2a.spec.TransportProtocol;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.a2a.server.AgentScopeA2aServer;
import io.agentscope.core.a2a.server.card.ConfigurableAgentCard;
import io.agentscope.core.a2a.server.registry.AgentRegistry;
import io.agentscope.core.a2a.server.transport.DeploymentProperties;
import io.agentscope.core.model.DashScopeChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @author xiweng.yy
 */
@Configuration
public class AgentScopeConfiguration {
    
    @Value("${AI_DASHSCOPE_API_KEY}")
    private String dashScopeApiKey;
    
    @Bean
    public ReActAgent.Builder agentBuilder() {
        return ReActAgent.builder().name("agentscope-a2a-example-agent").description(
                        "You are an example of A2A(Agent2Agent) Protocol Agent. You can answer some simple question according to your knowledge.")
                .model(model());
    }
    
    private DashScopeChatModel model() {
        if (!StringUtils.hasLength(dashScopeApiKey)) {
            throw new IllegalStateException(
                    "DashScope API Key is empty, please set environment variable" + " `AI_DASHSCOPE_API_KEY`");
        }
        return DashScopeChatModel.builder().apiKey(dashScopeApiKey).modelName("qwen-max").stream(true)
                .enableThinking(true).build();
    }
    
    @Bean
    public AgentScopeA2aServer agentScopeA2aServer(ReActAgent.Builder agentBuilder,
            List<AgentRegistry> agentRegistries) {
        AgentScopeA2aServer.Builder builder = AgentScopeA2aServer.builder(agentBuilder);
        builder.deploymentProperties(new DeploymentProperties.Builder().port(8888).build());
        //        builder.agentCard(agentCard());
        agentRegistries.forEach(builder::withAgentRegistry);
        return builder.build();
    }
    
    private ConfigurableAgentCard agentCard() {
        return new ConfigurableAgentCard.Builder().url("http://127.0.0.1:8888").version("1.0.0")
                .defaultInputModes(List.of("text")).defaultOutputModes(List.of("text")).skills(List.of())
                .preferredTransport(TransportProtocol.JSONRPC.name()).build();
    }
}
