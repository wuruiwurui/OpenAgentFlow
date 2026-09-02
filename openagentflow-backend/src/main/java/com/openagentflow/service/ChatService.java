package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.domain.chat.ChatCompletionRequest;
import com.openagentflow.domain.chat.ChatCompletionResponse;
import com.openagentflow.domain.chat.ChatMessage;
import com.openagentflow.domain.chat.ChatRunContext;
import com.openagentflow.domain.chat.IntentRoutePlan;
import com.openagentflow.domain.chat.LlmCallResult;
import com.openagentflow.domain.chat.ToolCallRequest;
import com.openagentflow.domain.chat.ToolDefinitionForModel;
import com.openagentflow.domain.tool.ToolExecutionResult;
import com.openagentflow.domain.knowledge.KnowledgeSource;
import com.openagentflow.domain.knowledge.RagRetrievalOutcome;
import com.openagentflow.domain.memory.MemoryDtos;
import com.openagentflow.domain.model.ModelRouteDecision;
import com.openagentflow.domain.prompt.PromptRuntimeDtos;
import com.openagentflow.entity.AgentEntity;
import com.openagentflow.entity.AgentSessionEntity;
import com.openagentflow.entity.ModelConfigEntity;
import com.openagentflow.entity.ModelProviderEntity;
import com.openagentflow.entity.RuntimeLlmCallEntity;
import com.openagentflow.entity.RuntimeRunEntity;
import com.openagentflow.entity.RuntimeTraceStepEntity;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.mapper.AgentMapper;
import com.openagentflow.mapper.ModelConfigMapper;
import com.openagentflow.mapper.RuntimeLlmCallMapper;
import com.openagentflow.mapper.RuntimeRunMapper;
import com.openagentflow.mapper.RuntimeTraceStepMapper;
import com.openagentflow.security.AuthUserDetails;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 聊天调试服务。
 *
 * <p>负责把前端输入转换为模型消息、调用 OpenAI-compatible 客户端，并记录运行日志和 Trace。</p>
 */
@Service
public class ChatService {

    /** Agent Mapper。 */
    private final AgentMapper agentMapper;

    /** Agent 资源级访问控制服务。 */
    private final AgentAccessService agentAccessService;

    /** 模型配置 Mapper。 */
    private final ModelConfigMapper modelConfigMapper;

    /** 运行记录 Mapper。 */
    private final RuntimeRunMapper runtimeRunMapper;

    /** 运行链路步骤 Mapper。 */
    private final RuntimeTraceStepMapper runtimeTraceStepMapper;

    /** LLM 调用日志 Mapper。 */
    private final RuntimeLlmCallMapper runtimeLlmCallMapper;

    /** 模型服务商服务。 */
    private final ModelProviderService modelProviderService;

    /** OpenAI-compatible 调用客户端。 */
    private final OpenAiCompatibleClient openAiCompatibleClient;

    /** 知识库 RAG 服务。 */
    private final KnowledgeBaseService knowledgeBaseService;

    /** 工具调用服务。 */
    private final ToolService toolService;

    /** Agent 历史会话服务。 */
    private final AgentSessionService agentSessionService;

    /** 成本与用量服务。 */
    private final UsageCostService usageCostService;

    /** 模型网关服务。 */
    private final ModelGatewayService modelGatewayService;

    /** Memory 记忆中心服务。 */
    private final MemoryService memoryService;

    /** JSON 序列化工具。 */
    private final ObjectMapper objectMapper;

    /** Agent Runtime专用有界执行器。 */
    private final TaskExecutor agentRuntimeExecutor;

    /** Runtime分布式停止控制服务。 */
    private final RuntimeControlService runtimeControlService;

    /** AI输入输出安全护栏。 */
    private final AiGuardrailService aiGuardrailService;

    /** Runtime流式事件断线续传服务。 */
    private final RuntimeEventStreamService runtimeEventStreamService;

    /** 统一Prompt Runtime服务。 */
    private final PromptRuntimeService promptRuntimeService;

    /** 通用多意图路由策略。 */
    private final IntentRoutingPolicy intentRoutingPolicy;

    public ChatService(AgentMapper agentMapper,
                       AgentAccessService agentAccessService,
                       ModelConfigMapper modelConfigMapper,
                       RuntimeRunMapper runtimeRunMapper,
                       RuntimeTraceStepMapper runtimeTraceStepMapper,
                       RuntimeLlmCallMapper runtimeLlmCallMapper,
                       ModelProviderService modelProviderService,
                       OpenAiCompatibleClient openAiCompatibleClient,
                       KnowledgeBaseService knowledgeBaseService,
                       ToolService toolService,
                       AgentSessionService agentSessionService,
                       UsageCostService usageCostService,
                       ModelGatewayService modelGatewayService,
                       MemoryService memoryService,
                       ObjectMapper objectMapper,
                       @org.springframework.beans.factory.annotation.Qualifier("agentRuntimeExecutor") TaskExecutor agentRuntimeExecutor,
                       RuntimeControlService runtimeControlService,
                       AiGuardrailService aiGuardrailService,
                       RuntimeEventStreamService runtimeEventStreamService,
                       PromptRuntimeService promptRuntimeService,
                       IntentRoutingPolicy intentRoutingPolicy) {
        this.agentMapper = agentMapper;
        this.agentAccessService = agentAccessService;
        this.modelConfigMapper = modelConfigMapper;
        this.runtimeRunMapper = runtimeRunMapper;
        this.runtimeTraceStepMapper = runtimeTraceStepMapper;
        this.runtimeLlmCallMapper = runtimeLlmCallMapper;
        this.modelProviderService = modelProviderService;
        this.openAiCompatibleClient = openAiCompatibleClient;
        this.knowledgeBaseService = knowledgeBaseService;
        this.toolService = toolService;
        this.agentSessionService = agentSessionService;
        this.usageCostService = usageCostService;
        this.modelGatewayService = modelGatewayService;
        this.memoryService = memoryService;
        this.objectMapper = objectMapper;
        this.agentRuntimeExecutor = agentRuntimeExecutor;
        this.runtimeControlService = runtimeControlService;
        this.aiGuardrailService = aiGuardrailService;
        this.runtimeEventStreamService = runtimeEventStreamService;
        this.promptRuntimeService = promptRuntimeService;
        this.intentRoutingPolicy = intentRoutingPolicy;
    }

    /**
     * 执行普通非流式聊天补全。
     *
     * @param request 聊天补全请求
     * @return 聊天补全响应
     */
    @Transactional(rollbackFor = Exception.class)
    public ChatCompletionResponse complete(ChatCompletionRequest request) {
        aiGuardrailService.inspectInput(request);
        ChatRunContext context = buildRunContext(request);
        attachSession(request, context);
        RuntimeRunEntity run = createRun(request, context);
        context.setRunId(run.getId());
        recordIntentRouteStep(run.getId(), request.getInput(), context.getIntentRoutePlan());
        enrichContextWithMemory(context, request);
        if (shouldRetrieveKnowledge(context)) {
            enrichContextWithRag(context, request, run.getId());
        }
        if (shouldRejectByTrustedAnswer(context)) {
            return completeTrustedAnswerRejected(run, context, false);
        }
        if (shouldClarifyRoute(context)) {
            return completeClarification(run, context, false);
        }
        if (context.getTools() != null && !context.getTools().isEmpty()) {
            return completeWithToolCalling(request, context, run);
        }
        RuntimeTraceStepEntity step = createLlmStep(run, context);
        try {
            LlmCallResult result = invokeWithGatewayFallback(context,
                    current -> openAiCompatibleClient.complete(current, request.getTemperature(), effectiveMaxTokens(request, current)),
                    current -> usageCostService.assertWithinQuota(run.getUserId(), run.getAgentId(), current.getProvider(), current.getModel(), current.getMessages(), effectiveMaxTokens(request, current)));
            finishSuccess(run, step, context, result, false);
            return toResponse(run, context, result, "SUCCESS", null);
        } catch (Exception exception) {
            finishFailure(run, step, context, exception, false);
            throw exception;
        }
    }

    /**
     * 执行 SSE 流式聊天补全。
     *
     * @param request 聊天补全请求
     * @return SSE 发射器
     */
    public SseEmitter completeStream(ChatCompletionRequest request) {
        aiGuardrailService.inspectInput(request);
        SseEmitter emitter = new SseEmitter(180_000L);
        // SecurityContext 不会自动传入异步线程，因此先在请求线程完成权限判断、上下文构建和运行记录创建。
        ChatRunContext context = buildRunContext(request);
        attachSession(request, context);
        RuntimeRunEntity run = createRun(request, context);
        context.setRunId(run.getId());
        recordIntentRouteStep(run.getId(), request.getInput(), context.getIntentRoutePlan());
        runtimeEventStreamService.bind(emitter, run.getId());
        emitter.onTimeout(() -> runtimeControlService.cancel(run.getId()));
        enrichContextWithMemory(context, request);
        if (shouldRetrieveKnowledge(context)) {
            enrichContextWithRag(context, request, run.getId());
        }
        if (shouldRejectByTrustedAnswer(context)) {
            completeStreamTrustedAnswerRejected(emitter, run, context);
            return emitter;
        }
        if (shouldClarifyRoute(context)) {
            completeStreamClarification(emitter, run, context);
            return emitter;
        }
        if (context.getTools() != null && !context.getTools().isEmpty()) {
            completeStreamWithToolCalling(emitter, request, context, run);
            return emitter;
        }
        RuntimeTraceStepEntity step = createLlmStep(run, context);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CompletableFuture.runAsync(() -> {
            try {
                // SSE 真正调用模型在异步线程执行，需要恢复登录上下文，避免 Memory 自动沉淀等后置动作误判未登录。
                SecurityContextHolder.getContext().setAuthentication(authentication);
                sendSse(emitter, "meta", buildStreamMeta(run, context, false));
                long streamStartedAt = System.nanoTime();
                AtomicLong firstTokenLatencyMs = new AtomicLong(0L);
                LlmCallResult result = invokeWithGatewayFallback(context,
                        current -> openAiCompatibleClient.completeStream(
                                current,
                                request.getTemperature(),
                                effectiveMaxTokens(request, current),
                                delta -> {
                                    ensureRuntimeActive(run.getId());
                                    if (StringUtils.hasText(delta)) {
                                        firstTokenLatencyMs.compareAndSet(0L, java.util.concurrent.TimeUnit.NANOSECONDS
                                                .toMillis(System.nanoTime() - streamStartedAt));
                                    }
                                    sendSse(emitter, "delta", Map.of("content", delta));
                                }
                        ),
                        current -> usageCostService.assertWithinQuota(run.getUserId(), run.getAgentId(), current.getProvider(), current.getModel(), current.getMessages(), effectiveMaxTokens(request, current)));
                result.setFirstTokenLatencyMs((int) Math.min(Integer.MAX_VALUE, firstTokenLatencyMs.get()));
                finishSuccess(run, step, context, result, true);
                sendDone(emitter, run, context, result, List.of());
                emitter.complete();
            } catch (Exception exception) {
                finishFailure(run, step, context, exception, true);
                sendSse(emitter, "error", Map.of("message", safeText(exception.getMessage())));
                emitter.complete();
            } finally {
                // 清理线程上下文，避免线程池复用时串到其他用户。
                SecurityContextHolder.clearContext();
            }
        }, agentRuntimeExecutor);
        return emitter;
    }

    /**
     * 构建聊天运行上下文。
     *
     * @param request 聊天请求
     * @return 聊天运行上下文
     */
    private ChatRunContext buildRunContext(ChatCompletionRequest request) {
        AgentEntity agent = resolveAgent(request.getAgentId());
        ModelRouteDecision routeDecision = modelGatewayService.resolveAgentChatRoute(request.getModelId(), agent);
        ModelConfigEntity model = routeDecision.getModel();
        ModelProviderEntity provider = routeDecision.getProvider();
        String apiKey = routeDecision.getApiKey();

        ChatRunContext context = new ChatRunContext();
        context.setAgent(agent);
        context.setModel(model);
        context.setProvider(provider);
        context.setApiKey(apiKey);
        context.setRouteDecision(routeDecision);
        PromptRuntimeDtos.CompileResult promptCompileResult = agent == null
                ? null : promptRuntimeService.compileForAgent(agent, request.getInput(), request.getSessionId());
        context.setPromptCompileResult(promptCompileResult);
        context.setMessages(buildMessages(agent, request, promptCompileResult));
        context.setSources(List.of());
        List<ToolDefinitionForModel> agentTools = toolService.listModelToolsForAgent(agent);
        boolean ragAvailable = agent != null && knowledgeBaseService.hasEnabledKnowledgeBindings(agent.getId());
        IntentRoutePlan routePlan = intentRoutingPolicy.plan(request.getInput(), agentTools, ragAvailable);
        context.setIntentRoutePlan(routePlan);
        context.setTools(selectPlannedTools(agentTools, routePlan));
        context.setMessages(injectIntentRoutePrompt(context.getMessages(), routePlan));
        return context;
    }

    /**
     * 根据结构化计划选择本轮允许暴露给模型的工具。
     *
     * @param tools Agent 已绑定且启用的工具
     * @param routePlan 结构化意图计划
     * @return 过滤后的工具列表
     */
    private List<ToolDefinitionForModel> selectPlannedTools(List<ToolDefinitionForModel> tools, IntentRoutePlan routePlan) {
        if (tools == null || tools.isEmpty() || routePlan == null || !routePlan.isNeedTool()
                || routePlan.isNeedsClarification()) {
            return List.of();
        }
        List<String> selectedNames = routePlan.getSelectedToolNames();
        return tools.stream()
                .filter(tool -> selectedNames.contains(tool.getName()))
                .toList();
    }

    /**
     * 将通用意图计划注入模型上下文，约束工具与知识来源的职责边界。
     *
     * @param messages 原始消息列表
     * @param routePlan 结构化意图计划
     * @return 注入提示后的消息列表
     */
    private List<ChatMessage> injectIntentRoutePrompt(List<ChatMessage> messages, IntentRoutePlan routePlan) {
        if (messages == null || messages.isEmpty() || routePlan == null) {
            return messages;
        }
        String routingPrompt = "本轮结构化意图路由计划如下：" + toJson(routePlan)
                + "。只能调用 selectedToolNames 中列出的工具，不得根据历史会话复用旧工具结果。";
        if (routePlan.isNeedsClarification()) {
            routingPrompt += "当前缺少工具必填实体，本轮禁止调用工具。可以回答互不依赖的知识问题，"
                    + "并向用户提出该澄清问题：" + safeText(routePlan.getClarificationQuestion()) + "。";
        } else if (routePlan.isNeedTool() && routePlan.isNeedRag()) {
            routingPrompt += "这是多意图请求：实时或外部数据以本轮工具结果为准，制度和说明性结论以本轮知识来源为准，最后合并回答。";
        } else if (routePlan.isNeedTool()) {
            routingPrompt += "需要实时或外部数据时调用候选工具，最终回答必须基于本轮工具结果。";
        } else if (routePlan.isNeedRag()) {
            routingPrompt += "本轮没有可靠工具候选，请依据本轮知识来源回答，不得声称已调用工具。";
        } else {
            routingPrompt += "本轮属于直接对话，不调用工具和知识库。";
        }
        List<ChatMessage> routedMessages = new ArrayList<>(messages);
        int insertIndex = Math.max(0, routedMessages.size() - 1);
        routedMessages.add(insertIndex, new ChatMessage("system", routingPrompt));
        return routedMessages;
    }

    /**
     * 为本次聊天绑定历史会话。
     *
     * @param request 聊天请求
     * @param context 聊天上下文
     */
    private void attachSession(ChatCompletionRequest request, ChatRunContext context) {
        AgentSessionEntity session = agentSessionService.ensureSession(context.getAgent(), request.getSessionId(), request.getInput());
        if (session != null) {
            context.setSessionId(session.getId());
            request.setSessionId(session.getId());
        }
    }

    /**
     * 执行带工具调用的非流式聊天。
     *
     * @param request 聊天请求
     * @param context 聊天上下文
     * @param run 运行记录
     * @return 聊天响应
     */
    private ChatCompletionResponse completeWithToolCalling(ChatCompletionRequest request,
                                                           ChatRunContext context,
                                                           RuntimeRunEntity run) {
        RuntimeTraceStepEntity decisionStep = createLlmStep(run, context);
        try {
            LlmCallResult decision = invokeWithGatewayFallback(context,
                    current -> openAiCompatibleClient.completeWithTools(current, request.getTemperature(), effectiveMaxTokens(request, current)),
                    current -> usageCostService.assertWithinQuota(run.getUserId(), run.getAgentId(), current.getProvider(), current.getModel(), current.getMessages(), effectiveMaxTokens(request, current)));
            if (decision.getToolCalls() == null || decision.getToolCalls().isEmpty()) {
                finishSuccess(run, decisionStep, context, decision, false);
                return toResponse(run, context, decision, "SUCCESS", null);
            }
            finishIntermediateLlmStep(run, decisionStep, context, decision, false);
            List<Map<String, Object>> toolResults = executeToolCalls(context, run, decisionStep.getId(), decision);
            RuntimeTraceStepEntity finalStep = createLlmStep(run, context);
            LlmCallResult finalResult = invokeWithGatewayFallback(context,
                    current -> openAiCompatibleClient.complete(current, request.getTemperature(), effectiveMaxTokens(request, current)),
                    current -> usageCostService.assertWithinQuota(run.getUserId(), run.getAgentId(), current.getProvider(), current.getModel(), current.getMessages(), effectiveMaxTokens(request, current)));
            finishSuccess(run, finalStep, context, finalResult, false);
            ChatCompletionResponse response = toResponse(run, context, finalResult, "SUCCESS", null);
            response.setToolResults(toolResults);
            return response;
        } catch (Exception exception) {
            finishFailure(run, decisionStep, context, exception, false);
            throw exception;
        }
    }

    /**
     * 执行带工具调用的 SSE 聊天。
     *
     * @param emitter SSE 发射器
     * @param request 聊天请求
     * @param context 聊天上下文
     * @param run 运行记录
     */
    private void completeStreamWithToolCalling(SseEmitter emitter,
                                               ChatCompletionRequest request,
                                               ChatRunContext context,
                                               RuntimeRunEntity run) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CompletableFuture.runAsync(() -> {
            // 带工具调用的流式链路同样需要登录上下文，工具权限、Memory 和 Trace 后置动作会读取当前用户。
            SecurityContextHolder.getContext().setAuthentication(authentication);
            RuntimeTraceStepEntity decisionStep = createLlmStep(run, context);
            try {
                sendSse(emitter, "meta", buildStreamMeta(run, context, true));
                LlmCallResult decision = invokeWithGatewayFallback(context,
                        current -> openAiCompatibleClient.completeWithTools(current, request.getTemperature(), effectiveMaxTokens(request, current)),
                        current -> usageCostService.assertWithinQuota(run.getUserId(), run.getAgentId(), current.getProvider(), current.getModel(), current.getMessages(), effectiveMaxTokens(request, current)));
                if (decision.getToolCalls() == null || decision.getToolCalls().isEmpty()) {
                    // 模型未选择工具时直接返回本次非流式决策内容，避免重复调用模型。
                    finishSuccess(run, decisionStep, context, decision, false);
                    sendSse(emitter, "delta", Map.of("content", safeText(decision.getContent())));
                    sendDone(emitter, run, context, decision, List.of());
                    emitter.complete();
                    return;
                }
                finishIntermediateLlmStep(run, decisionStep, context, decision, false);
                List<Map<String, Object>> toolResults = executeToolCalls(context, run, decisionStep.getId(), decision);
                sendSse(emitter, "tool", Map.of("toolResults", toolResults));
                RuntimeTraceStepEntity finalStep = createLlmStep(run, context);
                LlmCallResult finalResult = invokeWithGatewayFallback(context,
                        current -> openAiCompatibleClient.completeStream(
                                current,
                                request.getTemperature(),
                                effectiveMaxTokens(request, current),
                                delta -> {
                                    ensureRuntimeActive(run.getId());
                                    sendSse(emitter, "delta", Map.of("content", delta));
                                }
                        ),
                        current -> usageCostService.assertWithinQuota(run.getUserId(), run.getAgentId(), current.getProvider(), current.getModel(), current.getMessages(), effectiveMaxTokens(request, current)));
                finishSuccess(run, finalStep, context, finalResult, true);
                sendDone(emitter, run, context, finalResult, toolResults);
                emitter.complete();
            } catch (Exception exception) {
                finishFailure(run, decisionStep, context, exception, false);
                sendSse(emitter, "error", Map.of("message", safeText(exception.getMessage())));
                emitter.complete();
            } finally {
                // 清理线程上下文，避免线程池复用时串到其他用户。
                SecurityContextHolder.clearContext();
            }
        }, agentRuntimeExecutor);
    }

    /**
     * 执行模型请求的工具调用，并把工具结果追加回模型上下文。
     *
     * @param context 聊天上下文
     * @param run 运行记录
     * @param parentStepId 父步骤 ID
     * @param decision 模型工具决策结果
     * @return 工具结果摘要
     */
    private List<Map<String, Object>> executeToolCalls(ChatRunContext context,
                                                       RuntimeRunEntity run,
                                                       String parentStepId,
                                                       LlmCallResult decision) {
        List<ChatMessage> messages = new ArrayList<>(context.getMessages());
        ChatMessage assistantToolMessage = new ChatMessage("assistant", safeText(decision.getContent()));
        assistantToolMessage.setToolCalls(decision.getToolCalls());
        messages.add(assistantToolMessage);

        List<Map<String, Object>> results = new ArrayList<>();
        for (ToolCallRequest call : decision.getToolCalls()) {
            ToolExecutionResult result = toolService.executeToolCallForAgent(context.getAgent(), run, parentStepId, call);
            Map<String, Object> resultPayload = new LinkedHashMap<>();
            resultPayload.put("toolCallId", call.getId());
            resultPayload.put("toolName", call.getName());
            resultPayload.put("success", Boolean.TRUE.equals(result.getSuccess()));
            resultPayload.put("statusCode", result.getStatusCode());
            resultPayload.put("latencyMs", result.getLatencyMs());
            resultPayload.put("confirmationRequired", Boolean.TRUE.equals(result.getConfirmationRequired()));
            resultPayload.put("confirmationId", result.getConfirmationId());
            resultPayload.put("responseBody", safeText(result.getResponseBody()));
            resultPayload.put("errorMessage", safeText(result.getErrorMessage()));
            results.add(resultPayload);

            ChatMessage toolMessage = new ChatMessage("tool", toJson(resultPayload));
            toolMessage.setToolCallId(call.getId());
            toolMessage.setName(call.getName());
            messages.add(toolMessage);
        }

        messages.add(new ChatMessage("system", "以上是工具执行结果。请结合用户问题、知识库资料和工具结果生成最终回答；如果工具未执行或失败，请明确说明原因。"));
        context.setMessages(messages);
        return results;
    }

    /**
     * 执行 Memory 召回并把记忆上下文注入模型消息。
     *
     * @param context 聊天上下文
     * @param request 聊天请求
     */
    private void enrichContextWithMemory(ChatRunContext context, ChatCompletionRequest request) {
        if (context.getAgent() == null || !StringUtils.hasText(request.getInput())) {
            context.setMemories(List.of());
            return;
        }
        List<MemoryDtos.RecallItem> memories = memoryService.recallForChat(context.getAgent(), context.getSessionId(), request.getInput(), 5);
        context.setMemories(memories);
        String memoryPrompt = memoryService.buildMemoryPrompt(memories);
        if (!StringUtils.hasText(memoryPrompt)) {
            return;
        }
        List<ChatMessage> messages = new ArrayList<>(context.getMessages());
        messages.add(1, new ChatMessage("system", memoryPrompt));
        context.setMessages(messages);
    }

    /**
     * 执行 RAG 检索并把引用上下文注入模型消息。
     *
     * @param context 聊天上下文
     * @param request 聊天请求
     * @param runId 运行 ID
     */
    private void enrichContextWithRag(ChatRunContext context, ChatCompletionRequest request, String runId) {
        if (context.getAgent() == null || !StringUtils.hasText(request.getInput())) {
            return;
        }
        RuntimeTraceStepEntity ragStep = createRagStep(runId, request);
        RagRetrievalOutcome outcome;
        try {
            outcome = knowledgeBaseService.retrieveForAgentWithPolicy(
                    context.getAgent(), request.getInput(), runId, recentConversationContext(context));
            finishRagStep(ragStep, outcome, null);
        } catch (Exception exception) {
            finishRagStep(ragStep, null, exception);
            throw exception;
        }
        List<KnowledgeSource> sources = outcome.getSources() == null ? List.of() : outcome.getSources();
        aiGuardrailService.inspectRetrievedContent(context.getAgent().getWorkspaceId(), runId,
                context.getAgent().getId(), sources);
        context.setSources(sources);
        context.setRagTrustedAnswerMode(Boolean.TRUE.equals(outcome.getTrustedAnswerMode()));
        context.setRagAnswerable(outcome.getAnswerable() == null || Boolean.TRUE.equals(outcome.getAnswerable()));
        context.setRagRejectReason(outcome.getRejectReason());
        context.setRagConfidenceScore(outcome.getConfidenceScore());
        context.setRagMinCitationCount(outcome.getMinCitationCount());
        context.setRagCitationRequired(Boolean.TRUE.equals(outcome.getCitationRequired()));
        context.setRagQualityAdvice(outcome.getQualityAdvice());
        context.setRagEnhancedQueries(outcome.getEnhancedQueries());
        context.setRagCanonicalQuery(outcome.getCanonicalQuery());
        context.setRagRerankMode(outcome.getRerankMode());
        context.setRagRerankModelId(outcome.getRerankModelId());
        context.setRagRerankLatencyMs(outcome.getRerankLatencyMs());
        context.setRagRerankErrorMessage(outcome.getRerankErrorMessage());
        if (shouldRejectByTrustedAnswer(context)) {
            return;
        }
        if (sources.isEmpty()) {
            if (knowledgeBaseService.hasEnabledKnowledgeBindings(context.getAgent().getId())) {
                List<ChatMessage> messages = new ArrayList<>(context.getMessages());
                messages.add(1, new ChatMessage("system", "已绑定知识库，但本次 RAG 检索没有召回达到置信阈值的可靠资料。请不要编造知识库中不存在的事实；如果问题依赖企业知识库，请明确说明“当前知识库资料不足，无法确认”。"));
                context.setMessages(messages);
            }
            return;
        }
        List<ChatMessage> messages = new ArrayList<>(context.getMessages());
        messages.add(1, new ChatMessage("system", buildRagPrompt(sources, context)));
        context.setMessages(messages);
    }

    /**
     * 判断结构化路由计划是否要求执行知识检索。
     *
     * @param context 聊天运行上下文
     * @return 是否进入 RAG 链路
     */
    private boolean shouldRetrieveKnowledge(ChatRunContext context) {
        return context.getIntentRoutePlan() == null || context.getIntentRoutePlan().isNeedRag();
    }

    /**
     * 写入通用意图路由 Trace，让运行详情能够解释工具和 RAG 的选择依据。
     *
     * @param runId 运行 ID
     * @param input 用户原始输入
     * @param routePlan 结构化意图计划
     */
    private void recordIntentRouteStep(String runId, String input, IntentRoutePlan routePlan) {
        if (routePlan == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        RuntimeTraceStepEntity step = new RuntimeTraceStepEntity();
        step.setId(newId());
        step.setRunId(runId);
        step.setStepKey("intent_route");
        step.setStepName("多意图规划与路由");
        step.setStepType("ROUTER");
        step.setStatus("SUCCESS");
        step.setInputPayload(toJson(Map.of("input", safeText(input))));
        step.setOutputPayload(toJson(routePlan));
        step.setTokenUsage("{}");
        step.setCostAmount(BigDecimal.ZERO);
        step.setLatencyMs(0);
        step.setStartedAt(now);
        step.setFinishedAt(now);
        runtimeTraceStepMapper.insert(step);
    }

    /**
     * 创建 RAG 检索 Trace 步骤。
     *
     * @param runId 运行 ID
     * @param request 聊天请求
     * @return Trace 步骤
     */
    private RuntimeTraceStepEntity createRagStep(String runId, ChatCompletionRequest request) {
        RuntimeTraceStepEntity step = new RuntimeTraceStepEntity();
        step.setId(newId());
        step.setRunId(runId);
        step.setStepKey("rag_retrieve");
        step.setStepName("RAG 知识检索");
        step.setStepType("RAG");
        step.setStatus("RUNNING");
        step.setInputPayload(toJson(Map.of("query", request.getInput())));
        step.setTokenUsage("{}");
        step.setCostAmount(BigDecimal.ZERO);
        step.setStartedAt(LocalDateTime.now());
        runtimeTraceStepMapper.insert(step);
        return step;
    }

    /**
     * 完成 RAG 检索 Trace 步骤。
     *
     * @param step Trace 步骤
     * @param sources 引用来源
     * @param exception 异常对象
     */
    private void finishRagStep(RuntimeTraceStepEntity step, RagRetrievalOutcome outcome, Exception exception) {
        List<KnowledgeSource> sources = outcome == null || outcome.getSources() == null ? List.of() : outcome.getSources();
        step.setStatus(exception == null ? "SUCCESS" : "FAILED");
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("sources", sources);
        output.put("trustedAnswer", outcome == null ? Map.of() : Map.of(
                "enabled", Boolean.TRUE.equals(outcome.getTrustedAnswerMode()),
                "answerable", outcome.getAnswerable() == null || Boolean.TRUE.equals(outcome.getAnswerable()),
                "citationRequired", Boolean.TRUE.equals(outcome.getCitationRequired()),
                "minCitationCount", outcome.getMinCitationCount() == null ? 0 : outcome.getMinCitationCount(),
                "confidenceScore", outcome.getConfidenceScore() == null ? 0D : outcome.getConfidenceScore(),
                "rejectReason", safeText(outcome.getRejectReason()),
                "qualityAdvice", safeText(outcome.getQualityAdvice())
        ));
        if (outcome != null) {
            // Trace同时记录查询理解和重排状态，方便定位“为什么没有召回”。
            output.put("originalQuery", safeText(outcome.getOriginalQuery()));
            output.put("canonicalQuery", safeText(outcome.getCanonicalQuery()));
            output.put("enhancedQueries", outcome.getEnhancedQueries() == null ? List.of() : outcome.getEnhancedQueries());
            output.put("contextResolved", Boolean.TRUE.equals(outcome.getContextResolved()));
            output.put("rerankMode", safeText(outcome.getRerankMode()));
            output.put("rerankModelId", safeText(outcome.getRerankModelId()));
            output.put("rerankLatencyMs", outcome.getRerankLatencyMs() == null ? 0 : outcome.getRerankLatencyMs());
            output.put("rerankErrorMessage", safeText(outcome.getRerankErrorMessage()));
        }
        step.setOutputPayload(toJson(output));
        step.setLatencyMs((int) java.time.Duration.between(step.getStartedAt(), LocalDateTime.now()).toMillis());
        step.setErrorMessage(exception == null ? null : exception.getMessage());
        step.setFinishedAt(LocalDateTime.now());
        runtimeTraceStepMapper.updateById(step);
    }

    /**
     * 构建 RAG 引用提示词。
     *
     * @param sources 引用来源列表
     * @return 注入模型的上下文提示
     */
    private String buildRagPrompt(List<KnowledgeSource> sources, ChatRunContext context) {
        StringBuilder builder = new StringBuilder();
        if (Boolean.TRUE.equals(context.getRagTrustedAnswerMode())) {
            builder.append("已启用 RAG 可信回答模式。你只能依据下面的企业知识库来源回答，不得使用未被来源支持的事实；")
                    .append("如果来源不足或无法覆盖用户问题，必须明确说明“当前知识库资料不足，无法确认”。");
            if (Boolean.TRUE.equals(context.getRagCitationRequired())) {
                builder.append("回答中的关键结论必须使用 [来源1]、[来源2] 这样的编号标注依据。");
            }
            builder.append("\n");
        } else {
            builder.append("以下是从企业知识库检索到的参考资料。请优先依据资料回答；如果资料不足，请明确说明不确定。回答中可以用 [来源1]、[来源2] 标注依据。\n");
        }
        for (int index = 0; index < sources.size(); index++) {
            KnowledgeSource source = sources.get(index);
            builder.append("\n[来源").append(index + 1).append("] ")
                    .append(source.getDocumentName()).append(" / 分片 ").append(source.getChunkNo())
                    .append("，相似度 ").append(String.format(java.util.Locale.ROOT, "%.4f", source.getScore()))
                    .append("\n")
                    .append(source.getQuoteText());
        }
        return builder.toString();
    }

    /**
     * 判断本轮是否应该被可信回答模式拦截。
     *
     * @param context 聊天上下文
     * @return 是否需要拒答
     */
    private boolean shouldRejectByTrustedAnswer(ChatRunContext context) {
        return context != null
                && Boolean.TRUE.equals(context.getRagTrustedAnswerMode())
                && Boolean.FALSE.equals(context.getRagAnswerable());
    }

    /**
     * 判断是否需要由路由器直接澄清，而不是把不完整请求交给模型自由发挥。
     * <p>如果同一请求还有独立的知识咨询意图，仍允许模型回答知识部分并带出澄清问题。</p>
     *
     * @param context 聊天运行上下文
     * @return 是否直接返回澄清响应
     */
    private boolean shouldClarifyRoute(ChatRunContext context) {
        return context != null
                && context.getIntentRoutePlan() != null
                && context.getIntentRoutePlan().isNeedsClarification()
                && !context.getIntentRoutePlan().isNeedRag();
    }

    /**
     * 构造必填实体澄清文本。
     *
     * @param context 聊天运行上下文
     * @return 澄清内容
     */
    private String clarificationContent(ChatRunContext context) {
        String question = context == null || context.getIntentRoutePlan() == null
                ? "请补充执行该操作所需的信息。"
                : safeText(context.getIntentRoutePlan().getClarificationQuestion());
        return StringUtils.hasText(question) ? question : "请补充执行该操作所需的信息。";
    }

    /**
     * 保存确定性的路由澄清结果。
     * <p>该响应不调用 LLM，因此不会产生虚假 Token 和工具执行记录。</p>
     *
     * @param run 运行记录
     * @param context 聊天运行上下文
     * @param stream 是否流式
     * @return 澄清响应
     */
    private ChatCompletionResponse completeClarification(RuntimeRunEntity run,
                                                          ChatRunContext context,
                                                          boolean stream) {
        String content = clarificationContent(context);
        LocalDateTime finishedAt = LocalDateTime.now();
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("content", content);
        output.put("intentRoute", context.getIntentRoutePlan());
        output.put("clarificationRequired", true);
        run.setOutputText(content);
        run.setOutputPayload(toJson(output));
        run.setStatus("SUCCESS");
        run.setPromptTokens(0);
        run.setCompletionTokens(0);
        run.setTotalTokens(0);
        run.setTotalCost(BigDecimal.ZERO);
        run.setLatencyMs((int) java.time.Duration.between(run.getStartedAt(), finishedAt).toMillis());
        run.setFinishedAt(finishedAt);
        runtimeRunMapper.updateById(run);
        agentSessionService.appendAssistantMessage(context.getSessionId(), content, 0, Map.of(
                "runId", run.getId(),
                "status", "SUCCESS",
                "stream", stream,
                "clarificationRequired", true,
                "missingEntities", context.getIntentRoutePlan().getMissingEntities()
        ));
        LlmCallResult result = new LlmCallResult();
        result.setContent(content);
        result.setLatencyMs(run.getLatencyMs());
        result.setPromptTokens(0);
        result.setCompletionTokens(0);
        result.setTotalTokens(0);
        return toResponse(run, context, result, "SUCCESS", null);
    }

    /**
     * 以 SSE 形式输出确定性的路由澄清结果。
     *
     * @param emitter SSE 发射器
     * @param run 运行记录
     * @param context 聊天运行上下文
     */
    private void completeStreamClarification(SseEmitter emitter,
                                              RuntimeRunEntity run,
                                              ChatRunContext context) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CompletableFuture.runAsync(() -> {
            try {
                // 异步线程恢复认证上下文，保证澄清消息仍按当前用户写入历史会话。
                SecurityContextHolder.getContext().setAuthentication(authentication);
                ChatCompletionResponse response = completeClarification(run, context, true);
                sendSse(emitter, "meta", buildStreamMeta(run, context, false));
                sendSse(emitter, "delta", Map.of("content", response.getContent()));
                LlmCallResult clarificationResult = new LlmCallResult();
                clarificationResult.setLatencyMs(response.getLatencyMs());
                sendDone(emitter, run, context, clarificationResult, List.of());
                emitter.complete();
            } catch (Exception exception) {
                sendSse(emitter, "error", Map.of("message", safeText(exception.getMessage())));
                emitter.complete();
            } finally {
                // 清理线程上下文，避免线程池复用时串到其他用户。
                SecurityContextHolder.clearContext();
            }
        }, agentRuntimeExecutor);
    }

    /**
     * 生成可信回答模式拒答文本。
     *
     * @param context 聊天上下文
     * @return 拒答文本
     */
    private String trustedRejectContent(ChatRunContext context) {
        String reason = StringUtils.hasText(context.getRagRejectReason())
                ? context.getRagRejectReason()
                : "本轮没有召回足够可靠的知识库引用来源";
        String advice = StringUtils.hasText(context.getRagQualityAdvice())
                ? "\n\n建议：" + context.getRagQualityAdvice()
                : "";
        return "当前知识库资料不足，无法确认。\n\n可信回答模式已启用，本次回答被拦截，原因：" + reason + advice;
    }

    /**
     * 完成可信回答模式拒答。
     *
     * @param run 运行记录
     * @param context 聊天上下文
     * @param stream 是否流式
     * @return 聊天响应
     */
    private ChatCompletionResponse completeTrustedAnswerRejected(RuntimeRunEntity run, ChatRunContext context, boolean stream) {
        String content = trustedRejectContent(context);
        LocalDateTime finishedAt = LocalDateTime.now();
        run.setOutputText(content);
        run.setOutputPayload(toJson(Map.of(
                "content", content,
                "memories", context.getMemories() == null ? List.of() : context.getMemories(),
                "sources", context.getSources() == null ? List.of() : context.getSources(),
                "trustedAnswer", trustedAnswerPayload(context)
        )));
        run.setStatus("SUCCESS");
        run.setLatencyMs((int) java.time.Duration.between(run.getStartedAt(), finishedAt).toMillis());
        run.setFinishedAt(finishedAt);
        runtimeRunMapper.updateById(run);
        agentSessionService.appendAssistantMessage(context.getSessionId(), content, 0, Map.of(
                "runId", run.getId(),
                "status", "SUCCESS",
                "stream", stream,
                "trustedAnswerRejected", true,
                "sourceCount", context.getSources() == null ? 0 : context.getSources().size()
        ));
        LlmCallResult result = new LlmCallResult();
        result.setContent(content);
        result.setLatencyMs(run.getLatencyMs());
        result.setPromptTokens(0);
        result.setCompletionTokens(0);
        result.setTotalTokens(0);
        return toResponse(run, context, result, "SUCCESS", null);
    }

    /**
     * 以 SSE 形式输出可信回答模式拒答。
     *
     * @param emitter SSE 发射器
     * @param run 运行记录
     * @param context 聊天上下文
     */
    private void completeStreamTrustedAnswerRejected(SseEmitter emitter, RuntimeRunEntity run, ChatRunContext context) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CompletableFuture.runAsync(() -> {
            try {
                // 可信拒答也会写入历史会话，保持登录上下文便于后续扩展审计字段。
                SecurityContextHolder.getContext().setAuthentication(authentication);
                ChatCompletionResponse response = completeTrustedAnswerRejected(run, context, true);
                sendSse(emitter, "meta", buildStreamMeta(run, context, false));
                sendSse(emitter, "delta", Map.of("content", response.getContent()));
                LlmCallResult rejectedResult = new LlmCallResult();
                rejectedResult.setLatencyMs(response.getLatencyMs());
                sendDone(emitter, run, context, rejectedResult, List.of());
                emitter.complete();
            } catch (Exception exception) {
                sendSse(emitter, "error", Map.of("message", safeText(exception.getMessage())));
                emitter.complete();
            } finally {
                // 清理线程上下文，避免线程池复用时串到其他用户。
                SecurityContextHolder.clearContext();
            }
        }, agentRuntimeExecutor);
    }

    /**
     * 构建可信回答状态载荷。
     *
     * @param context 聊天上下文
     * @return 前端展示载荷
     */
    private Map<String, Object> trustedAnswerPayload(ChatRunContext context) {
        return Map.of(
                "enabled", Boolean.TRUE.equals(context.getRagTrustedAnswerMode()),
                "answerable", context.getRagAnswerable() == null || Boolean.TRUE.equals(context.getRagAnswerable()),
                "citationRequired", Boolean.TRUE.equals(context.getRagCitationRequired()),
                "minCitationCount", context.getRagMinCitationCount() == null ? 0 : context.getRagMinCitationCount(),
                "confidenceScore", context.getRagConfidenceScore() == null ? 0D : context.getRagConfidenceScore(),
                "rejectReason", safeText(context.getRagRejectReason()),
                "qualityAdvice", safeText(context.getRagQualityAdvice())
        );
    }

    /**
     * 解析当前 Agent。
     *
     * @param agentId Agent ID
     * @return Agent 实体，可为空
     */
    /**
     * 通过模型网关执行 LLM 调用，并在允许回退时自动切换候选模型。
     *
     * @param context 聊天运行上下文
     * @param invoker 实际模型调用函数
     * @param precheck 调用前预检查，主要用于配额拦截
     * @return LLM 调用结果
     */
    private LlmCallResult invokeWithGatewayFallback(ChatRunContext context,
                                                    Function<ChatRunContext, LlmCallResult> invoker,
                                                    Consumer<ChatRunContext> precheck) {
        try {
            precheck.accept(context);
            return invoker.apply(context);
        } catch (Exception firstException) {
            ModelRouteDecision fallback = modelGatewayService.nextFallbackDecision(context.getRouteDecision(), firstException.getMessage());
            if (fallback == null) {
                throw firstException;
            }
            // 回退时直接更新上下文中的模型、服务商和密钥，后续 Trace 和调用日志会记录新的实际调用对象。
            context.setRouteDecision(fallback);
            context.setModel(fallback.getModel());
            context.setProvider(fallback.getProvider());
            context.setApiKey(fallback.getApiKey());
            precheck.accept(context);
            return invoker.apply(context);
        }
    }

    private AgentEntity resolveAgent(String agentId) {
        if (StringUtils.hasText(agentId)) {
            AgentEntity agent = agentMapper.selectById(agentId);
            if (agent == null || agent.getDeletedAt() != null) {
                throw new BusinessException("AGENT_NOT_FOUND", "Agent 不存在");
            }
            agentAccessService.assertCanView(agent);
            return agent;
        }
        return agentMapper.selectList(new LambdaQueryWrapper<AgentEntity>()
                        .isNull(AgentEntity::getDeletedAt)
                        .orderByDesc(AgentEntity::getStatus)
                        .orderByDesc(AgentEntity::getCreatedAt))
                .stream()
                .filter(agentAccessService::canView)
                .findFirst()
                .orElse(null);
    }

    /**
     * 解析当前模型。
     *
     * @param modelId 请求指定模型 ID
     * @param agent 当前 Agent
     * @return 模型实体
     */
    private ModelConfigEntity resolveModel(String modelId, AgentEntity agent) {
        if (StringUtils.hasText(modelId)) {
            return modelProviderService.requireModel(modelId);
        }
        if (agent != null && StringUtils.hasText(agent.getModelId())) {
            return modelProviderService.requireModel(agent.getModelId());
        }
        return modelConfigMapper.selectList(new LambdaQueryWrapper<ModelConfigEntity>()
                        .eq(ModelConfigEntity::getModelType, "chat")
                        .eq(ModelConfigEntity::getStatus, "enabled")
                        .orderByDesc(ModelConfigEntity::getIsDefault)
                        .orderByDesc(ModelConfigEntity::getCreatedAt))
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException("MODEL_NOT_FOUND", "请先配置可用的 Chat 模型"));
    }

    /**
     * 构建发送给模型的消息列表。
     *
     * @param agent 当前 Agent
     * @param request 聊天请求
     * @return 消息列表
     */
    private List<ChatMessage> buildMessages(AgentEntity agent,
                                            ChatCompletionRequest request,
                                            PromptRuntimeDtos.CompileResult promptCompileResult) {
        List<ChatMessage> messages = new ArrayList<>();
        String systemPrompt = promptCompileResult != null && StringUtils.hasText(promptCompileResult.renderedPrompt)
                ? promptCompileResult.renderedPrompt
                : agent != null && StringUtils.hasText(agent.getSystemPrompt())
                ? agent.getSystemPrompt()
                : "你是 OpenAgentFlow-Java 的 AI 调试助手，请用清晰、准确的中文回答用户问题。";
        messages.add(new ChatMessage("system", systemPrompt));

        // 只透传标准对话角色，避免前端异常数据污染模型请求。
        if (request.getHistory() != null) {
            request.getHistory().stream()
                    .filter(message -> StringUtils.hasText(message.getRole()) && StringUtils.hasText(message.getContent()))
                    .filter(message -> List.of("system", "user", "assistant").contains(message.getRole()))
                    .forEach(messages::add);
        }
        messages.add(new ChatMessage("user", request.getInput()));
        return messages;
    }

    /**
     * 创建运行记录。
     *
     * @param request 聊天请求
     * @param context 聊天上下文
     * @return 运行记录实体
     */
    private RuntimeRunEntity createRun(ChatCompletionRequest request, ChatRunContext context) {
        RuntimeRunEntity run = new RuntimeRunEntity();
        run.setId(newId());
        run.setRunNo("run_" + DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now()));
        run.setRunType("AGENT_CHAT");
        run.setAgentId(context.getAgent() == null ? null : context.getAgent().getId());
        run.setSessionId(context.getSessionId());
        run.setUserId(currentUserId());
        run.setInputText(request.getInput());
        run.setInputPayload(toJson(Map.of(
                "input", request.getInput(),
                "modelId", context.getModel().getId(),
                "sessionId", safeText(context.getSessionId())
        )));
        run.setStatus("RUNNING");
        run.setTotalTokens(0);
        run.setPromptTokens(0);
        run.setCompletionTokens(0);
        run.setTotalCost(BigDecimal.ZERO);
        run.setMetadata(toJson(Map.of("providerId", context.getProvider().getId(), "modelId", context.getModel().getId())));
        run.setStartedAt(LocalDateTime.now());
        runtimeRunMapper.insert(run);
        agentSessionService.appendUserMessage(context.getSessionId(), request.getInput(), run.getId());
        return run;
    }

    /**
     * 创建 LLM Trace 步骤。
     *
     * @param run 运行记录
     * @param context 聊天上下文
     * @return Trace 步骤实体
     */
    private RuntimeTraceStepEntity createLlmStep(RuntimeRunEntity run, ChatRunContext context) {
        RuntimeTraceStepEntity step = new RuntimeTraceStepEntity();
        step.setId(newId());
        step.setRunId(run.getId());
        step.setStepKey("llm_generate");
        step.setStepName("LLM 生成");
        step.setStepType("LLM");
        step.setStatus("RUNNING");
        step.setInputPayload(toJson(Map.of("messages", context.getMessages())));
        step.setPromptText(toJson(context.getMessages()));
        step.setModelId(context.getModel().getId());
        step.setTokenUsage("{}");
        step.setCostAmount(BigDecimal.ZERO);
        step.setStartedAt(LocalDateTime.now());
        runtimeTraceStepMapper.insert(step);
        return step;
    }

    /**
     * 保存成功运行结果。
     *
     * @param run 运行记录
     * @param step Trace 步骤
     * @param context 聊天上下文
     * @param result LLM 调用结果
     * @param stream 是否流式
     */
    @Transactional(rollbackFor = Exception.class)
    public void finishSuccess(RuntimeRunEntity run,
                              RuntimeTraceStepEntity step,
                              ChatRunContext context,
                              LlmCallResult result,
                              boolean stream) {
        String workspaceId = context.getAgent() == null ? null : context.getAgent().getWorkspaceId();
        result.setContent(aiGuardrailService.sanitizeOutput(workspaceId, run.getId(), run.getAgentId(), result.getContent()));
        LocalDateTime finishedAt = LocalDateTime.now();
        BigDecimal cost = usageCostService.calculateCost(context.getModel(), nullToZero(result.getPromptTokens()), nullToZero(result.getCompletionTokens()));
        run.setOutputText(result.getContent());
        run.setOutputPayload(toJson(Map.of(
                "content", safeText(result.getContent()),
                "memories", context.getMemories() == null ? List.of() : context.getMemories(),
                "sources", context.getSources() == null ? List.of() : context.getSources(),
                "trustedAnswer", trustedAnswerPayload(context)
        )));
        run.setStatus("SUCCESS");
        // 一次运行可能包含“工具决策 LLM + 最终回复 LLM”，这里按累计值写入运行总账。
        run.setPromptTokens(nullToZero(run.getPromptTokens()) + nullToZero(result.getPromptTokens()));
        run.setCompletionTokens(nullToZero(run.getCompletionTokens()) + nullToZero(result.getCompletionTokens()));
        run.setTotalTokens(nullToZero(run.getTotalTokens()) + nullToZero(result.getTotalTokens()));
        run.setTotalCost(safeCost(run.getTotalCost()).add(cost));
        run.setLatencyMs(nullToZero(result.getLatencyMs()));
        if (nullToZero(result.getFirstTokenLatencyMs()) > 0) {
            run.setFirstTokenLatencyMs(nullToZero(result.getFirstTokenLatencyMs()));
        }
        run.setFinishedAt(finishedAt);
        runtimeRunMapper.updateById(run);

        step.setStatus("SUCCESS");
        step.setModelId(context.getModel().getId());
        step.setOutputPayload(toJson(Map.of(
                "content", safeText(result.getContent()),
                "memories", context.getMemories() == null ? List.of() : context.getMemories(),
                "sources", context.getSources() == null ? List.of() : context.getSources(),
                "trustedAnswer", trustedAnswerPayload(context)
        )));
        step.setTokenUsage(toJson(Map.of(
                "promptTokens", nullToZero(result.getPromptTokens()),
                "completionTokens", nullToZero(result.getCompletionTokens()),
                "totalTokens", nullToZero(result.getTotalTokens())
        )));
        step.setCostAmount(cost);
        step.setLatencyMs(nullToZero(result.getLatencyMs()));
        step.setFinishedAt(finishedAt);
        runtimeTraceStepMapper.updateById(step);

        saveLlmCall(run, step, context, result, stream, true, null, cost);
        agentSessionService.appendAssistantMessage(context.getSessionId(), safeText(result.getContent()), nullToZero(result.getTotalTokens()), Map.of(
                "runId", run.getId(),
                "status", "SUCCESS",
                "stream", stream,
                "memoryCount", context.getMemories() == null ? 0 : context.getMemories().size(),
                "sourceCount", context.getSources() == null ? 0 : context.getSources().size()
        ));
        memoryService.captureConversationMemory(context.getAgent(), context.getSessionId(), lastUserInput(context), result.getContent(), run.getId());
    }

    /**
     * 保存中间 LLM 步骤，通常用于工具调用前的模型决策。
     *
     * @param run 运行记录
     * @param step Trace 步骤
     * @param context 聊天上下文
     * @param result LLM 调用结果
     * @param stream 是否流式
     */
    private void finishIntermediateLlmStep(RuntimeRunEntity run,
                                           RuntimeTraceStepEntity step,
                                           ChatRunContext context,
                                           LlmCallResult result,
                                           boolean stream) {
        LocalDateTime finishedAt = LocalDateTime.now();
        BigDecimal cost = usageCostService.calculateCost(context.getModel(), nullToZero(result.getPromptTokens()), nullToZero(result.getCompletionTokens()));
        // 中间决策也消耗模型额度，先累加到运行总账，但运行状态仍保持 RUNNING。
        run.setPromptTokens(nullToZero(run.getPromptTokens()) + nullToZero(result.getPromptTokens()));
        run.setCompletionTokens(nullToZero(run.getCompletionTokens()) + nullToZero(result.getCompletionTokens()));
        run.setTotalTokens(nullToZero(run.getTotalTokens()) + nullToZero(result.getTotalTokens()));
        run.setTotalCost(safeCost(run.getTotalCost()).add(cost));
        runtimeRunMapper.updateById(run);
        step.setStatus("SUCCESS");
        step.setModelId(context.getModel().getId());
        step.setOutputPayload(toJson(Map.of(
                "content", safeText(result.getContent()),
                "toolCalls", result.getToolCalls() == null ? List.of() : result.getToolCalls()
        )));
        step.setTokenUsage(toJson(Map.of(
                "promptTokens", nullToZero(result.getPromptTokens()),
                "completionTokens", nullToZero(result.getCompletionTokens()),
                "totalTokens", nullToZero(result.getTotalTokens())
        )));
        step.setCostAmount(cost);
        step.setLatencyMs(nullToZero(result.getLatencyMs()));
        step.setFinishedAt(finishedAt);
        runtimeTraceStepMapper.updateById(step);
        saveLlmCall(run, step, context, result, stream, true, null, cost);
    }

    /**
     * 保存失败运行结果。
     *
     * @param run 运行记录
     * @param step Trace 步骤
     * @param context 聊天上下文
     * @param exception 异常对象
     * @param stream 是否流式
     */
    @Transactional(rollbackFor = Exception.class)
    public void finishFailure(RuntimeRunEntity run,
                              RuntimeTraceStepEntity step,
                              ChatRunContext context,
                              Exception exception,
                              boolean stream) {
        if (run == null || step == null || context == null) {
            return;
        }
        LocalDateTime finishedAt = LocalDateTime.now();
        run.setStatus("FAILED");
        run.setErrorMessage(exception.getMessage());
        run.setFinishedAt(finishedAt);
        runtimeRunMapper.updateById(run);

        step.setStatus("FAILED");
        step.setModelId(context.getModel().getId());
        step.setErrorMessage(exception.getMessage());
        step.setFinishedAt(finishedAt);
        runtimeTraceStepMapper.updateById(step);

        LlmCallResult result = new LlmCallResult();
        result.setContent("");
        saveLlmCall(run, step, context, result, stream, false, exception.getMessage(), BigDecimal.ZERO);
        agentSessionService.appendAssistantMessage(context.getSessionId(), "本次调用失败：" + safeText(exception.getMessage()), 0, Map.of(
                "runId", run.getId(),
                "status", "FAILED",
                "stream", stream
        ));
    }

    /**
     * 保存 LLM 调用日志。
     *
     * @param run 运行记录
     * @param step Trace 步骤
     * @param context 聊天上下文
     * @param result LLM 调用结果
     * @param stream 是否流式
     * @param success 是否成功
     * @param errorMessage 错误信息
     */
    private void saveLlmCall(RuntimeRunEntity run,
                             RuntimeTraceStepEntity step,
                             ChatRunContext context,
                             LlmCallResult result,
                             boolean stream,
                             boolean success,
                             String errorMessage,
                             BigDecimal cost) {
        RuntimeLlmCallEntity call = new RuntimeLlmCallEntity();
        call.setId(newId());
        call.setRunId(run.getId());
        call.setStepId(step.getId());
        call.setProviderId(context.getProvider().getId());
        call.setModelId(context.getModel().getId());
        if (context.getRouteDecision() != null) {
            call.setRoutePolicyId(context.getRouteDecision().getRoutePolicyId());
            call.setGatewaySceneType(context.getRouteDecision().getSceneType());
            call.setRouteDecision(modelGatewayService.toDecisionJson(context.getRouteDecision()));
            call.setFallbackUsed(Boolean.TRUE.equals(context.getRouteDecision().getFallbackUsed()));
        }
        call.setRequestMessages(toJson(context.getMessages()));
        call.setResponseMessage(toJson(Map.of("content", safeText(result.getContent()))));
        call.setStream(stream);
        call.setPromptTokens(nullToZero(result.getPromptTokens()));
        call.setCompletionTokens(nullToZero(result.getCompletionTokens()));
        call.setTotalTokens(nullToZero(result.getTotalTokens()));
        call.setCostAmount(cost == null ? BigDecimal.ZERO : cost);
        call.setLatencyMs(nullToZero(result.getLatencyMs()));
        call.setFirstTokenLatencyMs(nullToZero(result.getFirstTokenLatencyMs()));
        call.setSuccess(success);
        call.setErrorMessage(errorMessage);
        // Trace保存实际Prompt版本、最终内容哈希、变量来源和运行时装配层。
        promptRuntimeService.enrichLlmCall(call, context.getPromptCompileResult(), context.getMessages());
        runtimeLlmCallMapper.insert(call);
        promptRuntimeService.recordMetric(
                context.getAgent() == null ? null : context.getAgent().getWorkspaceId(),
                run.getId(), run.getAgentId(), context.getPromptCompileResult(), success,
                call.getLatencyMs(), call.getTotalTokens(), call.getCostAmount());
        usageCostService.recordActualUsage(run, context.getProvider(), context.getModel(), call.getTotalTokens(), call.getCostAmount(), success, call.getLatencyMs());
    }

    /**
     * 转换聊天响应。
     *
     * @param run 运行记录
     * @param context 聊天上下文
     * @param result LLM 调用结果
     * @param status 运行状态
     * @param errorMessage 错误信息
     * @return 聊天响应
     */
    private ChatCompletionResponse toResponse(RuntimeRunEntity run,
                                              ChatRunContext context,
                                              LlmCallResult result,
                                              String status,
                                              String errorMessage) {
        ChatCompletionResponse response = new ChatCompletionResponse();
        response.setRunId(run.getId());
        response.setSessionId(context.getSessionId());
        response.setContent(result.getContent());
        response.setProviderName(context.getProvider().getProviderName());
        response.setModelName(context.getModel().getModelName());
        response.setStatus(status);
        response.setPromptTokens(nullToZero(result.getPromptTokens()));
        response.setCompletionTokens(nullToZero(result.getCompletionTokens()));
        response.setTotalTokens(nullToZero(result.getTotalTokens()));
        response.setLatencyMs(nullToZero(result.getLatencyMs()));
        response.setErrorMessage(errorMessage);
        response.setMemories(context.getMemories() == null ? List.of() : context.getMemories());
        response.setSources(context.getSources() == null ? List.of() : context.getSources());
        response.setTrustedAnswerMode(Boolean.TRUE.equals(context.getRagTrustedAnswerMode()));
        response.setAnswerable(context.getRagAnswerable() == null || Boolean.TRUE.equals(context.getRagAnswerable()));
        response.setRejectReason(context.getRagRejectReason());
        response.setConfidenceScore(context.getRagConfidenceScore());
        response.setTrustedAnswer(trustedAnswerPayload(context));
        response.setIntentRoute(context.getIntentRoutePlan());
        response.setEnhancedQueries(context.getRagEnhancedQueries());
        response.setRerankMode(context.getRagRerankMode());
        response.setRerankModelId(context.getRagRerankModelId());
        response.setRerankLatencyMs(context.getRagRerankLatencyMs());
        response.setRerankErrorMessage(context.getRagRerankErrorMessage());
        return response;
    }

    /**
     * 发送 SSE 事件。
     *
     * @param emitter SSE 发射器
     * @param name 事件名称
     * @param data 事件数据
     */
    private void sendSse(SseEmitter emitter, String name, Object data) {
        runtimeEventStreamService.publish(emitter, name, data);
    }

    /**
     * 在流式分片和多阶段工具调用边界检查分布式停止令牌。
     *
     * @param runId 运行ID
     */
    private void ensureRuntimeActive(String runId) {
        if (runtimeControlService.isCancellationRequested(runId)) {
            runtimeControlService.acknowledgeCancellation(runId, Thread.currentThread().getName());
            throw new java.util.concurrent.CancellationException("运行已由用户停止");
        }
    }

    /**
     * 发送 SSE 完成事件。
     *
     * @param emitter SSE 发射器
     * @param run 运行记录
     * @param context 聊天上下文
     * @param result LLM 结果
     * @param toolResults 工具结果
     */
    private void sendDone(SseEmitter emitter,
                          RuntimeRunEntity run,
                          ChatRunContext context,
                          LlmCallResult result,
                          List<Map<String, Object>> toolResults) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("runId", run.getId());
        payload.put("sessionId", safeText(context.getSessionId()));
        payload.put("status", "SUCCESS");
        payload.put("latencyMs", nullToZero(result.getLatencyMs()));
        payload.put("promptTokens", nullToZero(result.getPromptTokens()));
        payload.put("completionTokens", nullToZero(result.getCompletionTokens()));
        payload.put("totalTokens", nullToZero(result.getTotalTokens()));
        payload.put("memories", context.getMemories() == null ? List.of() : context.getMemories());
        payload.put("sources", context.getSources() == null ? List.of() : context.getSources());
        payload.put("trustedAnswer", trustedAnswerPayload(context));
        payload.put("toolResults", toolResults == null ? List.of() : toolResults);
        payload.put("intentRoute", context.getIntentRoutePlan());
        payload.put("enhancedQueries", context.getRagEnhancedQueries() == null ? List.of() : context.getRagEnhancedQueries());
        payload.put("rerankMode", safeText(context.getRagRerankMode()));
        payload.put("rerankModelId", safeText(context.getRagRerankModelId()));
        payload.put("rerankLatencyMs", context.getRagRerankLatencyMs() == null ? 0 : context.getRagRerankLatencyMs());
        payload.put("rerankErrorMessage", safeText(context.getRagRerankErrorMessage()));
        sendSse(emitter, "done", payload);
    }

    /**
     * 构建统一 SSE 元数据，保证普通聊天、工具聊天和可信拒答字段一致。
     *
     * @param run 运行记录
     * @param context 聊天运行上下文
     * @param includeTools 是否附带候选工具定义
     * @return SSE 元数据
     */
    private Map<String, Object> buildStreamMeta(RuntimeRunEntity run,
                                                ChatRunContext context,
                                                boolean includeTools) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("runId", run.getId());
        payload.put("sessionId", safeText(context.getSessionId()));
        payload.put("providerName", context.getProvider().getProviderName());
        payload.put("modelName", context.getModel().getModelName());
        payload.put("memories", context.getMemories() == null ? List.of() : context.getMemories());
        payload.put("sources", context.getSources() == null ? List.of() : context.getSources());
        payload.put("trustedAnswer", trustedAnswerPayload(context));
        payload.put("intentRoute", context.getIntentRoutePlan());
        payload.put("enhancedQueries", context.getRagEnhancedQueries() == null ? List.of() : context.getRagEnhancedQueries());
        payload.put("rerankMode", safeText(context.getRagRerankMode()));
        payload.put("rerankModelId", safeText(context.getRagRerankModelId()));
        payload.put("rerankLatencyMs", context.getRagRerankLatencyMs() == null ? 0 : context.getRagRerankLatencyMs());
        payload.put("rerankErrorMessage", safeText(context.getRagRerankErrorMessage()));
        if (includeTools) {
            payload.put("tools", context.getTools() == null ? List.of() : context.getTools());
        }
        return payload;
    }

    /**
     * 计算最大输出 Token。
     *
     * @param request 聊天请求
     * @param context 聊天上下文
     * @return 最大输出 Token
     */
    private Integer effectiveMaxTokens(ChatCompletionRequest request, ChatRunContext context) {
        if (request.getMaxTokens() != null && request.getMaxTokens() > 0) {
            return request.getMaxTokens();
        }
        return context.getModel().getMaxOutputTokens();
    }

    /**
     * 获取本轮最后一条用户输入。
     *
     * @param context 聊天上下文
     * @return 用户输入
     */
    private String lastUserInput(ChatRunContext context) {
        if (context == null || context.getMessages() == null) {
            return "";
        }
        for (int index = context.getMessages().size() - 1; index >= 0; index--) {
            ChatMessage message = context.getMessages().get(index);
            if ("user".equals(message.getRole())) {
                return safeText(message.getContent());
            }
        }
        return "";
    }

    /**
     * 提取当前问题之前的最近用户和助手消息，供 RAG 查询指代消解使用。
     *
     * @param context 聊天运行上下文
     * @return 有界会话上下文
     */
    private String recentConversationContext(ChatRunContext context) {
        if (context == null || context.getMessages() == null || context.getMessages().size() < 2) {
            return "";
        }
        List<String> history = new ArrayList<>();
        int currentUserIndex = context.getMessages().size() - 1;
        for (int index = currentUserIndex - 1; index >= 0 && history.size() < 4; index--) {
            ChatMessage message = context.getMessages().get(index);
            if (message == null || (!"user".equals(message.getRole()) && !"assistant".equals(message.getRole()))) {
                continue;
            }
            if (StringUtils.hasText(message.getContent())) {
                history.add(message.getRole() + "：" + message.getContent());
            }
        }
        java.util.Collections.reverse(history);
        return String.join("\n", history);
    }

    /**
     * 获取当前登录用户 ID。
     *
     * @return 用户 ID
     */
    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUserDetails userDetails) {
            return userDetails.getUser().getId();
        }
        return null;
    }

    /**
     * 生成 UUID 主键。
     *
     * @return UUID 字符串
     */
    private String newId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 安全转换整数空值。
     *
     * @param value 整数值
     * @return 非空整数
     */
    private Integer nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 安全转换成本空值。
     *
     * @param value 成本金额
     * @return 非空成本
     */
    private BigDecimal safeCost(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 安全转换文本空值。
     *
     * @param text 文本值
     * @return 非空文本
     */
    private String safeText(String text) {
        return text == null ? "" : text;
    }

    /**
     * 转换 JSON 字符串。
     *
     * @param value 任意对象
     * @return JSON 字符串
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }
}
