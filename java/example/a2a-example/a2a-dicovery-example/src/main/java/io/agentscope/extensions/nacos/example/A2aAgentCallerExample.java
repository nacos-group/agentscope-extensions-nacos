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

package io.agentscope.extensions.nacos.example;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.ai.AiFactory;
import com.alibaba.nacos.api.ai.AiService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.common.utils.StringUtils;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.extensions.a2a.agent.A2aAgent;
import io.agentscope.extensions.a2a.agent.card.AgentCardResolver;
import io.agentscope.extensions.nacos.a2a.discovery.NacosAgentCardResolver;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

public class A2aAgentCallerExample {
    
    private static final String USER_INPUT_PREFIX = "\u001B[34mYou>\u001B[0m ";
    
    private static final String AGENT_RESPONSE_PREFIX = "\u001B[32mAgent>\u001B[0m ";
    
    public static void main(String[] args) throws NacosException {
        AiService aiService = buildNacosAiClient();
        AgentCardResolver agentCardResolver = new NacosAgentCardResolver(aiService);
        A2aAgent agent = new A2aAgent("agentscope-a2a-example-agent", agentCardResolver);
        startExample(agent);
    }
    
    private static AiService buildNacosAiClient() throws NacosException {
        String nacosServerAddress = "localhost:8848";
        if (StringUtils.isNotEmpty(System.getenv("NACOS_SERVER_ADDRESS"))) {
            nacosServerAddress = System.getenv("NACOS_SERVER_ADDRESS");
        }
        String nacosUserName = "nacos";
        if (StringUtils.isNotEmpty(System.getenv("NACOS_USERNAME"))) {
            nacosUserName = System.getenv("NACOS_USERNAME");
        }
        String nacosPassword = "nacos";
        if (StringUtils.isNotEmpty(System.getenv("NACOS_PASSWORD"))) {
            nacosPassword = System.getenv("NACOS_PASSWORD");
        }
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, nacosServerAddress);
        properties.put(PropertyKeyConst.USERNAME, nacosUserName);
        properties.put(PropertyKeyConst.PASSWORD, nacosPassword);
        return AiFactory.createAiService(properties);
    }
    
    private static void startExample(A2aAgent agent) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                // 用户输入提示
                System.out.print(USER_INPUT_PREFIX);
                String input = reader.readLine();
                
                // 退出条件检测
                if (input == null || input.trim().equalsIgnoreCase("exit") || input.trim().equalsIgnoreCase("quit")) {
                    System.out.println(AGENT_RESPONSE_PREFIX + "Bye！");
                    break;
                }
                
                System.out.println(AGENT_RESPONSE_PREFIX + "I have received your question: " + input);
                
                // 流式输出带前缀
                System.out.print(AGENT_RESPONSE_PREFIX);
                
                // 处理输入并获取响应
                processInput(agent, input).doOnNext(System.out::print).then().block();
                
                System.out.println();  // 换行分隔
            }
        } catch (IOException e) {
            System.err.println("input error: " + e.getMessage());
        }
    }
    
    private static Flux<String> processInput(A2aAgent agent, String input) {
        Msg msg = Msg.builder().role(MsgRole.USER).content(TextBlock.builder().text(input).build()).build();
        return agent.stream(msg).map(event -> {
            if (event.isLast()) {
                // The last message is whole artifact message result, which has been solved and print in before event handle.
                // Weather need to handle the last message, depends on the use case.
                return "";
            }
            Msg message = event.getMessage();
            StringBuilder partText = new StringBuilder();
            message.getContent().stream().filter(block -> block instanceof TextBlock).map(block -> (TextBlock) block)
                    .forEach(block -> partText.append(block.getText()));
            return partText.toString();
        });
    }
}
