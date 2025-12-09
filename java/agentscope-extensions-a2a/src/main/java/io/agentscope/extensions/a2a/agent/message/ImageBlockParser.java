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

import io.a2a.spec.FileContent;
import io.a2a.spec.FilePart;
import io.a2a.spec.FileWithBytes;
import io.a2a.spec.FileWithUri;
import io.a2a.spec.Part;
import io.agentscope.core.message.Base64Source;
import io.agentscope.core.message.ImageBlock;
import io.agentscope.core.message.Source;
import io.agentscope.core.message.URLSource;
import io.agentscope.extensions.a2a.agent.utils.LoggerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

/**
 * Parser for {@link ImageBlock} to {@link FilePart}.
 *
 *
 *
 * @author xiweng.yy
 */
public class ImageBlockParser implements ContentBlockParser<ImageBlock> {
    
    private static final Logger log = LoggerFactory.getLogger(ImageBlockParser.class);
    
    @Override
    public Part<?> parse(ImageBlock contentBlock) {
        Source source = contentBlock.getSource();
        FileContent file;
        if (source instanceof Base64Source base64Source) {
            file = parseFromBase64Source(base64Source);
        } else if (source instanceof URLSource urlSource) {
            file = parseFromUrlSource(urlSource);
        } else {
            LoggerUtil.warn(log, "Unsupported source type: {}", source.getClass().getName());
            return null;
        }
        return new FilePart(file, new HashMap<>());
    }
    
    private FileContent parseFromBase64Source(Base64Source source) {
        return new FileWithBytes(source.getMediaType(), generateRandomFileName(), source.getData());
    }
    
    private FileContent parseFromUrlSource(URLSource urlSource) {
        String url = urlSource.getUrl();
        return new FileWithUri(tryToParseMiniTypeFromUrl(url), generateRandomFileName(), url);
    }
    
    private String tryToParseMiniTypeFromUrl(String url) {
        try {
            URL javaUrl = new URL(url);
            String miniType = Files.probeContentType(Paths.get(javaUrl.getPath()));
            if (Objects.isNull(miniType)) {
                return MessageConstants.BlockContent.TYPE_IMAGE;
            }
            return miniType.startsWith(MessageConstants.BlockContent.TYPE_IMAGE) ? miniType
                    : MessageConstants.BlockContent.TYPE_IMAGE;
        } catch (Exception ignored) {
            return MessageConstants.BlockContent.TYPE_IMAGE;
        }
    }
    
    private String generateRandomFileName() {
        return UUID.randomUUID().toString();
    }
}
