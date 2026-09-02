package com.openagentflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.domain.chat.IntentRoutePlan;
import com.openagentflow.domain.chat.ToolDefinitionForModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 通用意图路由策略测试。
 */
class IntentRoutingPolicyTests {

    /** JSON 工具类。 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 问候语应由模型直接回答，不能误触发工具或知识库。 */
    @Test
    void shouldRouteGreetingToDirectAnswer() throws Exception {
        IntentRoutingPolicy policy = new IntentRoutingPolicy();

        IntentRoutePlan plan = policy.plan("你好啊", List.of(orderTool()), true);

        assertThat(plan.isDirectAnswer()).isTrue();
        assertThat(plan.isNeedTool()).isFalse();
        assertThat(plan.isNeedRag()).isFalse();
        assertThat(plan.getSelectedToolNames()).isEmpty();
    }

    /** 工具配置的路由示例应覆盖没有显式工具名称的同义表达。 */
    @Test
    void shouldMatchToolByRoutingExample() throws Exception {
        IntentRoutingPolicy policy = new IntentRoutingPolicy();

        IntentRoutePlan plan = policy.plan("我买的东西到哪了", List.of(orderTool()), true);

        assertThat(plan.isNeedTool()).isTrue();
        assertThat(plan.getSelectedToolNames()).containsExactly("order_query");
        assertThat(plan.getIntents()).contains("logistics.track");
    }

    /** 工具缺少必填实体时应先澄清，不能直接执行。 */
    @Test
    void shouldRequestClarificationWhenRequiredEntityIsMissing() throws Exception {
        IntentRoutingPolicy policy = new IntentRoutingPolicy();

        IntentRoutePlan plan = policy.plan("帮我查询这笔订单的物流", List.of(orderTool()), true);

        assertThat(plan.isNeedsClarification()).isTrue();
        assertThat(plan.getMissingEntities()).containsExactly("orderNo");
        assertThat(plan.getClarificationQuestion()).contains("orderNo");
    }

    /** 一次输入包含实时查询和政策咨询时，应同时规划工具与 RAG。 */
    @Test
    void shouldPlanToolAndRagForMultipleIntents() throws Exception {
        IntentRoutingPolicy policy = new IntentRoutingPolicy();

        IntentRoutePlan plan = policy.plan(
                "查询订单 OAF-DEMO-1001 到哪里了，并说明退款政策",
                List.of(orderTool()),
                true
        );

        assertThat(plan.isNeedTool()).isTrue();
        assertThat(plan.isNeedRag()).isTrue();
        assertThat(plan.getSelectedToolNames()).containsExactly("order_query");
        assertThat(plan.getEntities()).containsEntry("orderNo", "OAF-DEMO-1001");
        assertThat(plan.getUncoveredIntents()).isNotEmpty();
    }

    /** 构造包含通用路由元数据的订单查询工具。 */
    private ToolDefinitionForModel orderTool() throws Exception {
        ToolDefinitionForModel tool = new ToolDefinitionForModel();
        tool.setId("tool-order");
        tool.setName("order_query");
        tool.setDescription("按订单号查询订单状态和物流轨迹");
        tool.setIntentCodes(List.of("order.query", "logistics.track"));
        tool.setRoutingExamples(List.of("我的订单到哪里了", "查询订单物流", "查订单状态"));
        tool.setRequiredEntities(List.of("orderNo"));
        tool.setParameters(objectMapper.readTree("""
                {
                  "type": "object",
                  "properties": {
                    "orderNo": {"type": "string", "description": "订单编号"}
                  },
                  "required": ["orderNo"]
                }
                """));
        return tool;
    }
}
