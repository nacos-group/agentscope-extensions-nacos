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

package io.agentscope.extensions.nacos.mcp;

import com.alibaba.nacos.api.ai.AiFactory;
import com.alibaba.nacos.api.ai.AiService;
import com.alibaba.nacos.api.ai.listener.AbstractNacosMcpServerListener;
import com.alibaba.nacos.api.ai.listener.NacosMcpServerEvent;
import com.alibaba.nacos.api.ai.model.mcp.McpServerDetailInfo;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.exception.runtime.NacosRuntimeException;
import com.alibaba.nacos.common.utils.ConcurrentHashSet;
import com.alibaba.nacos.common.utils.JacksonUtils;
import com.alibaba.nacos.common.utils.StringUtils;
import io.agentscope.extensions.nacos.mcp.client.NacosMcpClientWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager of MCP Server discovery by Nacos.
 *
 * @author xiweng.yy
 */
public class NacosMcpServerManager {
    
    private static final Logger log = LoggerFactory.getLogger(NacosMcpServerManager.class);
    
    private final AiService aiService;
    
    private final Map<String, McpServerDetailInfo> mcpServerCaches;
    
    private final Map<String, McpServerListener> mcpServerListeners;
    
    private final Map<String, Set<NacosMcpClientWrapper>> subscribeMcpClients;
    
    public NacosMcpServerManager(AiService aiService) {
        this.aiService = aiService;
        this.mcpServerCaches = new ConcurrentHashMap<>(2);
        this.mcpServerListeners = new ConcurrentHashMap<>(2);
        this.subscribeMcpClients = new ConcurrentHashMap<>(2);
    }
    
    /**
     * Get MCP server detail info by MCP name.
     *
     * @param mcpName the name of MCP server
     * @return the MCP server detail info
     */
    public McpServerDetailInfo getMcpServer(String mcpName) {
        if (StringUtils.isBlank(mcpName)) {
            throw new NacosRuntimeException(NacosException.INVALID_PARAM, "mcpName can not be null or blank.");
        }
        if (mcpServerCaches.containsKey(mcpName)) {
            return mcpServerCaches.get(mcpName);
        }
        McpServerDetailInfo result = getAndSubscribe(mcpName);
        // If already put by listener, use listener put value
        return mcpServerCaches.computeIfAbsent(mcpName, name -> result);
    }
    
    /**
     * Register a client that subscribes to the MCP service
     *
     * @param mcpName   the name of the MCP service
     * @param mcpClient the MCP client wrapper instance
     */
    public void registerSubscribeMcpClient(String mcpName, NacosMcpClientWrapper mcpClient) {
        Set<NacosMcpClientWrapper> subscribeClients = subscribeMcpClients.computeIfAbsent(mcpName,
                name -> new ConcurrentHashSet<>());
        subscribeClients.add(mcpClient);
    }
    
    /**
     * Unregister a client that subscribes to the MCP service
     *
     * @param mcpName   the name of the MCP service
     * @param mcpClient the MCP client wrapper instance
     */
    public void unregisterSubscribeMcpClient(String mcpName, NacosMcpClientWrapper mcpClient) {
        Set<NacosMcpClientWrapper> subscribeClients = subscribeMcpClients.get(mcpName);
        if (null != subscribeClients) {
            subscribeClients.remove(mcpClient);
        }
    }
    
    private McpServerDetailInfo getAndSubscribe(String mcpName) {
        try {
            McpServerListener listener = mcpServerListeners.computeIfAbsent(mcpName, name -> new McpServerListener());
            return aiService.subscribeMcpServer(mcpName, listener);
        } catch (NacosException e) {
            throw new NacosRuntimeException(e.getErrCode(), e.getErrMsg(), e);
        }
    }
    
    /**
     * Create NacosMcpServerManager instance from properties.
     *
     * @param properties the properties used to create AiService
     * @return the created NacosMcpServerManager instance
     * @throws NacosException if failed to create AiService from properties
     */
    public static NacosMcpServerManager from(Properties properties) throws NacosException {
        return from(AiFactory.createAiService(properties));
    }
    
    /**
     * Create NacosMcpServerManager instance from Nacos Ai Client.
     *
     * @param aiService the Nacos Ai Client instance
     * @return the created NacosMcpServerManager instance
     */
    public static NacosMcpServerManager from(AiService aiService) {
        return new NacosMcpServerManager(aiService);
    }
    
    private class McpServerListener extends AbstractNacosMcpServerListener {
        
        @Override
        public void onEvent(NacosMcpServerEvent event) {
            if (log.isDebugEnabled()) {
                log.debug("MCP Server {} changed, new MCP Server Detail: {}", event.getMcpName(),
                        JacksonUtils.toJson(event.getMcpServerDetailInfo()));
            }
            mcpServerCaches.put(event.getMcpServerDetailInfo().getName(), event.getMcpServerDetailInfo());
            subscribeMcpClients.getOrDefault(event.getMcpName(), new HashSet<>())
                    .forEach(mcpClient -> mcpClient.refresh(event.getMcpServerDetailInfo()));
        }
    }
}
