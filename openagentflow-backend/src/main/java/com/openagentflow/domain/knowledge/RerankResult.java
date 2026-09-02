package com.openagentflow.domain.knowledge;

import java.util.ArrayList;
import java.util.List;

/**
 * Cross-Encoder 重排调用结果。
 */
public class RerankResult {

    /** 真实模型调用是否成功。 */
    private boolean success;

    /** 重排模式：cross_encoder 或 rule_fallback。 */
    private String mode;

    /** 平台重排模型配置 ID。 */
    private String modelId;

    /** 与输入候选顺序一致的相关度分数。 */
    private List<Double> scores = new ArrayList<>();

    /** 真实重排调用耗时毫秒。 */
    private int latencyMs;

    /** 调用失败或协议异常原因。 */
    private String errorMessage;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public List<Double> getScores() {
        return scores;
    }

    public void setScores(List<Double> scores) {
        this.scores = scores;
    }

    public int getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(int latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
