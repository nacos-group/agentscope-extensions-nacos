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

package io.agentscope.extensions.a2a.nacos.registry.constants;

import static io.agentscope.spring.boot.a2a.properties.Constants.A2A_SERVER_PREFIX;

/**
 * @author xiweng.yy
 */
public class Constants {
    
    public static final String AGENTSCOPE_RUNTIME_A2A_PREFIX = A2A_SERVER_PREFIX;
    
    public static final String NACOS_PREFIX = AGENTSCOPE_RUNTIME_A2A_PREFIX + ".nacos";
    
    public static final String REGISTRY_PREFIX = NACOS_PREFIX + ".registry";
}
