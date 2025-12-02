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

package io.agentscope.extensions.nacos.mcp.tool;

import com.alibaba.nacos.api.ai.model.mcp.McpServerDetailInfo;
import com.alibaba.nacos.api.ai.model.mcp.McpTool;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.exception.runtime.NacosRuntimeException;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.ToolCallParam;
import io.agentscope.extensions.nacos.mcp.client.NacosMcpClientWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Extension Implementation of {@link io.agentscope.core.tool.mcp.McpTool} for Nacos.
 *
 * <p>This Mcp Tool supported auto-updated from Nacos when MCP Server or Tools changed.
 * <p>This Mcp Tool will get Tool spec from Nacos according to input tool name and Nacos MCP client.
 *
 * @author xiweng.yy
 */
public class NacosMcpTool implements AgentTool {
    
    private static final Logger log = LoggerFactory.getLogger(NacosMcpTool.class);
    
    private final String toolName;
    
    private final NacosMcpClientWrapper mcpClient;
    
    private io.agentscope.core.tool.mcp.McpTool mcpTool;
    
    public NacosMcpTool(String toolName, NacosMcpClientWrapper mcpClient) {
        this.toolName = toolName;
        this.mcpClient = mcpClient;
        this.mcpTool = transform(getMcpTool(mcpClient.getMcpServer()));
        this.mcpClient.registerRefreshHook((mcpServer, mcpClient1) -> {
            log.debug("Refresh Tool {} by Nacos MCP client {}", toolName, mcpClient1.getName());
            this.mcpTool = transform(getMcpTool(mcpServer));
        });
    }
    
    @Override
    public String getName() {
        return this.toolName;
    }
    
    @Override
    public String getDescription() {
        return mcpTool.getDescription();
    }
    
    @Override
    public Map<String, Object> getParameters() {
        return mcpTool.getParameters();
    }
    
    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        return mcpTool.callAsync(param);
    }
    
    private McpTool getMcpTool(McpServerDetailInfo mcpServer) {
        return mcpServer.getToolSpec().getTools().stream().filter(mcpTool -> getName().equals(mcpTool.getName()))
                .findFirst().orElseThrow(() -> new NacosRuntimeException(NacosException.NOT_FOUND,
                        String.format("tool %s not found.", getName())));
    }
    
    private io.agentscope.core.tool.mcp.McpTool transform(McpTool mcpTool) {
        return new io.agentscope.core.tool.mcp.McpTool(this.toolName, mcpTool.getDescription(),
                mcpTool.getInputSchema(), mcpClient);
    }
}
