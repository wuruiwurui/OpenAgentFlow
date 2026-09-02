package com.openagentflow.domain.knowledge;

import java.time.LocalDateTime;

/**
 * 知识库列表摘要对象。
 */
public class KnowledgeBaseSummary {

    /** 知识库主键 ID。 */
    private String id;

    /** 知识库编码。 */
    private String kbCode;

    /** 知识库名称。 */
    private String kbName;

    /** 知识库描述。 */
    private String description;

    /** 所属工作空间 ID。 */
    private String workspaceId;

    /** 所属工作空间名称。 */
    private String workspaceName;

    /** 绑定的向量模型 ID。 */
    private String embeddingModelId;

    /** 绑定的向量模型名称。 */
    private String embeddingModelName;

    /** 绑定的 Cross-Encoder 重排模型 ID。 */
    private String rerankModelId;

    /** 切片策略。 */
    private String chunkStrategy;

    /** 切片大小。 */
    private Integer chunkSize;

    /** 切片重叠长度。 */
    private Integer chunkOverlap;

    /** Milvus 集合名称。 */
    private String milvusCollectionName;

    /** 知识库状态。 */
    private String status;

    /** 文档数量。 */
    private Integer documentCount;

    /** 分片数量。 */
    private Integer chunkCount;

    /** 已向量化分片数量。 */
    private Integer embeddingCount;

    /** 创建时间。 */
    private LocalDateTime createdAt;

    /** 更新时间。 */
    private LocalDateTime updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKbCode() {
        return kbCode;
    }

    public void setKbCode(String kbCode) {
        this.kbCode = kbCode;
    }

    public String getKbName() {
        return kbName;
    }

    public void setKbName(String kbName) {
        this.kbName = kbName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    public String getEmbeddingModelId() {
        return embeddingModelId;
    }

    public void setEmbeddingModelId(String embeddingModelId) {
        this.embeddingModelId = embeddingModelId;
    }

    public String getEmbeddingModelName() {
        return embeddingModelName;
    }

    public void setEmbeddingModelName(String embeddingModelName) {
        this.embeddingModelName = embeddingModelName;
    }

    public String getRerankModelId() {
        return rerankModelId;
    }

    public void setRerankModelId(String rerankModelId) {
        this.rerankModelId = rerankModelId;
    }

    public String getChunkStrategy() {
        return chunkStrategy;
    }

    public void setChunkStrategy(String chunkStrategy) {
        this.chunkStrategy = chunkStrategy;
    }

    public Integer getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Integer chunkSize) {
        this.chunkSize = chunkSize;
    }

    public Integer getChunkOverlap() {
        return chunkOverlap;
    }

    public void setChunkOverlap(Integer chunkOverlap) {
        this.chunkOverlap = chunkOverlap;
    }

    public String getMilvusCollectionName() {
        return milvusCollectionName;
    }

    public void setMilvusCollectionName(String milvusCollectionName) {
        this.milvusCollectionName = milvusCollectionName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getDocumentCount() {
        return documentCount;
    }

    public void setDocumentCount(Integer documentCount) {
        this.documentCount = documentCount;
    }

    public Integer getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(Integer chunkCount) {
        this.chunkCount = chunkCount;
    }

    public Integer getEmbeddingCount() {
        return embeddingCount;
    }

    public void setEmbeddingCount(Integer embeddingCount) {
        this.embeddingCount = embeddingCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
