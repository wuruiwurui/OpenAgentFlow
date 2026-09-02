package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.config.OpenAgentFlowProperties;
import com.openagentflow.api.PageResult;
import com.openagentflow.domain.knowledge.AgentKnowledgeBindingRequest;
import com.openagentflow.domain.knowledge.AgentKnowledgeBindingSummary;
import com.openagentflow.domain.knowledge.KnowledgeBaseDetail;
import com.openagentflow.domain.knowledge.KnowledgeBaseRequest;
import com.openagentflow.domain.knowledge.KnowledgeBaseSummary;
import com.openagentflow.domain.knowledge.KnowledgeChunkSummary;
import com.openagentflow.domain.knowledge.KnowledgeDocumentSummary;
import com.openagentflow.domain.knowledge.EnhancedQueryPlan;
import com.openagentflow.domain.knowledge.KnowledgeRetrievalRequest;
import com.openagentflow.domain.knowledge.KnowledgeRetrievalResult;
import com.openagentflow.domain.knowledge.KnowledgeSource;
import com.openagentflow.domain.knowledge.KnowledgeUploadResult;
import com.openagentflow.domain.knowledge.KnowledgeVectorRebuildResult;
import com.openagentflow.domain.knowledge.RagRetrievalOutcome;
import com.openagentflow.domain.knowledge.RerankResult;
import com.openagentflow.entity.AsyncTaskEntity;
import com.openagentflow.entity.AgentEntity;
import com.openagentflow.entity.AgentKnowledgeBindingEntity;
import com.openagentflow.entity.KnowledgeBaseEntity;
import com.openagentflow.entity.KnowledgeChunkEntity;
import com.openagentflow.entity.KnowledgeDocumentEntity;
import com.openagentflow.entity.KnowledgeEmbeddingEntity;
import com.openagentflow.entity.KnowledgeRetrievalLogEntity;
import com.openagentflow.entity.ModelConfigEntity;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.mapper.AgentKnowledgeBindingMapper;
import com.openagentflow.mapper.AgentMapper;
import com.openagentflow.mapper.KnowledgeBaseMapper;
import com.openagentflow.mapper.KnowledgeChunkMapper;
import com.openagentflow.mapper.KnowledgeDocumentMapper;
import com.openagentflow.mapper.KnowledgeEmbeddingMapper;
import com.openagentflow.mapper.KnowledgeRetrievalLogMapper;
import com.openagentflow.mapper.ModelConfigMapper;
import com.openagentflow.security.AuthUserDetails;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 知识库应用服务。
 */
@Service
public class KnowledgeBaseService implements DistributedTaskHandler {

    /** 日志对象，用于输出文档处理进度和模型调用结果。 */
    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);

    /** 默认向量连接 ID。 */
    private static final String DEFAULT_VECTOR_CONNECTION_ID = "70000000-0000-0000-0000-000000000001";

    /** 默认知识库向量集合 ID。 */
    private static final String DEFAULT_VECTOR_COLLECTION_ID = "70000000-0000-0000-0000-000000000101";

    /** 知识库 Mapper。 */
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    /** 知识文档 Mapper。 */
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;

    /** 知识分片 Mapper。 */
    private final KnowledgeChunkMapper knowledgeChunkMapper;

    /** 知识向量 Mapper。 */
    private final KnowledgeEmbeddingMapper knowledgeEmbeddingMapper;

    /** 检索日志 Mapper。 */
    private final KnowledgeRetrievalLogMapper knowledgeRetrievalLogMapper;

    /** Agent 知识库绑定 Mapper。 */
    private final AgentKnowledgeBindingMapper agentKnowledgeBindingMapper;

    /** Agent Mapper。 */
    private final AgentMapper agentMapper;

    /** Agent 资源级权限服务。 */
    private final AgentAccessService agentAccessService;

    /** 工作空间治理服务。 */
    private final WorkspaceGovernanceService workspaceGovernanceService;

    /** 异步任务中心服务。 */
    private final AsyncTaskService asyncTaskService;

    /** 模型配置 Mapper。 */
    private final ModelConfigMapper modelConfigMapper;

    /** 文档解析服务。 */
    private final DocumentParseService documentParseService;

    /** 切片服务。 */
    private final KnowledgeChunkingService chunkingService;

    /** Embedding 服务。 */
    private final EmbeddingService embeddingService;

    /** Milvus 写入服务。 */
    private final MilvusKnowledgeVectorService milvusKnowledgeVectorService;

    /** OpenSearch BM25检索服务。 */
    private final KeywordSearchService keywordSearchService;

    /** 查询改写、多查询和会话指代消解策略。 */
    private final QueryEnhancementPolicy queryEnhancementPolicy;

    /** 多查询召回候选融合策略。 */
    private final MultiQueryFusionPolicy multiQueryFusionPolicy;

    /** 真实 Cross-Encoder 重排服务。 */
    private final CrossEncoderRerankService crossEncoderRerankService;

    /** JDBC 工具。 */
    private final JdbcTemplate jdbcTemplate;

    /** JSON 工具。 */
    private final ObjectMapper objectMapper;

    /** OpenAgentFlow 配置。 */
    private final OpenAgentFlowProperties properties;

    public KnowledgeBaseService(KnowledgeBaseMapper knowledgeBaseMapper,
                                KnowledgeDocumentMapper knowledgeDocumentMapper,
                                KnowledgeChunkMapper knowledgeChunkMapper,
                                KnowledgeEmbeddingMapper knowledgeEmbeddingMapper,
                                KnowledgeRetrievalLogMapper knowledgeRetrievalLogMapper,
                                AgentKnowledgeBindingMapper agentKnowledgeBindingMapper,
                                AgentMapper agentMapper,
                                AgentAccessService agentAccessService,
                                WorkspaceGovernanceService workspaceGovernanceService,
                                AsyncTaskService asyncTaskService,
                                ModelConfigMapper modelConfigMapper,
                                DocumentParseService documentParseService,
                                KnowledgeChunkingService chunkingService,
                                EmbeddingService embeddingService,
                                MilvusKnowledgeVectorService milvusKnowledgeVectorService,
                                KeywordSearchService keywordSearchService,
                                QueryEnhancementPolicy queryEnhancementPolicy,
                                MultiQueryFusionPolicy multiQueryFusionPolicy,
                                CrossEncoderRerankService crossEncoderRerankService,
                                JdbcTemplate jdbcTemplate,
                                ObjectMapper objectMapper,
                                OpenAgentFlowProperties properties) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.knowledgeChunkMapper = knowledgeChunkMapper;
        this.knowledgeEmbeddingMapper = knowledgeEmbeddingMapper;
        this.knowledgeRetrievalLogMapper = knowledgeRetrievalLogMapper;
        this.agentKnowledgeBindingMapper = agentKnowledgeBindingMapper;
        this.agentMapper = agentMapper;
        this.agentAccessService = agentAccessService;
        this.workspaceGovernanceService = workspaceGovernanceService;
        this.asyncTaskService = asyncTaskService;
        this.modelConfigMapper = modelConfigMapper;
        this.documentParseService = documentParseService;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.milvusKnowledgeVectorService = milvusKnowledgeVectorService;
        this.keywordSearchService = keywordSearchService;
        this.queryEnhancementPolicy = queryEnhancementPolicy;
        this.multiQueryFusionPolicy = multiQueryFusionPolicy;
        this.crossEncoderRerankService = crossEncoderRerankService;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * 查询知识库摘要列表。
     *
     * @return 知识库摘要列表
     */
    public List<KnowledgeBaseSummary> listKnowledgeBases() {
        return knowledgeBaseMapper.selectList(new LambdaQueryWrapper<KnowledgeBaseEntity>()
                        .isNull(KnowledgeBaseEntity::getDeletedAt)
                        .orderByDesc(KnowledgeBaseEntity::getUpdatedAt)
                        .last("limit 100"))
                .stream()
                .filter(this::canView)
                .map(this::toSummary)
                .toList();
    }

    /**
     * 查询知识库详情。
     *
     * @param id 知识库 ID
     * @return 知识库详情
     */
    public KnowledgeBaseDetail getKnowledgeBase(String id) {
        KnowledgeBaseEntity entity = requireKnowledgeBase(id);
        assertCanView(entity);
        KnowledgeBaseDetail detail = new KnowledgeBaseDetail();
        KnowledgeBaseSummary summary = toSummary(entity);
        copySummary(summary, detail);
        detail.setDocuments(listDocuments(id));
        detail.setChunks(listChunks(id, 10));
        return detail;
    }

    /**
     * 创建知识库。
     *
     * @param request 保存请求
     * @return 知识库详情
     */
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBaseDetail createKnowledgeBase(KnowledgeBaseRequest request) {
        String userId = currentUserIdOrThrow();
        KnowledgeBaseEntity entity = new KnowledgeBaseEntity();
        entity.setId(newId());
        fillKnowledgeBase(entity, request, true);
        entity.setOwnerUserId(userId);
        entity.setCreatedBy(userId);
        entity.setWorkspaceId(workspaceGovernanceService.attachResource(request.getWorkspaceId(), "knowledge_base", entity.getId(), userId));
        entity.setVersion(0L);
        knowledgeBaseMapper.insert(entity);
        return getKnowledgeBase(entity.getId());
    }

    /**
     * 更新知识库。
     *
     * @param id 知识库 ID
     * @param request 保存请求
     * @return 知识库详情
     */
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBaseDetail updateKnowledgeBase(String id, KnowledgeBaseRequest request) {
        KnowledgeBaseEntity entity = requireKnowledgeBase(id);
        assertCanManage(entity);
        fillKnowledgeBase(entity, request, false);
        knowledgeBaseMapper.updateById(entity);
        return getKnowledgeBase(id);
    }

    /**
     * 软删除知识库。
     *
     * @param id 知识库 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledgeBase(String id) {
        KnowledgeBaseEntity entity = requireKnowledgeBase(id);
        assertCanManage(entity);
        entity.setStatus("deleted");
        entity.setDeletedAt(LocalDateTime.now());
        knowledgeBaseMapper.updateById(entity);
    }

    /**
     * 上传、解析、切片并向量化文档。
     *
     * @param kbId 知识库 ID
     * @param file 上传文件
     * @return 上传处理结果
     */
    public KnowledgeUploadResult uploadDocument(String kbId, MultipartFile file) {
        KnowledgeBaseEntity kb = requireKnowledgeBase(kbId);
        assertCanManage(kb);
        if (file == null || file.isEmpty()) {
            throw new BusinessException("DOCUMENT_EMPTY", "上传文件不能为空");
        }
        try {
            byte[] bytes = file.getBytes();
            String fileName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "document.txt";
            String fileExt = fileExt(fileName);
            String fileHash = DigestUtils.md5DigestAsHex(bytes);
            KnowledgeDocumentEntity duplicate = findParsedDuplicateDocument(kbId, fileHash);
            if (duplicate != null) {
                KnowledgeUploadResult result = new KnowledgeUploadResult();
                result.setDocument(toDocumentSummary(duplicate));
                result.setChunkCount(count("knowledge_chunk", "document_id", duplicate.getId()));
                result.setEmbeddingCount(countByJoin(duplicate.getId()));
                result.setMilvusSynced(true);
                result.setMessage("检测到相同文件已解析，已复用已有分片和向量结果");
                return result;
            }
            String documentId = newId();
            String storageKey = saveUploadFile(kbId, documentId, fileName, bytes);

            KnowledgeDocumentEntity document = new KnowledgeDocumentEntity();
            document.setId(documentId);
            document.setKbId(kbId);
            document.setDocName(fileName);
            document.setDocType(fileExt);
            document.setFileExt(fileExt);
            document.setFileSize(file.getSize());
            document.setFileHash(fileHash);
            document.setStorageBucket("local");
            document.setStorageKey(storageKey);
            document.setSourceType("upload");
            document.setParseStatus("processing");
            document.setMetadata("{}");
            document.setUploadedBy(currentUserId());
            knowledgeDocumentMapper.insert(document);

            String text = documentParseService.parse(bytes, fileExt);
            List<KnowledgeChunkingService.ChunkSegment> segments = chunkingService.splitSegments(text, kb.getChunkStrategy(), kb.getChunkSize(), kb.getChunkOverlap());
            List<KnowledgeChunkingService.ChunkSegment> embeddingSegments = segments.stream()
                    .filter(KnowledgeChunkingService.ChunkSegment::embeddingEnabled)
                    .toList();
            if (embeddingSegments.isEmpty()) {
                throw new BusinessException("DOCUMENT_CHUNK_EMPTY", "文档没有生成有效分片");
            }

            ModelConfigEntity embeddingModel = embeddingService.resolveEmbeddingModel(kb.getEmbeddingModelId());
            if (!StringUtils.hasText(kb.getEmbeddingModelId())) {
                kb.setEmbeddingModelId(embeddingModel.getId());
                knowledgeBaseMapper.updateById(kb);
            }
            List<List<Double>> vectors = embeddingService.embed(embeddingModel, embeddingSegments.stream().map(KnowledgeChunkingService.ChunkSegment::content).toList());
            boolean allMilvusSynced = true;
            String milvusMessage = "";
            Map<Integer, String> parentChunkIds = new LinkedHashMap<>();
            int vectorIndex = 0;
            int chunkNo = 1;
            for (KnowledgeChunkingService.ChunkSegment segment : segments) {
                String parentChunkId = segment.parentOrdinal() == null ? null : parentChunkIds.get(segment.parentOrdinal());
                KnowledgeChunkEntity chunk = saveChunk(kb, document, segment, chunkNo++, parentChunkId, fileHash);
                if ("parent".equalsIgnoreCase(segment.level())) {
                    parentChunkIds.put(segment.ordinal(), chunk.getId());
                    continue;
                }
                List<Double> vector = vectors.get(vectorIndex++);
                KnowledgeEmbeddingEntity embedding = saveEmbedding(kb, chunk, embeddingModel, vector);
                try {
                    milvusKnowledgeVectorService.upsertKnowledgeChunk(kb.getMilvusCollectionName(), embedding, chunk, vector);
                    embedding.setSyncStatus("synced");
                    embedding.setLastSyncedAt(LocalDateTime.now());
                } catch (Exception exception) {
                    allMilvusSynced = false;
                    milvusMessage = exception.getMessage();
                    embedding.setSyncStatus("mysql_fallback");
                }
                knowledgeEmbeddingMapper.updateById(embedding);
            }
            document.setParseStatus("parsed");
            knowledgeDocumentMapper.updateById(document);
            createKnowledgeBaseVersionSnapshot(kb, "upload-" + System.currentTimeMillis());

            KnowledgeUploadResult result = new KnowledgeUploadResult();
            result.setDocument(toDocumentSummary(document));
            result.setChunkCount(segments.size());
            result.setEmbeddingCount(vectors.size());
            result.setMilvusSynced(allMilvusSynced);
            result.setMessage(allMilvusSynced ? "文档已解析、切片、向量化并写入 Milvus" : "Milvus 写入失败，已保留 MySQL 向量兜底：" + milvusMessage);
            return result;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("DOCUMENT_UPLOAD_FAILED", exception.getMessage());
        }
    }

    /**
     * 查询知识库文档列表。
     *
     * @param kbId 知识库 ID
     * @return 文档摘要列表
     */
    public List<KnowledgeDocumentSummary> listDocuments(String kbId) {
        KnowledgeBaseEntity kb = requireKnowledgeBase(kbId);
        assertCanView(kb);
        return knowledgeDocumentMapper.selectList(new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                        .eq(KnowledgeDocumentEntity::getKbId, kbId)
                        .orderByDesc(KnowledgeDocumentEntity::getUploadedAt))
                .stream()
                .map(this::toDocumentSummary)
                .toList();
    }

    /**
     * 查询知识库分片列表。
     *
     * @param kbId 知识库 ID
     * @param limit 返回上限
     * @return 分片摘要列表
     */
    public List<KnowledgeChunkSummary> listChunks(String kbId, int limit) {
        KnowledgeBaseEntity kb = requireKnowledgeBase(kbId);
        assertCanView(kb);
        return knowledgeChunkMapper.selectList(new LambdaQueryWrapper<KnowledgeChunkEntity>()
                        .eq(KnowledgeChunkEntity::getKbId, kbId)
                        .orderByDesc(KnowledgeChunkEntity::getCreatedAt)
                        .last("limit " + Math.max(1, Math.min(limit, 200))))
                .stream()
                .map(this::toChunkSummary)
                .toList();
    }

    /** 分页查询指定知识库或文档的全部切片。 */
    public PageResult<KnowledgeChunkSummary> listChunks(String kbId, String documentId, Integer pageNo, Integer pageSize) {
        KnowledgeBaseEntity kb = requireKnowledgeBase(kbId);
        assertCanView(kb);
        int page = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        LambdaQueryWrapper<KnowledgeChunkEntity> countWrapper = new LambdaQueryWrapper<KnowledgeChunkEntity>()
                .eq(KnowledgeChunkEntity::getKbId, kbId)
                .eq(StringUtils.hasText(documentId), KnowledgeChunkEntity::getDocumentId, documentId);
        Long total = knowledgeChunkMapper.selectCount(countWrapper);
        List<KnowledgeChunkSummary> rows = knowledgeChunkMapper.selectList(new LambdaQueryWrapper<KnowledgeChunkEntity>()
                        .eq(KnowledgeChunkEntity::getKbId, kbId)
                        .eq(StringUtils.hasText(documentId), KnowledgeChunkEntity::getDocumentId, documentId)
                        .orderByAsc(KnowledgeChunkEntity::getChunkNo)
                        .last("LIMIT " + ((page - 1) * size) + "," + size))
                .stream().map(this::toChunkSummary).toList();
        return new PageResult<>(rows, total == null ? 0 : total, page, size);
    }

    /**
     * 执行单个知识库检索测试。
     *
     * @param kbId 知识库 ID
     * @param request 检索请求
     * @return 检索结果
     */
    public KnowledgeRetrievalResult retrievalTest(String kbId, KnowledgeRetrievalRequest request) {
        KnowledgeBaseEntity kb = requireKnowledgeBase(kbId);
        assertCanView(kb);
        return retrieveFromKnowledgeBase(kb, null, null, request.getQuery(), optionsFromRequest(request));
    }

    /**
     * 查询 Agent 已绑定知识库。
     *
     * @param agentId Agent ID
     * @return 绑定摘要列表
     */
    public List<AgentKnowledgeBindingSummary> listAgentBindings(String agentId) {
        AgentEntity agent = requireAgent(agentId);
        agentAccessService.assertCanView(agent);
        return agentKnowledgeBindingMapper.selectList(new LambdaQueryWrapper<AgentKnowledgeBindingEntity>()
                        .eq(AgentKnowledgeBindingEntity::getAgentId, agentId)
                        .eq(AgentKnowledgeBindingEntity::getEnabled, true))
                .stream()
                .map(this::toBindingSummary)
                .toList();
    }

    /**
     * 保存 Agent 知识库绑定。
     *
     * @param agentId Agent ID
     * @param request 绑定请求
     * @return 保存后的绑定列表
     */
    @Transactional(rollbackFor = Exception.class)
    public List<AgentKnowledgeBindingSummary> saveAgentBindings(String agentId, AgentKnowledgeBindingRequest request) {
        AgentEntity agent = requireAgent(agentId);
        agentAccessService.assertCanManage(agent);
        agentKnowledgeBindingMapper.delete(new LambdaQueryWrapper<AgentKnowledgeBindingEntity>()
                .eq(AgentKnowledgeBindingEntity::getAgentId, agentId));
        List<String> ids = request.getKnowledgeBaseIds() == null ? List.of() : request.getKnowledgeBaseIds();
        Set<String> uniqueIds = new LinkedHashSet<>(ids);
        double scoreThreshold = clampScore(request.getScoreThreshold(), properties.getRag().getDefaultScoreThreshold());
        double lowConfidenceThreshold = clampScore(request.getLowConfidenceThreshold(), Math.max(scoreThreshold, 0.62D));
        Map<String, Object> configMap = new LinkedHashMap<>();
        configMap.put("topK", normalizeTopK(request.getTopK()));
        configMap.put("candidateK", normalizeCandidateK(null, normalizeTopK(request.getTopK())));
        configMap.put("scoreThreshold", scoreThreshold);
        configMap.put("lowConfidenceThreshold", lowConfidenceThreshold);
        configMap.put("searchMode", "hybrid");
        configMap.put("rerankEnabled", true);
        configMap.put("rejectLowConfidence", true);
        configMap.put("trustedAnswerMode", request.getTrustedAnswerMode() == null || Boolean.TRUE.equals(request.getTrustedAnswerMode()));
        configMap.put("citationRequired", request.getCitationRequired() == null || Boolean.TRUE.equals(request.getCitationRequired()));
        configMap.put("minCitationCount", normalizeMinCitationCount(request.getMinCitationCount()));
        configMap.put("vectorWeight", 0.72D);
        configMap.put("keywordWeight", 0.28D);
        configMap.put("queryRewriteEnabled", Boolean.TRUE.equals(properties.getRag().getQueryRewriteEnabled()));
        configMap.put("multiQueryEnabled", Boolean.TRUE.equals(properties.getRag().getMultiQueryEnabled()));
        configMap.put("maxQueryVariants", properties.getRag().getMaxQueryVariants());
        String config = toJson(configMap);
        for (String kbId : uniqueIds) {
            KnowledgeBaseEntity kb = requireKnowledgeBase(kbId);
            assertCanView(kb);
            AgentKnowledgeBindingEntity binding = new AgentKnowledgeBindingEntity();
            binding.setAgentId(agentId);
            binding.setKnowledgeBaseId(kbId);
            binding.setRetrievalConfig(config);
            binding.setEnabled(true);
            agentKnowledgeBindingMapper.insert(binding);
        }
        return listAgentBindings(agentId);
    }

    /**
     * 根据 Agent 绑定的知识库执行 RAG 检索。
     *
     * @param agent Agent 实体
     * @param query 用户问题
     * @param runId 运行 ID
     * @return 引用来源列表
     */
    public List<KnowledgeSource> retrieveForAgent(AgentEntity agent, String query, String runId) {
        return retrieveForAgentWithPolicy(agent, query, runId).getSources();
    }

    /**
     * 根据 Agent 绑定的知识库执行 RAG 检索，并返回可信回答判定结果。
     *
     * @param agent Agent 实体
     * @param query 用户问题
     * @param runId 运行 ID
     * @return RAG 聚合检索结果
     */
    public RagRetrievalOutcome retrieveForAgentWithPolicy(AgentEntity agent, String query, String runId) {
        return retrieveForAgentWithPolicy(agent, query, runId, "");
    }

    /**
     * 根据 Agent 绑定的知识库执行 RAG 检索，并支持用最近会话消解查询指代。
     *
     * @param agent Agent 实体
     * @param query 用户问题
     * @param runId 运行 ID
     * @param conversationContext 最近会话上下文，可为空
     * @return RAG 聚合检索结果
     */
    public RagRetrievalOutcome retrieveForAgentWithPolicy(AgentEntity agent,
                                                          String query,
                                                          String runId,
                                                          String conversationContext) {
        RagRetrievalOutcome outcome = new RagRetrievalOutcome();
        outcome.setSources(List.of());
        outcome.setTrustedAnswerMode(false);
        outcome.setCitationRequired(false);
        outcome.setMinCitationCount(0);
        outcome.setAnswerable(true);
        outcome.setRejectReason("");
        outcome.setConfidenceScore(0D);
        outcome.setScoreThreshold(properties.getRag().getDefaultScoreThreshold());
        outcome.setLowConfidenceThreshold(Math.max(properties.getRag().getDefaultScoreThreshold(), 0.62D));
        outcome.setQualityAdvice("");
        outcome.setOriginalQuery(query);
        outcome.setEnhancedQueries(List.of());
        outcome.setCanonicalQuery(query);
        outcome.setContextResolved(false);
        outcome.setRerankMode("disabled");
        outcome.setRerankErrorMessage("");
        outcome.setRerankLatencyMs(0);
        if (agent == null || !StringUtils.hasText(query)) {
            return outcome;
        }
        List<AgentKnowledgeBindingEntity> bindings = agentKnowledgeBindingMapper.selectList(new LambdaQueryWrapper<AgentKnowledgeBindingEntity>()
                .eq(AgentKnowledgeBindingEntity::getAgentId, agent.getId())
                .eq(AgentKnowledgeBindingEntity::getEnabled, true));
        List<KnowledgeSource> allSources = new ArrayList<>();
        boolean trustedMode = false;
        boolean citationRequired = false;
        int minCitationCount = 0;
        double confidenceScore = 0D;
        double scoreThreshold = properties.getRag().getDefaultScoreThreshold();
        double lowConfidenceThreshold = Math.max(scoreThreshold, 0.62D);
        List<String> rejectReasons = new ArrayList<>();
        List<String> qualityAdvices = new ArrayList<>();
        for (AgentKnowledgeBindingEntity binding : bindings) {
            KnowledgeBaseEntity kb = knowledgeBaseMapper.selectById(binding.getKnowledgeBaseId());
            if (kb == null || kb.getDeletedAt() != null || !"active".equalsIgnoreCase(kb.getStatus())) {
                continue;
            }
            if (!canView(kb)) {
                qualityAdvices.add(kb.getKbName() + "：当前用户无权访问，已从本次检索中过滤");
                continue;
            }
            Map<String, Object> config = parseMap(binding.getRetrievalConfig());
            RetrievalOptions options = optionsFromConfig(config);
            trustedMode = trustedMode || options.trustedAnswerMode;
            citationRequired = citationRequired || options.citationRequired;
            minCitationCount = Math.max(minCitationCount, options.minCitationCount);
            scoreThreshold = Math.max(scoreThreshold, options.scoreThreshold);
            lowConfidenceThreshold = Math.max(lowConfidenceThreshold, options.lowConfidenceThreshold);
            options.conversationContext = conversationContext;
            KnowledgeRetrievalResult result = retrieveFromKnowledgeBase(kb, agent.getId(), runId, query, options);
            if (outcome.getEnhancedQueries().isEmpty()) {
                outcome.setEnhancedQueries(result.getEnhancedQueries());
                outcome.setCanonicalQuery(result.getCanonicalQuery());
            }
            outcome.setContextResolved(Boolean.TRUE.equals(outcome.getContextResolved())
                    || Boolean.TRUE.equals(result.getContextResolved()));
            if ("cross_encoder".equals(result.getRerankMode())) {
                outcome.setRerankMode("cross_encoder");
                outcome.setRerankModelId(result.getRerankModelId());
                outcome.setRerankLatencyMs(Math.max(outcome.getRerankLatencyMs(),
                        result.getRerankLatencyMs() == null ? 0 : result.getRerankLatencyMs()));
            } else if ("disabled".equals(outcome.getRerankMode()) && result.getRerankMode() != null) {
                outcome.setRerankMode(result.getRerankMode());
                outcome.setRerankModelId(result.getRerankModelId());
                outcome.setRerankLatencyMs(result.getRerankLatencyMs());
            }
            if (StringUtils.hasText(result.getRerankErrorMessage())) {
                String previousError = safeText(outcome.getRerankErrorMessage());
                outcome.setRerankErrorMessage(previousError.isBlank()
                        ? kb.getKbName() + "：" + result.getRerankErrorMessage()
                        : previousError + "；" + kb.getKbName() + "：" + result.getRerankErrorMessage());
            }
            confidenceScore = Math.max(confidenceScore, result.getConfidenceScore() == null ? 0D : result.getConfidenceScore());
            if (StringUtils.hasText(result.getRejectReason())) {
                rejectReasons.add(kb.getKbName() + "：" + result.getRejectReason());
            }
            if (StringUtils.hasText(result.getQualityAdvice())) {
                qualityAdvices.add(kb.getKbName() + "：" + result.getQualityAdvice());
            }
            // 低置信拒答开启时不把弱相关片段注入模型，避免模型基于不可靠资料继续编造答案。
            if (Boolean.FALSE.equals(result.getAnswerable()) && options.rejectLowConfidence) {
                continue;
            }
            allSources.addAll(result.getSources());
        }
        List<KnowledgeSource> sources = allSources.stream()
                .sorted(Comparator.comparing(KnowledgeSource::getScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(properties.getRag().getDefaultTopK())
                .toList();
        boolean answerable = !trustedMode || sources.size() >= Math.max(1, minCitationCount);
        if (trustedMode && !answerable && rejectReasons.isEmpty()) {
            rejectReasons.add("可信回答模式要求至少命中 " + Math.max(1, minCitationCount) + " 条可靠引用来源，当前仅命中 " + sources.size() + " 条");
        }
        outcome.setSources(sources);
        outcome.setTrustedAnswerMode(trustedMode);
        outcome.setCitationRequired(citationRequired);
        outcome.setMinCitationCount(minCitationCount);
        outcome.setAnswerable(answerable);
        outcome.setRejectReason(answerable ? "" : String.join("；", rejectReasons));
        outcome.setConfidenceScore(confidenceScore);
        outcome.setScoreThreshold(scoreThreshold);
        outcome.setLowConfidenceThreshold(lowConfidenceThreshold);
        outcome.setQualityAdvice(String.join("；", qualityAdvices));
        return outcome;
    }

    /**
     * 判断 Agent 是否绑定了启用中的知识库。
     *
     * @param agentId Agent ID
     * @return 是否存在启用绑定
     */
    public boolean hasEnabledKnowledgeBindings(String agentId) {
        if (!StringUtils.hasText(agentId)) {
            return false;
        }
        return agentKnowledgeBindingMapper.selectCount(new LambdaQueryWrapper<AgentKnowledgeBindingEntity>()
                .eq(AgentKnowledgeBindingEntity::getAgentId, agentId)
                .eq(AgentKnowledgeBindingEntity::getEnabled, true)) > 0;
    }

    /**
     * 提交知识库向量重建任务。
     *
     * @param kbId 知识库 ID
     * @return 异步任务受理结果
     */
    public KnowledgeVectorRebuildResult rebuildKnowledgeVectors(String kbId) {
        KnowledgeBaseEntity kb = requireKnowledgeBase(kbId);
        assertCanManage(kb);
        int chunkCount = count("knowledge_chunk", "kb_id", kb.getId());
        AsyncTaskEntity task = asyncTaskService.createTask(
                "重建知识库向量：" + kb.getKbName(),
                "KNOWLEDGE_VECTOR_REBUILD",
                "knowledge_base",
                kb.getId(),
                "knowledge_base",
                kb.getId(),
                kb.getWorkspaceId(),
                Map.of("kbId", kb.getId(), "kbName", kb.getKbName(), "chunkCount", chunkCount));
        KnowledgeVectorRebuildResult result = new KnowledgeVectorRebuildResult();
        result.setKbId(kb.getId());
        result.setKbName(kb.getKbName());
        result.setChunkCount(chunkCount);
        result.setAsyncAccepted(true);
        result.setAsyncTaskId(task.getId());
        result.setMessage("向量重建任务已提交，可在任务中心查看进度");
        return result;
    }

    /**
     * 执行单知识库质量增强检索。
     *
     * @param kb 知识库实体
     * @param agentId Agent ID
     * @param runId 运行 ID
     * @param query 查询文本
     * @param options 检索参数
     * @return 检索结果
     */
    private KnowledgeRetrievalResult retrieveFromKnowledgeBase(KnowledgeBaseEntity kb,
                                                              String agentId,
                                                              String runId,
                                                              String query,
                                                              RetrievalOptions options) {
        Instant startedAt = Instant.now();
        EnhancedQueryPlan queryPlan = buildEnhancedQueryPlan(query, options);
        options.enhancedQueries = queryPlan.getVariants();
        options.originalQuery = queryPlan.getOriginalQuery();
        options.canonicalQuery = queryPlan.getCanonicalQuery();
        options.contextResolved = queryPlan.isContextResolved();
        options.rerankModelId = kb.getRerankModelId();
        options.rerankMode = options.rerankEnabled
                ? (StringUtils.hasText(kb.getRerankModelId()) ? "cache" : "rule")
                : "disabled";
        options.rerankLatencyMs = 0;
        String cacheKey = retrievalCacheKey(kb.getId(), query, options);
        KnowledgeRetrievalResult cached = readRetrievalCache(cacheKey, options);
        if (cached != null) {
            String cacheAdvice = StringUtils.hasText(cached.getQualityAdvice())
                    ? cached.getQualityAdvice()
                    : "命中检索缓存，已复用热点问题引用来源";
            String logId = saveRetrievalLog(
                    kb,
                    agentId,
                    runId,
                    query,
                    List.of(),
                    options,
                    cached.getSources(),
                    cached.getCandidateCount() == null ? cached.getSources().size() : cached.getCandidateCount(),
                    cached.getConfidenceScore() == null ? 0D : cached.getConfidenceScore(),
                    false,
                    "",
                    cacheAdvice,
                    startedAt);
            cached.getSources().forEach(source -> source.setRetrievalLogId(logId));
            cached.setRetrievalLogId(logId);
            cached.setLatencyMs((int) Duration.between(startedAt, Instant.now()).toMillis());
            applyRetrievalMetadata(cached, queryPlan, options);
            return cached;
        }
        List<String> variants = options.enhancedQueries.isEmpty() ? List.of(query) : options.enhancedQueries;
        List<List<Double>> queryVectors = shouldUseVector(options.searchMode)
                ? embeddingService.embed(embeddingService.resolveEmbeddingModel(kb.getEmbeddingModelId()), variants)
                : List.of();
        List<List<MultiQueryFusionPolicy.RankedHit<RetrievalCandidate>>> queryHits = new ArrayList<>();
        for (int index = 0; index < variants.size(); index++) {
            String variant = variants.get(index);
            List<String> variantTerms = extractQueryTerms(variant);
            List<Double> queryVector = queryVectors.size() > index ? queryVectors.get(index) : List.of();
            List<RetrievalCandidate> recalled = recallCandidates(kb, variant, variantTerms, queryVector, options);
            List<MultiQueryFusionPolicy.RankedHit<RetrievalCandidate>> rankedHits = recalled.stream()
                    .sorted(Comparator.comparing((RetrievalCandidate item) -> item.baseScore).reversed())
                    .limit(options.candidateK)
                    .map(candidate -> new MultiQueryFusionPolicy.RankedHit<>(
                            candidate.chunk.getId(), candidate, candidate.baseScore, recalled.indexOf(candidate) + 1))
                    .toList();
            queryHits.add(rankedHits);
        }
        List<MultiQueryFusionPolicy.FusedHit<RetrievalCandidate>> fusedHits = multiQueryFusionPolicy.fuse(
                queryHits, options.candidateK);
        List<RetrievalCandidate> candidates = fusedHits.stream().map(fused -> {
            RetrievalCandidate candidate = fused.payload();
            // 多个查询同时命中同一分片时只做有限加权，避免查询变体数量改变分数尺度。
            candidate.baseScore = clamp(fused.bestScore() + Math.min(0.06D, (fused.hitCount() - 1) * 0.02D), 0D, 1D);
            return candidate;
        }).toList();
        List<String> terms = variants.stream()
                .flatMap(variant -> extractQueryTerms(variant).stream())
                .distinct()
                .toList();
        List<RetrievalCandidate> rankedCandidates = rankCandidates(candidates,
                options.canonicalQuery, terms, options);
        RerankResult rerankResult = applyCrossEncoderRerank(kb, options.canonicalQuery, rankedCandidates, options);
        options.rerankMode = rerankResult.getMode();
        options.rerankModelId = rerankResult.getModelId();
        options.rerankLatencyMs = rerankResult.getLatencyMs();
        options.rerankErrorMessage = rerankResult.getErrorMessage();
        rankedCandidates = applyRerankScores(rankedCandidates, rerankResult);
        List<KnowledgeSource> sources = rankedCandidates.stream()
                .filter(candidate -> candidate.finalScore >= options.scoreThreshold)
                .limit(options.topK)
                .map(candidate -> toSource(kb, candidate, query, terms))
                .toList();
        Double confidenceScore = sources.isEmpty() ? 0D : sources.getFirst().getScore();
        boolean lowConfidence = sources.isEmpty() || confidenceScore < options.lowConfidenceThreshold;
        String rejectReason = lowConfidence
                ? "未召回达到低置信阈值的可靠片段，建议拒答或引导用户补充问题"
                : "";
        String qualityAdvice = buildQualityAdvice(options, rankedCandidates.size(), sources.size(), lowConfidence);
        List<Double> queryVector = queryVectors.isEmpty() ? List.of() : queryVectors.getFirst();
        String logId = saveRetrievalLog(kb, agentId, runId, query, queryVector, options, sources, rankedCandidates.size(), confidenceScore, lowConfidence, rejectReason, qualityAdvice, startedAt);
        sources.forEach(source -> source.setRetrievalLogId(logId));
        if (!sources.isEmpty()) {
            writeRetrievalCache(cacheKey, kb.getId(), query, options, sources, confidenceScore);
        }

        KnowledgeRetrievalResult result = new KnowledgeRetrievalResult();
        result.setRetrievalLogId(logId);
        result.setSources(sources);
        result.setLatencyMs((int) Duration.between(startedAt, Instant.now()).toMillis());
        result.setSearchMode(options.searchMode);
        result.setRerankEnabled(options.rerankEnabled);
        result.setCandidateCount(rankedCandidates.size());
        result.setResultCount(sources.size());
        result.setConfidenceScore(confidenceScore);
        result.setLowConfidence(lowConfidence);
        result.setAnswerable(!lowConfidence || !options.rejectLowConfidence);
        result.setRejectReason(rejectReason);
        result.setScoreThreshold(options.scoreThreshold);
        result.setLowConfidenceThreshold(options.lowConfidenceThreshold);
        result.setQualityAdvice(qualityAdvice);
        applyRetrievalMetadata(result, queryPlan, options);
        return result;
    }

    /** 构建本次检索使用的查询增强计划，关闭开关时退化为单个原始查询。 */
    private EnhancedQueryPlan buildEnhancedQueryPlan(String query, RetrievalOptions options) {
        EnhancedQueryPlan plan = queryEnhancementPolicy.enhance(
                query, options.conversationContext, options.maxQueryVariants);
        if (!options.queryRewriteEnabled) {
            plan.setCanonicalQuery(plan.getOriginalQuery());
            plan.setVariants(List.of(plan.getOriginalQuery()));
            plan.setSynonymExpansions(Map.of());
            plan.setContextResolved(false);
            return plan;
        }
        if (!options.multiQueryEnabled) {
            plan.setVariants(List.of(plan.getCanonicalQuery()));
        }
        return plan;
    }

    /**
     * 调用真实 Cross-Encoder，并把失败明确标记为规则降级。
     */
    private RerankResult applyCrossEncoderRerank(KnowledgeBaseEntity kb,
                                                 String query,
                                                 List<RetrievalCandidate> candidates,
                                                 RetrievalOptions options) {
        RerankResult result = new RerankResult();
        result.setModelId(kb.getRerankModelId());
        if (!options.rerankEnabled) {
            result.setMode("disabled");
            return result;
        }
        if (!StringUtils.hasText(kb.getRerankModelId()) || candidates.isEmpty()) {
            result.setMode("rule");
            return result;
        }
        int limit = Math.min(candidates.size(), properties.getRag().getRerankCandidateLimit() == null
                ? 30 : properties.getRag().getRerankCandidateLimit());
        List<String> documents = candidates.stream()
                .limit(limit)
                .map(candidate -> expandParentChunk(candidate.chunk))
                .map(KnowledgeChunkEntity::getContent)
                .map(this::safeText)
                .toList();
        return crossEncoderRerankService.rerank(kb.getRerankModelId(), query, documents);
    }

    /** 将真实重排分数写回候选，未成功时保留规则重排结果。 */
    private List<RetrievalCandidate> applyRerankScores(List<RetrievalCandidate> candidates, RerankResult result) {
        if (result == null || !result.isSuccess() || result.getScores() == null) {
            return candidates;
        }
        int scoreCount = Math.min(candidates.size(), result.getScores().size());
        for (int index = 0; index < scoreCount; index++) {
            Double score = result.getScores().get(index);
            if (score != null) {
                candidates.get(index).finalScore = clamp(score, 0D, 1D);
            }
        }
        return candidates.stream()
                .sorted(Comparator.comparing((RetrievalCandidate item) -> item.finalScore).reversed())
                .toList();
    }

    /** 将增强查询和重排运行信息写入检索结果。 */
    private void applyRetrievalMetadata(KnowledgeRetrievalResult result,
                                       EnhancedQueryPlan queryPlan,
                                       RetrievalOptions options) {
        result.setOriginalQuery(queryPlan.getOriginalQuery());
        result.setCanonicalQuery(queryPlan.getCanonicalQuery());
        result.setEnhancedQueries(queryPlan.getVariants());
        result.setContextResolved(queryPlan.isContextResolved());
        result.setRerankMode(options.rerankMode);
        result.setRerankModelId(options.rerankModelId);
        result.setRerankLatencyMs(options.rerankLatencyMs);
        result.setRerankErrorMessage(options.rerankErrorMessage);
    }

    /**
     * 召回候选分片。
     *
     * @param kb 知识库
     * @param query 查询文本
     * @param terms 查询关键词
     * @param queryVector 查询向量
     * @param options 检索参数
     * @return 候选列表
     */
    private List<RetrievalCandidate> recallCandidates(KnowledgeBaseEntity kb,
                                                      String query,
                                                      List<String> terms,
                                                      List<Double> queryVector,
                                                      RetrievalOptions options) {
        List<RetrievalCandidate> candidates = new ArrayList<>();
        if ("keyword".equals(options.searchMode)) {
            if (keywordSearchService.isEnabled()) {
                try {
                    List<KeywordSearchService.KeywordHit> hits = keywordSearchService.search(kb.getId(), query, options.candidateK);
                    if (!hits.isEmpty()) {
                        Map<String, Double> scores = hits.stream().collect(java.util.stream.Collectors.toMap(
                                KeywordSearchService.KeywordHit::chunkId, KeywordSearchService.KeywordHit::score, Math::max));
                        List<KnowledgeChunkEntity> indexedChunks = knowledgeChunkMapper.selectBatchIds(scores.keySet());
                        for (KnowledgeChunkEntity chunk : indexedChunks) {
                            if (chunk != null && "active".equalsIgnoreCase(chunk.getStatus()) && matchesRetrievalFilters(chunk, options)) {
                                candidates.add(buildCandidate(chunk, 0D, scores.getOrDefault(chunk.getId(), 0D), options));
                            }
                        }
                        return candidates;
                    }
                } catch (Exception exception) {
                    log.warn("OpenSearch关键词召回失败，回退MySQL轻量检索：kbId={}, error={}", kb.getId(), exception.getMessage());
                }
            }
            List<KnowledgeChunkEntity> chunks = knowledgeChunkMapper.selectList(new LambdaQueryWrapper<KnowledgeChunkEntity>()
                    .eq(KnowledgeChunkEntity::getKbId, kb.getId())
                    .eq(KnowledgeChunkEntity::getStatus, "active")
                    .and(wrapper -> wrapper.ne(KnowledgeChunkEntity::getChunkLevel, "parent").or().isNull(KnowledgeChunkEntity::getChunkLevel))
                    .last("limit 2000"));
            for (KnowledgeChunkEntity chunk : chunks) {
                if (!matchesRetrievalFilters(chunk, options)) {
                    continue;
                }
                candidates.add(buildCandidate(chunk, 0D, keywordScore(query, terms, chunk), options));
            }
            return candidates;
        }

        Map<String, Double> externalKeywordScores = new LinkedHashMap<>();
        Map<String, Integer> keywordRanks = new LinkedHashMap<>();
        if ("hybrid".equals(options.searchMode) && keywordSearchService.isEnabled()) {
            try {
                List<KeywordSearchService.KeywordHit> keywordHits = keywordSearchService.search(kb.getId(), query, options.candidateK);
                double maxKeywordScore = keywordHits.stream().mapToDouble(KeywordSearchService.KeywordHit::score).max().orElse(1D);
                for (int index = 0; index < keywordHits.size(); index++) {
                    KeywordSearchService.KeywordHit hit = keywordHits.get(index);
                    externalKeywordScores.put(hit.chunkId(), hit.score() / Math.max(0.000001D, maxKeywordScore));
                    keywordRanks.put(hit.chunkId(), index + 1);
                }
            } catch (Exception exception) {
                log.warn("OpenSearch混合召回失败，回退本地关键词得分：kbId={}, error={}", kb.getId(), exception.getMessage());
            }
        }
        if ("hybrid".equals(options.searchMode) && externalKeywordScores.isEmpty()) {
            // 本地未启用OpenSearch时仍保留精确词法通道，避免PDF原句只依赖向量TopK而被遗漏。
            List<KnowledgeChunkEntity> lexicalChunks = knowledgeChunkMapper.selectList(new LambdaQueryWrapper<KnowledgeChunkEntity>()
                    .eq(KnowledgeChunkEntity::getKbId, kb.getId())
                    .eq(KnowledgeChunkEntity::getStatus, "active")
                    .and(wrapper -> wrapper.ne(KnowledgeChunkEntity::getChunkLevel, "parent").or().isNull(KnowledgeChunkEntity::getChunkLevel))
                    .last("limit 2000"));
            List<Map.Entry<String, Double>> lexicalHits = lexicalChunks.stream()
                    .map(chunk -> Map.entry(chunk.getId(), keywordScore(query, terms, chunk)))
                    .filter(entry -> entry.getValue() > 0D)
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(options.candidateK)
                    .toList();
            for (int index = 0; index < lexicalHits.size(); index++) {
                Map.Entry<String, Double> hit = lexicalHits.get(index);
                externalKeywordScores.put(hit.getKey(), hit.getValue());
                keywordRanks.put(hit.getKey(), index + 1);
            }
        }
        try {
            List<MilvusKnowledgeVectorService.VectorHit> vectorHits = milvusKnowledgeVectorService.searchKnowledgeChunks(
                    kb.getMilvusCollectionName(), kb.getId(), queryVector, options.candidateK);
            Map<String, MilvusKnowledgeVectorService.VectorHit> hitsByChunkId = new LinkedHashMap<>();
            Map<String, Integer> vectorRanks = new LinkedHashMap<>();
            for (int index = 0; index < vectorHits.size(); index++) {
                MilvusKnowledgeVectorService.VectorHit hit = vectorHits.get(index);
                if (StringUtils.hasText(hit.chunkId())) {
                    hitsByChunkId.put(hit.chunkId(), hit);
                    vectorRanks.put(hit.chunkId(), index + 1);
                }
            }
            Set<String> candidateIds = new LinkedHashSet<>(hitsByChunkId.keySet());
            candidateIds.addAll(externalKeywordScores.keySet());
            if (candidateIds.isEmpty()) {
                return candidates;
            }
            List<KnowledgeChunkEntity> chunks = knowledgeChunkMapper.selectBatchIds(candidateIds);
            for (KnowledgeChunkEntity chunk : chunks) {
                if (chunk == null || !"active".equalsIgnoreCase(chunk.getStatus())
                        || "parent".equalsIgnoreCase(chunk.getChunkLevel())
                        || !matchesRetrievalFilters(chunk, options)) {
                    continue;
                }
                MilvusKnowledgeVectorService.VectorHit hit = hitsByChunkId.get(chunk.getId());
                double vectorScore = hit == null ? 0D : hit.score();
                double keywordScore = externalKeywordScores.getOrDefault(chunk.getId(), keywordScore(query, terms, chunk));
                RetrievalCandidate candidate = buildCandidate(chunk, vectorScore, keywordScore, options);
                if ("hybrid".equals(options.searchMode)) {
                    // RRF 对不同检索引擎的分值尺度不敏感，适合融合 Milvus COSINE 与 BM25 排名。
                    candidate.baseScore = reciprocalRankFusion(vectorRanks.get(chunk.getId()), keywordRanks.get(chunk.getId()));
                    candidate.finalScore = candidate.baseScore;
                }
                candidates.add(candidate);
            }
            return candidates;
        } catch (Exception exception) {
            if (!Boolean.TRUE.equals(properties.getRag().getAllowMysqlVectorFallback())) {
                throw new BusinessException("RAG_VECTOR_RECALL_FAILED", "Milvus 向量召回失败，请检查向量服务状态：" + exception.getMessage());
            }
            log.warn("Milvus向量召回失败，开发模式回退MySQL扫描：kbId={}, error={}", kb.getId(), exception.getMessage());
        }
        return recallFromMysqlVectors(kb, query, terms, queryVector, options, externalKeywordScores);
    }

    /**
     * 仅供本地开发兼容的 MySQL 向量扫描逻辑，生产环境默认禁止。
     */
    private List<RetrievalCandidate> recallFromMysqlVectors(KnowledgeBaseEntity kb,
                                                            String query,
                                                            List<String> terms,
                                                            List<Double> queryVector,
                                                            RetrievalOptions options,
                                                            Map<String, Double> externalKeywordScores) {
        List<RetrievalCandidate> candidates = new ArrayList<>();
        List<KnowledgeEmbeddingEntity> embeddings = knowledgeEmbeddingMapper.selectList(new LambdaQueryWrapper<KnowledgeEmbeddingEntity>()
                .eq(KnowledgeEmbeddingEntity::getKbId, kb.getId())
                .isNotNull(KnowledgeEmbeddingEntity::getEmbeddingJson)
                .last("limit 2000"));
        Map<String, KnowledgeEmbeddingEntity> embeddingByChunkId = new LinkedHashMap<>();
        embeddings.forEach(item -> embeddingByChunkId.put(item.getChunkId(), item));
        List<KnowledgeChunkEntity> chunks = knowledgeChunkMapper.selectBatchIds(embeddingByChunkId.keySet());
        for (KnowledgeChunkEntity chunk : chunks) {
            if (chunk == null || !"active".equalsIgnoreCase(chunk.getStatus())) {
                continue;
            }
            if ("parent".equalsIgnoreCase(chunk.getChunkLevel())) {
                continue;
            }
            if (!matchesRetrievalFilters(chunk, options)) {
                continue;
            }
            KnowledgeEmbeddingEntity embedding = embeddingByChunkId.get(chunk.getId());
            double vectorScore = cosine(queryVector, parseVector(embedding.getEmbeddingJson()));
            double keywordScore = externalKeywordScores.getOrDefault(chunk.getId(), keywordScore(query, terms, chunk));
            candidates.add(buildCandidate(chunk, vectorScore, keywordScore, options));
        }
        return candidates;
    }

    /**
     * 使用 k=60 的倒数排名融合，并归一化到零到一范围。
     */
    private double reciprocalRankFusion(Integer vectorRank, Integer keywordRank) {
        double score = 0D;
        if (vectorRank != null) {
            score += 1D / (60D + vectorRank);
        }
        if (keywordRank != null) {
            score += 1D / (60D + keywordRank);
        }
        return clamp(score / (2D / 61D), 0D, 1D);
    }

    /**
     * 构建候选项。
     *
     * @param chunk 分片
     * @param vectorScore 向量得分
     * @param keywordScore 关键词得分
     * @param options 检索参数
     * @return 候选项
     */
    private RetrievalCandidate buildCandidate(KnowledgeChunkEntity chunk,
                                              double vectorScore,
                                              double keywordScore,
                                              RetrievalOptions options) {
        RetrievalCandidate candidate = new RetrievalCandidate();
        candidate.chunk = chunk;
        candidate.vectorScore = vectorScore;
        candidate.keywordScore = keywordScore;
        if ("vector".equals(options.searchMode)) {
            candidate.baseScore = vectorScore;
        } else if ("keyword".equals(options.searchMode)) {
            candidate.baseScore = keywordScore;
        } else {
            double totalWeight = Math.max(0.01D, options.vectorWeight + options.keywordWeight);
            candidate.baseScore = (vectorScore * options.vectorWeight + keywordScore * options.keywordWeight) / totalWeight;
        }
        candidate.finalScore = candidate.baseScore;
        return candidate;
    }

    /**
     * 排序并可选重排候选分片。
     *
     * @param candidates 原始候选
     * @param query 查询文本
     * @param terms 查询关键词
     * @param options 检索参数
     * @return 排序后的候选
     */
    private List<RetrievalCandidate> rankCandidates(List<RetrievalCandidate> candidates,
                                                    String query,
                                                    List<String> terms,
                                                    RetrievalOptions options) {
        List<RetrievalCandidate> recalled = candidates.stream()
                .sorted(Comparator.comparing((RetrievalCandidate item) -> item.baseScore).reversed())
                .limit(options.candidateK)
                .toList();
        if (!options.rerankEnabled) {
            return recalled;
        }
        for (RetrievalCandidate candidate : recalled) {
            double phraseBoost = containsNormalized(candidate.chunk.getContent(), query) ? 0.05D : 0D;
            double titleBoost = containsAny(candidate.chunk.getTitle(), terms) ? 0.04D : 0D;
            double lengthPenalty = candidate.chunk.getTokenCount() != null && candidate.chunk.getTokenCount() < 20 ? 0.04D : 0D;
            // 本地规则重排会轻量提升关键词密集、标题命中和短语直接命中的候选，避免纯向量得分误召回。
            candidate.finalScore = clamp(candidate.baseScore + candidate.keywordScore * 0.12D + phraseBoost + titleBoost - lengthPenalty, 0D, 1D);
        }
        return recalled.stream()
                .sorted(Comparator.comparing((RetrievalCandidate item) -> item.finalScore).reversed())
                .toList();
    }

    /**
     * 将候选项转换为引用来源。
     *
     * @param kb 知识库
     * @param candidate 候选项
     * @return 引用来源
     */
    private KnowledgeSource toSource(KnowledgeBaseEntity kb, RetrievalCandidate candidate, String query, List<String> terms) {
        KnowledgeChunkEntity displayChunk = expandParentChunk(candidate.chunk);
        KnowledgeSource source = toSource(kb, displayChunk, candidate.finalScore);
        source.setChunkId(candidate.chunk.getId());
        source.setChunkNo(candidate.chunk.getChunkNo());
        source.setParentChunkId(candidate.chunk.getParentChunkId());
        source.setChunkLevel(candidate.chunk.getChunkLevel());
        source.setSectionTitle(candidate.chunk.getSectionTitle());
        source.setSectionPath(candidate.chunk.getSectionPath());
        source.setVectorScore(candidate.vectorScore);
        source.setKeywordScore(candidate.keywordScore);
        source.setRerankScore(candidate.finalScore);
        source.setMatchReason(matchReason(candidate));
        source.setRankReason(rankReason(candidate, terms));
        source.setHighlightedQuoteText(highlightQuote(displayChunk.getContent(), query, terms));
        return source;
    }

    /**
     * 命中子分片时展开父分片上下文，用更完整的上下文给模型回答。
     *
     * @param chunk 命中的候选分片
     * @return 用于展示和回答的分片
     */
    private KnowledgeChunkEntity expandParentChunk(KnowledgeChunkEntity chunk) {
        if (chunk == null || !StringUtils.hasText(chunk.getParentChunkId())) {
            return chunk;
        }
        KnowledgeChunkEntity parent = knowledgeChunkMapper.selectById(chunk.getParentChunkId());
        return parent == null || !"active".equalsIgnoreCase(parent.getStatus()) ? chunk : parent;
    }

    /**
     * 生成人类可读的命中原因。
     *
     * @param candidate 候选项
     * @return 命中原因
     */
    private String matchReason(RetrievalCandidate candidate) {
        if (candidate.keywordScore > 0.25D && candidate.vectorScore > 0.25D) {
            return "向量相似 + 关键词命中";
        }
        if (candidate.keywordScore > 0.25D) {
            return "关键词命中";
        }
        return "向量相似";
    }

    /**
     * 判断分片是否满足生产级过滤条件。
     *
     * @param chunk 分片实体
     * @param options 检索参数
     * @return 是否满足
     */
    private boolean matchesRetrievalFilters(KnowledgeChunkEntity chunk, RetrievalOptions options) {
        if (chunk == null) {
            return false;
        }
        if (!options.documentIds.isEmpty() && !options.documentIds.contains(chunk.getDocumentId())) {
            return false;
        }
        if (options.pageNo != null && !options.pageNo.equals(chunk.getPageNo())) {
            return false;
        }
        if (StringUtils.hasText(options.metadataKeyword)) {
            String keyword = normalizeText(options.metadataKeyword);
            String target = normalizeText(safeText(chunk.getTitle()) + " " + safeText(chunk.getContent()) + " " + safeText(chunk.getMetadata()));
            return target.contains(keyword);
        }
        return true;
    }

    /**
     * 生成重排原因说明。
     *
     * @param candidate 候选项
     * @return 重排说明
     */
    private String rankReason(RetrievalCandidate candidate, List<String> terms) {
        List<String> reasons = new ArrayList<>();
        if (candidate.keywordScore > 0.25D) {
            reasons.add("关键词覆盖较高");
        }
        if (candidate.vectorScore > 0.25D) {
            reasons.add("语义相似度较高");
        }
        if (containsAny(candidate.chunk.getTitle(), terms)) {
            reasons.add("标题字段参与排序");
        }
        if (candidate.chunk.getTokenCount() != null && candidate.chunk.getTokenCount() < 20) {
            reasons.add("短分片已轻微降权");
        }
        return reasons.isEmpty() ? "按综合得分排序" : String.join("，", reasons);
    }

    /**
     * 构建引用高亮文本。
     *
     * @param content 原始分片文本
     * @param query 查询文本
     * @param terms 查询关键词
     * @return HTML 安全的高亮文本
     */
    private String highlightQuote(String content, String query, List<String> terms) {
        String highlighted = escapeHtml(content);
        List<String> highlightTerms = new ArrayList<>();
        if (StringUtils.hasText(query) && query.trim().length() >= 2 && query.trim().length() <= 30) {
            highlightTerms.add(query.trim());
        }
        terms.stream()
                .filter(StringUtils::hasText)
                .filter(term -> term.length() >= 2 && term.length() <= 18)
                .limit(8)
                .forEach(highlightTerms::add);
        for (String term : highlightTerms) {
            highlighted = highlighted.replaceAll("(?i)(" + java.util.regex.Pattern.quote(escapeHtml(term)) + ")", "<mark>$1</mark>");
        }
        return highlighted;
    }

    /**
     * 构建生产级检索质量建议。
     *
     * @param options 检索参数
     * @param candidateCount 候选数量
     * @param resultCount 结果数量
     * @param lowConfidence 是否低置信
     * @return 质量建议
     */
    private String buildQualityAdvice(RetrievalOptions options, int candidateCount, int resultCount, boolean lowConfidence) {
        List<String> advice = new ArrayList<>();
        if (candidateCount == 0) {
            advice.add("未召回候选分片，可放宽文档/页码/元数据过滤条件，或检查文档是否完成向量化");
        }
        if (resultCount == 0) {
            advice.add("最终结果为空，可降低得分阈值、增大候选数或补充知识库内容");
        }
        if (lowConfidence) {
            advice.add("最佳结果低于低置信阈值，建议开启拒答或引导用户补充更具体的问题");
        }
        if ("vector".equals(options.searchMode) && options.keywordWeight <= 0.01D) {
            advice.add("纯向量检索适合语义问答；若专业名词较多，建议切换混合检索");
        }
        if ("keyword".equals(options.searchMode)) {
            advice.add("关键词检索适合精确术语；若表达方式多样，建议切换混合检索");
        }
        return advice.isEmpty() ? "检索质量正常，可在引用来源中查看分数明细和高亮片段" : String.join("；", advice);
    }

    /**
     * 执行知识库向量重建任务。
     *
     * @param taskId 异步任务 ID
     * @param kbId 知识库 ID
     */
    private Map<String, Object> rebuildKnowledgeVectorsTask(String taskId, String kbId) {
        try {
            KnowledgeBaseEntity kb = knowledgeBaseMapper.selectById(kbId);
            if (kb == null || kb.getDeletedAt() != null) {
                throw new BusinessException("KNOWLEDGE_BASE_NOT_FOUND", "知识库不存在，无法重建向量");
            }
            List<KnowledgeChunkEntity> chunks = knowledgeChunkMapper.selectList(new LambdaQueryWrapper<KnowledgeChunkEntity>()
                    .eq(KnowledgeChunkEntity::getKbId, kbId)
                    .eq(KnowledgeChunkEntity::getStatus, "active")
                    .and(wrapper -> wrapper.ne(KnowledgeChunkEntity::getChunkLevel, "parent").or().isNull(KnowledgeChunkEntity::getChunkLevel))
                    .orderByAsc(KnowledgeChunkEntity::getChunkNo));
            if (chunks.isEmpty()) {
                return Map.of("chunkCount", 0, "embeddingCount", 0, "milvusSynced", false);
            }

            ModelConfigEntity embeddingModel = embeddingService.resolveEmbeddingModel(kb.getEmbeddingModelId());
            List<KnowledgeEmbeddingEntity> savedEmbeddings = new ArrayList<>();
            List<KnowledgeChunkEntity> savedChunks = new ArrayList<>();
            List<List<Double>> savedVectors = new ArrayList<>();
            int batchSize = 64;
            for (int start = 0; start < chunks.size(); start += batchSize) {
                if (asyncTaskService.isCancelRequested(taskId)) {
                    asyncTaskService.markCanceled(taskId, "用户取消向量重建任务");
                    return Map.of("canceled", true, "kbId", kbId);
                }
                int end = Math.min(start + batchSize, chunks.size());
                List<KnowledgeChunkEntity> batchChunks = chunks.subList(start, end);
                EmbeddingBatchResult embeddingResult = embeddingService.embedWithTrace(embeddingModel, batchChunks.stream().map(KnowledgeChunkEntity::getContent).toList());
                List<List<Double>> vectors = embeddingResult.getVectors();
                for (int index = 0; index < batchChunks.size(); index++) {
                    KnowledgeChunkEntity chunk = batchChunks.get(index);
                    KnowledgeEmbeddingEntity embedding = saveOrUpdateEmbedding(kb, chunk, embeddingModel, vectors.get(index));
                    savedEmbeddings.add(embedding);
                    savedChunks.add(chunk);
                    savedVectors.add(vectors.get(index));
                }
                int progress = 10 + (int) ((end * 65.0D) / chunks.size());
                asyncTaskService.updateProgress(taskId, "embedding", "已重建 " + end + " / " + chunks.size() + " 个分片向量", progress, Map.of(
                        "processed", end,
                        "total", chunks.size(),
                        "embeddingModel", embeddingModel.getModelName(),
                        "fallbackUsed", Boolean.TRUE.equals(embeddingResult.getFallbackUsed()),
                        "embeddingApi", safeString(embeddingResult.getEmbeddingApi())
                ));
            }

            boolean milvusSynced = syncRebuiltVectors(taskId, kb, savedEmbeddings, savedChunks, savedVectors);
            return Map.of(
                    "chunkCount", chunks.size(),
                    "embeddingCount", savedEmbeddings.size(),
                    "milvusSynced", milvusSynced
            );
        } catch (Exception exception) {
            throw new IllegalStateException("知识库向量重建失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * 返回 Kafka 任务类型。
     */
    @Override
    public String taskType() {
        return "KNOWLEDGE_VECTOR_REBUILD";
    }

    /**
     * 执行 Kafka 知识库向量重建任务。
     */
    @Override
    public Map<String, Object> executeDistributedTask(AsyncTaskEntity task) {
        return rebuildKnowledgeVectorsTask(task.getId(), task.getSourceId());
    }

    /**
     * 保存或更新单个分片向量。
     *
     * @param kb 知识库
     * @param chunk 分片
     * @param model Embedding 模型
     * @param vector 向量
     * @return 向量记录
     */
    private KnowledgeEmbeddingEntity saveOrUpdateEmbedding(KnowledgeBaseEntity kb,
                                                           KnowledgeChunkEntity chunk,
                                                           ModelConfigEntity model,
                                                           List<Double> vector) {
        KnowledgeEmbeddingEntity embedding = knowledgeEmbeddingMapper.selectOne(new LambdaQueryWrapper<KnowledgeEmbeddingEntity>()
                .eq(KnowledgeEmbeddingEntity::getChunkId, chunk.getId())
                .last("limit 1"));
        boolean create = embedding == null;
        if (create) {
            embedding = new KnowledgeEmbeddingEntity();
            embedding.setId(newId());
        }
        embedding.setChunkId(chunk.getId());
        embedding.setKbId(kb.getId());
        embedding.setModelId(model.getId());
        embedding.setVectorCollectionId(StringUtils.hasText(kb.getVectorCollectionId()) ? kb.getVectorCollectionId() : DEFAULT_VECTOR_COLLECTION_ID);
        embedding.setMilvusCollectionName(kb.getMilvusCollectionName());
        embedding.setVectorPrimaryKey("chunk_" + chunk.getId().replace("-", ""));
        embedding.setSyncStatus("pending");
        embedding.setLastSyncedAt(null);
        embedding.setEmbeddingJson(toJson(vector));
        embedding.setEmbeddingDim(vector.size());
        embedding.setContentHash(DigestUtils.md5DigestAsHex(chunk.getContent().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        if (create) {
            knowledgeEmbeddingMapper.insert(embedding);
        } else {
            knowledgeEmbeddingMapper.updateById(embedding);
        }
        return embedding;
    }

    /**
     * 同步重建后的向量到 Milvus。
     *
     * @param taskId 异步任务 ID
     * @param kb 知识库
     * @param embeddings 向量记录
     * @param chunks 分片列表
     * @param vectors 向量列表
     * @return 是否全部同步成功
     */
    private boolean syncRebuiltVectors(String taskId,
                                       KnowledgeBaseEntity kb,
                                       List<KnowledgeEmbeddingEntity> embeddings,
                                       List<KnowledgeChunkEntity> chunks,
                                       List<List<Double>> vectors) {
        asyncTaskService.updateProgress(taskId, "milvus_sync", "开始写入 Milvus 或保留 MySQL 向量兜底", 82, Map.of("embeddingCount", embeddings.size()));
        try {
            milvusKnowledgeVectorService.upsertKnowledgeChunks(kb.getMilvusCollectionName(), embeddings, chunks, vectors);
            LocalDateTime syncedAt = LocalDateTime.now();
            for (KnowledgeEmbeddingEntity embedding : embeddings) {
                embedding.setSyncStatus("synced");
                embedding.setLastSyncedAt(syncedAt);
                knowledgeEmbeddingMapper.updateById(embedding);
            }
            asyncTaskService.updateProgress(taskId, "milvus_done", "Milvus 向量写入完成", 95, Map.of("milvusSynced", true));
            return true;
        } catch (Exception exception) {
            for (KnowledgeEmbeddingEntity embedding : embeddings) {
                embedding.setSyncStatus("mysql_fallback");
                knowledgeEmbeddingMapper.updateById(embedding);
            }
            asyncTaskService.updateProgress(taskId, "mysql_fallback", "Milvus 写入失败，已保留 MySQL 向量兜底：" + exception.getMessage(), 95, Map.of(
                    "milvusSynced", false,
                    "error", exception.getMessage()
            ));
            return false;
        }
    }

    /**
     * 填充知识库实体。
     *
     * @param entity 知识库实体
     * @param request 保存请求
     * @param create 是否创建场景
     */
    private void fillKnowledgeBase(KnowledgeBaseEntity entity, KnowledgeBaseRequest request, boolean create) {
        String code = StringUtils.hasText(request.getKbCode()) ? request.getKbCode().trim() : slugify(request.getKbName());
        entity.setKbCode(create ? uniqueKbCode(code) : code);
        entity.setKbName(request.getKbName().trim());
        entity.setDescription(request.getDescription());
        if (!create && StringUtils.hasText(request.getWorkspaceId())) {
            entity.setWorkspaceId(workspaceGovernanceService.attachResource(request.getWorkspaceId(), "knowledge_base", entity.getId(), entity.getOwnerUserId()));
        }
        ModelConfigEntity embeddingModel = embeddingService.resolveEmbeddingModel(request.getEmbeddingModelId());
        entity.setEmbeddingModelId(embeddingModel.getId());
        entity.setRerankModelId(StringUtils.hasText(request.getRerankModelId()) ? request.getRerankModelId().trim() : null);
        entity.setVectorConnectionId(DEFAULT_VECTOR_CONNECTION_ID);
        entity.setVectorCollectionId(DEFAULT_VECTOR_COLLECTION_ID);
        entity.setMilvusCollectionName(properties.getMilvus().getDefaultKnowledgeCollection());
        entity.setChunkStrategy(StringUtils.hasText(request.getChunkStrategy()) ? request.getChunkStrategy() : "parent_child");
        entity.setChunkSize(request.getChunkSize() == null ? 512 : request.getChunkSize());
        entity.setChunkOverlap(request.getChunkOverlap() == null ? 64 : request.getChunkOverlap());
        entity.setVisibility(StringUtils.hasText(request.getVisibility()) ? request.getVisibility() : "private");
        entity.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "active");
    }

    /**
     * 保存切片记录。
     *
     * @param kb 知识库
     * @param document 文档
     * @param content 切片内容
     * @param chunkNo 切片序号
     * @return 切片实体
     */
    private KnowledgeChunkEntity saveChunk(KnowledgeBaseEntity kb, KnowledgeDocumentEntity document, String content, int chunkNo) {
        KnowledgeChunkingService.ChunkSegment segment = new KnowledgeChunkingService.ChunkSegment(
                content, "child", null, chunkNo, "", "", chunkNo, 0, content == null ? 0 : content.length(), true);
        return saveChunk(kb, document, segment, chunkNo, null, document.getFileHash());
    }

    /**
     * 保存带结构元数据的切片记录。
     *
     * @param kb 知识库
     * @param document 文档
     * @param segment 切片段落
     * @param chunkNo 全局切片序号
     * @param parentChunkId 父分片 ID
     * @param sourceHash 来源文档哈希
     * @return 切片实体
     */
    private KnowledgeChunkEntity saveChunk(KnowledgeBaseEntity kb,
                                           KnowledgeDocumentEntity document,
                                           KnowledgeChunkingService.ChunkSegment segment,
                                           int chunkNo,
                                           String parentChunkId,
                                           String sourceHash) {
        String content = segment.content();
        String contentHash = DigestUtils.md5DigestAsHex(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        KnowledgeChunkEntity chunk = new KnowledgeChunkEntity();
        chunk.setId(newId());
        chunk.setKbId(kb.getId());
        chunk.setDocumentId(document.getId());
        chunk.setChunkNo(chunkNo);
        chunk.setParentChunkId(parentChunkId);
        chunk.setChunkLevel(segment.level());
        chunk.setTitle(StringUtils.hasText(segment.sectionTitle()) ? segment.sectionTitle() : document.getDocName() + " #" + chunkNo);
        chunk.setSectionTitle(segment.sectionTitle());
        chunk.setSectionPath(segment.sectionPath());
        chunk.setParagraphNo(segment.paragraphNo());
        chunk.setContent(content);
        chunk.setTokenCount(chunkingService.estimateTokens(content));
        chunk.setStartOffset(segment.startOffset() == null ? 0 : segment.startOffset());
        chunk.setEndOffset(segment.endOffset() == null ? content.length() : segment.endOffset());
        chunk.setStrategyVersion("rag-chunk-v2");
        chunk.setContentHash(contentHash);
        chunk.setSourceHash(sourceHash);
        chunk.setMetadata(toJson(Map.of(
                "chunkStrategy", safeString(kb.getChunkStrategy()),
                "chunkLevel", safeString(segment.level()),
                "parentChunkId", safeString(parentChunkId),
                "sectionTitle", safeString(segment.sectionTitle()),
                "sectionPath", safeString(segment.sectionPath()),
                "contentHash", contentHash,
                "sourceHash", safeString(sourceHash)
        )));
        chunk.setStatus("active");
        knowledgeChunkMapper.insert(chunk);
        return chunk;
    }

    /**
     * 查找同一知识库下已解析的相同文件，用于避免重复切片和重复向量化。
     *
     * @param kbId 知识库 ID
     * @param fileHash 文件哈希
     * @return 已解析文档，找不到返回 null
     */
    private KnowledgeDocumentEntity findParsedDuplicateDocument(String kbId, String fileHash) {
        if (!StringUtils.hasText(fileHash)) {
            return null;
        }
        return knowledgeDocumentMapper.selectOne(new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                .eq(KnowledgeDocumentEntity::getKbId, kbId)
                .eq(KnowledgeDocumentEntity::getFileHash, fileHash)
                .eq(KnowledgeDocumentEntity::getParseStatus, "parsed")
                .orderByDesc(KnowledgeDocumentEntity::getUploadedAt)
                .last("limit 1"));
    }

    /**
     * 创建知识库版本快照，用于后续回滚、对比和灰度绑定。
     *
     * @param kb 知识库
     * @param versionNo 版本号
     */
    private void createKnowledgeBaseVersionSnapshot(KnowledgeBaseEntity kb, String versionNo) {
        try {
            int documentCount = count("knowledge_document", "kb_id", kb.getId());
            int chunkCount = count("knowledge_chunk", "kb_id", kb.getId());
            int embeddingCount = count("knowledge_embedding", "kb_id", kb.getId());
            jdbcTemplate.update("""
                    INSERT INTO knowledge_base_version
                      (id, kb_id, version_no, version_name, document_count, chunk_count, embedding_count,
                       chunk_strategy, chunk_size, chunk_overlap, snapshot_json, status, created_by)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON), 'active', ?)
                    ON DUPLICATE KEY UPDATE
                      document_count = VALUES(document_count),
                      chunk_count = VALUES(chunk_count),
                      embedding_count = VALUES(embedding_count),
                      snapshot_json = VALUES(snapshot_json),
                      status = 'active'
                    """,
                    newId(),
                    kb.getId(),
                    versionNo,
                    "文档处理快照",
                    documentCount,
                    chunkCount,
                    embeddingCount,
                    kb.getChunkStrategy(),
                    kb.getChunkSize(),
                    kb.getChunkOverlap(),
                    toJson(Map.of("kbId", kb.getId(), "documentCount", documentCount, "chunkCount", chunkCount, "embeddingCount", embeddingCount)),
                    currentUserId());
        } catch (Exception ignored) {
            // 版本快照失败不影响文档主处理链路。
        }
    }

    /**
     * 保存向量记录。
     *
     * @param kb 知识库
     * @param chunk 分片
     * @param model Embedding 模型
     * @param vector 向量
     * @return 向量实体
     */
    private KnowledgeEmbeddingEntity saveEmbedding(KnowledgeBaseEntity kb,
                                                   KnowledgeChunkEntity chunk,
                                                   ModelConfigEntity model,
                                                   List<Double> vector) {
        KnowledgeEmbeddingEntity embedding = new KnowledgeEmbeddingEntity();
        embedding.setId(newId());
        embedding.setChunkId(chunk.getId());
        embedding.setKbId(kb.getId());
        embedding.setModelId(model.getId());
        embedding.setVectorCollectionId(kb.getVectorCollectionId());
        embedding.setMilvusCollectionName(kb.getMilvusCollectionName());
        embedding.setVectorPrimaryKey("chunk_" + chunk.getId().replace("-", ""));
        embedding.setSyncStatus("pending");
        embedding.setEmbeddingJson(toJson(vector));
        embedding.setEmbeddingDim(vector.size());
        embedding.setContentHash(DigestUtils.md5DigestAsHex(chunk.getContent().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        knowledgeEmbeddingMapper.insert(embedding);
        return embedding;
    }

    /**
     * 保存检索日志。
     *
     * @param kb 知识库
     * @param agentId Agent ID
     * @param runId 运行 ID
     * @param query 查询文本
     * @param queryVector 查询向量
     * @param options 检索参数
     * @param sources 引用来源
     * @param candidateCount 候选数量
     * @param lowConfidence 是否低置信
     * @param rejectReason 拒答原因
     * @param startedAt 开始时间
     * @return 检索日志 ID
     */
    private String saveRetrievalLog(KnowledgeBaseEntity kb,
                                    String agentId,
                                    String runId,
                                    String query,
                                    List<Double> queryVector,
                                    RetrievalOptions options,
                                    List<KnowledgeSource> sources,
                                    int candidateCount,
                                    Double confidenceScore,
                                    boolean lowConfidence,
                                    String rejectReason,
                                    String qualityAdvice,
                                    Instant startedAt) {
        KnowledgeRetrievalLogEntity log = new KnowledgeRetrievalLogEntity();
        log.setId(newId());
        log.setKbId(kb.getId());
        log.setAgentId(agentId);
        log.setRunId(runId);
        log.setVectorCollectionId(kb.getVectorCollectionId());
        log.setMilvusCollectionName(kb.getMilvusCollectionName());
        log.setQueryText(query);
        log.setQueryEmbeddingJson(toJson(queryVector));
        Map<String, Object> searchParams = new LinkedHashMap<>();
        searchParams.put("mode", options.searchMode);
        searchParams.put("candidateK", options.candidateK);
        searchParams.put("candidateCount", candidateCount);
        searchParams.put("vectorWeight", options.vectorWeight);
        searchParams.put("keywordWeight", options.keywordWeight);
        searchParams.put("documentIds", options.documentIds);
        searchParams.put("pageNo", options.pageNo);
        searchParams.put("metadataKeyword", options.metadataKeyword);
        searchParams.put("lowConfidence", lowConfidence);
        searchParams.put("lowConfidenceThreshold", options.lowConfidenceThreshold);
        searchParams.put("rejectLowConfidence", options.rejectLowConfidence);
        searchParams.put("rejectReason", rejectReason);
        searchParams.put("qualityAdvice", qualityAdvice);
        searchParams.put("queryRewriteEnabled", options.queryRewriteEnabled);
        searchParams.put("multiQueryEnabled", options.multiQueryEnabled);
        searchParams.put("maxQueryVariants", options.maxQueryVariants);
        searchParams.put("originalQuery", options.originalQuery);
        searchParams.put("canonicalQuery", options.canonicalQuery);
        searchParams.put("enhancedQueries", options.enhancedQueries);
        searchParams.put("contextResolved", options.contextResolved);
        searchParams.put("rerankMode", options.rerankMode);
        searchParams.put("rerankModelId", options.rerankModelId);
        searchParams.put("rerankLatencyMs", options.rerankLatencyMs);
        searchParams.put("rerankErrorMessage", options.rerankErrorMessage);
        searchParams.put("engine", "mysql_hybrid_fallback");
        log.setMilvusSearchParams(toJson(searchParams));
        log.setSearchMode(options.searchMode);
        log.setCandidateK(options.candidateK);
        log.setMetadataFilter(toJson(Map.of(
                "documentIds", options.documentIds,
                "pageNo", options.pageNo == null ? "" : options.pageNo,
                "metadataKeyword", options.metadataKeyword
        )));
        log.setConfidenceScore(BigDecimal.valueOf(confidenceScore == null ? 0D : confidenceScore));
        log.setLowConfidence(lowConfidence);
        log.setQualityAdvice(qualityAdvice);
        log.setTopK(options.topK);
        log.setScoreThreshold(BigDecimal.valueOf(options.scoreThreshold));
        log.setRerankEnabled(options.rerankEnabled);
        log.setResultCount(sources.size());
        log.setLatencyMs((int) Duration.between(startedAt, Instant.now()).toMillis());
        log.setResults(toJson(sources));
        log.setMilvusResultIds(toJson(sources.stream().map(KnowledgeSource::getChunkId).toList()));
        knowledgeRetrievalLogMapper.insert(log);
        return log.getId();
    }

    /**
     * 转换知识库摘要。
     *
     * @param entity 知识库实体
     * @return 摘要对象
     */
    private KnowledgeBaseSummary toSummary(KnowledgeBaseEntity entity) {
        KnowledgeBaseSummary item = new KnowledgeBaseSummary();
        item.setId(entity.getId());
        item.setKbCode(entity.getKbCode());
        item.setKbName(entity.getKbName());
        item.setDescription(entity.getDescription());
        item.setWorkspaceId(entity.getWorkspaceId());
        item.setWorkspaceName(findWorkspaceName(entity.getWorkspaceId()));
        item.setEmbeddingModelId(entity.getEmbeddingModelId());
        item.setEmbeddingModelName(findModelName(entity.getEmbeddingModelId()));
        item.setRerankModelId(entity.getRerankModelId());
        item.setChunkStrategy(entity.getChunkStrategy());
        item.setChunkSize(entity.getChunkSize());
        item.setChunkOverlap(entity.getChunkOverlap());
        item.setMilvusCollectionName(entity.getMilvusCollectionName());
        item.setStatus(entity.getStatus());
        item.setDocumentCount(count("knowledge_document", "kb_id", entity.getId()));
        item.setChunkCount(count("knowledge_chunk", "kb_id", entity.getId()));
        item.setEmbeddingCount(count("knowledge_embedding", "kb_id", entity.getId()));
        item.setCreatedAt(entity.getCreatedAt());
        item.setUpdatedAt(entity.getUpdatedAt());
        return item;
    }

    /**
     * 拷贝摘要字段到详情对象。
     *
     * @param source 摘要
     * @param target 详情
     */
    private void copySummary(KnowledgeBaseSummary source, KnowledgeBaseDetail target) {
        target.setId(source.getId());
        target.setKbCode(source.getKbCode());
        target.setKbName(source.getKbName());
        target.setDescription(source.getDescription());
        target.setWorkspaceId(source.getWorkspaceId());
        target.setWorkspaceName(source.getWorkspaceName());
        target.setEmbeddingModelId(source.getEmbeddingModelId());
        target.setEmbeddingModelName(source.getEmbeddingModelName());
        target.setRerankModelId(source.getRerankModelId());
        target.setChunkStrategy(source.getChunkStrategy());
        target.setChunkSize(source.getChunkSize());
        target.setChunkOverlap(source.getChunkOverlap());
        target.setMilvusCollectionName(source.getMilvusCollectionName());
        target.setStatus(source.getStatus());
        target.setDocumentCount(source.getDocumentCount());
        target.setChunkCount(source.getChunkCount());
        target.setEmbeddingCount(source.getEmbeddingCount());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
    }

    /**
     * 转换文档摘要。
     *
     * @param entity 文档实体
     * @return 文档摘要
     */
    private KnowledgeDocumentSummary toDocumentSummary(KnowledgeDocumentEntity entity) {
        Map<String, Object> metadata = parseMap(entity.getMetadata());
        KnowledgeDocumentSummary item = new KnowledgeDocumentSummary();
        item.setId(entity.getId());
        item.setKbId(entity.getKbId());
        item.setDocName(entity.getDocName());
        item.setDocType(entity.getDocType());
        item.setFileExt(entity.getFileExt());
        item.setFileSize(entity.getFileSize());
        item.setFileHash(entity.getFileHash());
        item.setParseStatus(entity.getParseStatus());
        item.setParseError(entity.getParseError());
        item.setAsyncTaskId(asString(metadata.get("asyncTaskId")));
        item.setProcessStage(asString(metadata.get("processStage")));
        item.setProcessStageLabel(asString(metadata.get("processStageLabel")));
        item.setProgressPercent(intValue(metadata.get("progressPercent"), "parsed".equals(entity.getParseStatus()) ? 100 : 0));
        item.setLastMessage(asString(metadata.get("lastMessage")));
        item.setChunkCount(count("knowledge_chunk", "document_id", entity.getId()));
        item.setEmbeddingCount(countByJoin(entity.getId()));
        item.setUploadedAt(entity.getUploadedAt());
        return item;
    }

    /**
     * 转换分片摘要。
     *
     * @param entity 分片实体
     * @return 分片摘要
     */
    private KnowledgeChunkSummary toChunkSummary(KnowledgeChunkEntity entity) {
        KnowledgeChunkSummary item = new KnowledgeChunkSummary();
        item.setId(entity.getId());
        item.setDocumentId(entity.getDocumentId());
        item.setChunkNo(entity.getChunkNo());
        item.setTitle(entity.getTitle());
        item.setContent(entity.getContent());
        item.setTokenCount(entity.getTokenCount());
        item.setStatus(entity.getStatus());
        item.setSyncStatus(findEmbeddingSyncStatus(entity.getId()));
        item.setCreatedAt(entity.getCreatedAt());
        return item;
    }

    /**
     * 转换 Agent 知识库绑定摘要。
     *
     * @param entity 绑定实体
     * @return 绑定摘要
     */
    private AgentKnowledgeBindingSummary toBindingSummary(AgentKnowledgeBindingEntity entity) {
        AgentKnowledgeBindingSummary item = new AgentKnowledgeBindingSummary();
        item.setAgentId(entity.getAgentId());
        item.setKnowledgeBaseId(entity.getKnowledgeBaseId());
        KnowledgeBaseEntity kb = knowledgeBaseMapper.selectById(entity.getKnowledgeBaseId());
        item.setKbName(kb == null ? "" : kb.getKbName());
        item.setRetrievalConfig(entity.getRetrievalConfig());
        item.setEnabled(entity.getEnabled());
        item.setCreatedAt(entity.getCreatedAt());
        return item;
    }

    /**
     * 转换检索引用来源。
     *
     * @param kb 知识库
     * @param chunk 分片
     * @param score 得分
     * @return 引用来源
     */
    private KnowledgeSource toSource(KnowledgeBaseEntity kb, KnowledgeChunkEntity chunk, double score) {
        KnowledgeDocumentEntity document = knowledgeDocumentMapper.selectById(chunk.getDocumentId());
        KnowledgeSource source = new KnowledgeSource();
        source.setKbId(kb.getId());
        source.setKbName(kb.getKbName());
        source.setDocumentId(chunk.getDocumentId());
        source.setDocumentName(document == null ? "" : document.getDocName());
        source.setChunkId(chunk.getId());
        source.setParentChunkId(chunk.getParentChunkId());
        source.setChunkNo(chunk.getChunkNo());
        source.setChunkLevel(chunk.getChunkLevel());
        source.setSectionTitle(chunk.getSectionTitle());
        source.setSectionPath(chunk.getSectionPath());
        source.setQuoteText(chunk.getContent());
        source.setScore(score);
        source.setPageNo(chunk.getPageNo());
        return source;
    }

    /**
     * 生成检索缓存键。
     *
     * @param kbId 知识库 ID
     * @param query 查询文本
     * @param options 检索参数
     * @return 缓存键
     */
    private String retrievalCacheKey(String kbId, String query, RetrievalOptions options) {
        return DigestUtils.md5DigestAsHex((safeString(kbId) + "|" + normalizeText(query) + "|" + retrievalOptionsFingerprint(options)).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * 读取检索缓存。
     *
     * @param cacheKey 缓存键
     * @param options 检索参数
     * @return 命中的缓存结果
     */
    private KnowledgeRetrievalResult readRetrievalCache(String cacheKey, RetrievalOptions options) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                    SELECT sources_json, confidence_score
                    FROM knowledge_retrieval_cache
                    WHERE cache_key = ? AND expires_at > CURRENT_TIMESTAMP(3)
                    LIMIT 1
                    """, cacheKey);
            if (rows.isEmpty()) {
                return null;
            }
            jdbcTemplate.update("UPDATE knowledge_retrieval_cache SET hit_count = hit_count + 1 WHERE cache_key = ?", cacheKey);
            List<KnowledgeSource> sources = objectMapper.readValue(String.valueOf(rows.getFirst().get("sources_json")), new TypeReference<List<KnowledgeSource>>() {
            });
            KnowledgeRetrievalResult result = new KnowledgeRetrievalResult();
            result.setSources(sources);
            result.setSearchMode(options.searchMode);
            result.setRerankEnabled(options.rerankEnabled);
            result.setCandidateCount(sources.size());
            result.setResultCount(sources.size());
            result.setConfidenceScore(doubleValue(rows.getFirst().get("confidence_score"), 0D));
            result.setLowConfidence(false);
            result.setAnswerable(true);
            result.setQualityAdvice("命中检索缓存，已复用热点问题引用来源");
            result.setScoreThreshold(options.scoreThreshold);
            result.setLowConfidenceThreshold(options.lowConfidenceThreshold);
            return result;
        } catch (Exception exception) {
            return null;
        }
    }

    /**
     * 写入检索缓存。
     *
     * @param cacheKey 缓存键
     * @param kbId 知识库 ID
     * @param query 查询文本
     * @param options 检索参数
     * @param sources 引用来源
     * @param confidenceScore 置信得分
     */
    private void writeRetrievalCache(String cacheKey,
                                     String kbId,
                                     String query,
                                     RetrievalOptions options,
                                     List<KnowledgeSource> sources,
                                     Double confidenceScore) {
        try {
            String optionsHash = DigestUtils.md5DigestAsHex(retrievalOptionsFingerprint(options).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            jdbcTemplate.update("""
                    INSERT INTO knowledge_retrieval_cache
                      (id, cache_key, kb_id, query_hash, options_hash, sources_json, confidence_score, expires_at)
                    VALUES (?, ?, ?, ?, ?, CAST(? AS JSON), ?, DATE_ADD(CURRENT_TIMESTAMP(3), INTERVAL 10 MINUTE))
                    ON DUPLICATE KEY UPDATE
                      sources_json = VALUES(sources_json),
                      confidence_score = VALUES(confidence_score),
                      expires_at = VALUES(expires_at),
                      updated_at = CURRENT_TIMESTAMP(3)
                    """,
                    newId(),
                    cacheKey,
                    kbId,
                    DigestUtils.md5DigestAsHex(normalizeText(query).getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                    optionsHash,
                    toJson(sources),
                    confidenceScore == null ? 0D : confidenceScore);
        } catch (Exception ignored) {
            // 缓存失败不能影响主检索链路。
        }
    }

    /**
     * 生成检索参数指纹。
     *
     * @param options 检索参数
     * @return 参数指纹
     */
    private String retrievalOptionsFingerprint(RetrievalOptions options) {
        Map<String, Object> fingerprint = new LinkedHashMap<>();
        fingerprint.put("topK", options.topK);
        fingerprint.put("candidateK", options.candidateK);
        fingerprint.put("scoreThreshold", options.scoreThreshold);
        fingerprint.put("searchMode", safeString(options.searchMode));
        fingerprint.put("rerankEnabled", options.rerankEnabled);
        fingerprint.put("rerankModelId", safeString(options.rerankModelId));
        fingerprint.put("vectorWeight", options.vectorWeight);
        fingerprint.put("keywordWeight", options.keywordWeight);
        fingerprint.put("lowConfidenceThreshold", options.lowConfidenceThreshold);
        fingerprint.put("rejectLowConfidence", options.rejectLowConfidence);
        fingerprint.put("queryRewriteEnabled", options.queryRewriteEnabled);
        fingerprint.put("multiQueryEnabled", options.multiQueryEnabled);
        fingerprint.put("maxQueryVariants", options.maxQueryVariants);
        fingerprint.put("conversationContext", safeString(options.conversationContext));
        fingerprint.put("documentIds", options.documentIds);
        fingerprint.put("pageNo", options.pageNo == null ? "" : options.pageNo);
        fingerprint.put("metadataKeyword", safeString(options.metadataKeyword));
        return toJson(fingerprint);
    }

    /**
     * 查询知识库实体。
     *
     * @param id 知识库 ID
     * @return 知识库实体
     */
    private KnowledgeBaseEntity requireKnowledgeBase(String id) {
        KnowledgeBaseEntity entity = knowledgeBaseMapper.selectById(id);
        if (entity == null || entity.getDeletedAt() != null) {
            throw new BusinessException("KNOWLEDGE_BASE_NOT_FOUND", "知识库不存在");
        }
        return entity;
    }

    /**
     * 查询 Agent 实体。
     *
     * @param id Agent ID
     * @return Agent 实体
     */
    private AgentEntity requireAgent(String id) {
        AgentEntity entity = agentMapper.selectById(id);
        if (entity == null || entity.getDeletedAt() != null) {
            throw new BusinessException("AGENT_NOT_FOUND", "Agent 不存在");
        }
        return entity;
    }

    /**
     * 判断当前用户是否可查看知识库。
     *
     * @param entity 知识库实体
     * @return 是否可查看
     */
    private boolean canView(KnowledgeBaseEntity entity) {
        if (entity == null || entity.getDeletedAt() != null) {
            return false;
        }
        return workspaceGovernanceService.canViewResource(
                "knowledge_base",
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getOwnerUserId(),
                entity.getCreatedBy(),
                entity.getVisibility());
    }

    /**
     * 校验知识库查看权限。
     *
     * @param entity 知识库实体
     */
    private void assertCanView(KnowledgeBaseEntity entity) {
        if (!canView(entity)) {
            throw new BusinessException("KNOWLEDGE_FORBIDDEN", "没有访问该知识库的权限");
        }
    }

    /**
     * 校验知识库管理权限。
     *
     * @param entity 知识库实体
     */
    private void assertCanManage(KnowledgeBaseEntity entity) {
        if (!workspaceGovernanceService.canManageResource("knowledge_base", entity.getWorkspaceId(), entity.getOwnerUserId(), entity.getCreatedBy())) {
            throw new BusinessException("KNOWLEDGE_FORBIDDEN", "没有管理该知识库的权限");
        }
    }

    /**
     * 从请求对象构建检索参数。
     *
     * @param request 检索请求
     * @return 检索参数
     */
    private RetrievalOptions optionsFromRequest(KnowledgeRetrievalRequest request) {
        RetrievalOptions options = new RetrievalOptions();
        options.topK = normalizeTopK(request.getTopK());
        options.candidateK = normalizeCandidateK(request.getCandidateK(), options.topK);
        options.scoreThreshold = clampScore(request.getScoreThreshold(), properties.getRag().getDefaultScoreThreshold());
        options.searchMode = normalizeSearchMode(request.getSearchMode());
        options.rerankEnabled = request.getRerankEnabled() == null || Boolean.TRUE.equals(request.getRerankEnabled());
        options.vectorWeight = clamp(request.getVectorWeight() == null ? 0.72D : request.getVectorWeight(), 0D, 1D);
        options.keywordWeight = clamp(request.getKeywordWeight() == null ? 0.28D : request.getKeywordWeight(), 0D, 1D);
        options.lowConfidenceThreshold = clampScore(request.getLowConfidenceThreshold(), Math.max(options.scoreThreshold, 0.62D));
        options.rejectLowConfidence = request.getRejectLowConfidence() == null || Boolean.TRUE.equals(request.getRejectLowConfidence());
        options.trustedAnswerMode = false;
        options.citationRequired = Boolean.TRUE.equals(request.getRejectLowConfidence());
        options.minCitationCount = 1;
        options.documentIds = normalizeDocumentIds(request.getDocumentIds());
        options.pageNo = request.getPageNo() != null && request.getPageNo() > 0 ? request.getPageNo() : null;
        options.metadataKeyword = safeText(request.getMetadataKeyword()).trim();
        options.queryRewriteEnabled = request.getQueryRewriteEnabled() == null
                ? Boolean.TRUE.equals(properties.getRag().getQueryRewriteEnabled())
                : Boolean.TRUE.equals(request.getQueryRewriteEnabled());
        options.multiQueryEnabled = request.getMultiQueryEnabled() == null
                ? Boolean.TRUE.equals(properties.getRag().getMultiQueryEnabled())
                : Boolean.TRUE.equals(request.getMultiQueryEnabled());
        options.maxQueryVariants = normalizeQueryVariantLimit(request.getMaxQueryVariants());
        options.conversationContext = safeText(request.getConversationContext()).trim();
        return options;
    }

    /**
     * 从 Agent 绑定配置构建检索参数。
     *
     * @param config 绑定配置
     * @return 检索参数
     */
    private RetrievalOptions optionsFromConfig(Map<String, Object> config) {
        RetrievalOptions options = new RetrievalOptions();
        options.topK = normalizeTopK(intValue(config.get("topK"), properties.getRag().getDefaultTopK()));
        options.candidateK = normalizeCandidateK(intValue(config.get("candidateK"), options.topK * 4), options.topK);
        options.scoreThreshold = clampScore(doubleValue(config.get("scoreThreshold"), properties.getRag().getDefaultScoreThreshold()), properties.getRag().getDefaultScoreThreshold());
        options.searchMode = normalizeSearchMode(asString(config.get("searchMode")));
        options.rerankEnabled = booleanValue(config.get("rerankEnabled"), true);
        options.vectorWeight = clamp(doubleValue(config.get("vectorWeight"), 0.72D), 0D, 1D);
        options.keywordWeight = clamp(doubleValue(config.get("keywordWeight"), 0.28D), 0D, 1D);
        options.lowConfidenceThreshold = clampScore(doubleValue(config.get("lowConfidenceThreshold"), Math.max(options.scoreThreshold, 0.62D)), Math.max(options.scoreThreshold, 0.62D));
        options.rejectLowConfidence = booleanValue(config.get("rejectLowConfidence"), true);
        options.trustedAnswerMode = booleanValue(config.get("trustedAnswerMode"), false);
        options.citationRequired = booleanValue(config.get("citationRequired"), options.trustedAnswerMode);
        options.minCitationCount = normalizeMinCitationCount(intValue(config.get("minCitationCount"), 1));
        options.documentIds = normalizeDocumentIds(readStringList(config.get("documentIds")));
        options.pageNo = intValue(config.get("pageNo"), 0) > 0 ? intValue(config.get("pageNo"), 0) : null;
        options.metadataKeyword = safeText(asString(config.get("metadataKeyword"))).trim();
        options.queryRewriteEnabled = booleanValue(config.get("queryRewriteEnabled"),
                Boolean.TRUE.equals(properties.getRag().getQueryRewriteEnabled()));
        options.multiQueryEnabled = booleanValue(config.get("multiQueryEnabled"),
                Boolean.TRUE.equals(properties.getRag().getMultiQueryEnabled()));
        options.maxQueryVariants = normalizeQueryVariantLimit(intValue(config.get("maxQueryVariants"),
                properties.getRag().getMaxQueryVariants()));
        return options;
    }

    /** 将查询变体数量限制在稳定范围内，避免一次请求放大下游压力。 */
    private int normalizeQueryVariantLimit(Integer value) {
        int fallback = properties.getRag().getMaxQueryVariants() == null
                ? 4 : properties.getRag().getMaxQueryVariants();
        int limit = value == null ? fallback : value;
        return Math.max(1, Math.min(8, limit));
    }

    /**
     * 归一化检索条数。
     *
     * @param value 原始条数
     * @return 安全条数
     */
    private int normalizeTopK(Integer value) {
        int fallback = properties.getRag().getDefaultTopK();
        int topK = value == null ? fallback : value;
        return Math.max(1, Math.min(20, topK));
    }

    /**
     * 归一化候选召回条数。
     *
     * @param value 原始候选数
     * @param topK 最终条数
     * @return 安全候选数
     */
    private int normalizeCandidateK(Integer value, int topK) {
        int candidateK = value == null ? topK * 4 : value;
        return Math.max(topK, Math.min(100, candidateK));
    }

    /**
     * 归一化可信回答最少引用数。
     *
     * @param value 原始引用数
     * @return 安全引用数
     */
    private int normalizeMinCitationCount(Integer value) {
        int count = value == null ? 1 : value;
        return Math.max(1, Math.min(5, count));
    }

    /**
     * 归一化检索模式。
     *
     * @param value 原始模式
     * @return 检索模式
     */
    private String normalizeSearchMode(String value) {
        String mode = StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "hybrid";
        if ("vector".equals(mode) || "keyword".equals(mode) || "hybrid".equals(mode)) {
            return mode;
        }
        return "hybrid";
    }

    /**
     * 判断是否需要向量检索。
     *
     * @param searchMode 检索模式
     * @return 是否需要向量
     */
    private boolean shouldUseVector(String searchMode) {
        return !"keyword".equals(searchMode);
    }

    /**
     * 归一化得分阈值。
     *
     * @param value 原始阈值
     * @param fallback 默认阈值
     * @return 安全阈值
     */
    private double clampScore(Double value, double fallback) {
        return clamp(value == null ? fallback : value, 0D, 1D);
    }

    /**
     * 提取查询关键词。
     *
     * @param query 查询文本
     * @return 关键词列表
     */
    private List<String> extractQueryTerms(String query) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        String normalized = query.toLowerCase(Locale.ROOT).replaceAll("[\\p{Punct}\\s]+", " ").trim();
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        for (String term : normalized.split("\\s+")) {
            if (term.length() >= 2) {
                terms.add(term);
            }
        }
        String compact = normalized.replace(" ", "");
        if (containsCjk(compact) && compact.length() >= 2) {
            for (int index = 0; index < compact.length() - 1; index++) {
                terms.add(compact.substring(index, index + 2));
            }
        }
        return new ArrayList<>(terms);
    }

    /**
     * 计算关键词命中得分。
     *
     * @param query 查询文本
     * @param terms 关键词列表
     * @param chunk 分片
     * @return 关键词得分
     */
    private double keywordScore(String query, List<String> terms, KnowledgeChunkEntity chunk) {
        if (chunk == null || terms.isEmpty()) {
            return 0D;
        }
        String content = normalizeText((chunk.getTitle() == null ? "" : chunk.getTitle() + " ") + chunk.getContent());
        long matched = terms.stream().filter(content::contains).count();
        double coverage = matched / (double) terms.size();
        double phraseBoost = containsNormalized(chunk.getContent(), query) ? 0.15D : 0D;
        double titleBoost = containsAny(chunk.getTitle(), terms) ? 0.1D : 0D;
        return clamp(coverage * 0.85D + phraseBoost + titleBoost, 0D, 1D);
    }

    /**
     * 判断文本是否包含标准化后的短语。
     *
     * @param text 文本
     * @param phrase 短语
     * @return 是否包含
     */
    private boolean containsNormalized(String text, String phrase) {
        String normalizedText = normalizeText(text);
        String normalizedPhrase = normalizeText(phrase);
        return StringUtils.hasText(normalizedPhrase) && normalizedText.contains(normalizedPhrase);
    }

    /**
     * 判断文本是否命中任一关键词。
     *
     * @param text 文本
     * @param terms 关键词
     * @return 是否命中
     */
    private boolean containsAny(String text, List<String> terms) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String normalized = normalizeText(text);
        return terms.stream().anyMatch(normalized::contains);
    }

    /**
     * 标准化文本用于关键词比较。
     *
     * @param text 原始文本
     * @return 标准化文本
     */
    private String normalizeText(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("[\\p{Punct}\\s]+", "");
    }

    /**
     * 清洗文档 ID 列表。
     *
     * @param values 原始文档 ID
     * @return 去重后的文档 ID
     */
    private Set<String> normalizeDocumentIds(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 从配置对象读取字符串列表。
     *
     * @param value 配置值
     * @return 字符串列表
     */
    private List<String> readStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            return List.of(text.split(","));
        }
        return List.of();
    }

    /**
     * HTML 转义，用于安全输出高亮片段。
     *
     * @param text 原始文本
     * @return 转义文本
     */
    private String escapeHtml(String text) {
        return safeText(text)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * 文本空值兜底。
     *
     * @param text 原始文本
     * @return 非空文本
     */
    private String safeText(String text) {
        return text == null ? "" : text;
    }

    /**
     * 判断文本是否包含中文字符。
     *
     * @param text 文本
     * @return 是否包含中文
     */
    private boolean containsCjk(String text) {
        if (text == null) {
            return false;
        }
        for (int index = 0; index < text.length(); index++) {
            Character.UnicodeScript script = Character.UnicodeScript.of(text.charAt(index));
            if (script == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }

    /**
     * 计算余弦相似度。
     *
     * @param left 左向量
     * @param right 右向量
     * @return 相似度得分
     */
    private double cosine(List<Double> left, List<Double> right) {
        int size = Math.min(left.size(), right.size());
        double dot = 0D;
        double leftNorm = 0D;
        double rightNorm = 0D;
        for (int index = 0; index < size; index++) {
            double l = left.get(index);
            double r = right.get(index);
            dot += l * r;
            leftNorm += l * l;
            rightNorm += r * r;
        }
        if (leftNorm == 0D || rightNorm == 0D) {
            return 0D;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    /**
     * 解析向量 JSON。
     *
     * @param json 向量 JSON
     * @return 向量值
     */
    private List<Double> parseVector(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception exception) {
            return List.of();
        }
    }

    /**
     * 解析 JSON 对象。
     *
     * @param json JSON 字符串
     * @return Map 对象
     */
    private Map<String, Object> parseMap(String json) {
        try {
            if (!StringUtils.hasText(json)) {
                return Map.of();
            }
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception exception) {
            return Map.of();
        }
    }

    /**
     * 保存上传文件到本地 data 目录。
     *
     * @param kbId 知识库 ID
     * @param documentId 文档 ID
     * @param fileName 文件名
     * @param bytes 文件字节
     * @return 存储相对路径
     */
    private String saveUploadFile(String kbId, String documentId, String fileName, byte[] bytes) throws Exception {
        Path folder = Path.of("data", "uploads", "knowledge", kbId);
        Files.createDirectories(folder);
        String safeName = fileName.replaceAll("[\\\\/:*?\"<>|]+", "_");
        Path target = folder.resolve(documentId + "_" + safeName);
        Files.write(target, bytes);
        return target.toString().replace('\\', '/');
    }

    /**
     * 提取文件扩展名。
     *
     * @param fileName 文件名
     * @return 扩展名
     */
    private String fileExt(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index < 0 ? "txt" : fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 查询字段计数。
     *
     * @param table 表名
     * @param column 字段名
     * @param value 字段值
     * @return 数量
     */
    private Integer count(String table, String column, String value) {
        Number count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM " + table + " WHERE " + column + " = ?", Number.class, value);
        return count == null ? 0 : count.intValue();
    }

    /**
     * 查询文档对应向量数量。
     *
     * @param documentId 文档 ID
     * @return 向量数量
     */
    private Integer countByJoin(String documentId) {
        Number count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM knowledge_embedding e
                JOIN knowledge_chunk c ON c.id = e.chunk_id
                WHERE c.document_id = ?
                """, Number.class, documentId);
        return count == null ? 0 : count.intValue();
    }

    /**
     * 查询分片向量同步状态。
     *
     * @param chunkId 分片 ID
     * @return 同步状态
     */
    private String findEmbeddingSyncStatus(String chunkId) {
        KnowledgeEmbeddingEntity embedding = knowledgeEmbeddingMapper.selectOne(new LambdaQueryWrapper<KnowledgeEmbeddingEntity>()
                .eq(KnowledgeEmbeddingEntity::getChunkId, chunkId)
                .last("limit 1"));
        return embedding == null ? "pending" : embedding.getSyncStatus();
    }

    /**
     * 查询模型展示名称。
     *
     * @param modelId 模型 ID
     * @return 模型名称
     */
    private String findModelName(String modelId) {
        if (!StringUtils.hasText(modelId)) {
            return "";
        }
        ModelConfigEntity model = modelConfigMapper.selectById(modelId);
        return model == null ? "" : model.getModelName();
    }

    /**
     * 查询工作空间展示名称。
     *
     * @param workspaceId 工作空间 ID
     * @return 工作空间名称
     */
    private String findWorkspaceName(String workspaceId) {
        if (!StringUtils.hasText(workspaceId)) {
            return "";
        }
        List<String> names = jdbcTemplate.queryForList(
                "SELECT workspace_name FROM oaf_workspace WHERE id = ? LIMIT 1",
                String.class,
                workspaceId);
        return names.isEmpty() ? "" : names.get(0);
    }

    /**
     * 生成唯一知识库编码。
     *
     * @param baseCode 基础编码
     * @return 唯一编码
     */
    private String uniqueKbCode(String baseCode) {
        String normalized = StringUtils.hasText(baseCode) ? baseCode : "kb";
        String candidate = normalized;
        int suffix = 1;
        while (knowledgeBaseMapper.selectCount(new LambdaQueryWrapper<KnowledgeBaseEntity>()
                .eq(KnowledgeBaseEntity::getKbCode, candidate)) > 0) {
            candidate = normalized + "-" + suffix++;
        }
        return candidate;
    }

    /**
     * 将名称转换为保守编码。
     *
     * @param text 名称文本
     * @return 编码文本
     */
    private String slugify(String text) {
        String cleaned = text == null ? "kb" : text.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
                .replaceAll("^-|-$", "");
        return StringUtils.hasText(cleaned) ? cleaned : "kb";
    }

    /**
     * 判断当前用户是否系统管理员。
     *
     * @return 是否系统管理员
     */
    private boolean isSystemManager() {
        return hasAuthority("ROLE_super_admin") || hasAuthority("ROLE_admin") || hasAuthority("knowledge:manage");
    }

    /**
     * 判断当前用户是否拥有指定权限。
     *
     * @param authority 权限标识
     * @return 是否拥有
     */
    private boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }

    /**
     * 获取当前用户 ID。
     *
     * @return 当前用户 ID
     */
    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUserDetails userDetails) {
            return userDetails.getUser().getId();
        }
        return null;
    }

    /**
     * 获取当前用户 ID，未登录时抛出异常。
     *
     * @return 当前用户 ID
     */
    private String currentUserIdOrThrow() {
        String userId = currentUserId();
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException("UNAUTHORIZED", "请先登录");
        }
        return userId;
    }

    /**
     * 读取整数配置值。
     *
     * @param value 配置值
     * @param fallback 默认值
     * @return 整数值
     */
    private int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return fallback;
    }

    /**
     * 读取小数配置值。
     *
     * @param value 配置值
     * @param fallback 默认值
     * @return 小数值
     */
    private double doubleValue(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return fallback;
    }

    /**
     * 读取布尔配置值。
     *
     * @param value 配置值
     * @param fallback 默认值
     * @return 布尔值
     */
    private boolean booleanValue(Object value, boolean fallback) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value != null) {
            return Boolean.parseBoolean(String.valueOf(value));
        }
        return fallback;
    }

    /**
     * 限制小数范围。
     *
     * @param value 原始值
     * @param min 最小值
     * @param max 最大值
     * @return 限制后的值
     */
    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 安全读取字符串值。
     *
     * @param value 原始值
     * @return 字符串值
     */
    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 安全字符串，避免任务日志 Map 写入 null。
     *
     * @param value 原始值
     * @return 非空字符串
     */
    private String safeString(String value) {
        return value == null ? "" : value;
    }

    /**
     * 生成 UUID 主键。
     *
     * @return UUID 字符串
     */
    private String newId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 转换 JSON 字符串。
     *
     * @param value 任意对象
     * @return JSON 字符串
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    /**
     * 检索参数内部对象。
     */
    private static final class RetrievalOptions {
        /** 最终返回条数。 */
        private int topK;
        /** 候选召回条数。 */
        private int candidateK;
        /** 检索模式。 */
        private String searchMode;
        /** 得分阈值。 */
        private double scoreThreshold;
        /** 低置信阈值。 */
        private double lowConfidenceThreshold;
        /** 是否启用重排。 */
        private boolean rerankEnabled;
        /** 是否低置信拒答。 */
        private boolean rejectLowConfidence;
        /** 是否启用可信回答模式。 */
        private boolean trustedAnswerMode;
        /** 是否要求答案必须携带引用。 */
        private boolean citationRequired;
        /** 可信回答最少引用来源数量。 */
        private int minCitationCount;
        /** 向量权重。 */
        private double vectorWeight;
        /** 关键词权重。 */
        private double keywordWeight;
        /** 指定文档过滤。 */
        private Set<String> documentIds = Set.of();
        /** 指定页码过滤。 */
        private Integer pageNo;
        /** 元数据关键词过滤。 */
        private String metadataKeyword = "";
        /** 是否启用查询改写。 */
        private boolean queryRewriteEnabled;
        /** 是否启用多查询融合。 */
        private boolean multiQueryEnabled;
        /** 最大查询变体数量。 */
        private int maxQueryVariants;
        /** 会话上下文。 */
        private String conversationContext = "";
        /** 本次实际使用的查询变体。 */
        private List<String> enhancedQueries = List.of();
        /** 本次实际使用的规范查询。 */
        private String originalQuery = "";
        /** 查询理解后的标准查询。 */
        private String canonicalQuery = "";
        /** 本次是否完成会话指代消解。 */
        private boolean contextResolved;
        /** 实际重排模式。 */
        private String rerankMode;
        /** 实际重排模型 ID。 */
        private String rerankModelId;
        /** 实际重排耗时。 */
        private int rerankLatencyMs;
        /** 重排降级原因。 */
        private String rerankErrorMessage;
    }

    /**
     * 检索候选内部对象。
     */
    private static final class RetrievalCandidate {
        /** 分片实体。 */
        private KnowledgeChunkEntity chunk;
        /** 向量得分。 */
        private double vectorScore;
        /** 关键词得分。 */
        private double keywordScore;
        /** 初始混合得分。 */
        private double baseScore;
        /** 重排后的最终得分。 */
        private double finalScore;
    }
}
