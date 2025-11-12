package io.agentscope.extensions.runtime.a2a.nacos.condition;

import io.agentscope.extensions.runtime.a2a.nacos.utils.NacosBeanUtil;
import io.agentscope.runtime.protocol.ProtocolConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Auto-Configuration condition for {@link io.agentscope.extensions.runtime.a2a.nacos.NacosA2aProtocolConfig} existed.
 *
 * @author xiweng.yy
 */
public class NacosA2aProtocolConfigExistCondition implements Condition {
    
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        if (null == context.getBeanFactory()) {
            return false;
        }
        ObjectProvider<ProtocolConfig> protocolConfigs = context.getBeanFactory().getBeanProvider(ProtocolConfig.class);
        return null != NacosBeanUtil.getNacosA2aProtocolConfig(protocolConfigs);
    }
}
