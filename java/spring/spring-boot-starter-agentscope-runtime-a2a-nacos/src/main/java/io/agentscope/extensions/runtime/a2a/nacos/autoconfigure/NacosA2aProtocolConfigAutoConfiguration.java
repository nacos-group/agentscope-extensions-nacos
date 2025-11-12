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
import com.alibaba.nacos.api.ai.AiFactory;
import com.alibaba.nacos.api.exception.NacosException;
import io.agentscope.extensions.runtime.a2a.nacos.NacosA2aProtocolConfig;
import io.agentscope.extensions.runtime.a2a.nacos.condition.NacosA2aProtocolConfigExistCondition;
import io.agentscope.extensions.runtime.a2a.nacos.properties.NacosA2aProperties;
import io.agentscope.extensions.runtime.a2a.nacos.utils.NacosBeanUtil;
import io.agentscope.runtime.protocol.ProtocolConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

/**
 * The AutoConfiguration for A2A Nacos Client and properties.
 *
 * <p>Mutual exclusion with {@link NacosA2aPropertiesAutoConfiguration}, used by start runtime with
 * {@link io.agentscope.runtime.LocalDeployManager}.
 *
 * @author xiweng.yy
 */
@AutoConfiguration(before = NacosA2aRegistryAutoConfiguration.class)
@Conditional(NacosA2aProtocolConfigExistCondition.class)
public class NacosA2aProtocolConfigAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public A2aService a2aService(ObjectProvider<ProtocolConfig> protocolConfigs) throws NacosException {
        NacosA2aProtocolConfig nacosA2aProtocolConfig = NacosBeanUtil.getNacosA2aProtocolConfig(protocolConfigs);
        return AiFactory.createAiService(nacosA2aProtocolConfig.getNacosProperties());
    }
    
    @Bean
    @ConditionalOnMissingBean
    public NacosA2aProperties nacosA2aProperties(ObjectProvider<ProtocolConfig> protocolConfigs) {
        NacosA2aProtocolConfig nacosA2aProtocolConfig = NacosBeanUtil.getNacosA2aProtocolConfig(protocolConfigs);
        NacosA2aProperties nacosA2aProperties = new NacosA2aProperties();
        nacosA2aProperties.setRegisterAsLatest(nacosA2aProtocolConfig.isRegisterAsLatest());
        return nacosA2aProperties;
    }
    
}
