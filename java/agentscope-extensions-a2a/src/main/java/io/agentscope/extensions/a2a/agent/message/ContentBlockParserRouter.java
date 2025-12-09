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

package io.agentscope.extensions.a2a.agent.message;

import io.a2a.spec.Part;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ThinkingBlock;

/**
 * The router for {@link ContentBlockParser} according to class type of {@link ContentBlock}.
 *
 * @author xiweng.yy
 */
public class ContentBlockParserRouter {
    
    /**
     * Parse {@link ContentBlock} to {@link Part}.
     *
     * @param contentBlock the content block to parse
     * @return the parsed part, or null if the part is null or not supported
     */
    public Part<?> parse(ContentBlock contentBlock) {
        if (null == contentBlock) {
            return null;
        }
        // TODO current only support text type.
        if (contentBlock instanceof TextBlock textBlock) {
            return new TextBlockParser().parse(textBlock);
        } else if (contentBlock instanceof ThinkingBlock thinkingBlock) {
            return new ThinkingBlockParser().parse(thinkingBlock);
        }
        return null;
    }
}
