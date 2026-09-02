import { API_BASE_URL, applyAuthHeaders, request } from './http';

export interface KnowledgeSource {
  kbId: string;
  kbName: string;
  documentId: string;
  documentName: string;
  chunkId: string;
  chunkNo: number;
  quoteText: string;
  highlightedQuoteText?: string;
  score: number;
  vectorScore?: number;
  keywordScore?: number;
  rerankScore?: number;
  matchReason?: string;
  rankReason?: string;
  pageNo?: number;
  retrievalLogId?: string;
}

export interface KnowledgeBaseSummary {
  id: string;
  kbCode: string;
  kbName: string;
  description?: string;
  embeddingModelId?: string;
  embeddingModelName?: string;
  rerankModelId?: string;
  chunkStrategy: string;
  chunkSize: number;
  chunkOverlap: number;
  milvusCollectionName?: string;
  status: string;
  documentCount: number;
  chunkCount: number;
  embeddingCount: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface KnowledgeDocumentSummary {
  id: string;
  kbId: string;
  docName: string;
  docType: string;
  fileExt?: string;
  fileSize?: number;
  fileHash?: string;
  parseStatus: string;
  parseError?: string;
  processStage?: string;
  processStageLabel?: string;
  progressPercent?: number;
  lastMessage?: string;
  asyncTaskId?: string;
  embeddingFallbackUsed?: boolean;
  embeddingApi?: string;
  embeddingModelCode?: string;
  embeddingModelName?: string;
  embeddingDimension?: number;
  milvusSynced?: boolean;
  processLogs?: string[];
  chunkCount: number;
  embeddingCount: number;
  uploadedAt?: string;
}

export interface KnowledgeChunkSummary {
  id: string;
  documentId: string;
  chunkNo: number;
  title?: string;
  content: string;
  tokenCount: number;
  status: string;
  syncStatus?: string;
  createdAt?: string;
}

export interface KnowledgeChunkPage {
  records: KnowledgeChunkSummary[];
  total: number;
  pageNo: number;
  pageSize: number;
}

export interface KnowledgeBaseDetail extends KnowledgeBaseSummary {
  documents: KnowledgeDocumentSummary[];
  chunks: KnowledgeChunkSummary[];
}

export interface KnowledgeBaseRequest {
  kbCode?: string;
  kbName: string;
  description?: string;
  embeddingModelId?: string;
  rerankModelId?: string;
  chunkStrategy?: string;
  chunkSize?: number;
  chunkOverlap?: number;
  visibility?: string;
  status?: string;
}

export interface KnowledgeUploadResult {
  document: KnowledgeDocumentSummary;
  chunkCount: number;
  embeddingCount: number;
  milvusSynced: boolean;
  message: string;
  asyncAccepted?: boolean;
  asyncTaskId?: string;
}

export interface KnowledgeDirectUploadTicket {
  documentId: string;
  bucket: string;
  objectKey: string;
  uploadUrl: string;
  expiresAt: string;
}

export interface KnowledgeRetrievalResult {
  retrievalLogId: string;
  sources: KnowledgeSource[];
  latencyMs: number;
  searchMode?: string;
  rerankEnabled?: boolean;
  candidateCount?: number;
  resultCount?: number;
  confidenceScore?: number;
  lowConfidence?: boolean;
  answerable?: boolean;
  rejectReason?: string;
  qualityAdvice?: string;
  scoreThreshold?: number;
  lowConfidenceThreshold?: number;
  originalQuery?: string;
  canonicalQuery?: string;
  enhancedQueries?: string[];
  contextResolved?: boolean;
  rerankMode?: string;
  rerankModelId?: string;
  rerankLatencyMs?: number;
  rerankErrorMessage?: string;
}

export interface KnowledgeRetrievalOptions {
  query: string;
  topK?: number;
  candidateK?: number;
  scoreThreshold?: number;
  searchMode?: 'vector' | 'keyword' | 'hybrid';
  rerankEnabled?: boolean;
  vectorWeight?: number;
  keywordWeight?: number;
  documentIds?: string[];
  pageNo?: number;
  metadataKeyword?: string;
  lowConfidenceThreshold?: number;
  rejectLowConfidence?: boolean;
  queryRewriteEnabled?: boolean;
  multiQueryEnabled?: boolean;
  maxQueryVariants?: number;
  conversationContext?: string;
}

export interface KnowledgeVectorRebuildResult {
  kbId: string;
  kbName: string;
  chunkCount: number;
  asyncAccepted: boolean;
  asyncTaskId: string;
  message: string;
}

export interface AgentKnowledgeBindingSummary {
  agentId: string;
  knowledgeBaseId: string;
  kbName: string;
  retrievalConfig: string;
  enabled: boolean;
  createdAt?: string;
}

export interface AgentKnowledgeBindingOptions {
  topK?: number;
  scoreThreshold?: number;
  lowConfidenceThreshold?: number;
  trustedAnswerMode?: boolean;
  citationRequired?: boolean;
  minCitationCount?: number;
  queryRewriteEnabled?: boolean;
  multiQueryEnabled?: boolean;
  maxQueryVariants?: number;
}

export interface KnowledgeGovernanceOverview {
  knowledgeBaseCount: number;
  documentCount: number;
  parsedDocumentCount: number;
  failedDocumentCount: number;
  processingDocumentCount: number;
  chunkCount: number;
  embeddingCount: number;
  milvusFallbackCount: number;
  openIssueCount: number;
  highRiskIssueCount: number;
  staleDocumentCount: number;
  unboundKnowledgeBaseCount: number;
}

export interface KnowledgeQualityRow {
  kbId: string;
  kbName: string;
  documentCount: number;
  chunkCount: number;
  embeddingCount: number;
  failedDocumentCount: number;
  fallbackEmbeddingCount: number;
  agentBindingCount: number;
  lastUploadedAt?: string;
  qualityScore: number;
  riskLevel: string;
}

export interface KnowledgeGovernancePolicySummary {
  id: string;
  policyCode: string;
  policyName: string;
  kbId?: string;
  staleDays: number;
  minChunkTokens: number;
  maxChunkTokens: number;
  maxFailedDocuments: number;
  requireAgentBinding: boolean;
  requireMilvusSync: boolean;
  autoIssueEnabled: boolean;
  status: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface KnowledgeGovernancePolicyRequest {
  policyCode?: string;
  policyName: string;
  kbId?: string;
  staleDays?: number;
  minChunkTokens?: number;
  maxChunkTokens?: number;
  maxFailedDocuments?: number;
  requireAgentBinding?: boolean;
  requireMilvusSync?: boolean;
  autoIssueEnabled?: boolean;
  status?: string;
}

export interface KnowledgeGovernanceIssueSummary {
  id: string;
  kbId: string;
  kbName: string;
  documentId?: string;
  documentName?: string;
  chunkId?: string;
  issueType: string;
  severity: string;
  issueTitle: string;
  issueDetail?: string;
  evidence?: Record<string, unknown>;
  status: string;
  handlerUserId?: string;
  handledAt?: string;
  handleNote?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface KnowledgeGovernanceScanResult {
  asyncTaskId: string;
  status: string;
  message: string;
}

export async function fetchKnowledgeBases() {
  return request<KnowledgeBaseSummary[]>('/knowledge-bases');
}

export async function fetchKnowledgeBase(id: string) {
  return request<KnowledgeBaseDetail>(`/knowledge-bases/${id}`);
}

export async function fetchKnowledgeChunks(id: string, documentId: string, pageNo = 1, pageSize = 10) {
  const query = new URLSearchParams({ documentId, pageNo: String(pageNo), pageSize: String(pageSize) });
  return request<KnowledgeChunkPage>(`/knowledge-bases/${id}/chunks?${query.toString()}`);
}

export async function createKnowledgeBase(payload: KnowledgeBaseRequest) {
  return request<KnowledgeBaseDetail>('/knowledge-bases', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateKnowledgeBase(id: string, payload: KnowledgeBaseRequest) {
  return request<KnowledgeBaseDetail>(`/knowledge-bases/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function deleteKnowledgeBase(id: string) {
  return request<void>(`/knowledge-bases/${id}`, { method: 'DELETE' });
}

export async function uploadKnowledgeDocument(id: string, file: File) {
  // 大文件绕过 Spring Boot 直接写入 MinIO，避免占用后端连接和 JVM 堆内存。
  if (file.size >= 5 * 1024 * 1024) {
    const ticket = await request<KnowledgeDirectUploadTicket>(`/knowledge-bases/${id}/documents/direct-upload`, {
      method: 'POST',
      body: JSON.stringify({
        fileName: file.name,
        fileSize: file.size,
        contentType: file.type || 'application/octet-stream',
      }),
    });
    const uploadResponse = await fetch(ticket.uploadUrl, {
      method: 'PUT',
      headers: { 'Content-Type': file.type || 'application/octet-stream' },
      body: file,
    });
    if (!uploadResponse.ok) {
      throw new Error(`MinIO 直传失败，HTTP ${uploadResponse.status}`);
    }
    return request<KnowledgeUploadResult>(
      `/knowledge-bases/${id}/documents/${ticket.documentId}/direct-upload/complete`,
      { method: 'POST' },
    );
  }
  const form = new FormData();
  form.append('file', file);
  const headers = new Headers();
  applyAuthHeaders(headers);
  const response = await fetch(`${API_BASE_URL}/knowledge-bases/${id}/documents`, {
    method: 'POST',
    headers,
    body: form,
  });
  const body = await response.json();
  if (!response.ok || !body.success) {
    throw new Error(body.message || '文档上传失败');
  }
  return body.data as KnowledgeUploadResult;
}

export async function fetchKnowledgeDocumentStatus(kbId: string, documentId: string) {
  return request<KnowledgeDocumentSummary>(`/knowledge-bases/${kbId}/documents/${documentId}`);
}

export async function reprocessKnowledgeDocument(kbId: string, documentId: string) {
  return request<KnowledgeUploadResult>(`/knowledge-bases/${kbId}/documents/${documentId}/reprocess`, {
    method: 'POST',
  });
}

export async function retrievalTest(id: string, payload: string | KnowledgeRetrievalOptions, topK = 5, scoreThreshold = 0.65) {
  const body = typeof payload === 'string' ? { query: payload, topK, scoreThreshold } : payload;
  return request<KnowledgeRetrievalResult>(`/knowledge-bases/${id}/retrieval-test`, {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export async function rebuildKnowledgeVectors(id: string) {
  return request<KnowledgeVectorRebuildResult>(`/knowledge-bases/${id}/vectors/rebuild`, { method: 'POST' });
}

export async function fetchAgentKnowledgeBindings(agentId: string) {
  return request<AgentKnowledgeBindingSummary[]>(`/agents/${agentId}/knowledge-bases`);
}

export async function saveAgentKnowledgeBindings(
  agentId: string,
  knowledgeBaseIds: string[],
  options: AgentKnowledgeBindingOptions | number = {},
  legacyScoreThreshold = 0.65,
) {
  const body = typeof options === 'number'
    ? { knowledgeBaseIds, topK: options, scoreThreshold: legacyScoreThreshold }
    : { knowledgeBaseIds, ...options };
  return request<AgentKnowledgeBindingSummary[]>(`/agents/${agentId}/knowledge-bases`, {
    method: 'PUT',
    body: JSON.stringify(body),
  });
}

export async function fetchKnowledgeGovernanceOverview() {
  return request<KnowledgeGovernanceOverview>('/knowledge-governance/overview');
}

export async function fetchKnowledgeQualityRows() {
  return request<KnowledgeQualityRow[]>('/knowledge-governance/quality');
}

export async function scanKnowledgeGovernanceIssues() {
  return request<KnowledgeGovernanceScanResult>('/knowledge-governance/scan', { method: 'POST' });
}

export async function fetchKnowledgeGovernanceIssues(params: {
  status?: string;
  severity?: string;
  issueType?: string;
  kbId?: string;
  limit?: number;
} = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== '') {
      query.set(key, String(value));
    }
  });
  return request<KnowledgeGovernanceIssueSummary[]>(`/knowledge-governance/issues${query.toString() ? `?${query}` : ''}`);
}

export async function handleKnowledgeGovernanceIssue(id: string, status: string, handleNote?: string) {
  return request<KnowledgeGovernanceIssueSummary>(`/knowledge-governance/issues/${id}/handle`, {
    method: 'POST',
    body: JSON.stringify({ status, handleNote }),
  });
}

export async function fetchKnowledgeGovernancePolicies() {
  return request<KnowledgeGovernancePolicySummary[]>('/knowledge-governance/policies');
}

export async function createKnowledgeGovernancePolicy(payload: KnowledgeGovernancePolicyRequest) {
  return request<KnowledgeGovernancePolicySummary>('/knowledge-governance/policies', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateKnowledgeGovernancePolicy(id: string, payload: KnowledgeGovernancePolicyRequest) {
  return request<KnowledgeGovernancePolicySummary>(`/knowledge-governance/policies/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function deleteKnowledgeGovernancePolicy(id: string) {
  return request<void>(`/knowledge-governance/policies/${id}`, { method: 'DELETE' });
}
