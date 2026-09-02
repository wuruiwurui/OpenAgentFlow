package com.openagentflow.domain.knowledge;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 知识库检索请求。
 */
public class KnowledgeRetrievalRequest {

    /** 检索问题或关键词。 */
    @NotBlank(message = "检索内容不能为空")
    private String query;

    /** 返回条数。 */
    private Integer topK;

    /** 相似度阈值。 */
    private Double scoreThreshold;

    /** 检索模式：vector、keyword、hybrid。 */
    private String searchMode;

    /** 候选召回数量，最终会在候选内重排后截取 topK。 */
    private Integer candidateK;

    /** 是否启用本地规则重排。 */
    private Boolean rerankEnabled;

    /** 向量得分权重，混合检索时生效。 */
    private Double vectorWeight;

    /** 关键词得分权重，混合检索时生效。 */
    private Double keywordWeight;

    /** 低置信阈值，最佳结果低于该值时提示拒答。 */
    private Double lowConfidenceThreshold;

    /** 低置信时是否建议拒答。 */
    private Boolean rejectLowConfidence;

    /** 指定文档 ID 列表，为空时检索整个知识库。 */
    private List<String> documentIds;

    /** 指定页码，为空时不按页过滤。 */
    private Integer pageNo;

    /** 元数据关键词，会匹配分片标题、正文和 metadata JSON。 */
    private String metadataKeyword;

    /** 是否启用查询规范化、口语清理和同义词改写。 */
    private Boolean queryRewriteEnabled;

    /** 是否使用多个查询变体执行召回并融合。 */
    private Boolean multiQueryEnabled;

    /** 单次检索允许生成的最大查询变体数量。 */
    private Integer maxQueryVariants;

    /** 最近会话上下文，用于消解“它、这个、上述”等指代表达。 */
    private String conversationContext;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }

    public Double getScoreThreshold() {
        return scoreThreshold;
    }

    public void setScoreThreshold(Double scoreThreshold) {
        this.scoreThreshold = scoreThreshold;
    }

    public String getSearchMode() {
        return searchMode;
    }

    public void setSearchMode(String searchMode) {
        this.searchMode = searchMode;
    }

    public Integer getCandidateK() {
        return candidateK;
    }

    public void setCandidateK(Integer candidateK) {
        this.candidateK = candidateK;
    }

    public Boolean getRerankEnabled() {
        return rerankEnabled;
    }

    public void setRerankEnabled(Boolean rerankEnabled) {
        this.rerankEnabled = rerankEnabled;
    }

    public Double getVectorWeight() {
        return vectorWeight;
    }

    public void setVectorWeight(Double vectorWeight) {
        this.vectorWeight = vectorWeight;
    }

    public Double getKeywordWeight() {
        return keywordWeight;
    }

    public void setKeywordWeight(Double keywordWeight) {
        this.keywordWeight = keywordWeight;
    }

    public Double getLowConfidenceThreshold() {
        return lowConfidenceThreshold;
    }

    public void setLowConfidenceThreshold(Double lowConfidenceThreshold) {
        this.lowConfidenceThreshold = lowConfidenceThreshold;
    }

    public Boolean getRejectLowConfidence() {
        return rejectLowConfidence;
    }

    public void setRejectLowConfidence(Boolean rejectLowConfidence) {
        this.rejectLowConfidence = rejectLowConfidence;
    }

    public List<String> getDocumentIds() {
        return documentIds;
    }

    public void setDocumentIds(List<String> documentIds) {
        this.documentIds = documentIds;
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public String getMetadataKeyword() {
        return metadataKeyword;
    }

    public void setMetadataKeyword(String metadataKeyword) {
        this.metadataKeyword = metadataKeyword;
    }

    public Boolean getQueryRewriteEnabled() {
        return queryRewriteEnabled;
    }

    public void setQueryRewriteEnabled(Boolean queryRewriteEnabled) {
        this.queryRewriteEnabled = queryRewriteEnabled;
    }

    public Boolean getMultiQueryEnabled() {
        return multiQueryEnabled;
    }

    public void setMultiQueryEnabled(Boolean multiQueryEnabled) {
        this.multiQueryEnabled = multiQueryEnabled;
    }

    public Integer getMaxQueryVariants() {
        return maxQueryVariants;
    }

    public void setMaxQueryVariants(Integer maxQueryVariants) {
        this.maxQueryVariants = maxQueryVariants;
    }

    public String getConversationContext() {
        return conversationContext;
    }

    public void setConversationContext(String conversationContext) {
        this.conversationContext = conversationContext;
    }
}
