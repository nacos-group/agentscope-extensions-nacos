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
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.extensions.nacos.mcp.NacosMcpServerManager;
import io.agentscope.extensions.nacos.mcp.client.NacosMcpClientBuilder;
import io.agentscope.extensions.nacos.mcp.client.NacosMcpClientWrapper;
import io.agentscope.extensions.nacos.mcp.tool.NacosMcpToolBuilder;
import io.agentscope.extensions.nacos.mcp.tool.NacosToolkit;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * Run this example need to do Register MCP Server <a href="https://mcp.nacos.io/server/server9016">IP Location</a> into
 * Nacos.
 *
 * <p>Step follow:
 * <ul>
 *     <li>1. Access <a href="https://mcp.nacos.io/server/server9016">IP Location</a> by Browser</li>
 *     <li>2. Sign in and click `Create` Button to generate URL and apikey</li>
 *     <li>3. Copy the URL in page, such as `https://mcp.higress.ai/mcp-ip-query/.../sse`</li>
 *     <li>4. Access Nacos Console such as `http://localhost:8080` And enter `MCP List` page</li>
 *     <li>5. Create new MCP server with `name`:IP-Location, `Protocol Type`:sse, `HTTP to MCP`:disabled, `MCP Server Endpoint`:URL from step 3 and finally click `Auto import from MCP Server` in Tools block.</li>
 * </ul>
 */
public class McpDiscoveryWithNacosExample {
    
    private static final String USER_INPUT_PREFIX = "\u001B[34mYou>\u001B[0m ";
    
    private static final String AGENT_RESPONSE_PREFIX = "\u001B[32mAgent>\u001B[0m ";
    
    public static void main(String[] args) throws NacosException {
        AiService aiService = buildNacosAiClient();
        Toolkit toolkit = buildToolKitWay1(aiService);
        //        Toolkit toolkit = buildToolKitWay2(aiService);
        ReActAgent agent = ReActAgent.builder().name("IP Location Agent").sysPrompt(
                        "You are an IP location Agent. You can help users analyze the user's location based on their IP address. If users don't provide their IP, you should lead users to get their IP like use `curl ipinfo.io`.")
                .model(buildModel()).toolkit(toolkit).build();
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
    
    /**
     * The first way to discovery mcp tools and server by {@link NacosToolkit}.
     *
     * <p>
     * In this way, Nacos will auto-rebuild MCP Tools when MCP Server or MCP Tools specification changed or endpoints
     * changed. For these changed MCP servers and tools, {@link NacosToolkit} will remove the old MCP Tools in changed
     * MCP Server and full rebuild MCP Tools into {@link Toolkit}.
     * </p>
     *
     * <p>
     * In example, use public <a href="https://mcp.nacos.io/server/server9016">IP Location</a> tools. Before run this
     * example, please generate and register this MCP Server to Nacos.
     * </p>
     */
    private static Toolkit buildToolKitWay1(AiService aiService) {
        NacosMcpServerManager mcpServerManager = NacosMcpServerManager.from(aiService);
        NacosMcpClientWrapper mcpClientWrapper = NacosMcpClientBuilder.create("IP-Location", mcpServerManager).build();
        Toolkit toolkit = new NacosToolkit();
        toolkit.registerMcpClient(mcpClientWrapper).block();
        return toolkit;
    }
    
    /**
     * The second way to discovery mcp tools and server by
     * {@link io.agentscope.extensions.nacos.mcp.tool.NacosMcpTool}.
     *
     * <p>
     * In this way, Nacos will auto-rebuild MCP Tools when MCP Server or MCP Tools specification changed or endpoints
     * changed. Different with Way 1 {@link #buildToolKitWay1(AiService)}, This way only rebuild and replace metadata
     * and actual mcp server connection in {@link io.agentscope.extensions.nacos.mcp.tool.NacosMcpTool} not to do full
     * rebuild in {@link Toolkit}.
     * </p>
     *
     * <p>
     * In example, use public <a href="https://mcp.nacos.io/server/server9016">IP Location</a> tools. Before run this
     * example, please generate and register this MCP Server to Nacos.
     * </p>
     */
    private static Toolkit buildToolKitWay2(AiService aiService) {
        NacosMcpServerManager mcpServerManager = NacosMcpServerManager.from(aiService);
        NacosMcpClientWrapper mcpClientWrapper = NacosMcpClientBuilder.create("IP-Location", mcpServerManager).build();
        Toolkit toolkit = new Toolkit();
        NacosMcpToolBuilder.create(mcpClientWrapper).build().forEach(toolkit::registerTool);
        return toolkit;
    }
    
    private static Model buildModel() {
        String dashScopeApiKey = System.getenv("AI_DASHSCOPE_API_KEY");
        if (StringUtils.isEmpty(dashScopeApiKey)) {
            throw new RuntimeException("AI_DASHSCOPE_API_KEY should be set in environment variable.");
        }
        return DashScopeChatModel.builder().apiKey(dashScopeApiKey).modelName("qwen-max").stream(true)
                .enableThinking(true).build();
    }
    
    private static void startExample(ReActAgent agent) {
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
    
    private static Flux<String> processInput(ReActAgent agent, String input) {
        Msg msg = Msg.builder().role(MsgRole.USER).content(TextBlock.builder().text(input).build()).build();
        return agent.stream(msg).map(event -> {
            Msg message = event.getMessage();
            StringBuilder partText = new StringBuilder();
            message.getContent().stream().filter(block -> block instanceof TextBlock).map(block -> (TextBlock) block)
                    .forEach(block -> partText.append(block.getText()));
            return partText.toString();
        });
    }
}
