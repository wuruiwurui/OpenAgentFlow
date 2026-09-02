package com.openagentflow.service;

import com.openagentflow.domain.knowledge.EnhancedQueryPlan;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RAG 查询理解策略测试。
 */
class QueryEnhancementPolicyTests {

    /** 查询改写应清理口语前缀，同时完整保留编号和日期。 */
    @Test
    void shouldPreserveIdentifiersAndDatesDuringRewrite() {
        QueryEnhancementPolicy policy = new QueryEnhancementPolicy();

        EnhancedQueryPlan plan = policy.enhance(
                "请帮我查一下订单号 OAF-DEMO-1001 在 2026-09-02 的状态",
                "",
                4
        );

        assertThat(plan.getCanonicalQuery()).contains("OAF-DEMO-1001", "2026-09-02");
        assertThat(plan.getCanonicalQuery()).doesNotStartWith("请帮我");
        assertThat(plan.getVariants()).doesNotHaveDuplicates().hasSizeLessThanOrEqualTo(4);
    }

    /** 指代性问题应带入最近会话上下文中的主题。 */
    @Test
    void shouldResolveReferenceWithConversationContext() {
        QueryEnhancementPolicy policy = new QueryEnhancementPolicy();

        EnhancedQueryPlan plan = policy.enhance(
                "它的退款条件呢",
                "上一轮正在讨论产品 A-100 的企业采购合同",
                4
        );

        assertThat(plan.isContextResolved()).isTrue();
        assertThat(plan.getCanonicalQuery()).contains("A-100", "退款条件");
    }

    /** 自定义领域同义词应把错误或别名表达扩展为可召回变体。 */
    @Test
    void shouldExpandConfiguredDomainSynonyms() {
        QueryEnhancementPolicy policy = new QueryEnhancementPolicy(Map.of(
                "优惠券", List.of("优惠卷", "代金券")
        ));

        EnhancedQueryPlan plan = policy.enhance("有哪些优惠卷可以使用", "", 4);

        assertThat(plan.getCanonicalQuery()).contains("优惠券");
        assertThat(plan.getVariants()).anyMatch(query -> query.contains("代金券"));
        assertThat(plan.getSynonymExpansions()).containsKey("优惠券");
    }
}
