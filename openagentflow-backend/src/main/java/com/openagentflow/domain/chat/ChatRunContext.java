package com.openagentflow.domain.chat;

import com.openagentflow.entity.AgentEntity;
import com.openagentflow.entity.ModelConfigEntity;
import com.openagentflow.entity.ModelProviderEntity;
import com.openagentflow.domain.knowledge.KnowledgeSource;
import com.openagentflow.domain.memory.MemoryDtos;
import com.openagentflow.domain.model.ModelRouteDecision;
import com.openagentflow.domain.prompt.PromptRuntimeDtos;

import java.util.List;

/**
 * 聊天运行上下文。
 */
public class ChatRunContext {

    /** 当前Runtime运行ID，用于跨线程追踪和主动取消模型HTTP请求。 */
    private String runId;

    /** 当前 Agent。 */
    private AgentEntity agent;

    /** 当前模型。 */
    private ModelConfigEntity model;

    /** 当前模型服务商。 */
    private ModelProviderEntity provider;

    /** 调用模型使用的 API Key。 */
    private String apiKey;

    /** 当前历史会话 ID。 */
    private String sessionId;

    /** 本次发送给模型的消息列表。 */
    private List<ChatMessage> messages;

    /** 本次 RAG 检索命中的引用来源。 */
    private List<KnowledgeSource> sources;

    /** 是否启用 RAG 可信回答模式。 */
    private Boolean ragTrustedAnswerMode;

    /** 本次 RAG 检索是否满足可信回答条件。 */
    private Boolean ragAnswerable;

    /** RAG 可信回答拒答原因。 */
    private String ragRejectReason;

    /** RAG 最佳置信得分。 */
    private Double ragConfidenceScore;

    /** RAG 最低引用数量。 */
    private Integer ragMinCitationCount;

    /** RAG 是否要求答案带引用。 */
    private Boolean ragCitationRequired;

    /** RAG 质量建议。 */
    private String ragQualityAdvice;

    /** 本次 Memory 召回命中的记忆列表。 */
    private List<MemoryDtos.RecallItem> memories;

    /** 当前 Agent 可用的工具定义。 */
    private List<ToolDefinitionForModel> tools;

    /** 本轮输入的结构化意图路由计划。 */
    private IntentRoutePlan intentRoutePlan;

    /** 模型网关路由决策。 */
    private ModelRouteDecision routeDecision;

    /** 本次运行实际Prompt编译结果。 */
    private PromptRuntimeDtos.CompileResult promptCompileResult;

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public AgentEntity getAgent() {
        return agent;
    }

    public void setAgent(AgentEntity agent) {
        this.agent = agent;
    }

    public ModelConfigEntity getModel() {
        return model;
    }

    public void setModel(ModelConfigEntity model) {
        this.model = model;
    }

    public ModelProviderEntity getProvider() {
        return provider;
    }

    public void setProvider(ModelProviderEntity provider) {
        this.provider = provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }

    public List<KnowledgeSource> getSources() {
        return sources;
    }

    public void setSources(List<KnowledgeSource> sources) {
        this.sources = sources;
    }

    public Boolean getRagTrustedAnswerMode() {
        return ragTrustedAnswerMode;
    }

    public void setRagTrustedAnswerMode(Boolean ragTrustedAnswerMode) {
        this.ragTrustedAnswerMode = ragTrustedAnswerMode;
    }

    public Boolean getRagAnswerable() {
        return ragAnswerable;
    }

    public void setRagAnswerable(Boolean ragAnswerable) {
        this.ragAnswerable = ragAnswerable;
    }

    public String getRagRejectReason() {
        return ragRejectReason;
    }

    public void setRagRejectReason(String ragRejectReason) {
        this.ragRejectReason = ragRejectReason;
    }

    public Double getRagConfidenceScore() {
        return ragConfidenceScore;
    }

    public void setRagConfidenceScore(Double ragConfidenceScore) {
        this.ragConfidenceScore = ragConfidenceScore;
    }

    public Integer getRagMinCitationCount() {
        return ragMinCitationCount;
    }

    public void setRagMinCitationCount(Integer ragMinCitationCount) {
        this.ragMinCitationCount = ragMinCitationCount;
    }

    public Boolean getRagCitationRequired() {
        return ragCitationRequired;
    }

    public void setRagCitationRequired(Boolean ragCitationRequired) {
        this.ragCitationRequired = ragCitationRequired;
    }

    public String getRagQualityAdvice() {
        return ragQualityAdvice;
    }

    public void setRagQualityAdvice(String ragQualityAdvice) {
        this.ragQualityAdvice = ragQualityAdvice;
    }

    public List<MemoryDtos.RecallItem> getMemories() {
        return memories;
    }

    public void setMemories(List<MemoryDtos.RecallItem> memories) {
        this.memories = memories;
    }

    public List<ToolDefinitionForModel> getTools() {
        return tools;
    }

    public void setTools(List<ToolDefinitionForModel> tools) {
        this.tools = tools;
    }

    public IntentRoutePlan getIntentRoutePlan() {
        return intentRoutePlan;
    }

    public void setIntentRoutePlan(IntentRoutePlan intentRoutePlan) {
        this.intentRoutePlan = intentRoutePlan;
    }

    public ModelRouteDecision getRouteDecision() {
        return routeDecision;
    }

    public void setRouteDecision(ModelRouteDecision routeDecision) {
        this.routeDecision = routeDecision;
    }

    public PromptRuntimeDtos.CompileResult getPromptCompileResult() {
        return promptCompileResult;
    }

    public void setPromptCompileResult(PromptRuntimeDtos.CompileResult promptCompileResult) {
        this.promptCompileResult = promptCompileResult;
    }
}
