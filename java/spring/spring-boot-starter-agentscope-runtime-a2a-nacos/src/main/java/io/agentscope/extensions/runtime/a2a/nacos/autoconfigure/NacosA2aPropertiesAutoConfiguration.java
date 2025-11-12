/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.agentscope.extensions.runtime.a2a.nacos.autoconfigure;

import com.alibaba.nacos.api.ai.A2aService;
import com.alibaba.nacos.api.ai.AiFactory;
import com.alibaba.nacos.api.exception.NacosException;
import io.agentscope.extensions.runtime.a2a.nacos.condition.NacosA2aProtocolConfigNonExistCondition;
import io.agentscope.extensions.runtime.a2a.nacos.properties.NacosA2aProperties;
import io.agentscope.extensions.runtime.a2a.nacos.properties.NacosServerProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

/**
 * The AutoConfiguration for A2A Nacos Client.
 *
 * <p>Mutual exclusion with {@link NacosA2aProtocolConfigAutoConfiguration}, used by start runtime with
 * sprint-boot-starter-runtime-a2a.
 *
 * @author xiweng.yy
 */
@AutoConfiguration(before = NacosA2aRegistryAutoConfiguration.class)
@EnableConfigurationProperties({NacosA2aProperties.class, NacosServerProperties.class})
@ConditionalOnProperty(prefix = NacosA2aProperties.PREFIX, value = ".enabled", havingValue = "true", matchIfMissing = true)
@Conditional(NacosA2aProtocolConfigNonExistCondition.class)
public class NacosA2aPropertiesAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public A2aService a2aService(NacosServerProperties nacosServerProperties) throws NacosException {
        return AiFactory.createAiService(nacosServerProperties.getNacosProperties());
    }
    
}
