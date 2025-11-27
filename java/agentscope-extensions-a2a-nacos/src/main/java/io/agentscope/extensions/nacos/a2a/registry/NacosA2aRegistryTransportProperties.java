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
public record NacosA2aRegistryTransportProperties(String transport, String endpointAddress, int endpointPort,
                                                  String endpointPath, boolean isSupportTls, String endpointProtocol,
                                                  String endpointQuery) {
    
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
            result.endpointAddress(uri.getHost()).endpointPort(uri.getPort()).endpointPath(uri.getPath())
                    .endpointProtocol(uri.getScheme()).endpointQuery(uri.getQuery());
            // Check if the protocol is HTTPS and set TLS support flag accordingly
            if (StringUtils.isNotEmpty(result.endpointProtocol) && HTTPS_PROTOCOL.equalsIgnoreCase(
                    result.endpointProtocol)) {
                result.isSupportTls(true);
            }
        } catch (IllegalArgumentException e) {
            throw new NacosRuntimeException(NacosException.INVALID_PARAM, "Invalid URL: " + agentInterface.url(), e);
        }
        return result;
    }
    
    public static class Builder {
        
        private String transport;
        
        private String endpointAddress;
        
        private int endpointPort;
        
        private String endpointPath;
        
        private boolean isSupportTls;
        
        private String endpointProtocol;
        
        private String endpointQuery;
        
        public Builder transport(String transport) {
            this.transport = transport;
            return this;
        }
        
        public Builder endpointAddress(String endpointAddress) {
            this.endpointAddress = endpointAddress;
            return this;
        }
        
        public Builder endpointPort(int endpointPort) {
            this.endpointPort = endpointPort;
            return this;
        }
        
        public Builder endpointPath(String endpointPath) {
            this.endpointPath = endpointPath;
            return this;
        }
        
        public Builder isSupportTls(boolean isSupportTls) {
            this.isSupportTls = isSupportTls;
            return this;
        }
        
        public Builder endpointProtocol(String endpointProtocol) {
            this.endpointProtocol = endpointProtocol;
            return this;
        }
        
        public Builder endpointQuery(String endpointQuery) {
            this.endpointQuery = endpointQuery;
            return this;
        }
        
        public NacosA2aRegistryTransportProperties build() {
            if (StringUtils.isEmpty(transport)) {
                throw new IllegalArgumentException("A2a Endpoint `Transport` can not be empty.");
            }
            if (StringUtils.isEmpty(endpointAddress)) {
                throw new IllegalArgumentException("A2a Endpoint `Address` can not be empty.");
            }
            return new NacosA2aRegistryTransportProperties(transport, endpointAddress, endpointPort, endpointPath,
                    isSupportTls, endpointProtocol, endpointQuery);
        }
    }
}