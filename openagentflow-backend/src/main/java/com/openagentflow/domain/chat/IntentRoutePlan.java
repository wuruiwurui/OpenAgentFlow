package com.openagentflow.domain.chat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 运行前的结构化意图路由计划。
 */
public class IntentRoutePlan {

    /** 识别出的意图编码。 */
    private List<String> intents = new ArrayList<>();

    /** 从用户输入中抽取的实体。 */
    private Map<String, String> entities = new LinkedHashMap<>();

    /** 允许交给模型选择的工具函数名称。 */
    private List<String> selectedToolNames = new ArrayList<>();

    /** 未被工具覆盖、应交给知识检索处理的子意图。 */
    private List<String> uncoveredIntents = new ArrayList<>();

    /** 当前工具调用缺失的必填实体名称。 */
    private List<String> missingEntities = new ArrayList<>();

    /** 是否需要模型执行工具调用。 */
    private boolean needTool;

    /** 是否需要执行知识库检索。 */
    private boolean needRag;

    /** 是否属于可以直接回答的轻量对话。 */
    private boolean directAnswer;

    /** 是否必须先向用户澄清。 */
    private boolean needsClarification;

    /** 路由置信度，取值范围为 0 到 1。 */
    private double confidence;

    /** 路由决策的可读解释。 */
    private String reason;

    /** 缺失实体时建议向用户提出的问题。 */
    private String clarificationQuestion;

    public List<String> getIntents() {
        return intents;
    }

    public void setIntents(List<String> intents) {
        this.intents = intents;
    }

    public Map<String, String> getEntities() {
        return entities;
    }

    public void setEntities(Map<String, String> entities) {
        this.entities = entities;
    }

    public List<String> getSelectedToolNames() {
        return selectedToolNames;
    }

    public void setSelectedToolNames(List<String> selectedToolNames) {
        this.selectedToolNames = selectedToolNames;
    }

    public List<String> getUncoveredIntents() {
        return uncoveredIntents;
    }

    public void setUncoveredIntents(List<String> uncoveredIntents) {
        this.uncoveredIntents = uncoveredIntents;
    }

    public List<String> getMissingEntities() {
        return missingEntities;
    }

    public void setMissingEntities(List<String> missingEntities) {
        this.missingEntities = missingEntities;
    }

    public boolean isNeedTool() {
        return needTool;
    }

    public void setNeedTool(boolean needTool) {
        this.needTool = needTool;
    }

    public boolean isNeedRag() {
        return needRag;
    }

    public void setNeedRag(boolean needRag) {
        this.needRag = needRag;
    }

    public boolean isDirectAnswer() {
        return directAnswer;
    }

    public void setDirectAnswer(boolean directAnswer) {
        this.directAnswer = directAnswer;
    }

    public boolean isNeedsClarification() {
        return needsClarification;
    }

    public void setNeedsClarification(boolean needsClarification) {
        this.needsClarification = needsClarification;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getClarificationQuestion() {
        return clarificationQuestion;
    }

    public void setClarificationQuestion(String clarificationQuestion) {
        this.clarificationQuestion = clarificationQuestion;
    }
}
