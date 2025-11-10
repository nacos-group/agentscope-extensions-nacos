package io.agentscope.extensions.nacos.mcp.client;

import com.alibaba.nacos.api.ai.constant.AiConstants;
import com.alibaba.nacos.api.ai.model.mcp.McpEndpointInfo;
import com.alibaba.nacos.api.ai.model.mcp.McpServerDetailInfo;
import com.alibaba.nacos.api.ai.model.mcp.McpTool;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.exception.runtime.NacosRuntimeException;
import com.alibaba.nacos.common.utils.StringUtils;
import io.agentscope.core.tool.mcp.McpClientBuilder;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import io.agentscope.extensions.nacos.mcp.tool.NacosToolkit;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Extension for {@link io.agentscope.core.tool.mcp.McpClientWrapper} for Nacos.
 *
 * <p>Supports dynamic refresh actual MCP Client when MCP Server Endpoints changed.
 *
 * @author xiweng.yy
 * @see NacosMcpClientBuilder
 * @see #refresh(McpServerDetailInfo).
 */
public class NacosMcpClientWrapper extends McpClientWrapper {
    
    private static final Logger log = LoggerFactory.getLogger(NacosMcpClientWrapper.class);
    
    private final boolean asyncClient;
    
    private McpServerDetailInfo mcpServer;
    
    private McpClientWrapper mcpClient;
    
    private final NacosMcpClientBuilder.ClientLifecycleCallback lifecycleCallback;
    
    private final Map<NacosToolkit, NacosToolkit.ToolsRefresher> subscriberToolkits;
    
    NacosMcpClientWrapper(boolean asyncClient, McpServerDetailInfo mcpServer,
            NacosMcpClientBuilder.ClientLifecycleCallback lifecycleCallback) {
        super(mcpServer.getName());
        this.asyncClient = asyncClient;
        this.mcpServer = mcpServer;
        this.lifecycleCallback = lifecycleCallback;
        this.subscriberToolkits = new ConcurrentHashMap<>(2);
    }
    
    @Override
    public Mono<Void> initialize() {
        if (isInitialized()) {
            return Mono.empty();
        }
        
        log.info("Initializing Nacos MCP client: {} with Async: {}", name, this.asyncClient);
        
        return rebuildMcpClient(mcpServer).doOnNext(client -> this.mcpClient = client)
                .then(Mono.defer((Supplier<Mono<?>>) () -> {
                    Mono<Void> result = mcpClient.initialize();
                    initialized = true;
                    lifecycleCallback.onInitialize(this);
                    return result;
                })).then().doOnError(error -> this.mcpClient = null);
    }
    
    @Override
    public Mono<List<McpSchema.Tool>> listTools() {
        Map<String, McpTool> toolsInNacos = mcpServer.getToolSpec().getTools().stream()
                .collect(Collectors.toMap(McpTool::getName, mcpTool -> mcpTool));
        return this.mcpClient.listTools()
                .map(tools -> tools.stream().map(tool -> refreshToolSpec(tool, toolsInNacos)).toList());
    }
    
    @Override
    public Mono<McpSchema.CallToolResult> callTool(String toolName, Map<String, Object> arguments) {
        return this.mcpClient.callTool(toolName, arguments);
    }
    
    @Override
    public void close() {
        this.lifecycleCallback.onClose(this);
        this.subscriberToolkits.clear();
        this.mcpClient.close();
    }
    
    /**
     * Register a toolkit refresher to this MCP client wrapper. The registered refresher will be notified when the MCP
     * client is refreshed.
     *
     * @param toolkit   the Nacos toolkit to register
     * @param refresher the refresher associated with the toolkit
     */
    public void registerToolkitRefresher(NacosToolkit toolkit, NacosToolkit.ToolsRefresher refresher) {
        this.subscriberToolkits.put(toolkit, refresher);
    }
    
    /**
     * Refresh the MCP client with new server information.
     *
     * <p>This method rebuilds the MCP client using the provided server information,
     * replaces the current client with the new one, and closes the old client.
     *
     * @param mcpServer the new MCP server information to use for rebuilding the client.
     * @see io.agentscope.extensions.nacos.mcp.NacosMcpServerManager#subscribeMcpClients
     */
    public void refresh(McpServerDetailInfo mcpServer) {
        log.info("Refreshing Nacos MCP client: {} with Async: {}", name, this.asyncClient);
        rebuildMcpClient(mcpServer).flatMap(client -> client.initialize().thenReturn(client)).map(client -> {
                    this.mcpServer = mcpServer;
                    McpClientWrapper oldClient = this.mcpClient;
                    this.mcpClient = client;
                    return oldClient;
                }).doOnNext(McpClientWrapper::close).then().doOnSuccess(unused -> notifyToolkitRefreshers())
                .doOnError(error -> log.error("Failed to refresh mcp client.", error)).block();
    }
    
    private Mono<McpClientWrapper> rebuildMcpClient(McpServerDetailInfo mcpServer) {
        String protocol = parseMcpProtocol(mcpServer);
        McpClientBuilder builder = McpClientBuilder.create(getName());
        
        String url = parseUrl(mcpServer);
        
        log.debug("Building Nacos MCP client: {} with URL: {} and Protocol: {}", getName(), url, protocol);
        
        switch (protocol) {
            case AiConstants.Mcp.MCP_PROTOCOL_SSE -> builder.sseTransport(url);
            case AiConstants.Mcp.MCP_PROTOCOL_STREAMABLE -> builder.streamableHttpTransport(url);
            default -> throw new UnsupportedOperationException("Unsupported mcp protocol: " + protocol);
        }
        return asyncClient ? builder.buildAsync() : Mono.just(builder.buildSync());
    }
    
    private String parseMcpProtocol(McpServerDetailInfo mcpServer) {
        return StringUtils.isBlank(mcpServer.getFrontProtocol()) ? mcpServer.getProtocol()
                : mcpServer.getFrontProtocol();
    }
    
    private String parseUrl(McpServerDetailInfo mcpServer) {
        if (null != mcpServer.getFrontendEndpoints() && !mcpServer.getFrontendEndpoints().isEmpty()) {
            return parseUrlFromEndpoints(mcpServer.getFrontendEndpoints());
        }
        return parseUrlFromEndpoints(mcpServer.getBackendEndpoints());
    }
    
    private String parseUrlFromEndpoints(List<McpEndpointInfo> endpointInfos) {
        return endpointInfos.stream().map(this::parseUrlFromEndpoint).findAny()
                .orElseThrow(() -> new NacosRuntimeException(NacosException.NOT_FOUND, "No endpoint found."));
    }
    
    private String parseUrlFromEndpoint(McpEndpointInfo endpointInfo) {
        String transport = StringUtils.isBlank(endpointInfo.getProtocol()) ? AiConstants.Mcp.MCP_PROTOCOL_HTTP
                : endpointInfo.getProtocol();
        String path = endpointInfo.getPath();
        if (StringUtils.isBlank(path)) {
            path = StringUtils.EMPTY;
        } else {
            path = path.startsWith("/") ? path : "/" + path;
        }
        return String.format("%s://%s:%d%s", transport, endpointInfo.getAddress(), endpointInfo.getPort(), path);
    }
    
    private McpSchema.Tool refreshToolSpec(McpSchema.Tool originalTool, Map<String, McpTool> toolsInNacos) {
        if (!toolsInNacos.containsKey(originalTool.name())) {
            return originalTool;
        }
        try {
            log.debug("Refresh tool spec for {} from Nacos.", originalTool.name());
            McpTool toolInNacos = toolsInNacos.get(originalTool.name());
            McpSchema.JsonSchema toolInputSchema = McpJsonMapper.getDefault()
                    .convertValue(toolInNacos.getInputSchema(), McpSchema.JsonSchema.class);
            return McpSchema.Tool.builder().name(originalTool.name()).description(toolInNacos.getDescription())
                    .inputSchema(toolInputSchema)
                    // TODO: refresh title, outputSchema, annotations, meta from Nacos when Nacos support this feature.
                    .title(originalTool.title()).outputSchema(originalTool.outputSchema())
                    .annotations(originalTool.annotations()).meta(originalTool.meta()).build();
        } catch (Exception e) {
            log.error("Failed to refresh tool spec for {}, directly use original tool spec from mcp client.",
                    originalTool.name(), e);
            return originalTool;
        }
    }
    
    private void notifyToolkitRefreshers() {
        Set<NacosToolkit.ToolsRefresher> refreshers = new HashSet<>(this.subscriberToolkits.values());
        refreshers.forEach(refresher -> refresher.doRefresh(this));
    }
}
