package com.openagentflow.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 多查询候选融合策略测试。
 */
class MultiQueryFusionPolicyTests {

    /** 相同分片跨查询重复命中时应去重，并排在同分单次命中之前。 */
    @Test
    void shouldDeduplicateAndRewardCrossQueryHits() {
        MultiQueryFusionPolicy policy = new MultiQueryFusionPolicy();
        List<List<MultiQueryFusionPolicy.RankedHit<String>>> queryHits = List.of(
                List.of(
                        new MultiQueryFusionPolicy.RankedHit<>("chunk-a", "A", 0.78D, 1),
                        new MultiQueryFusionPolicy.RankedHit<>("chunk-b", "B", 0.78D, 2)
                ),
                List.of(
                        new MultiQueryFusionPolicy.RankedHit<>("chunk-a", "A2", 0.74D, 2),
                        new MultiQueryFusionPolicy.RankedHit<>("chunk-c", "C", 0.70D, 1)
                )
        );

        List<MultiQueryFusionPolicy.FusedHit<String>> fused = policy.fuse(queryHits, 10);

        assertThat(fused).extracting(MultiQueryFusionPolicy.FusedHit::id)
                .containsExactly("chunk-a", "chunk-b", "chunk-c");
        assertThat(fused.getFirst().hitCount()).isEqualTo(2);
        assertThat(fused.getFirst().payload()).isEqualTo("A");
    }

    /** 融合结果必须遵守候选上限。 */
    @Test
    void shouldLimitFusedCandidates() {
        MultiQueryFusionPolicy policy = new MultiQueryFusionPolicy();

        List<MultiQueryFusionPolicy.FusedHit<String>> fused = policy.fuse(List.of(List.of(
                new MultiQueryFusionPolicy.RankedHit<>("a", "A", 0.9D, 1),
                new MultiQueryFusionPolicy.RankedHit<>("b", "B", 0.8D, 2),
                new MultiQueryFusionPolicy.RankedHit<>("c", "C", 0.7D, 3)
        )), 2);

        assertThat(fused).hasSize(2);
    }
}
