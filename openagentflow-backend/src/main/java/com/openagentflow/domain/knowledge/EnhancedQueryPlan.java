package com.openagentflow.domain.knowledge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 查询理解与改写计划。
 */
public class EnhancedQueryPlan {

    /** 规范化后的用户原始查询。 */
    private String originalQuery;

    /** 用于主召回的标准查询。 */
    private String canonicalQuery;

    /** 实际参与多路召回的查询变体。 */
    private List<String> variants = new ArrayList<>();

    /** 本次命中的标准词及其同义表达。 */
    private Map<String, List<String>> synonymExpansions = new LinkedHashMap<>();

    /** 是否使用会话上下文消解了指代。 */
    private boolean contextResolved;

    public String getOriginalQuery() {
        return originalQuery;
    }

    public void setOriginalQuery(String originalQuery) {
        this.originalQuery = originalQuery;
    }

    public String getCanonicalQuery() {
        return canonicalQuery;
    }

    public void setCanonicalQuery(String canonicalQuery) {
        this.canonicalQuery = canonicalQuery;
    }

    public List<String> getVariants() {
        return variants;
    }

    public void setVariants(List<String> variants) {
        this.variants = variants;
    }

    public Map<String, List<String>> getSynonymExpansions() {
        return synonymExpansions;
    }

    public void setSynonymExpansions(Map<String, List<String>> synonymExpansions) {
        this.synonymExpansions = synonymExpansions;
    }

    public boolean isContextResolved() {
        return contextResolved;
    }

    public void setContextResolved(boolean contextResolved) {
        this.contextResolved = contextResolved;
    }
}
