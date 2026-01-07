/*
 * Copyright 1999-2026 Alibaba Group Holding Ltd.
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

package io.agentscope.extensions.runtime.a2a.nacos.utils;

import com.alibaba.nacos.api.ai.model.a2a.AgentCapabilities;
import com.alibaba.nacos.api.ai.model.a2a.AgentCard;
import com.alibaba.nacos.api.ai.model.a2a.AgentInterface;
import com.alibaba.nacos.api.ai.model.a2a.AgentProvider;
import com.alibaba.nacos.api.ai.model.a2a.AgentSkill;
import com.alibaba.nacos.api.ai.model.a2a.SecurityScheme;
import com.alibaba.nacos.common.utils.JacksonUtils;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AgentCard Converter between Nacos {@link com.alibaba.nacos.api.ai.model.a2a.AgentCard} and A2A specification
 * {@link io.a2a.spec.AgentCard}.
 *
 * @author xiweng.yy
 */
public class AgentCardConverterUtil {
    
    /**
     * Convert A2A specification AgentCard object to Nacos AgentCard object
     *
     * @param agentCard the A2A specification AgentCard object
     * @return the converted Nacos AgentCard object
     */
    public static AgentCard convertToNacosAgentCard(io.a2a.spec.AgentCard agentCard) {
        AgentCard card = new AgentCard();
        card.setProtocolVersion(agentCard.protocolVersion());
        card.setName(agentCard.name());
        card.setDescription(agentCard.description());
        card.setVersion(agentCard.version());
        card.setIconUrl(agentCard.iconUrl());
        card.setCapabilities(convertToNacosAgentCapabilities(agentCard.capabilities()));
        card.setSkills(agentCard.skills().stream().map(AgentCardConverterUtil::convertToNacosAgentSkill).toList());
        card.setUrl(agentCard.url());
        card.setPreferredTransport(agentCard.preferredTransport());
        card.setAdditionalInterfaces(convertToNacosAgentInterfaces(agentCard.additionalInterfaces()));
        card.setProvider(convertToNacosAgentProvider(agentCard.provider()));
        card.setDocumentationUrl(agentCard.documentationUrl());
        card.setSecuritySchemes(convertToNacosSecuritySchemes(agentCard.securitySchemes()));
        card.setSecurity(agentCard.security());
        card.setDefaultInputModes(agentCard.defaultInputModes());
        card.setDefaultOutputModes(agentCard.defaultOutputModes());
        card.setSupportsAuthenticatedExtendedCard(agentCard.supportsAuthenticatedExtendedCard());
        return card;
    }
    
    private static AgentCapabilities convertToNacosAgentCapabilities(io.a2a.spec.AgentCapabilities capabilities) {
        com.alibaba.nacos.api.ai.model.a2a.AgentCapabilities nacosCapabilities = new com.alibaba.nacos.api.ai.model.a2a.AgentCapabilities();
        nacosCapabilities.setStreaming(capabilities.streaming());
        nacosCapabilities.setPushNotifications(capabilities.pushNotifications());
        nacosCapabilities.setStateTransitionHistory(capabilities.stateTransitionHistory());
        return nacosCapabilities;
    }
    
    private static AgentSkill convertToNacosAgentSkill(io.a2a.spec.AgentSkill agentSkill) {
        AgentSkill skill = new AgentSkill();
        skill.setId(agentSkill.id());
        skill.setName(agentSkill.name());
        skill.setDescription(agentSkill.description());
        skill.setTags(agentSkill.tags());
        skill.setExamples(agentSkill.examples());
        skill.setInputModes(agentSkill.inputModes());
        skill.setOutputModes(agentSkill.outputModes());
        return skill;
    }
    
    private static List<AgentInterface> convertToNacosAgentInterfaces(
            List<io.a2a.spec.AgentInterface> agentInterfaces) {
        if (agentInterfaces == null) {
            return List.of();
        }
        return agentInterfaces.stream().map(AgentCardConverterUtil::convertToNacosAgentInterface)
                .collect(Collectors.toList());
    }
    
    private static AgentInterface convertToNacosAgentInterface(io.a2a.spec.AgentInterface agentInterface) {
        AgentInterface nacosAgentInterface = new AgentInterface();
        nacosAgentInterface.setUrl(agentInterface.url());
        nacosAgentInterface.setTransport(agentInterface.transport());
        return nacosAgentInterface;
    }
    
    private static AgentProvider convertToNacosAgentProvider(io.a2a.spec.AgentProvider agentProvider) {
        if (null == agentProvider) {
            return null;
        }
        AgentProvider nacosAgentProvider = new AgentProvider();
        nacosAgentProvider.setOrganization(agentProvider.organization());
        nacosAgentProvider.setUrl(agentProvider.url());
        return nacosAgentProvider;
    }
    
    private static Map<String, SecurityScheme> convertToNacosSecuritySchemes(
            Map<String, io.a2a.spec.SecurityScheme> securitySchemes) {
        if (securitySchemes == null) {
            return null;
        }
        String originalJson = JacksonUtils.toJson(securitySchemes);
        return JacksonUtils.toObj(originalJson, new TypeReference<>() {
        });
    }
    
}
