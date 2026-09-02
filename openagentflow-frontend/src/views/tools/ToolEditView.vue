<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ArrowLeft, Save, TestTube2, Trash2 } from 'lucide-vue-next';
import PageHeader from '../../components/PageHeader.vue';
import StatusBadge from '../../components/StatusBadge.vue';
import {
  createTool,
  deleteTool,
  fetchTool,
  testTool,
  updateTool,
  type ToolDefinitionRequest,
  type ToolExecutionResult,
} from '../../api/tools';
import { useOverlay } from '../../composables/useOverlay';

const route = useRoute();
const router = useRouter();
const { toast } = useOverlay();
const loading = ref(false);
const testing = ref(false);
const suppressTypeDefaults = ref(false);
const testInput = ref('{\n  "orderId": "10001"\n}');
const testResult = ref<ToolExecutionResult | null>(null);

const toolTypes = [
  { value: 'REST_API', label: 'REST API', detail: 'HTTP 接口' },
  { value: 'WEBHOOK', label: 'Webhook', detail: '事件回调' },
  { value: 'DB_QUERY', label: '数据库查询', detail: '只读 SELECT' },
  { value: 'MCP', label: 'MCP 工具', detail: 'Server 同步' },
];

const form = reactive({
  toolCode: '',
  toolName: '',
  toolType: 'REST_API',
  description: '',
  requestMethod: 'GET',
  endpointUrl: '',
  authType: 'none',
  authConfig: '{}',
  headers: '{}',
  requestSchema: '{\n  "type": "object",\n  "properties": {},\n  "required": []\n}',
  responseSchema: '{\n  "type": "object"\n}',
  intentCodesText: '',
  routingExamplesText: '',
  requiredEntitiesText: '',
  timeoutMs: 30000,
  retryCount: 0,
  riskLevel: 'low',
  requireConfirm: false,
  enabled: true,
  status: 'active',
  mcpServerId: '',
  mcpToolName: '',
});

const isNew = computed(() => route.params.id === 'new');
const pageTitle = computed(() => (isNew.value ? '新建工具' : form.toolName || '编辑工具'));
const pageDescription = computed(() => {
  if (form.toolType === 'WEBHOOK') return '配置 Webhook 地址、签名密钥、事件载荷示例与风险策略';
  if (form.toolType === 'DB_QUERY') return '配置只读 SQL 模板、参数映射、Schema 与测试查询';
  if (form.toolType === 'MCP') return '来自 MCP Server 的工具，统一复用工具测试、Agent 绑定、工作流节点和 Trace 日志';
  return '配置 REST API 请求方法、URL、Headers、认证、Schema 与风险策略';
});
const riskLabel = computed(() => {
  if (form.riskLevel === 'high') return '高风险';
  if (form.riskLevel === 'medium') return '中风险';
  return '低风险';
});
const endpointLabel = computed(() => {
  if (form.toolType === 'DB_QUERY') return 'SQL 查询模板';
  if (form.toolType === 'WEBHOOK') return 'Webhook URL';
  if (form.toolType === 'MCP') return 'MCP Server 端点';
  return 'API URL';
});
const endpointPlaceholder = computed(() => {
  if (form.toolType === 'DB_QUERY') return 'SELECT * FROM orders WHERE order_no = {orderId}';
  if (form.toolType === 'WEBHOOK') return 'https://example.com/webhooks/order-created';
  if (form.toolType === 'MCP') return '从 MCP Server 同步生成';
  return 'https://api.example.com/v1/orders/{orderId}';
});

onMounted(() => {
  if (!isNew.value) {
    void loadTool();
  } else {
    form.toolName = '查询订单状态';
    form.toolCode = 'query_order_status';
    form.endpointUrl = 'https://api.example.com/v1/orders/{orderId}';
    form.description = '查询订单配送状态、物流单号与预计送达时间。';
    form.requestSchema = '{\n  "type": "object",\n  "properties": {\n    "orderId": { "type": "string", "description": "订单号" }\n  },\n  "required": ["orderId"]\n}';
  }
});

watch(() => form.toolType, (toolType) => {
  if (!suppressTypeDefaults.value) {
    applyTypeDefaults(toolType);
  }
});

async function loadTool() {
  loading.value = true;
  suppressTypeDefaults.value = true;
  try {
    const detail = await fetchTool(String(route.params.id));
    form.toolCode = detail.toolCode;
    form.toolName = detail.toolName;
    form.toolType = detail.toolType;
    form.description = detail.description || '';
    form.requestMethod = detail.requestMethod || 'GET';
    form.endpointUrl = detail.endpointUrl || '';
    form.authType = detail.authType || 'none';
    form.authConfig = detail.authConfig || '{}';
    form.headers = detail.headers || '{}';
    form.requestSchema = detail.requestSchema || form.requestSchema;
    form.responseSchema = detail.responseSchema || form.responseSchema;
    form.intentCodesText = (detail.intentCodes || []).join('\n');
    form.routingExamplesText = (detail.routingExamples || []).join('\n');
    form.requiredEntitiesText = (detail.requiredEntities || []).join('\n');
    form.timeoutMs = detail.timeoutMs || 30000;
    form.retryCount = detail.retryCount || 0;
    form.riskLevel = detail.riskLevel || 'low';
    form.requireConfirm = detail.requireConfirm;
    form.enabled = detail.enabled;
    form.status = detail.status || 'active';
    form.mcpServerId = detail.mcpServerId || '';
    form.mcpToolName = detail.mcpToolName || '';
  } finally {
    suppressTypeDefaults.value = false;
    loading.value = false;
  }
}

async function handleSave(enable = form.enabled) {
  const payload = toRequest(enable);
  const saved = isNew.value
    ? await createTool(payload)
    : await updateTool(String(route.params.id), payload);
  toast('工具已保存');
  if (isNew.value) {
    router.replace(`/tools/${saved.id}`);
  } else {
    await loadTool();
  }
}

async function handleDelete() {
  if (isNew.value || !window.confirm(`确认删除工具「${form.toolName}」？`)) {
    return;
  }
  await deleteTool(String(route.params.id));
  toast('工具已删除');
  router.push('/tools');
}

async function handleTest() {
  if (isNew.value) {
    const saved = await createTool(toRequest(true));
    router.replace(`/tools/${saved.id}`);
    toast('工具已保存，开始测试');
    setTimeout(() => void handleTest(), 100);
    return;
  }
  testing.value = true;
  testResult.value = null;
  try {
    testResult.value = await testTool(String(route.params.id), parseJson(testInput.value));
  } catch (error) {
    testResult.value = {
      success: false,
      statusCode: 0,
      latencyMs: 0,
      errorMessage: error instanceof Error ? error.message : '工具测试失败',
    };
  } finally {
    testing.value = false;
  }
}

function toRequest(enabled: boolean): ToolDefinitionRequest {
  return {
    toolCode: form.toolCode,
    toolName: form.toolName,
    toolType: form.toolType,
    description: form.description,
    requestMethod: form.requestMethod,
    endpointUrl: form.endpointUrl,
    authType: form.authType,
    authConfig: normalizedJson(form.authConfig, '{}'),
    headers: normalizedJson(form.headers, '{}'),
    requestSchema: normalizedJson(form.requestSchema, '{"type":"object","properties":{}}'),
    responseSchema: normalizedJson(form.responseSchema, '{"type":"object"}'),
    intentCodes: parseLines(form.intentCodesText),
    routingExamples: parseLines(form.routingExamplesText),
    requiredEntities: parseLines(form.requiredEntitiesText),
    timeoutMs: Number(form.timeoutMs),
    retryCount: Number(form.retryCount),
    riskLevel: form.riskLevel,
    requireConfirm: form.requireConfirm || form.riskLevel === 'high',
    enabled,
    mcpServerId: form.mcpServerId || undefined,
    mcpToolName: form.mcpToolName || undefined,
    status: enabled ? 'active' : 'disabled',
  };
}

function applyTypeDefaults(toolType: string) {
  testResult.value = null;
  if (toolType === 'WEBHOOK') {
    form.requestMethod = 'POST';
    form.authType = form.authType === 'none' ? 'api_key' : form.authType;
    form.headers = '{\n  "Content-Type": "application/json",\n  "X-Webhook-Signature": "{{signature}}"\n}';
    form.authConfig = '{\n  "headerName": "X-Webhook-Signature",\n  "apiKey": "replace-with-signature-secret"\n}';
    form.requestSchema = '{\n  "type": "object",\n  "properties": {\n    "event": { "type": "string", "description": "事件名称" },\n    "payload": { "type": "object", "description": "事件载荷" }\n  },\n  "required": ["event", "payload"]\n}';
    testInput.value = '{\n  "event": "order.created",\n  "payload": {\n    "orderId": "10001"\n  }\n}';
  } else if (toolType === 'DB_QUERY') {
    form.requestMethod = 'GET';
    form.authType = 'none';
    form.headers = '{}';
    form.authConfig = '{}';
    form.endpointUrl = form.endpointUrl.startsWith('http') ? 'SELECT id, order_no, status FROM demo_order WHERE order_no = {orderId}' : form.endpointUrl;
    form.requestSchema = '{\n  "type": "object",\n  "properties": {\n    "orderId": { "type": "string", "description": "订单号" }\n  },\n  "required": ["orderId"]\n}';
    testInput.value = '{\n  "orderId": "10001"\n}';
  } else if (toolType === 'MCP') {
    form.requestMethod = 'MCP';
    form.authType = 'none';
    form.headers = '{}';
    form.authConfig = '{}';
    form.requestSchema = form.requestSchema || '{\n  "type": "object",\n  "properties": {}\n}';
    testInput.value = '{}';
  } else {
    form.requestMethod = form.requestMethod === 'GET' ? 'GET' : 'POST';
    form.authType = 'none';
    form.headers = '{}';
    form.authConfig = '{}';
    form.endpointUrl = form.endpointUrl.toLowerCase().startsWith('select ') ? 'https://api.example.com/v1/orders/{orderId}' : form.endpointUrl;
    form.requestSchema = '{\n  "type": "object",\n  "properties": {\n    "orderId": { "type": "string", "description": "订单号" }\n  },\n  "required": ["orderId"]\n}';
    testInput.value = '{\n  "orderId": "10001"\n}';
  }
}

function parseJson(text: string) {
  try {
    return JSON.parse(text || '{}') as Record<string, unknown>;
  } catch {
    toast('测试入参不是合法 JSON');
    return {};
  }
}

function normalizedJson(text: string, fallback: string) {
  try {
    return JSON.stringify(JSON.parse(text || fallback), null, 2);
  } catch {
    return fallback;
  }
}

function parseLines(text: string) {
  return text
    .split(/\r?\n/)
    .map((item) => item.trim())
    .filter(Boolean);
}
</script>

<template>
  <PageHeader :title="pageTitle" :description="pageDescription">
    <template #actions>
      <label class="inline-switch"><span class="switch" :class="{ on: form.enabled }" /> {{ form.enabled ? '启用' : '停用' }}</label>
    </template>
  </PageHeader>

  <section class="form-layout">
    <div class="section-block">
      <div class="section-title"><h2>基本信息</h2><StatusBadge :label="riskLabel" /></div>
      <div class="form-grid">
        <label>工具名称<input v-model="form.toolName" placeholder="请输入工具名称" /></label>
        <label>Code<input v-model="form.toolCode" class="mono" placeholder="模型调用函数名，建议英文下划线" /></label>
        <label>风险等级<select v-model="form.riskLevel"><option value="low">低风险</option><option value="medium">中风险</option><option value="high">高风险</option></select></label>
        <label>需要确认<select v-model="form.requireConfirm"><option :value="false">否</option><option :value="true">是</option></select></label>
        <label>状态<select v-model="form.enabled"><option :value="true">启用</option><option :value="false">停用</option></select></label>
        <label class="wide">描述<textarea v-model="form.description" placeholder="描述越清晰，模型越容易正确选择工具" /></label>
        <label class="wide">意图编码（每行一个）<textarea v-model="form.intentCodesText" class="compact-textarea" placeholder="order.query\norder.refund" /></label>
        <label class="wide">路由示例（每行一个）<textarea v-model="form.routingExamplesText" class="compact-textarea" placeholder="查询订单物流\n客户要退款" /></label>
        <label class="wide">必填实体（每行一个）<textarea v-model="form.requiredEntitiesText" class="compact-textarea" placeholder="orderId" /></label>
      </div>
    </div>

    <div class="section-block">
      <div class="section-title"><h2>工具类型</h2></div>
      <div class="tool-type-grid">
        <button
          v-for="type in toolTypes"
          :key="type.value"
          class="tool-type-option"
          :class="{ active: form.toolType === type.value }"
          type="button"
          @click="form.toolType = type.value"
        >
          <b>{{ type.label }}</b>
          <span>{{ type.detail }}</span>
        </button>
      </div>
    </div>

    <div class="section-block">
      <div class="section-title"><h2>{{ form.toolType === 'DB_QUERY' ? '查询配置' : form.toolType === 'WEBHOOK' ? 'Webhook 配置' : form.toolType === 'MCP' ? 'MCP 配置' : '请求配置' }}</h2></div>
      <div class="form-grid">
        <label v-if="form.toolType !== 'DB_QUERY' && form.toolType !== 'MCP'">请求方法<select v-model="form.requestMethod"><option>GET</option><option>POST</option><option>PUT</option><option>DELETE</option></select></label>
        <label v-if="form.toolType !== 'DB_QUERY' && form.toolType !== 'MCP'">认证方式<select v-model="form.authType"><option value="none">无认证</option><option value="bearer">Bearer Token</option><option value="api_key">API Key</option><option value="basic">Basic</option></select></label>
        <label>超时毫秒<input v-model.number="form.timeoutMs" type="number" min="1000" step="1000" /></label>
        <label>重试次数<input v-model.number="form.retryCount" type="number" min="0" max="3" /></label>
        <label class="wide">{{ endpointLabel }}<input v-model="form.endpointUrl" :placeholder="endpointPlaceholder" /></label>
        <template v-if="form.toolType === 'REST_API'">
          <label class="wide">请求头 JSON<textarea v-model="form.headers" class="mono" /></label>
          <label class="wide">认证配置 JSON<textarea v-model="form.authConfig" class="mono" /></label>
        </template>
        <template v-else-if="form.toolType === 'WEBHOOK'">
          <label class="wide">签名请求头 JSON<textarea v-model="form.headers" class="mono" /></label>
          <label class="wide">签名密钥配置 JSON<textarea v-model="form.authConfig" class="mono" /></label>
        </template>
        <template v-else-if="form.toolType === 'DB_QUERY'">
          <div class="wide type-note danger-note">
            <b>只读限制</b>
            <span>数据库查询工具仅允许单条 SELECT 语句，测试和 Agent 调用都会阻止写入、删除和多语句执行。</span>
          </div>
        </template>
        <template v-else>
          <label>MCP Server ID<input v-model="form.mcpServerId" class="mono" readonly /></label>
          <label>MCP 工具名<input v-model="form.mcpToolName" class="mono" readonly /></label>
          <div class="wide type-note danger-note">
            <b>安全策略</b>
            <span>高风险 MCP 工具默认停用并需要确认，启用后仍会写入工具调用日志和 Trace。</span>
          </div>
        </template>
      </div>
    </div>
  </section>

  <section class="detail-columns">
    <div class="section-block">
      <div class="section-title"><h2>{{ form.toolType === 'DB_QUERY' ? '参数映射 Schema' : form.toolType === 'WEBHOOK' ? '事件载荷 Schema' : '请求参数 Schema' }}</h2></div>
      <textarea v-model="form.requestSchema" class="code-editor" />
    </div>
    <div class="section-block">
      <div class="section-title"><h2>{{ form.toolType === 'DB_QUERY' ? '查询结果 Schema' : '响应 Schema' }}</h2></div>
      <textarea v-model="form.responseSchema" class="code-editor" />
    </div>
  </section>

  <section class="detail-columns">
    <div class="section-block">
      <div class="section-title"><h2>{{ form.toolType === 'WEBHOOK' ? '事件载荷示例' : form.toolType === 'DB_QUERY' ? '测试参数' : '测试入参' }}</h2></div>
      <textarea v-model="testInput" class="code-editor compact" />
      <button class="secondary-button" type="button" :disabled="testing" @click="handleTest"><TestTube2 :size="16" /> {{ testing ? '测试中' : '测试工具' }}</button>
    </div>
    <div class="section-block">
      <div class="section-title"><h2>{{ form.toolType === 'DB_QUERY' ? '测试查询结果' : '测试结果' }}</h2><StatusBadge v-if="testResult" :label="testResult.success ? '成功' : '失败'" /></div>
      <div v-if="!testResult" class="empty-state">尚未执行测试</div>
      <pre v-else class="code-block light">statusCode: {{ testResult.statusCode }}
latencyMs: {{ testResult.latencyMs }}
confirmationRequired: {{ testResult.confirmationRequired ? 'true' : 'false' }}
error: {{ testResult.errorMessage || '-' }}

{{ testResult.responseBody || '' }}</pre>
    </div>
  </section>

  <div class="sticky-actions">
    <button class="secondary-button" type="button" @click="router.push('/tools')"><ArrowLeft :size="16" /> 返回</button>
    <button v-if="!isNew" class="danger-button" type="button" @click="handleDelete"><Trash2 :size="16" /> 删除</button>
    <button class="secondary-button" type="button" @click="handleSave(false)">保存草稿</button>
    <button class="primary-button" type="button" @click="handleSave(true)"><Save :size="16" /> 保存并启用</button>
  </div>
</template>
