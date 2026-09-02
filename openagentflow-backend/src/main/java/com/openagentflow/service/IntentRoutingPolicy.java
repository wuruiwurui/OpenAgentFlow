package com.openagentflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.openagentflow.domain.chat.IntentRoutePlan;
import com.openagentflow.domain.chat.ToolDefinitionForModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通用意图路由策略。
 * <p>路由依据全部来自工具元数据和参数 Schema，不包含订单、优惠券等具体业务规则。</p>
 */
@Component
public class IntentRoutingPolicy {

    /** 工具候选最低匹配分数。 */
    private static final double TOOL_MATCH_THRESHOLD = 0.14D;

    /** 与最高分接近的工具也进入候选，支持一次请求中的多工具意图。 */
    private static final double TOOL_SCORE_GAP = 0.12D;

    /** 中文、英文和数字组成的可检索字符。 */
    private static final Pattern INDEXABLE_CHARACTER = Pattern.compile("[\\p{IsHan}a-z0-9]");

    /** 常见业务编号形态，例如 OAF-DEMO-1001。 */
    private static final Pattern BUSINESS_IDENTIFIER = Pattern.compile(
            "(?i)(?<![a-z0-9])[a-z]{2,}(?:-[a-z0-9]+)+(?![a-z0-9])|(?<!\\d)\\d{6,}(?!\\d)"
    );

    /** 显式键值表达，例如 orderNo: OAF-DEMO-1001。 */
    private static final Pattern EXPLICIT_ENTITY = Pattern.compile(
            "(?i)([a-z_][a-z0-9_.-]*)\\s*[:：=]\\s*([a-z0-9][a-z0-9_.-]{1,80})"
    );

    /** 无需业务路由的常见寒暄表达。 */
    private static final Set<String> GREETINGS = Set.of(
            "你好", "你好啊", "您好", "嗨", "哈喽", "hello", "hi", "早上好", "下午好", "晚上好", "在吗"
    );

    /**
     * 根据输入和可用工具生成结构化运行计划。
     *
     * @param input 用户原始输入
     * @param tools Agent 已绑定并启用的工具
     * @param ragAvailable Agent 是否具备知识检索能力
     * @return 可解释的意图路由计划
     */
    public IntentRoutePlan plan(String input, List<ToolDefinitionForModel> tools, boolean ragAvailable) {
        IntentRoutePlan plan = new IntentRoutePlan();
        String normalizedInput = normalize(input);
        if (normalizedInput.isBlank()) {
            plan.setDirectAnswer(true);
            plan.setReason("输入为空，不执行工具和知识检索");
            return plan;
        }
        if (isGreeting(normalizedInput)) {
            plan.setDirectAnswer(true);
            plan.setConfidence(1D);
            plan.setReason("识别为轻量寒暄，由模型直接回答");
            return plan;
        }

        List<String> clauses = splitClauses(input);
        List<ToolScore> matches = scoreTools(clauses, tools);
        double bestScore = matches.isEmpty() ? 0D : matches.getFirst().score();
        List<ToolDefinitionForModel> candidateTools = matches.stream()
                .filter(match -> match.score() >= TOOL_MATCH_THRESHOLD)
                .filter(match -> bestScore - match.score() <= TOOL_SCORE_GAP)
                .map(ToolScore::tool)
                .toList();

        // 多个工具都命中时，先用输入中已抽取的实体淘汰无法执行的候选，避免把明细工具的必填参数错误施加到汇总工具。
        Map<String, String> candidateEntities = extractEntities(input, candidateTools);
        List<ToolDefinitionForModel> readyTools = candidateTools.stream()
                .filter(tool -> findMissingEntities(List.of(tool), candidateEntities).isEmpty())
                .toList();
        List<ToolDefinitionForModel> selectedTools = readyTools.isEmpty() ? candidateTools : readyTools;

        LinkedHashSet<String> selectedNames = new LinkedHashSet<>();
        LinkedHashSet<String> intents = new LinkedHashSet<>();
        LinkedHashSet<String> coveredClauses = new LinkedHashSet<>();
        for (ToolDefinitionForModel tool : selectedTools) {
            selectedNames.add(tool.getName());
            intents.addAll(nonBlank(tool.getIntentCodes()));
            matches.stream()
                    .filter(match -> match.tool() == tool)
                    .findFirst()
                    .ifPresent(match -> coveredClauses.addAll(match.matchedClauses()));
        }

        List<String> uncovered = clauses.stream()
                .filter(clause -> !coveredClauses.contains(clause))
                .toList();
        plan.setSelectedToolNames(new ArrayList<>(selectedNames));
        plan.setIntents(new ArrayList<>(intents));
        plan.setUncoveredIntents(uncovered);
        plan.setNeedTool(!selectedTools.isEmpty());
        plan.setNeedRag(ragAvailable && (selectedTools.isEmpty() || !uncovered.isEmpty()));
        plan.setDirectAnswer(selectedTools.isEmpty() && !ragAvailable);
        plan.setConfidence(Math.min(1D, bestScore));

        Map<String, String> entities = readyTools.isEmpty() ? candidateEntities : extractEntities(input, selectedTools);
        List<String> missingEntities = findMissingEntities(selectedTools, entities);
        plan.setEntities(entities);
        plan.setMissingEntities(missingEntities);
        plan.setNeedsClarification(!missingEntities.isEmpty());
        if (!missingEntities.isEmpty()) {
            plan.setClarificationQuestion("继续执行前请补充以下信息：" + String.join("、", missingEntities));
        }
        plan.setReason(buildReason(plan));
        return plan;
    }

    /** 计算每个工具与所有子意图的匹配分数。 */
    private List<ToolScore> scoreTools(List<String> clauses, List<ToolDefinitionForModel> tools) {
        if (tools == null || tools.isEmpty()) {
            return List.of();
        }
        List<ToolScore> scores = new ArrayList<>();
        for (ToolDefinitionForModel tool : tools) {
            double totalScore = 0D;
            List<String> matchedClauses = new ArrayList<>();
            for (String clause : clauses) {
                double score = scoreClause(clause, tool);
                if (score >= TOOL_MATCH_THRESHOLD) {
                    matchedClauses.add(clause);
                    totalScore += score;
                }
            }
            if (totalScore > 0D) {
                scores.add(new ToolScore(tool, Math.min(1D, totalScore), matchedClauses));
            }
        }
        scores.sort(Comparator.comparingDouble(ToolScore::score).reversed());
        return scores;
    }

    /** 使用名称、描述、意图编码和示例中的最佳相似度评价一个子意图。 */
    private double scoreClause(String clause, ToolDefinitionForModel tool) {
        List<String> corpus = new ArrayList<>();
        corpus.add(tool.getName());
        corpus.add(tool.getDescription());
        corpus.addAll(nonBlank(tool.getIntentCodes()));
        corpus.addAll(nonBlank(tool.getRoutingExamples()));
        return corpus.stream()
                .filter(value -> value != null && !value.isBlank())
                .mapToDouble(value -> diceSimilarity(clause, value))
                .max()
                .orElse(0D);
    }

    /** 使用字符二元组 Dice 系数兼容中文和英文短句。 */
    private double diceSimilarity(String left, String right) {
        String normalizedLeft = indexable(left);
        String normalizedRight = indexable(right);
        if (normalizedLeft.isBlank() || normalizedRight.isBlank()) {
            return 0D;
        }
        if (normalizedLeft.contains(normalizedRight) || normalizedRight.contains(normalizedLeft)) {
            int min = Math.min(normalizedLeft.length(), normalizedRight.length());
            int max = Math.max(normalizedLeft.length(), normalizedRight.length());
            return 0.55D + 0.45D * min / max;
        }
        Set<String> leftPairs = ngrams(normalizedLeft);
        Set<String> rightPairs = ngrams(normalizedRight);
        long intersection = leftPairs.stream().filter(rightPairs::contains).count();
        return leftPairs.isEmpty() || rightPairs.isEmpty()
                ? 0D
                : 2D * intersection / (leftPairs.size() + rightPairs.size());
    }

    /** 从输入中抽取显式键值和常见业务编号，并映射到工具必填实体。 */
    private Map<String, String> extractEntities(String input, List<ToolDefinitionForModel> tools) {
        Map<String, String> entities = new LinkedHashMap<>();
        Matcher explicitMatcher = EXPLICIT_ENTITY.matcher(input == null ? "" : input);
        while (explicitMatcher.find()) {
            entities.put(explicitMatcher.group(1), explicitMatcher.group(2));
        }
        Matcher identifierMatcher = BUSINESS_IDENTIFIER.matcher(input == null ? "" : input);
        String identifier = identifierMatcher.find() ? identifierMatcher.group() : null;
        if (identifier != null) {
            for (String entityName : requiredEntities(tools)) {
                String normalizedName = entityName.toLowerCase(Locale.ROOT);
                if (!entities.containsKey(entityName)
                        && (normalizedName.endsWith("no") || normalizedName.contains("number")
                        || normalizedName.endsWith("id") || normalizedName.contains("code"))) {
                    entities.put(entityName, identifier);
                }
            }
        }
        return entities;
    }

    /** 汇总工具显式配置和 JSON Schema 中的 required 字段。 */
    private List<String> requiredEntities(Collection<ToolDefinitionForModel> tools) {
        LinkedHashSet<String> required = new LinkedHashSet<>();
        for (ToolDefinitionForModel tool : tools) {
            required.addAll(nonBlank(tool.getRequiredEntities()));
            JsonNode schemaRequired = tool.getParameters() == null ? null : tool.getParameters().path("required");
            if (schemaRequired != null && schemaRequired.isArray()) {
                schemaRequired.forEach(node -> {
                    if (node.isTextual() && !node.asText().isBlank()) {
                        required.add(node.asText());
                    }
                });
            }
        }
        return new ArrayList<>(required);
    }

    /** 找出选中工具尚未获得值的必填实体。 */
    private List<String> findMissingEntities(List<ToolDefinitionForModel> tools, Map<String, String> entities) {
        return requiredEntities(tools).stream()
                .filter(name -> entities.entrySet().stream().noneMatch(entry -> entry.getKey().equalsIgnoreCase(name)))
                .toList();
    }

    /** 生成简短且可展示的路由说明。 */
    private String buildReason(IntentRoutePlan plan) {
        if (plan.isNeedsClarification()) {
            return "已匹配工具，但缺少必填实体，暂不执行工具";
        }
        if (plan.isNeedTool() && plan.isNeedRag()) {
            return "识别到实时操作和知识咨询两个或多个子意图";
        }
        if (plan.isNeedTool()) {
            return "用户表达与工具路由元数据匹配";
        }
        if (plan.isNeedRag()) {
            return "没有工具能够可靠覆盖该问题，转入知识检索";
        }
        return "没有可用工具或知识库，由模型直接回答";
    }

    /** 按标点和常见并列连接词拆分多意图输入。 */
    private List<String> splitClauses(String input) {
        if (input == null || input.isBlank()) {
            return List.of();
        }
        List<String> clauses = Arrays.stream(input.split("[，,；;。!?！？]|(?:并且)|(?:同时)|(?:以及)|(?:然后)|(?:再帮我)|(?:并说明)"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        return clauses.isEmpty() ? List.of(input.trim()) : clauses;
    }

    /** 判断是否是无需检索的轻量寒暄。 */
    private boolean isGreeting(String input) {
        return GREETINGS.contains(input.replaceAll("[。！!？?~～\\s]", ""));
    }

    /** 统一大小写和空白。 */
    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    /** 仅保留适合计算字符相似度的字符。 */
    private String indexable(String value) {
        String normalized = normalize(value);
        StringBuilder builder = new StringBuilder();
        normalized.codePoints().forEach(codePoint -> {
            String character = new String(Character.toChars(codePoint));
            if (INDEXABLE_CHARACTER.matcher(character).matches()) {
                builder.append(character);
            }
        });
        return builder.toString();
    }

    /** 生成字符二元组，单字符文本保留自身。 */
    private Set<String> ngrams(String value) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (value.length() < 2) {
            result.add(value);
            return result;
        }
        for (int index = 0; index < value.length() - 1; index++) {
            result.add(value.substring(index, index + 2));
        }
        return result;
    }

    /** 过滤空路由元数据。 */
    private List<String> nonBlank(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(value -> value != null && !value.isBlank())
                .toList();
    }

    /** 工具与输入子句的内部评分结果。 */
    private record ToolScore(ToolDefinitionForModel tool, double score, List<String> matchedClauses) {
    }
}
