package com.openagentflow.domain.tool;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工具定义展示摘要。
 */
public class ToolDefinitionSummary {

    /** 工具 ID。 */
    private String id;

    /** 工具编码。 */
    private String toolCode;

    /** 工具名称。 */
    private String toolName;

    /** 工具类型。 */
    private String toolType;

    /** 所属工作空间 ID。 */
    private String workspaceId;

    /** 所属工作空间名称。 */
    private String workspaceName;

    /** 工具描述。 */
    private String description;

    /** 请求方法。 */
    private String requestMethod;

    /** 请求 URL 或查询模板。 */
    private String endpointUrl;

    /** 认证类型。 */
    private String authType;

    /** 认证配置 JSON。 */
    private String authConfig;

    /** 请求头 JSON。 */
    private String headers;

    /** 请求参数 JSON Schema。 */
    private String requestSchema;

    /** 响应 JSON Schema。 */
    private String responseSchema;

    /** 工具可处理的意图编码。 */
    private List<String> intentCodes;

    /** 工具自然语言路由示例。 */
    private List<String> routingExamples;

    /** 执行工具前必须具备的实体名称。 */
    private List<String> requiredEntities;

    /** 超时毫秒。 */
    private Integer timeoutMs;

    /** 重试次数。 */
    private Integer retryCount;

    /** 风险等级。 */
    private String riskLevel;

    /** 风险等级中文名称。 */
    private String riskLabel;

    /** 是否需要确认。 */
    private Boolean requireConfirm;

    /** 是否启用。 */
    private Boolean enabled;

    /** 来源类型，例如 manual 或 mcp。 */
    private String sourceType;

    /** MCP Server ID，仅 MCP 工具有值。 */
    private String mcpServerId;

    /** MCP 原始工具名称，仅 MCP 工具有值。 */
    private String mcpToolName;

    /** 状态。 */
    private String status;

    /** 调用次数。 */
    private Integer invocationCount;

    /** 成功率百分比。 */
    private Integer successRate;

    /** 更新时间。 */
    private LocalDateTime updatedAt;

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

    public String getWorkspaceName() {
        return workspaceName;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
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

    public List<String> getIntentCodes() {
        return intentCodes;
    }

    public void setIntentCodes(List<String> intentCodes) {
        this.intentCodes = intentCodes;
    }

    public List<String> getRoutingExamples() {
        return routingExamples;
    }

    public void setRoutingExamples(List<String> routingExamples) {
        this.routingExamples = routingExamples;
    }

    public List<String> getRequiredEntities() {
        return requiredEntities;
    }

    public void setRequiredEntities(List<String> requiredEntities) {
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

    public String getRiskLabel() {
        return riskLabel;
    }

    public void setRiskLabel(String riskLabel) {
        this.riskLabel = riskLabel;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getInvocationCount() {
        return invocationCount;
    }

    public void setInvocationCount(Integer invocationCount) {
        this.invocationCount = invocationCount;
    }

    public Integer getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(Integer successRate) {
        this.successRate = successRate;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
