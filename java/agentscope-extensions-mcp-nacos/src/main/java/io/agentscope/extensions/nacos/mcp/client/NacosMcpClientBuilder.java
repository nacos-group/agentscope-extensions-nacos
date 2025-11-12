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

package io.agentscope.extensions.nacos.mcp.client;

import com.alibaba.nacos.common.utils.StringUtils;
import io.agentscope.extensions.nacos.mcp.NacosMcpServerManager;

/**
 * Builder for creating {@link NacosMcpClientWrapper} which extended by
 * {@link io.agentscope.core.tool.mcp.McpClientWrapper}.
 *
 * <p>Supports build McpClientWrapper from specification from Nacos MCP Registry.
 * <p>Example usage:
 * <pre>{@code
 *  NacosMcpServerManager mcpServerManager = NacosMcpServerManager
 *      .from(new Properties());
 *
 *  // Default: build Async Client and initialize when build.
 *  NacosMcpClientWrapper mcpClient = NacosMcpClientBuilder
 *      .create("example-mcp-server", mcpServerManager)
 *      .build();
 *
 *  // Delay Initialize Client.
 *  NacosMcpClientWrapper mcpClient = NacosMcpClientBuilder
 *      .create("example-mcp-server", mcpServerManager)
 *      .delayInitialize(true)
 *      .build();
 *  mcpClient.initialize();
 *
 *  // Build Sync Client.
 *  NacosMcpClientWrapper mcpClient = NacosMcpClientBuilder
 *      .create("example-mcp-server", mcpServerManager)
 *      .asyncClient(false)
 *      .build();
 * }</pre>
 *
 * @see NacosMcpServerManager
 * @author xiweng.yy
 */
public class NacosMcpClientBuilder {
    
    private final String mcpServerName;
    
    private final NacosMcpServerManager mcpServerManager;
    
    private boolean asyncClient;
    
    private boolean delayInitialize;
    
    private NacosMcpClientBuilder(String mcpServerName, NacosMcpServerManager mcpServerManager) {
        this.mcpServerName = mcpServerName;
        this.mcpServerManager = mcpServerManager;
        this.delayInitialize = false;
        this.asyncClient = true;
    }
    
    public static NacosMcpClientBuilder create(String mcpServerName, NacosMcpServerManager mcpServerManager) {
        return new NacosMcpClientBuilder(mcpServerName, mcpServerManager);
    }
    
    public NacosMcpClientBuilder asyncClient(boolean asyncClient) {
        this.asyncClient = asyncClient;
        return this;
    }
    
    public NacosMcpClientBuilder delayInitialize(boolean delayInitialize) {
        this.delayInitialize = delayInitialize;
        return this;
    }
    
    public NacosMcpClientWrapper build() {
        if (StringUtils.isBlank(mcpServerName)) {
            throw new IllegalArgumentException("Mcp server name can not be blank.");
        }
        if (null == mcpServerManager) {
            throw new IllegalArgumentException("Mcp server manager can not be null.");
        }
        NacosMcpClientWrapper result = new NacosMcpClientWrapper(asyncClient,
                mcpServerManager.getMcpServer(mcpServerName), new ClientLifecycleCallback());
        if (!delayInitialize) {
            result.initialize().then().block();
        }
        return result;
    }
    
    /**
     * Callback class for handling client lifecycle events.
     * Registers and unregisters MCP clients with the server manager based on client lifecycle.
     */
    class ClientLifecycleCallback {
        
        /**
         * Called when the MCP client is initialized.
         * Registers the client with the server manager.
         *
         * @param mcpClientWrapper the MCP client wrapper being initialized
         */
        void onInitialize(NacosMcpClientWrapper mcpClientWrapper) {
            mcpServerManager.registerSubscribeMcpClient(mcpServerName, mcpClientWrapper);
        }
        
        /**
         * Called when the MCP client is closed.
         * Unregisters the client from the server manager.
         *
         * @param mcpClientWrapper the MCP client wrapper being closed
         */
        void onClose(NacosMcpClientWrapper mcpClientWrapper) {
            mcpServerManager.unregisterSubscribeMcpClient(mcpServerName, mcpClientWrapper);
        }
    }
}
