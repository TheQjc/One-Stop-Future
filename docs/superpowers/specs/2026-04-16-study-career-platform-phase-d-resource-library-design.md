# 校园一站式信息平台 Phase D 资料库模块设计说明

> **Validation note:** This design was implemented and validated on 2026-04-16. Execution record: `docs/superpowers/plans/2026-04-16-study-career-platform-phase-d-resource-library-implementation.md`. Documented verification covered `CampusApplicationTests`, `ApplicationConfigSafetyTests`, `ResourceControllerTests`, `UserControllerTests`, `HomeControllerTests`, `AdminResourceControllerTests`, `ResourcesListView.spec.js`, `ResourceDetailView.spec.js`, `ResourceUploadView.spec.js`, `HomeView.spec.js`, `ProfileFavoritesView.spec.js`, `ProfileResourcesView.spec.js`, `AdminResourceManageView.spec.js`, `ProfileView.spec.js`, full backend/frontend regression, and local smoke.

## 1. 文档目标

本文档用于确认社区首期和岗位聚合首期完成后的下一阶段子项目范围，即 `资料库首期`。

目标不是一次性做完正式需求中的全部 P1 资料能力，而是在现有 Spring Boot + Vue 基础上，优先交付一个可上传、可审核、可公开浏览、可下载、可收藏、可在个人中心查看状态的最小闭环，为后续统一搜索、MinIO 对象存储接入和运营看板提供稳定接入面。

## 2. 当前结论

### 2.1 子项目定位

Phase D 选择 `资料库首期` 作为下一子项目，原因如下：

1. 社区和岗位已经分别提供了内容互动与信息聚合基础，资料库补齐后，统一搜索才具备完整的三类内容源。
2. 资料库比统一搜索更适合先形成独立闭环，因为它可以先在本地文件系统上完成真实上传、审核、发布和下载链路。
3. 资料收藏可以继续复用现有通用收藏模型，为后续统一搜索结果聚合和个人中心统一收藏视图保持一致结构。

### 2.2 首期交付原则

资料库首期按以下原则收口：

- 采用真实文件流，不做纯元数据占位。
- 文件先落本地文件系统，不强依赖 MinIO。
- 通过存储抽象隔离底层实现，为后续切换 MinIO 保留清晰边界。
- 登录用户可上传资料，管理员负责审核、驳回和下架。
- 公开前台只展示 `PUBLISHED` 资料。
- 登录用户可下载已发布资料，并维护下载计数。
- 登录用户可收藏资料，个人中心继续复用统一收藏入口。
- 个人中心本期提供只读 `我的资料` 列表，不支持原地编辑和重提。
- 本期不接入统一搜索，不做资料排行、运营看板和复杂审核工作流。

## 3. 范围定义

### 3.1 本期范围

本期资料库模块包含：

- 资料列表页
- 资料详情页
- 资料上传页
- 我的资料页
- 资料收藏 / 取消收藏
- 资料下载
- 管理员资料审核台
- 首页 `resources` 入口从占位变为真实可进入模块
- 个人中心收藏页新增 `RESOURCE` 类型切换

### 3.2 明确不做

以下能力不属于本期范围：

- 分片上传和断点续传
- 批量上传、批量审核、批量下架
- 资料在线预览
- 驳回后原地编辑并重新提交
- 下载排行、热门资料榜、运营分析
- 与统一搜索的真实联动
- MinIO 正式接入
- Elasticsearch 搜索索引接入
- 资料评论、点赞、举报
- 资料下载通知

## 4. 角色与权限

| 角色 | 可浏览资料列表 / 详情 | 可上传资料 | 可下载资料 | 可收藏资料 | 可查看我的资料 | 可查看资料收藏 | 可审核资料 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 游客 | 是 | 否 | 否 | 否 | 否 | 否 | 否 |
| 普通用户 | 是 | 是 | 是 | 是 | 是 | 是 | 否 |
| 已认证用户 | 是 | 是 | 是 | 是 | 是 | 是 | 否 |
| 管理员 | 是 | 是 | 是 | 是 | 是 | 是 | 是 |

补充规则：

- 资料上传、下载、收藏必须依赖现有 JWT 登录态。
- 管理能力必须在后端单独做角色校验，不能只依赖前端菜单隐藏。
- 管理员在前台仍视作普通登录用户，但额外拥有资料审核和下架权限。

## 5. 信息架构

### 5.1 页面结构

资料库首期页面结构如下：

- `/resources`
  资料列表页，支持关键词和分类筛选，只展示已发布资料。
- `/resources/:id`
  资料详情页，展示资料摘要、分类、上传者、发布时间、下载次数和下载入口。
- `/resources/upload`
  上传页，登录用户可提交资料元数据和文件。
- `/profile/resources`
  我的资料页，登录用户查看自己上传的资料及状态。
- `/profile/favorites`
  继续复用现有收藏页，但新增 `RESOURCE` 类型切换。
- `/admin/resources`
  管理员审核台。

### 5.2 与现有模块的衔接方式

- 首页继续沿用现有聚合结构，但 `resources` 入口不再是 `COMING_SOON` 占位，而是正式可用模块。
- 个人中心继续沿用现有 `/profile` 体系，在快捷入口中增加 `我的资料`。
- 收藏继续复用现有统一收藏壳层，不新增第二套资料收藏页面。
- 管理后台继续沿用现有最小后台结构，在认证审核、社区治理、岗位维护之外新增资料审核台。

## 6. 资料模型设计

### 6.1 资料主对象

建议新增 `ResourceItem` 实体，首期保持单表主模型，至少包含：

- `id`
- `title`
- `category`
- `summary`
- `description`
- `status`
- `uploaderId`
- `reviewedBy`
- `rejectReason`
- `fileName`
- `fileExt`
- `contentType`
- `fileSize`
- `storageKey`
- `downloadCount`
- `favoriteCount`
- `publishedAt`
- `reviewedAt`
- `createdAt`
- `updatedAt`

字段约束建议：

- `title` 必填，限制在 120 字以内。
- `summary` 必填，限制在 300 字以内，用于列表摘要。
- `description` 可选，用于详情补充说明，长度可控制在 10000 字以内。
- `fileName` 保留用户原始上传文件名，用于下载展示。
- `fileExt` 为小写扩展名，用于校验和展示。
- `contentType` 存储服务器识别出的 MIME 类型。
- `storageKey` 仅存相对存储路径，不暴露物理磁盘绝对路径。

### 6.2 固定枚举

资料分类首期固定为：

- `EXAM_PAPER`
- `LANGUAGE_TEST`
- `RESUME_TEMPLATE`
- `INTERVIEW_EXPERIENCE`
- `OTHER`

资料状态定义为：

- `PENDING`
  用户上传后待审核，仅上传者和管理员可见。
- `PUBLISHED`
  审核通过，前台列表和详情可见，可下载。
- `REJECTED`
  审核驳回，仅上传者和管理员可见。
- `OFFLINE`
  管理员下架，前台不可见。

### 6.3 收藏模型复用

不新增资料收藏表，直接复用现有 `UserFavorite`：

- `targetType` 新增 `RESOURCE`
- `targetId` 指向资料主键

这样可以继续复用 `/api/users/me/favorites`，保证帖子、岗位、资料三类收藏查询结构一致。

## 7. 存储抽象与文件落盘方案

### 7.1 存储接口

建议新增 `ResourceFileStorage` 抽象，至少提供以下能力：

- `store(...)`
- `open(...)`
- `delete(...)`
- `exists(...)`

当前实现为 `LocalResourceFileStorage`，后续切换 MinIO 时仅替换该实现，不改控制器、服务协议和前端页面流程。

### 7.2 本机文件系统方案

首期本机落盘规则建议如下：

- 默认根目录：`backend/.local-storage/resources/`
- 数据库存储相对 `storageKey`
- 文件命名采用时间目录 + UUID，避免重名覆盖
- 示例：`resources/2026/04/16/550e8400-e29b-41d4-a716-446655440000.pdf`

补充规则：

- 下载由后端流式返回，不把磁盘路径暴露给前端。
- 当 MinIO 关闭时，所有环境均可继续使用本地文件系统实现。
- `local` profile 默认使用本地文件系统，不启用 MinIO。

## 8. 状态流转与数据流

### 8.1 上传流

资料上传流转为：

1. 登录用户在 `/resources/upload` 提交表单和文件。
2. 系统校验文件类型、大小和基础字段。
3. 文件写入本地存储目录。
4. 数据库新增 `PENDING` 状态资料记录。
5. 上传者在 `/profile/resources` 中可看到上传结果和审核状态。

### 8.2 审核流

资料审核流转为：

1. 管理员在 `/admin/resources` 查看资料列表。
2. 对 `PENDING` 资料执行 `publish` 或 `reject`。
3. `publish` 后写入 `publishedAt` 与 `reviewedAt`，状态变为 `PUBLISHED`。
4. `reject` 时必须填写驳回原因，状态变为 `REJECTED`。
5. 管理员可将 `PUBLISHED` 资料改为 `OFFLINE`。
6. `OFFLINE` 资料可再次发布回 `PUBLISHED`。

### 8.3 下载流

资料下载流转为：

1. 登录用户打开资料详情页。
2. 点击下载按钮，调用下载接口。
3. 后端校验资料状态必须为 `PUBLISHED`。
4. 打开文件流并返回下载响应。
5. 成功返回后累加 `downloadCount`。

### 8.4 收藏流

- 登录用户可收藏或取消收藏资料。
- 收藏记录通过 `FavoriteTargetType.RESOURCE` 写入现有收藏表。
- 个人中心收藏页通过 `type=RESOURCE` 查询。
- 资料详情页和列表卡片可展示 `favoritedByMe` 状态。

## 9. 接口边界

### 9.1 公共 / 用户侧接口

建议资料相关接口如下：

- `GET /api/resources`
  获取资料列表，支持：
  - `keyword`
  - `category`
- `GET /api/resources/{id}`
  获取资料详情。
- `GET /api/resources/{id}/download`
  下载资料文件。
- `POST /api/resources`
  上传资料，使用 `multipart/form-data`。
- `GET /api/resources/mine`
  获取我的资料列表。
- `POST /api/resources/{id}/favorite`
  收藏资料。
- `DELETE /api/resources/{id}/favorite`
  取消收藏资料。
- `GET /api/users/me/favorites?type=RESOURCE`
  获取我的资料收藏。

### 9.2 管理侧接口

建议后台接口如下：

- `GET /api/admin/resources`
  获取资料审核列表。
- `POST /api/admin/resources/{id}/publish`
  审核通过。
- `POST /api/admin/resources/{id}/reject`
  审核驳回。
- `POST /api/admin/resources/{id}/offline`
  下架资料。

后台列表至少展示：

- 标题
- 分类
- 上传者昵称
- 文件名
- 文件大小
- 下载次数
- 状态
- 上传时间
- 审核时间

## 10. 前端交互设计

### 10.1 资料列表页

列表页职责：

- 展示资料库聚合说明区
- 展示关键词和分类筛选条
- 展示资料卡片列表
- 为游客统一提供 `查看详情`
- 为登录用户统一提供 `下载` 和 `收藏 / 取消收藏`
- 展示空态、加载态和失败重试态

本期排序保持简单：

- 默认按 `publishedAt` 倒序
- 不做热门排序和下载排行

### 10.2 资料详情页

详情页职责：

- 展示完整标题、分类、摘要和补充说明
- 展示上传者、发布时间、下载次数
- 提供下载按钮
- 提供收藏 / 取消收藏按钮
- 游客只可阅读，下载和收藏动作引导登录

### 10.3 上传页

上传页仅保留最小表单：

- 分类
- 标题
- 摘要
- 补充说明
- 文件选择

提交成功后建议跳转到 `/profile/resources`，让用户直接看到当前资料状态。

### 10.4 我的资料页

我的资料页职责：

- 展示我上传的资料列表
- 展示状态、上传时间、驳回原因
- 不提供原地编辑和删除

这样可以在本期范围内满足“上传记录可见”，同时避免提前引入复杂重提流。

### 10.5 管理员审核页

管理员审核页职责：

- 查看资料审核列表
- 查看待审核资料核心信息
- 执行通过、驳回、下架
- 在驳回时填写原因

本期不做批量审核，不做高级筛选面板，不做操作日志面板。

## 11. 异常处理与后续接入位

至少覆盖以下规则：

- 游客访问上传、下载、收藏接口时返回 `401`
- 普通用户访问后台资料审核接口时返回 `403`
- 普通用户访问 `PENDING / REJECTED / OFFLINE` 资料详情时返回 `404`
- 文件类型不在 `PDF / DOCX / PPTX / ZIP` 范围内时返回 `400`
- 文件超过 `100MB` 时返回 `400`
- 驳回原因缺失时返回 `400`
- 下载时若数据库记录存在但底层文件缺失，返回统一错误结构并提示文件不可用

继续沿用现有统一响应格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

为后续 MinIO 和统一搜索保留接入位：

- 前端继续使用 `/resources` 固定路由，避免后续迁移。
- 后端通过 `ResourceFileStorage` 抽象隔离本地存储和 MinIO。
- 列表筛选参数命名保持稳定，后续可直接作为统一搜索的 `resources` 子域过滤条件。

## 12. 测试与验收

### 12.1 后端测试重点

- 资料表存在性检查
- 上传权限校验
- 文件类型和大小校验
- 公开列表 / 详情可见性
- 下载计数递增
- 资料收藏幂等
- 我的资料列表查询
- 管理员审核状态流转
- 普通用户越权访问后台接口

### 12.2 前端测试重点

- 资料列表页筛选与空态
- 资料详情页下载与收藏交互
- 上传页表单和文件校验提示
- 我的资料状态列表
- 管理员审核页动作按钮和驳回表单
- 首页 `resources` 入口真实跳转
- 收藏页 `RESOURCE` 类型切换

### 12.3 本机冒烟验收

至少验证以下链路：

1. 登录用户可上传资料。
2. 上传后在 `我的资料` 中可看到 `PENDING` 状态。
3. 管理员可审核通过资料。
4. 审核通过后，游客可浏览资料列表和详情。
5. 登录用户可下载资料并看到下载次数增加。
6. 登录用户可收藏资料，并在个人中心收藏页看到 `RESOURCE` 记录。
7. 管理员下架后，前台公共列表和详情不再可见该资料。

## 13. 实施顺序建议

建议按以下顺序实施：

1. 先冻结资料表结构、枚举和收藏类型扩展
2. 再实现文件存储抽象与上传 / 下载后端链路
3. 再实现公开列表 / 详情 / 收藏 / 我的资料接口
4. 再实现管理员审核接口
5. 再接前端资料列表、详情、上传、我的资料、管理员审核页
6. 最后接首页入口、收藏页类型切换、README 和本机联调

## 14. 待确认项

当前无新增待确认项。资料库首期设计已确认完成，可进入实现计划阶段。
