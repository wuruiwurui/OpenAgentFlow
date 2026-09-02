package com.openagentflow.service;

import com.openagentflow.domain.knowledge.KnowledgeRetrievalRequest;
import com.openagentflow.domain.knowledge.KnowledgeRetrievalResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P78 检索增强 API 字段合同测试。
 */
class KnowledgeRetrievalEnhancementContractTests {

    /** 请求开关和响应可观察字段应可由 JSON DTO 正常承载。 */
    @Test
    void shouldExposeQueryEnhancementAndRerankMetadata() {
        KnowledgeRetrievalRequest request = new KnowledgeRetrievalRequest();
        request.setQueryRewriteEnabled(true);
        request.setMultiQueryEnabled(true);
        request.setMaxQueryVariants(4);
        request.setConversationContext("上一轮讨论合同 A-100");

        KnowledgeRetrievalResult result = new KnowledgeRetrievalResult();
        result.setOriginalQuery("它的退款条件");
        result.setCanonicalQuery("合同 A-100 的退款条件");
        result.setEnhancedQueries(List.of("合同 A-100 的退款条件", "合同 A-100 的退费条件"));
        result.setContextResolved(true);
        result.setRerankMode("cross_encoder");
        result.setRerankModelId("rerank-1");
        result.setRerankLatencyMs(31);

        assertThat(request.getMaxQueryVariants()).isEqualTo(4);
        assertThat(request.getConversationContext()).contains("A-100");
        assertThat(result.getEnhancedQueries()).hasSize(2);
        assertThat(result.getRerankMode()).isEqualTo("cross_encoder");
        assertThat(result.getRerankLatencyMs()).isEqualTo(31);
    }
}
