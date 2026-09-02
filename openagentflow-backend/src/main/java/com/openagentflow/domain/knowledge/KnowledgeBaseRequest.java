package com.openagentflow.domain.knowledge;

import jakarta.validation.constraints.NotBlank;

/**
 * 知识库保存请求。
 */
public class KnowledgeBaseRequest {

    /** 知识库编码，留空时后端会根据名称生成。 */
    private String kbCode;

    /** 知识库名称。 */
    @NotBlank(message = "知识库名称不能为空")
    private String kbName;

    /** 知识库描述。 */
    private String description;

    /** 所属工作空间 ID。 */
    private String workspaceId;

    /** Embedding 模型 ID。 */
    private String embeddingModelId;

    /** Cross-Encoder 重排模型 ID，可为空以使用规则重排。 */
    private String rerankModelId;

    /** 切片策略。 */
    private String chunkStrategy;

    /** 切片大小。 */
    private Integer chunkSize;

    /** 切片重叠长度。 */
    private Integer chunkOverlap;

    /** 可见范围。 */
    private String visibility;

    /** 知识库状态。 */
    private String status;

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

    public String getEmbeddingModelId() {
        return embeddingModelId;
    }

    public void setEmbeddingModelId(String embeddingModelId) {
        this.embeddingModelId = embeddingModelId;
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

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
