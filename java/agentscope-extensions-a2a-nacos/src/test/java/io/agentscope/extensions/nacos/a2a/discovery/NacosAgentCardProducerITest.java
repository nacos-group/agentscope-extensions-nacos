package io.agentscope.extensions.nacos.a2a.discovery;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.extensions.a2a.agent.A2aAgent;
import io.agentscope.extensions.a2a.agent.A2aAgentConfig;
import org.junit.jupiter.api.Test;
import reactor.core.Disposable;

import java.util.Properties;

class NacosAgentCardProducerITest {
    
    @Test
    public void test() throws NacosException {
        Properties properties = new Properties();
        properties.setProperty(PropertyKeyConst.SERVER_ADDR, "127.0.0.1");
        
        NacosAgentCardProducer agentCardProducer = new NacosAgentCardProducer(properties);
        A2aAgentConfig a2aAgentConfig = new A2aAgentConfig.A2aAgentConfigBuilder().adaptOldVersionA2aDateTimeSerialization(
                true).agentCardProducer(agentCardProducer).build();
        A2aAgent agent = new A2aAgent("agentscope-runtime", a2aAgentConfig);
        
        Msg requestMsg = Msg.builder().content(TextBlock.builder().text("什么是Nacos").build()).build();
        
        //        callWithSync(agent, requestMsg);
        
        System.out.println("\n");
        System.out.println("---- Stream Call ----\n");
        
        callWithStream(agent, requestMsg);
    }
    
    private void callWithSync(A2aAgent agent, Msg requestMsg) {
        Msg msg = agent.call(requestMsg).block();
        msg.getContent().forEach(contentBlock -> {
            if (contentBlock instanceof TextBlock textBlock) {
                System.out.print(textBlock.getText());
            }
        });
    }
    
    private void callWithStream(A2aAgent agent, Msg requestMsg) {
        Disposable disposable = agent.stream(requestMsg)
                .subscribe(event -> event.getMessage().getContent().forEach(contentBlock -> {
                    if (contentBlock instanceof TextBlock textBlock) {
                        System.out.print(textBlock.getText());
                    }
                }));
        while (!disposable.isDisposed()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}