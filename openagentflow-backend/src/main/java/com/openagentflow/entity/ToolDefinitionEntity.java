package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 工具定义表。
 * <p>对应数据库表：tool_definition。</p>
 */
@TableName("tool_definition")
public class ToolDefinitionEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 工具编码。 */
    @TableField("tool_code")
    private String toolCode;

    /** 工具名称。 */
    @TableField("tool_name")
    private String toolName;

    /** 工具类型。 */
    @TableField("tool_type")
    private String toolType;

    /** 所属工作空间ID。 */
    @TableField("workspace_id")
    private String workspaceId;

    /** 描述。 */
    @TableField("description")
    private String description;

    /** 请求方法。 */
    @TableField("request_method")
    private String requestMethod;

    /** 端点URL。 */
    @TableField("endpoint_url")
    private String endpointUrl;

    /** 认证类型。 */
    @TableField("auth_type")
    private String authType;

    /** 认证配置。 */
    @TableField("auth_config")
    private String authConfig;

    /** 请求头。 */
    @TableField("headers")
    private String headers;

    /** 请求Schema。 */
    @TableField("request_schema")
    private String requestSchema;

    /** 响应Schema。 */
    @TableField("response_schema")
    private String responseSchema;

    /** 工具可处理的意图编码 JSON 数组。 */
    @TableField("intent_codes")
    private String intentCodes;

    /** 工具自然语言路由示例 JSON 数组。 */
    @TableField("routing_examples")
    private String routingExamples;

    /** 工具执行前必填实体 JSON 数组。 */
    @TableField("required_entities")
    private String requiredEntities;

    /** 超时毫秒。 */
    @TableField("timeout_ms")
    private Integer timeoutMs;

    /** 重试数量。 */
    @TableField("retry_count")
    private Integer retryCount;

    /** 风险级别。 */
    @TableField("risk_level")
    private String riskLevel;

    /** REQUIRE确认。 */
    @TableField("require_confirm")
    private Boolean requireConfirm;

    /** 是否启用。 */
    @TableField("enabled")
    private Boolean enabled;

    /** 状态。 */
    @TableField("status")
    private String status;

    /** 来源类型。 */
    @TableField("source_type")
    private String sourceType;

    /** MCP服务ID。 */
    @TableField("mcp_server_id")
    private String mcpServerId;

    /** MCP工具名称。 */
    @TableField("mcp_tool_name")
    private String mcpToolName;

    /** 所有者用户ID。 */
    @TableField("owner_user_id")
    private String ownerUserId;

    /** 创建人ID。 */
    @TableField("created_by")
    private String createdBy;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /** 删除时间。 */
    @TableField("deleted_at")
    private LocalDateTime deletedAt;

    /** 版本。 */
    @TableField("version")
    private Long version;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getToolCode() {
        return toolCode;
    }

    public void setToolCode(String toolCode) {
        this.toolCode = toolCode;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getToolType() {
        return toolType;
    }

    public void setToolType(String toolType) {
        this.toolType = toolType;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getAuthConfig() {
        return authConfig;
    }

    public void setAuthConfig(String authConfig) {
        this.authConfig = authConfig;
    }

    public String getHeaders() {
        return headers;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }

    public String getRequestSchema() {
        return requestSchema;
    }

    public void setRequestSchema(String requestSchema) {
        this.requestSchema = requestSchema;
    }

    public String getResponseSchema() {
        return responseSchema;
    }

    public void setResponseSchema(String responseSchema) {
        this.responseSchema = responseSchema;
    }

    public String getIntentCodes() {
        return intentCodes;
    }

    public void setIntentCodes(String intentCodes) {
        this.intentCodes = intentCodes;
    }

    public String getRoutingExamples() {
        return routingExamples;
    }

    public void setRoutingExamples(String routingExamples) {
        this.routingExamples = routingExamples;
    }

    public String getRequiredEntities() {
        return requiredEntities;
    }

    public void setRequiredEntities(String requiredEntities) {
        this.requiredEntities = requiredEntities;
    }

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Integer timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Boolean getRequireConfirm() {
        return requireConfirm;
    }

    public void setRequireConfirm(Boolean requireConfirm) {
        this.requireConfirm = requireConfirm;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getMcpServerId() {
        return mcpServerId;
    }

    public void setMcpServerId(String mcpServerId) {
        this.mcpServerId = mcpServerId;
    }

    public String getMcpToolName() {
        return mcpToolName;
    }

    public void setMcpToolName(String mcpToolName) {
        this.mcpToolName = mcpToolName;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
