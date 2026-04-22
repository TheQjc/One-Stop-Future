# Frontend Chinese Localization Design

## 1. Goal

This design defines a Chinese-first frontend presentation refresh for the student growth platform so the public-facing experience feels natural for Chinese users without changing the existing route model, backend contracts, permission rules, or core product workflows.

The immediate goal is to replace the current mixed English and Chinese presentation with a consistent "campus growth service" voice, reorganize homepage reading order around user state and common tasks, and establish a naming system that can extend from the navigation and homepage to the main frontend surfaces.

## 2. User-Validated Scope

The following decisions were explicitly validated during brainstorming:

- the primary problem to solve first is Chinese localization and wording quality
- the preferred tone is `campus growth service`: warm, supportive, and practical
- the rollout should proceed in order:
  - navigation and homepage hero first
  - the full homepage second
  - the broader main frontend wording system third
- the frontend should become Chinese-first
- the English brand `One-Stop Future` should remain only in secondary positions
- the homepage should combine:
  - top-level personal status and common actions
  - lower-level career / exam / abroad growth paths
- the homepage should answer three user questions quickly:
  - what state am I in now
  - where should I go first
  - which path should I continue with next

## 3. Non-Goals

This design does not include:

- backend API changes
- route-path changes
- permission or role-policy changes
- business-flow redesign for profile, verification, notifications, jobs, resources, or community
- a full visual redesign of spacing, color system, or component architecture
- a new official Chinese legal brand name
- admin workflow redesign beyond terminology cleanup

## 4. Current Problems

The current frontend has three issues that make the product feel less natural for Chinese users:

- top-level English labeling remains prominent in the navigation, homepage sections, buttons, and card metadata
- some wording follows internal or editorial vocabulary such as `Desk`, `Board`, `Entry`, and `Discover`, which does not match common Chinese product language
- the homepage describes the platform concept before clearly telling users their current state and next action

Observed examples in the current frontend include:

- navigation labels such as `Home`, `Community`, `Discover`, `Search`, `Profile`, and `Notifications`
- brand messaging such as `Editorial Student Decision Desk`
- homepage sections such as `Today's Snapshot`, `Three Tracks`, `Quick Entry`, and `Discover Preview`
- card metadata such as `Desk 00`

## 5. Chosen Approach

### 5.1 Recommendation

Adopt a Chinese-first content-system refresh rather than a literal translation pass.

The homepage should behave like a student growth workbench:

- the top of the page should focus on current state, pending tasks, and common actions
- the middle of the page should expose common entry points
- the lower section should keep the three growth paths for career, exam, and abroad
- trend and notification modules should remain visible, but after the user has already oriented themselves

### 5.2 Why This Approach

This approach best fits the approved direction because it:

- matches Chinese users' expectation that the homepage should first explain "what do I do now"
- preserves the existing platform concept of career / exam / abroad without making it the only thing shown first
- avoids a shallow word-for-word rewrite that would still feel foreign
- keeps implementation bounded to presentation and wording layers

### 5.3 Rejected Alternatives

#### Translation-Only Refresh

Rejected because it would keep the current English editorial framing and only swap labels, which would still feel unnatural for the target users.

#### Efficiency-Only Task Dashboard

Rejected because it would over-emphasize utilities such as verification and notifications, weakening the approved growth-service tone and the career / exam / abroad direction model.

#### Preserve Existing Brand-Led Homepage Shape

Rejected because it would under-deliver on the user's request to optimize the frontend for Chinese users and would likely stop at cosmetic localization.

## 6. Content Architecture

### 6.1 Frontend Voice

The frontend voice should follow these rules:

- Chinese is the default language of the interface
- English brand text may remain, but only in supporting positions
- wording should feel like a campus growth assistant rather than an editorial desk
- copy should prefer direct user guidance over platform self-description
- copy should often use the "first..., then..." structure to create a supportive flow

Examples of preferred phrasing patterns:

- `先看清状态，再安排下一步`
- `先处理待办，再继续规划`
- `从这里进入你现在最可能会用到的功能`

### 6.2 Naming Principles

All frontend naming should follow these rules:

- module titles should usually stay within 4 to 6 Chinese characters
- titles should describe user value, not internal product concepts
- the same concept should use one stable Chinese label across the frontend
- numbered pseudo-editorial labels such as `Desk 00` should be removed from the public-facing UI

## 7. Navigation And Brand Design

### 7.1 Public Navigation Labels

The main public navigation should use:

- `/` -> `首页`
- `/community` -> `社区`
- `/discover` -> `趋势`
- `/search` -> `搜索`

For authenticated users, add:

- `/profile` -> `我的`
- `/notifications` -> `通知`

### 7.2 Admin Navigation Labels

Admin navigation should remain operational and concise:

- `/admin/dashboard` -> `运营总览`
- `/admin/users` -> `用户管理`
- `/admin/applications` -> `申请管理`
- `/admin/verifications` -> `认证审核`
- `/admin/community` -> `社区管理`

### 7.3 Auth Action Labels

Authentication actions should use:

- `Log In` -> `登录`
- `Register` -> `注册`
- `Log Out` -> `退出登录`

### 7.4 Brand Placement

The top brand block should become Chinese-first:

- overline: `学生成长服务平台`
- primary title: `一站式成长平台`
- secondary brand text: `One-Stop Future`

This keeps the English brand available without making it the first reading target.

## 8. Homepage Information Hierarchy

### 8.1 Module Order

The homepage should follow this order:

1. welcome hero
2. `今日概览`
3. `常用入口`
4. `成长方向`
5. `本周趋势`
6. `最新通知`

### 8.2 Reasoning

This order is chosen so that users:

- first understand their current status
- then enter the most likely next function
- then continue into longer-term growth directions
- then browse broader public signals and recent updates

## 9. Homepage Copy Design

### 9.1 Hero Copy

Guest-state hero:

- title: `把就业、考研、留学放到一个首页里，先看方向，再做决定`
- description: `公开内容、常用入口和成长方向会集中展示，先帮你看清选择，再进入具体模块。`
- primary CTA: `登录查看个人待办`
- secondary CTA: `立即注册`

Authenticated-state hero:

- title: `你好，{昵称}，今天先从这几件事开始`
- description: `认证进度、未读通知和常用入口都会集中在这里，帮你先处理当下，再继续规划下一步。`
- primary CTA: `进入个人中心`
- secondary CTA: `查看通知`

### 9.2 Search Area

The hero search area should use:

- label: `站内搜索`
- placeholder: `搜索经验帖、岗位、院校、资料`
- submit button: `搜索`

### 9.3 Status Chips

Examples of top status wording:

- guest state chip: `首页服务已开启`
- logged-in identity chip: `{角色} / {昵称}`
- unread chip: `{数量} 条未读通知`

## 10. Homepage Module Terminology

### 10.1 Homepage Section Labels

Replace current section naming with:

- `Today's Snapshot` -> `今日概览`
- `Three Tracks` -> `成长方向`
- `Quick Entry` -> `常用入口`
- `Discover Preview` -> `本周趋势`
- `Latest Updates` -> `最新通知`

### 10.2 Section Supporting Copy

Recommended supporting copy:

- `今日概览`: `把身份状态、认证进度、未读通知和今日待办集中看清。`
- `常用入口`: `从首页直接进入你现在最可能会用到的功能。`
- `成长方向`: `先看清你更关注哪条路径，再进入对应模块继续推进。`
- `本周趋势`: `看看这一周大家都在关注什么，再决定要不要深入查看。`
- `最新通知`: `及时查看认证进度、系统提醒和与你相关的更新。`

## 11. Card-Level Wording

### 11.1 Common Entry Cards

Homepage entry cards should prioritize these user-facing labels:

- `个人中心`
- `通知中心`
- `社区交流`
- admin-only: `认证审核`

Card footer / meta wording should use:

- `立即进入`
- `登录后可用`
- `即将开放`
- `查看当前阶段`

Public-facing numbered tags such as `Desk 00` and `Desk 01` should be removed or replaced with neutral category cues.

### 11.2 Trend Cards

Trend card terminology should use:

- `Community` -> `社区`
- `Job` -> `岗位`
- `Resource` -> `资料`
- `Discover Pick` -> `本周推荐`
- `Publishing window pending` -> `待发布`
- `No summary has been attached to this board item yet.` -> `暂未提供内容摘要`
- `Heat {score}` -> `热度 {score}`

The main CTA from homepage trend preview should use:

- `Enter Discover` -> `查看全部趋势`

## 12. State Messaging

### 12.1 Loading States

Loading text should be natural and action-aware. Examples:

- `正在整理首页信息...`
- `正在加载本周趋势...`
- `正在同步最新通知...`

### 12.2 Empty States

Empty states should explain what the user can do next. Examples:

- notifications for guests: `登录后可查看与你相关的通知和处理结果。`
- notifications for authenticated users: `当前还没有新的通知。`
- trend preview empty state: `本周趋势还在更新中，可以稍后再来看。`

### 12.3 Error States

Error states should avoid technical language when possible. Examples:

- `加载失败，请稍后重试`
- retry action: `重新加载`

## 13. Implementation Boundary

### 13.1 Phase 1

Implement first:

- navigation wording
- brand block wording
- homepage hero title, description, search, status chips, and CTA labels

### 13.2 Phase 2

Implement second:

- all remaining homepage section titles
- homepage supporting copy
- homepage card labels
- homepage empty / loading / error states

### 13.3 Phase 3

Implement third:

- main frontend page wording system for:
  - search
  - trends
  - community
  - jobs
  - resources
  - profile
  - notifications
  - login
  - register
- secondary pass for:
  - assessment
  - timeline
  - analytics
  - school comparison

## 14. Affected Frontend Surface

This design is expected to drive wording and presentation changes across at least these current files and routes:

- `frontend/src/components/NavBar.vue`
- `frontend/src/views/HomeView.vue`
- `frontend/src/components/HomeEntryCard.vue`
- `frontend/src/components/DiscoverItemCard.vue`
- `frontend/src/views/DiscoverView.vue`
- `frontend/src/views/SearchView.vue`
- `frontend/src/views/CommunityListView.vue`
- `frontend/src/views/JobsListView.vue`
- `frontend/src/views/ResourcesListView.vue`
- `frontend/src/views/ProfileView.vue`
- `frontend/src/views/NotificationCenterView.vue`
- `frontend/src/views/LoginView.vue`
- `frontend/src/views/RegisterView.vue`

The first implementation slice only needs to change the navigation and homepage surfaces. The later wording-system rollout can extend to the remaining frontend pages in order.

## 15. Architecture And Data-Flow Constraints

This presentation refresh must preserve the current runtime structure:

- no API contract changes
- no route-path changes
- no authentication or authorization behavior changes
- no changes to summary payload meaning
- no business-rule changes for verification, notifications, community, jobs, resources, assessment, or analytics

Copy and presentation logic may move toward centralized label maps or helper structures during implementation, but the user-visible result should remain within the approved wording system above.

## 16. Testing Strategy

Implementation planning should cover both rendering and wording validation.

At minimum, the plan should verify:

- public navigation renders the approved Chinese labels
- authenticated navigation renders `我的` and `通知`
- homepage hero shows the approved guest and authenticated Chinese text variants
- homepage section titles use the approved Chinese naming
- homepage card meta states use the approved Chinese wording
- empty, loading, and retry wording remain consistent after the refresh
- existing user-state logic still controls which actions and chips appear

Where tests already cover homepage and card rendering, prefer updating and extending those tests instead of introducing duplicate suites.

## 17. Acceptance Criteria

This work is complete when:

- the public-facing homepage no longer presents prominent top-level English labels except the secondary brand `One-Stop Future`
- the homepage feels Chinese-first and naturally readable to Chinese users
- the homepage clearly communicates current status, likely next action, and growth direction within a few seconds
- public-facing module titles follow the approved naming system
- homepage wording uses one consistent supportive tone rather than mixed editorial and literal-translation styles
- public-facing numbered desk labels are removed
- loading, empty, and error states on the homepage read as natural Chinese product copy
- the implementation remains presentation-only and does not alter backend or route behavior

## 18. Open Implementation Notes

Implementation should pay attention to two practical issues discovered during exploration:

- some homepage strings have already been partially localized while others remain English, so duplicate wording sources may need consolidation
- earlier terminal output suggested possible encoding-display confusion in some files, so implementation should verify source-file encoding carefully before editing Chinese text
