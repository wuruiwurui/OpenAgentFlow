package com.openagentflow.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 多查询候选融合策略。
 */
@Component
public class MultiQueryFusionPolicy {

    /** RRF 平滑常量，降低单次排名波动对最终结果的影响。 */
    private static final double RRF_K = 60D;

    /**
     * 融合多组已排序候选。
     *
     * @param queryHits 每个查询变体各自的候选列表
     * @param limit 最终候选上限
     * @param <T> 业务候选载荷类型
     * @return 去重并重新排序后的候选
     */
    public <T> List<FusedHit<T>> fuse(List<List<RankedHit<T>>> queryHits, int limit) {
        if (queryHits == null || queryHits.isEmpty() || limit <= 0) {
            return List.of();
        }
        Map<String, MutableFusedHit<T>> merged = new LinkedHashMap<>();
        for (List<RankedHit<T>> hits : queryHits) {
            if (hits == null) {
                continue;
            }
            for (RankedHit<T> hit : hits) {
                if (hit == null || hit.id() == null || hit.id().isBlank()) {
                    continue;
                }
                MutableFusedHit<T> state = merged.computeIfAbsent(hit.id(), ignored -> new MutableFusedHit<>());
                if (state.payload == null || hit.score() > state.bestScore) {
                    state.payload = hit.payload();
                    state.bestScore = hit.score();
                }
                state.hitCount++;
                state.rrfScore += 1D / (RRF_K + Math.max(1, hit.rank()));
            }
        }
        List<FusedHit<T>> result = new ArrayList<>();
        merged.forEach((id, state) -> {
            // 最佳相关度是主信号，重复命中和 RRF 用于稳定同分候选顺序。
            double fusionScore = state.bestScore + state.hitCount * 0.03D + state.rrfScore;
            result.add(new FusedHit<>(id, state.payload, state.bestScore, fusionScore, state.hitCount));
        });
        return result.stream()
                .sorted(Comparator.comparingDouble(FusedHit<T>::fusionScore).reversed())
                .limit(limit)
                .toList();
    }

    /** 单个查询中的已排序候选。 */
    public record RankedHit<T>(String id, T payload, double score, int rank) {
    }

    /** 多查询融合后的候选。 */
    public record FusedHit<T>(String id, T payload, double bestScore, double fusionScore, int hitCount) {
    }

    /** 融合过程中的可变累加状态。 */
    private static final class MutableFusedHit<T> {
        /** 当前最佳候选载荷。 */
        private T payload;
        /** 当前最佳原始得分。 */
        private double bestScore;
        /** 跨查询 RRF 累计分数。 */
        private double rrfScore;
        /** 命中该分片的查询变体数量。 */
        private int hitCount;
    }
}
