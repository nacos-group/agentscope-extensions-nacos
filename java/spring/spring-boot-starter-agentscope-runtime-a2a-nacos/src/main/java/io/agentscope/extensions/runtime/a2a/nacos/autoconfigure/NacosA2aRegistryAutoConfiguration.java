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

package io.agentscope.extensions.runtime.a2a.nacos.autoconfigure;

import com.alibaba.nacos.api.ai.A2aService;
import io.agentscope.extensions.runtime.a2a.nacos.registry.NacosA2aRegistry;
import io.agentscope.extensions.runtime.a2a.nacos.properties.NacosA2aProperties;
import io.agentscope.extensions.runtime.a2a.nacos.registry.NacosAgentRegistry;
import io.agentscope.extensions.runtime.a2a.registry.autoconfigure.A2aServerRegistryAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;

/**
 * The AutoConfiguration for A2A Nacos registry.
 *
 * <p>If Nacos Client and A2A Properties bean exist, will autoconfigure
 * {@link io.agentscope.extensions.runtime.a2a.registry.AgentRegistry} for Nacos A2A Registry.
 *
 * @author xiweng.yy
 */
@AutoConfiguration(before = A2aServerRegistryAutoConfiguration.class)
@ConditionalOnBean({A2aService.class, NacosA2aProperties.class})
public class NacosA2aRegistryAutoConfiguration {
    
    @Bean
    public NacosA2aRegistry nacosA2aRegistry(A2aService a2aService) {
        return new NacosA2aRegistry(a2aService);
    }
    
    @Bean
    public NacosAgentRegistry nacosAgentRegistry(NacosA2aRegistry nacosA2aRegistry,
            NacosA2aProperties nacosA2aProperties) {
        return new NacosAgentRegistry(nacosA2aRegistry, nacosA2aProperties);
    }
    
}
