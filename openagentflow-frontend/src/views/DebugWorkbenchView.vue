<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { AlertCircle, CornerDownRight, FileSearch, History, MessageSquarePlus, Send, Sparkles, Square, Trash2 } from 'lucide-vue-next';
import PageHeader from '../components/PageHeader.vue';
import RuntimeInterpreter from '../components/RuntimeInterpreter.vue';
import StatusBadge from '../components/StatusBadge.vue';
import { fetchAgents, streamAgent, type AgentSummary } from '../api/agents';
import { streamChat, type ChatMessage, type TrustedAnswerStatus } from '../api/chat';
import type { KnowledgeSource } from '../api/knowledge';
import type { MemoryRecallItem } from '../api/memories';
import { fetchChatModels, type ModelConfigSummary } from '../api/models';
import {
  deleteAgentSession,
  fetchAgentSessionMessages,
  fetchAgentSessions,
  type AgentSessionSummary,
} from '../api/sessions';
import { useOverlay } from '../composables/useOverlay';
import { scrollToLatest } from '../utils/chatScroll';

interface UiMessage {
  role: 'user' | 'assistant';
  content: string;
  status?: string;
}

type EvidencePanel = 'sources' | 'tools' | 'stats';

const route = useRoute();
const router = useRouter();
const { showModal, showDrawer } = useOverlay();

const agents = ref<AgentSummary[]>([]);
const models = ref<ModelConfigSummary[]>([]);
const selectedAgentId = ref('');
const selectedModelId = ref('');
const selectedSessionId = ref('');
const inputText = ref('请介绍一下 OpenAgentFlow-Java 当前平台能力，并给出下一步建设建议。');
const messages = ref<UiMessage[]>([]);
/** 中间对话消息容器，用于打开历史会话后定位到最新消息。 */
const chatMessagesRef = ref<HTMLElement | null>(null);
const sessions = ref<AgentSessionSummary[]>([]);
const sessionsLoading = ref(false);
const loading = ref(false);
const streamAbortController = ref<AbortController | null>(null);
const activeAssistantMessage = ref<UiMessage | null>(null);
const generationPaused = ref(false);
const errorMessage = ref('');
const runMeta = ref<Record<string, unknown>>({});
const runDone = ref<Record<string, unknown>>({});
const retrievalSources = ref<KnowledgeSource[]>([]);
const memoryResults = ref<MemoryRecallItem[]>([]);
const toolResults = ref<Record<string, unknown>[]>([]);
const trustedAnswer = ref<TrustedAnswerStatus | null>(null);
const activeEvidencePanel = ref<EvidencePanel>('sources');

const selectedAgent = computed(() => agents.value.find((agent) => agent.id === selectedAgentId.value));
const selectedModel = computed(() => models.value.find((model) => model.id === selectedModelId.value));
const canIntroduceSupplement = computed(() => generationPaused.value && !loading.value && Boolean(inputText.value.trim()) && Boolean(selectedModelId.value));
const bestSourceScore = computed(() => retrievalSources.value.reduce((best, source) => Math.max(best, source.score || 0), 0));
const successfulToolCount = computed(() => toolResults.value.filter((tool) => Boolean(tool.success)).length);
const evidenceSummary = computed(() => ({
  sourceCount: retrievalSources.value.length,
  bestScore: bestSourceScore.value,
  toolCount: toolResults.value.length,
  successfulToolCount: successfulToolCount.value,
  tokenCount: Number(runDone.value.totalTokens || 0),
  latencyMs: Number(runDone.value.latencyMs || 0),
}));

const routeSummary = computed(() => {
  const value = runDone.value.intentRoute || runMeta.value.intentRoute;
  if (!value || typeof value !== 'object') {
    return null;
  }
  const routePlan = value as Record<string, unknown>;
  const selectedTools = Array.isArray(routePlan.selectedToolNames)
    ? routePlan.selectedToolNames.map((item) => String(item)).filter(Boolean)
    : [];
  const intents = Array.isArray(routePlan.intents)
    ? routePlan.intents.map((item) => String(item)).filter(Boolean)
    : [];
  const title = routePlan.needsClarification === true
    ? '需要补充实体'
    : routePlan.needTool === true && routePlan.needRag === true
      ? '工具 + 知识库'
      : routePlan.needTool === true
        ? '工具调用'
        : routePlan.needRag === true
          ? '知识库检索'
          : '直接回答';
  const detail = [
    intents.length ? `意图：${intents.join('、')}` : '',
    selectedTools.length ? `工具：${selectedTools.join('、')}` : '工具：未触发',
    routePlan.missingEntities && Array.isArray(routePlan.missingEntities) && routePlan.missingEntities.length
      ? `缺少：${routePlan.missingEntities.join('、')}`
      : '',
    String(routePlan.reason || ''),
  ].filter(Boolean).join('；');
  return { title, detail };
});

const enhancedQuerySummary = computed(() => {
  const value = runDone.value.enhancedQueries || runMeta.value.enhancedQueries;
  return Array.isArray(value) ? value.map((item) => String(item)).filter(Boolean) : [];
});

function switchEvidencePanel(panel: EvidencePanel) {
  activeEvidencePanel.value = panel;
}

const runtimePhases = computed(() => {
  const hasRun = Boolean(runMeta.value.runId || runDone.value.runId);
  const done = String(runDone.value.status || '').toUpperCase() === 'SUCCESS';
  const failed = Boolean(errorMessage.value);
  const hasUserMessage = messages.value.some((message) => message.role === 'user');
  const hasAssistantOutput = messages.value.some((message) => message.role === 'assistant' && Boolean(message.content));
  const trustedBlocked = trustedAnswer.value?.enabled && trustedAnswer.value.answerable === false;
  return [
    {
      id: 'input',
      label: '输入接收',
      status: hasUserMessage ? 'success' : inputText.value.trim() ? 'running' : 'pending',
      summary: hasUserMessage ? '已进入会话' : '等待输入',
      reason: hasUserMessage ? '用户问题已进入 Runtime，上下文开始装配。' : '输入后 Runtime 会创建一次新的运行链路。',
      metric: selectedSessionId.value ? `Session ${selectedSessionId.value}` : '新会话',
    },
    {
      id: 'context',
      label: '上下文装配',
      status: hasRun ? 'success' : loading.value ? 'running' : 'pending',
      summary: selectedAgent.value ? selectedAgent.value.agentName : '默认 Agent',
      reason: `模型 ${selectedModel.value?.modelName || '-'}，Agent ${selectedAgent.value?.agentName || '默认 Agent'}，记忆召回 ${memoryResults.value.length} 条。`,
      metric: runMeta.value.runId ? `Run ${runMeta.value.runId}` : '未生成 Run',
      evidence: memoryResults.value.slice(0, 3).map((item) => item.memoryText),
    },
    {
      id: 'rag',
      label: 'RAG 证据',
      status: trustedBlocked ? 'warning' : retrievalSources.value.length > 0 ? 'success' : hasRun ? 'neutral' : 'pending',
      summary: retrievalSources.value.length > 0 ? `${retrievalSources.value.length} 条来源` : trustedBlocked ? '证据不足' : '未命中',
      reason: trustedBlocked ? trustedAnswer.value?.rejectReason || '可信回答模式拦截' : `引用来源 ${retrievalSources.value.length} 条，最佳置信 ${(trustedAnswer.value?.confidenceScore || 0).toFixed(4)}。`,
      metric: trustedAnswer.value?.enabled ? `可信模式 · 最少 ${trustedAnswer.value.minCitationCount || 0} 条` : '普通 RAG',
      evidence: retrievalSources.value.slice(0, 3).map((source) => `${source.documentName || source.kbName} #${source.chunkNo || '-'}`),
    },
    {
      id: 'tool',
      label: '工具动作',
      status: toolResults.value.length > 0 ? 'success' : hasRun ? 'neutral' : 'pending',
      summary: toolResults.value.length > 0 ? `${toolResults.value.length} 次调用` : '未触发',
      reason: toolResults.value.length > 0 ? '模型选择了工具并将结果回填到最终回答链路。' : '本轮没有工具调用，直接进入模型生成或可信拒答。',
      evidence: toolResults.value.slice(0, 3).map((tool) => String(tool.toolName || tool.toolCode || 'tool')),
    },
    {
      id: 'llm',
      label: '模型生成',
      status: failed ? 'danger' : loading.value ? 'running' : done || hasAssistantOutput ? 'success' : hasRun ? 'pending' : 'pending',
      summary: loading.value ? '流式输出中' : done || hasAssistantOutput ? '已有输出' : '待生成',
      reason: failed ? errorMessage.value : `Token ${runDone.value.totalTokens || 0}，耗时 ${runDone.value.latencyMs || 0}ms。`,
      metric: `${runMeta.value.providerName || selectedModel.value?.providerName || '-'} / ${runMeta.value.modelName || selectedModel.value?.modelName || '-'}`,
    },
    {
      id: 'governance',
      label: '治理判定',
      status: trustedBlocked ? 'warning' : failed ? 'danger' : done ? 'success' : hasRun ? 'running' : 'pending',
      summary: trustedBlocked ? '可信拒答' : trustedAnswer.value?.enabled ? '可信通过' : '常规通过',
      reason: trustedBlocked ? trustedAnswer.value?.rejectReason || '证据未达到可信回答条件。' : trustedAnswer.value?.qualityAdvice || '当前运行未触发阻断策略。',
      metric: trustedAnswer.value?.citationRequired ? '要求引用' : '不强制引用',
    },
    {
      id: 'output',
      label: '结果交付',
      status: failed ? 'danger' : done ? 'success' : loading.value ? 'running' : 'pending',
      summary: done ? '已完成' : loading.value ? '生成中' : '待完成',
      reason: done ? '最终回复、引用来源、工具结果和 Token 用量已回传到前端。' : '等待 Runtime 完成输出。',
      metric: runDone.value.runId ? `Trace ${runDone.value.runId}` : '',
    },
  ] as const;
});

async function loadOptions() {
  errorMessage.value = '';
  try {
    const [agentResult, modelResult] = await Promise.all([fetchAgents(), fetchChatModels()]);
    agents.value = agentResult;
    models.value = modelResult;
    const queryAgentId = typeof route.query.agentId === 'string' ? route.query.agentId : '';
    const queryModelId = typeof route.query.modelId === 'string' ? route.query.modelId : '';
    selectedAgentId.value = agentResult.some((agent) => agent.id === queryAgentId)
      ? queryAgentId
      : agentResult[0]?.id ?? '';
    selectedModelId.value = modelResult.some((model) => model.id === queryModelId)
      ? queryModelId
      : modelResult.find((model) => model.isDefault)?.id ?? modelResult[0]?.id ?? '';
    if (selectedAgentId.value) {
      await loadSessions(true);
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载调试配置失败';
  }
}

async function loadSessions(openLatest = false) {
  if (!selectedAgentId.value) {
    sessions.value = [];
    selectedSessionId.value = '';
    messages.value = [];
    return;
  }
  sessionsLoading.value = true;
  try {
    sessions.value = await fetchAgentSessions(selectedAgentId.value);
    const stillExists = sessions.value.some((session) => session.id === selectedSessionId.value);
    if (openLatest && sessions.value.length > 0) {
      await openSession(sessions.value[0].id);
    } else if (!stillExists) {
      selectedSessionId.value = '';
      messages.value = [];
    }
  } finally {
    sessionsLoading.value = false;
  }
}

async function openSession(sessionId: string) {
  if (!selectedAgentId.value || !sessionId) {
    return;
  }
  selectedSessionId.value = sessionId;
  const rows = await fetchAgentSessionMessages(selectedAgentId.value, sessionId);
  messages.value = rows
    .filter((message): message is typeof message & { role: 'user' | 'assistant' } => message.role === 'user' || message.role === 'assistant')
    .map((message) => ({
      role: message.role,
      content: message.content,
      status: message.role === 'assistant' ? '历史消息' : undefined,
    }));
  runMeta.value = {};
  runDone.value = {};
  retrievalSources.value = [];
  memoryResults.value = [];
  toolResults.value = [];
  trustedAnswer.value = null;
  generationPaused.value = false;
  // 等待历史消息完成渲染，再把独立消息容器定位到最后一条消息。
  await nextTick();
  if (messages.value.length > 0) {
    scrollToLatest(chatMessagesRef.value);
  }
}

function startNewSession() {
  selectedSessionId.value = '';
  messages.value = [];
  runMeta.value = {};
  runDone.value = {};
  retrievalSources.value = [];
  memoryResults.value = [];
  toolResults.value = [];
  trustedAnswer.value = null;
  errorMessage.value = '';
  generationPaused.value = false;
}

async function removeSession(sessionId: string) {
  if (!selectedAgentId.value || !sessionId) {
    return;
  }
  await deleteAgentSession(selectedAgentId.value, sessionId);
  if (selectedSessionId.value === sessionId) {
    startNewSession();
  }
  await loadSessions(false);
}

async function selectAgent(agentId: string) {
  selectedAgentId.value = agentId;
  startNewSession();
  await loadSessions(true);
}

function stopGeneration() {
  if (!loading.value || !streamAbortController.value) {
    return;
  }
  streamAbortController.value.abort();
  generationPaused.value = true;
  loading.value = false;
  if (activeAssistantMessage.value) {
    activeAssistantMessage.value.status = '已暂停';
    if (!activeAssistantMessage.value.content) {
      activeAssistantMessage.value.content = '已停止接收模型输出，可输入补充说明后继续。';
    }
  }
}

async function introduceSupplement() {
  const supplement = inputText.value.trim();
  if (!canIntroduceSupplement.value) {
    return;
  }
  const question = `补充说明：${supplement}\n\n请结合上一轮已生成的内容继续回答，不需要从头重复。`;
  await sendMessageWithText(question, `补充说明：${supplement}`);
}

async function sendMessage() {
  if (loading.value) {
    stopGeneration();
    return;
  }
  await sendMessageWithText(inputText.value.trim());
}

async function sendMessageWithText(question: string, displayQuestion = question) {
  if (!question || loading.value) {
    return;
  }

  const history = buildCleanHistory(question);
  messages.value.push({ role: 'user', content: displayQuestion });
  const assistantMessage: UiMessage = { role: 'assistant', content: '', status: '生成中' };
  messages.value.push(assistantMessage);
  inputText.value = '';
  generationPaused.value = false;
  loading.value = true;
  const controller = new AbortController();
  streamAbortController.value = controller;
  activeAssistantMessage.value = assistantMessage;
  errorMessage.value = '';
  runMeta.value = {};
  runDone.value = {};
  retrievalSources.value = [];
  memoryResults.value = [];
  toolResults.value = [];
  trustedAnswer.value = null;

  const payload = {
    agentId: selectedAgentId.value || undefined,
    modelId: selectedModelId.value || undefined,
    sessionId: selectedSessionId.value || undefined,
    input: question,
    history,
    temperature: 0.3,
    maxTokens: selectedModel.value?.maxOutputTokens ? Math.min(selectedModel.value.maxOutputTokens, 2048) : 2048,
  };

  try {
    const streamResult = selectedAgentId.value
      ? await streamAgent(selectedAgentId.value, payload, streamHandlers(assistantMessage), controller.signal)
      : await streamChat(payload, streamHandlers(assistantMessage), controller.signal);
    if (streamResult.aborted) {
      assistantMessage.status = '已暂停';
      generationPaused.value = true;
      return;
    }
    if (!streamResult.doneReceived && !streamResult.errorReceived) {
      assistantMessage.status = assistantMessage.content ? '已完成' : '未收到完成事件';
    }
  } catch (error) {
    if (controller.signal.aborted) {
      assistantMessage.status = '已暂停';
      generationPaused.value = true;
      return;
    }
    if (String(runDone.value.status ?? '').toUpperCase() === 'SUCCESS') {
      assistantMessage.status = '已完成';
      return;
    }
    assistantMessage.status = '失败';
    errorMessage.value = error instanceof Error ? error.message : '模型调用失败';
  } finally {
    loading.value = false;
    if (streamAbortController.value === controller) {
      streamAbortController.value = null;
    }
    if (activeAssistantMessage.value === assistantMessage) {
      activeAssistantMessage.value = null;
    }
  }
}

function buildCleanHistory(_question: string): ChatMessage[] {
  return messages.value.map((message) => ({
    role: message.role,
    content: message.content,
  }));
}

function streamHandlers(assistantMessage: UiMessage) {
  return {
    onMeta: (data: Record<string, unknown>) => {
      runMeta.value = data;
      selectedSessionId.value = String(data.sessionId || selectedSessionId.value || '');
      memoryResults.value = normalizeMemories(data.memories);
      retrievalSources.value = normalizeSources(data.sources);
      trustedAnswer.value = normalizeTrustedAnswer(data.trustedAnswer);
    },
    onDelta: (content: string) => {
      assistantMessage.content += content;
    },
    onTool: (data: Record<string, unknown>) => {
      toolResults.value = normalizeToolResults(data.toolResults);
    },
    onDone: (data: Record<string, unknown>) => {
      runDone.value = data;
      selectedSessionId.value = String(data.sessionId || selectedSessionId.value || '');
      memoryResults.value = normalizeMemories(data.memories);
      retrievalSources.value = normalizeSources(data.sources);
      toolResults.value = normalizeToolResults(data.toolResults);
      trustedAnswer.value = normalizeTrustedAnswer(data.trustedAnswer);
      assistantMessage.status = '已完成';
      void loadSessions(false);
    },
    onError: (message: string) => {
      assistantMessage.status = '失败';
      errorMessage.value = message;
    },
  };
}

function normalizeSources(value: unknown): KnowledgeSource[] {
  return Array.isArray(value) ? (value as KnowledgeSource[]) : [];
}

function normalizeMemories(value: unknown): MemoryRecallItem[] {
  return Array.isArray(value) ? (value as MemoryRecallItem[]) : [];
}

function normalizeToolResults(value: unknown): Record<string, unknown>[] {
  return Array.isArray(value) ? (value as Record<string, unknown>[]) : [];
}

function normalizeTrustedAnswer(value: unknown): TrustedAnswerStatus | null {
  if (!value || typeof value !== 'object') {
    return null;
  }
  const payload = value as Record<string, unknown>;
  return {
    enabled: payload.enabled === true,
    answerable: payload.answerable !== false,
    citationRequired: payload.citationRequired === true,
    minCitationCount: Number(payload.minCitationCount || 0),
    confidenceScore: Number(payload.confidenceScore || 0),
    rejectReason: String(payload.rejectReason || ''),
    qualityAdvice: String(payload.qualityAdvice || ''),
  };
}

function onInputKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault();
    if (!loading.value) {
      void sendMessage();
    }
  }
}

function openTrace() {
  const runId = String(runMeta.value.runId ?? runDone.value.runId ?? '');
  if (runId) {
    void router.push(`/logs/${runId}`);
  }
}

onMounted(() => {
  void loadOptions();
});

watch(selectedAgentId, (agentId, previousAgentId) => {
  if (agentId && previousAgentId && agentId !== previousAgentId) {
    void selectAgent(agentId);
  }
});
</script>

<template>
  <PageHeader title="调试台 / 对话工作台" description="选择 Agent 与模型进行真实对话，流式查看模型输出和基础 Trace">
    <template #actions>
      <button class="secondary-button" type="button" @click="showModal('prompt')"><FileSearch :size="16" /> Prompt 预览</button>
      <button class="secondary-button" type="button" @click="showDrawer('sources')"><Sparkles :size="16" /> 引用来源</button>
      <button class="primary-button" type="button" :disabled="!runMeta.runId" @click="openTrace"><History :size="16" /> 查看 Trace</button>
    </template>
  </PageHeader>

  <section class="debug-layout">
    <aside class="debug-rail">
      <label>
        Agent
        <select v-model="selectedAgentId">
          <option value="">默认 Agent</option>
          <option v-for="agent in agents" :key="agent.id" :value="agent.id">{{ agent.agentName }}</option>
        </select>
      </label>
      <label>
        模型
        <select v-model="selectedModelId">
          <option v-for="model in models" :key="model.id" :value="model.id">
            {{ model.providerName }} / {{ model.modelName }}
          </option>
        </select>
      </label>
      <section class="debug-scroll-box agent-scroll-box">
        <button
          v-for="agent in agents"
          :key="agent.id"
          class="agent-pick"
          :class="{ selected: selectedAgentId === agent.id }"
          type="button"
          @click="selectAgent(agent.id)"
        >
          <span>A</span>
          <b>{{ agent.agentName }}</b>
          <small>{{ agent.category }} - {{ agent.statusLabel || agent.status }}</small>
        </button>
        <div v-if="agents.length === 0" class="session-empty">暂无可用智能体</div>
      </section>

      <div class="session-header">
        <h2>历史会话</h2>
        <button class="icon-button" type="button" title="新建会话" @click="startNewSession">
          <MessageSquarePlus :size="16" />
        </button>
      </div>
      <section class="debug-scroll-box session-list">
        <div v-if="sessionsLoading" class="session-empty">正在加载会话...</div>
        <article
          v-for="session in sessions"
          :key="session.id"
          class="session-item"
          :class="{ active: selectedSessionId === session.id }"
        >
          <button class="session-open" type="button" @click="openSession(session.id)">
            <b>{{ session.sessionTitle }}</b>
            <span>{{ session.lastMessage || '暂无消息' }}</span>
            <small>{{ session.messageCount }} 条消息</small>
          </button>
          <button class="session-delete" type="button" title="删除会话" @click.stop="removeSession(session.id)">
            <Trash2 :size="14" />
          </button>
        </article>
        <div v-if="!sessionsLoading && sessions.length === 0" class="session-empty">暂无历史会话</div>
      </section>
    </aside>

    <div class="chat-panel">
      <div ref="chatMessagesRef" class="chat-messages">
        <div v-if="!messages.length" class="empty-state">
          <Sparkles :size="22" />
          <b>真实模型调试已就绪</b>
          <span>选择一个 Agent 和模型，发送消息后会通过后端 SSE 接收流式输出。</span>
        </div>
        <div v-for="(message, index) in messages" :key="`${message.role}-${index}`" class="message" :class="message.role">
          <p v-if="message.role === 'user'">{{ message.content }}</p>
          <span v-else>AI</span>
          <div v-if="message.role === 'assistant'">
            <StatusBadge :label="message.status || '生成中'" :tone="message.status === '失败' ? 'danger' : 'success'" />
            <p class="markdown-text">{{ message.content || '正在等待模型返回...' }}</p>
          </div>
        </div>
      </div>
      <p v-if="errorMessage" class="form-error"><AlertCircle :size="15" /> {{ errorMessage }}</p>
      <div class="chat-input">
        <textarea v-model="inputText" placeholder="请输入你的问题，Enter 发送，Shift + Enter 换行" @keydown="onInputKeydown" />
        <div class="chat-input-actions">
          <button
            :class="loading ? 'danger-button' : 'primary-button'"
            type="button"
            :disabled="!loading && !selectedModelId"
            @click="sendMessage"
          >
            <Square v-if="loading" :size="15" />
            <Send v-else :size="16" />
            {{ loading ? '暂停' : '发送' }}
          </button>
          <button class="secondary-button" type="button" :disabled="!canIntroduceSupplement" @click="introduceSupplement">
            <CornerDownRight :size="16" /> 引入补充
          </button>
        </div>
      </div>
      <p v-if="generationPaused" class="supplement-hint">
        已暂停生成，可输入补充说明后点击“引入补充”继续当前会话。
      </p>
    </div>

    <aside class="trace-panel">
      <div class="section-title"><h2>实时 Trace</h2><StatusBadge :label="loading ? '运行中' : runDone.status ? '成功' : '待运行'" /></div>
      <div class="trace-step"><b>Agent</b><span>{{ selectedAgent?.agentName || '默认 Agent' }}</span></div>
      <div class="trace-step"><b>模型服务商</b><span>{{ runMeta.providerName || selectedModel?.providerName || '-' }}</span></div>
      <div class="trace-step"><b>模型</b><span>{{ runMeta.modelName || selectedModel?.modelName || '-' }}</span></div>
      <div class="trace-step"><b>Session ID</b><span class="mono">{{ selectedSessionId || '-' }}</span></div>
      <div class="trace-step"><b>Run ID</b><span class="mono">{{ runMeta.runId || '-' }}</span></div>
      <div class="trace-step"><b>记忆召回</b><span :title="memoryResults.map((item) => item.memoryText).join('\n\n')">{{ memoryResults.length }} 条</span></div>
      <div class="trusted-answer-card" :class="{ blocked: trustedAnswer?.enabled && trustedAnswer.answerable === false }">
        <div>
          <b>可信回答</b>
          <StatusBadge
            :label="trustedAnswer?.enabled ? (trustedAnswer.answerable === false ? '已拒答' : '已启用') : '未启用'"
            :tone="trustedAnswer?.enabled && trustedAnswer.answerable === false ? 'warning' : trustedAnswer?.enabled ? 'success' : 'neutral'"
          />
        </div>
        <p>
          最佳置信 {{ (trustedAnswer?.confidenceScore || 0).toFixed(4) }} ·
          最少引用 {{ trustedAnswer?.minCitationCount || 0 }} ·
          {{ trustedAnswer?.citationRequired ? '要求引用' : '不强制引用' }}
        </p>
        <small v-if="trustedAnswer?.rejectReason">{{ trustedAnswer.rejectReason }}</small>
      </div>
      <RuntimeInterpreter title="Runtime 解释器" :phases="runtimePhases" compact />
      <div v-if="routeSummary" class="insight-strip runtime-route-summary">
        <b>本轮路由：{{ routeSummary.title }}</b>
        <p>{{ routeSummary.detail }}</p>
      </div>
      <section class="trace-evidence-tabs" aria-label="调试证据切换" role="tablist">
        <input id="debug-evidence-sources" v-model="activeEvidencePanel" class="trace-evidence-radio" type="radio" name="debug-evidence-panel" value="sources" />
        <label
          class="trace-evidence-tab"
          :class="{ active: activeEvidencePanel === 'sources' }"
          for="debug-evidence-sources"
          role="tab"
          tabindex="0"
          :aria-selected="activeEvidencePanel === 'sources'"
          @keydown.enter.prevent="switchEvidencePanel('sources')"
          @keydown.space.prevent="switchEvidencePanel('sources')"
        >
          <span>检索结果</span>
          <b>{{ retrievalSources.length }}</b>
          <small>知识库引用</small>
        </label>
        <input id="debug-evidence-tools" v-model="activeEvidencePanel" class="trace-evidence-radio" type="radio" name="debug-evidence-panel" value="tools" />
        <label
          class="trace-evidence-tab"
          :class="{ active: activeEvidencePanel === 'tools' }"
          for="debug-evidence-tools"
          role="tab"
          tabindex="0"
          :aria-selected="activeEvidencePanel === 'tools'"
          @keydown.enter.prevent="switchEvidencePanel('tools')"
          @keydown.space.prevent="switchEvidencePanel('tools')"
        >
          <span>工具调用</span>
          <b>{{ toolResults.length }}</b>
          <small>{{ successfulToolCount }} 次成功</small>
        </label>
        <input id="debug-evidence-stats" v-model="activeEvidencePanel" class="trace-evidence-radio" type="radio" name="debug-evidence-panel" value="stats" />
        <label
          class="trace-evidence-tab"
          :class="{ active: activeEvidencePanel === 'stats' }"
          for="debug-evidence-stats"
          role="tab"
          tabindex="0"
          :aria-selected="activeEvidencePanel === 'stats'"
          @keydown.enter.prevent="switchEvidencePanel('stats')"
          @keydown.space.prevent="switchEvidencePanel('stats')"
        >
          <span>引用统计</span>
          <b>{{ bestSourceScore.toFixed(2) }}</b>
          <small>最佳置信</small>
        </label>
      </section>
      <section v-if="activeEvidencePanel === 'sources'" :key="activeEvidencePanel" class="trace-scroll-section trace-evidence-panel">
        <div class="section-title"><h2>检索结果</h2><span>{{ retrievalSources.length }} 条</span></div>
        <div class="debug-scroll-box source-scroll-box">
          <div v-if="retrievalSources.length === 0" class="empty-state compact-empty">当前对话暂无知识库引用</div>
          <article v-for="source in retrievalSources" :key="source.chunkId" class="chunk-item">
            <div>
              <b>{{ source.documentName || source.kbName }}</b>
              <StatusBadge :label="source.score ? source.score.toFixed(4) : '命中'" />
            </div>
            <p>{{ source.quoteText }}</p>
          </article>
        </div>
      </section>
      <section v-else-if="activeEvidencePanel === 'tools'" :key="activeEvidencePanel" class="trace-scroll-section trace-evidence-panel">
        <div class="section-title"><h2>工具调用</h2><span>{{ toolResults.length }} 次</span></div>
        <div class="debug-scroll-box tool-scroll-box">
          <div v-if="toolResults.length === 0" class="empty-state compact-empty">当前对话暂无工具调用</div>
          <article v-for="tool in toolResults" :key="String(tool.toolCallId || tool.toolName)" class="chunk-item">
            <div>
              <b>{{ tool.toolName }}</b>
              <StatusBadge :label="tool.success ? '成功' : tool.confirmationRequired ? '待确认' : '失败'" :tone="tool.success ? 'success' : tool.confirmationRequired ? 'warning' : 'danger'" />
            </div>
            <p>statusCode: {{ tool.statusCode || 0 }} - latencyMs: {{ tool.latencyMs || 0 }}</p>
            <p v-if="tool.confirmationId" class="mono">confirmationId: {{ tool.confirmationId }}</p>
            <p>{{ tool.errorMessage || tool.responseBody || '工具已执行' }}</p>
          </article>
        </div>
      </section>
      <section v-else :key="activeEvidencePanel" class="trace-scroll-section trace-evidence-panel">
        <div class="section-title"><h2>引用统计</h2><span>{{ runDone.status || '待运行' }}</span></div>
        <div class="trace-stat-grid">
          <article class="trace-stat-card">
            <span>检索结果</span>
            <b>{{ evidenceSummary.sourceCount }}</b>
            <small>当前轮引用条数</small>
          </article>
          <article class="trace-stat-card">
            <span>最佳置信</span>
            <b>{{ evidenceSummary.bestScore.toFixed(4) }}</b>
            <small>{{ trustedAnswer?.qualityAdvice || '暂无质量建议' }}</small>
          </article>
          <article class="trace-stat-card">
            <span>工具成功</span>
            <b>{{ evidenceSummary.successfulToolCount }}/{{ evidenceSummary.toolCount }}</b>
            <small>调用成功率</small>
          </article>
          <article class="trace-stat-card">
            <span>Token</span>
            <b>{{ evidenceSummary.tokenCount }}</b>
            <small>本次模型消耗</small>
          </article>
          <article class="trace-stat-card">
            <span>耗时</span>
            <b>{{ evidenceSummary.latencyMs }}ms</b>
            <small>端到端返回</small>
          </article>
          <pre class="code-block light">promptTokens: {{ runDone.promptTokens || 0 }}
completionTokens: {{ runDone.completionTokens || 0 }}
totalTokens: {{ runDone.totalTokens || 0 }}
latencyMs: {{ runDone.latencyMs || 0 }}</pre>
          <div v-if="enhancedQuerySummary.length || runDone.rerankMode || runMeta.rerankMode" class="insight-strip retrieval-runtime-summary">
            <b>检索增强链路</b>
            <p v-if="enhancedQuerySummary.length">查询变体：{{ enhancedQuerySummary.join(' / ') }}</p>
            <p>重排模式：{{ runDone.rerankMode || runMeta.rerankMode }}；重排耗时：{{ runDone.rerankLatencyMs || runMeta.rerankLatencyMs || 0 }}ms</p>
            <p v-if="runDone.rerankErrorMessage || runMeta.rerankErrorMessage" class="muted-text">降级原因：{{ runDone.rerankErrorMessage || runMeta.rerankErrorMessage }}</p>
          </div>
        </div>
      </section>
    </aside>
  </section>
</template>
