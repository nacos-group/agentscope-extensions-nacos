package io.agentscope.extensions.nacos.mcp.tool;

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
 *
 * @author xiweng.yy
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
            nacosMcpClient.registerToolkitRefresher(NacosToolkit.this, new ToolsRefresher());
        }
    }
    
    public class ToolsRefresher {
        
        public void doRefresh(NacosMcpClientWrapper nacosMcpClient) {
            log.debug("Refresh Tools in Toolkit {} by Nacos MCP client {}", NacosToolkit.this,
                    nacosMcpClient.getName());
            McpClientInfo info = mcpClientInfos.getOrDefault(nacosMcpClient.getName(), EMPTY_MCP_CLIENT_INFO);
            delegateRemoveMcpClient(nacosMcpClient.getName()).then(Mono.defer(
                    (Supplier<Mono<?>>) () -> delegateRegisterMcpClient(nacosMcpClient, info.enableTools(),
                            info.disableTools(), info.groupName()))).block();
        }
    }
    
    private record McpClientInfo(String groupName, List<String> enableTools, List<String> disableTools) {
    
    }
}
