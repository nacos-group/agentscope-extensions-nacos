/*
 * Copyright 1999-2025 Alibaba Group Holding Ltd.
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

package io.agentscope.extensions.nacos.a2a.registry.constants;

/**
 * @author xiweng.yy
 */
public class Constants {
    
    public static final String PROTOCOL_TYPE_HTTP = "http";
    
    public static final String PROTOCOL_TYPE_HTTPS = "https";
    
    public static final String PROPERTIES_ENV_PREFIX = "NACOS_A2A_AGENT_";
    
    public enum TransportPropertiesAttribute {
        HOST("HOST", "host"),
        PORT("PORT", "port"),
        PATH("PATH", "path"),
        PROTOCOL("PROTOCOL", "protocol"),
        QUERY("QUERY", "query"),
        SUPPORT_TLS("SUPPORTTLS", "supportTls");
        
        private final String envKey;
        
        private final String propertyKey;
        
        TransportPropertiesAttribute(String envKey, String propertyKey) {
            this.envKey = envKey;
            this.propertyKey = propertyKey;
        }
        
        public String getPropertyKey() {
            return propertyKey;
        }
        
        public static TransportPropertiesAttribute getByEnvKey(String envKey) {
            for (TransportPropertiesAttribute each : TransportPropertiesAttribute.values()) {
                if (each.envKey.equals(envKey)) {
                    return each;
                }
            }
            return null;
        }
        
        public static TransportPropertiesAttribute getByPropertyKey(String propertyKey) {
            for (TransportPropertiesAttribute each : TransportPropertiesAttribute.values()) {
                if (each.propertyKey.equals(propertyKey)) {
                    return each;
                }
            }
            return null;
        }
    }
}
