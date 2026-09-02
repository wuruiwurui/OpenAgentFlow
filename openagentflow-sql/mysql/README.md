# OpenAgentFlow-Java MySQL SQL

Database target: MySQL 8.0.13+ / InnoDB / `utf8mb4`.

This is the MySQL alternative to the PostgreSQL + pgvector schema.

## Important Difference From PostgreSQL

MySQL Community Edition does not provide the same mature in-database vector index path as PostgreSQL `pgvector`.
For RAG embeddings, this schema stores:

- `embedding_json`: readable vector payload for debugging or low-volume fallback.
- `embedding_blob`: compact binary payload if the backend serializes float arrays.
- `external_vector_id`: ID for Milvus, Elasticsearch/OpenSearch, Qdrant, or another vector index.

Recommended production architecture:

```text
MySQL: business data, auth, Agent config, workflow, trace, eval
Vector DB/Search: embeddings and ANN retrieval
Object Storage: uploaded files and parsed artifacts
Redis: cache, sessions, queue state
```

## Files

- `V001__database_common.sql`: database, common system tables.
- `V002__all_feature_tables.sql`: all functional tables.
- `V003__indexes_views_seed.sql`: indexes, dashboard views, seed data.
- `V004__milvus_integration.sql`: Milvus connection metadata, collection/partition mapping, vector sync task tables, and MySQL-to-Milvus mapping fields.
- `V005__refresh_zh_comments.sql`: refresh Chinese table and column comments for databases created before comments were added.
- `V006__refresh_admin_password.sql`: refresh the built-in `admin/123456` BCrypt password for databases initialized before authentication was implemented.
- `V007__seed_doubao_model_provider.sql`: seed Doubao Ark OpenAI-compatible provider and endpoint model without storing any API key.
- `V008__agent_crud_permissions.sql`: seed Agent CRUD, publish, copy, delete, run permissions and owner ACL for the built-in Agent.
- `V009__rag_embedding_model_and_permissions.sql`: seed Doubao multimodal embedding endpoint `ep-20260615092553-lqvch` and knowledge-base permissions.
- `V010__usage_cost_center.sql`: usage and cost-center permissions, indexes, quota seed data.
- `V011__organization_workspace_governance.sql`: organization, workspace, workspace members, resource ownership, and workspace IDs for core resources.
- `V012__async_task_center.sql`: unified async task center tables, logs, permissions, cancel and retry visibility.
- `V013__audit_risk_governance_center.sql`: risk governance event table, indexes, permissions, and unified audit/risk-center support.
- `V014__model_gateway_governance.sql`: model gateway governance fields, route-policy indexes, default Agent chat route policy, and gateway decision logging.
- `V015__knowledge_governance_enhancement.sql`: knowledge-base governance policies, quality issues, permissions, and default governance policy.
- `V016__ops_monitor_alert_center.sql`: platform operation monitoring, health checks, alert rules, alert events, notification channels, and monitoring permissions.
- `V017__prompt_template_center.sql`: Prompt template center permissions, default Prompt versions, variable comments, and seed Prompt templates.
- `V018__iam_admin_center.sql`: user, department, role, and permission administration support.
- `V019__seed_default_login_users.sql`: default login user seed data.
- `V020__multi_agent_collaboration.sql`: multi-Agent team, member, and collaboration runtime tables.
- `V021__runtime_trace_token_usage_default.sql`: runtime trace token usage defaults.
- `V022__memory_center.sql`: Agent memory center tables and indexes.
- `V023__seed_customer_support_memory_template.sql`: customer-support Agent long-term memory template seed data.
- `V024__rag_production_retrieval_enhancement.sql`: production RAG recall fields, confidence indexes, and metadata filter columns.
- `V025__evaluation_llm_as_judge.sql`: LLM-as-Judge metric setup, judge detail comments, and default score configuration.
- `V026__delivery_acceptance_center.sql`: delivery acceptance report table, permissions, and menu API access.
- `V027__workflow_production_enhancement.sql`: workflow templates, API endpoints, strategy hit logs, input/output schema, execution policy, and advanced workflow permissions.
- `V028__workflow_execution_reliability_final.sql`: workflow idempotency, heartbeat, recovery snapshot, and failed-node resume fields.
- `V029__demo_data_package.sql`: one-click demo data package with Prompt, Agents, knowledge chunks, tools, workflow, evaluation dataset, multi-Agent team, Memory, and delivery checks.
- `V030__customer_service_intent_guard_coupon_policy.sql`: customer-service tool intent boundary and coupon-policy knowledge chunk.
- `V031__customer_service_product_policy.sql`: product/service-scope knowledge chunk and knowledge-first customer-service routing prompt.
- `V032__demo_workflow_node_conditions.sql`: sample node-level run conditions for the demo RAG and order-tool nodes.
- `V033__demo_order_summary_tool_intent.sql`: demo order summary routing for "my orders" and "how many orders" questions.
- `V034__recursive_knowledge_chunking.sql`: Parent-Child recursive structured chunking becomes the default strategy for new knowledge bases.
- `V035__enterprise_rag_metadata_parent_child.sql`: enterprise RAG metadata, existing knowledge-base strategy migration, Parent-Child chunks, knowledge-base versions, and retrieval cache.
- `V036__kafka_distributed_async_tasks.sql`: Kafka queue metadata, Worker locks, heartbeat, retry scheduling, recovery indexes, and dead-letter fields for distributed async tasks.
- `V037__enterprise_async_outbox_pipeline.sql`: Transactional Outbox, Fencing Token, resumable checkpoint JSON, and structured task-stage tables for high-throughput document pipelines.
- `V038__production_scale_p35_p42.sql`: production scale, tenant quota, lifecycle, observability, and high-availability fields.
- `V039__production_closure_p43_p52.sql`: platform SLO, guardrails, release gates, consistency, and supply-chain governance.
- `V040__production_closure_p53_p62.sql`: real retrieval engines, strong tenant context, runtime recovery, OTLP, disaster recovery, and deployment proof.
- `V041__memory_production_p63.sql`: production Memory tenant isolation, fact versions, policy, feedback, governance issues, metrics, vector consistency, and permissions.
- `V042__quality_migration_distributed_pipeline.sql`: Flyway migration governance and document DAG generation fencing, expected/actual cardinality, reconciliation issues, and supporting indexes.
- `V043__production_governance_p67_p72.sql`: golden evaluation baselines, privacy consent, secure file scanning, alert delivery compensation, capacity baselines, disaster-recovery targets, and tenant-isolation audit tables.
- `V044__tenant_workspace_backfill.sql`: trusted workspace backfill and non-null tenant constraints for historical evaluation, Prompt, and high-risk confirmation data.
- `V045__demo_order_summary_intent_variants.sql`: unified order runtime intent condition and node-ID-based demo workflow graph repair.
- `V046__promptops_production_p73.sql`: PromptOps typed variables, resource version binding, multi-environment promotion, stable gray release, A/B experiments, runtime metrics, Trace snapshots, permissions, and Chinese comments.
- `V047__prompt_version_schema_contract_p73.sql`: immutable typed variable Schema contract for every Prompt version.
- `V048__solution_template_marketplace_p74.sql`: enterprise solution template publishing, immutable versions, review governance, asynchronous installation mapping, three-way upgrades, ratings, comments, favorites, reports, permissions, and public seed packages.
- `V049__solution_template_seed_contract_fix.sql`: compatibility statement repair for the first public solution template versions.
- `V050__template_report_pending_guard_p74.sql`: pending report uniqueness guard that preserves handled report history.
- `V051__p0_complete_builtin_solution_packages.sql`: complete built-in solution package resources.
- `V052__p1_notification_center.sql`: notification center delivery and preference support.
- `V053__permission_governance_enhancement.sql`: permission governance and route/action authorization support.
- `V054__workspace_governance_permissions.sql`: workspace governance permission support.
- `V055__least_privilege_workspace_role_fix.sql`: least-privilege workspace role corrections.
- `V056__tool_intent_routing_metadata.sql`: generic tool intent codes, routing examples, and required entity metadata.

Recommended execution order:

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

Coverage matches the PostgreSQL version at the feature level:

- IAM, roles, permissions, ACL.
- Model providers, model configs, API keys, quotas.
- Agent, prompt, session, message, memory.
- RAG knowledge bases, documents, parse tasks, chunks, embeddings, retrieval logs, citations.
- Tool Center and high-risk confirmations.
- MCP server, capabilities, tests, discovery.
- Workflow design, versions, nodes, edges, schedules, runtime.
- Runtime Trace, LLM call logs, costs, guardrail events.
- Evaluation datasets, samples, metrics, tasks, scores, reports.
- Templates, notifications, audit, files, webhooks, tags.
- Advanced roadmap: multi-agent, Prompt A/B, model routing, guardrails, plugins, local model deployment, import jobs.

## Latest RAG Update

- `V009__rag_embedding_model_and_permissions.sql` does not store any real API key. API keys stay in local `model_api_key` rows only.
- The seeded Doubao embedding config uses `embeddingApi=multimodal` and calls `/api/v3/embeddings/multimodal`; the model code remains the endpoint ID `ep-20260615092553-lqvch`.
- Knowledge vectors are written to Milvus first. The backend also stores `embedding_json` in MySQL so low-volume retrieval and development fallback can continue when a vector service or embedding endpoint is unavailable.
- Milvus knowledge collections are separated by vector dimension, for example `oaf_knowledge_chunks_d2048`, so real 2048-dimensional vectors do not conflict with earlier local fallback vectors.
- The backend now supports knowledge-base CRUD, upload, parsing, chunking, embedding, Milvus write, retrieval test, source citations, and Agent binding.
- `V024__rag_production_retrieval_enhancement.sql` adds search mode, candidate size, metadata filter, confidence score, low-confidence flag, and quality advice fields for production recall analysis.
- The backend supports hybrid recall, vector/keyword weights, candidate expansion, rerank, document/page/metadata filtering, highlighted citations, rank reasons, and low-confidence advice.

## Latest Governance Update

- `V011__organization_workspace_governance.sql` creates organization and workspace governance tables with Chinese comments for every table and column.
- Existing Agent, knowledge base, tool, workflow, and MCP server rows are assigned to the default workspace during migration.
- The backend can use workspace membership to decide whether a user can view or manage team resources, while preserving owner, ACL, and administrator permissions.

## Latest Async Task Update

- `V012__async_task_center.sql` creates `async_task` and `async_task_log`, with Chinese comments for every table and column.
- `V036__kafka_distributed_async_tasks.sql` adds Kafka Topic/message IDs, Worker locks, heartbeat, next retry time, final failure time, and recovery indexes with Chinese comments.
- Knowledge document processing, vector rebuild, evaluation batch execution, MCP discovery, knowledge governance scans, Memory cleanup, and historical cost recalculation are submitted to Kafka and executed by distributed Workers.
- MySQL conditional updates provide idempotent task claims; five-second and thirty-second retry Topics provide delayed retries; exhausted tasks enter the dead-letter Topic and can be replayed from the task center.
- Original uploaded files are stored in MinIO so any Worker instance can parse the same document.
- `V037__enterprise_async_outbox_pipeline.sql` makes task creation and Kafka pending messages atomic through `async_task_outbox`.
- `async_task.lock_version` prevents stale Workers from committing newer task results, while `checkpoint_json` stores resumable artifact locations.
- `async_task_stage` stores parse, chunk, Embedding, persistence, and Milvus stage state with Worker and execution generation fields.

## Latest Governance Risk Update

- `V013__audit_risk_governance_center.sql` creates `risk_governance_event`, with Chinese comments for every table and column.
- Existing `audit_operation_log`, `tool_invocation_log`, `tool_confirm_request`, `runtime_guardrail_event`, `tool_definition`, and `mcp_capability` become the source tables for the governance center.
- The backend can automatically collect operation audit logs and aggregate high-risk tools, MCP capabilities, pending confirmations, failed tool calls, and guardrail events into one risk list.

## Latest Model Gateway Update

- `V014__model_gateway_governance.sql` extends `runtime_llm_call` with `route_policy_id`, `gateway_scene_type`, `route_decision`, and `fallback_used`, all with Chinese column comments.
- The default `AGENT_CHAT` route policy is seeded as `default-agent-chat`; existing enabled chat models are inserted as route candidates.
- The backend can route unpinned Agent chat and workflow LLM calls through the gateway, record route decisions, and fall back to the next healthy candidate when a model call fails.
- The frontend now includes a Model Gateway page for route-policy CRUD, candidate ordering, model health, recent gateway calls, failure rate, latency, and fallback visibility.

## Latest Knowledge Governance Update

- `V015__knowledge_governance_enhancement.sql` creates `knowledge_governance_policy` and `knowledge_governance_issue`, with Chinese comments for every table and column.
- The default policy checks stale documents, abnormal chunk token ranges, failed parsing, missing embeddings, Milvus sync fallback, empty knowledge bases, and unbound knowledge bases.
- The backend exposes `/knowledge-governance/overview`, `/knowledge-governance/quality`, `/knowledge-governance/scan`, `/knowledge-governance/issues`, and `/knowledge-governance/policies`.
- The frontend now includes a Knowledge Governance page for quality overview, issue scanning, issue handling, policy CRUD, and per-knowledge-base quality scores.

## Latest Ops Monitor Update

- `V016__ops_monitor_alert_center.sql` creates `ops_alert_rule`, `ops_alert_event`, `ops_health_check`, and `ops_notify_channel`, with Chinese comments for every table and column.
- The migration seeds station and Webhook notification channels, seven health checks, five default alert rules, and `ops:monitor:view` / `ops:monitor:manage` permissions.
- The backend exposes `/ops-monitor/overview`, `/ops-monitor/health`, `/ops-monitor/inspect`, `/ops-monitor/rules`, `/ops-monitor/events`, `/ops-monitor/checks`, and `/ops-monitor/channels`.
- The frontend now includes an Ops Monitor page for overview cards, health matrix, alert event handling, alert-rule CRUD, health checks, and notification channels.

## Latest Prompt Template Update

- `V017__prompt_template_center.sql` refreshes Chinese comments for `prompt_template` and `prompt_template_version`, and adds `prompt:manage` permission.
- Existing default RAG and Tool Prompt rows get an initial `v1` version snapshot; the migration also seeds default Evaluation Judge and Workflow Summary Prompt templates.
- The backend exposes `/prompt-templates/overview`, `/prompt-templates`, `/prompt-templates/{id}`, `/prompt-templates/{id}/publish`, `/prompt-templates/{id}/copy`, and `/prompt-templates/{id}/versions/{versionId}/rollback`.
- The frontend now includes a Prompt Template Center page for template CRUD, variable preview, version publishing, copying, rollback, and Agent System Prompt binding.

## Latest Evaluation Update

- `V025__evaluation_llm_as_judge.sql` adds the `llm_judge_overall` metric and marks accuracy, relevance, completeness, and hallucination control as Judge-ready metrics.
- Evaluation tasks can enable or disable LLM-as-Judge, choose a Judge model, and provide a custom Judge Prompt.
- Judge output must be JSON and is stored in `eval_score.judge_detail` with model, latency, Token, reason, strengths, risks, and fallback information.
- The frontend evaluation result page shows Judge score, Judge source type, and low-score reasons.

## Latest Delivery Acceptance Update

- `V026__delivery_acceptance_center.sql` creates `delivery_acceptance_report`, with Chinese comments for every table and column.
- The migration adds `delivery:acceptance:view` and `delivery:acceptance:manage` permissions for the delivery acceptance menu and API.
- The backend exposes `/delivery-acceptance/overview`, `/delivery-acceptance/checks`, `/delivery-acceptance/run`, and `/delivery-acceptance/reports`.
- The frontend now includes a Delivery Acceptance page for environment checks, core-chain checks, risks, delivery manifest, and generated reports.

## Latest Workflow Production Update

- `V027__workflow_production_enhancement.sql` extends `workflow_definition` with input schema, output schema, execution policy, API enabled flag, and release strategy.
- The migration creates `workflow_template`, `workflow_api_endpoint`, and `workflow_policy_hit_log`, with Chinese comments for every table and column.
- The backend exposes advanced workflow overview, templates, API endpoint publishing, human task decisions, endpoint invocation, and version diff APIs.
- The frontend workflow designer now includes production node types, retry and timeout policy, failure branch strategy, debug options, templates, API publishing, governance, human tasks, and version comparison.

## Latest Demo Package Update

- `V029__demo_data_package.sql` seeds a complete customer-service demo package for local showcase and delivery acceptance.
- The package includes customer-service Prompt templates, three Agents, a knowledge base document with four chunks, three tool types, the `demo_order` table, a published workflow, an evaluation dataset, a multi-Agent team, and long-term Memory.
- Demo HTTP tools use `mock.openagentflow.local` for display, while the backend returns built-in mock payloads for seeded demo tools.
- `V030__customer_service_intent_guard_coupon_policy.sql` adds customer-service intent boundaries and a coupon-policy knowledge chunk, so coupon or promotion questions stay on RAG instead of calling order tools.
- `V031__customer_service_product_policy.sql` adds product/service-scope knowledge for “what products do you have” questions and reinforces knowledge-first routing.
- `V032__demo_workflow_node_conditions.sql` adds sample node-level run conditions to the demo workflow RAG node and order-tool node.
- `V033__demo_order_summary_tool_intent.sql` expands demo order-tool routing for “my orders” and “how many orders” questions.
- `scripts/init-demo-data.ps1` can apply the package to an existing local MySQL database and optionally write a local model API key without storing it in repository files.
- The delivery acceptance backend checks whether the P33 demo package covers Agent, knowledge base, chunks, tools, workflow, evaluation dataset, team, Prompt, and Memory.

## Milvus Mapping

The MySQL schema now keeps Milvus operational metadata in:

- `vector_store_connection`
- `vector_collection`
- `vector_partition`
- `vector_record_mapping`
- `vector_sync_task`
- `vector_sync_error`

Knowledge and memory rows point to Milvus through:

- `knowledge_base.vector_collection_id`
- `knowledge_embedding.vector_collection_id`
- `knowledge_embedding.vector_primary_key`
- `agent_memory.vector_collection_id`
- `agent_memory.vector_primary_key`
- `knowledge_retrieval_log.milvus_result_ids`

## P35-P42 生产规模升级

`V038__production_scale_p35_p42.sql` 增加以下数据库能力：

- `async_task` 增加父任务、根任务、分片序号、分片总数和幂等键。
- `async_task_stage` 按任务、阶段、分片和执行代次保留阶段明细。
- `document_pipeline_node` 保存文档 DAG 节点、依赖、阶段产物和错误摘要。
- `runtime_control_command` 保存 Runtime 停止、暂停、恢复和补充指令。
- `knowledge_index_version` 保存 Milvus 物理集合、稳定别名和关键词索引版本。
- `data_lifecycle_job` 统一调度 MySQL、MinIO 和向量索引的数据清理。
- `tenant_resource_quota` 限制工作空间文档、存储、向量、并发和 Token 用量。
- `platform_security_event` 保存基础设施安全事件和处置状态。

所有新增表和字段均包含中文注释。

## P43-P52 生产闭环升级

`V039__production_closure_p43_p52.sql` 增加以下能力：

- 异步任务和Outbox增加Trace ID，支持跨Kafka分片链路追踪。
- `tenant_resource_reservation`保存工作空间Redis原子预占的数据库明细。
- `platform_slo_policy`、`platform_slo_violation`保存SLO目标及违规数据。
- `data_consistency_issue`保存MySQL、Milvus、OpenSearch、MinIO之间的一致性问题。
- `ai_guardrail_policy`、`ai_guardrail_event`保存输入、输出和工具护栏策略及命中事件。
- `release_gate_policy`、`release_gate_execution`保存Agent等资源的发布质量门禁。
- `software_artifact_attestation`保存SBOM、签名、漏洞、许可证和密钥扫描结果。

所有新增表、字段和索引均带中文注释，初始化脚本可重复执行。

## P67-P72 生产治理升级

`V043__production_governance_p67_p72.sql` 增加以下数据库能力：

- `evaluation_baseline`、`evaluation_regression`保存黄金评测基线、候选版本指标和退化明细。
- `privacy_consent`、`pii_data_subject_request`保存隐私同意、撤回、导出、遗忘和限制处理申请。
- `file_security_scan`保存上传文件真实类型、压缩包风险和扫描结果。
- `ops_notification_delivery`保存告警渠道投递、重试次数、下次补偿时间和死信状态。
- `capacity_baseline`保存并发、吞吐、P50/P95/P99、错误率、数据规模和资源饱和度。
- `disaster_recovery_target`保存 MySQL、Redis、Kafka、MinIO、Milvus 的副本、RPO 和 RTO 目标。
- `tenant_isolation_audit`保存 MySQL 与跨存储租户隔离问题。
- 发布门禁、高风险工具、告警事件、评测任务和 Prompt 模板补充生产治理字段。

所有新增表与字段均包含中文注释，字段和索引升级可重复执行。

`V044__tenant_workspace_backfill.sql` 会优先按资源创建人的有效空间成员关系回填历史数据，无法匹配时使用首个启用空间，并在具备默认空间时收紧非空约束。

`V045__demo_order_summary_intent_variants.sql` 将演示订单工具条件统一为 `intent:order_runtime`，并按节点 ID 修复当前工作流图和已发布版本中的 RAG、工具与 LLM 配置归属。

## P73 PromptOps 生产化

`V046__promptops_production_p73.sql` 增加以下数据库能力：

- Agent 增加 Prompt 指定版本、绑定模式和资源级变量，支持手工、锁定版本和跟随稳定版。
- Prompt 模板与版本增加强类型变量 Schema、内容哈希、安全检查结果、质量分、稳定版本和当前环境。
- `prompt_binding` 统一保存 Agent、工作流、RAG、工具和评测资源的 Prompt 版本关系。
- `prompt_environment_release` 支持开发、测试、生产环境晋级以及稳定哈希灰度比例。
- `prompt_experiment` 与变体表增加最小样本量、自动选优、胜出变体和在线聚合指标。
- `prompt_runtime_metric` 汇总版本与实验的成功率、质量、耗时、Token 和成本。
- `runtime_llm_call` 增加实际模板、版本、内容哈希、装配层和变量来源，便于 Trace 解释最终 Prompt。
- `V047__prompt_version_schema_contract_p73.sql` 为每个 Prompt 版本固化强类型变量 Schema，锁定版本、回滚和实验不会受模板当前变量变化影响。

所有新增表与字段均包含中文注释。

## P74 企业解决方案模板广场

`V048__solution_template_marketplace_p74.sql`、`V049__solution_template_seed_contract_fix.sql` 与 `V050__template_report_pending_guard_p74.sql` 增加以下数据库能力：

- `agent_template` 支持工作空间私有与系统公开范围、作者、审核、许可证、MinIO模板包、评分、收藏、趋势和举报聚合。
- `agent_template_version` 保存不可变语义化版本、资源清单、依赖图、安全检查、最小运行检查和对象哈希。
- `agent_template_resource` 保存 Agent、团队、Prompt、工具、MCP、知识库、文档、切片、向量、工作流和 Memory 快照。
- `agent_template_install` 与资源映射表保存 Kafka 任务、安装进度、来源与目标资源、安装哈希和对象清单。
- 三方升级冲突表保存旧模板、本地副本、新模板哈希以及用户逐项选择。
- 收藏、评分、评论、作者回复、举报处置和待处理举报唯一守卫支撑模板广场运营治理。
- 首批内置企业客服、知识问答、数据分析和智能运维公开解决方案模板。

所有新增表与字段均包含中文注释。

## P77/P78 智能路由与 RAG 增强

`V056__tool_intent_routing_metadata.sql` 为 `tool_definition` 增加通用路由元数据：

- `intent_codes` 保存工具可处理的意图编码 JSON 数组。
- `routing_examples` 保存自然语言路由示例 JSON 数组。
- `required_entities` 保存调用前必须抽取的实体名称 JSON 数组。

后端根据工具元数据和请求参数 Schema 生成结构化多意图路由计划，决定工具、知识库或直接回答；仅缺少工具必填实体且没有独立知识意图时，由 Runtime 直接返回澄清响应，避免模型自由发挥或误执行工具。缺失实体和路由原因写入 Trace 与 SSE。查询改写、多查询融合和重排参数存储在现有检索日志的 `milvus_search_params` JSON 中，不新增表结构。

知识库可通过保存接口的 `rerankModelId` 配置 Cross-Encoder 模型。未配置时使用规则重排；已配置但上游 `/rerank` 调用失败时返回 `rule_fallback` 和错误原因，主检索链路继续可用。

RAG Trace、普通聊天响应和 SSE 事件都会记录原始查询、规范查询、实际增强查询、会话指代消解状态、重排模型、重排耗时和降级原因，调试台会直接展示规则降级原因，便于定位召回质量问题。
