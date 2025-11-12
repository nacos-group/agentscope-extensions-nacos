/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.agentscope.extensions.runtime.a2a.registry;

import io.a2a.spec.AgentCard;
import io.agentscope.runtime.autoconfigure.DeployProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

/**
 * Agent registry service.
 *
 * <p>Do Register operation when A2A server is ready.
 *
 * @author xiweng.yy
 */
public class AgentRegistryService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentRegistryService.class);
    
    private final AgentRegistry agentRegistry;
    
    private final AgentCard agentCard;
    
    private final DeployProperties deployProperties;
    
    public AgentRegistryService(AgentRegistry agentRegistry, AgentCard agentCard, DeployProperties deployProperties) {
        this.agentRegistry = agentRegistry;
        this.agentCard = agentCard;
        this.deployProperties = deployProperties;
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void register() {
        LOGGER.info("Auto register agent {} into Registry {}.", agentCard.name(), agentRegistry.registryName());
        try {
            agentRegistry.register(agentCard, deployProperties);
            LOGGER.info("Auto register agent {} into Registry {} successfully.", agentCard.name(),
                    agentRegistry.registryName());
        } catch (Exception e) {
            LOGGER.error("Auto register agent {} into Registry {} failed.", agentCard.name(),
                    agentRegistry.registryName(), e);
        }
    }
    
}
