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

package io.agentscope.extensions.a2a.agent;

import io.a2a.client.Client;
import io.a2a.client.ClientBuilder;
import io.a2a.client.ClientEvent;
import io.a2a.client.TaskUpdateEvent;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfig;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Artifact;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import io.a2a.spec.UpdateEvent;
import io.agentscope.core.agent.AgentBase;
import io.agentscope.core.hook.ErrorEvent;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostCallEvent;
import io.agentscope.core.hook.PreCallEvent;
import io.agentscope.core.hook.ReasoningChunkEvent;
import io.agentscope.core.interruption.InterruptContext;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.extensions.a2a.agent.card.AgentCardProducer;
import io.agentscope.extensions.a2a.agent.utils.DateTimeSerializationUtil;
import io.agentscope.extensions.a2a.agent.utils.LoggerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * The implementation of Agent for A2A(Agent2Agent).
 *
 * <p>Example Usage:
 * <pre>{@code
 *  // Simple usage.
 *  AgentCard agentCard = generateAgentCardByCode();
 *  A2aAgent a2aAgent = new A2aAgent("remote-agent-name", agentCard);
 *
 *  // Auto get AgentCard
 *  AgentCardProducer agentCardProducer = new WellKnownAgentCardProducer("http://127.0.0.1:8080", "/.well-known/agent-card.json", Map.of());
 *  A2aAgentConfig a2aAgentConfig = A2aAgentConfig.builder().agentCardProducer(agentCardProducer).build()
 *  A2aAgent a2aAgent = new A2aAgent("remote-agent-name", a2aAgentConfig);
 * }</pre>
 *
 * @author xiweng.yy
 */
public class A2aAgent extends AgentBase {
    
    private static final Logger log = LoggerFactory.getLogger(A2aAgent.class);
    
    private static final String INTERRUPT_HINT_PATTERN = "Task %s interrupt successfully.";
    
    private final A2aAgentConfig a2aAgentConfig;
    
    private Client a2aClient;
    
    /**
     * According to the design, one agent should not be call with multiple threads and tasks at the same time.
     */
    private String currentTaskId;
    
    public A2aAgent(String name, AgentCard agentCard) {
        this(name, new A2aAgentConfig.A2aAgentConfigBuilder().agentCard(agentCard).build());
    }
    
    public A2aAgent(String name, A2aAgentConfig a2aAgentConfig) {
        this(name, a2aAgentConfig, null);
    }
    
    public A2aAgent(String name, A2aAgentConfig a2aAgentConfig, List<Hook> hooks) {
        super(name, hooks);
        this.a2aAgentConfig = a2aAgentConfig;
        LoggerUtil.debug(log, "A2aAgent init with config: {}", a2aAgentConfig);
        getHooks().add(new A2aClientLifecycleHook());
        AgentCardProducer agentCardProducer = a2aAgentConfig.agentCardProducer();
        if (null == agentCardProducer) {
            throw new IllegalArgumentException("AgentCardProducer cannot be null");
        }
        if (a2aAgentConfig.adaptOldVersionA2aDateTimeSerialization()) {
            DateTimeSerializationUtil.adaptOldVersionA2aDateTimeSerialization();
        }
    }
    
    @Override
    protected Mono<Msg> doCall(List<Msg> msgs) {
        LoggerUtil.info(log, "[{}] A2aAgent start call.", currentTaskId);
        LoggerUtil.debug(log, "[{}] A2aAgent call with input messages: ", currentTaskId);
        LoggerUtil.logTextMsgDetail(log, msgs);
        registerState("taskId", obj -> currentTaskId, obj -> obj);
        return Mono.defer(() -> {
            List<Part<?>> messageParts = msgs.stream().map(this::msgToParts).flatMap(Collection::stream).toList();
            Message message = new Message.Builder().taskId(currentTaskId).role(Message.Role.USER).parts(messageParts)
                    .build();
            return checkInterruptedAsync().then(doExecute(message));
        });
    }
    
    @Override
    public void interrupt() {
        super.interrupt();
        handleInterrupt(InterruptContext.builder().build()).block();
    }
    
    @Override
    public void interrupt(Msg msg) {
        super.interrupt(msg);
        handleInterrupt(InterruptContext.builder().userMessage(msg).build()).block();
    }
    
    @Override
    protected Mono<Void> doObserve(Msg msg) {
        // TODO Implement observe
        return Mono.empty();
    }
    
    @Override
    protected Mono<Msg> handleInterrupt(InterruptContext context, Msg... originalArgs) {
        LoggerUtil.debug(log, "[{}] A2aAgent handle interrupt.", currentTaskId);
        try {
            TaskIdParams taskIdParams = new TaskIdParams(currentTaskId);
            a2aClient.cancelTask(taskIdParams, null);
            return Mono.just(Msg.builder()
                    .content(TextBlock.builder().text(String.format(INTERRUPT_HINT_PATTERN, currentTaskId)).build())
                    .build());
        } catch (A2AClientException e) {
            return Mono.just(Msg.builder().content(TextBlock.builder().text(e.getMessage()).build()).build());
        }
    }
    
    private Client buildA2aClient(String name) {
        ClientBuilder builder = Client.builder(this.a2aAgentConfig.agentCardProducer().produce(name));
        if (this.a2aAgentConfig.clientTransports().isEmpty()) {
            // Default Add The Basic JSON-RPC Transport
            builder.withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig());
        } else {
            this.a2aAgentConfig.clientTransports().forEach(builder::withTransport);
        }
        return builder.build();
    }
    
    private Mono<Msg> doExecute(Message message) {
        return Mono.create(sink -> {
            BiConsumer<ClientEvent, AgentCard> a2aEventConsumer = (event, agentCard) -> {
                LoggerUtil.trace(log, "[{}] A2aAgent receive event {}: ", currentTaskId,
                        event.getClass().getSimpleName());
                LoggerUtil.logA2aClientEventDetail(log, event);
                if (event instanceof TaskUpdateEvent taskUpdateEvent) {
                    handleTaskUpdateEvent(sink, taskUpdateEvent);
                }
            };
            a2aClient.sendMessage(message, List.of(a2aEventConsumer), sink::error);
        });
    }
    
    private void handleTaskUpdateEvent(MonoSink<Msg> sink, TaskUpdateEvent taskUpdateEvent) {
        if (isTaskFinished(taskUpdateEvent.getUpdateEvent())) {
            Msg msg = buildMsgFromArtifact(taskUpdateEvent.getTask().getArtifacts());
            sink.success(msg);
            LoggerUtil.info(log, "[{}] A2aAgent complete call.", currentTaskId);
            LoggerUtil.debug(log, "[{}] A2aAgent complete with artifact messages: ", currentTaskId);
            LoggerUtil.logTextMsgDetail(log, List.of(msg));
        } else {
            if (taskUpdateEvent.getUpdateEvent() instanceof TaskStatusUpdateEvent) {
                TaskStatus taskStatus = taskUpdateEvent.getTask().getStatus();
                Msg msg = buildMsgFromParts(taskStatus.message().getParts());
                LoggerUtil.debug(log, "[{}] A2aAgent task status updated with messages: ", currentTaskId);
                LoggerUtil.logTextMsgDetail(log, List.of(msg));
                ReasoningChunkEvent event = new ReasoningChunkEvent(this, "A2A", null, msg, msg);
                getSortedHooks().forEach(hook -> hook.onEvent(event));
            }
        }
    }
    
    private boolean isTaskFinished(UpdateEvent updateEvent) {
        return updateEvent instanceof TaskStatusUpdateEvent taskStatusUpdateEvent && taskStatusUpdateEvent.isFinal();
    }
    
    private Msg buildMsgFromArtifact(List<Artifact> artifacts) {
        StringBuilder resultArtifact = new StringBuilder();
        for (Artifact each : artifacts) {
            resultArtifact.append(buildTextResultFromParts(each.parts()));
        }
        return Msg.builder().role(MsgRole.ASSISTANT)
                .content(TextBlock.builder().text(resultArtifact.toString()).build()).build();
    }
    
    private Msg buildMsgFromParts(List<Part<?>> parts) {
        return Msg.builder().role(MsgRole.ASSISTANT)
                .content(TextBlock.builder().text(buildTextResultFromParts(parts)).build()).build();
    }
    
    private String buildTextResultFromParts(List<Part<?>> parts) {
        StringBuilder resultMsg = new StringBuilder();
        parts.forEach(part -> {
            if (Part.Kind.TEXT.equals(part.getKind())) {
                resultMsg.append(((TextPart) part).getText());
            }
        });
        return resultMsg.toString();
    }
    
    private List<Part<?>> msgToParts(Msg msg) {
        return msg.getContent().stream().filter(block -> block instanceof TextBlock)
                .map((Function<ContentBlock, Part<?>>) this::extractTextMsgContent).toList();
    }
    
    private Part<?> extractTextMsgContent(ContentBlock contentBlock) {
        TextBlock textBlock = (TextBlock) contentBlock;
        String msgContent = textBlock.getText();
        return new TextPart(msgContent);
    }
    
    private class A2aClientLifecycleHook implements Hook {
        
        @Override
        public <T extends HookEvent> Mono<T> onEvent(T event) {
            if (event instanceof PreCallEvent preCallEvent) {
                currentTaskId = UUID.randomUUID().toString();
                a2aClient = buildA2aClient(preCallEvent.getAgent().getName());
                LoggerUtil.debug(log, "[{}] A2aAgent build A2a Client with Agent Card: {}.", currentTaskId,
                        a2aClient.getAgentCard());
            } else if (event instanceof PostCallEvent || event instanceof ErrorEvent) {
                a2aClient.close();
                a2aClient = null;
                LoggerUtil.debug(log, "[{}] A2aAgent close A2a Client.", currentTaskId);
            }
            return Mono.just(event);
        }
        
        @Override
        public int priority() {
            return Integer.MAX_VALUE;
        }
    }
}
