# 校园一站式信息平台 Phase G 资料资源生命周期补完设计说明
## 1. 文档目标

本文档用于确认在 Phase F 发现与热榜首期完成之后，下一阶段子项目 `资料资源生命周期补完首期` 的设计边界。
本期目标不是一次性做完正式需求中的全部资料能力，而是在现有 Spring Boot + Vue 基础上，补齐资料库首期闭环中最明显的两个断点：

- 驳回后只能停留在只读状态，无法在原记录上修正并重新提交
- 资料详情页只能下载，无法对 PDF 形成公开可预览的阅读入口

因此，本期要交付的是一个最小但完整的补完闭环：

- `REJECTED` 资料可在原记录上编辑并重提
- 编辑时可选替换文件，不强制重新上传
- 在线预览首期只支持 `PDF`
- 已发布 PDF 可公开预览
- 资源拥有者与管理员可预览自己有权限看到的未发布 PDF
- 前台、个人中心、管理台都能接入这一套统一资源状态与预览能力

## 2. 当前结论

### 2.1 子项目定位
Phase G 选择 `资料资源生命周期补完首期` 作为下一子项目，原因如下：
1. Phase D 资料库首期已经具备上传、审核、发布、下载、收藏和个人记录查看能力，但驳回后的修复链路仍然中断，用户需要重新创建新记录才能继续，体验和管理成本都偏高。
2. 资料资源相较社区帖子和岗位卡片更天然需要“打开看看”的体验，仅靠下载不利于公开内容的阅读转化，PDF 预览是最小且最合理的第一步。
3. 本期可以继续复用现有资源实体、文件存储抽象、审核状态机和详情页，不需要新建版本系统、对象存储或文档转换服务，适合在当前代码结构上做增量补完。

### 2.2 首期交付原则

资料资源生命周期补完首期按以下原则收口：

- 继续复用单条 `ResourceItem` 记录，不引入资源版本历史
- 驳回后的修复通过“编辑原记录并重提”完成，不要求用户新建第二条记录
- 重提时允许只改元数据，也允许替换文件
- `PUBLISHED` 和 `OFFLINE` 首期不开放编辑，避免把范围扩散到“发布后修改再审核”
- 在线预览首期只支持 `PDF`
- `DOCX / PPTX / ZIP` 继续保持下载打开，不做在线转换
- 预览能力直接复用现有文件存储流读取，不引入 PDF viewer 服务端渲染或 Office 转码
- 预览权限遵循“当前详情可见，则当前 PDF 可预览”的规则，不另建第二套授权体系

## 3. 范围定义

### 3.1 本期范围

本期资料资源生命周期补完模块包含：

- 资源详情接口扩展为“拥有者 / 管理员可读取未发布详情”
- 新增 `PUT /api/resources/{id}` 用于 `REJECTED` 资源编辑并重提
- 新增 `GET /api/resources/{id}/preview` 用于 PDF 在线预览
- 资源详情返回中补充编辑态与预览态字段
- 我的资源列表返回中补充预览入口判断字段
- 管理台资源审核列表返回中补充预览入口判断字段
- 新增 `/resources/:id/edit` 路由
- 资源上传页重构为“创建 / 编辑重提”共用表单
- 我的资源页对 `REJECTED` 资源提供 `Edit And Resubmit` 入口
- 资源详情页提供 `Preview PDF` 入口
- 管理台资源审核页提供 PDF 预览入口

### 3.2 明确不做

以下能力不属于本期范围：

- `DOCX / PPTX / ZIP` 在线预览
- Office 文档转 PDF
- 资源历史版本列表、版本回滚、差异对比
- `PUBLISHED` 资源编辑后重新审核
- `OFFLINE` 资源编辑后重新审核
- 资源预览统计、阅读时长、阅读进度、批注
- MinIO 正式接入
- 分片上传、断点续传、批量上传

## 4. 角色与权限

| 角色 | 可查看已发布资源详情 | 可查看自己未发布资源详情 | 可编辑重提 `REJECTED` | 可预览已发布 PDF | 可预览自己未发布 PDF | 可预览任意未发布 PDF |
| --- | --- | --- | --- | --- | --- | --- |
| 游客 | 是 | 否 | 否 | 是 | 否 | 否 |
| 普通用户 | 是 | 是 | 是，仅自己且仅 `REJECTED` | 是 | 是，仅自己 | 否 |
| 已认证用户 | 是 | 是 | 是，仅自己且仅 `REJECTED` | 是 | 是，仅自己 | 否 |
| 管理员 | 是 | 是 | 否，管理员不替用户重提 | 是 | 是 | 是 |

补充规则：

- 普通用户和已认证用户的“未发布可见”仅限于自己上传的资源
- 管理员延续现有审核权限，可读取任意状态资源详情并预览任意状态 PDF
- 资源编辑重提严格限定为资源拥有者本人，不给管理员开放代编辑入口
- 预览权限与详情可见性绑定，不能看详情就不能看预览

## 5. 信息架构

### 5.1 页面结构

本期页面结构如下：

- `/resources/:id`
  继续作为统一资源详情页，新增预览入口，并对拥有者 / 管理员放开未发布详情可见
- `/resources/:id/edit`
  新增编辑重提页，仅服务 `REJECTED` 资源
- `/resources/upload`
  继续保留新建上传入口，但内部复用与编辑页相同的表单组件 / 逻辑
- `/profile/resources`
  在 `REJECTED` 资源卡片上展示 `Edit And Resubmit` 与 `Preview PDF`
- `/admin/resources`
  在选中资源面板和列表动作中展示 `Preview PDF`

### 5.2 与现有页面的衔接方式

- 资源详情页继续是用户查看资源的统一入口，不新增独立“预览页路由”
- 在线预览首期直接打开后端 `inline` PDF 响应，由浏览器原生承接阅读
- 编辑重提页延续现有上传页布局语言，不再单独创造第三套资源表单界面
- 我的资源页仍以记录看板为主，但不再保持完全只读
- 管理台审核页继续聚焦审核动作，不增加复杂运营能力，只补一个低干扰预览入口

## 6. 数据模型与接口边界

### 6.1 资源详情返回补充字段

建议扩展 `ResourceDetailResponse`，在现有字段基础上新增：

- `rejectReason`
  供拥有者在编辑重提前读取上一轮审核意见
- `editableByMe`
  表示当前查看者是否可对该资源执行编辑重提
- `previewAvailable`
  表示当前查看者是否可对该资源执行 PDF 预览

其中：

- `editableByMe = true` 的条件为：
  - 当前用户已登录
  - 当前用户是资源拥有者
  - 当前资源状态为 `REJECTED`
- `previewAvailable = true` 的条件为：
  - 当前资源文件类型为 `PDF`
  - 当前查看者对该资源详情有可见权限

### 6.2 我的资源列表补充字段

建议扩展 `MyResourceListResponse.ResourceItem`，补充：

- `previewAvailable`
- `editable`

原因如下：

- 我的资源页需要直接决定是否展示 `Preview PDF`
- 我的资源页需要直接决定是否展示 `Edit And Resubmit`
- 让后端直接返回动作态，比前端根据状态和扩展名自行猜测更稳妥

### 6.3 管理台资源列表补充字段

建议扩展 `AdminResourceListResponse.ResourceItem`，补充：

- `previewAvailable`

原因如下：

- 管理台列表和选中面板都需要直接判断是否展示预览入口
- 管理端预览态由后端统一给出，更符合审核工作台的“操作面板”语义

### 6.4 编辑重提接口

建议新增：

- `PUT /api/resources/{id}`

请求方式继续使用 `multipart/form-data`，与现有新建上传保持一致，支持字段：

- `title`
- `category`
- `summary`
- `description`
- `file`
  可选；缺失表示仅修改元数据，不替换文件

该接口的职责定义为：

- 校验当前用户为资源拥有者
- 校验资源当前状态必须为 `REJECTED`
- 更新标题、分类、摘要、描述
- 若上传新文件，则替换原文件元数据与 `storageKey`
- 清空上一轮审核痕迹
- 将资源状态重置为 `PENDING`
- 返回更新后的 `ResourceDetailResponse`

### 6.5 PDF 预览接口

建议新增：

- `GET /api/resources/{id}/preview`

响应要求：

- 对 `PDF` 返回 `Content-Disposition: inline`
- `Content-Type` 返回 `application/pdf`
- 非 PDF 返回统一业务错误

权限要求：

- 游客可预览 `PUBLISHED` 的 PDF
- 登录用户可预览自己可见的 PDF
- 管理员可预览任意可见的 PDF

### 6.6 统一可见性规则

当前 `GET /api/resources/{id}` 建议从“只有管理员可看未发布详情”调整为：

- 游客 / 普通他人：只能看 `PUBLISHED`
- 资源拥有者：可看自己的 `PENDING / REJECTED / OFFLINE / PUBLISHED`
- 管理员：可看任意状态

这样可以保证：

- 编辑页加载原记录时不需要额外新建“我的资源详情”接口
- 预览接口和详情接口共享同一套可见性判断
- 资源状态与审核意见可以在统一详情视图中被拥有者读取

## 7. 状态机与数据流

### 7.1 编辑重提状态流转

本期新增流转仅包含：

- `REJECTED -> PENDING`

流转时的字段处理规则建议如下：

- `status = PENDING`
- `rejectReason = null`
- `reviewedAt = null`
- `reviewedBy = null`
- `publishedAt = null`
- `updatedAt = now`

以下字段保留原值，除非本次请求显式更新：

- `title`
- `category`
- `summary`
- `description`
- `fileName / fileExt / contentType / fileSize / storageKey`

以下统计字段本期不重置：

- `downloadCount`
- `favoriteCount`

说明：

- 理论上 `REJECTED` 资源通常未公开，这两个值一般接近 `0`
- 首期保留统计值比强制重置更安全，避免无依据地清洗历史数据

### 7.2 替换文件数据流

当用户在编辑重提时替换文件，建议采用以下顺序：

1. 校验资源状态与用户身份
2. 校验新文件类型和大小
3. 将新文件存入 `ResourceFileStorage`
4. 更新数据库中的文件元信息与 `storageKey`
5. 提交数据库事务
6. best-effort 删除旧文件

这样做的原因是：

- 避免先删旧文件、后存新文件失败，导致资源记录彻底失联
- 数据库只在新文件已经成功落盘后才切换指向
- 旧文件删除失败不会影响用户本次重提成功，只需要记录日志

### 7.3 预览数据流

PDF 预览的数据流建议如下：

1. 前端点击 `Preview PDF`
2. 浏览器直接打开 `/api/resources/{id}/preview`
3. 后端先做资源可见性判断
4. 后端再校验文件扩展名 / contentType 是否为 PDF
5. 调用现有 `ResourceFileStorage.open(storageKey)` 读取文件流
6. 以 `inline` 响应返回给浏览器

本期不引入：

- 临时签名 URL
- 独立预览 CDN
- 页面内嵌复杂 PDF 组件

## 8. 后端边界设计

### 8.1 控制器职责

`ResourceController` 继续作为资源前台统一入口，新增两个动作：

- `PUT /api/resources/{id}`
- `GET /api/resources/{id}/preview`

不新增第二个“资源编辑控制器”或“资源预览控制器”，原因如下：

- 当前资源模块边界仍然足够集中
- 编辑重提和预览都属于现有资源主链路自然延伸
- 保持接口发现成本低，符合当前代码结构

### 8.2 服务职责

`ResourceService` 建议新增以下能力：

- `updateRejectedResource(...)`
- `previewResource(...)`
- 更细粒度的“可见资源校验”与“可编辑资源校验”内部方法

内部建议拆出三个核心校验方法：

- `requireVisibleResourceForViewer(resourceId, viewer)`
- `requireEditableRejectedResource(resourceId, viewer)`
- `requirePreviewableResource(resourceId, viewer)`

这样可以把：

- 详情可见性
- 编辑重提可行性
- PDF 预览可行性

三类规则显式分开，避免把所有判断堆在一个 `requireVisibleResource` 里变得含混。

### 8.3 文件存储抽象复用

本期继续复用现有 `ResourceFileStorage`：

- `store(...)`
- `open(...)`
- `delete(...)`
- `exists(...)`

不新增第二套“预览文件存储”抽象。

原因如下：

- 预览和下载读取的是同一份源文件
- 当前抽象已经足够支持替换文件与预览流读取
- MinIO 后续接入时，这两个能力都可自然继承

### 8.4 安全规则建议

安全配置需要补充以下规则：

- `GET /api/resources/{id}/preview` 允许匿名访问
  - 具体能否成功由资源可见性判断决定
- `PUT /api/resources/{id}` 需要登录

这样与当前下载行为形成清晰区分：

- `download` 继续只对登录用户开放
- `preview` 首期允许公开 PDF 阅读

## 9. 前端交互设计

### 9.1 创建 / 编辑共用表单

前端建议把当前 `ResourceUploadView` 演进为“共用资源表单页面”，支持两种模式：

- `create`
- `edit-resubmit`

推荐做法：

- 页面内部按路由或传入资源数据判断模式
- 表单字段保持一致：
  - `title`
  - `category`
  - `summary`
  - `description`
  - `file`
- 在编辑模式下增加三个上下文块：
  - `Review Note`
  - `Current File`
  - `Replace File (Optional)`

### 9.2 编辑页行为

`/resources/:id/edit` 的行为规则建议如下：

- 进入页面先加载资源详情
- 若后端返回 `editableByMe = false`，则展示错误态并引导返回 `/profile/resources`
- 表单预填现有标题、分类、摘要、描述
- 当前文件信息只读展示
- 文件选择框标注为“可选替换”
- 主按钮文案为 `Save And Resubmit`

提交成功后：

- 跳回 `/profile/resources`
- 让用户直接看到记录已恢复为 `PENDING`

### 9.3 我的资源页行为

我的资源页中的记录卡片建议新增以下动作：

- 当 `editable = true` 时显示 `Edit And Resubmit`
- 当 `previewAvailable = true` 时显示 `Preview PDF`

对于 `REJECTED` 记录，卡片仍保留：

- 状态标签
- 驳回原因
- 创建时间 / 更新时间

这样可以把“看审核意见 -> 进入编辑 -> 重提 -> 回到记录看板”形成完整闭环。

### 9.4 资源详情页行为

资源详情页建议新增 `Preview PDF` 按钮，显示条件为：

- `detail.previewAvailable = true`

按钮位置建议与现有：

- `Save To Collection`
- `Download Resource`

并列放置，保持动作层级清晰。

行为上：

- 点击后通过 `window.open(...)` 或原生链接打开 `/api/resources/{id}/preview`
- 不在当前 SPA 页面内嵌复杂阅读器
- 非 PDF 资源不显示该按钮

### 9.5 管理台行为

管理台资源审核页建议补一个低干扰预览动作：

- 选中资源面板显示 `Preview PDF`
- 列表行级动作在 `previewAvailable = true` 时可显示 `Preview`

预览在管理台中的职责仅为：

- 让审核员快速确认 PDF 内容与标题摘要是否一致

本期不扩展为：

- 审核批注
- 多页批量对比
- 预览中直接发布 / 驳回

## 10. 异常处理与降级

至少覆盖以下规则：

- 非拥有者调用编辑重提接口时返回 `403`
- 资源状态不是 `REJECTED` 时调用编辑重提接口返回 `400`
- 资源不存在时返回 `404`
- 非 PDF 调用预览接口返回 `400`
- 文件底层缺失时预览返回统一错误结构
- 新文件存储失败时编辑重提返回 `500`
- 旧文件删除失败时：
  - 重提流程仍成功
  - 记录 warning 日志

前端对应策略：

- 编辑页加载失败时展示错误态，不留空白表单
- 预览不做前端复杂错误兜底，由后端返回明确错误
- 我的资源页和管理台对没有预览能力的记录不展示预览按钮，而不是点击后再弹错

## 11. 测试与验收

### 11.1 后端测试重点

- 资源拥有者可读取自己的 `REJECTED` 资源详情
- 非拥有者不可读取未发布资源详情
- 管理员可读取任意状态资源详情
- 资源拥有者可在不替换文件时编辑重提
- 资源拥有者可在替换文件时编辑重提
- `PENDING / PUBLISHED / OFFLINE` 不可编辑重提
- `PUT /api/resources/{id}` 重提后状态正确回到 `PENDING`
- PDF 资源可预览
- ZIP 资源不可预览
- 游客可预览 `PUBLISHED` PDF
- 游客不可预览未发布 PDF

### 11.2 前端测试重点

- 编辑页能正确预填现有资源信息
- 编辑页不选新文件也能成功提交
- 编辑页选新文件时会把替换文件带入请求
- 资源详情页在 `previewAvailable = true` 时显示预览按钮
- 资源详情页在非 PDF 时不显示预览按钮
- 我的资源页在 `REJECTED` 记录上显示编辑入口
- 我的资源页仅在 `previewAvailable = true` 时显示预览入口
- 管理台在可预览 PDF 上显示预览入口

### 11.3 本期验收标准

满足以下条件时，可判定 Phase G 进入可实施状态：

- 用户可在同一条 `REJECTED` 资源记录上完成修正并重提
- 重提时可只改元数据，也可替换文件
- 重提后资源状态回到 `PENDING`
- 已发布 PDF 可被游客和登录用户直接在线预览
- 资源拥有者可预览自己未发布的 PDF
- 管理员可在审核台预览 PDF
- `DOCX / PPTX / ZIP` 继续保持下载，不误显示预览入口
- 不引入新版本表或额外存储系统仍能完成完整闭环

## 12. 实施顺序建议

建议按以下顺序实施：

1. 先扩展后端资源可见性判断与详情返回字段
2. 再实现 `PUT /api/resources/{id}` 编辑重提接口
3. 再实现 `GET /api/resources/{id}/preview` PDF 预览接口
4. 再补充后端控制器 / 服务测试
5. 再改造前端资源上传页为创建 / 编辑共用表单
6. 再接入我的资源页、资源详情页、管理台预览入口
7. 最后更新 README、手测清单和回归测试

## 13. 待确认项

当前无新增待确认项。资料资源生命周期补完首期设计已确认完成，可进入实施计划阶段。
