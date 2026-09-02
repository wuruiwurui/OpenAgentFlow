<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { Copy, Eye, Rocket, Save, TestTube2, Trash2 } from 'lucide-vue-next';
import PageHeader from '../../components/PageHeader.vue';
import PaginationBar from '../../components/PaginationBar.vue';
import RuntimeInterpreter from '../../components/RuntimeInterpreter.vue';
import StatusBadge from '../../components/StatusBadge.vue';
import {
  copyAgent,
  createAgent,
  deleteAgent,
  fetchAgent,
  publishAgent,
  updateAgent,
  type AgentDetail,
  type AgentRequest,
} from '../../api/agents';
import {
  fetchAgentKnowledgeBindings,
  fetchKnowledgeBases,
  saveAgentKnowledgeBindings,
  type AgentKnowledgeBindingSummary,
  type AgentKnowledgeBindingOptions,
  type KnowledgeBaseSummary,
} from '../../api/knowledge';
import {
  fetchAgentToolBindings,
  fetchTools,
  saveAgentToolBindings,
  type AgentToolBindingSummary,
  type ToolDefinitionSummary,
} from '../../api/tools';
import {
  fetchAgentWorkflowBindings,
  fetchWorkflows,
  saveAgentWorkflowBindings,
  type AgentWorkflowBindingSummary,
  type WorkflowSummary,
} from '../../api/workflows';
import { fetchChatModels, type ModelConfigSummary } from '../../api/models';
import { fetchPromptTemplate, fetchPromptTemplates, type PromptTemplateDetail, type PromptTemplateSummary } from '../../api/prompts';
import { useOverlay } from '../../composables/useOverlay';
import { usePagination } from '../../composables/usePagination';

const route = useRoute();
const router = useRouter();
const { toast } = useOverlay();
const tabs = ['基础信息', '模型参数', 'Prompt 配置', '知识库绑定', '工具绑定', '工作流绑定', '安全策略'];
const activeTab = ref('基础信息');
const loading = ref(false);
const models = ref<ModelConfigSummary[]>([]);
const promptTemplates = ref<PromptTemplateSummary[]>([]);
const selectedPromptDetail = ref<PromptTemplateDetail | null>(null);
const currentAgent = ref<AgentDetail | null>(null);
const knowledgeBases = ref<KnowledgeBaseSummary[]>([]);
const knowledgeBindings = ref<AgentKnowledgeBindingSummary[]>([]);
const selectedKnowledgeBaseIds = ref<string[]>([]);
const tools = ref<ToolDefinitionSummary[]>([]);
const toolBindings = ref<AgentToolBindingSummary[]>([]);
const selectedToolIds = ref<string[]>([]);
const workflows = ref<WorkflowSummary[]>([]);
const workflowBindings = ref<AgentWorkflowBindingSummary[]>([]);
const selectedWorkflowIds = ref<string[]>([]);
const { currentPage: kbPage, pagedItems: pagedKnowledgeBases } = usePagination(knowledgeBases);
const { currentPage: toolPage, pagedItems: pagedTools } = usePagination(tools);
const { currentPage: workflowPage, pagedItems: pagedWorkflows } = usePagination(workflows);

const form = reactive({
  agentCode: '',
  agentName: '',
  category: '通用',
  description: '',
  agentType: 'chat_agent',
  modelId: '',
  systemPromptTemplateId: '',
  systemPromptVersionId: '',
  promptBindingMode: 'MANUAL' as 'MANUAL' | 'LOCKED' | 'FOLLOW_STABLE',
  promptVariables: '{}',
  systemPrompt: '你是 OpenAgentFlow-Java 的智能体，请使用清晰、准确的中文回答用户。',
  temperature: 0.3,
  maxTokens: 2048,
  memoryStrategy: 'none',
  visibility: 'private',
  status: 'draft',
});

const knowledgePolicy = reactive<Required<AgentKnowledgeBindingOptions>>({
  topK: 5,
  scoreThreshold: 0.65,
  lowConfidenceThreshold: 0.72,
  trustedAnswerMode: true,
  citationRequired: true,
  minCitationCount: 1,
  queryRewriteEnabled: true,
  multiQueryEnabled: true,
  maxQueryVariants: 4,
});

const isNew = computed(() => route.params.id === 'new');
const selectedPromptVersions = computed(() => selectedPromptDetail.value?.versions || []);
const pageTitle = computed(() => (isNew.value ? '新建智能体' : form.agentName || '智能体详情'));
const pageDescription = computed(() => form.description || '配置 Prompt、模型参数、知识库、工具、工作流与运行权限');
const statusLabel = computed(() => currentAgent.value?.statusLabel || statusText(form.status));
const agentRuntimePhases = computed(() => [
  {
    id: 'entry',
    label: '入口识别',
    status: form.agentName ? 'success' : 'warning',
    summary: form.agentName || '待命名',
    reason: form.agentName ? 'Agent 已具备可运行身份，调试台会按该 Agent 创建运行链路。' : '请先填写智能体名称。',
    metric: form.agentType,
  },
  {
    id: 'model',
    label: '模型路由',
    status: form.modelId ? 'success' : 'warning',
    summary: models.value.find((model) => model.id === form.modelId)?.modelName || '未绑定',
    reason: form.modelId ? '运行时会优先使用当前绑定模型，并受模型网关策略影响。' : '未选择模型时无法进行真实生成。',
    metric: `Temperature ${Number(form.temperature).toFixed(2)} · Max ${form.maxTokens}`,
  },
  {
    id: 'prompt',
    label: 'Prompt 装配',
    status: form.systemPrompt.trim() ? 'success' : 'warning',
    summary: form.systemPromptTemplateId ? '模板驱动' : '手动维护',
    reason: form.systemPrompt.trim() ? 'System Prompt 会作为 Runtime 首个系统指令进入模型上下文。' : '缺少 System Prompt 时回答风格和边界会不稳定。',
    metric: `${form.systemPrompt.length} 字符`,
  },
  {
    id: 'memory',
    label: 'Memory',
    status: form.memoryStrategy === 'none' ? 'neutral' : 'success',
    summary: form.memoryStrategy === 'none' ? '未启用' : form.memoryStrategy,
    reason: form.memoryStrategy === 'none' ? '本 Agent 不会自动召回记忆。' : '运行时会召回相关记忆并注入上下文。',
  },
  {
    id: 'rag',
    label: 'RAG 证据',
    status: selectedKnowledgeBaseIds.value.length > 0 ? 'success' : 'neutral',
    summary: `${selectedKnowledgeBaseIds.value.length} 个知识库`,
    reason: selectedKnowledgeBaseIds.value.length > 0
      ? '运行时会先检索绑定知识库，再将可靠来源注入模型上下文。'
      : '未绑定知识库时不会触发企业知识检索。',
    metric: knowledgePolicy.trustedAnswerMode ? `可信模式 · 最少 ${knowledgePolicy.minCitationCount} 条引用` : '普通模式',
    evidence: knowledgeBases.value
      .filter((kb) => selectedKnowledgeBaseIds.value.includes(kb.id))
      .slice(0, 3)
      .map((kb) => kb.kbName),
  },
  {
    id: 'tool',
    label: '工具动作',
    status: selectedToolIds.value.length > 0 ? 'success' : 'neutral',
    summary: `${selectedToolIds.value.length} 个工具`,
    reason: selectedToolIds.value.length > 0 ? '模型可在需要外部动作时选择绑定工具。' : '未绑定工具时只执行对话和知识检索。',
    evidence: tools.value.filter((tool) => selectedToolIds.value.includes(tool.id)).slice(0, 3).map((tool) => tool.toolName),
  },
  {
    id: 'workflow',
    label: '工作流',
    status: selectedWorkflowIds.value.length > 0 ? 'success' : 'neutral',
    summary: `${selectedWorkflowIds.value.length} 个工作流`,
    reason: selectedWorkflowIds.value.length > 0 ? '调试或运行时可优先进入绑定工作流。' : '未绑定工作流时按 Agent 对话链路运行。',
    evidence: workflows.value.filter((workflow) => selectedWorkflowIds.value.includes(workflow.id)).slice(0, 3).map((workflow) => workflow.workflowName),
  },
  {
    id: 'governance',
    label: '治理边界',
    status: form.status === 'disabled' ? 'warning' : 'success',
    summary: `${statusText(form.status)} · ${form.visibility}`,
    reason: form.status === 'disabled' ? '当前 Agent 已暂停，运行入口应被限制。' : '运行权限、可见范围和资源绑定共同决定谁能调用该 Agent。',
    metric: knowledgePolicy.citationRequired ? 'RAG 要求引用' : 'RAG 不强制引用',
  },
] as const);

onMounted(async () => {
  await Promise.all([loadModels(), loadPromptTemplates(), loadKnowledgeBases(), loadTools(), loadWorkflows(), loadAgent()]);
});

async function loadModels() {
  models.value = await fetchChatModels();
  if (!form.modelId && models.value.length > 0) {
    form.modelId = models.value[0].id;
  }
}

async function loadAgent() {
  if (isNew.value) {
    return;
  }
  loading.value = true;
  try {
    const detail = await fetchAgent(String(route.params.id));
    currentAgent.value = detail;
    fillForm(detail);
    if (detail.systemPromptTemplateId) {
      selectedPromptDetail.value = await fetchPromptTemplate(detail.systemPromptTemplateId);
    }
    await Promise.all([loadKnowledgeBindings(detail.id), loadToolBindings(detail.id), loadWorkflowBindings(detail.id)]);
  } finally {
    loading.value = false;
  }
}

async function loadPromptTemplates() {
  const result = await fetchPromptTemplates({
    promptType: 'system',
    status: 'published',
    pageNo: 1,
    pageSize: 100,
  });
  promptTemplates.value = result.records;
}

async function loadKnowledgeBases() {
  knowledgeBases.value = await fetchKnowledgeBases();
}

async function loadKnowledgeBindings(agentId: string) {
  knowledgeBindings.value = await fetchAgentKnowledgeBindings(agentId);
  selectedKnowledgeBaseIds.value = knowledgeBindings.value.map((binding) => binding.knowledgeBaseId);
  applyKnowledgePolicy(knowledgeBindings.value[0]?.retrievalConfig);
}

async function loadTools() {
  tools.value = await fetchTools();
}

async function loadToolBindings(agentId: string) {
  toolBindings.value = await fetchAgentToolBindings(agentId);
  selectedToolIds.value = toolBindings.value.map((binding) => binding.toolId);
}

async function loadWorkflows() {
  workflows.value = await fetchWorkflows();
}

async function loadWorkflowBindings(agentId: string) {
  workflowBindings.value = await fetchAgentWorkflowBindings(agentId);
  selectedWorkflowIds.value = workflowBindings.value.map((binding) => binding.workflowId);
}

function fillForm(detail: AgentDetail) {
  form.agentCode = detail.agentCode;
  form.agentName = detail.agentName;
  form.category = detail.category || '通用';
  form.description = detail.description || '';
  form.agentType = detail.agentType || 'chat_agent';
  form.modelId = detail.modelId || form.modelId;
  form.systemPromptTemplateId = detail.systemPromptTemplateId || '';
  form.systemPromptVersionId = detail.systemPromptVersionId || '';
  form.promptBindingMode = detail.promptBindingMode || (detail.systemPromptTemplateId ? 'FOLLOW_STABLE' : 'MANUAL');
  form.promptVariables = detail.promptVariables || '{}';
  form.systemPrompt = detail.systemPrompt || form.systemPrompt;
  form.memoryStrategy = detail.memoryStrategy || 'none';
  form.visibility = detail.visibility || 'private';
  form.status = detail.status || 'draft';

  try {
    const params = JSON.parse(detail.modelParams || '{}');
    form.temperature = Number(params.temperature ?? form.temperature);
    form.maxTokens = Number(params.maxTokens ?? params.max_tokens ?? form.maxTokens);
  } catch {
    form.temperature = 0.3;
    form.maxTokens = 2048;
  }
}

async function handleSave() {
  const payload = toRequest();
  if (isNew.value) {
    const created = await createAgent(payload);
    await saveBindings(created.id);
    toast('智能体已创建');
    router.replace(`/agents/${created.id}`);
    currentAgent.value = created;
    fillForm(created);
    await Promise.all([loadKnowledgeBindings(created.id), loadToolBindings(created.id), loadWorkflowBindings(created.id)]);
    return;
  }
  const updated = await updateAgent(String(route.params.id), payload);
  await saveBindings(updated.id);
  currentAgent.value = updated;
  fillForm(updated);
  await Promise.all([loadKnowledgeBindings(updated.id), loadToolBindings(updated.id), loadWorkflowBindings(updated.id)]);
  toast('智能体配置已保存');
}

async function saveBindings(agentId: string) {
  await Promise.all([
    saveAgentKnowledgeBindings(agentId, selectedKnowledgeBaseIds.value, { ...knowledgePolicy }),
    saveAgentToolBindings(agentId, selectedToolIds.value),
    saveAgentWorkflowBindings(agentId, selectedWorkflowIds.value),
  ]);
}

async function handlePublish() {
  if (isNew.value) {
    await handleSave();
  }
  const id = String(route.params.id === 'new' ? currentAgent.value?.id : route.params.id);
  if (!id || id === 'undefined') {
    return;
  }
  const published = await publishAgent(id, {
    versionNo: `v${new Date().toISOString().slice(0, 19).replace(/[-:T]/g, '')}`,
    publishNote: '通过管理台发布',
  });
  currentAgent.value = published;
  fillForm(published);
  toast('智能体已发布');
}

async function handleCopy() {
  if (!currentAgent.value) {
    return;
  }
  const copied = await copyAgent(currentAgent.value.id);
  toast('智能体已复制');
  router.push(`/agents/${copied.id}`);
}

async function handleDelete() {
  if (!currentAgent.value || !window.confirm('确认删除该智能体？')) {
    return;
  }
  await deleteAgent(currentAgent.value.id);
  toast('智能体已删除');
  router.push('/agents');
}

function goDebug() {
  const id = currentAgent.value?.id || String(route.params.id);
  router.push({ path: '/debug', query: { agentId: id, modelId: form.modelId } });
}

function toRequest(): AgentRequest {
  return {
    agentCode: form.agentCode,
    agentName: form.agentName,
    category: form.category,
    description: form.description,
    agentType: form.agentType,
    modelId: form.modelId,
    systemPromptTemplateId: form.systemPromptTemplateId || undefined,
    systemPromptVersionId: form.promptBindingMode === 'LOCKED' ? form.systemPromptVersionId || undefined : undefined,
    promptBindingMode: form.systemPromptTemplateId ? form.promptBindingMode : 'MANUAL',
    promptVariables: form.promptVariables || '{}',
    systemPrompt: form.systemPrompt,
    modelParams: JSON.stringify({
      temperature: Number(form.temperature),
      maxTokens: Number(form.maxTokens),
    }),
    memoryStrategy: form.memoryStrategy,
    visibility: form.visibility,
    status: form.status,
  };
}

async function applySystemPromptTemplate() {
  const template = promptTemplates.value.find((item) => item.id === form.systemPromptTemplateId);
  if (template) {
    selectedPromptDetail.value = await fetchPromptTemplate(template.id);
    form.systemPrompt = template.content;
    form.promptBindingMode = template.stableVersionId ? 'FOLLOW_STABLE' : 'LOCKED';
    form.systemPromptVersionId = template.stableVersionId || selectedPromptDetail.value.versions?.[0]?.id || '';
  } else {
    selectedPromptDetail.value = null;
    form.promptBindingMode = 'MANUAL';
    form.systemPromptVersionId = '';
  }
}

function applyKnowledgePolicy(config?: string) {
  if (!config) {
    return;
  }
  try {
    const parsed = JSON.parse(config) as Partial<AgentKnowledgeBindingOptions>;
    knowledgePolicy.topK = Number(parsed.topK ?? knowledgePolicy.topK);
    knowledgePolicy.scoreThreshold = Number(parsed.scoreThreshold ?? knowledgePolicy.scoreThreshold);
    knowledgePolicy.lowConfidenceThreshold = Number(parsed.lowConfidenceThreshold ?? knowledgePolicy.lowConfidenceThreshold);
    knowledgePolicy.trustedAnswerMode = parsed.trustedAnswerMode !== false;
    knowledgePolicy.citationRequired = parsed.citationRequired !== false;
    knowledgePolicy.minCitationCount = Number(parsed.minCitationCount ?? knowledgePolicy.minCitationCount);
  } catch {
    // 旧数据解析失败时继续使用默认可信回答策略。
  }
}

function statusText(status: string) {
  if (status === 'published') return '运行中';
  if (status === 'draft') return '开发中';
  if (status === 'disabled') return '已暂停';
  return status || '未知';
}
</script>

<template>
  <PageHeader :title="pageTitle" :description="pageDescription">
    <template #actions>
      <button class="secondary-button" type="button"><Eye :size="16" /> 预览</button>
      <button class="secondary-button" type="button" :disabled="isNew" @click="goDebug"><TestTube2 :size="16" /> 调试</button>
      <button class="secondary-button" type="button" :disabled="isNew" @click="handleCopy"><Copy :size="16" /> 复制</button>
      <button class="secondary-button" type="button" @click="handlePublish"><Rocket :size="16" /> 发布</button>
      <button class="primary-button" type="button" @click="handleSave"><Save :size="16" /> 保存</button>
      <button v-if="!isNew" class="danger-button" type="button" @click="handleDelete"><Trash2 :size="16" /> 删除</button>
    </template>
  </PageHeader>

  <div class="tabs">
    <button v-for="tab in tabs" :key="tab" class="tab" :class="{ active: activeTab === tab }" type="button" @click="activeTab = tab">{{ tab }}</button>
  </div>

  <section v-if="activeTab === '基础信息'" class="form-layout">
    <RuntimeInterpreter title="Agent Runtime 策略解释器" :phases="agentRuntimePhases" compact />
    <div class="section-block">
      <div class="section-title"><h2>基础信息</h2><StatusBadge :label="statusLabel" /></div>
      <div class="form-grid">
        <label>智能体名称<input v-model="form.agentName" /></label>
        <label>分类<select v-model="form.category"><option>通用</option><option>客服</option><option>知识问答</option><option>数据分析</option><option>运维</option></select></label>
        <label class="wide">描述<textarea v-model="form.description" /></label>
        <label>编码<input v-model="form.agentCode" placeholder="不填则自动生成" /></label>
        <label>状态<select v-model="form.status"><option value="draft">开发中</option><option value="published">运行中</option><option value="disabled">已暂停</option></select></label>
      </div>
    </div>
  </section>

  <section v-else-if="activeTab === '模型参数'" class="form-layout">
    <div class="section-block">
      <div class="section-title"><h2>模型配置</h2><span>OpenAI-compatible / Ollama / Qwen / DeepSeek</span></div>
      <div class="form-grid">
        <label>Agent 类型<select v-model="form.agentType"><option value="chat_agent">对话 Agent</option><option value="rag_tool_agent">RAG 工具 Agent</option><option value="workflow_agent">工作流 Agent</option></select></label>
        <label>基础模型<select v-model="form.modelId"><option v-for="model in models" :key="model.id" :value="model.id">{{ model.providerName }} / {{ model.modelName }}</option></select></label>
        <label>Temperature<input v-model.number="form.temperature" type="range" min="0" max="2" step="0.01" /></label>
        <label>最大 Tokens<input v-model.number="form.maxTokens" type="range" min="256" max="8192" step="128" /></label>
      </div>
    </div>
  </section>

  <section v-else-if="activeTab === 'Prompt 配置'" class="form-layout">
    <div class="section-block">
      <div class="section-title"><h2>Prompt 配置</h2><span>可绑定 Prompt 模板，也可手动调整当前 Agent 的 System Prompt</span></div>
      <div class="form-grid">
        <label>System Prompt 模板
          <select v-model="form.systemPromptTemplateId" @change="applySystemPromptTemplate">
            <option value="">不使用模板</option>
            <option v-for="template in promptTemplates" :key="template.id" :value="template.id">
              {{ template.templateName }} / {{ template.latestVersionNo || '未发布版本' }}
            </option>
          </select>
        </label>
        <label>版本绑定模式
          <select v-model="form.promptBindingMode" :disabled="!form.systemPromptTemplateId">
            <option value="MANUAL">手工 Prompt</option>
            <option value="LOCKED">锁定指定版本</option>
            <option value="FOLLOW_STABLE">跟随生产稳定版</option>
          </select>
        </label>
        <label v-if="form.promptBindingMode === 'LOCKED'">锁定版本
          <select v-model="form.systemPromptVersionId">
            <option value="">请选择版本</option>
            <option v-for="version in selectedPromptVersions" :key="version.id" :value="version.id">
              {{ version.versionNo }} / {{ version.environment || 'development' }}
            </option>
          </select>
        </label>
        <label class="wide">Prompt 变量 JSON
          <textarea v-model="form.promptVariables" class="code-editor compact" rows="4" placeholder='{"tenant_name":"示例企业"}' />
        </label>
        <label class="wide">System Prompt<textarea v-model="form.systemPrompt" /></label>
      </div>
    </div>
  </section>

  <section v-else-if="activeTab === '知识库绑定'" class="agent-binding-panel">
    <div class="section-block">
      <div class="section-title"><h2>已绑定知识库</h2><span>{{ selectedKnowledgeBaseIds.length }} 个</span></div>
      <div class="trusted-rag-policy">
        <label class="checkbox-line"><input v-model="knowledgePolicy.trustedAnswerMode" type="checkbox" /> 可信回答模式</label>
        <label class="checkbox-line"><input v-model="knowledgePolicy.citationRequired" type="checkbox" /> 答案必须带引用</label>
        <label class="inline-field">TopK<input v-model.number="knowledgePolicy.topK" type="number" min="1" max="20" /></label>
        <label class="inline-field">相似度阈值<input v-model.number="knowledgePolicy.scoreThreshold" type="number" min="0" max="1" step="0.01" /></label>
        <label class="inline-field">低置信阈值<input v-model.number="knowledgePolicy.lowConfidenceThreshold" type="number" min="0" max="1" step="0.01" /></label>
        <label class="inline-field">最少引用<input v-model.number="knowledgePolicy.minCitationCount" type="number" min="1" max="5" /></label>
      </div>
      <div v-if="knowledgeBases.length === 0" class="empty-state">暂无知识库，请先在知识库模块创建并上传文档</div>
      <template v-else>
        <div class="agent-binding-list">
          <div v-for="kb in pagedKnowledgeBases" :key="kb.id" class="list-row agent-binding-row">
            <label class="checkbox-row">
              <input v-model="selectedKnowledgeBaseIds" type="checkbox" :value="kb.id" />
              <b>{{ kb.kbName }}</b>
            </label>
            <span>{{ kb.documentCount }} 文档 · {{ kb.chunkCount }} 分片</span>
            <StatusBadge :label="selectedKnowledgeBaseIds.includes(kb.id) ? '已启用' : '未绑定'" />
          </div>
        </div>
        <PaginationBar v-model:page="kbPage" :total="knowledgeBases.length" />
      </template>
    </div>
  </section>

  <section v-else-if="activeTab === '工具绑定'" class="agent-binding-panel">
    <div class="section-block">
      <div class="section-title"><h2>已绑定工具</h2><span>{{ selectedToolIds.length }} 个</span></div>
      <div v-if="tools.length === 0" class="empty-state">暂无工具，请先在工具中心创建 REST API、Webhook 或数据库查询工具</div>
      <template v-else>
        <div class="agent-binding-list">
          <div v-for="tool in pagedTools" :key="tool.id" class="list-row agent-binding-row">
            <label class="checkbox-row">
              <input v-model="selectedToolIds" type="checkbox" :value="tool.id" />
              <b>{{ tool.toolName }}</b>
            </label>
            <span class="mono">{{ tool.toolCode }}</span>
            <StatusBadge :label="selectedToolIds.includes(tool.id) ? '已启用' : tool.riskLabel" :tone="tool.riskLevel === 'high' ? 'danger' : tool.riskLevel === 'medium' ? 'warning' : undefined" />
          </div>
        </div>
        <PaginationBar v-model:page="toolPage" :total="tools.length" />
      </template>
    </div>
  </section>

  <section v-else-if="activeTab === '工作流绑定'" class="agent-binding-panel">
    <div class="section-block">
      <div class="section-title"><h2>已绑定工作流</h2><span>{{ selectedWorkflowIds.length }} 个</span></div>
      <div v-if="workflows.length === 0" class="empty-state">暂无工作流，请先在工作流编排中创建并发布</div>
      <template v-else>
        <div class="agent-binding-list">
          <div v-for="workflow in pagedWorkflows" :key="workflow.id" class="list-row agent-binding-row">
            <label class="checkbox-row">
              <input v-model="selectedWorkflowIds" type="checkbox" :value="workflow.id" />
              <b>{{ workflow.workflowName }}</b>
            </label>
            <span class="mono">{{ workflow.workflowCode }}</span>
            <StatusBadge :label="selectedWorkflowIds.includes(workflow.id) ? '调试时优先运行' : workflow.statusLabel" />
          </div>
        </div>
        <PaginationBar v-model:page="workflowPage" :total="workflows.length" />
      </template>
    </div>
  </section>

  <section v-else class="form-layout">
    <div class="section-block">
      <div class="section-title"><h2>安全策略</h2><span>会话记忆、可见范围和发布状态</span></div>
      <div class="form-grid">
        <label>记忆策略
          <select v-model="form.memoryStrategy">
            <option value="none">不启用记忆</option>
            <option value="short_term">短期会话记忆</option>
            <option value="long_term">长期记忆</option>
          </select>
        </label>
        <label>可见范围
          <select v-model="form.visibility">
            <option value="private">仅自己可见</option>
            <option value="team">团队可见</option>
            <option value="public">公开可见</option>
          </select>
        </label>
        <label>运行状态
          <select v-model="form.status">
            <option value="draft">开发中</option>
            <option value="published">运行中</option>
            <option value="disabled">已暂停</option>
          </select>
        </label>
      </div>
    </div>
  </section>
</template>
