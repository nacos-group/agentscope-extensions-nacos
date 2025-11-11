package io.agentscope.extensions.nacos.mcp;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.extensions.nacos.mcp.client.NacosMcpClientBuilder;
import io.agentscope.extensions.nacos.mcp.client.NacosMcpClientWrapper;
import io.agentscope.extensions.nacos.mcp.tool.NacosMcpTool;
import io.agentscope.extensions.nacos.mcp.tool.NacosMcpToolBuilder;
import io.agentscope.extensions.nacos.mcp.tool.NacosToolkit;
import org.junit.jupiter.api.Test;

import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

class NacosMcpToolITest {
    
    @Test
    public void test() throws NacosException {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, "127.0.0.1:8848");
        
        NacosMcpServerManager mcpServerManager = NacosMcpServerManager.from(properties);
        
        NacosMcpClientWrapper mcpClient = NacosMcpClientBuilder.create("nacos-cluster-provider", mcpServerManager)
                .build();
        
        //        Toolkit toolkit = createToolKitByWay1(mcpClient, mcpServerManager);
        Toolkit toolkit = createToolKitByWay2(mcpClient, mcpServerManager);
        //        Toolkit toolkit = createToolKitByWay3(mcpClient, mcpServerManager);
        //        Toolkit toolkit = createToolKitByWay4(mcpClient, mcpServerManager);
        
        Model model = DashScopeChatModel.builder().apiKey(System.getenv("DASHSCOPE_API_KEY")).modelName("qwen-max")
                .build();
        
        Scanner scanner = new Scanner(System.in);
        String line = "";
        Set<String> quitWords = Set.of("quit", "exit");
        System.out.print("You> ");
        while (null != (line = scanner.nextLine())) {
            if (quitWords.contains(line.trim().toLowerCase())) {
                System.out.println("Agent> Bye!");
                System.exit(0);
            }
            if ("go".equalsIgnoreCase(line.trim())) {
                System.out.printf("Agent> %s%n", exampleQuestion(model, toolkit));
                System.out.print("You> ");
            }
        }
        
    }
    
    private Toolkit createToolKitByWay1(NacosMcpClientWrapper mcpClient, NacosMcpServerManager mcpServerManager)
            throws NacosException {
        NacosMcpTool listNacosClusterTool = new NacosMcpTool("listNacosClusters", mcpClient);
        NacosMcpTool loginNacosClusterTool = new NacosMcpTool("loginNacos", mcpClient);
        Toolkit toolkit = new Toolkit();
        toolkit.registerAgentTool(listNacosClusterTool);
        toolkit.registerAgentTool(loginNacosClusterTool);
        return toolkit;
    }
    
    private Toolkit createToolKitByWay2(NacosMcpClientWrapper mcpClient, NacosMcpServerManager mcpServerManager)
            throws NacosException {
        Toolkit toolkit = new Toolkit();
        NacosMcpToolBuilder builder = NacosMcpToolBuilder.create(mcpClient);
        builder.build().forEach(toolkit::registerAgentTool);
        return toolkit;
    }
    
    private Toolkit createToolKitByWay3(NacosMcpClientWrapper mcpClient, NacosMcpServerManager mcpServerManager)
            throws NacosException {
        // Not support auto-refresh tool spec changes, only support auto-refresh endpoints changes
        Toolkit toolkit = new Toolkit();
        toolkit.registerMcpClient(mcpClient).block();
        return toolkit;
    }
    
    private Toolkit createToolKitByWay4(NacosMcpClientWrapper mcpClient, NacosMcpServerManager mcpServerManager)
            throws NacosException {
        // Both support auto-refresh tool spec changes and endpoints changes.
        Toolkit toolkit = new NacosToolkit();
        toolkit.registerMcpClient(mcpClient).block();
        return toolkit;
    }
    
    private String exampleQuestion(Model model, Toolkit toolkit) {
        ReActAgent agent = ReActAgent.builder().name("hello-world-agent").sysPrompt("You are a helpful AI assistant.")
                .model(model).toolkit(toolkit).memory(new InMemoryMemory()).build();
        
        Msg userMessage = null;
        Msg response = null;
        
        userMessage = Msg.builder().role(MsgRole.USER).content(TextBlock.builder()
                .text("请列举出我的环境中有哪些Nacos集群，根据结果执行如下操作。 "
                        + "1. 如果环境中没有Nacos集群，帮我登录一个新Nacos集群local,ip为127.0.0.1端口为8848，用户名密码均为nacos。 "
                        + "2. 如果有Nacos集群，请列出这些Nacos集群，并获取列表中第一个集群的详细信息。"
                        + "3. 第一个步骤和第二个步骤互斥，即两个步骤不会同时被执行。").build()).build();
        
        response = agent.call(userMessage).block();
//        System.out.println("Agent Response: " + response.getContent().get(0).toString());
        return response.getContent().get(0).toString();
    }
    
    public static void main(String[] args) throws NacosException {
        NacosMcpToolITest test = new NacosMcpToolITest();
        test.test();
    }
}