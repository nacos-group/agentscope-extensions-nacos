package io.agentscope.extensions.nacos.mcp.tool;

import com.alibaba.nacos.api.ai.model.mcp.McpServerDetailInfo;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.ToolkitConfig;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import io.agentscope.extensions.nacos.mcp.client.NacosMcpClientWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Extension for {@link io.agentscope.core.tool.Toolkit}.
 *
 * <p>Support dynamic refresh tools when MCP Server in Nacos changed.
 * <p>Full replace {@link Toolkit} with same usages.
 *
 * <p>Example usage:
 * <pre>{@code
 *  McpClientWrapper agentscopeMcpClient = McpClientBuilder
 *      .create(...)
 *      .build();
 *  McpClientWrapper nacosMcpClient = NacosMcpClientBuilder
 *      .create(...)
 *      .build();
 *  // Use NacosToolkit same as {@link Toolkit}
 *  Toolkit toolkit = new NacosToolkit();
 *  // Both can register original agentscope MCP client and Nacos MCP client
 *  toolkit.registerMcpClient(agentscopeMcpClient);
 *  toolkit.registerMcpClient(nacosMcpClient);
 *  ReActAgent agent = new ReActAgent(..., toolkit, ...);
 *  agent.call()
 * }
 * </pre>
 *
 * @author xiweng.yy
 * @see io.agentscope.core.tool.Toolkit#registerMcpClient(McpClientWrapper)
 */
public class NacosToolkit extends Toolkit {
    
    private static final Logger log = LoggerFactory.getLogger(NacosToolkit.class);
    
    private static final McpClientInfo EMPTY_MCP_CLIENT_INFO = new McpClientInfo(null, null, null);
    
    private final Map<String, McpClientInfo> mcpClientInfos;
    
    public NacosToolkit() {
        this(ToolkitConfig.defaultConfig());
    }
    
    public NacosToolkit(ToolkitConfig config) {
        super(config);
        this.mcpClientInfos = new ConcurrentHashMap<>(2);
    }
    
    @Override
    public Mono<Void> registerMcpClient(McpClientWrapper mcpClientWrapper) {
        return this.registerMcpClient(mcpClientWrapper, null);
    }
    
    @Override
    public Mono<Void> registerMcpClient(McpClientWrapper mcpClientWrapper, List<String> enableTools) {
        return this.registerMcpClient(mcpClientWrapper, enableTools, null);
    }
    
    @Override
    public Mono<Void> registerMcpClient(McpClientWrapper mcpClientWrapper, List<String> enableTools,
            List<String> disableTools) {
        return this.registerMcpClient(mcpClientWrapper, enableTools, disableTools, null);
    }
    
    @Override
    public Mono<Void> registerMcpClient(McpClientWrapper mcpClientWrapper, List<String> enableTools,
            List<String> disableTools, String groupName) {
        return delegateRegisterMcpClient(mcpClientWrapper, enableTools, disableTools, groupName).doOnSuccess(
                unused -> cacheMcpClientInfo(mcpClientWrapper, enableTools, disableTools, groupName));
    }
    
    @Override
    public Mono<Void> removeMcpClient(String mcpClientName) {
        return delegateRemoveMcpClient(mcpClientName).doOnSuccess(unused -> mcpClientInfos.remove(mcpClientName));
    }
    
    private Mono<Void> delegateRegisterMcpClient(McpClientWrapper mcpClientWrapper, List<String> enableTools,
            List<String> disableTools, String groupName) {
        return super.registerMcpClient(mcpClientWrapper, enableTools, disableTools, groupName);
    }
    
    private Mono<Void> delegateRemoveMcpClient(String mcpClientName) {
        log.debug("Remove MCP client {} from Toolkit {}", mcpClientName, NacosToolkit.this);
        return super.removeMcpClient(mcpClientName);
    }
    
    private void cacheMcpClientInfo(McpClientWrapper mcpClientWrapper, List<String> enableTools,
            List<String> disableTools, String groupName) {
        if (mcpClientWrapper instanceof NacosMcpClientWrapper nacosMcpClient) {
            log.debug("Register Nacos MCP client {} to Toolkit {}", mcpClientWrapper.getName(), NacosToolkit.this);
            McpClientInfo mcpClientInfo = new McpClientInfo(groupName, enableTools, disableTools);
            mcpClientInfos.put(nacosMcpClient.getName(), mcpClientInfo);
            nacosMcpClient.registerRefreshHook(new ToolsRefresher());
        }
    }
    
    public class ToolsRefresher implements NacosMcpClientWrapper.RefreshHook {
        
        @Override
        public void postRefresh(McpServerDetailInfo mcpServer, NacosMcpClientWrapper mcpClient) {
            log.debug("Refresh Tools in Toolkit {} by Nacos MCP client {}", NacosToolkit.this, mcpClient.getName());
            McpClientInfo info = mcpClientInfos.getOrDefault(mcpClient.getName(), EMPTY_MCP_CLIENT_INFO);
            delegateRemoveMcpClient(mcpClient.getName()).then(Mono.defer(
                    (Supplier<Mono<?>>) () -> delegateRegisterMcpClient(mcpClient, info.enableTools(),
                            info.disableTools(), info.groupName()))).block();
        }
    }
    
    private record McpClientInfo(String groupName, List<String> enableTools, List<String> disableTools) {
    
    }
}
