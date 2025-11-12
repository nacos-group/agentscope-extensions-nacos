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

package io.agentscope.extensions.runtime.a2a.registry.autoconfigure;

import io.a2a.spec.AgentCard;
import io.agentscope.extensions.runtime.a2a.registry.AgentRegistry;
import io.agentscope.extensions.runtime.a2a.registry.AgentRegistryService;
import io.agentscope.runtime.autoconfigure.DeployProperties;
import io.agentscope.runtime.protocol.a2a.configuration.AgentCardConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;

/**
 * The AutoConfiguration for A2A server registry.
 *
 * <p>If runner with A2A protocol enabled, try to do register AgentCard and AgentInterface to Registry.
 * <p>This AutoConfiguration should load after agentscope export AgentCard and DeployProperties.
 *
 * @author xiweng.yy
 */
@AutoConfiguration(after = AgentCardConfiguration.class, afterName = "io.agentscope.runtime.autoconfigure.A2aAutoConfiguration")
@ConditionalOnBean({AgentCard.class, DeployProperties.class})
public class A2aServerRegistryAutoConfiguration {
    
    @Bean
    @ConditionalOnBean(AgentRegistry.class)
    public AgentRegistryService agentRegistryService(AgentCard agentCard, AgentRegistry agentRegistry,
            DeployProperties deployProperties) {
        return new AgentRegistryService(agentRegistry, agentCard, deployProperties);
    }
    
}
