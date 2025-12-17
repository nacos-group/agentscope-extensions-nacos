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

package io.agentscope.extensions.a2a.nacos.registry;

import com.alibaba.nacos.api.ai.AiFactory;
import com.alibaba.nacos.api.ai.AiService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.client.ai.NacosAiService;
import io.agentscope.core.a2a.server.registry.AgentRegistry;
import io.agentscope.extensions.a2a.nacos.registry.constants.Constants;
import io.agentscope.extensions.a2a.nacos.registry.properties.NacosA2aProperties;
import io.agentscope.extensions.a2a.nacos.registry.properties.NacosServerProperties;
import io.agentscope.extensions.nacos.a2a.registry.NacosA2aRegistryProperties;
import io.agentscope.extensions.nacos.a2a.registry.NacosAgentRegistry;
import io.agentscope.spring.boot.a2a.AgentscopeA2aAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * The AutoConfiguration for A2A Nacos registry.
 *
 * <p>If Nacos Client and A2A Properties bean exist, will autoconfigure
 * {@link AgentRegistry} for Nacos A2A Registry.
 *
 * @author xiweng.yy
 */
@AutoConfiguration(before = AgentscopeA2aAutoConfiguration.class)
@EnableConfigurationProperties({NacosServerProperties.class, NacosA2aProperties.class})
@ConditionalOnClass(NacosAiService.class)
@ConditionalOnProperty(
        prefix = Constants.NACOS_PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class NacosA2aRegistryAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public AiService a2aService(NacosServerProperties nacosServerProperties) throws NacosException {
        return AiFactory.createAiService(nacosServerProperties.getNacosProperties());
    }
    
    @Bean
    public AgentRegistry nacosAgentRegistry(AiService a2aService, NacosA2aProperties nacosA2aProperties) {
        NacosAgentRegistry.Builder builder = NacosAgentRegistry.builder(a2aService);
        builder.nacosA2aProperties(buildNacosA2aProperties(nacosA2aProperties));
        return builder.build();
    }
    
    private NacosA2aRegistryProperties buildNacosA2aProperties(NacosA2aProperties nacosA2aProperties) {
        return NacosA2aRegistryProperties.builder().setAsLatest(nacosA2aProperties.isRegisterAsLatest())
                .enabledRegisterEndpoint(nacosA2aProperties.isEnabledRegisterEndpoint())
                .overwritePreferredTransport(nacosA2aProperties.getOverwritePreferredTransport()).build();
    }
    
}
