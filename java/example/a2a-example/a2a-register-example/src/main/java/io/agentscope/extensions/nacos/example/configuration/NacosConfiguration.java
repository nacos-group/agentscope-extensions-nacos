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

package io.agentscope.extensions.nacos.example.configuration;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.ai.AiFactory;
import com.alibaba.nacos.api.ai.AiService;
import com.alibaba.nacos.api.exception.NacosException;
import io.agentscope.core.a2a.server.registry.AgentRegistry;
import io.agentscope.extensions.nacos.a2a.registry.NacosAgentRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * @author xiweng.yy
 */
@Configuration
public class NacosConfiguration {
    
    @Value("${NACOS_SERVER_ADDR:127.0.0.1:8848}")
    private String nacosServerAddr;
    
    @Value("${NACOS_NAMESPACE_ID:public}")
    private String nacosNamespaceId;
    
    @Value("${NACOS_USERNAME:nacos}")
    private String nacosUsername;
    
    @Value("${NACOS_PASSWORD:nacos}")
    private String nacosPassword;
    
    @Bean
    public AgentRegistry nacosAgentRegistry() throws NacosException {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, nacosServerAddr);
        properties.put(PropertyKeyConst.NAMESPACE, nacosNamespaceId);
        properties.put(PropertyKeyConst.USERNAME, nacosUsername);
        properties.put(PropertyKeyConst.PASSWORD, nacosPassword);
        AiService aiService = AiFactory.createAiService(properties);
        return NacosAgentRegistry.builder(aiService).build();
    }
}
