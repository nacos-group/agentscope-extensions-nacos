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

import com.alibaba.nacos.common.utils.StringUtils;
import io.agentscope.core.model.DashScopeChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DashScopeConfiguration {
    
    @Value("${AI_DASHSCOPE_API_KEY}")
    private String dashScopeApiKey;
    
    @Bean
    public DashScopeChatModel dashScopeChatModel() {
        if (StringUtils.isEmpty(dashScopeApiKey)) {
            throw new IllegalStateException(
                    "DashScope API Key is empty, please set environment variable `AI_DASHSCOPE_API_KEY`");
        }
        return DashScopeChatModel.builder().apiKey(dashScopeApiKey).modelName("qwen-max").stream(true)
                .enableThinking(true).build();
    }
}
