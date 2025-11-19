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

package io.agentscope.extensions.a2a.agent.utils;

import io.a2a.spec.Artifact;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.extensions.a2a.agent.message.ContentBlockParserRouter;
import io.agentscope.extensions.a2a.agent.message.PartParserRouter;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Message Converter between Agentscope {@link io.agentscope.core.message.Msg} and A2A {@link io.a2a.spec.Message} or
 * {@link io.a2a.spec.Artifact}.
 *
 * @author xiweng.yy
 */
public class MessageConvertUtil {
    
    private static final PartParserRouter PART_PARSER = new PartParserRouter();
    
    private static final ContentBlockParserRouter CONTENT_BLOCK_PARSER = new ContentBlockParserRouter();
    
    /**
     * Convert a single {@link io.a2a.spec.Artifact} to {@link io.agentscope.core.message.Msg}.
     *
     * @param artifact the artifact to convert
     * @return the converted Msg object
     */
    public static Msg convertFromArtifact(Artifact artifact) {
        return convertFromArtifact(List.of(artifact));
    }
    
    /**
     * Convert a list of {@link io.a2a.spec.Artifact} to {@link io.agentscope.core.message.Msg}.
     *
     * @param artifacts the list of artifacts to convert
     * @return the converted Msg object
     */
    public static Msg convertFromArtifact(List<Artifact> artifacts) {
        Msg.Builder builder = Msg.builder();
        List<ContentBlock> contentBlocks = new LinkedList<>();
        artifacts.stream().filter(Objects::nonNull).filter(artifact -> isNotEmptyCollection(artifact.parts()))
                .forEach(artifact -> {
                    builder.id(artifact.artifactId());
                    builder.name(artifact.name());
                    builder.metadata(artifact.metadata());
                    contentBlocks.addAll(convertFromParts(artifact.parts()));
                });
        builder.role(MsgRole.ASSISTANT);
        builder.content(contentBlocks);
        return builder.build();
    }
    
    /**
     * Convert a single {@link io.a2a.spec.Message} to {@link io.agentscope.core.message.Msg}.
     *
     * @param message the message to convert
     * @return the converted Msg object
     */
    public static Msg convertFromMessage(Message message) {
        return convertFromMessage(List.of(message));
    }
    
    /**
     * Convert a list of {@link io.a2a.spec.Message} to {@link io.agentscope.core.message.Msg}.
     *
     * @param messages the list of messages to convert
     * @return the converted Msg object
     */
    public static Msg convertFromMessage(List<Message> messages) {
        Msg.Builder builder = Msg.builder();
        List<ContentBlock> contentBlocks = new LinkedList<>();
        messages.stream().filter(Objects::nonNull).filter(message -> isNotEmptyCollection(message.getParts()))
                .forEach(message -> {
                    builder.id(message.getMessageId());
                    builder.metadata(message.getMetadata());
                    contentBlocks.addAll(convertFromParts(message.getParts()));
                });
        builder.role(MsgRole.ASSISTANT);
        builder.content(contentBlocks);
        return builder.build();
    }
    
    /**
     * Convert a list of {@link io.agentscope.core.message.Msg} to {@link io.a2a.spec.Message}.
     *
     * @param msgs the list of Msg to convert
     * @return the converted Message object
     */
    public static Message convertFromMsg(List<Msg> msgs) {
        Message.Builder builder = new Message.Builder();
        Map<String, Object> metadata = new HashMap<>();
        List<Part<?>> parts = new LinkedList<>();
        msgs.stream().filter(Objects::nonNull).filter(msg -> isNotEmptyCollection(msg.getContent())).forEach(msg -> {
            metadata.putAll(msg.getMetadata());
            parts.addAll(msg.getContent().stream().map(CONTENT_BLOCK_PARSER::parse).filter(Objects::nonNull).toList());
        });
        return builder.parts(parts).metadata(metadata).build();
    }
    
    private static boolean isNotEmptyCollection(Collection<?> collection) {
        return null != collection && !collection.isEmpty();
    }
    
    private static List<ContentBlock> convertFromParts(List<Part<?>> parts) {
        return parts.stream().map(PART_PARSER::parse).filter(Objects::nonNull).toList();
    }
}
