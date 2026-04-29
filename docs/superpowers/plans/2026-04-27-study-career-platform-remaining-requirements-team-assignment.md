# Study-Career Platform Remaining Requirements Team Assignment Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the remaining formal-requirement gaps and README backlog with a 5-person team, while keeping file ownership clear enough that work can proceed in parallel.

**Architecture:** Follow the repo's existing module boundaries instead of cross-cutting refactors. Split the remaining work into five lanes: search backend, search frontend, admin analytics, infrastructure/platform hardening, and upload/notification workflow. Shared contracts should be frozen early, then each lane can iterate mostly independently.

**Tech Stack:** Java 17, Spring Boot 3, Spring Security, MyBatis-Plus, H2/MySQL, Vue 3, Pinia, Vue Router, Axios, Vite, Vitest, MinIO, Elasticsearch (planned), Redis (planned)

---

## Current Gap Summary

Based on the current codebase, README, and formal requirements, the remaining gaps are:

| Requirement / Gap | Current State | Priority |
| --- | --- | --- |
| `FR-SEARCH-002` 关键词高亮 | 分类切换已完成；高亮未完成 | P0 |
| Elasticsearch 主检索 | 只有配置预留；尚未接入业务链路 | P0 |
| 搜索降级 | 当前实际实现是数据库检索，但没有“ES 主实现 + 故障降级”闭环 | P0 |
| `FR-ADMIN-004` 运营数据查看 | 只有最小后台总览；完整运营看板未完成 | P0 |
| Redis 排行与缓存 | 只有配置预留；未接入 discover / community hot / home preview | P1 |
| 缓存降级 | 未形成正式降级闭环 | P1 |
| `FR-NOTIFICATION-004` 通知类型覆盖 | 目前只覆盖欢迎通知、认证结果通知、社区回复通知 | P1 |
| 分片续传 | 资料上传仍是单请求直传 | P1 |
| 全局输入防护 | 缺少统一输入清洗 / XSS 防护层 | P1 |
| 接口文档同步 | 缺少 OpenAPI / Swagger 产物 | P1 |
| README backlog: 版本历史、简历重命名 / 替换 | 非本轮正式需求收口重点 | P2 |

## Assignment Principles

- 每个人只负责一条主线的主文件集，避免多人同时改同一核心文件。
- 共享契约先冻结，再并行开发。
- 所有 P0 任务先完成接口和测试骨架，再推进 UI 或基础设施增强。
- `分片续传`、`Redis`、`OpenAPI` 这类横切能力，尽量通过新增文件接入，不打散现有模块。

## Team Role Matrix

| Member | Primary Lane | Secondary Support | Main Output |
| --- | --- | --- | --- |
| Member 1 | 搜索后端 / Elasticsearch | 支持 Member 2 的高亮字段契约 | ES 主检索 + DB 降级 + 高亮字段 |
| Member 2 | 搜索前端 / 高亮展示 | 支持 Member 5 的上传前端交互 | 搜索页高亮渲染与结果体验 |
| Member 3 | 管理端运营看板 | 支持 Member 4 的性能验证口径 | 运营指标、图表、导出 |
| Member 4 | Redis 缓存 / 平台硬化 | 支持 Member 3 的统计接口稳定性 | 排行缓存、缓存降级、输入防护、OpenAPI |
| Member 5 | 上传链路 / 通知覆盖 | 支持 Member 2 的 UI 契约联调 | 分片上传、通知类型补齐 |

## Recommended Delivery Order

1. Member 1 先冻结搜索响应新增字段。
2. Member 2 在字段冻结后接入高亮展示。
3. Member 3 与 Member 4 并行推进后台看板与缓存 / 平台硬化。
4. Member 5 单独推进上传链路，同时补齐通知覆盖。
5. 全员最后一起做联调、README 更新和回归测试。

## Shared Contracts To Freeze First

### Contract A: Search Highlight Response

由 Member 1 和 Member 2 在最开始约定：

- 是否在 `SearchResponse.SearchResultItem` 中新增 `highlightTitle`
- 是否新增 `highlightSummary`
- 是否允许前端在高亮字段缺失时回退到原始 `title` / `summary`

建议契约：

- 保留原字段 `title`、`summary`
- 额外新增可选字段 `highlightTitle`、`highlightSummary`
- 统一由前端安全渲染，不直接信任 HTML

### Contract B: Admin Analytics API Shape

由 Member 3 和 Member 4 在最开始约定：

- 管理端运营数据是扩展现有 `/api/admin/dashboard/summary`
- 还是新增 `/api/admin/analytics/summary`

建议方案：

- 保持 `/api/admin/dashboard/summary` 继续轻量
- 新增 `/api/admin/analytics/summary`
- 导出接口单独使用 `/api/admin/analytics/export`

### Contract C: Chunk Upload Flow

由 Member 5 和 Member 2 在最开始约定：

- 初始化分片上传接口
- 分片上传接口
- 合并完成接口
- 失败重试与秒传策略是否本轮实现

建议本轮收口：

- 初始化
- 分片上传
- 合并完成
- 基于 upload session 的断点续传

不做：

- 秒传
- 复杂去重
- 多文件并行调度器

## Detailed Assignment

### Member 1: Search Backend / Elasticsearch Lead

**Requirements:**

- `FR-SEARCH-002` 的后端字段部分
- Elasticsearch 主检索
- 搜索降级

**Files:**

- Modify: `backend/src/main/java/com/campus/controller/SearchController.java`
- Modify: `backend/src/main/java/com/campus/service/SearchService.java`
- Modify: `backend/src/main/java/com/campus/dto/SearchResponse.java`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/resources/application-local.yml`
- Modify: `backend/src/test/java/com/campus/controller/SearchControllerTests.java`
- Create: `backend/src/main/java/com/campus/config/ElasticsearchIntegrationProperties.java`
- Create: `backend/src/main/java/com/campus/search/SearchGateway.java`
- Create: `backend/src/main/java/com/campus/search/DatabaseSearchGateway.java`
- Create: `backend/src/main/java/com/campus/search/ElasticsearchSearchGateway.java`

**Scope:**

- 在 `ES_ENABLED=true` 时走 Elasticsearch 主检索。
- 在 ES 不可用、未启用或查询异常时，回退到当前数据库搜索逻辑。
- 给搜索结果增加可选高亮字段，供前端渲染。
- 保持现有 `type` / `sort` / `q` 参数兼容。

**Do Not Own:**

- 不改搜索页具体 UI 样式。
- 不负责 `SearchResultCard.vue` 的渲染细节。

**Acceptance:**

- `SearchControllerTests` 增加 ES / fallback 覆盖。
- ES 关闭时，功能与当前行为等价。
- ES 异常时，不返回 500，而是自动回退到数据库结果。

**Suggested Commit Sequence:**

- `feat: add search gateway abstraction`
- `feat: add elasticsearch search fallback`
- `feat: expose search highlight fields`

### Member 2: Search Frontend / Highlight UX Lead

**Requirements:**

- `FR-SEARCH-002` 的前端展示部分

**Files:**

- Modify: `frontend/src/views/SearchView.vue`
- Modify: `frontend/src/components/SearchResultCard.vue`
- Modify: `frontend/src/components/SearchResultCard.spec.js`
- Modify: `frontend/src/views/SearchView.spec.js`
- Modify: `frontend/src/api/search.js`

**Scope:**

- 渲染高亮后的标题和摘要。
- 当后端未返回高亮字段时，自动回退到普通 `title` / `summary`。
- 保持现有分类切换、URL query 同步和空态逻辑不回退。
- 高亮渲染必须是安全的，不直接注入不受控 HTML。

**Dependency:**

- 依赖 Member 1 先冻结 `highlightTitle` / `highlightSummary` 字段名。

**Do Not Own:**

- 不接 Elasticsearch 客户端或 Spring 配置。
- 不改搜索排序算法。

**Acceptance:**

- `SearchView.spec.js` 和 `SearchResultCard.spec.js` 覆盖高亮与回退两条路径。
- 搜索页在无高亮字段时不报错。
- 高亮后的 UI 不打断现有卡片布局。

**Suggested Commit Sequence:**

- `feat: render search highlight fields`
- `test: cover search highlight fallback`

### Member 3: Admin Operations Analytics Lead

**Requirements:**

- `FR-ADMIN-004` 运营数据查看

**Files:**

- Modify: `backend/src/main/java/com/campus/controller/admin/AdminDashboardController.java`
- Modify: `backend/src/main/java/com/campus/service/AdminDashboardService.java`
- Modify: `backend/src/test/java/com/campus/controller/admin/AdminDashboardControllerTests.java`
- Modify: `frontend/src/views/admin/AdminDashboardView.vue`
- Modify: `frontend/src/views/admin/AdminDashboardView.spec.js`
- Modify: `frontend/src/api/admin.js`
- Create: `backend/src/main/java/com/campus/controller/admin/AdminAnalyticsController.java`
- Create: `backend/src/main/java/com/campus/service/AdminAnalyticsService.java`
- Create: `backend/src/main/java/com/campus/dto/AdminAnalyticsSummaryResponse.java`
- Create: `backend/src/main/java/com/campus/mapper/AdminAnalyticsReadMapper.java`
- Create: `frontend/src/views/admin/AdminOperationsAnalyticsView.vue`
- Create: `frontend/src/views/admin/AdminOperationsAnalyticsView.spec.js`

**Scope:**

- 提供注册趋势、发帖趋势、资料下载排行、标签比例等运营数据。
- 给管理端提供独立运营分析视图，而不是把所有内容都塞进现有总览页。
- 支持基础导出能力，建议优先 CSV。
- 先做 `7D / 30D` 两档，避免复杂自定义时间范围。

**Suggested Metrics:**

- 新注册用户数趋势
- 发帖数趋势
- 资料上传数趋势
- 资料下载排行
- 社区标签分布
- 活跃近似指标

**Dependency:**

- 和 Member 4 对齐缓存与性能口径，避免看板查询拖慢在线接口。

**Acceptance:**

- 管理员可从后台进入运营看板。
- 后端返回稳定的汇总结构和趋势数组。
- 图表和表格至少各有一类真实数据展示。

**Suggested Commit Sequence:**

- `feat: add admin analytics summary api`
- `feat: add admin analytics dashboard view`
- `feat: add admin analytics export`

### Member 4: Redis Cache / Platform Hardening Lead

**Requirements:**

- Redis 排行与缓存
- 缓存降级
- 全局输入防护
- 接口文档同步

**Files:**

- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/resources/application-local.yml`
- Modify: `backend/src/main/java/com/campus/service/DiscoverService.java`
- Modify: `backend/src/main/java/com/campus/service/HomeService.java`
- Modify: `backend/src/main/java/com/campus/service/CommunityService.java`
- Modify: `backend/src/test/java/com/campus/controller/DiscoverControllerTests.java`
- Modify: `backend/src/test/java/com/campus/controller/HomeControllerTests.java`
- Modify: `backend/src/test/java/com/campus/controller/CommunityControllerTests.java`
- Modify: `backend/src/test/java/com/campus/config/ApplicationConfigSafetyTests.java`
- Modify: `backend/pom.xml`
- Create: `backend/src/main/java/com/campus/config/RedisIntegrationProperties.java`
- Create: `backend/src/main/java/com/campus/config/OpenApiConfiguration.java`
- Create: `backend/src/main/java/com/campus/web/InputSanitizer.java`
- Create: `backend/src/main/java/com/campus/web/SanitizingRequestAdvice.java`

**Scope:**

- 优先缓存以下读接口：
  - `/api/discover`
  - `/api/home/summary` 的 discover preview 部分
  - `/api/community/hot`
- 当 Redis 未启用或不可用时，自动回退到数据库直查。
- 提供统一输入清洗机制，先覆盖字符串类请求参数和表单输入。
- 接入 OpenAPI / Swagger，至少让后端接口有同步生成文档的入口。

**Do Not Own:**

- 不接手管理看板的指标定义。
- 不负责搜索结果高亮契约。

**Acceptance:**

- `REDIS_ENABLED=false` 时系统可正常运行。
- Redis 故障时 discover / hot / home 不应该整体 500。
- `/swagger-ui` 或等效接口文档入口可访问。
- 输入清洗不破坏正常中文内容提交。

**Suggested Commit Sequence:**

- `feat: add redis cache integration for ranking reads`
- `feat: add request sanitizing layer`
- `docs: expose openapi api docs`

### Member 5: Upload Workflow / Notification Coverage Lead

**Requirements:**

- 分片续传
- `FR-NOTIFICATION-004` 通知类型覆盖

**Files:**

- Modify: `backend/src/main/java/com/campus/common/NotificationType.java`
- Modify: `backend/src/main/java/com/campus/service/NotificationService.java`
- Modify: `backend/src/main/java/com/campus/service/CommunityService.java`
- Modify: `backend/src/main/java/com/campus/service/AdminResourceService.java`
- Modify: `backend/src/main/java/com/campus/controller/ResourceController.java`
- Modify: `backend/src/main/java/com/campus/service/ResourceService.java`
- Modify: `backend/src/test/java/com/campus/controller/NotificationControllerTests.java`
- Modify: `backend/src/test/java/com/campus/controller/ResourceControllerTests.java`
- Modify: `backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java`
- Modify: `frontend/src/api/resources.js`
- Modify: `frontend/src/views/ResourceUploadView.vue`
- Modify: `frontend/src/views/ResourceUploadView.spec.js`
- Modify: `frontend/src/components/ResourceEditorForm.vue`
- Modify: `frontend/src/views/NotificationCenterView.vue`
- Create: `backend/src/main/java/com/campus/dto/ChunkUploadInitResponse.java`
- Create: `backend/src/main/java/com/campus/dto/ChunkUploadMergeResponse.java`

**Scope A: Notification Coverage**

- 补齐点赞通知。
- 补齐资料审核通过 / 驳回 / 下线相关通知。
- 在通知中心补齐对应类型文案映射。

**Scope B: Chunk Upload**

- 给资源上传增加“初始化上传会话 / 分片上传 / 合并完成”链路。
- 允许大文件中断后继续上传。
- 保持最终资源审核流程不变，分片上传只改变上传方式，不改变资源状态机。

**Dependency:**

- 前端 chunk UI 需要和 Member 2 保持基本交互一致性。
- 若 Member 4 的输入清洗层改动了上传请求处理，需要提前联调。

**Acceptance:**

- 新通知类型可在通知中心正确展示。
- 点赞和资源审核动作能落通知记录。
- 上传大文件时可以看到 upload session 进度。
- 中断后再次上传可以续传而不是整文件重来。

**Suggested Commit Sequence:**

- `feat: add notification coverage for likes and resource review`
- `feat: add resource chunk upload session api`
- `feat: add resource upload resume flow`

## Cross-Team Risks

- Member 1 与 Member 2 的搜索高亮字段若反复修改，会拖慢两边进度。
- Member 3 与 Member 4 如果都改后台首页入口，要提前确定是“扩展原页面”还是“新增分析页”。
- Member 5 的分片上传如果直接重构现有资源编辑表单，容易和已有上传 / 编辑回归发生冲突，建议先保持 create-only。

## Suggested Sprint Breakdown

### Sprint 1

- Member 1: 搜索后端网关、ES 开关、fallback
- Member 2: 搜索结果高亮 UI
- Member 3: 运营分析后端接口
- Member 4: Redis 缓存与降级
- Member 5: 通知类型补齐

### Sprint 2

- Member 3: 运营图表与导出
- Member 4: OpenAPI 与输入防护
- Member 5: 分片上传与断点续传
- Member 1 / Member 2: 搜索联调与体验修正

## Done Definition

每条主线完成时，至少满足：

- 对应需求在 README 中不再属于“明确未实现”
- 后端有控制器 / 服务 / 测试闭环
- 前端有页面 / 组件 / 测试闭环
- 关键行为有文档说明

## Out Of Current Round

以下事项建议不放进本轮 5 人主计划：

- 简历重命名 / 替换
- 完整版本历史
- 更复杂的秒传 / 文件去重
- Redis 全站通用缓存重构
- 搜索相关性的进一步算法优化
