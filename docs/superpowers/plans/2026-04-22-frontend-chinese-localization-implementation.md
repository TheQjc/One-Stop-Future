# Frontend Chinese Localization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the frontend's mixed English/Chinese presentation with the approved Chinese-first navigation, homepage, and main-page wording system while preserving existing routes, API contracts, and user-state behavior.

**Architecture:** Keep the current Vue SPA structure and localize one surface group at a time by extending the matching Vitest coverage before each copy change. Reuse existing component boundaries, avoid a global i18n refactor, and only add small local label maps where a shared card or filter already needs stable Chinese wording.

**Tech Stack:** Vue 3, Vue Router 4, Pinia, Vitest, Vue Test Utils, Vite

---

## File Map

### Shared Navigation And Homepage

- Modify: `frontend/src/components/NavBar.vue`
  - Own the top navigation labels, brand block, and auth action copy.
- Modify: `frontend/src/components/NavBar.spec.js`
  - Pin the approved Chinese navigation and brand text for guest, authenticated, and admin users.
- Modify: `frontend/src/views/HomeView.vue`
  - Own the homepage hero, module order, module titles, CTA text, and homepage state/empty/error wording.
- Modify: `frontend/src/views/HomeView.spec.js`
  - Protect homepage copy, ordering, and route-driven behavior.
- Modify: `frontend/src/components/HomeEntryCard.vue`
  - Remove public desk numbering and keep homepage entry-card meta text Chinese-first.
- Modify: `frontend/src/components/DiscoverItemCard.vue`
  - Provide Chinese defaults for trend card type labels, badges, score copy, and fallback text.
- Create: `frontend/src/components/DiscoverItemCard.spec.js`
  - Pin shared trend-card default Chinese wording.

### Search And Trend Surfaces

- Modify: `frontend/src/views/DiscoverView.vue`
  - Localize the trend page hero, controls, metrics, empty states, and retry copy.
- Modify: `frontend/src/views/DiscoverView.spec.js`
  - Verify Chinese trend labels while preserving URL-backed tab/period behavior.
- Modify: `frontend/src/views/SearchView.vue`
  - Localize search hero, controls, totals, summaries, empty states, and retry copy.
- Modify: `frontend/src/views/SearchView.spec.js`
  - Verify Chinese search wording while preserving URL-backed query/type/sort behavior.
- Modify: `frontend/src/components/SearchResultCard.vue`
  - Provide Chinese labels for search-result type, date fallback, summary fallback, and meta defaults.
- Create: `frontend/src/components/SearchResultCard.spec.js`
  - Pin shared search-result fallback wording.

### Community, Profile, And Notification Surfaces

- Modify: `frontend/src/views/CommunityListView.vue`
  - Localize hero, hot-board wording, tag labels, list empty states, and CTA text.
- Modify: `frontend/src/views/CommunityListView.spec.js`
  - Verify community list/hot-board wording and refetch behavior.
- Modify: `frontend/src/components/CommunityPostCard.vue`
  - Replace leftover English badges such as `Experience Post` with stable Chinese copy.
- Create: `frontend/src/components/CommunityPostCard.spec.js`
  - Pin shared community card badge and fallback wording.
- Modify: `frontend/src/views/ProfileView.vue`
  - Localize personal center hero, quick links, form labels, hints, and verification copy.
- Modify: `frontend/src/views/ProfileView.spec.js`
  - Verify Chinese personal-center wording while preserving profile and verification behavior.
- Modify: `frontend/src/views/NotificationCenterView.vue`
  - Finish the notification center's remaining mixed-language title and status wording.
- Modify: `frontend/src/views/NotificationCenterView.spec.js`
  - Pin notification-center labels and unread/read state copy.

### Jobs, Resources, And Auth Surfaces

- Modify: `frontend/src/views/JobsListView.vue`
  - Localize the jobs page hero, filter summary, counts, empty states, and CTA text.
- Modify: `frontend/src/views/JobsListView.spec.js`
  - Verify jobs page Chinese wording while preserving filter-driven fetch behavior.
- Modify: `frontend/src/components/JobFilterBar.vue`
  - Replace filter labels and submit/reset actions with Chinese wording.
- Modify: `frontend/src/components/JobPostingCard.vue`
  - Replace saved/status/fallback copy with Chinese wording.
- Modify: `frontend/src/views/ResourcesListView.vue`
  - Localize the resources page hero, filter summary, counts, empty states, and CTA text.
- Modify: `frontend/src/views/ResourcesListView.spec.js`
  - Verify resources page Chinese wording while preserving filter-driven fetch behavior.
- Modify: `frontend/src/components/ResourceFilterBar.vue`
  - Replace filter labels and submit/reset actions with Chinese wording.
- Modify: `frontend/src/components/ResourceCard.vue`
  - Replace category fallback, saved state, summary fallback, and meta defaults with Chinese wording.
- Modify: `frontend/src/views/LoginView.vue`
  - Keep the phone-code flow intact while making the surrounding presentation fully Chinese-first.
- Modify: `frontend/src/views/LoginView.spec.js`
  - Pin login copy and validation wording.
- Modify: `frontend/src/views/RegisterView.vue`
  - Localize the register flow copy to match the same tone and naming system.
- Create: `frontend/src/views/RegisterView.spec.js`
  - Add coverage for register copy and core validation states.

### Secondary Decision-Support Pass

- Modify: `frontend/src/views/AssessmentView.vue`
- Modify: `frontend/src/views/AssessmentView.spec.js`
- Modify: `frontend/src/views/TimelineView.vue`
- Modify: `frontend/src/views/TimelineView.spec.js`
- Modify: `frontend/src/views/AnalyticsView.vue`
- Modify: `frontend/src/views/AnalyticsView.spec.js`
- Modify: `frontend/src/views/SchoolCompareView.vue`
- Modify: `frontend/src/views/SchoolCompareView.spec.js`
  - Localize the secondary decision-support pages without changing any submission, comparison, or analytics behavior.

### Verification And Audit

- Run against: `frontend/package.json`
  - Use existing `vitest run` and `vite build` scripts for regression verification.
- Audit with: `rg`
  - Find leftover top-level English product wording before closing the rollout.

## Task 1: Localize Navigation And Brand Copy

**Files:**
- Modify: `frontend/src/components/NavBar.spec.js`
- Modify: `frontend/src/components/NavBar.vue`
- Test: `frontend/src/components/NavBar.spec.js`

- [ ] **Step 1: Write the failing navigation-copy assertions**

```js
expect(wrapper.text()).toContain("学生成长服务平台");
expect(wrapper.text()).toContain("一站式成长平台");
expect(wrapper.text()).toContain("首页");
expect(wrapper.text()).toContain("趋势");
expect(wrapper.text()).toContain("登录");
expect(wrapper.text()).toContain("注册");
expect(wrapper.text()).toContain("我的");
expect(wrapper.text()).toContain("通知");
expect(wrapper.text()).toContain("运营总览");
expect(wrapper.text()).toContain("申请管理");
expect(wrapper.text()).toContain("退出登录");
expect(wrapper.text()).not.toContain("Home");
```

- [ ] **Step 2: Run the navbar spec to verify it fails on the old English copy**

Run: `npm --prefix frontend test -- src/components/NavBar.spec.js`
Expected: FAIL because the rendered text still includes labels such as `Home`, `Discover`, `Log In`, and the old brand line.

- [ ] **Step 3: Update `NavBar.vue` with the approved Chinese labels and Chinese-first brand block**

```js
const items = [
  { to: "/", label: "首页" },
  { to: "/community", label: "社区" },
  { to: "/discover", label: "趋势" },
  { to: "/search", label: "搜索" },
];
```

```vue
<span class="site-brand__mark">学生成长服务平台</span>
<strong>一站式成长平台</strong>
<small>One-Stop Future</small>
```

- [ ] **Step 4: Re-run the navbar spec**

Run: `npm --prefix frontend test -- src/components/NavBar.spec.js`
Expected: PASS with guest, authenticated, and admin navbar labels all rendered in Chinese.

- [ ] **Step 5: Commit the navigation slice**

```bash
git add frontend/src/components/NavBar.vue frontend/src/components/NavBar.spec.js
git commit -m "feat: localize navigation and brand copy"
```

## Task 2: Localize The Homepage Hero And Status-First Layout

**Files:**
- Modify: `frontend/src/views/HomeView.spec.js`
- Modify: `frontend/src/views/HomeView.vue`
- Test: `frontend/src/views/HomeView.spec.js`

- [ ] **Step 1: Add failing homepage assertions for hero copy, CTA text, search labels, and module order**

```js
expect(wrapper.text()).toContain("把就业、考研、留学放到一个首页里，先看方向，再做决定");
expect(wrapper.text()).toContain("站内搜索");
expect(wrapper.text()).toContain("登录查看个人待办");
expect(wrapper.text()).toContain("今日概览");
expect(wrapper.text()).toContain("常用入口");
expect(wrapper.text().indexOf("常用入口")).toBeLessThan(wrapper.text().indexOf("成长方向"));
```

- [ ] **Step 2: Run the homepage spec to verify it fails**

Run: `npm --prefix frontend test -- src/views/HomeView.spec.js`
Expected: FAIL because the hero still renders English labels such as `Unified search`, `Today's Snapshot`, or the old module order.

- [ ] **Step 3: Update `HomeView.vue` to use the approved guest/authenticated hero copy, search wording, CTA labels, and module order**

```js
const heroEyebrow = computed(() => (isGuest.value ? "学生成长服务平台" : "今日成长工作台"));
const heroTitle = computed(() => (
  isGuest.value
    ? "把就业、考研、留学放到一个首页里，先看方向，再做决定"
    : `你好，${viewerName.value}，今天先从这几件事开始`
));
```

```vue
<label class="hero-search__label" for="home-search">站内搜索</label>
<button type="submit" class="app-btn hero-search__submit">搜索</button>
```

- [ ] **Step 4: Re-run the homepage spec**

Run: `npm --prefix frontend test -- src/views/HomeView.spec.js`
Expected: PASS with both guest and authenticated homepage hero variants rendered in Chinese.

- [ ] **Step 5: Commit the hero/layout slice**

```bash
git add frontend/src/views/HomeView.vue frontend/src/views/HomeView.spec.js
git commit -m "feat: localize homepage hero and layout order"
```

## Task 3: Finish Homepage Module Labels And Shared Trend Card Copy

**Files:**
- Create: `frontend/src/components/DiscoverItemCard.spec.js`
- Modify: `frontend/src/components/DiscoverItemCard.vue`
- Modify: `frontend/src/components/HomeEntryCard.vue`
- Modify: `frontend/src/views/HomeView.vue`
- Modify: `frontend/src/views/HomeView.spec.js`
- Test: `frontend/src/components/DiscoverItemCard.spec.js`
- Test: `frontend/src/views/HomeView.spec.js`

- [ ] **Step 1: Add failing tests for Chinese homepage module labels, Chinese trend-card defaults, and removal of public desk numbering**

```js
expect(wrapper.text()).toContain("本周趋势");
expect(wrapper.text()).toContain("最新通知");
expect(wrapper.text()).not.toContain("Desk 00");
expect(card.text()).toContain("本周推荐");
expect(card.text()).toContain("热度 12");
expect(card.text()).toContain("暂未提供内容摘要");
```

- [ ] **Step 2: Run the homepage and trend-card specs to verify they fail**

Run: `npm --prefix frontend test -- src/views/HomeView.spec.js src/components/DiscoverItemCard.spec.js`
Expected: FAIL because the homepage still renders `Quick Entry` / `Discover Preview` and the shared card still renders `Discover Pick` / `Heat`.

- [ ] **Step 3: Update homepage module titles, entry-card presentation, and shared trend-card defaults**

```vue
<span class="section-eyebrow">常用入口</span>
<span class="section-eyebrow">成长方向</span>
<span class="section-eyebrow">本周趋势</span>
<span class="section-eyebrow">最新通知</span>
```

```vue
<p v-if="entry.code && !/^Desk\\s\\d+/i.test(entry.code)" class="home-entry-card__code">
  {{ entry.code }}
</p>
```

```js
const typeLabels = {
  POST: "社区",
  JOB: "岗位",
  RESOURCE: "资料",
};
```

- [ ] **Step 4: Re-run the homepage and trend-card specs**

Run: `npm --prefix frontend test -- src/views/HomeView.spec.js src/components/DiscoverItemCard.spec.js`
Expected: PASS with homepage module naming, card fallbacks, and trend score text all localized.

- [ ] **Step 5: Commit the homepage shared-copy slice**

```bash
git add frontend/src/views/HomeView.vue frontend/src/views/HomeView.spec.js frontend/src/components/HomeEntryCard.vue frontend/src/components/DiscoverItemCard.vue frontend/src/components/DiscoverItemCard.spec.js
git commit -m "feat: localize homepage modules and trend card copy"
```

## Task 4: Localize Search And Trend Pages

**Files:**
- Modify: `frontend/src/views/DiscoverView.vue`
- Modify: `frontend/src/views/DiscoverView.spec.js`
- Modify: `frontend/src/views/SearchView.vue`
- Modify: `frontend/src/views/SearchView.spec.js`
- Modify: `frontend/src/components/SearchResultCard.vue`
- Create: `frontend/src/components/SearchResultCard.spec.js`
- Test: `frontend/src/views/DiscoverView.spec.js`
- Test: `frontend/src/views/SearchView.spec.js`
- Test: `frontend/src/components/SearchResultCard.spec.js`

- [ ] **Step 1: Add failing assertions for Chinese search/trend heroes, controls, summaries, and shared card fallback copy**

```js
expect(discoverWrapper.text()).toContain("趋势");
expect(discoverWrapper.text()).toContain("时间范围");
expect(discoverWrapper.text()).toContain("查看全部趋势");
expect(searchWrapper.text()).toContain("站内搜索");
expect(searchWrapper.text()).toContain("搜索结果");
expect(searchWrapper.text()).toContain("先输入关键词");
expect(card.text()).toContain("社区");
expect(card.text()).toContain("资料");
expect(card.text()).toContain("暂未提供内容摘要");
```

- [ ] **Step 2: Run the search/trend specs to verify they fail**

Run: `npm --prefix frontend test -- src/views/DiscoverView.spec.js src/views/SearchView.spec.js src/components/SearchResultCard.spec.js`
Expected: FAIL because these surfaces still render English hero text, chip labels, summaries, and component fallbacks.

- [ ] **Step 3: Update the two views and the shared search-result card with Chinese-first wording**

```js
const TAB_OPTIONS = [
  { value: "ALL", label: "全部" },
  { value: "POST", label: "帖子" },
  { value: "JOB", label: "岗位" },
  { value: "RESOURCE", label: "资料" },
];
```

```js
const SORT_OPTIONS = [
  { value: "RELEVANCE", label: "相关度" },
  { value: "LATEST", label: "最新" },
];
```

```vue
<span class="section-eyebrow">趋势</span>
<span class="section-eyebrow">站内搜索</span>
```

- [ ] **Step 4: Re-run the search/trend specs**

Run: `npm --prefix frontend test -- src/views/DiscoverView.spec.js src/views/SearchView.spec.js src/components/SearchResultCard.spec.js`
Expected: PASS with URL-backed behavior unchanged and the page copy rendered in Chinese.

- [ ] **Step 5: Commit the search/trend slice**

```bash
git add frontend/src/views/DiscoverView.vue frontend/src/views/DiscoverView.spec.js frontend/src/views/SearchView.vue frontend/src/views/SearchView.spec.js frontend/src/components/SearchResultCard.vue frontend/src/components/SearchResultCard.spec.js
git commit -m "feat: localize search and trend pages"
```

## Task 5: Localize Community Surfaces And Shared Post Cards

**Files:**
- Modify: `frontend/src/views/CommunityListView.vue`
- Modify: `frontend/src/views/CommunityListView.spec.js`
- Modify: `frontend/src/components/CommunityPostCard.vue`
- Create: `frontend/src/components/CommunityPostCard.spec.js`
- Test: `frontend/src/views/CommunityListView.spec.js`
- Test: `frontend/src/components/CommunityPostCard.spec.js`

- [ ] **Step 1: Add failing tests for Chinese community hero/hot-board wording and the shared `经验贴` badge**

```js
expect(wrapper.text()).toContain("社区交流");
expect(wrapper.text()).toContain("热门讨论");
expect(wrapper.text()).toContain("登录后参与");
expect(wrapper.text()).toContain("暂无帖子");
expect(card.text()).toContain("经验贴");
expect(card.text()).not.toContain("Experience Post");
```

- [ ] **Step 2: Run the community specs to verify they fail**

Run: `npm --prefix frontend test -- src/views/CommunityListView.spec.js src/components/CommunityPostCard.spec.js`
Expected: FAIL because the page and shared card still render English hero and badge wording.

- [ ] **Step 3: Update `CommunityListView.vue` and `CommunityPostCard.vue` with the approved Chinese community wording**

```js
const TAG_OPTIONS = [
  { value: "", label: "全部", eyebrow: "总览" },
  { value: "CAREER", label: "就业", eyebrow: "方向" },
  { value: "EXAM", label: "考研", eyebrow: "备考" },
  { value: "ABROAD", label: "留学", eyebrow: "申请" },
  { value: "CHAT", label: "闲聊", eyebrow: "交流" },
];
```

```vue
<span v-if="experienceEnabled" class="community-post-card__experience-badge">经验贴</span>
```

- [ ] **Step 4: Re-run the community specs**

Run: `npm --prefix frontend test -- src/views/CommunityListView.spec.js src/components/CommunityPostCard.spec.js`
Expected: PASS with community hero, hot-board copy, and shared post badge all localized.

- [ ] **Step 5: Commit the community slice**

```bash
git add frontend/src/views/CommunityListView.vue frontend/src/views/CommunityListView.spec.js frontend/src/components/CommunityPostCard.vue frontend/src/components/CommunityPostCard.spec.js
git commit -m "feat: localize community surfaces"
```

## Task 6: Localize Profile And Notification Center Copy

**Files:**
- Modify: `frontend/src/views/ProfileView.vue`
- Modify: `frontend/src/views/ProfileView.spec.js`
- Modify: `frontend/src/views/NotificationCenterView.vue`
- Modify: `frontend/src/views/NotificationCenterView.spec.js`
- Test: `frontend/src/views/ProfileView.spec.js`
- Test: `frontend/src/views/NotificationCenterView.spec.js`

- [ ] **Step 1: Add failing assertions for Chinese personal-center headings, quick links, form actions, and notification-center title copy**

```js
expect(profileWrapper.text()).toContain("个人中心");
expect(profileWrapper.text()).toContain("我的帖子");
expect(profileWrapper.text()).toContain("保存资料");
expect(notificationWrapper.text()).toContain("通知中心");
expect(notificationWrapper.text()).not.toContain("Notification Center");
```

- [ ] **Step 2: Run the profile and notification specs to verify they fail**

Run: `npm --prefix frontend test -- src/views/ProfileView.spec.js src/views/NotificationCenterView.spec.js`
Expected: FAIL because the profile view still renders English section names and the notification view still includes a mixed-language eyebrow.

- [ ] **Step 3: Update `ProfileView.vue` and `NotificationCenterView.vue` with Chinese-first titles, quick-link labels, hints, and action text**

```vue
<span class="section-eyebrow">个人中心</span>
<RouterLink to="/notifications" class="app-link">查看通知</RouterLink>
```

```vue
<span class="section-eyebrow">通知</span>
<button type="button" class="ghost-btn">{{ markingAll ? "处理中..." : "全部标记为已读" }}</button>
```

- [ ] **Step 4: Re-run the profile and notification specs**

Run: `npm --prefix frontend test -- src/views/ProfileView.spec.js src/views/NotificationCenterView.spec.js`
Expected: PASS with profile and notification behavior unchanged and the presentation localized.

- [ ] **Step 5: Commit the profile/notification slice**

```bash
git add frontend/src/views/ProfileView.vue frontend/src/views/ProfileView.spec.js frontend/src/views/NotificationCenterView.vue frontend/src/views/NotificationCenterView.spec.js
git commit -m "feat: localize profile and notification copy"
```

## Task 7: Localize Jobs And Resources Pages Plus Their Shared Cards

**Files:**
- Modify: `frontend/src/views/JobsListView.vue`
- Modify: `frontend/src/views/JobsListView.spec.js`
- Modify: `frontend/src/components/JobFilterBar.vue`
- Modify: `frontend/src/components/JobPostingCard.vue`
- Modify: `frontend/src/views/ResourcesListView.vue`
- Modify: `frontend/src/views/ResourcesListView.spec.js`
- Modify: `frontend/src/components/ResourceFilterBar.vue`
- Modify: `frontend/src/components/ResourceCard.vue`
- Test: `frontend/src/views/JobsListView.spec.js`
- Test: `frontend/src/views/ResourcesListView.spec.js`

- [ ] **Step 1: Add failing assertions for Chinese page heroes, filter actions, CTA labels, and card fallback copy on jobs/resources pages**

```js
expect(jobsWrapper.text()).toContain("岗位机会");
expect(jobsWrapper.text()).toContain("筛选条件");
expect(jobsWrapper.text()).toContain("应用筛选");
expect(resourcesWrapper.text()).toContain("资料库");
expect(resourcesWrapper.text()).toContain("上传资料");
expect(resourcesWrapper.text()).toContain("暂无摘要");
```

- [ ] **Step 2: Run the jobs/resources specs to verify they fail**

Run: `npm --prefix frontend test -- src/views/JobsListView.spec.js src/views/ResourcesListView.spec.js`
Expected: FAIL because the pages and shared components still render English hero, filter, and fallback text.

- [ ] **Step 3: Update the jobs/resources views plus their filter/card components with stable Chinese terminology**

```js
const cityOptions = [
  { value: "", label: "全部城市" },
  { value: "Shenzhen", label: "深圳" },
];
```

```vue
<button type="submit" class="app-btn">{{ loading ? "加载中..." : "应用筛选" }}</button>
```

```vue
<span v-if="resource.favoritedByMe" class="status-badge approved">已收藏</span>
<p class="resource-card__summary">{{ resource.summary || "暂无摘要" }}</p>
```

- [ ] **Step 4: Re-run the jobs/resources specs**

Run: `npm --prefix frontend test -- src/views/JobsListView.spec.js src/views/ResourcesListView.spec.js`
Expected: PASS with filter-driven fetch behavior intact and user-facing copy localized.

- [ ] **Step 5: Commit the jobs/resources slice**

```bash
git add frontend/src/views/JobsListView.vue frontend/src/views/JobsListView.spec.js frontend/src/components/JobFilterBar.vue frontend/src/components/JobPostingCard.vue frontend/src/views/ResourcesListView.vue frontend/src/views/ResourcesListView.spec.js frontend/src/components/ResourceFilterBar.vue frontend/src/components/ResourceCard.vue
git commit -m "feat: localize jobs and resources pages"
```

## Task 8: Localize Login And Register Pages

**Files:**
- Modify: `frontend/src/views/LoginView.vue`
- Modify: `frontend/src/views/LoginView.spec.js`
- Modify: `frontend/src/views/RegisterView.vue`
- Create: `frontend/src/views/RegisterView.spec.js`
- Test: `frontend/src/views/LoginView.spec.js`
- Test: `frontend/src/views/RegisterView.spec.js`

- [ ] **Step 1: Add failing assertions for Chinese-first login/register titles, helper copy, CTA labels, and validation messages**

```js
expect(loginWrapper.text()).toContain("手机号验证码登录");
expect(loginWrapper.text()).toContain("获取验证码");
expect(registerWrapper.text()).toContain("注册");
expect(registerWrapper.text()).toContain("创建账号");
expect(registerWrapper.text()).toContain("11 位手机号");
```

- [ ] **Step 2: Run the auth specs to verify they fail or are missing**

Run: `npm --prefix frontend test -- src/views/LoginView.spec.js src/views/RegisterView.spec.js`
Expected: FAIL because `RegisterView.spec.js` does not exist yet and the current auth surfaces still include leftover English presentation around the flow.

- [ ] **Step 3: Update the auth views and add the missing register spec**

```vue
<span class="section-eyebrow">登录</span>
<span class="section-eyebrow">注册</span>
```

```js
test("renders Chinese register flow copy", async () => {
  const wrapper = mount(RegisterView);
  expect(wrapper.text()).toContain("注册");
  expect(wrapper.text()).toContain("获取验证码");
});
```

- [ ] **Step 4: Re-run the auth specs**

Run: `npm --prefix frontend test -- src/views/LoginView.spec.js src/views/RegisterView.spec.js`
Expected: PASS with login/register flows still working and user-facing wording fully Chinese-first.

- [ ] **Step 5: Commit the auth slice**

```bash
git add frontend/src/views/LoginView.vue frontend/src/views/LoginView.spec.js frontend/src/views/RegisterView.vue frontend/src/views/RegisterView.spec.js
git commit -m "feat: localize auth page copy"
```

## Task 9: Localize Secondary Decision-Support Pages

**Files:**
- Modify: `frontend/src/views/AssessmentView.vue`
- Modify: `frontend/src/views/AssessmentView.spec.js`
- Modify: `frontend/src/views/TimelineView.vue`
- Modify: `frontend/src/views/TimelineView.spec.js`
- Modify: `frontend/src/views/AnalyticsView.vue`
- Modify: `frontend/src/views/AnalyticsView.spec.js`
- Modify: `frontend/src/views/SchoolCompareView.vue`
- Modify: `frontend/src/views/SchoolCompareView.spec.js`
- Test: `frontend/src/views/AssessmentView.spec.js`
- Test: `frontend/src/views/TimelineView.spec.js`
- Test: `frontend/src/views/AnalyticsView.spec.js`
- Test: `frontend/src/views/SchoolCompareView.spec.js`

- [ ] **Step 1: Add failing assertions for Chinese page titles and action labels on assessment, timeline, analytics, and school-compare pages**

```js
expect(assessmentWrapper.text()).toContain("测评");
expect(timelineWrapper.text()).toContain("时间线");
expect(analyticsWrapper.text()).toContain("路径分析");
expect(compareWrapper.text()).toContain("院校对比");
expect(compareWrapper.text()).toContain("返回时间线");
```

- [ ] **Step 2: Run the secondary decision-support specs to verify they fail**

Run: `npm --prefix frontend test -- src/views/AssessmentView.spec.js src/views/TimelineView.spec.js src/views/AnalyticsView.spec.js src/views/SchoolCompareView.spec.js`
Expected: FAIL because these pages still render `Decision Desk`, `Back Home`, `Open Assessment`, `School Compare`, and other English-first copy.

- [ ] **Step 3: Update the four decision-support views with Chinese-first page copy while preserving all workflow logic**

```vue
<span class="section-eyebrow">成长决策</span>
<RouterLink to="/timeline" class="app-link">查看时间线</RouterLink>
<RouterLink to="/assessment" class="app-link">前往测评</RouterLink>
```

- [ ] **Step 4: Re-run the secondary decision-support specs**

Run: `npm --prefix frontend test -- src/views/AssessmentView.spec.js src/views/TimelineView.spec.js src/views/AnalyticsView.spec.js src/views/SchoolCompareView.spec.js`
Expected: PASS with forms, routing, and comparison behavior preserved under localized copy.

- [ ] **Step 5: Commit the secondary-page slice**

```bash
git add frontend/src/views/AssessmentView.vue frontend/src/views/AssessmentView.spec.js frontend/src/views/TimelineView.vue frontend/src/views/TimelineView.spec.js frontend/src/views/AnalyticsView.vue frontend/src/views/AnalyticsView.spec.js frontend/src/views/SchoolCompareView.vue frontend/src/views/SchoolCompareView.spec.js
git commit -m "feat: localize decision support pages"
```

## Task 10: Run A Leftover-English Audit And Full Frontend Regression

**Files:**
- Modify: any touched frontend file that still leaks top-level English product wording
- Test: `frontend/package.json`

- [ ] **Step 1: Audit the frontend for leftover top-level English product copy**

Run: `rg -n "Home|Discover|Search|Board|Desk|Archive|Opportunity|Unified|Back Home|Retry|Loading" frontend/src`
Expected: only technical identifiers, route names, or approved secondary brand text remain; any leftover user-facing English copy should be fixed before closeout.

- [ ] **Step 2: Run the full frontend Vitest suite**

Run: `npm --prefix frontend test`
Expected: PASS across the full frontend suite after the localization rollout.

- [ ] **Step 3: Run a production build**

Run: `npm --prefix frontend run build`
Expected: PASS with a successful Vite production build and no encoding-related compile failures.

- [ ] **Step 4: If the audit or regression exposes drift, apply the smallest wording-only fixes and re-run the affected checks**

```bash
npm --prefix frontend test -- src/views/HomeView.spec.js
npm --prefix frontend run build
```

- [ ] **Step 5: Commit the final verified rollout**

```bash
git add frontend/src/components/NavBar.vue frontend/src/components/NavBar.spec.js frontend/src/views/HomeView.vue frontend/src/views/HomeView.spec.js frontend/src/components/HomeEntryCard.vue frontend/src/components/DiscoverItemCard.vue frontend/src/components/DiscoverItemCard.spec.js frontend/src/views/DiscoverView.vue frontend/src/views/DiscoverView.spec.js frontend/src/views/SearchView.vue frontend/src/views/SearchView.spec.js frontend/src/components/SearchResultCard.vue frontend/src/components/SearchResultCard.spec.js frontend/src/views/CommunityListView.vue frontend/src/views/CommunityListView.spec.js frontend/src/components/CommunityPostCard.vue frontend/src/components/CommunityPostCard.spec.js frontend/src/views/ProfileView.vue frontend/src/views/ProfileView.spec.js frontend/src/views/NotificationCenterView.vue frontend/src/views/NotificationCenterView.spec.js frontend/src/views/JobsListView.vue frontend/src/views/JobsListView.spec.js frontend/src/components/JobFilterBar.vue frontend/src/components/JobPostingCard.vue frontend/src/views/ResourcesListView.vue frontend/src/views/ResourcesListView.spec.js frontend/src/components/ResourceFilterBar.vue frontend/src/components/ResourceCard.vue frontend/src/views/LoginView.vue frontend/src/views/LoginView.spec.js frontend/src/views/RegisterView.vue frontend/src/views/RegisterView.spec.js frontend/src/views/AssessmentView.vue frontend/src/views/AssessmentView.spec.js frontend/src/views/TimelineView.vue frontend/src/views/TimelineView.spec.js frontend/src/views/AnalyticsView.vue frontend/src/views/AnalyticsView.spec.js frontend/src/views/SchoolCompareView.vue frontend/src/views/SchoolCompareView.spec.js
git commit -m "feat: complete frontend Chinese localization rollout"
```
