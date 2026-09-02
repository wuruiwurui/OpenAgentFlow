<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { Plus, RefreshCw } from 'lucide-vue-next';
import PageHeader from '../../components/PageHeader.vue';
import PaginationBar from '../../components/PaginationBar.vue';
import StatCard from '../../components/StatCard.vue';
import StatusBadge from '../../components/StatusBadge.vue';
import { createKnowledgeBase, fetchKnowledgeBases, type KnowledgeBaseSummary } from '../../api/knowledge';
import { useOverlay } from '../../composables/useOverlay';
import { usePagination } from '../../composables/usePagination';

const router = useRouter();
const { toast } = useOverlay();
const knowledgeBases = ref<KnowledgeBaseSummary[]>([]);
const loading = ref(false);
const keyword = ref('');
const statusFilter = ref('');

const filteredKnowledgeBases = computed(() => knowledgeBases.value.filter((kb) => {
  const hitKeyword = !keyword.value || `${kb.kbName}${kb.description ?? ''}${kb.kbCode}`.toLowerCase().includes(keyword.value.toLowerCase());
  const hitStatus = !statusFilter.value || kb.status === statusFilter.value;
  return hitKeyword && hitStatus;
}));

const documentTotal = computed(() => knowledgeBases.value.reduce((sum, kb) => sum + (kb.documentCount || 0), 0));
const chunkTotal = computed(() => knowledgeBases.value.reduce((sum, kb) => sum + (kb.chunkCount || 0), 0));
const embeddingTotal = computed(() => knowledgeBases.value.reduce((sum, kb) => sum + (kb.embeddingCount || 0), 0));
const {
  currentPage: knowledgePage,
  pagedItems: pagedKnowledgeBases,
  resetPage: resetKnowledgePage,
} = usePagination(filteredKnowledgeBases);

onMounted(() => {
  void loadKnowledgeBases();
});

async function loadKnowledgeBases() {
  loading.value = true;
  try {
    knowledgeBases.value = await fetchKnowledgeBases();
  } finally {
    loading.value = false;
  }
}

async function handleCreate() {
  const name = window.prompt('请输入知识库名称');
  if (!name?.trim()) {
    return;
  }
  const detail = await createKnowledgeBase({
    kbName: name.trim(),
    description: '通过前端管理台创建',
    chunkSize: 512,
    chunkOverlap: 64,
    visibility: 'private',
    status: 'active',
  });
  toast('知识库已创建');
  router.push(`/knowledge/${detail.id}`);
}
</script>

<template>
  <PageHeader title="知识库" description="管理企业知识资产，支持文档解析、切片、向量化、Milvus 同步和引用追踪">
    <template #actions>
      <button class="secondary-button" type="button" :disabled="loading" @click="loadKnowledgeBases">
        <RefreshCw :size="16" /> 刷新
      </button>
      <button class="primary-button" type="button" @click="handleCreate">
        <Plus :size="16" /> 新建知识库
      </button>
    </template>
  </PageHeader>

  <section class="metric-grid">
    <StatCard label="知识库总数" :value="String(knowledgeBases.length)" detail="真实数据" icon="Library" tone="info" />
    <StatCard label="文档总数" :value="String(documentTotal)" detail="已上传文档" icon="Activity" tone="success" />
    <StatCard label="切片总数" :value="chunkTotal.toLocaleString()" detail="可检索分片" icon="Braces" tone="warning" />
    <StatCard label="向量总数" :value="embeddingTotal.toLocaleString()" detail="MySQL + Milvus" icon="Server" tone="neutral" />
  </section>

  <section class="filter-row">
    <select v-model="statusFilter" @change="resetKnowledgePage">
      <option value="">全部状态</option>
      <option value="active">启用</option>
      <option value="disabled">停用</option>
    </select>
    <input v-model="keyword" placeholder="搜索知识库名称、编码或描述" @input="resetKnowledgePage" />
  </section>

  <section class="section-block">
    <table class="data-table">
      <thead>
        <tr>
          <th>知识库名称</th>
          <th>描述</th>
          <th>文档数</th>
          <th>切片数</th>
          <th>向量模型</th>
          <th>重排模型</th>
          <th>Milvus 集合</th>
          <th>状态</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="kb in pagedKnowledgeBases" :key="kb.id" @click="router.push(`/knowledge/${kb.id}`)">
          <td><b>{{ kb.kbName }}</b><br /><span class="mono">{{ kb.kbCode }}</span></td>
          <td>{{ kb.description || '-' }}</td>
          <td>{{ kb.documentCount }}</td>
          <td>{{ kb.chunkCount.toLocaleString() }}</td>
          <td>{{ kb.embeddingModelName || kb.embeddingModelId || '-' }}</td>
          <td class="mono" :title="kb.rerankModelId || '未配置，使用规则重排'">{{ kb.rerankModelId || '规则重排' }}</td>
          <td class="mono">{{ kb.milvusCollectionName || '-' }}</td>
          <td><StatusBadge :label="kb.status === 'active' ? '启用' : kb.status" /></td>
        </tr>
      </tbody>
    </table>
    <PaginationBar v-model:page="knowledgePage" :total="filteredKnowledgeBases.length" />
    <div v-if="!loading && filteredKnowledgeBases.length === 0" class="empty-state">暂无知识库</div>
  </section>
</template>
