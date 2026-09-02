package com.openagentflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.domain.knowledge.RerankResult;
import com.openagentflow.entity.ModelConfigEntity;
import com.openagentflow.entity.ModelProviderEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 真实 Cross-Encoder Rerank 模型调用服务。
 */
@Service
public class CrossEncoderRerankService {

    /** 模型服务商配置服务。 */
    private final ModelProviderService modelProviderService;

    /** JSON 序列化工具。 */
    private final ObjectMapper objectMapper;

    /** 支持连接复用和超时控制的 JDK HTTP 客户端。 */
    private final HttpClient httpClient;

    /** Spring 生产环境构造函数。 */
    @Autowired
    public CrossEncoderRerankService(ModelProviderService modelProviderService, ObjectMapper objectMapper) {
        this(modelProviderService, objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    /** 测试可注入 HTTP 客户端的构造函数。 */
    CrossEncoderRerankService(ModelProviderService modelProviderService,
                              ObjectMapper objectMapper,
                              HttpClient httpClient) {
        this.modelProviderService = modelProviderService;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    /**
     * 使用平台模型配置重排候选文本。
     *
     * @param modelId 重排模型配置 ID
     * @param query 用户查询
     * @param documents 候选分片正文
     * @return 与候选顺序一致的模型相关度
     */
    public RerankResult rerank(String modelId, String query, List<String> documents) {
        long startedAt = System.nanoTime();
        RerankResult result = new RerankResult();
        result.setModelId(modelId);
        result.setMode("rule_fallback");
        if (!StringUtils.hasText(modelId) || documents == null || documents.isEmpty()) {
            result.setErrorMessage("未配置重排模型或候选为空");
            return result;
        }
        try {
            ModelConfigEntity model = modelProviderService.requireModel(modelId);
            ModelProviderEntity provider = modelProviderService.requireProviderByModel(model);
            String apiKey = modelProviderService.findApiKeyValue(provider.getId());
            String endpoint = rerankEndpoint(provider.getBaseUrl());

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", model.getModelCode());
            payload.put("query", query);
            payload.put("documents", documents);
            payload.put("top_n", documents.size());
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8));
            applyDefaultHeaders(requestBuilder, provider.getDefaultHeaders());
            if (StringUtils.hasText(apiKey)) {
                requestBuilder.header("Authorization", "Bearer " + apiKey);
            }

            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Rerank HTTP " + response.statusCode() + "：" + compact(response.body()));
            }
            result.setScores(parseScores(response.body(), documents.size()));
            result.setSuccess(true);
            result.setMode("cross_encoder");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            result.setErrorMessage("Rerank 调用被中断：" + compact(exception.getMessage()));
        } catch (Exception exception) {
            result.setErrorMessage(compact(exception.getMessage()));
        } finally {
            result.setLatencyMs((int) Math.min(Integer.MAX_VALUE,
                    java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)));
        }
        return result;
    }

    /** 解析标准 Cross-Encoder 结果并恢复为原候选顺序。 */
    private List<Double> parseScores(String responseBody, int documentCount) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode results = root.path("results");
        if (!results.isArray()) {
            results = root.path("data");
        }
        if (!results.isArray()) {
            throw new IllegalStateException("Rerank 响应缺少 results 或 data 数组");
        }
        List<Double> scores = new ArrayList<>(java.util.Collections.nCopies(documentCount, null));
        for (JsonNode item : results) {
            int index = item.path("index").asInt(-1);
            JsonNode scoreNode = item.has("relevance_score") ? item.path("relevance_score") : item.path("score");
            if (index < 0 || index >= documentCount || !scoreNode.isNumber()) {
                throw new IllegalStateException("Rerank 响应包含非法候选索引或分数");
            }
            scores.set(index, scoreNode.asDouble());
        }
        if (scores.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalStateException("Rerank 响应未覆盖全部候选");
        }
        return scores;
    }

    /** 合并服务商默认请求头。 */
    private void applyDefaultHeaders(HttpRequest.Builder builder, String defaultHeaders) {
        if (!StringUtils.hasText(defaultHeaders)) {
            return;
        }
        try {
            Map<String, Object> headers = objectMapper.readValue(defaultHeaders, new TypeReference<Map<String, Object>>() {
            });
            headers.forEach((name, value) -> {
                if (StringUtils.hasText(name) && value != null
                        && !"authorization".equalsIgnoreCase(name)
                        && !"content-type".equalsIgnoreCase(name)) {
                    builder.header(name, String.valueOf(value));
                }
            });
        } catch (Exception ignored) {
            // 无效默认头不应阻断真实重排，认证头仍由模型服务统一生成。
        }
    }

    /** 根据模型服务商基础地址生成标准重排地址。 */
    private String rerankEndpoint(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException("重排模型服务商 Base URL 不能为空");
        }
        String normalized = baseUrl.trim().replaceAll("/+$", "");
        return normalized.endsWith("/rerank") ? normalized : normalized + "/rerank";
    }

    /** 压缩上游错误文本，避免把超长响应写入日志和页面。 */
    private String compact(String value) {
        String text = value == null ? "未知错误" : value.replaceAll("\\s+", " ").trim();
        return text.length() <= 500 ? text : text.substring(0, 500);
    }
}
