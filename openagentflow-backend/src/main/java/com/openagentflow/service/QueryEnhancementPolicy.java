package com.openagentflow.service;

import com.openagentflow.domain.knowledge.EnhancedQueryPlan;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * RAG 查询理解策略。
 * <p>采用确定性规则保护编号、日期等关键实体，并提供可配置的领域同义词扩展。</p>
 */
@Component
public class QueryEnhancementPolicy {

    /** 查询变体允许的最大数量，避免放大下游召回请求。 */
    private static final int HARD_MAX_VARIANTS = 8;

    /** 常见无检索价值的口语请求前缀。 */
    private static final Pattern CONVERSATIONAL_PREFIX = Pattern.compile(
            "^(?:请问|请帮我|麻烦你|麻烦|帮我|我想知道|我想了解|能不能|可以帮我|请查询|查一下)+"
    );

    /** 需要结合上文消解的指代表达。 */
    private static final Pattern REFERENCE_EXPRESSION = Pattern.compile(
            "(?:它|这个|这项|该(?:产品|合同|政策|订单|事项)?|上述|上面|前面|刚才|其)"
    );

    /** 默认中文领域同义词，后续可由工作空间词典配置覆盖。 */
    private final Map<String, List<String>> synonyms;

    /** 使用平台内置同义词创建策略。 */
    public QueryEnhancementPolicy() {
        this(defaultSynonyms());
    }

    /**
     * 使用指定同义词创建策略。
     *
     * @param synonyms 标准词到同义表达的映射
     */
    public QueryEnhancementPolicy(Map<String, List<String>> synonyms) {
        this.synonyms = normalizeSynonyms(synonyms);
    }

    /**
     * 生成查询理解计划。
     *
     * @param query 用户查询
     * @param conversationContext 最近会话上下文
     * @param maxVariants 最大查询变体数
     * @return 查询改写计划
     */
    public EnhancedQueryPlan enhance(String query, String conversationContext, int maxVariants) {
        EnhancedQueryPlan plan = new EnhancedQueryPlan();
        String original = normalizeText(query);
        String rewritten = stripConversationalPrefix(original);
        boolean contextResolved = REFERENCE_EXPRESSION.matcher(rewritten).find()
                && StringUtils.hasText(conversationContext);
        if (contextResolved) {
            String context = tail(normalizeText(conversationContext), 240);
            rewritten = context + "；当前问题：" + rewritten;
        }

        Map<String, List<String>> matchedSynonyms = new LinkedHashMap<>();
        String canonical = canonicalize(rewritten, matchedSynonyms);
        LinkedHashSet<String> variants = new LinkedHashSet<>();
        variants.add(original);
        variants.add(canonical);
        for (Map.Entry<String, List<String>> entry : matchedSynonyms.entrySet()) {
            for (String alias : entry.getValue()) {
                variants.add(canonical.replace(entry.getKey(), alias));
            }
        }
        String keywordVariant = compactKeywords(canonical);
        if (StringUtils.hasText(keywordVariant)) {
            variants.add(keywordVariant);
        }

        int limit = Math.max(1, Math.min(HARD_MAX_VARIANTS, maxVariants));
        plan.setOriginalQuery(original);
        plan.setCanonicalQuery(canonical);
        plan.setVariants(variants.stream().filter(StringUtils::hasText).limit(limit).toList());
        plan.setSynonymExpansions(matchedSynonyms);
        plan.setContextResolved(contextResolved);
        return plan;
    }

    /** 不带会话上下文的便捷查询增强入口。 */
    public EnhancedQueryPlan enhance(String query, int maxVariants) {
        return enhance(query, "", maxVariants);
    }

    /** 将命中的别名替换为标准词，并记录本次实际扩展。 */
    private String canonicalize(String query, Map<String, List<String>> matchedSynonyms) {
        String canonical = query;
        for (Map.Entry<String, List<String>> entry : synonyms.entrySet()) {
            boolean matched = canonical.contains(entry.getKey());
            for (String alias : entry.getValue()) {
                if (canonical.contains(alias)) {
                    canonical = canonical.replace(alias, entry.getKey());
                    matched = true;
                }
            }
            if (matched) {
                matchedSynonyms.put(entry.getKey(), entry.getValue());
            }
        }
        return canonical;
    }

    /** 清理口语前缀，但不删除编号、日期、数字和专有名词。 */
    private String stripConversationalPrefix(String query) {
        String stripped = CONVERSATIONAL_PREFIX.matcher(query).replaceFirst("");
        return stripped.replaceFirst("^(?:一下|下|有关|关于)", "").trim();
    }

    /** 生成面向 BM25 的紧凑关键词变体。 */
    private String compactKeywords(String query) {
        return query.replaceAll("(?:请|帮|一下|一下子|能否|是否|可以|怎么|如何|什么是|有哪些)", " ")
                .replaceAll("[，,。！？!?；;：:]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /** 使用 NFKC 统一全半角符号并压缩空白。 */
    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .replaceAll("\\s+", " ")
                .trim();
    }

    /** 截取最近上下文，避免查询变体无限增长。 */
    private String tail(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(value.length() - maxLength);
    }

    /** 规范化同义词字典，去除空值和重复项。 */
    private Map<String, List<String>> normalizeSynonyms(Map<String, List<String>> source) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        if (source == null) {
            return result;
        }
        source.forEach((canonical, aliases) -> {
            if (!StringUtils.hasText(canonical)) {
                return;
            }
            List<String> normalizedAliases = aliases == null ? List.of() : aliases.stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .filter(alias -> !alias.equals(canonical.trim()))
                    .distinct()
                    .toList();
            result.put(canonical.trim(), normalizedAliases);
        });
        return result;
    }

    /** 平台默认词典，覆盖常见中文别名、错别字和业务表达。 */
    private static Map<String, List<String>> defaultSynonyms() {
        Map<String, List<String>> values = new LinkedHashMap<>();
        values.put("优惠券", List.of("优惠卷", "代金券"));
        values.put("订单编号", List.of("订单号", "单号"));
        values.put("知识库", List.of("文档库", "资料库"));
        values.put("退款", List.of("退钱", "退费"));
        values.put("物流", List.of("快递", "配送轨迹"));
        return values;
    }
}
