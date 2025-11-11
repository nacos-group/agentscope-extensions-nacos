package io.agentscope.extensions.nacos.mcp.tool;

import com.alibaba.nacos.common.utils.CollectionUtils;
import io.agentscope.extensions.nacos.mcp.client.NacosMcpClientWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * The Builder of {@link NacosMcpTool}.
 *
 * <p>Example Usages:
 * <pre>{@code
 *  Toolkit toolkit = new Toolkit();
 *  NacosMcpClientWrapper nacosMcpClient = NacosMcpClientBuilder.create(...).build();
 *  builder.build().forEach(toolkit::registerAgentTool);
 * }
 * </pre>
 *
 * @author xiweng.yy
 */
public class NacosMcpToolBuilder {
    
    private static final Logger log = LoggerFactory.getLogger(NacosMcpToolBuilder.class);
    
    private final NacosMcpClientWrapper mcpClient;
    
    private Collection<String> includeTools = new LinkedList<>();
    
    private Collection<String> excludeTools = new LinkedList<>();
    
    private NacosMcpToolBuilder(NacosMcpClientWrapper mcpClient) {
        this.mcpClient = mcpClient;
    }
    
    public static NacosMcpToolBuilder create(NacosMcpClientWrapper mcpClient) {
        return new NacosMcpToolBuilder(mcpClient);
    }
    
    public NacosMcpToolBuilder includeTools(Collection<String> includeTools) {
        this.includeTools = includeTools;
        return this;
    }
    
    public NacosMcpToolBuilder excludeTools(Collection<String> excludeTools) {
        this.excludeTools = excludeTools;
        return this;
    }
    
    /**
     * Build {@link NacosMcpTool}s from target {@link NacosMcpClientWrapper} and
     * {@link com.alibaba.nacos.api.ai.model.mcp.McpServerDetailInfo} from Nacos.
     *
     * <ul>
     *     <li>{@link #includeTools} will filter MCP tool which name in include</li>
     *     <li>{@link #excludeTools} will filter MCP tool which name not in exclude</li>
     *     <li>{@link #includeTools} and {@link #excludeTools} both empty will return all tools in MCP Server</li>
     *     <li>{@link #includeTools} and {@link #excludeTools} both not empty will only effect {@link #includeTools}</li>
     * </ul>
     *
     * @return List of {@link NacosMcpTool} from MCP Server registered in Nacos.
     */
    public List<NacosMcpTool> build() {
        if (CollectionUtils.isNotEmpty(includeTools) && CollectionUtils.isNotEmpty(excludeTools)) {
            log.warn("Include tools and exclude tools are both set, exclude tools will be ignored.");
        }
        return mcpClient.getMcpServer().getToolSpec().getTools().stream()
                .filter(tool -> CollectionUtils.isEmpty(includeTools) || includeTools.contains(tool.getName()))
                .filter(tool -> CollectionUtils.isEmpty(excludeTools) || !excludeTools.contains(tool.getName()))
                .map(tool -> new NacosMcpTool(tool.getName(), mcpClient)).toList();
    }
}
