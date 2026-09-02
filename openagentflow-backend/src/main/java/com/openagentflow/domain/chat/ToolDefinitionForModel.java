package com.openagentflow.domain.chat;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 发送给模型的工具定义。
 */
public class ToolDefinitionForModel {

    /** 工具 ID。 */
    private String id;

    /** 工具函数名称。 */
    private String name;

    /** 工具描述。 */
    private String description;

    /** 工具参数 JSON Schema。 */
    private JsonNode parameters;

    /** 工具可处理的意图编码。 */
    private List<String> intentCodes = new ArrayList<>();

    /** 用于匹配用户自然语言表达的路由示例。 */
    private List<String> routingExamples = new ArrayList<>();

    /** 执行工具前必须抽取到的实体名称。 */
    private List<String> requiredEntities = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public JsonNode getParameters() {
        return parameters;
    }

    public void setParameters(JsonNode parameters) {
        this.parameters = parameters;
    }

    public List<String> getIntentCodes() {
        return intentCodes;
    }

    public void setIntentCodes(List<String> intentCodes) {
        this.intentCodes = intentCodes == null ? new ArrayList<>() : intentCodes;
    }

    public List<String> getRoutingExamples() {
        return routingExamples;
    }

    public void setRoutingExamples(List<String> routingExamples) {
        this.routingExamples = routingExamples == null ? new ArrayList<>() : routingExamples;
    }

    public List<String> getRequiredEntities() {
        return requiredEntities;
    }

    public void setRequiredEntities(List<String> requiredEntities) {
        this.requiredEntities = requiredEntities == null ? new ArrayList<>() : requiredEntities;
    }
}
