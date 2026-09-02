package com.openagentflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OpenAgentFlow 自定义配置对象。
 *
 * <p>这里集中承载 Milvus、安全开关和 RAG 默认参数，避免业务代码散落读取配置键。</p>
 */
@ConfigurationProperties(prefix = "openagentflow")
public class OpenAgentFlowProperties {

    /** Milvus 向量数据库连接配置。 */
    private Milvus milvus = new Milvus();

    /** 安全认证配置。 */
    private Security security = new Security();

    /** RAG 检索默认配置。 */
    private Rag rag = new Rag();

    /** Kafka 分布式异步任务配置。 */
    private AsyncTask asyncTask = new AsyncTask();

    /** MinIO 共享对象存储配置。 */
    private ObjectStorage objectStorage = new ObjectStorage();

    /** MCP 原生传输配置。 */
    private Mcp mcp = new Mcp();

    public Milvus getMilvus() {
        return milvus;
    }

    public void setMilvus(Milvus milvus) {
        this.milvus = milvus;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public Rag getRag() {
        return rag;
    }

    public void setRag(Rag rag) {
        this.rag = rag;
    }

    public AsyncTask getAsyncTask() {
        return asyncTask;
    }

    public void setAsyncTask(AsyncTask asyncTask) {
        this.asyncTask = asyncTask;
    }

    public ObjectStorage getObjectStorage() {
        return objectStorage;
    }

    public void setObjectStorage(ObjectStorage objectStorage) {
        this.objectStorage = objectStorage;
    }

    public Mcp getMcp() {
        return mcp;
    }

    public void setMcp(Mcp mcp) {
        this.mcp = mcp;
    }

    /**
     * MCP 原生传输配置。
     */
    public static class Mcp {

        /** 单次 MCP 请求超时秒数。 */
        private Integer requestTimeoutSeconds = 30;

        /** stdio 允许启动的可执行命令，多个命令使用英文逗号分隔。 */
        private String stdioAllowedCommands = "node,npx,java,python,python3,uv,uvx,docker";

        public Integer getRequestTimeoutSeconds() {
            return requestTimeoutSeconds;
        }

        public void setRequestTimeoutSeconds(Integer requestTimeoutSeconds) {
            this.requestTimeoutSeconds = requestTimeoutSeconds;
        }

        public String getStdioAllowedCommands() {
            return stdioAllowedCommands;
        }

        public void setStdioAllowedCommands(String stdioAllowedCommands) {
            this.stdioAllowedCommands = stdioAllowedCommands;
        }
    }

    /**
     * Kafka 分布式任务配置。
     */
    public static class AsyncTask {

        /** 是否启用 Kafka 任务生产和消费。 */
        private Boolean enabled = true;

        /** 主任务 Topic。 */
        private String topic = "openagentflow.async-task";

        /** 第一级五秒重试 Topic。 */
        private String retryTopic5s = "openagentflow.async-task.retry-5s";

        /** 第二级三十秒重试 Topic。 */
        private String retryTopic30s = "openagentflow.async-task.retry-30s";

        /** 最终失败死信 Topic。 */
        private String deadLetterTopic = "openagentflow.async-task.dlt";

        /** Kafka 消费组名称。 */
        private String consumerGroup = "openagentflow-async-workers";

        /** Topic 分区数量。 */
        private Integer partitions = 6;

        /** Topic 副本数量，本地单 Broker 默认为1，生产建议3。 */
        private Integer replicationFactor = 1;

        /** Topic 最小同步副本数，本地默认为1，生产建议2。 */
        private Integer minInSyncReplicas = 1;

        /** 最大自动重试次数。 */
        private Integer maxRetries = 3;

        /** 运行任务失联判定秒数。 */
        private Long staleSeconds = 300L;

        /** Outbox 单次领取数量。 */
        private Integer outboxBatchSize = 100;

        /** Outbox 消息最大发送次数。 */
        private Integer outboxMaxAttempts = 20;

        /** 已发送 Outbox 保留天数。 */
        private Integer outboxRetentionDays = 7;

        /** Worker 角色：all、document、evaluation、integration、maintenance。 */
        private String workerRole = "all";

        /** 当前实例是否启用 Kafka 任务消费者。 */
        private Boolean consumerEnabled = true;

        /** 当前实例是否启用 Outbox 发布器。 */
        private Boolean publisherEnabled = true;

        /** 单实例允许同时运行的最大任务数。 */
        private Integer maxRunningTasks = 32;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public String getRetryTopic5s() {
            return retryTopic5s;
        }

        public void setRetryTopic5s(String retryTopic5s) {
            this.retryTopic5s = retryTopic5s;
        }

        public String getRetryTopic30s() {
            return retryTopic30s;
        }

        public void setRetryTopic30s(String retryTopic30s) {
            this.retryTopic30s = retryTopic30s;
        }

        public String getDeadLetterTopic() {
            return deadLetterTopic;
        }

        public void setDeadLetterTopic(String deadLetterTopic) {
            this.deadLetterTopic = deadLetterTopic;
        }

        public String getConsumerGroup() {
            return consumerGroup;
        }

        public void setConsumerGroup(String consumerGroup) {
            this.consumerGroup = consumerGroup;
        }

        public Integer getPartitions() {
            return partitions;
        }

        public void setPartitions(Integer partitions) {
            this.partitions = partitions;
        }

        public Integer getReplicationFactor() {
            return replicationFactor;
        }

        public void setReplicationFactor(Integer replicationFactor) {
            this.replicationFactor = replicationFactor;
        }

        public Integer getMinInSyncReplicas() {
            return minInSyncReplicas;
        }

        public void setMinInSyncReplicas(Integer minInSyncReplicas) {
            this.minInSyncReplicas = minInSyncReplicas;
        }

        public Integer getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
        }

        public Long getStaleSeconds() {
            return staleSeconds;
        }

        public void setStaleSeconds(Long staleSeconds) {
            this.staleSeconds = staleSeconds;
        }

        public Integer getOutboxBatchSize() {
            return outboxBatchSize;
        }

        public void setOutboxBatchSize(Integer outboxBatchSize) {
            this.outboxBatchSize = outboxBatchSize;
        }

        public Integer getOutboxMaxAttempts() {
            return outboxMaxAttempts;
        }

        public void setOutboxMaxAttempts(Integer outboxMaxAttempts) {
            this.outboxMaxAttempts = outboxMaxAttempts;
        }

        public Integer getOutboxRetentionDays() {
            return outboxRetentionDays;
        }

        public void setOutboxRetentionDays(Integer outboxRetentionDays) {
            this.outboxRetentionDays = outboxRetentionDays;
        }

        public String getWorkerRole() {
            return workerRole;
        }

        public void setWorkerRole(String workerRole) {
            this.workerRole = workerRole;
        }

        public Boolean getConsumerEnabled() {
            return consumerEnabled;
        }

        public void setConsumerEnabled(Boolean consumerEnabled) {
            this.consumerEnabled = consumerEnabled;
        }

        public Boolean getPublisherEnabled() {
            return publisherEnabled;
        }

        public void setPublisherEnabled(Boolean publisherEnabled) {
            this.publisherEnabled = publisherEnabled;
        }

        public Integer getMaxRunningTasks() {
            return maxRunningTasks;
        }

        public void setMaxRunningTasks(Integer maxRunningTasks) {
            this.maxRunningTasks = maxRunningTasks;
        }
    }

    /**
     * MinIO 共享对象存储配置。
     */
    public static class ObjectStorage {

        /** 是否启用 MinIO。 */
        private Boolean enabled = true;

        /** MinIO 服务地址。 */
        private String endpoint = "http://localhost:9000";

        /** 浏览器访问 MinIO 的公网地址，用于生成预签名URL。 */
        private String publicEndpoint = "http://localhost:9000";

        /** MinIO 访问账号。 */
        private String accessKey = "minioadmin";

        /** MinIO 访问密钥。 */
        private String secretKey = "minioadmin";

        /** OpenAgentFlow 对象桶名称。 */
        private String bucket = "openagentflow";

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getPublicEndpoint() {
            return publicEndpoint;
        }

        public void setPublicEndpoint(String publicEndpoint) {
            this.publicEndpoint = publicEndpoint;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }
    }

    /**
     * Milvus 连接配置。
     */
    public static class Milvus {

        /** 是否启用 Milvus 客户端，开发环境未启动 Milvus 时可关闭以便后端降级启动。 */
        private Boolean enabled = true;

        /** Milvus 主机地址。 */
        private String host = "localhost";

        /** Milvus 服务端口。 */
        private Integer port = 19530;

        /** Milvus database 名称。 */
        private String databaseName = "default";

        /** Milvus 用户名，可为空。 */
        private String username;

        /** Milvus 密码，可为空。 */
        private String password;

        /** 知识库默认 Collection 名称。 */
        private String defaultKnowledgeCollection = "oaf_knowledge_chunks";

        /** Agent 记忆默认 Collection 名称。 */
        private String defaultMemoryCollection = "oaf_agent_memory";

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public String getDatabaseName() {
            return databaseName;
        }

        public void setDatabaseName(String databaseName) {
            this.databaseName = databaseName;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDefaultKnowledgeCollection() {
            return defaultKnowledgeCollection;
        }

        public void setDefaultKnowledgeCollection(String defaultKnowledgeCollection) {
            this.defaultKnowledgeCollection = defaultKnowledgeCollection;
        }

        public String getDefaultMemoryCollection() {
            return defaultMemoryCollection;
        }

        public void setDefaultMemoryCollection(String defaultMemoryCollection) {
            this.defaultMemoryCollection = defaultMemoryCollection;
        }
    }

    /**
     * 安全认证配置。
     */
    public static class Security {

        /** 是否启用认证鉴权。 */
        private Boolean authEnabled = false;

        /** JWT 签名密钥。 */
        private String jwtSecret = "openagentflow-local-dev-secret-change-me";

        /** JWT 有效分钟数。 */
        private Long jwtExpireMinutes = 1440L;

        /** CORS 允许来源，多个域名用英文逗号分隔。 */
        private String allowedOrigins = "http://localhost:5173,http://127.0.0.1:5173";

        public Boolean getAuthEnabled() {
            return authEnabled;
        }

        public void setAuthEnabled(Boolean authEnabled) {
            this.authEnabled = authEnabled;
        }

        public String getJwtSecret() {
            return jwtSecret;
        }

        public void setJwtSecret(String jwtSecret) {
            this.jwtSecret = jwtSecret;
        }

        public Long getJwtExpireMinutes() {
            return jwtExpireMinutes;
        }

        public void setJwtExpireMinutes(Long jwtExpireMinutes) {
            this.jwtExpireMinutes = jwtExpireMinutes;
        }

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    /**
     * RAG 检索默认配置。
     */
    public static class Rag {

        /** 默认召回 TopK 数量。 */
        private Integer defaultTopK = 5;

        /** 默认相似度阈值。 */
        private Double defaultScoreThreshold = 0.65;

        /** 单个 Embedding 服务商默认每秒批请求数。 */
        private Integer embeddingQps = 8;

        /** 单个 Embedding 服务商默认并发批请求数。 */
        private Integer embeddingConcurrency = 4;

        /** 等待 Embedding 分布式许可的毫秒数。 */
        private Long embeddingAcquireTimeoutMs = 10000L;

        /** 是否允许 Embedding 失败后使用本地模拟向量，仅建议开发环境开启。 */
        private Boolean allowLocalEmbeddingFallback = true;

        /** Milvus 不可用时是否允许扫描 MySQL 向量，仅限本地开发兼容。 */
        private Boolean allowMysqlVectorFallback = false;

        /** 是否默认启用 RAG 查询改写。 */
        private Boolean queryRewriteEnabled = true;

        /** 是否默认启用多查询召回融合。 */
        private Boolean multiQueryEnabled = true;

        /** 默认最大查询变体数量。 */
        private Integer maxQueryVariants = 4;

        /** 真实 Cross-Encoder 单次最大候选数量。 */
        private Integer rerankCandidateLimit = 30;

        public Integer getDefaultTopK() {
            return defaultTopK;
        }

        public void setDefaultTopK(Integer defaultTopK) {
            this.defaultTopK = defaultTopK;
        }

        public Double getDefaultScoreThreshold() {
            return defaultScoreThreshold;
        }

        public void setDefaultScoreThreshold(Double defaultScoreThreshold) {
            this.defaultScoreThreshold = defaultScoreThreshold;
        }

        public Integer getEmbeddingQps() {
            return embeddingQps;
        }

        public void setEmbeddingQps(Integer embeddingQps) {
            this.embeddingQps = embeddingQps;
        }

        public Integer getEmbeddingConcurrency() {
            return embeddingConcurrency;
        }

        public void setEmbeddingConcurrency(Integer embeddingConcurrency) {
            this.embeddingConcurrency = embeddingConcurrency;
        }

        public Long getEmbeddingAcquireTimeoutMs() {
            return embeddingAcquireTimeoutMs;
        }

        public void setEmbeddingAcquireTimeoutMs(Long embeddingAcquireTimeoutMs) {
            this.embeddingAcquireTimeoutMs = embeddingAcquireTimeoutMs;
        }

        public Boolean getAllowLocalEmbeddingFallback() {
            return allowLocalEmbeddingFallback;
        }

        public void setAllowLocalEmbeddingFallback(Boolean allowLocalEmbeddingFallback) {
            this.allowLocalEmbeddingFallback = allowLocalEmbeddingFallback;
        }

        public Boolean getAllowMysqlVectorFallback() {
            return allowMysqlVectorFallback;
        }

        public void setAllowMysqlVectorFallback(Boolean allowMysqlVectorFallback) {
            this.allowMysqlVectorFallback = allowMysqlVectorFallback;
        }

        public Boolean getQueryRewriteEnabled() {
            return queryRewriteEnabled;
        }

        public void setQueryRewriteEnabled(Boolean queryRewriteEnabled) {
            this.queryRewriteEnabled = queryRewriteEnabled;
        }

        public Boolean getMultiQueryEnabled() {
            return multiQueryEnabled;
        }

        public void setMultiQueryEnabled(Boolean multiQueryEnabled) {
            this.multiQueryEnabled = multiQueryEnabled;
        }

        public Integer getMaxQueryVariants() {
            return maxQueryVariants;
        }

        public void setMaxQueryVariants(Integer maxQueryVariants) {
            this.maxQueryVariants = maxQueryVariants;
        }

        public Integer getRerankCandidateLimit() {
            return rerankCandidateLimit;
        }

        public void setRerankCandidateLimit(Integer rerankCandidateLimit) {
            this.rerankCandidateLimit = rerankCandidateLimit;
        }
    }
}
