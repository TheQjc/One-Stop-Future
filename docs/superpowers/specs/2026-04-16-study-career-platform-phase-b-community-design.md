# 校园一站式信息平台 Phase B 社区模块设计说明

> **Validation note:** This design was implemented and validated on 2026-04-16. Execution record: `docs/superpowers/plans/2026-04-16-study-career-platform-phase-b-community-implementation.md`. Documented verification covered `CampusApplicationTests`, `CommunityControllerTests`, `UserControllerTests`, `AdminCommunityControllerTests`, `CommunityListView.spec.js`, `CommunityCreateView.spec.js`, `CommunityDetailView.spec.js`, `ProfilePostsView.spec.js`, `ProfileFavoritesView.spec.js`, `AdminCommunityManageView.spec.js`, full backend/frontend regression, and local smoke.

## 1. 文档目标

本文档用于确认 Phase A 基础能力完成后的下一阶段子项目范围，即 `社区模块首期`。  
目标不是一次性做完正式需求中的全部 P1 社区能力，而是在现有 Spring Boot + Vue 基础上，优先交付一个可浏览、可发布、可互动、可治理的最小闭环，为后续统一搜索、岗位聚合、资料库和收藏复用提供稳定底座。

## 2. 当前结论

### 2.1 子项目定位

Phase B 首个子项目选择 `社区模块`，原因如下：

1. 社区是后续统一搜索和首页内容聚合最自然的内容源。
2. 社区比岗位聚合、资料库和统一搜索更适合先做单域闭环。
3. 社区的点赞、评论、收藏和后台治理能力能够沉淀可复用的数据与权限模式。

### 2.2 首期交付原则

社区首期按以下原则收口：

- 发帖后直接发布，不走管理员前置审核。
- 游客可浏览列表与详情，登录后才能发帖、评论、点赞、收藏。
- 标签固定为 `就业 / 考研 / 留学 / 闲聊`，首期不做后台动态标签配置。
- 帖子仅支持 `标题 + 正文 + 固定标签` 的纯文本结构，不做图片和附件。
- 管理端先提供 `查看 / 下架 / 删除` 的最小治理能力，不做举报、关注、私信和复杂审核流。

## 3. 范围定义

### 3.1 本期范围

本期社区模块包含：

- 社区列表页
- 帖子详情页
- 发帖页
- 一级评论
- 点赞 / 取消点赞
- 收藏 / 取消收藏
- 我的发布
- 我的收藏
- 管理员社区治理页
- 首页 `community` 入口从占位变为真实可进入模块

### 3.2 明确不做

以下能力不属于本期范围：

- 图片上传、附件上传
- 二级评论 / 楼中楼
- 发帖审核流
- 社区举报
- 关注、粉丝、私信
- 热榜与复杂推荐
- 社区互动通知
- 统一搜索接入
- 和岗位、资料库的跨域联动

## 4. 角色与权限

| 角色 | 可浏览社区列表 / 详情 | 可发帖 | 可评论 | 可点赞 | 可收藏 | 可查看我的发布 / 收藏 | 可治理帖子 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 游客 | 是 | 否 | 否 | 否 | 否 | 否 | 否 |
| 普通用户 | 是 | 是 | 是 | 是 | 是 | 是 | 否 |
| 已认证用户 | 是 | 是 | 是 | 是 | 是 | 是 | 否 |
| 管理员 | 是 | 是 | 是 | 是 | 是 | 是 | 是 |

补充规则：

- 社区写操作必须依赖现有 JWT 登录态。
- 管理能力必须在后端单独做角色校验，不能只依赖前端菜单隐藏。
- 管理员在前台仍然视作普通登录用户，但额外拥有后台治理权限。

## 5. 信息架构

### 5.1 页面结构

社区首期页面结构如下：

- `/community`
  社区列表页，支持按固定标签筛选，默认按最新发布排序。
- `/community/:id`
  帖子详情页，展示正文、互动状态和评论列表。
- `/community/create`
  发帖页，登录用户可进入。
- `/profile/posts`
  我的发布页。
- `/profile/favorites`
  我的收藏页。
- `/admin/community`
  管理员社区治理页。

### 5.2 与现有 Phase A 的衔接方式

- 首页继续保留现有聚合结构，但 `community` 入口不再是未来占位，而是正式可用模块。
- 个人中心不新建第二套壳层，而是在现有 `/profile` 体系下延伸 `我的发布` 和 `我的收藏`。
- 管理后台继续沿用现有最小后台结构，在认证审核页之外增加社区治理页。

## 6. 内容模型设计

### 6.1 帖子对象

建议新增 `CommunityPost` 实体，至少包含：

- `id`
- `authorId`
- `tag`
- `title`
- `content`
- `status`
- `likeCount`
- `commentCount`
- `favoriteCount`
- `createdAt`
- `updatedAt`

标签固定为：

- `CAREER`
- `EXAM`
- `ABROAD`
- `CHAT`

帖子状态定义为：

- `PUBLISHED`
  前台列表和详情可见。
- `HIDDEN`
  管理员下架，前台不可见。
- `DELETED`
  管理员删除，作为软删除保留审计痕迹。

### 6.2 评论对象

建议新增 `CommunityComment` 实体，至少包含：

- `id`
- `postId`
- `authorId`
- `content`
- `status`
- `createdAt`
- `updatedAt`

评论状态定义为：

- `VISIBLE`
- `DELETED`

本期仅支持一级评论，不做父评论 ID 和楼中楼回复。

### 6.3 点赞对象

建议新增 `PostLike` 或等价实体，至少包含：

- `id`
- `postId`
- `userId`
- `createdAt`

同一用户对同一帖子只能存在一条点赞记录，应通过唯一约束保证幂等。

### 6.4 收藏对象

建议直接引入通用收藏模型 `UserFavorite`，至少包含：

- `id`
- `userId`
- `targetType`
- `targetId`
- `createdAt`

其中：

- `targetType` 本期先支持 `POST`
- 后续可扩展为 `JOB`、`RESOURCE`

这样可以让社区首期直接为岗位和资料库复用收藏表，避免后续迁移。

## 7. 状态流转

### 7.1 帖子流转

帖子状态流转为：

1. 登录用户发帖
2. 系统直接创建 `PUBLISHED` 帖子
3. 帖子立即进入社区列表与详情可见范围
4. 管理员可将帖子改为 `HIDDEN` 或 `DELETED`

补充规则：

- 普通用户本期不支持自行删帖
- 前台只读取 `PUBLISHED` 数据
- `HIDDEN` 和 `DELETED` 对普通用户统一视为不存在

### 7.2 评论流转

评论状态流转为：

1. 登录用户在帖子详情页发表评论
2. 系统直接创建 `VISIBLE` 评论
3. 用户本人或管理员可将其改为 `DELETED`

### 7.3 互动流转

- 点赞与取消点赞采用幂等切换
- 收藏与取消收藏采用幂等切换
- 点赞数、评论数、收藏数由帖子侧聚合字段承接，优先满足列表和详情读取效率

## 8. 接口边界

### 8.1 公共 / 用户侧接口

建议社区相关接口如下：

- `GET /api/community/posts`
  获取帖子列表，可按标签筛选。
- `GET /api/community/posts/{id}`
  获取帖子详情。
- `POST /api/community/posts`
  创建帖子。
- `GET /api/community/posts/mine`
  获取我的发布。
- `POST /api/community/posts/{id}/comments`
  创建评论。
- `POST /api/community/posts/{id}/like`
  点赞。
- `DELETE /api/community/posts/{id}/like`
  取消点赞。
- `POST /api/community/posts/{id}/favorite`
  收藏。
- `DELETE /api/community/posts/{id}/favorite`
  取消收藏。
- `GET /api/users/me/favorites?type=POST`
  获取我的帖子收藏。

### 8.2 管理侧接口

建议后台接口如下：

- `GET /api/admin/community/posts`
  获取治理列表。
- `POST /api/admin/community/posts/{id}/hide`
  下架帖子。
- `POST /api/admin/community/posts/{id}/delete`
  删除帖子。

后台列表至少展示：

- 帖子标题
- 标签
- 作者昵称
- 发布时间
- 状态
- 点赞 / 评论 / 收藏计数

## 9. 前端交互设计

### 9.1 社区列表页

列表页职责：

- 展示标签筛选条
- 展示帖子摘要卡片
- 提供发帖入口
- 游客点击发帖时跳转登录
- 展示作者昵称、发布时间、标签、计数摘要

本期不在列表页直接展开评论或执行复杂互动，互动统一收束到详情页。

### 9.2 帖子详情页

详情页承担社区互动主中心职责：

- 展示完整标题、标签、正文
- 展示当前用户点赞状态和收藏状态
- 展示评论列表
- 登录用户可提交评论
- 游客只能阅读，写操作引导登录

### 9.3 发帖页

发帖页仅保留三项输入：

- 标签
- 标题
- 正文

提交成功后直接跳回新发帖子的详情页。

### 9.4 个人中心延展

个人中心在现有结构下增加：

- `我的发布`
- `我的收藏`

而不是新增独立用户主页，以减少首期前端壳层改造成本。

### 9.5 管理员治理页

管理员治理页职责：

- 查看帖子治理列表
- 按状态和标签筛选
- 执行下架
- 执行删除

本期不做复杂操作日志面板和举报工作台。

## 10. 异常处理规则

至少覆盖以下规则：

- 游客访问社区写接口时返回 `401`
- 已登录普通用户访问后台治理接口时返回 `403`
- 普通用户访问 `HIDDEN` 或 `DELETED` 帖子详情时返回 `404`
- 标题、正文、评论内容必须做非空与长度校验
- 点赞和收藏重复操作不报业务错误，应按幂等处理
- 评论或帖子目标不存在时返回统一错误结构

社区首期沿用现有全局响应格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

## 11. 测试与验收

### 11.1 后端测试重点

- 列表筛选
- 帖子详情可见性
- 发帖创建
- 评论创建
- 点赞幂等
- 收藏查询
- 我的发布查询
- 管理员下架 / 删除
- 普通用户越权访问后台接口

### 11.2 前端测试重点

- 社区列表页渲染与标签切换
- 帖子详情页互动区域
- 发帖页表单校验
- 我的发布与我的收藏页面
- 管理员治理页
- 游客与登录用户的路由守卫差异

### 11.3 本机冒烟验收

至少验证以下链路：

1. 游客可浏览社区列表和详情
2. 登录用户可发帖
3. 登录用户可评论、点赞、收藏
4. 我的发布和我的收藏可看到对应内容
5. 管理员下架后，前台普通用户无法再访问该帖

## 12. 实施顺序建议

建议按以下顺序实施：

1. 先冻结社区数据模型与表结构
2. 再完成社区公共接口与互动接口
3. 再接管理员治理接口
4. 再实现前端社区列表、详情、发帖
5. 最后接入个人中心扩展、首页入口和完整联调

## 13. 待确认项

当前无新增待确认项。社区首期设计已确认完成，可以进入实施计划阶段。
