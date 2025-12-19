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

package io.agentscope.extensions.a2a.nacos.registry.properties;

import com.alibaba.nacos.common.utils.StringUtils;

/**
 * A2a transport properties for Nacos A2A Registry.
 *
 * @author xiweng.yy
 */
public class NacosA2aTransportProperties {
    
    private String host;
    
    private int port;
    
    private String path;
    
    private Boolean supportTls;
    
    private String protocol;
    
    private String query;
    
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public Boolean isSupportTls() {
        return supportTls;
    }
    
    public void setSupportTls(Boolean supportTls) {
        this.supportTls = supportTls;
    }
    
    public String getProtocol() {
        return protocol;
    }
    
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    /**
     * Merges non-null and non-empty properties from the given NacosA2aTransportProperties object into this object.
     *
     * <p>
     * This method selectively updates this object's properties with values from the newProperties object. Only
     * properties that are non-null (for objects) or non-empty (for strings) or greater than zero (for port) in the
     * newProperties object will be used to update this object's corresponding properties.
     * </p>
     *
     * @param newProperties the NacosA2aTransportProperties object containing new values to merge
     */
    public void merge(NacosA2aTransportProperties newProperties) {
        if (null == newProperties) {
            return;
        }
        if (StringUtils.isNotEmpty(newProperties.getHost())) {
            this.setHost(newProperties.getHost());
        }
        if (newProperties.getPort() > 0) {
            this.setPort(newProperties.getPort());
        }
        if (StringUtils.isNotEmpty(newProperties.getPath())) {
            this.setPath(newProperties.getPath());
        }
        if (StringUtils.isNotEmpty(newProperties.getProtocol())) {
            this.setProtocol(newProperties.getProtocol());
        }
        if (StringUtils.isNotEmpty(newProperties.getQuery())) {
            this.setQuery(newProperties.getQuery());
        }
        if (null != newProperties.isSupportTls()) {
            this.setSupportTls(newProperties.isSupportTls());
        }
    }
}