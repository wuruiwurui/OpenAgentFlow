package com.openagentflow.domain.knowledge;

import java.util.List;

/**
 * Agent RAG 检索聚合结果。
 */
public class RagRetrievalOutcome {

    /** 本轮最终可注入模型的引用来源。 */
    private List<KnowledgeSource> sources;

    /** 是否启用可信回答模式。 */
    private Boolean trustedAnswerMode;

    /** 是否要求回答中必须带引用依据。 */
    private Boolean citationRequired;

    /** 最少需要命中的引用数量。 */
    private Integer minCitationCount;

    /** 本轮检索是否具备可信回答条件。 */
    private Boolean answerable;

    /** 拒答或低置信原因。 */
    private String rejectReason;

    /** 最佳置信得分。 */
    private Double confidenceScore;

    /** 本轮使用的相似度阈值。 */
    private Double scoreThreshold;

    /** 本轮使用的低置信阈值。 */
    private Double lowConfidenceThreshold;

    /** 检索质量建议。 */
    private String qualityAdvice;

    /** 本次检索接收的原始查询。 */
    private String originalQuery;

    /** 本次实际使用的增强查询列表。 */
    private List<String> enhancedQueries;

    /** 查询理解后的标准查询。 */
    private String canonicalQuery;

    /** 实际重排模式。 */
    private String rerankMode;

    /** 实际重排模型配置 ID。 */
    private String rerankModelId;

    /** 真实重排调用耗时毫秒。 */
    private Integer rerankLatencyMs;

    /** 查询是否使用了会话上下文完成指代消解。 */
    private Boolean contextResolved;

    /** 重排失败后降级的原因。 */
    private String rerankErrorMessage;

    public List<KnowledgeSource> getSources() {
        return sources;
    }

    public void setSources(List<KnowledgeSource> sources) {
        this.sources = sources;
    }

    public Boolean getTrustedAnswerMode() {
        return trustedAnswerMode;
    }

    public void setTrustedAnswerMode(Boolean trustedAnswerMode) {
        this.trustedAnswerMode = trustedAnswerMode;
    }

    public Boolean getCitationRequired() {
        return citationRequired;
    }

    public void setCitationRequired(Boolean citationRequired) {
        this.citationRequired = citationRequired;
    }

    public Integer getMinCitationCount() {
        return minCitationCount;
    }

    public void setMinCitationCount(Integer minCitationCount) {
        this.minCitationCount = minCitationCount;
    }

    public Boolean getAnswerable() {
        return answerable;
    }

    public void setAnswerable(Boolean answerable) {
        this.answerable = answerable;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public Double getScoreThreshold() {
        return scoreThreshold;
    }

    public void setScoreThreshold(Double scoreThreshold) {
        this.scoreThreshold = scoreThreshold;
    }

    public Double getLowConfidenceThreshold() {
        return lowConfidenceThreshold;
    }

    public void setLowConfidenceThreshold(Double lowConfidenceThreshold) {
        this.lowConfidenceThreshold = lowConfidenceThreshold;
    }

    public String getQualityAdvice() {
        return qualityAdvice;
    }

    public void setQualityAdvice(String qualityAdvice) {
        this.qualityAdvice = qualityAdvice;
    }

    public String getOriginalQuery() {
        return originalQuery;
    }

    public void setOriginalQuery(String originalQuery) {
        this.originalQuery = originalQuery;
    }

    public List<String> getEnhancedQueries() {
        return enhancedQueries;
    }

    public void setEnhancedQueries(List<String> enhancedQueries) {
        this.enhancedQueries = enhancedQueries;
    }

    public String getCanonicalQuery() {
        return canonicalQuery;
    }

    public void setCanonicalQuery(String canonicalQuery) {
        this.canonicalQuery = canonicalQuery;
    }

    public String getRerankMode() {
        return rerankMode;
    }

    public void setRerankMode(String rerankMode) {
        this.rerankMode = rerankMode;
    }

    public String getRerankModelId() {
        return rerankModelId;
    }

    public void setRerankModelId(String rerankModelId) {
        this.rerankModelId = rerankModelId;
    }

    public Integer getRerankLatencyMs() {
        return rerankLatencyMs;
    }

    public void setRerankLatencyMs(Integer rerankLatencyMs) {
        this.rerankLatencyMs = rerankLatencyMs;
    }

    public Boolean getContextResolved() {
        return contextResolved;
    }

    public void setContextResolved(Boolean contextResolved) {
        this.contextResolved = contextResolved;
    }

    public String getRerankErrorMessage() {
        return rerankErrorMessage;
    }

    public void setRerankErrorMessage(String rerankErrorMessage) {
        this.rerankErrorMessage = rerankErrorMessage;
    }
}
