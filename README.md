# OpenAgentFlow-Java

OpenAgentFlow-Java 是一个基于 **Java 21 + Spring Boot 3 + Vue 3** 的开源 AI Agent 工作流平台。项目面向企业知识库问答、工具调用、MCP 接入、工作流编排、运行 Trace 和模型评测场景，目标是提供一套可运行、可追踪、可评测、可扩展的 AI Agent 应用开发底座。

## 为什么核心链路自研

OpenAgentFlow-Java 的目标不是做一个简单的 AI 调用 Demo，而是完整呈现企业级 Agent 平台的核心链路。项目中的 Agent 编排、RAG 知识库、Tool Calling、MCP 接入、工作流执行、Trace 追踪、模型评测和成本治理均采用自研实现，方便开发者直接理解底层流程、学习关键设计并进行二次开发。

项目不会把能力绑定到某一个 AI 框架。模型、Embedding、向量库和工具调用都按开放适配思路设计，当前默认使用 OpenAI-compatible 接口、MySQL、Redis 和 Milvus；后续也可以按需扩展 Spring AI、LangChain4j 或其他模型网关。为企业落地留下可插拔的工程空间。

## 核心能力

- **模型接入**：OpenAI-compatible、豆包方舟、Ollama、DeepSeek、Qwen 等供应商配置，支持连通性测试、普通对话和 SSE 流式输出。
- **Agent 管理**：Agent CRUD、发布、复制、删除、模型参数、System Prompt、资源级权限、调试运行和 Runtime 策略解释器。
- **多 Agent 协作**：协作团队 CRUD、成员分工、顺序/并行/路由/主控/复核模式、真实 Agent 调用、协作执行和 Trace 追踪。
- **PromptOps 生产治理**：System、User、RAG、Tool、Evaluation、Workflow Prompt 统一编译，支持强类型变量 Schema、敏感变量遮蔽、分层装配、版本锁定/跟随稳定版、差异与影响分析、开发/测试/生产晋级、灰度发布、A/B 实验、自动选优、在线指标和 Trace 快照。
- **企业解决方案模板广场**：支持系统公开与工作空间私有模板、多个 Agent/团队及关联资源自动收集和手动增删、不可变语义化版本、自动安全门禁、人工审核、Kafka 异步独立安装、内置数据库快照包、MinIO 完整模板包与向量载荷、Milvus 向量恢复、敏感凭证清空、失败补偿、三方差异升级、安全卸载、作者主页、收藏评分评论、举报处置和推荐运营；7 个官方模板均具备可安装版本，四套解决方案包含 Prompt、工具、知识库、文档、切片、工作流、Agent、团队和 Memory 完整资源。
- **RAG 知识库**：知识库 CRUD、文档上传、解析、Parent-Child 递归结构化切片、Embedding 数量完整性校验、Milvus 写入、切片分页预览、混合召回、重排、检索缓存、低置信度提示、可信回答模式、强制引用来源和 Agent 绑定。
- **Tool Calling**：REST API、Webhook、数据库查询、MCP 工具，支持 Schema、连通性测试、风险等级、调用日志和 Trace。
- **可视化工作流**：Vue Flow 画布，支持基础信息弹框新建、空画布、双击画布或工具栏弹出下拉式节点类型选择器、开始、LLM、RAG、工具、条件、人工确认、并行、循环、子流程、插件、API、通知、输出、结束节点，支持节点级执行条件、真正的有界线程池并行分支与确定性 JOIN、Spring Bean/Java ServiceLoader 插件 SPI、Kafka Outbox 异步运行、稳定哈希灰度版本路由、工作流批量评测、节点配置保存反馈、重试超时、失败分支、变量映射、版本差异、预算、沙箱策略、运行中节点动效、幂等运行、心跳快照、失败重跑和从失败节点恢复。
- **MCP 接入**：MCP Server CRUD，原生支持 Streamable HTTP、传统 SSE 和 stdio 子进程传输，具备标准 initialize/initialized 会话、HTTP 会话头、JSON/SSE 响应、tools/prompts/resources 发现、同步到工具中心和 Agent/工作流调用；stdio 不经过 shell 并受可执行命令白名单限制。
- **运行观测 Trace**：统一串联 LLM、RAG、Tool、Workflow、Evaluation 步骤，展示 Token、耗时、错误、引用来源、详情页卡片切换、步骤时间线按需展示、内部滚动和 Agent Runtime 可视化解释器。
- **成本与用量中心**：按服务商、模型、Agent、用户、工作流、评测统计 Token、成本、耗时，支持明细导出、价格配置和配额拦截。
- **组织空间治理**：组织、工作空间、空间成员、资源归属和空间级访问控制，支持 Agent、知识库、工具、工作流按空间隔离。
- **权限治理增强**：Spring Security 统一路由裁决、数据库 API 权限元数据、租户资源默认空间上下文、平台与空间管理员边界、空间多角色与治理权委派、部门数据范围、读写动作分离、用户/角色/部门资源 ACL、授权到期、Redis 全会话撤销、按钮级权限和授权审计明细。
- **运营监控告警**：统一展示平台健康、关键指标、告警规则、告警事件、通知渠道和一键巡检，支撑日常运营与交付验收。
- **通知中心与消息触达**：提供个人通知分页、未读汇总、批量已读、归档、接收偏好、角色或全员发布和业务去重；顶部铃铛、通知抽屉与完整页面均接入真实接口，Webhook、钉钉和企业微信渠道支持 CRUD、连通性测试、HMAC-SHA256 签名、指数退避、死信收敛、投递明细与人工重投。
- **交付验收中心**：面向开源发布、客户交付和部署上线，提供环境检查、核心链路检查、风险提示、交付清单和报告生成。
- **一键演示数据**：提供 P33 演示样例包，内置 Prompt、Agent、知识库、工具、工作流、评测集、多 Agent 团队和 Memory，支持脚本快速初始化。
- **模型评测 Evaluation**：评测集、样本导入、批量执行 Agent 或工作流、可选工作流运行 Agent、LLM-as-Judge、规则兜底、模型/Prompt/知识库策略对比、RAG 引用与工具结果评分和低分样本 Trace 追溯。
- **持续评测与发布门禁**：Agent、Prompt、知识库和工作流可从成功评测任务生成黄金基线，发布时逐项比较 RAG、工具、Memory、工作流指标，超过允许退化阈值自动阻断。
- **安全与隐私合规**：API Key 加密保存，日志与工具载荷统一脱敏，上传文件真实类型、压缩炸弹和危险内容扫描，高风险工具采用双人审批与一次性执行令牌，支持 PII 同意、撤回、导出和遗忘申请。
- **SRE 与容量工程**：OpenTelemetry Collector、Prometheus、Tempo、Grafana、黄金信号、单次 Run 资源画像、告警收敛与渠道补偿、HMAC 游标分页、100/500/1000 并发基线和容量数据入库。
- **高可用与全局租户隔离**：API、Runtime、文档 Worker、集成 Worker 独立扩缩，MySQL/Redis/Kafka/MinIO/Milvus 多副本目标，RPO/RTO 矩阵，MyBatis 核心表租户拦截与跨存储命名空间巡检。
- **Agent 历史会话**：每个 Agent 支持按用户保存历史会话、消息列表、继续对话、新建会话和删除会话，调试台进入或切换历史会话后自动定位最新消息，并支持流式生成暂停、保留部分回答及引入补充说明继续；长对话内容、引用来源和工具调用在独立区域内滚动展示。
- **Memory 记忆中心**：支持 Redis 短期记忆、MySQL 事实主数据、Milvus HNSW 长期记忆、LLM 结构化提取、Kafka + Outbox 异步流水线、内容哈希去重、事实版本与冲突替代、PII 策略、租户硬隔离、混合召回与时间衰减、Prompt Token 预算、反馈学习、配额、向量补偿、用户遗忘、治理问题和运营指标。
- **分布式异步任务**：基于 Kafka + Transactional Outbox 拆分任务提交与 Worker 执行，文档处理、向量重建、批量评测、工作流运行、MCP 能力发现、知识治理扫描、Memory 清理、历史成本重算均支持分类 Topic、多实例消费、MySQL 幂等抢占、Fencing Token、Worker 心跳、自动补偿、两级延迟重试和死信回放；上传文件通过 MinIO 在多个 Worker 间共享。
- **开源工程化**：Flyway 数据库迁移、无外部依赖的测试 Profile、MySQL Testcontainers、Vitest、Playwright、JaCoCo、Maven Enforcer、Docker Compose、CI、脚本、License、Issue/PR 模板和开源文档；CI 强制运行数据库容器套件及真实登录权限浏览器用例。

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 前端 | Vue 3.5、TypeScript、Vite、Vue Router、Vue Flow、lucide-vue-next |
| 后端 | Java 21、Spring Boot 3.3、Spring Security、JWT、MyBatis-Plus |
| 数据 | MySQL 8、Redis 7、Milvus 2.4、MinIO |
| AI | OpenAI-compatible Chat、Embedding、Function Calling、MCP |
| 工程 | Kafka、Flyway、JUnit/Testcontainers、Vitest、Playwright、JaCoCo、Docker Compose、GitHub Actions、PowerShell scripts |

## 界面预览

![工作台](docs/截图/工作台.png)

![调试台](docs/截图/调试台.png)

![工作流编排](docs/截图/工作流编排.png)

更多截图见 [演示流程](docs/演示流程.md)。

## 快速启动

### Docker Compose

```powershell
cd E:\xm\OpenAgentFlow-Java\dm
Copy-Item .env.example .env
docker compose up -d --build
```

执行前需要先启动 Kafka，并保证宿主机 `localhost:9092` 可用；Compose 中的后端通过 `host.docker.internal:9092` 访问该 Broker。

访问地址：

- 前端：http://localhost:5173
- 后端：http://localhost:8080/api
- Swagger：http://localhost:8080/api/swagger-ui.html
- MinIO 控制台：http://localhost:9001

默认账号：

```text
admin / 123456
```

### 本地开发

```powershell
cd E:\xm\OpenAgentFlow-Java\dm
.\scripts\start-dev.ps1
```

本地构建校验：

```powershell
.\scripts\build-all.ps1
```

后端单元测试默认启用 `test` Profile，并关闭 Milvus、Kafka Consumer、对象存储、OpenSearch 和 OTLP 等外部适配器；涉及 Flyway、模板安装和权限治理的集成套件使用临时 MySQL 8 容器。Testcontainers 固定为兼容 Docker Engine 29 的 `1.21.4`，GitHub Actions 运行时必须具备 Docker Engine，关键容器套件若被跳过会直接阻断 CI。

停止本地服务：

```powershell
.\scripts\stop-dev.ps1
```

## 目录结构

```text
dm/
  .github/                    GitHub Actions、Issue 模板、PR 模板
  docs/                       架构、快速启动、配置、演示流程、路线图、界面截图
  docs/截图/                  登录页、工作台、智能体、调试台、工作流、运行日志截图
  scripts/                    本地开发和 Docker 启停脚本
  openagentflow-backend/      Spring Boot 后端
  openagentflow-frontend/     Vue 3 前端
  openagentflow-sql/          MySQL 初始化脚本
  docker-compose.yml          一键启动 MySQL、Redis、Milvus、后端、前端
  .env.example                示例环境变量
  LICENSE                     MIT License
```

## 文档

- [快速启动](docs/快速启动.md)
- [架构说明](docs/架构说明.md)
- [配置说明](docs/配置说明.md)
- [演示流程](docs/演示流程.md)
- [演示数据包](docs/演示数据包.md)
- [路线图](docs/路线图.md)
- [生产部署](docs/生产部署.md)
- [运营监控](docs/运营监控.md)
- [MySQL SQL 说明](openagentflow-sql/mysql/README.md)

## 配置约定

后端默认读取环境变量，未配置时使用本地开发默认值：

- MySQL：`openagentflow`，默认 `root/123456`
- Redis：`localhost:6379`
- Milvus：`localhost:19530`
- Milvus 开关：`OAF_MILVUS_ENABLED`，本地开发脚本会在 19530 未监听时自动设置为 `false`，后端使用 MySQL 向量兜底启动。
- Kafka：IDEA 本地启动默认连接 `localhost:9092`；Docker Compose 默认连接 `host.docker.internal:9092`。
- Kafka Worker：默认消费组 `openagentflow-async-workers`，并发数由 `OAF_KAFKA_CONCURRENCY` 控制。
- MCP：`OAF_MCP_REQUEST_TIMEOUT_SECONDS` 控制协议请求超时，`OAF_MCP_STDIO_ALLOWED_COMMANDS` 使用英文逗号配置 stdio 可执行命令白名单。
- MinIO：默认连接 `http://localhost:9000`，存储桶为 `openagentflow`，用于分布式文档任务共享原始文件。
- MinIO 直传：`OAF_MINIO_ENDPOINT` 供后端内部访问，`OAF_MINIO_PUBLIC_ENDPOINT` 必须是浏览器可访问地址，用于生成预签名 URL。
- MinIO CORS：`OAF_MINIO_CORS_ALLOWED_ORIGINS` 仅配置允许上传的前端正式域名，生产环境不要使用通配来源。
- Worker 角色：`OAF_KAFKA_WORKER_ROLE` 支持 `all`、`document`、`evaluation`、`integration`、`maintenance`，可按负载类型分别部署。
- Embedding 背压：通过 `OAF_EMBEDDING_QPS`、`OAF_EMBEDDING_CONCURRENCY` 和 Redis 分布式许可控制模型端点压力；生产环境禁止本地模拟向量兜底。
- RAG 查询增强：`OAF_RAG_QUERY_REWRITE_ENABLED`、`OAF_RAG_MULTI_QUERY_ENABLED`、`OAF_RAG_MAX_QUERY_VARIANTS` 和 `OAF_RAG_RERANK_CANDIDATE_LIMIT` 控制查询改写、多查询融合和 Cross-Encoder 候选数量；知识库的 `rerankModelId` 为空时使用规则重排。
- 后端上下文路径：`/api`
- JWT Secret：生产环境必须通过 `OAF_JWT_SECRET` 覆盖
- IDEA控制台启动时会显示后端启动成功摘要、IP、端口、Swagger和基础依赖地址；空闲阶段保持安静，仅在HTTP请求ID存在时输出请求URL、Spring MVC匹配路由、业务链路、MyBatis SQL耗时和JdbcTemplate SQL。定时任务、Kafka消费和内部Trace不会持续刷出成功SQL，无请求上下文的ERROR仍会显示。SQL参数值不输出，避免密码、Token与API Key泄露。可通过`OAF_SQL_LOG_ENABLED`、`OAF_SLOW_SQL_MS`、`OAF_SLOW_REQUEST_MS`调整。

真实模型 API Key 不会写入源码、SQL 或 README。SQL 只初始化模型供应商和模型接入点，真实 Key 请在系统设置页或本地数据库中配置。

## Kafka 分布式任务

后端通过 `KafkaTaskClient` 统一封装消息序列化、任务 Key 和 Broker ACK。API 在同一个 MySQL 事务中写入 `async_task` 与 `async_task_outbox`，Outbox 发布器再以任务 ID 作为 Kafka Key 投递；Worker 使用 MySQL 条件更新原子领取任务，并恢复任务创建人的 Spring Security 权限上下文。

默认 Topic：

```text
openagentflow.async-task
openagentflow.async-task.retry-5s
openagentflow.async-task.retry-30s
openagentflow.async-task.dlt
```

新任务会按负载进入 `.document`、`.evaluation`、`.integration`、`.maintenance` 分类 Topic，对应重试 Topic 使用相同后缀。Worker 每 20 秒刷新心跳，每次接管递增 `lock_version`；旧 Worker 无法提交新执行代次的结果。执行失败后进入 5 秒和 30 秒重试 Topic，超过最大次数后进入死信 Topic；Outbox 独立重试 Broker 发送，补偿调度器处理遗留待执行任务和心跳超时任务。调试台 SSE、单次 Agent、多 Agent 和工作流运行保留实时响应链路。

大于 5MB 的文档由浏览器使用 MinIO 预签名 URL 直传，小文件通过后端 InputStream 流式写入；Worker 把对象流式落到临时文件后解析。解析文本和向量批次保存为 MinIO 检查点，Embedding 默认每 16 个子分片形成一个物理任务，并对每批输入数、向量返回数执行强一致校验，禁止模型部分返回时静默丢失后部内容。稳定分片ID包含文档、全局分片号、父级、偏移量和内容哈希，避免不同父分片下的局部序号碰撞；Fan-in必须等待达到预期分片总数后才能收口。数据库或 Milvus 阶段失败后可以跳过已完成的模型调用，任务中心展示解析、切片、Embedding、持久化、Milvus 五阶段时间线。

生产环境要求 Kafka Topic 副本数至少 `3`、`min.insync.replicas` 至少 `2`，并应使用多 Broker 集群和独立 MinIO/S3 服务。API 实例可设置 `OAF_KAFKA_CONSUMER_ENABLED=false`，专用 Worker 按角色独立扩容。

## SQL 初始化

MySQL 脚本目录：

```text
openagentflow-sql/mysql
```

执行顺序：

```text
V001__database_common.sql
V002__all_feature_tables.sql
V003__indexes_views_seed.sql
V004__milvus_integration.sql
V005__refresh_zh_comments.sql
V006__refresh_admin_password.sql
V007__seed_doubao_model_provider.sql
V008__agent_crud_permissions.sql
V009__rag_embedding_model_and_permissions.sql
V010__usage_cost_center.sql
V011__organization_workspace_governance.sql
V012__async_task_center.sql
V013__audit_risk_governance_center.sql
V014__model_gateway_governance.sql
V015__knowledge_governance_enhancement.sql
V016__ops_monitor_alert_center.sql
V017__prompt_template_center.sql
V018__iam_admin_center.sql
V019__seed_default_login_users.sql
V020__multi_agent_collaboration.sql
V021__runtime_trace_token_usage_default.sql
V022__memory_center.sql
V023__seed_customer_support_memory_template.sql
V024__rag_production_retrieval_enhancement.sql
V025__evaluation_llm_as_judge.sql
V026__delivery_acceptance_center.sql
V027__workflow_production_enhancement.sql
V028__workflow_execution_reliability_final.sql
V029__demo_data_package.sql
V030__customer_service_intent_guard_coupon_policy.sql
V031__customer_service_product_policy.sql
V032__demo_workflow_node_conditions.sql
V033__demo_order_summary_tool_intent.sql
V034__recursive_knowledge_chunking.sql
V035__enterprise_rag_metadata_parent_child.sql
V036__kafka_distributed_async_tasks.sql
V037__enterprise_async_outbox_pipeline.sql
V038__production_scale_p35_p42.sql
V039__production_closure_p43_p52.sql
V040__production_closure_p53_p62.sql
V041__memory_production_p63.sql
V042__quality_migration_distributed_pipeline.sql
V043__production_governance_p67_p72.sql
V044__tenant_workspace_backfill.sql
V045__demo_order_summary_intent_variants.sql
V046__promptops_production_p73.sql
V047__prompt_version_schema_contract_p73.sql
V048__solution_template_marketplace_p74.sql
V049__solution_template_seed_contract_fix.sql
V050__template_report_pending_guard_p74.sql
V051__p0_complete_builtin_solution_packages.sql
V052__p1_notification_center.sql
V053__permission_governance_enhancement.sql
V054__workspace_governance_permissions.sql
V055__least_privilege_workspace_role_fix.sql
V056__tool_intent_routing_metadata.sql
```

后端使用 Flyway 管理迁移。空数据库按 V001-V056 顺序初始化；已有非空数据库默认以 V041 建立基线，再执行 V042-V056 及后续版本。`openagentflow-sql/mysql` 是 SQL 主目录，`openagentflow-backend/src/main/resources/db/migration` 是运行时副本，修改 SQL 后执行：

```powershell
.\scripts\sync-flyway-migrations.ps1
.\scripts\sync-flyway-migrations.ps1 -Check
```

生产环境禁止执行 Flyway clean，并保持 `OAF_FLYWAY_ENABLED=true`、`OAF_FLYWAY_BASELINE_VERSION=41`。新部署不得修改已发布迁移文件，只能追加更高版本。

## 质量门禁

后端要求 Maven 3.9 与 JDK 21，`verify` 阶段执行单元测试、Testcontainers 集成测试、JaCoCo 关键可靠性规则覆盖率检查和 Maven Enforcer 环境约束：

```powershell
cd openagentflow-backend
mvn "-Dmaven.repo.local=D:\kfhj\maven\mavenopenagent" verify
```

前端门禁包括 Vitest 单元测试、TypeScript 类型检查、Vite 生产构建和 Playwright Chromium 冒烟测试：

```powershell
cd openagentflow-frontend
npm run test:unit
npm run build
npm run test:e2e
```

GitHub Actions CI 会依次检查 Flyway SQL 副本、后端测试与覆盖率、前端单测、生产构建和浏览器冒烟场景。

## 五分钟完整体验

已有数据库可以单独初始化 P33 演示样例包：

```powershell
cd E:\xm\OpenAgentFlow-Java\dm
.\scripts\init-demo-data.ps1
```

脚本会在导入 P33 前检查并补齐 `V028__workflow_execution_reliability_final.sql`，避免旧数据库缺少 `workflow_run.locked_by` 等工作流可靠性字段；导入 V029 后会继续应用 V030 到 V033 的演示增强脚本。

如需同时写入本地模型 Key：

```powershell
.\scripts\init-demo-data.ps1 -DemoApiKey "你的本地模型APIKey"
```

如果 `mysql.exe` 未加入 `PATH`，可指定 MySQL 客户端路径：

```powershell
.\scripts\init-demo-data.ps1 -MysqlExe "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
```

推荐问题：

```text
订单 OAF-DEMO-1001 到哪里了？如果客户要求退款应该怎么处理？
现在有那些订单？
```

单笔查询会使用演示订单 `OAF-DEMO-1001`：订单状态为运输中，物流单号为 `SF-DEMO-001`，预计明天18:00前送达；订单汇总查询会返回当前共有 1 笔演示订单。演示 REST 工具使用后端内置 mock 返回，并会从完整用户问题中提取订单号，工作流 LLM 节点会注入结构化 `toolResult`，DB 查询工具使用 `demo_order` 表。

体验路径：登录后依次查看智能体“客服助手”、知识库“产品手册知识库”、工具中心三类演示工具、工作流“演示客服 RAG 工具工作流”、多 Agent 团队“演示客服协作团队”、评测集“演示客服问答评测集”，最后进入交付验收中心检查演示数据项。

## 当前版本状态

| 阶段 | 状态 | 说明 |
| --- | --- | --- |
| P0 登录、权限、模型接入 | 已完成 | JWT、Redis、模型供应商、SSE |
| P1 Agent 管理 | 已完成 | CRUD、发布、复制、删除、运行、Agent 权限 |
| P2 RAG 知识库 | 已完成 | 上传、PDFBox中文PDF解析、扫描件与加密PDF识别、文档原地重新解析、Parent-Child递归结构化切片、Embedding、Milvus、引用来源、可信回答模式 |
| P3 Tool Calling | 已完成 | REST API、Webhook、DB Query、工具日志 |
| P4 Trace 运行观测 | 已完成 | 运行列表、步骤详情、RAG/Tool/LLM 统一链路 |
| P5 工作流编排 | 已完成 | Vue Flow、节点执行、上下文变量、Trace |
| P6 MCP 工具接入 | 已完成 | Server 管理、Streamable HTTP/传统 SSE/stdio 原生传输、标准会话、发现、同步、调用、审计和命令白名单 |
| P7 模型评测 | 已完成 | 评测集、Agent/工作流批量运行、指标、对比、RAG与工具评分、Trace |
| P8 GitHub 开源发布准备 | 已完成 | Docker、CI、脚本、License、开源文档 |
| P9 Agent 历史会话 | 已完成 | 会话列表、消息持久化、继续对话、调试台历史面板 |
| P10 成本与用量中心 | 已完成 | 用量统计、成本明细、模型价格、配额拦截、日报、导出、Trace 跳转 |
| P11 组织/空间/资源治理 | 已完成 | 组织、工作空间、成员、资源归属、空间权限和前端管理页 |
| P12 异步任务中心 | 已完成 | Kafka 分布式执行、文档处理、向量重建、批量评测、工作流运行、MCP 发现、知识治理扫描、Memory 清理、历史成本重算、MySQL 幂等锁、心跳、补偿、延迟重试、死信回放、MinIO 文件共享 |
| P13 审计与风险治理中心 | 已完成 | 操作审计采集、风险事件归集、高风险确认审批、处置闭环 |
| P14 生产部署加固 | 已完成 | prod Profile、Secret 校验、安全头、生产 Compose、非 root 容器、部署文档 |
| P15 模型网关与模型治理 | 已完成 | 路由策略、候选模型、健康统计、失败回退、网关调用观测 |
| P16 知识库治理增强 | 已完成 | 治理策略、问题扫描、质量评分、风险级别、交付问题闭环 |
| P17 平台运营监控与告警中心 | 已完成 | 运营总览、健康矩阵、告警规则、告警事件、通知渠道、一键巡检 |
| P18 通知中心与消息触达 | 已完成 | 个人收件箱、未读汇总、批量已读、归档、接收偏好、统一发布、真实铃铛抽屉、渠道CRUD与测试、HMAC签名、失败补偿、死信和人工重投 |
| P19 Prompt 模板中心与版本治理 | 已完成 | Prompt 模板 CRUD、变量解析、版本发布、复制、回滚、Agent 绑定 |
| P20 工作台 Dashboard 全量真实化 | 已完成 | 真实指标、运行趋势、最近运行、模型排行、任务队列、告警健康、知识库质量 |
| P21 多 Agent 协作 | 已完成 | 协作团队 CRUD、成员分工、五种协作模式、真实 Agent 调用、协作执行、Trace 追踪 |
| P24 Memory 记忆中心 | 已完成 | 短期记忆、长期记忆、任务记忆、向量记忆、客服助手长期记忆模板、召回测试、过期清理、调试链路自动沉淀、SSE 异步登录态传递 |
| P26 评测增强 LLM-as-Judge | 已完成 | 裁判模型评分、Judge Prompt、质量维度 JSON、规则兜底、Judge 综合分和低分原因 |
| P27 RAG 生产级召回增强 | 已完成 | Parent-Child 分片、分片元数据、已有知识库默认策略迁移、重复文件复用、Office 文档解析增强、Embedding 批处理限流、权限感知检索、Agent 链路热点检索缓存、混合召回、候选扩召、向量/关键词权重、文档/页码/元数据过滤、重排、父分片上下文扩展、引用高亮、排序原因和低置信度建议 |
| P77 通用意图路由与多意图治理 | 已完成 | 工具意图编码、路由示例、必填实体、结构化路由计划、多意图拆分、工具/RAG/直接回答分流、缺失实体确定性澄清、路由 Trace/SSE 解释和移除前端业务硬编码 |
| P78 RAG 查询理解与重排增强 | 已完成 | 查询规范化、上下文指代消解、同义词扩展、多查询向量召回、跨查询 RRF 融合、候选去重、真实 Cross-Encoder 重排、失败规则降级、检索参数与增强链路可观测、调试台降级原因展示 |
| P29 Agent Runtime 可视化解释器 | 已完成 | 调试台实时链路、Trace 复盘链路、Agent 详情策略预演、调试台右侧检索结果/工具调用/引用统计原生切换、右侧栏整体滚动、Runtime 解释器双倍高度、证据区固定高度滚动、引用来源抽屉卡片切换、详情页卡片切换、步骤时间线按需展示和内部滚动 |
| P28 交付验收中心 | 已完成 | 环境检查、核心链路检查、风险提示、交付清单、报告生成和权限菜单 |
| P29 工作流生产级增强 | 已完成 | 基础信息弹框新建、空画布、画布双击加节点、节点级执行条件、重试超时、失败分支、人工确认、变量映射、条件表达式、同步/异步运行、Kafka Outbox、有界并行分支、确定性JOIN、版本快照执行、稳定哈希灰度、工作流评测、子流程、插件SPI、API发布、沙箱、对话节点输出和运行中节点动效 |
| P33 一键演示数据与交付样例包 | 已完成 | 幂等 SQL 样例包、PowerShell 初始化脚本、客服知识库文档、Prompt/Agent/工具/工作流/评测集/多 Agent/Memory 样例、工作流工具节点编码兼容、LLM 节点结构化工具结果注入、客服订单统一意图门控、Agent 绑定工作流入口路由、产品与优惠券知识分片、非订单问题历史污染隔离、节点级 `intent:order_runtime` 条件、订单汇总同义句式识别、版本快照同步、交付验收中心演示数据检查 |
| P34 海量文档与高并发任务架构 | 已完成 | Transactional Outbox、分类 Topic、Worker 角色隔离、Fencing Token、Redis Embedding 背压、MinIO 预签名直传、流式文件处理、解析与向量检查点、结构化阶段时间线、生产副本约束 |
| P35 文档处理 DAG 分片架构 | 已完成 | 持久化 DAG 根任务、父子任务、阶段节点、幂等键、分片协议、阶段尝试留存、根任务延迟收口和任务中心时间线 |
| P36 Agent Runtime 执行面 | 已完成 | Runtime 专用有界线程池、队列背压、Redis 停止令牌、MySQL 控制指令、SSE 分片停止检查和超时终止 |
| P37 检索与向量存储生产化 | 已完成 | Milvus 严格写入、失败重试、蓝绿索引版本、物理集合与稳定别名、关键词索引版本预留和回滚元数据 |
| P38 可观测与弹性伸缩 | 已完成 | Prometheus、Micrometer、OpenTelemetry 桥接、Outbox 年龄、任务积压、Runtime 并发、健康探针和 KEDA Kafka Lag 扩容 |
| P39 数据生命周期治理 | 已完成 | 文档跨 MySQL、MinIO、向量映射的异步清理作业、删除数量汇总、失败重试模型和高增长表小批量保留清理 |
| P40 安全与租户隔离 | 已完成 | 工作空间文档/存储/向量/并发配额、危险文件拦截、API Key AES-256-GCM、Kafka SASL_SSL 配置和安全事件表 |
| P41 高可用与容灾交付 | 已完成 | Helm Chart、API/文档 Worker/集成 Worker/Runtime 拆分、滚动升级、反亲和、PDB、优雅退出、健康探针、MySQL备份恢复脚本和RPO/RTO说明 |
| P42 容量基线与故障演练 | 已完成 | Testcontainers 基础设施测试、k6 Runtime/任务中心压测、Kafka 暂停与 Worker 强退脚本、容量目标和故障矩阵 |
| P43 文档物理DAG | 已完成 | Parse、Chunk、Embedding分片、向量写入分片、Finalize五类Kafka任务，稳定分片ID、对象产物、Fan-out/Fan-in和分片幂等提交 |
| P44 Runtime执行面分离 | 已完成 | API与Runtime双角色Profile、聊天流量独立路由、Redis停止令牌、活动HTTP请求映射和跨实例500毫秒强取消观察器 |
| P45 检索基础设施闭环 | 已完成 | Milvus新物理集合异步批量重建、真实Alias切换、文档向量过滤删除、OpenSearch BM25建索引、Bulk写入、关键词召回、混合召回和删除同步 |
| P46 多租户硬隔离 | 已完成 | X-Workspace-Id成员校验、可信空间上下文、Redis Lua原子配额预占、MySQL预占明细和空间安全拦截 |
| P47 SLO与全链路观测 | 已完成 | 任务Trace ID跨Outbox/Kafka/Worker透传、平台SLO策略、违规归集、恢复检测、Prometheus指标和告警规则 |
| P48 跨存储一致性 | 已完成 | MySQL、MinIO、Milvus、OpenSearch真实删除，缺失向量、孤儿映射、处理超时巡检和一致性问题列表 |
| P49 容量与混沌测试 | 已完成 | 文档物理DAG到达率压测、Runtime与任务中心压测、Kafka/Worker故障脚本、Testcontainers和Kafka Schema契约测试 |
| P50 AI安全护栏 | 已完成 | Prompt注入阻断、模型输出密钥与个人信息脱敏、危险工具二次确认、护栏策略和事件查询接口 |
| P51 发布质量门禁 | 已完成 | Agent发布前自动检查评测得分、失败率、耗时、安全与成本，失败阻断发布并独立保存门禁执行数据 |
| P52 软件供应链安全 | 已完成 | CycloneDX SBOM、Trivy漏洞/密钥/配置扫描、许可证检查、Cosign无密钥签名和软件制品准入接口 |
| P53 检索引擎真实化 | 已完成 | Milvus HNSW ANN、OpenSearch BM25、RRF排名融合，生产默认禁止MySQL向量扫描降级 |
| P54 海量文档工件分片 | 已完成 | 切片清单按Embedding批次拆成独立对象工件，Worker只加载当前子分片及所需父分片 |
| P55 强租户上下文 | 已完成 | 生产租户资源强制X-Workspace-Id、成员校验、登录自动选空间、流式上传导出统一透传 |
| P56 Runtime断线续传 | 已完成 | Redis有界事件缓冲、Run内单调序号、Last-Event-ID重放和终态自动收口 |
| P57 首Token与OTLP观测 | 已完成 | Run及LLM调用首Token延迟入库、OTLP导出、Prometheus与Trace关联；本地默认关闭OTLP，生产按环境变量启用 |
| P58 主动一致性修复 | 已完成 | MinIO对象与OpenSearch文档主动探测、一致性问题入库、Kafka幂等修复任务 |
| P59 动态AI安全治理 | 已完成 | 数据库动态护栏、Unicode混淆归一化、RAG间接注入阻断、HTTP工具SSRF防护 |
| P60 多资源发布门禁 | 已完成 | Agent、Prompt、工作流发布检查，真实P95、安全与成本配额，限时豁免审批 |
| P61 灾备演练闭环 | 已完成 | MySQL一致性备份、SHA-256校验、隔离恢复、核心表冒烟查询和RTO测量脚本 |
| P62 可部署供应链证明 | 已完成 | GHCR不可变镜像、Cosign镜像签名、GitHub provenance、平台准入回写和跨故障域调度 |
| P63 Memory生产级增强 | 已完成 | LLM结构化事实提取、Redis短期记忆、Kafka异步沉淀、MySQL事实版本、Milvus ANN、租户标量过滤、混合排序、PII与冲突策略、容量配额、反馈学习、治理扫描、向量重建、用户遗忘和运营指标 |
| P64 自动化测试与质量门禁 | 已完成 | JUnit可靠性规则、MySQL Flyway Testcontainers、Vitest分页规则、Playwright登录冒烟、JaCoCo、Maven Enforcer和GitHub Actions阻断门禁 |
| P65 Flyway数据库迁移治理 | 已完成 | V001-V056类路径迁移、既有库V041基线、启动校验、禁止乱序与clean、SQL主目录同步脚本和迁移集成测试 |
| P66 分布式执行正确性 | 已完成 | 文档DAG代次Fencing、根任务幂等初始化、事务化Fan-out、行锁唯一Fan-in、预期/实际条目屏障、最终数量对账、停滞巡检和问题自动收口 |
| P67 AI持续评测 | 已完成 | 黄金评测基线、RAG/工具/Memory/工作流指标、版本差异、单项退化阈值和多资源发布阻断 |
| P68 安全与合规治理 | 已完成 | 统一敏感数据脱敏、文件类型与压缩炸弹扫描、PII同意及数据主体申请、高风险工具双人审批和一次性执行令牌 |
| P69 可观测性与SRE | 已完成 | OTel Collector、Prometheus、Tempo、Grafana、黄金信号、Run资源画像、告警去重升级和渠道失败补偿 |
| P70 性能与容量工程 | 已完成 | 前端路由分包、HMAC游标分页、100/500/1000并发压测矩阵、容量基线入库和资源饱和度数据模型 |
| P71 高可用与容灾 | 已完成 | API/Runtime/Worker独立副本、外部基础设施多副本目标、RPO/RTO数据、故障切换矩阵和跨故障域调度 |
| P72 全局租户隔离 | 已完成 | Eval/Prompt/高风险确认空间字段与旧数据回填、MyBatis核心表租户拦截、可信Kafka上下文和MySQL/Redis/Milvus/MinIO/OpenSearch命名空间巡检 |
| P73 PromptOps生产化 | 已完成 | 统一Prompt编译Runtime、版本级强类型变量契约、敏感值遮蔽、Agent/工作流/评测版本绑定、版本差异与影响面、环境晋级、生产门禁、稳定灰度、A/B实验、自动选优、在线质量与成本指标、Trace Prompt快照和前端治理控制台 |
| P74 企业解决方案模板广场 | 已完成 | 系统公开与空间私有模板、7个官方可安装版本、四套内置完整资源快照包、工作流画布物化、解决方案依赖收集、语义化不可变版本、敏感配置清洗、安全门禁、Kafka异步独立复制、MinIO模板包和向量载荷、Milvus向量恢复、失败补偿、三方升级冲突、安全卸载和推荐运营 |
| P75 权限治理增强 | 已完成 | 统一路由鉴权、权限路径元数据生效、工作空间强制隔离、平台与空间权限分层、空间多角色、最小权限修正、部门数据范围、资源读写动作分离、通用资源ACL、授权有效期、会话强制撤销、按钮权限、授权审计和治理查询回归测试 |
| P76 可重复构建与质量门禁闭环 | 已完成 | 测试Profile隔离外部适配器、Docker 29兼容的Testcontainers 1.21.4、MySQL共享基座、Apache Kafka基础设施冒烟、模板与权限关键用例容器化、CI Docker检查及容器套件防跳过门禁、真实登录默认空间和菜单裁剪Playwright用例 |

## 演示建议

1. 登录 `admin / 123456`。
2. 在系统设置中配置真实模型 API Key。
3. 打开 Prompt 模板中心，创建并发布 System Prompt 版本，通过“发布治理”晋级环境，再在 Agent 详情页选择锁定版本或跟随生产稳定版。
4. 在调试台发起一次模型对话，并查看 Run ID。
5. 上传知识库文档，等待处理完成后做检索测试。
6. 绑定知识库到 Agent，在调试台查看引用来源。
7. 创建工具并绑定 Agent，触发 Tool Calling。
8. 打开运行日志，查看 LLM、RAG、Tool 的完整 Trace。
9. 创建工作流并运行，查看工作流 Trace。
10. 接入 MCP Server，发现并测试 MCP 工具。
11. 创建评测集、导入样本、运行评测并跳转低分样本 Trace。
12. 打开 Memory 记忆中心，新增长期记忆或向量记忆，并使用召回测试确认 Agent 可参考相关上下文。
13. 打开用量中心，查看模型成本趋势、调用明细、维度拆分和配额规则。
14. 打开组织空间，创建团队空间、添加成员，并确认 Agent、知识库、工具、工作流归属到空间。
15. 打开任务中心，查看知识库文档解析、切片、Embedding、Milvus 写入的实时进度和日志。
16. 打开风险治理，查看审计日志、高风险工具、MCP 风险、护栏事件和待确认请求，并完成处置闭环。
17. 使用 `.env.prod` 和 `docker-compose.prod.yml` 检查生产部署配置，确认默认密钥不能启动生产后端。
18. 打开运营监控，点击立即巡检，查看 MySQL、Redis、Kafka、Milvus、模型供应商、任务队列、API 质量和模型质量状态，并处理告警事件。
19. 打开交付验收中心，点击一键验收，查看环境、权限、核心链路、配置风险和交付清单。
20. 打开工作流编排，使用模板创建流程，配置节点策略，在调试面板运行并查看 Trace。
21. 执行 `scripts/init-demo-data.ps1`，使用推荐问题体验客服助手的 RAG、工具、工作流、Trace 和交付验收链路。
22. 使用 `deploy/helm/openagentflow` 部署独立 API 与 Worker，并通过 KEDA 按文档 Topic Lag 自动扩缩容。
23. 使用 `scripts/load-test` 建立并发容量基线，使用 `scripts/fault-injection` 检查 Outbox 恢复、Fencing Token 和严格向量写入。
24. 生产环境使用 `prod,api`、`prod,runtime`、`prod,worker` 三类Profile拆分API、模型流量与Kafka Worker。
25. 启用OpenSearch后使用BM25与向量混合召回，并通过索引版本接口完成Milvus集合Alias切换。
26. 发布Agent前先运行评测任务，发布门禁会自动检查质量、稳定性、安全与成本指标。
27. GitHub Actions中的“软件供应链安全”流程会生成SBOM、扫描镜像和源码，并签名SBOM证明。
28. 使用 `docker compose -f docker-compose.observability.yml up -d` 启动 Collector、Prometheus、Tempo 和 Grafana，通过 `/sre/runs/{runId}/resources` 查看单次运行资源画像。
29. 使用 `scripts/load-test/invoke-capacity-matrix.ps1` 建立三档并发基线，使用 `scripts/dr/invoke-ha-failover-matrix.ps1` 生成组件故障切换矩阵。

本地启用OpenSearch BM25：

```powershell
docker compose -f docker-compose.yml -f docker-compose.search.yml up -d
```

默认本地Compose不启动OpenSearch；使用覆盖文件后，后端会自动创建知识库BM25索引并启用关键词与混合召回。

## 开源发布清单

- [x] 根级 `.gitignore`
- [x] `.env.example`
- [x] MIT `LICENSE`
- [x] Dockerfile 与 Docker Compose
- [x] GitHub Actions CI
- [x] Issue / PR 模板
- [x] 本地启动、停止、构建脚本
- [x] README 开源首页
- [x] 架构、配置、快速启动、演示、路线图文档
- [x] SQL 初始化说明
- [x] 中文界面截图

## 维护约定

- 新增或修改 Java 类、方法、字段和主要业务逻辑时补充中文注释。
- 每次修改前端、后端、SQL、项目结构、依赖、配置、启动方式或关键功能时，同步更新 README 或 docs。
- 前端页面已按原型图搭建，后续优先保持既有页面结构和视觉，不随意重做页面。
- 不要把真实模型 API Key 写入源码、SQL、README、docs 或 Git 提交历史。
- 生产环境必须配置 `OAF_SECRET_ENCRYPTION_KEY`，并将 Kafka 切换为 `SASL_SSL`；密钥由 Kubernetes Secret、Vault 或云 KMS 注入。
- 后端业务代码调整后只执行编译和测试检查，不自动启动后端进程。

## License

[MIT](LICENSE)
