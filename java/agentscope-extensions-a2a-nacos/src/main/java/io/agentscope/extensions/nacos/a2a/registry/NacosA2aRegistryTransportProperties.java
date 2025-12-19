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

package io.agentscope.extensions.nacos.a2a.registry;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.exception.runtime.NacosRuntimeException;
import com.alibaba.nacos.common.utils.StringUtils;
import io.a2a.spec.AgentInterface;

import java.net.URI;

/**
 * Properties for A2A Transports Endpoint registry to Nacos.
 *
 * @author xiweng.yy
 */
public record NacosA2aRegistryTransportProperties(String transport, String host, int port,
                                                  String path, boolean supportTls, String protocol,
                                                  String query) {
    
    private static final String HTTPS_PROTOCOL = "https";
    
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Creates a Builder instance from an AgentInterface object.
     *
     * <p>
     * This method extracts transport and endpoint information from the given {@link AgentInterface} and populates a
     * Builder with these values. It parses the URL to extract host, port, path, protocol and query parameters. If the
     * protocol is HTTPS, it automatically sets the TLS support flag.
     * </p>
     *
     * @param agentInterface the AgentInterface to extract information from
     * @return a Builder instance populated with values from the AgentInterface
     * @throws NacosRuntimeException if the URL in agentInterface is invalid
     */
    public static Builder from(AgentInterface agentInterface) {
        Builder result = builder();
        result.transport(agentInterface.transport());
        try {
            URI uri = URI.create(agentInterface.url());
            result.host(uri.getHost()).port(uri.getPort()).path(uri.getPath())
                    .protocol(uri.getScheme()).query(uri.getQuery());
            // Check if the protocol is HTTPS and set TLS support flag accordingly
            if (StringUtils.isNotEmpty(result.protocol) && HTTPS_PROTOCOL.equalsIgnoreCase(
                    result.protocol)) {
                result.supportTls(true);
            }
        } catch (IllegalArgumentException e) {
            throw new NacosRuntimeException(NacosException.INVALID_PARAM, "Invalid URL: " + agentInterface.url(), e);
        }
        return result;
    }
    
    public static class Builder {
        
        private String transport;
        
        private String host;
        
        private int port;
        
        private String path;
        
        private boolean supportTls;
        
        private String protocol;
        
        private String query;
        
        public Builder transport(String transport) {
            this.transport = transport;
            return this;
        }
        
        public Builder host(String host) {
            this.host = host;
            return this;
        }
        
        public Builder port(int port) {
            this.port = port;
            return this;
        }
        
        public Builder path(String path) {
            this.path = path;
            return this;
        }
        
        public Builder supportTls(boolean supportTls) {
            this.supportTls = supportTls;
            return this;
        }
        
        public Builder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }
        
        public Builder query(String query) {
            this.query = query;
            return this;
        }
        
        public NacosA2aRegistryTransportProperties build() {
            if (StringUtils.isEmpty(transport)) {
                throw new IllegalArgumentException("A2a Endpoint `transport` can not be empty.");
            }
            if (StringUtils.isEmpty(host)) {
                throw new IllegalArgumentException("A2a Endpoint `host` can not be empty.");
            }
            return new NacosA2aRegistryTransportProperties(transport, host, port, path, supportTls, protocol, query);
        }
    }
}