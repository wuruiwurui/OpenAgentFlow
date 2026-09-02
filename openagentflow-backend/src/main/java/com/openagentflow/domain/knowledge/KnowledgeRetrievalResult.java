package com.openagentflow.domain.knowledge;

import java.util.List;

/**
 * 知识库检索结果。
 */
public class KnowledgeRetrievalResult {

    /** 检索日志 ID。 */
    private String retrievalLogId;

    /** 检索结果来源。 */
    private List<KnowledgeSource> sources;

    /** 本次检索耗时毫秒。 */
    private Integer latencyMs;

    /** 检索模式。 */
    private String searchMode;

    /** 是否启用重排。 */
    private Boolean rerankEnabled;

    /** 候选召回数量。 */
    private Integer candidateCount;

    /** 最终结果数量。 */
    private Integer resultCount;

    /** 最佳置信得分。 */
    private Double confidenceScore;

    /** 是否低置信。 */
    private Boolean lowConfidence;

    /** 是否建议回答。 */
    private Boolean answerable;

    /** 拒答或低置信原因。 */
    private String rejectReason;

    /** 本次使用的相似度阈值。 */
    private Double scoreThreshold;

    /** 本次使用的低置信阈值。 */
    private Double lowConfidenceThreshold;

    /** 生产级检索建议，例如阈值、切片或过滤条件调整。 */
    private String qualityAdvice;

    /** 规范化后的用户原始查询。 */
    private String originalQuery;

    /** 查询理解后的标准查询。 */
    private String canonicalQuery;

    /** 实际参与召回的增强查询列表。 */
    private List<String> enhancedQueries;

    /** 是否使用会话上下文消解了指代。 */
    private Boolean contextResolved;

    /** 实际重排模式：disabled、rule、cross_encoder 或 rule_fallback。 */
    private String rerankMode;

    /** 本次使用的重排模型配置 ID。 */
    private String rerankModelId;

    /** 真实重排调用耗时毫秒。 */
    private Integer rerankLatencyMs;

    /** 真实重排降级原因。 */
    private String rerankErrorMessage;

    public String getRetrievalLogId() {
        return retrievalLogId;
    }

    public void setRetrievalLogId(String retrievalLogId) {
        this.retrievalLogId = retrievalLogId;
    }

    public List<KnowledgeSource> getSources() {
        return sources;
    }

    public void setSources(List<KnowledgeSource> sources) {
        this.sources = sources;
    }

    public Integer getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Integer latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getSearchMode() {
        return searchMode;
    }

    public void setSearchMode(String searchMode) {
        this.searchMode = searchMode;
    }

    public Boolean getRerankEnabled() {
        return rerankEnabled;
    }

    public void setRerankEnabled(Boolean rerankEnabled) {
        this.rerankEnabled = rerankEnabled;
    }

    public Integer getCandidateCount() {
        return candidateCount;
    }

    public void setCandidateCount(Integer candidateCount) {
        this.candidateCount = candidateCount;
    }

    public Integer getResultCount() {
        return resultCount;
    }

    public void setResultCount(Integer resultCount) {
        this.resultCount = resultCount;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public Boolean getLowConfidence() {
        return lowConfidence;
    }

    public void setLowConfidence(Boolean lowConfidence) {
        this.lowConfidence = lowConfidence;
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

    public String getCanonicalQuery() {
        return canonicalQuery;
    }

    public void setCanonicalQuery(String canonicalQuery) {
        this.canonicalQuery = canonicalQuery;
    }

    public List<String> getEnhancedQueries() {
        return enhancedQueries;
    }

    public void setEnhancedQueries(List<String> enhancedQueries) {
        this.enhancedQueries = enhancedQueries;
    }

    public Boolean getContextResolved() {
        return contextResolved;
    }

    public void setContextResolved(Boolean contextResolved) {
        this.contextResolved = contextResolved;
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

    public String getRerankErrorMessage() {
        return rerankErrorMessage;
    }

    public void setRerankErrorMessage(String rerankErrorMessage) {
        this.rerankErrorMessage = rerankErrorMessage;
    }
}
