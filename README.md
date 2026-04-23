# 一站式成长平台（One-Stop Future）

当前仓库状态：`Phase A 基础能力 + Phase B 社区 + Phase C 岗位 + Phase D 资料库首个切片 + Phase E 统一搜索首个切片 + Phase F 趋势榜首个切片 + Phase G 资料生命周期完善首个切片 + Phase H 资料预览扩展首个切片 + Phase I MinIO 原始资料存储首个切片 + Phase J 历史本地资料 MinIO 迁移首个切片 + Phase K 决策支持首个切片 + Phase L 决策分析首个切片 + Phase M 管理端总览首个切片 + Phase N 岗位申请与简历流程首个切片 + Phase O 管理端用户状态管理首个切片 + Phase P 社区热榜首个切片 + Phase Q 经验贴结构化首个切片 + Phase R 社区分层回复首个切片 + Phase S DOCX 资料预览首个切片 + Phase T MinIO 预览产物存储首个切片 + Phase U 管理端岗位批量导入首个切片 + Phase V 第三方岗位同步首个切片 + Phase W 历史预览产物 MinIO 迁移首个切片 + Phase X 预览产物运行时双读兜底首个切片 + Phase Y 预览产物清理首个切片 + Phase Z 简历在线预览首个切片 + Phase AA 申请快照在线预览首个切片`。

## 当前范围

当前已实现：

- 手机验证码注册 / 登录
- 账号状态控制与封禁后禁止登录
- 独立聚合首页
- 个人中心与学生认证申请流程
- 通知中心
- 社区列表 / 详情 / 发布 / 评论 / 回复 / 点赞 / 收藏
- `/community` 内的 `DAY / WEEK / ALL` 社区热榜
- 在发帖、列表、详情流程里可选启用的经验贴结构
- 社区一级分层回复与回复通知
- 我的帖子 / 我的收藏
- 岗位列表 / 详情 / 筛选 / 来源跳转
- 岗位收藏 / 取消收藏
- 简历库上传 / 列表 / 预览 / 下载 / 删除
- 站内一次性岗位申请与简历快照
- 我的申请历史，以及快照预览 / 下载
- 管理端只读申请工作台，以及快照预览 / 下载
- 公开资料列表 / 详情 / 上传
- 资料预览 / 下载 / 收藏 / 取消收藏
- 我的资料 / 资料收藏 / 驳回后编辑重提
- 管理端认证审核
- 管理端社区治理
- 管理端岗位创建 / 编辑 / 发布 / 下线 / 删除
- `/admin/jobs` 上基于 UTF-8 CSV 的管理端岗位批量导入；导入成功的记录以 `DRAFT` 创建，校验失败时整文件回滚
- `/admin/jobs` 上基于固定 HTTP JSON 数据源的第三方岗位同步；新记录以 `DRAFT` 创建，已存在且非 `DELETED` 的记录按 `sourceUrl` 原地更新，跳过项和无效项在同步结果中统一汇总
- 非 `local` 运行环境下使用 MinIO 存储原始资料，并支持为新生成的 `PPTX` / `DOCX` / `ZIP` 预览产物单独启用基于 MinIO 的存储
- 管理端历史本地资料向 MinIO 迁移，支持 dry-run 和有界批处理
- 管理端历史预览产物向 MinIO 迁移，支持 dry-run 和有界批处理
- 管理端只读总览页，可跳转到现有工作台
- 管理端用户状态工作台，可对非管理员账号执行封禁 / 恢复
- 面向已发布帖子 / 岗位 / 资料的统一搜索
- 面向已发布帖子 / 岗位 / 资料的趋势榜
- 首页每周趋势预览
- 趋势榜 / 首页趋势预览中，经验贴会获得一个稳定且较小的额外加分，但不会改变社区时间流排序
- 登录用户可进行决策测评、提交结果并查看最近一次测评结果
- 登录用户可查看带稳定锚点日期兜底的方向时间线
- 面向 `EXAM` / `ABROAD` 的公开院校候选列表和 `2-4` 所院校对比
- 首页中的测评入口已激活
- `/analytics` 上的公开决策分析工作台，支持 `7D / 30D` 总览、趋势与方向占比
- 登录用户在同一个 `/analytics` 页面中可查看个人决策快照、历史记录与下一步建议
- 首页中的分析入口对游客和登录用户都已激活
- 可见资料支持 PDF 内联预览
- PPTX 通过缓存后的 PDF 转换结果进行内联预览
- DOCX 通过可配置的 `soffice` 转换为缓存 PDF 后进行内联预览
- ZIP 支持目录树内联预览

当前明确未实现：

- 完整的管理后台运营看板、DAU / 漏斗指标、可导出分析报表
- 版本历史、分片上传、简历重命名 / 替换

## 项目结构

- `backend/`: Spring Boot 3、Spring Security、MyBatis-Plus、JWT
- `frontend/`: Vue 3、Pinia、Vue Router、Axios、Vite、Vitest
- `docs/superpowers/`: 需求、规格与实施计划
- `backend/.local-storage/resources/`: `local` profile 下默认的本地原始资料存储目录
- `backend/.local-storage/previews/`: `local` profile 下默认的 PPTX 转 PDF、DOCX 转 PDF、ZIP 预览产物缓存目录；非 `local` 运行环境也可通过 `RESOURCE_PREVIEW_TYPE=minio` 将新生成的预览产物写入 MinIO
  - 当前预览行为通过指纹失效并写入新产物来更新预览缓存
  - 将预览存储从本地切换到 MinIO 不会自动迁移历史本地预览产物
  - 仅当 `RESOURCE_PREVIEW_READ_FALLBACK_LOCAL_ENABLED=true` 时，运行时读取才支持 `MinIO 优先 -> 历史本地兜底`
  - 管理员也可以通过 `POST /api/admin/resources/migrate-preview-artifacts-to-minio` 手动触发历史预览产物迁移
  - 当能够精确识别旧的逻辑预览 key 时，被驳回资料重提后会尽力清理旧的派生预览产物
  - 资料的被动交互和管理端状态流转不再因无关的 `updatedAt` 写入而轮换预览产物 key
  - 当前阶段仍未引入递归扫描预览根目录或定时清理预览垃圾的机制
  - 本地开发若要重置派生预览状态，请先停止后端，再删除 `backend/.local-storage/previews/`

## 本地运行

### 后端

```bash
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

说明：

- 本地开发必须使用 `local` profile
- `local` 使用内置 H2、种子数据、本地文件系统资料存储和本地文件系统预览产物存储
- 当前默认路径均为相对路径；在 `cd backend` 后，它们会解析到 `backend/.local-storage/resources/` 和 `backend/.local-storage/previews/`
- `application-local.yml` 固定了 `RESOURCE_PREVIEW_TYPE=local`，因此本地开发时预览产物仍保存在磁盘
- 资料库里的 `DOCX` 预览依赖 LibreOffice `soffice` 在 `PATH` 中，或通过 `RESOURCE_PREVIEW_DOCX_SOFFICE_COMMAND` 指向实际安装路径
- 本地后端地址：`http://127.0.0.1:8080`

### 前端

```bash
cd frontend
npm install
npm run dev -- --host 127.0.0.1
```

说明：

- 前端地址通常为 `http://127.0.0.1:5173`
- 如果 `5173` 被占用，Vite 会自动选择下一个可用端口；请以开发服务器日志输出为准
- Vite 会把 `/api/**` 代理到 `http://127.0.0.1:8080`
- 当前推荐的开发方式仍然是本地后端 + 本地前端，而不是 Docker

### 可选 Docker 运行栈

如果本地已安装 Docker 和 Docker Compose，仓库中也提供了一个可选的部署脚手架：

```bash
docker compose up --build
```

说明：

- 前端暴露在 `http://127.0.0.1:5173`
- MySQL 暴露在 `127.0.0.1:3306`
- MinIO API 暴露在 `http://127.0.0.1:9000`
- MinIO 控制台暴露在 `http://127.0.0.1:9001`
- 后端只暴露在 Compose 网络内部，并通过前端 Nginx 代理访问
- 后端会将原始资料文件存入 MinIO，并将预览产物保存在 `backend-data` 命名卷中
- 当前 `docker-compose.yml` 还没有设置 `RESOURCE_PREVIEW_TYPE=minio`，因此这个 Compose 脚手架仍会把预览产物保存在本地卷中
- 当前后端镜像没有安装 LibreOffice `soffice`，所以 DOCX 预览生成默认并不适合直接在容器里使用
- 日常开发当前仍推荐使用前面的本地后端 + 本地前端方案
- 若要把现有的本地文件数据库切换到 MinIO，这一阶段仍然是管理员手动触发的后端迁移流程，而不是运行时自动切换

## 预览产物存储

后端的预览产物存储与原始资料存储是分开配置的：

- `RESOURCE_PREVIEW_TYPE=local|minio`
- `RESOURCE_PREVIEW_LOCAL_ROOT=.local-storage/previews`
- `RESOURCE_PREVIEW_MINIO_PREFIX=preview-artifacts`
- `RESOURCE_PREVIEW_READ_FALLBACK_LOCAL_ENABLED=false`
- `RESOURCE_PREVIEW_DOCX_SOFFICE_COMMAND=soffice`

当前行为：

- `RESOURCE_PREVIEW_TYPE=local` 会把预览产物保存在本地文件系统
- `RESOURCE_PREVIEW_TYPE=minio` 需要 `MINIO_ENABLED=true`，并复用共享的 `MINIO_BUCKET`
- 预览产物存储的选择与 `RESOURCE_STORAGE_TYPE` 相互独立
- 切换到 `minio` 后，只有新生成的预览产物才会写入 MinIO
- 当 `RESOURCE_PREVIEW_TYPE=minio` 且 `RESOURCE_PREVIEW_READ_FALLBACK_LOCAL_ENABLED=true` 时，运行时读取会先查 MinIO，再回退到现有本地预览目录中的历史 `PPTX`、`DOCX`、`ZIP` 产物
- 本地兜底命中只读，不会自动把产物复制进 MinIO
- MinIO 基础设施故障不会被本地兜底掩盖
- 管理员可调用 `POST /api/admin/resources/migrate-preview-artifacts-to-minio` 对历史预览产物执行 dry-run 或实际迁移
- 迁移只针对每个合格资料当前逻辑上的 `PPTX`、`DOCX` 或 `ZIP` 预览产物
- 迁移成功后，本地源预览产物仍会保留
- 当能够精确识别旧逻辑预览 key 时，被驳回资料重提后会尽力清理旧的派生预览产物
- 资料的被动交互和管理端状态流转不再因无关的 `updatedAt` 写入而轮换预览产物 key
- 当前阶段仍未引入递归扫描预览根目录或定时清理预览垃圾的机制

## 本地演示账号

当后端以 `local` profile 启动时，内置种子账号如下：

- 管理员：`13800000000`（`平台管理员`）
- 普通用户：`13800000001`（`普通同学`）
- 已认证用户：`13800000002`（`认证同学`）

登录使用手机验证码认证：

- 打开 `/login`
- 为目标手机号请求登录验证码
- 在 `local` profile 下，后端会返回一个可立即使用的调试验证码

## 测试与构建

### 后端

```bash
cd backend
mvn -q test
```

### 前端测试

```bash
cd frontend
npm run test
```

### 前端构建

```bash
cd frontend
npm run build
```

## 关键路由

公开 / 用户侧：

- `/`
- `/login`
- `/register`
- `/search`
- `/discover`
- `/community`
- `/community/:id`
- `/community/create`
- `/jobs`
- `/jobs/:id`
- `/resources`
- `/resources/:id`
- `/resources/:id/edit`
- `/resources/upload`
- `/assessment`
- `/analytics`
- `/timeline`
- `/schools/compare`
- `/profile`
- `/profile/posts`
- `/profile/favorites`
- `/profile/resumes`
- `/profile/applications`
- `/profile/resources`
- `/notifications`

管理端：

- `/admin/dashboard`
- `/admin/users`
- `/admin/applications`
- `/admin/verifications`
- `/admin/community`
- `/admin/jobs`
- `/admin/resources`

## 基础认证、首页、个人中心、通知与认证

后端接口：

- `POST /api/auth/codes/send`
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/home/summary`
- `GET /api/users/me`
- `PUT /api/users/me`
- `POST /api/verifications`
- `GET /api/notifications`
- `POST /api/notifications/{id}/read`
- `POST /api/notifications/read-all`

前端路由：

- `/`
- `/login`
- `/register`
- `/profile`
- `/notifications`

当前 Phase A 范围：

- 游客可以打开独立首页、请求手机验证码，并从 `/login` 或 `/register` 进入认证流程
- 已登录用户会在同一个首页入口看到自己的身份态、未读数和下一步建议
- `/profile` 展示当前账号资料以及当前登录用户的认证状态视图
- 当前第一版认证流程下，一个用户同一时间只能提交一个有效认证申请
- `/notifications` 支持欢迎通知、认证通知以及后续工作流通知的列表、单条已读和全部已读
- 管理端的认证审核仍然在 `/admin/verifications` 中完成；无论通过还是驳回，结果都会回流到用户可见状态和通知中

## 资料库、生命周期与审核

后端接口：

- `GET /api/resources`
- `GET /api/resources/mine`
- `GET /api/resources/{id}`
- `POST /api/resources`
- `PUT /api/resources/{id}`
- `POST /api/resources/{id}/favorite`
- `DELETE /api/resources/{id}/favorite`
- `GET /api/resources/{id}/download`
- `GET /api/resources/{id}/preview`
- `GET /api/resources/{id}/preview-zip`
- `GET /api/admin/resources`
- `POST /api/admin/resources/{id}/publish`
- `POST /api/admin/resources/{id}/reject`
- `POST /api/admin/resources/{id}/offline`

前端路由：

- `/resources`
- `/resources/upload`
- `/resources/:id`
- `/resources/:id/edit`
- `/profile/resources`
- `/admin/resources`

当前 Phase D + G + H + S 范围：

- 公开资料列表和详情页支持关键词 / 分类浏览、可见资料详情查看，以及收藏 / 取消收藏
- 已登录用户可以上传资料，在 `/profile/resources` 中查看自己的资料，并通过 `/resources/:id/edit` 对被驳回资料进行重提
- 资料审核仍在 `/admin/resources` 中完成，管理员可以发布、驳回或下线资料
- PDF 支持内联预览；PPTX 和 DOCX 通过缓存后的 PDF 转换结果预览；ZIP 通过目录树接口预览
- 原始资料和预览产物的 MinIO 迁移细节见下方专门的存储章节

## 岗位浏览、详情与收藏

后端接口：

- `GET /api/jobs`
- `GET /api/jobs/{id}`
- `POST /api/jobs/{id}/favorite`
- `DELETE /api/jobs/{id}/favorite`

前端路由：

- `/jobs`
- `/jobs/:id`

当前 Phase C 范围：

- 公开岗位列表支持关键词、城市、岗位类型、学历要求和来源平台筛选
- 岗位详情保留公开来源链接，并能反映当前登录用户的收藏状态
- 已登录用户可以在不离开岗位列表或详情上下文的情况下收藏或取消收藏可见岗位
- 站内申请、简历选择与申请历史见下方单独的工作流章节

## 岗位申请与简历流程

后端接口：

- `GET /api/resumes/mine`
- `POST /api/resumes`
- `GET /api/resumes/{id}/preview`
- `GET /api/resumes/{id}/download`
- `DELETE /api/resumes/{id}`
- `POST /api/jobs/{id}/apply`
- `GET /api/applications/mine`
- `GET /api/applications/{id}/resume/preview`
- `GET /api/applications/{id}/resume/download`
- `GET /api/admin/applications`
- `GET /api/admin/applications/{id}/resume/preview`
- `GET /api/admin/applications/{id}/resume/download`

前端路由：

- `/profile/resumes`
- `/profile/applications`
- `/admin/applications`

当前 Phase N + Phase AA 范围：

- 已登录用户可以在 `/profile/resumes` 中维护多份简历文件
- 支持的简历格式为 `PDF`、`DOC`、`DOCX`
- 已登录用户可以在 `/profile/resumes` 中预览自己的 `PDF` 和 `DOCX` 简历
- `DOC` 简历在当前阶段仍仅支持下载
- 已发布岗位详情页保留外部来源链接，同时支持站内申请
- 同一用户对同一岗位最多只能申请一次
- 每次申请都会保存一个不可变的简历快照，因此删除实时简历不会影响历史下载
- `/profile/applications` 面向申请人且只读，支持 `PDF` / `DOCX` 快照预览，以及 `PDF` / `DOCX` / `DOC` 快照下载
- `/admin/applications` 面向管理员且只读，支持 `PDF` / `DOCX` 快照预览，以及 `PDF` / `DOCX` / `DOC` 快照下载
- 申请快照预览与 `/profile/resumes` 中的实时简历预览是分开的
- 当前阶段中，`DOC` 申请快照仍仅支持下载

## 管理端总览

管理端后端接口：

- `GET /api/admin/dashboard/summary`

管理端前端路由：

- `/admin/dashboard`

当前管理端总览范围：

- 后端汇总接口和前端路由都仅允许管理员访问
- 总览页只读，用于汇总关键管理工作区，不在总览页内直接执行审核或编辑操作
- 汇总内容覆盖认证、社区、岗位和资料，提供数量、近期记录和跳转入口
- 总览卡片会把管理员导向现有工作台，如认证审核、社区治理、岗位管理和资料审核
- 本地验证路径：以管理员身份登录后打开 `/admin/dashboard`，确认首页管理员入口和主导航管理员入口都能进入同一总览路由

## 管理端用户状态管理

管理端后端接口：

- `GET /api/admin/users`
- `POST /api/admin/users/{id}/ban`
- `POST /api/admin/users/{id}/unban`

管理端前端路由：

- `/admin/users`

当前 Phase O 范围：

- 提供仅管理员可访问的用户状态工作台，展示账号总数与当前状态明细
- 管理员账号仍然可见，但在当前阶段不允许被修改状态
- 非管理员账号可以在同一界面中被封禁和恢复
- 被封禁用户在登录时会被拦截，在已认证业务接口上也会持续被拒绝，直到恢复

## 管理端岗位管理、导入与同步

管理端后端接口：

- `GET /api/admin/jobs`
- `POST /api/admin/jobs`
- `PUT /api/admin/jobs/{id}`
- `POST /api/admin/jobs/{id}/publish`
- `POST /api/admin/jobs/{id}/offline`
- `POST /api/admin/jobs/{id}/delete`
- `POST /api/admin/jobs/import`
- `POST /api/admin/jobs/sync`

管理端前端路由：

- `/admin/jobs`

当前 Phase C + U + V 范围：

- 管理端岗位工作台把创建 / 编辑 / 发布 / 下线 / 删除全生命周期集中在一个界面中
- CSV 批量导入通过现有 `/admin/jobs` 页面上传一个 `UTF-8` 文件，并把合法行创建为 `DRAFT`
- 当前阶段的 CSV 导入是全有或全无：只要校验失败，就回滚整个文件，不做部分导入
- 第三方同步在同一个 `/admin/jobs` 工作台中拉取一个固定的服务端 HTTP JSON 数据源，不额外增加管理端路由
- 新同步到的岗位会以 `DRAFT` 创建；已存在且非 `DELETED` 的记录按 `sourceUrl` 原地更新
- 本地已经标记为 `DELETED` 的岗位会被跳过并记录到结果中，而不会被自动重新创建
- 无论是 CSV 导入还是第三方同步，在当前阶段都不会自动发布岗位

## 管理端认证审核

管理端后端接口：

- `GET /api/admin/verifications/dashboard`
- `GET /api/admin/verifications`
- `POST /api/admin/verifications/{id}/review`

管理端前端路由：

- `/admin/verifications`

当前 Phase A 范围：

- 管理员专用认证总览会展示待审核与近期已审核申请数量
- 现有的 `/admin/verifications` 工作台会列出已提交的认证申请供审核
- 管理员可以在同一个工作台中逐条通过或驳回申请
- 审核通过会把申请人升级为 `VERIFIED`；驳回则把用户退回未认证路径
- 审核结果会生成面向用户的通知，并让整个审核闭环留在管理端认证工作台中

## 管理端社区治理

管理端后端接口：

- `GET /api/admin/community/posts`
- `POST /api/admin/community/posts/{id}/hide`
- `POST /api/admin/community/posts/{id}/delete`

管理端前端路由：

- `/admin/community`

当前 Phase B 范围：

- 社区治理仍保留在现有 `/admin/community` 工作台中，仅管理员可访问
- 管理员可以在同一个治理列表中审核社区帖子
- 隐藏帖子会让它从公开视图中消失，但不会在 UI 合约上被视作硬删除
- 删除帖子是显式的管理端治理操作，用于处理不应继续保留的内容

## 社区流与互动

后端接口：

- `GET /api/community/posts`
- `GET /api/community/posts/mine`
- `GET /api/community/posts/{id}`
- `POST /api/community/posts`
- `POST /api/community/posts/{id}/comments`
- `POST /api/community/comments/{id}/replies`
- `POST /api/community/posts/{id}/like`
- `DELETE /api/community/posts/{id}/like`
- `POST /api/community/posts/{id}/favorite`
- `DELETE /api/community/posts/{id}/favorite`

前端路由：

- `/community`
- `/community/create`
- `/community/:id`
- `/profile/posts`
- `/profile/favorites`

当前 Phase B 范围：

- 公开社区流支持已发布帖子的列表与详情浏览
- 已登录用户可以发帖、发布顶层评论、点赞帖子和收藏帖子
- `/profile/posts` 和 `/profile/favorites` 提供当前登录用户的发帖视图和收藏视图
- 热榜、经验贴和分层回复等增强能力在下方社区专属章节中单独说明

## 个人帖子与收藏

后端接口：

- `GET /api/community/posts/mine`
- `GET /api/users/me/favorites?type=POST|JOB|RESOURCE`

前端路由：

- `/profile/posts`
- `/profile/favorites`

当前 Phase B + C + D 范围：

- `/profile/posts` 通过与公开社区流相同的领域模型展示当前登录用户发表的社区帖子
- `/profile/favorites` 可在 `POST`、`JOB`、`RESOURCE` 三种视图之间切换，并复用同一个收藏接口
- 当前阶段的收藏列表保持只读，并与社区、岗位、资料卡片上的收藏状态一致

## 社区热榜

公开后端接口：

- `GET /api/community/hot`

前端路由：

- `/community`

支持的查询参数：

- `period`
- `limit`

支持的周期值：

- `DAY`
- `WEEK`
- `ALL`

当前 Phase P 范围：

- `/community` 现在会在最新帖子流上方展示一个独立的公开热榜区块
- 热榜仅属于社区域，不复用趋势页的接口契约
- 只有已发布帖子会参与排行
- `DAY` 表示最近滚动 24 小时内发布的帖子，按当前累计热度排序
- `WEEK` 表示最近滚动 7 天内发布的帖子，按当前累计热度排序
- `ALL` 表示全量已发布历史
- 当前热度公式为 `likeCount * 3 + commentCount * 4 + favoriteCount * 5 + verifiedAuthorBonus + freshnessBonus`
- 当前阶段不包含 Redis 缓存、交互事件增量排行，也不提供独立的 `/community/hot` 页面

## 社区经验贴

公开后端接口：

- `GET /api/community/posts`
- `GET /api/community/posts/{id}`

登录后后端接口：

- `POST /api/community/posts`

前端路由：

- `/community`
- `/community/create`
- `/community/:id`

当前 Phase Q 范围：

- 社区帖子可选开启 `experiencePost=true`，并携带 `experienceTargetLabel`、`experienceOutcomeLabel`、`experienceTimelineSummary`、`experienceActionSummary`
- 非经验贴保持原有行为，并返回 `experience.enabled=false`
- 当开关启用时，社区列表卡片和详情页会渲染结构化的经验贴摘要
- 趋势榜和首页趋势预览会给经验贴一个小幅且稳定的分数加成
- 该加成不会改变 `/community` 时间流、`/community/hot` 或统一搜索的排序

## 社区分层回复

公开后端接口：

- `GET /api/community/posts/{id}`

登录后后端接口：

- `POST /api/community/posts/{id}/comments`
- `POST /api/community/comments/{id}/replies`

前端路由：

- `/community/:id`
- `/notifications`

当前 Phase R 范围：

- 顶层评论仍通过现有帖子评论接口发布
- 社区帖子详情现在会返回顶层评论及其嵌套 `replies`
- 回复层级在当前阶段仅允许位于顶层评论下一层
- 对回复继续回复会被拒绝
- 回复他人的顶层评论会生成 `COMMUNITY_REPLY_RECEIVED`
- 回复自己的顶层评论不会生成通知
- 通知中心会把新的回复通知类型映射为可读的社区回复标签
- 无限层级线程、回复编辑、回复删除以及评论锚点深链仍不在当前范围内

## 统一搜索

公开后端接口：

- `GET /api/search`

前端路由：

- `/search`

支持的查询参数：

- `q`
- `type`
- `sort`

支持的搜索类型：

- `ALL`
- `POST`
- `JOB`
- `RESOURCE`

支持的排序方式：

- `RELEVANCE`
- `LATEST`

当前搜索范围：

- 仅检索已发布社区帖子
- 仅检索已发布岗位
- 仅检索已发布资料
- 仅返回游客可访问的公开结果

## 趋势榜

公开后端接口：

- `GET /api/discover`

前端路由：

- `/discover`

支持的查询参数：

- `tab`
- `period`
- `limit`

支持的趋势标签：

- `ALL`
- `POST`
- `JOB`
- `RESOURCE`

支持的周期值：

- `WEEK`
- `ALL`

当前趋势范围：

- 在一个公开榜单上对已发布社区帖子、已发布岗位和已发布资料统一排序
- `WEEK` 覆盖最近滚动 7 天，并按当前累计热度排序
- `ALL` 覆盖全部已发布历史
- 首页包含 `discoverPreview` 负载，用于展示每周趋势预览

## 决策支持与分析

后端接口：

- `GET /api/decision/assessment/questions`
- `POST /api/decision/assessment/submissions`
- `GET /api/decision/assessment/latest`
- `GET /api/decision/timeline?track=CAREER|EXAM|ABROAD&anchorDate=YYYY-MM-DD`
- `GET /api/decision/schools?track=EXAM|ABROAD&keyword=...`
- `POST /api/decision/schools/compare`
- `GET /api/analytics/summary?period=7D|30D`

前端路由：

- `/assessment`
- `/timeline`
- `/schools/compare`
- `/analytics`

当前决策支持与分析范围：

- 测评仅对登录用户开放，问题由后端内置种子数据提供
- 提交后会持久化最近一次测评结果，并返回稳定的分数、排序和下一步建议
- 时间线仅对登录用户开放；优先使用显式 `anchorDate`，否则使用最近一次测评会话日期
- 当既没有显式锚点也没有最近结果时，时间线返回 `assessmentRequired=true`
- 院校候选列表与对比接口对 `EXAM` 和 `ABROAD` 公开只读
- 院校对比强制要求 `2-4` 所学校，保留请求顺序，并对缺失值给出显式标记
- `/analytics` 是公开可访问的混合分析工作台
- 游客可以打开 `/analytics`，查看公开总览卡片、趋势卡片和方向占比
- 已登录用户在同一个页面中还能看到个人快照、近期历史和后端给出的下一步建议
- 分析周期由后端控制，目前仅支持 `7D` 与 `30D`
- 首页中的 `assessment` 入口对登录用户直接可用，对游客显示 `LOGIN_REQUIRED`
- 首页中的 `analytics` 对游客和登录用户都可用
- 管理端总览只读且仅管理员可访问
- 完整的管理端运营看板在当前阶段仍不在范围内

## 历史本地资料 MinIO 迁移

管理端后端接口：

- `POST /api/admin/resources/migrate-to-minio`

当前迁移范围：

- 仅后端支持、仅管理员可触发；当前阶段没有前端迁移 UI
- 支持历史原始资料文件的 dry-run 和有界批处理迁移
- 迁移成功后保留原有 `storageKey`，并继续保留本地源文件
- 迁移源文件读取自 `app.resource-storage.local-root`
- 即使当前原始资料存储仍为本地，也要求 `platform.integrations.minio.enabled=true`
- 在基于环境变量的部署方式中，这个启用项由现有映射 `MINIO_ENABLED=true` 提供
- 该迁移流程只覆盖原始资料文件；预览产物请使用单独的管理端接口 `POST /api/admin/resources/migrate-preview-artifacts-to-minio`

## 权限

游客：

- 可以浏览首页、社区、岗位以及已发布资料
- 可以对已发布帖子、岗位和资料使用统一搜索
- 可以浏览公开趋势榜
- 可以打开 `/analytics` 并查看公开决策分析看板
- 可以浏览 `EXAM` / `ABROAD` 的公开院校候选与院校对比
- 可以内联预览已发布 PDF 资料
- 可以以内联转换 PDF 的方式预览已发布 PPTX 资料
- 可以以内联转换 PDF 的方式预览已发布 DOCX 资料
- 可以以目录树方式预览已发布 ZIP 资料
- 不能发布内容、保存收藏或下载资料文件

已登录用户：

- 可以发布社区帖子
- 可以评论 / 回复 / 点赞 / 收藏社区帖子
- 可以收藏岗位
- 可以在 `/profile/resumes` 管理自己的简历库，内联预览 `PDF` / `DOCX` 简历，并下载已保存简历文件
- 可以选择一份简历对已发布岗位发起一次申请
- 可以在 `/profile/applications` 查看自己的申请历史，内联预览 `PDF` / `DOCX` 快照简历，并下载已保存的快照简历
- 可以完成决策测评并查看最近一次结果
- 可以在 `/analytics` 查看个人快照 / 历史 / 下一步建议（若可用）
- 可以在完成测评后打开方向时间线
- 可以上传资料
- 可以预览已发布 PDF、可见 PPTX、可见 DOCX 以及可见 ZIP 目录树
- 可以收藏、取消收藏和下载已发布资料
- 可以从 `/resources/:id/edit` 编辑并重提自己被驳回的资料
- 可以在个人中心查看 `POST`、`JOB`、`RESOURCE` 三类收藏
- 可以在 `/profile/resources` 中查看资料及其生命周期操作

管理员：

- 可以打开 `/admin/dashboard` 查看只读总览，并由此进入现有管理工作台
- 可以打开 `/admin/applications` 内联预览 `PDF` / `DOCX` 申请快照简历，或下载申请快照简历
- 可以审核认证申请
- 可以治理社区帖子
- 可以在 `/admin/jobs` 中维护岗位卡片、导入 UTF-8 CSV 岗位、触发固定数据源岗位同步
- 可以通过发布 / 驳回 / 下线操作审核资料
- 可以在管理端资料看板中内联预览 PDF、PPTX、DOCX 资料以及 ZIP 内容

## 当前枚举与数据形状

收藏目标类型：

- `POST`
- `JOB`
- `RESOURCE`

岗位状态：

- `DRAFT`
- `PUBLISHED`
- `OFFLINE`
- `DELETED`

岗位类型：

- `INTERNSHIP`
- `FULL_TIME`
- `CAMPUS`

学历要求：

- `ANY`
- `BACHELOR`
- `MASTER`
- `DOCTOR`

资料分类：

- `EXAM_PAPER`
- `LANGUAGE_TEST`
- `RESUME_TEMPLATE`
- `INTERVIEW_EXPERIENCE`
- `OTHER`

资料状态：

- `PENDING`
- `PUBLISHED`
- `REJECTED`
- `OFFLINE`

## 手工冒烟检查清单

1. 使用 `mvn spring-boot:run "-Dspring-boot.run.profiles=local"` 启动后端。
2. 使用 `npm run dev -- --host 127.0.0.1` 启动前端。
3. 以游客身份打开 `/community`，确认热榜显示在最新帖子流上方。
4. 以游客身份在社区热榜中切换 `DAY`、`WEEK`、`ALL`。
5. 以任意已登录用户身份打开一个社区详情页，发布一条顶层评论，并确认评论出现在列表中。
6. 使用另一个已登录用户回复这条顶层评论，并确认回复嵌套在正确的评论下方。
7. 确认当前阶段 UI 中无法对回复继续回复，且后端契约也会拒绝这类请求。
8. 以被回复的用户身份打开 `/notifications`，确认出现新的社区回复通知。
9. 以游客身份打开 `/jobs` 和 `/resources`。
10. 以游客身份打开一个已发布 PDF 资料 `/resources/:id`，确认无需登录即可预览。
11. 以游客身份打开一个已发布 PPTX 资料 `/resources/:id`，确认无需登录即可看到转换后的 PDF 预览。
12. 以游客身份打开一个已发布 ZIP 资料 `/resources/:id`，确认可以内联加载目录树。
13. 以游客身份打开一个已发布 DOCX 资料 `/resources/:id`，确认无需登录即可看到转换后的 PDF 预览。
14. 以游客身份确认下载操作仍然会被登录拦截。
15. 使用普通用户 `13800000001` 登录，并从 `/resources/upload` 上传一个资料。
16. 打开 `/profile/resources`，确认新文件显示为 `PENDING`。
17. 使用管理员 `13800000000` 登录并打开 `/admin/resources`。
18. 驳回一个待审核资料，并填写清晰的审核备注。
19. 重新切回资料所有者，打开 `/profile/resources`，点击“编辑并重新提交”，修改元数据后在不替换文件的情况下提交。
20. 使用 PDF 替换文件再走一遍重提流程，并确认记录重新回到 `PENDING`。
21. 使用 PPTX 或 DOCX 替换文件再走一遍重提流程，并确认新的预览打开的是重新生成的 PDF，而不是旧缓存。
22. 以资料所有者身份，在 `/profile/resources` 或 `/resources/:id` 中预览一个尚未公开但仍可见的 PDF、PPTX 或 DOCX。
23. 以管理员身份确认 `/admin/resources` 中的可见 PDF / PPTX / DOCX 行显示预览入口，而 ZIP 行显示目录预览入口。
24. 发布该待审核资料，并确认它出现在公开 `/resources` 列表中。
25. 以普通用户身份收藏并下载一个已发布资料。
26. 打开 `/profile/favorites`，在 `POST`、`JOB`、`RESOURCE` 之间切换。
27. 使用首页搜索框或 `/search` 搜索 `resume`。
28. 切换 `ALL / POST / JOB / RESOURCE` 以及 `RELEVANCE / LATEST`。
29. 刷新 `/search`，确认搜索状态保留在 URL 中。
30. 以游客身份打开 `/discover`，确认页面能加载公开排行看板。
31. 在趋势页切换 `ALL / POST / JOB / RESOURCE` 和 `WEEK / ALL`。
32. 刷新 `/discover?tab=JOB&period=ALL`，确认状态保留在 URL 中。
33. 返回 `/`，确认首页趋势预览会展示条目，或在无条目时展示友好的空态。
34. 使用 `13800000001` 登录，打开 `/assessment`，回答所有问题并提交一份结果。
35. 确认结果页展示推荐方向，并附带跳转到 `/timeline` 和 `/schools/compare` 的链接。
36. 打开 `/timeline`，确认默认定位到推荐方向并渲染里程碑卡片。
37. 在 `/timeline` 中切换 `CAREER`、`EXAM`、`ABROAD`，确认里程碑列表会重新加载。
38. 以游客或已登录用户身份打开 `/schools/compare`，选择 `2-4` 所学校，并确认对比表格和图表区域正常渲染。
39. 以游客身份打开 `/analytics`，确认公开总览、趋势卡片和决策方向占比正常渲染。
40. 使用 `13800000001` 登录后打开 `/analytics`，确认会显示个人快照 / 历史，或显示测评引导 CTA。
41. 返回 `/`，确认 `assessment` 和 `analytics` 两个首页入口都可用。
42. 使用管理员 `13800000000` 登录并打开 `/admin/dashboard`，确认页面展示只读总览。
43. 分别从首页管理员入口和主导航管理员入口进入，确认最终都落在 `/admin/dashboard`。
44. 从 `/admin/dashboard` 点击交接链接，确认能够进入现有管理工作台。
45. 使用 `13800000001` 登录，打开 `/profile/resumes`，上传至少一份 `PDF` 或 `DOCX` 简历，并确认列表中同时显示预览和下载入口。
46. 从 `/profile/resumes` 使用预览，确认简历可内联打开；然后打开 `/jobs/1`，确认外部来源链接仍在，并使用这份可预览简历提交一次站内申请。
47. 打开 `/profile/applications`，确认新记录展示岗位标题、公司、城市、状态、提交时间、简历快照标题，以及预览和下载入口。
48. 在 `/profile/applications` 中使用预览，确认快照可内联打开；然后删除 `/profile/resumes` 中的原始实时简历，再返回 `/profile/applications`，确认记录仍正常显示。
49. 以管理员 `13800000000` 登录并打开 `/admin/applications`，确认同一条记录展示申请人信息、简历快照文件名，以及预览和下载简历入口。
50. 在 `/admin/applications` 中使用预览或下载简历，确认即使实时简历被删除后，快照依旧可用。
51. 以申请人身份返回 `/jobs/1`，确认页面仍显示已申请状态。
52. 以管理员 `13800000000` 登录并打开 `/admin/users`，确认列表显示总数、活跃、封禁、已认证等计数。
53. 在 `/admin/users` 中封禁普通用户 `13800000001`，确认该行状态变为 `BANNED`。
54. 尝试以 `13800000001` 再次登录或打开 `/profile`，确认应用会明确阻止被封禁账号。
55. 在 `/admin/users` 中恢复 `13800000001`，并确认该用户可以重新登录。

## 定向社区热榜验证

### 后端

```bash
cd backend
mvn -q -Dtest=CommunityControllerTests test
```

### 前端

```bash
cd frontend
npx vitest run src/views/CommunityListView.spec.js
```

## 定向经验贴验证

### 后端

```bash
cd backend
mvn -q "-Dtest=CommunityControllerTests,DiscoverControllerTests,HomeControllerTests" test
```

### 前端

```bash
cd frontend
npx vitest run src/views/CommunityCreateView.spec.js src/views/CommunityDetailView.spec.js src/views/CommunityListView.spec.js src/views/HomeView.spec.js src/views/DiscoverView.spec.js
```

## 定向社区分层回复验证

### 后端

```bash
cd backend
mvn -q "-Dtest=CommunityControllerTests,NotificationControllerTests,HomeControllerTests" test
```

### 前端

```bash
cd frontend
npx vitest run src/views/CommunityDetailView.spec.js src/views/NotificationCenterView.spec.js src/views/HomeView.spec.js
```

## 定向管理员用户状态验证

### 后端

```bash
cd backend
mvn -q "-Dtest=AdminUserControllerTests,AuthControllerTests,UserControllerTests,HomeControllerTests,HomeServiceTests" test
```

### 前端

```bash
cd frontend
npx vitest run src/views/admin/AdminUsersView.spec.js src/components/NavBar.spec.js
```

## 定向管理员总览验证

### 后端

```bash
cd backend
mvn -q "-Dtest=AdminDashboardControllerTests,HomeServiceTests,HomeControllerTests" test
```

### 前端

```bash
cd frontend
npx vitest run src/views/admin/AdminDashboardView.spec.js src/views/HomeView.spec.js src/components/NavBar.spec.js
```

## 定向管理员岗位管理验证

### 后端

```bash
cd backend
mvn -q "-Dtest=AdminJobControllerTests,AdminJobImportControllerTests,AdminJobSyncControllerTests,JobImportCsvParserTests,JobBatchImportServiceTests,ThirdPartyJobSyncServiceTests,JobControllerTests" test
```

### 前端

```bash
cd frontend
npx vitest run src/views/admin/AdminJobManageView.spec.js src/components/NavBar.spec.js
```

## 定向管理员认证审核验证

### 后端

```bash
cd backend
mvn -q -Dtest=AdminVerificationControllerTests test
```

### 前端

```bash
cd frontend
npx vitest run src/views/admin/AdminVerificationReviewView.spec.js
```

## 定向管理员社区治理验证

### 后端

```bash
cd backend
mvn -q -Dtest=AdminCommunityControllerTests test
```

### 前端

```bash
cd frontend
npx vitest run src/views/admin/AdminCommunityManageView.spec.js
```

## 定向基础用户流程验证

### 后端

```bash
cd backend
mvn -q "-Dtest=AuthControllerTests,HomeControllerTests,UserControllerTests,VerificationControllerTests,NotificationControllerTests" test
```

### 前端

```bash
cd frontend
npx vitest run src/App.spec.js src/views/LoginView.spec.js src/views/HomeView.spec.js src/views/ProfileView.spec.js src/views/NotificationCenterView.spec.js
```

## 定向资源生命周期验证

### 后端

```bash
cd backend
mvn -q "-Dtest=ResourceControllerTests,AdminResourceControllerTests" test
```

### 前端

```bash
cd frontend
npx vitest run src/views/ResourcesListView.spec.js src/views/ResourceUploadView.spec.js src/views/ResourceEditView.spec.js src/views/ResourceDetailView.spec.js src/views/ProfileResourcesView.spec.js src/views/admin/AdminResourceManageView.spec.js
```

## 定向岗位浏览验证

### 后端

```bash
cd backend
mvn -q -Dtest=JobControllerTests test
```

### 前端

```bash
cd frontend
npx vitest run src/views/JobsListView.spec.js src/views/JobDetailView.spec.js src/views/ProfileFavoritesView.spec.js
```

## 定向社区流验证

### 后端

```bash
cd backend
mvn -q -Dtest=CommunityControllerTests test
```

### 前端

```bash
cd frontend
npx vitest run src/views/CommunityListView.spec.js src/views/CommunityCreateView.spec.js src/views/CommunityDetailView.spec.js src/views/ProfilePostsView.spec.js src/views/ProfileFavoritesView.spec.js
```

## 定向个人帖子与收藏验证

### 后端

```bash
cd backend
mvn -q "-Dtest=UserControllerTests,CommunityControllerTests" test
```

### 前端

```bash
cd frontend
npx vitest run src/views/ProfilePostsView.spec.js src/views/ProfileFavoritesView.spec.js src/views/ProfileView.spec.js
```

## 定向资源预览验证

### 后端

```bash
cd backend
mvn -q "-Dtest=ResourceControllerTests,AdminResourceControllerTests,ResourcePreviewServiceTests,ApachePoiPptxPreviewGeneratorTests,SofficeDocxPreviewGeneratorTests" test
```

### 前端

```bash
cd frontend
npx vitest run src/components/ResourceZipPreviewPanel.spec.js src/views/ResourceDetailView.spec.js src/views/ProfileResourcesView.spec.js src/views/admin/AdminResourceManageView.spec.js
```

## 定向统一搜索验证

### 后端

```bash
cd backend
mvn -q -Dtest=SearchControllerTests,DiscoverControllerTests,HomeControllerTests,HomeServiceTests test
```

### 前端

```bash
cd frontend
npx vitest run src/views/SearchView.spec.js src/views/DiscoverView.spec.js src/views/HomeView.spec.js src/components/NavBar.spec.js
```

## 定向决策支持验证

### 后端

```bash
cd backend
mvn -q "-Dtest=DecisionAssessmentServiceTests,DecisionAssessmentControllerTests,DecisionTimelineServiceTests,DecisionTimelineControllerTests,DecisionSchoolServiceTests,DecisionSchoolControllerTests,HomeServiceTests,HomeControllerTests" test
```

### 前端

```bash
cd frontend
npx vitest run src/views/AssessmentView.spec.js src/views/TimelineView.spec.js src/views/SchoolCompareView.spec.js src/views/HomeView.spec.js
```

## 定向决策分析验证

### 后端

```bash
cd backend
mvn -q "-Dtest=AnalyticsServiceTests,AnalyticsControllerTests,HomeServiceTests,HomeControllerTests" test
```

### 前端

```bash
cd frontend
npm run test -- src/views/AnalyticsView.spec.js src/views/HomeView.spec.js
```

## 定向岗位申请与简历流程验证

### 后端

```bash
cd backend
mvn -q "-Dtest=ResumeControllerTests,JobApplicationControllerTests,AdminJobApplicationControllerTests,JobControllerTests" test
```

### 前端

```bash
cd frontend
npx vitest run src/views/ProfileResumesView.spec.js src/views/ProfileApplicationsView.spec.js src/views/JobDetailView.spec.js src/views/admin/AdminApplicationsView.spec.js src/views/ProfileView.spec.js src/components/NavBar.spec.js
```
