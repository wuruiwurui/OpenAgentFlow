package com.openagentflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.domain.knowledge.RerankResult;
import com.openagentflow.entity.ModelConfigEntity;
import com.openagentflow.entity.ModelProviderEntity;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 真实 Cross-Encoder Rerank HTTP 适配器测试。
 */
class CrossEncoderRerankServiceTests {

    /** 测试 HTTP 服务。 */
    private HttpServer server;

    /** 每个测试结束后关闭端口。 */
    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    /** 标准 results 响应应按 index 还原每个候选的相关度。 */
    @Test
    void shouldCallRerankEndpointAndParseScores() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/rerank", exchange -> {
            String authorization = exchange.getRequestHeaders().getFirst("Authorization");
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(authorization).isEqualTo("Bearer test-key");
            assertThat(requestBody).contains("rerank-model", "退款条件", "候选二");
            byte[] body = "{\"results\":[{\"index\":1,\"relevance_score\":0.91},{\"index\":0,\"relevance_score\":0.52}]}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        ModelProviderService providerService = providerService("http://localhost:" + server.getAddress().getPort() + "/v1");
        CrossEncoderRerankService service = new CrossEncoderRerankService(
                providerService, new ObjectMapper(), HttpClient.newHttpClient()
        );

        RerankResult result = service.rerank("rerank-id", "退款条件", List.of("候选一", "候选二"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMode()).isEqualTo("cross_encoder");
        assertThat(result.getScores()).containsExactly(0.52D, 0.91D);
        assertThat(result.getModelId()).isEqualTo("rerank-id");
    }

    /** 上游异常时应返回显式规则降级状态，不中断检索。 */
    @Test
    void shouldReturnFallbackResultWhenProviderFails() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/rerank", exchange -> {
            byte[] body = "{\"error\":\"temporary unavailable\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        CrossEncoderRerankService service = new CrossEncoderRerankService(
                providerService("http://localhost:" + server.getAddress().getPort() + "/v1"),
                new ObjectMapper(),
                HttpClient.newHttpClient()
        );

        RerankResult result = service.rerank("rerank-id", "问题", List.of("候选"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMode()).isEqualTo("rule_fallback");
        assertThat(result.getErrorMessage()).contains("HTTP 500");
    }

    /** 构造可返回指定端点和密钥的模型服务商依赖。 */
    private ModelProviderService providerService(String baseUrl) {
        ModelProviderService service = mock(ModelProviderService.class);
        ModelConfigEntity model = new ModelConfigEntity();
        model.setId("rerank-id");
        model.setModelCode("rerank-model");
        model.setProviderId("provider-1");
        ModelProviderEntity provider = new ModelProviderEntity();
        provider.setId("provider-1");
        provider.setBaseUrl(baseUrl);
        provider.setDefaultHeaders("{}");
        when(service.requireModel("rerank-id")).thenReturn(model);
        when(service.requireProviderByModel(model)).thenReturn(provider);
        when(service.findApiKeyValue("provider-1")).thenReturn("test-key");
        return service;
    }
}
