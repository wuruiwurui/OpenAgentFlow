package com.openagentflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.domain.chat.ToolDefinitionForModel;
import com.openagentflow.domain.tool.ToolDefinitionSummary;
import com.openagentflow.entity.ToolDefinitionEntity;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 工具路由元数据映射测试。
 */
class ToolRoutingMetadataMappingTests {

    /** 工具实体的路由配置应完整传递给模型定义和前端摘要。 */
    @Test
    void shouldMapRoutingMetadataToModelAndSummary() {
        ToolService service = new ToolService(
                null, null, null, null, null, null, mock(JdbcTemplate.class), new ObjectMapper(),
                null, null, null, null, null, null
        );
        ToolDefinitionEntity entity = new ToolDefinitionEntity();
        entity.setId("tool-1");
        entity.setToolCode("invoice_lookup");
        entity.setToolName("发票查询");
        entity.setDescription("查询发票开具状态");
        entity.setRequestSchema("{\"type\":\"object\"}");
        entity.setIntentCodes("[\"invoice.query\"]");
        entity.setRoutingExamples("[\"我的发票开了吗\",\"查询发票状态\"]");
        entity.setRequiredEntities("[\"invoiceNo\"]");

        ToolDefinitionForModel modelTool = ReflectionTestUtils.invokeMethod(service, "toModelTool", entity);
        ToolDefinitionSummary summary = ReflectionTestUtils.invokeMethod(service, "toSummary", entity);

        assertThat(modelTool).isNotNull();
        assertThat(modelTool.getIntentCodes()).containsExactly("invoice.query");
        assertThat(modelTool.getRoutingExamples()).containsExactly("我的发票开了吗", "查询发票状态");
        assertThat(modelTool.getRequiredEntities()).containsExactly("invoiceNo");
        assertThat(summary).isNotNull();
        assertThat(summary.getIntentCodes()).containsExactly("invoice.query");
        assertThat(summary.getRoutingExamples()).hasSize(2);
        assertThat(summary.getRequiredEntities()).containsExactly("invoiceNo");
    }
}
