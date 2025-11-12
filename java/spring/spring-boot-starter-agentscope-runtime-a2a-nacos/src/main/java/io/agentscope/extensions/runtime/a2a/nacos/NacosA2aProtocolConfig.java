package io.agentscope.extensions.runtime.a2a.nacos;

import io.a2a.spec.AgentInterface;
import io.a2a.spec.AgentProvider;
import io.a2a.spec.AgentSkill;
import io.a2a.spec.SecurityScheme;
import io.agentscope.runtime.protocol.a2a.A2aProtocolConfig;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Extensions {@link io.agentscope.runtime.protocol.a2a.A2aProtocolConfig} for Nacos A2A Registry.
 *
 * @author xiweng.yy
 */
public class NacosA2aProtocolConfig extends A2aProtocolConfig {
    
    private final Properties nacosProperties;
    
    private final boolean registerAsLatest;
    
    public NacosA2aProtocolConfig(String name, String description, String url, AgentProvider provider, String version,
            String documentationUrl, List<String> defaultInputModes, List<String> defaultOutputModes,
            List<AgentSkill> skills, boolean supportsAuthenticatedExtendedCard,
            Map<String, SecurityScheme> securitySchemes, List<Map<String, List<String>>> security, String iconUrl,
            List<AgentInterface> additionalInterfaces, Properties nacosProperties, boolean registerAsLatest) {
        super(name, description, url, provider, version, documentationUrl, defaultInputModes, defaultOutputModes,
                skills, supportsAuthenticatedExtendedCard, securitySchemes, security, iconUrl, additionalInterfaces);
        this.nacosProperties = nacosProperties;
        this.registerAsLatest = registerAsLatest;
    }
    
    public Properties getNacosProperties() {
        return nacosProperties;
    }
    
    public boolean isRegisterAsLatest() {
        return registerAsLatest;
    }
    
    public static class Builder extends A2aProtocolConfig.Builder {
        
        private final Properties nacosProperties;
        
        private boolean registerAsLatest = true;
        
        public Builder(Properties nacosProperties) {
            this.nacosProperties = nacosProperties;
        }
        
        public Builder registerAsLatest(boolean registerAsLatest) {
            this.registerAsLatest = registerAsLatest;
            return this;
        }
        
        @Override
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder url(String url) {
            this.url = url;
            return this;
        }
        
        public Builder provider(AgentProvider provider) {
            this.provider = provider;
            return this;
        }
        
        public Builder version(String version) {
            this.version = version;
            return this;
        }
        
        public Builder documentationUrl(String documentationUrl) {
            this.documentationUrl = documentationUrl;
            return this;
        }
        
        public Builder defaultInputModes(List<String> defaultInputModes) {
            this.defaultInputModes = defaultInputModes;
            return this;
        }
        
        public Builder defaultOutputModes(List<String> defaultOutputModes) {
            this.defaultOutputModes = defaultOutputModes;
            return this;
        }
        
        public Builder skills(List<AgentSkill> skills) {
            this.skills = skills;
            return this;
        }
        
        public Builder supportsAuthenticatedExtendedCard(boolean supportsAuthenticatedExtendedCard) {
            this.supportsAuthenticatedExtendedCard = supportsAuthenticatedExtendedCard;
            return this;
        }
        
        public Builder securitySchemes(Map<String, SecurityScheme> securitySchemes) {
            this.securitySchemes = securitySchemes;
            return this;
        }
        
        public Builder security(List<Map<String, List<String>>> security) {
            this.security = security;
            return this;
        }
        
        public Builder iconUrl(String iconUrl) {
            this.iconUrl = iconUrl;
            return this;
        }
        
        public Builder additionalInterfaces(List<AgentInterface> additionalInterfaces) {
            this.additionalInterfaces = additionalInterfaces;
            return this;
        }
        
        @Override
        public A2aProtocolConfig build() {
            if (null == nacosProperties) {
                throw new IllegalArgumentException("Nacos properties can not be null");
            }
            return new NacosA2aProtocolConfig(name, description, url, provider, version, documentationUrl,
                    defaultInputModes, defaultOutputModes, skills, supportsAuthenticatedExtendedCard, securitySchemes,
                    security, iconUrl, additionalInterfaces, nacosProperties, registerAsLatest);
        }
    }
}
