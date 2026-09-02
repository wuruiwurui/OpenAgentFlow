<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ArrowLeft, ClipboardList, RefreshCw, Search, Upload } from 'lucide-vue-next';
import PageHeader from '../../components/PageHeader.vue';
import PaginationBar from '../../components/PaginationBar.vue';
import StatCard from '../../components/StatCard.vue';
import StatusBadge from '../../components/StatusBadge.vue';
import {
  fetchKnowledgeBase,
  fetchKnowledgeChunks,
  fetchKnowledgeDocumentStatus,
  rebuildKnowledgeVectors,
  reprocessKnowledgeDocument,
  retrievalTest,
  uploadKnowledgeDocument,
  type KnowledgeBaseDetail,
  type KnowledgeDocumentSummary,
  type KnowledgeChunkSummary,
  type KnowledgeRetrievalOptions,
  type KnowledgeRetrievalResult,
  type KnowledgeSource,
} from '../../api/knowledge';
import { useOverlay } from '../../composables/useOverlay';
import { usePagination } from '../../composables/usePagination';

const route = useRoute();
const router = useRouter();
const { toast } = useOverlay();
const detail = ref<KnowledgeBaseDetail | null>(null);
const selectedDocumentId = ref('');
const retrievalDocumentId = ref('');
const fileInput = ref<HTMLInputElement | null>(null);
const loading = ref(false);
const uploading = ref(false);
const rebuilding = ref(false);
const reprocessingDocumentId = ref('');
const pollingDocumentId = ref('');
const pollTimer = ref<number | null>(null);
const query = ref('请根据知识库总结核心内容');
const sources = ref<KnowledgeSource[]>([]);
const retrievalLatency = ref(0);
const retrievalQuality = ref<KnowledgeRetrievalResult | null>(null);
const chunks = ref<KnowledgeChunkSummary[]>([]);
const chunkTotal = ref(0);
const chunkPage = ref(1);
const chunkPageSize = 10;
type KnowledgeDetailPanel = 'chunks' | 'retrieval' | 'sources';
const activePanel = ref<KnowledgeDetailPanel>('chunks');
const retrievalForm = reactive<KnowledgeRetrievalOptions>({
  query: '',
  topK: 5,
  candidateK: 20,
  scoreThreshold: 0.55,
  searchMode: 'hybrid',
  rerankEnabled: true,
  vectorWeight: 0.72,
  keywordWeight: 0.28,
  pageNo: undefined,
  metadataKeyword: '',
  lowConfidenceThreshold: 0.62,
  rejectLowConfidence: true,
  queryRewriteEnabled: true,
  multiQueryEnabled: true,
  maxQueryVariants: 4,
});

const documents = computed(() => detail.value?.documents ?? []);
const selectedDocument = computed(() => detail.value?.documents.find((doc) => doc.id === selectedDocumentId.value));
const { currentPage: documentPage, pagedItems: pagedDocuments } = usePagination(documents);
const { currentPage: sourcePage, pagedItems: pagedSources } = usePagination(sources);
const processing = computed(() => selectedDocument.value?.parseStatus === 'processing');

onMounted(() => {
  void loadDetail();
});

onUnmounted(() => {
  stopPolling();
});

async function loadDetail() {
  loading.value = true;
  try {
    detail.value = await fetchKnowledgeBase(String(route.params.id));
    selectedDocumentId.value = selectedDocumentId.value || detail.value.documents[0]?.id || '';
    await loadChunks(1);
    const processingDoc = detail.value.documents.find((doc) => doc.parseStatus === 'processing');
    if (processingDoc) {
      startPolling(processingDoc.id);
    }
  } finally {
    loading.value = false;
  }
}

async function loadChunks(page = chunkPage.value) {
  if (!selectedDocumentId.value) {
    chunks.value = [];
    chunkTotal.value = 0;
    return;
  }
  chunkPage.value = page;
  const result = await fetchKnowledgeChunks(String(route.params.id), selectedDocumentId.value, page, chunkPageSize);
  chunks.value = result.records;
  chunkTotal.value = result.total;
}

async function selectDocument(documentId: string) {
  selectedDocumentId.value = documentId;
  await loadChunks(1);
}

function chooseFile() {
  fileInput.value?.click();
}

async function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) {
    return;
  }
  uploading.value = true;
  try {
    const result = await uploadKnowledgeDocument(String(route.params.id), file);
    upsertDocument(result.document);
    selectedDocumentId.value = result.document.id;
    toast(result.message || '文件已上传，后台开始处理');
    startPolling(result.document.id);
  } finally {
    uploading.value = false;
    input.value = '';
  }
}

async function handleRetrievalTest() {
  if (!query.value.trim()) {
    return;
  }
  const result = await retrievalTest(String(route.params.id), {
    ...retrievalForm,
    query: query.value.trim(),
    topK: Number(retrievalForm.topK || 5),
    candidateK: Number(retrievalForm.candidateK || 20),
    scoreThreshold: Number(retrievalForm.scoreThreshold ?? 0.55),
    lowConfidenceThreshold: Number(retrievalForm.lowConfidenceThreshold ?? 0.62),
    vectorWeight: Number(retrievalForm.vectorWeight ?? 0.72),
    keywordWeight: Number(retrievalForm.keywordWeight ?? 0.28),
    documentIds: retrievalDocumentId.value ? [retrievalDocumentId.value] : undefined,
    pageNo: Number(retrievalForm.pageNo || 0) > 0 ? Number(retrievalForm.pageNo) : undefined,
    metadataKeyword: retrievalForm.metadataKeyword?.trim() || undefined,
  });
  sources.value = result.sources;
  retrievalLatency.value = result.latencyMs;
  retrievalQuality.value = result;
  sourcePage.value = 1;
  activePanel.value = 'sources';
  toast(result.lowConfidence ? '检索完成，但结果低置信，请检查阈值或知识库内容' : `检索完成，命中 ${result.sources.length} 条来源`);
}

function switchPanel(panel: KnowledgeDetailPanel) {
  activePanel.value = panel;
}

async function handleRebuildVectors() {
  if (!detail.value || rebuilding.value) {
    return;
  }
  if (!window.confirm(`确认重建知识库「${detail.value.kbName}」的全部分片向量吗？任务会在后台执行。`)) {
    return;
  }
  rebuilding.value = true;
  try {
    const result = await rebuildKnowledgeVectors(String(route.params.id));
    toast(result.message || '向量重建任务已提交');
    router.push('/tasks');
  } finally {
    rebuilding.value = false;
  }
}

async function handleReprocessDocument() {
  if (!selectedDocument.value || reprocessingDocumentId.value) return;
  if (!window.confirm(`确认重新解析「${selectedDocument.value.docName}」吗？旧分片和向量会被清理后重新生成。`)) return;
  reprocessingDocumentId.value = selectedDocument.value.id;
  try {
    const result = await reprocessKnowledgeDocument(String(route.params.id), selectedDocument.value.id);
    upsertDocument(result.document);
    toast(result.message || '文档重新解析任务已提交');
    startPolling(selectedDocument.value.id);
  } finally {
    reprocessingDocumentId.value = '';
  }
}

function startPolling(documentId: string) {
  stopPolling();
  pollingDocumentId.value = documentId;
  void pollDocument();
  pollTimer.value = window.setInterval(() => {
    void pollDocument();
  }, 1500);
}

function stopPolling() {
  if (pollTimer.value) {
    window.clearInterval(pollTimer.value);
    pollTimer.value = null;
  }
}

async function pollDocument() {
  if (!pollingDocumentId.value) {
    return;
  }
  const doc = await fetchKnowledgeDocumentStatus(String(route.params.id), pollingDocumentId.value);
  upsertDocument(doc);
  if (doc.parseStatus === 'parsed' || doc.parseStatus === 'failed') {
    stopPolling();
    await loadDetail();
    upsertDocument(doc);
  }
}

function upsertDocument(doc: KnowledgeDocumentSummary) {
  if (!detail.value) {
    return;
  }
  const index = detail.value.documents.findIndex((item) => item.id === doc.id);
  if (index >= 0) {
    detail.value.documents[index] = doc;
  } else {
    detail.value.documents.unshift(doc);
  }
}

function formatSize(size?: number) {
  if (!size) return '-';
  if (size > 1024 * 1024) return `${(size / 1024 / 1024).toFixed(2)} MB`;
  if (size > 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${size} B`;
}

function statusLabel(status?: string) {
  if (status === 'parsed') return '已完成';
  if (status === 'processing') return '处理中';
  if (status === 'failed') return '失败';
  if (status === 'active') return '启用';
  return status || '未知';
}

function syncLabel(doc?: KnowledgeDocumentSummary) {
  if (!doc) return '未开始';
  if (doc.embeddingFallbackUsed) return '本地兜底';
  if (doc.embeddingDimension) return `真实模型 ${doc.embeddingDimension} 维`;
  if (doc.parseStatus === 'processing') return '等待模型返回';
  return '未生成';
}

function scoreText(value?: number) {
  return Number(value || 0).toFixed(4);
}

function searchModeLabel(value?: string) {
  if (value === 'vector') return '向量检索';
  if (value === 'keyword') return '关键词检索';
  return '混合检索';
}

function rerankModeLabel(value?: string) {
  if (value === 'cross_encoder') return '真实 Cross-Encoder 重排';
  if (value === 'rule') return '规则重排';
  if (value === 'rule_fallback') return '规则降级重排';
  if (value === 'disabled') return '未启用重排';
  if (value === 'cache') return '缓存结果';
  return value || '未执行';
}

function sourceQuoteHtml(source: KnowledgeSource) {
  return source.highlightedQuoteText || escapeHtml(source.quoteText || '');
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
</script>

<template>
  <PageHeader
    :title="detail ? detail.kbName : '知识库详情'"
    :description="detail ? `${detail.description || '暂无描述'} · ${detail.milvusCollectionName || '未绑定集合'}` : '加载中'"
  >
    <template #actions>
      <button class="secondary-button" type="button" @click="router.push('/knowledge')"><ArrowLeft :size="16" /> 返回</button>
      <button class="secondary-button" type="button" :disabled="loading" @click="loadDetail"><RefreshCw :size="16" /> 刷新</button>
      <button class="secondary-button" type="button" :disabled="rebuilding || !detail?.chunkCount" @click="handleRebuildVectors">
        <RefreshCw :size="16" /> {{ rebuilding ? '提交中' : '重建向量' }}
      </button>
      <button class="primary-button" type="button" :disabled="uploading" @click="chooseFile"><Upload :size="16" /> {{ uploading ? '上传中' : '上传文档' }}</button>
      <input ref="fileInput" type="file" hidden @change="handleFileChange" />
    </template>
  </PageHeader>

  <section v-if="detail" class="knowledge-detail">
    <aside class="document-list">
      <button
        v-for="doc in pagedDocuments"
        :key="doc.id"
        class="document-item"
        :class="{ active: selectedDocumentId === doc.id }"
        type="button"
        @click="selectDocument(doc.id)"
      >
        <b>{{ doc.docName }}</b>
        <span>{{ formatSize(doc.fileSize) }} · {{ statusLabel(doc.parseStatus) }} · {{ doc.progressPercent || 0 }}%</span>
      </button>
      <PaginationBar v-model:page="documentPage" :total="documents.length" />
      <div v-if="detail.documents.length === 0" class="empty-state">暂无文档，上传后会自动解析和向量化</div>
    </aside>

    <div class="section-block">
      <div class="tabs">
        <button class="tab" :class="{ active: activePanel === 'chunks' }" type="button" @click="switchPanel('chunks')">切片预览</button>
        <button class="tab" :class="{ active: activePanel === 'retrieval' }" type="button" @click="switchPanel('retrieval')">检索测试</button>
        <button class="tab" :class="{ active: activePanel === 'sources' }" type="button" @click="switchPanel('sources')">引用来源</button>
      </div>

      <template v-if="activePanel === 'chunks'">
        <div v-if="selectedDocument" class="process-panel">
          <div class="process-header">
            <div>
              <b>{{ selectedDocument.processStageLabel || statusLabel(selectedDocument.parseStatus) }}</b>
              <span>{{ selectedDocument.lastMessage || '等待处理日志' }}</span>
            </div>
            <StatusBadge :label="syncLabel(selectedDocument)" />
          </div>
          <div class="progress-track">
            <div class="progress-bar" :style="{ width: `${selectedDocument.progressPercent || 0}%` }"></div>
          </div>
          <div class="process-meta">
            <span>状态：{{ statusLabel(selectedDocument.parseStatus) }}</span>
            <span>接口：{{ selectedDocument.embeddingApi || '-' }}</span>
            <span>模型：{{ selectedDocument.embeddingModelCode || '-' }}</span>
            <span>Milvus：{{ selectedDocument.milvusSynced ? '已同步' : processing ? '同步中' : '未同步' }}</span>
          </div>
          <button
            v-if="selectedDocument.asyncTaskId"
            class="secondary-button"
            type="button"
            @click="router.push('/tasks')"
          >
            <ClipboardList :size="16" /> 查看任务中心日志
          </button>
          <button
            class="secondary-button"
            type="button"
            :disabled="processing || Boolean(reprocessingDocumentId)"
            @click="handleReprocessDocument"
          >
            <RefreshCw :size="16" /> {{ reprocessingDocumentId ? '提交中' : '重新解析' }}
          </button>
          <ul class="process-log">
            <li v-for="line in selectedDocument.processLogs || []" :key="line">{{ line }}</li>
          </ul>
          <p v-if="selectedDocument.parseError" class="error-text">{{ selectedDocument.parseError }}</p>
        </div>

        <article v-for="chunk in chunks" :key="chunk.id" class="chunk-item">
          <div>
            <b>{{ chunk.title || `分片 ${chunk.chunkNo}` }}</b>
            <StatusBadge :label="chunk.syncStatus === 'synced' ? '已写入 Milvus' : chunk.syncStatus || '待同步'" />
          </div>
          <p>{{ chunk.content }}</p>
          <span>{{ chunk.tokenCount }} tokens</span>
        </article>
        <PaginationBar :page="chunkPage" :total="chunkTotal" :page-size="chunkPageSize" @update:page="loadChunks" />
        <div v-if="chunks.length === 0" class="empty-state">
          {{ selectedDocument ? '当前文档暂无切片' : '暂无可预览分片' }}
        </div>
      </template>

      <template v-else-if="activePanel === 'retrieval'">
        <div class="metric-grid compact">
          <StatCard label="文档总数" :value="String(detail.documentCount)" detail="已上传" icon="Library" tone="info" />
          <StatCard label="切片总数" :value="String(detail.chunkCount)" detail="已入库" icon="Braces" tone="success" />
          <StatCard label="向量总数" :value="String(detail.embeddingCount)" detail="MySQL + Milvus" icon="Activity" tone="neutral" />
          <StatCard label="检索耗时" :value="`${retrievalLatency}ms`" detail="最近一次" icon="ShieldCheck" tone="warning" />
        </div>

        <div class="filter-row">
          <input v-model="query" placeholder="输入检索测试问题" />
          <select v-model="retrievalForm.searchMode">
            <option value="hybrid">混合检索</option>
            <option value="vector">向量检索</option>
            <option value="keyword">关键词检索</option>
          </select>
          <select v-model="retrievalDocumentId" title="限定文档">
            <option value="">全部文档</option>
            <option v-for="doc in documents" :key="doc.id" :value="doc.id">{{ doc.docName }}</option>
          </select>
          <label class="inline-field">TopK<input v-model.number="retrievalForm.topK" type="number" min="1" max="20" /></label>
          <label class="inline-field">候选<input v-model.number="retrievalForm.candidateK" type="number" min="1" max="100" /></label>
          <label class="inline-field">阈值<input v-model.number="retrievalForm.scoreThreshold" type="number" min="0" max="1" step="0.01" /></label>
          <label class="inline-field">低置信<input v-model.number="retrievalForm.lowConfidenceThreshold" type="number" min="0" max="1" step="0.01" /></label>
          <label class="inline-field">页码<input v-model.number="retrievalForm.pageNo" type="number" min="1" placeholder="不限" /></label>
          <label class="inline-field wide">元数据<input v-model.trim="retrievalForm.metadataKeyword" placeholder="来源/标签" /></label>
          <label class="checkbox-line"><input v-model="retrievalForm.rerankEnabled" type="checkbox" /> 重排</label>
          <label class="checkbox-line"><input v-model="retrievalForm.queryRewriteEnabled" type="checkbox" /> 查询改写</label>
          <label class="checkbox-line"><input v-model="retrievalForm.multiQueryEnabled" type="checkbox" /> 多查询融合</label>
          <label class="inline-field">变体数<input v-model.number="retrievalForm.maxQueryVariants" type="number" min="1" max="8" /></label>
          <label class="checkbox-line"><input v-model="retrievalForm.rejectLowConfidence" type="checkbox" /> 低置信拒答</label>
          <button class="primary-button" type="button" @click="handleRetrievalTest"><Search :size="16" /> 检索测试</button>
        </div>

        <div v-if="retrievalQuality" class="insight-strip retrieval-quality-summary">
          <b>{{ retrievalQuality.lowConfidence ? '低置信检索结果' : '检索质量通过' }}</b>
          <p>
            模式：{{ searchModeLabel(retrievalQuality.searchMode) }}；
            候选：{{ retrievalQuality.candidateCount || 0 }}；
            返回：{{ retrievalQuality.resultCount || sources.length }}；
            最佳置信：{{ scoreText(retrievalQuality.confidenceScore) }}；
            阈值：{{ scoreText(retrievalQuality.scoreThreshold) }} / 低置信 {{ scoreText(retrievalQuality.lowConfidenceThreshold) }}；
            重排：{{ rerankModeLabel(retrievalQuality.rerankMode) }}（{{ retrievalQuality.rerankLatencyMs || 0 }}ms）。
          </p>
          <p v-if="retrievalQuality.enhancedQueries?.length">增强查询：{{ retrievalQuality.enhancedQueries.join(' / ') }}</p>
          <p v-if="retrievalQuality.rerankErrorMessage">重排降级原因：{{ retrievalQuality.rerankErrorMessage }}</p>
          <p v-if="retrievalQuality.rejectReason">{{ retrievalQuality.rejectReason }}</p>
          <p v-if="retrievalQuality.qualityAdvice">{{ retrievalQuality.qualityAdvice }}</p>
        </div>
        <div v-else class="empty-state">配置参数后执行一次检索测试，命中结果会自动进入引用来源卡片。</div>
      </template>

      <template v-else>
        <article v-for="source in pagedSources" :key="source.chunkId" class="chunk-item">
          <div>
            <b>{{ source.documentName }} / 分片 {{ source.chunkNo }}</b>
            <StatusBadge :label="`最终 ${scoreText(source.score)}`" />
          </div>
          <span>{{ source.matchReason || '向量相似' }} · 向量 {{ scoreText(source.vectorScore) }} · 关键词 {{ scoreText(source.keywordScore) }} · 重排 {{ scoreText(source.rerankScore) }}</span>
          <span v-if="source.rankReason" class="rank-reason">{{ source.rankReason }}</span>
          <p class="highlighted-quote" v-html="sourceQuoteHtml(source)"></p>
        </article>
        <PaginationBar v-model:page="sourcePage" :total="sources.length" />
        <div v-if="sources.length === 0" class="empty-state">暂无引用来源，请先在检索测试卡片中执行查询。</div>
      </template>
    </div>
  </section>
</template>
