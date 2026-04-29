<script setup>
import { computed, onMounted, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import { getHomeSummary } from "../api/home.js";
import DiscoverItemCard from "../components/DiscoverItemCard.vue";
import HomeEntryCard from "../components/HomeEntryCard.vue";
import VerificationStatusBadge from "../components/VerificationStatusBadge.vue";
import { useUserStore } from "../stores/user.js";

const userStore = useUserStore();
const router = useRouter();

const loading = ref(true);
const errorMessage = ref("");
const searchKeyword = ref("");
const summary = ref({
  viewerType: userStore.isAuthenticated ? "USER" : "GUEST",
  identity: null,
  roleLabel: userStore.isAuthenticated ? userStore.roleLabel : "访客",
  verificationStatus: userStore.profile?.verificationStatus ?? null,
  unreadNotificationCount: userStore.unreadCount ?? 0,
  todos: [],
  entries: [],
  latestNotifications: [],
  discoverPreview: {
    period: "WEEK",
    items: [],
  },
});

const entryMetaMap = {
  community: { label: "平台资讯" },
  jobs: { label: "就业方向" },
  resources: { label: "考研方向" },
  assessment: { label: "留学方向" },
  analytics: { label: "路径分析" },
  "admin-dashboard": { label: "运营总览" },
  "admin-verifications": { label: "认证审核" },
};

function findEntry(code) {
  return (summary.value.entries || []).find((item) => item.code === code) || null;
}

function badgeToText(badge) {
  if (badge === "COMING_SOON") {
    return "规划中";
  }

  if (badge === "LOGIN_REQUIRED") {
    return "登录后开启";
  }

  return "已纳入首页";
}

function entryStatusText(entry) {
  if (!entry) {
    return badgeToText("COMING_SOON");
  }

  if (entry.badge) {
    return badgeToText(entry.badge);
  }

  if (entry.enabled) {
    return "已开放";
  }

  return badgeToText("COMING_SOON");
}

function translateRole(role) {
  const roleMap = {
    ADMIN: "管理员",
    USER: "普通用户",
    GUEST: "访客",
  };

  return roleMap[role || "GUEST"] || "访客";
}

function translateVerification(status) {
  const statusMap = {
    UNVERIFIED: "待认证",
    PENDING: "审核中",
    VERIFIED: "已认证",
  };

  return statusMap[status || "UNVERIFIED"] || "待认证";
}

function translateTodo(todo) {
  if (!todo) {
    return "当前没有待处理事项，可以先浏览首页入口。";
  }

  if (todo === "Sign in to unlock profile, verification, and notifications.") {
    return "登录后即可解锁个人资料、认证申请与通知中心。";
  }

  if (todo === "Complete student verification.") {
    return "完成学生身份认证，后续通知和审核结果会统一回到个人中心。";
  }

  if (todo === "Student verification is under review.") {
    return "认证申请审核中，请在通知中心留意最新反馈。";
  }

  if (todo === "No pending tasks.") {
    return "当前没有待处理事项，可以先浏览首页入口。";
  }

  const unreadMatch = todo.match(/^You have (\d+) unread notifications\.$/);

  if (unreadMatch) {
    return `当前有 ${unreadMatch[1]} 条未读通知待处理。`;
  }

  const reviewMatch = todo.match(/^Review (\d+) pending verification applications\.$/);

  if (reviewMatch) {
    return `还有 ${reviewMatch[1]} 条认证申请待审核。`;
  }

  return todo;
}

function formatNotificationTime(value) {
  if (!value) {
    return "刚刚";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "刚刚";
  }

  return new Intl.DateTimeFormat("zh-CN", {
    month: "numeric",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

const isGuest = computed(() => summary.value.viewerType === "GUEST");
const canReviewVerifications = computed(() => {
  const currentRole = summary.value.identity?.role || userStore.profile?.role;
  return currentRole === "ADMIN";
});

const viewerName = computed(() => (
  summary.value.identity?.nickname
  || userStore.profile?.nickname
  || "访客"
));

const heroEyebrow = computed(() => (
  "学生成长服务平台"
));

const heroTitle = computed(() => (
  isGuest.value
    ? "把就业、考研、留学放到一个首页里，先看方向，再做决定"
    : `你好，${viewerName.value}，今天先从这几件事开始`
));

const heroCopy = computed(() => (
  isGuest.value
    ? "公开内容、常用入口和成长方向会集中展示，先帮你看清选择，再进入具体模块。"
    : "认证进度、未读通知和常用入口都会集中在这里，帮你先处理当下，再继续规划下一步。"
));

const primaryStatusChip = computed(() => (
  isGuest.value
    ? "首页服务已开启"
    : `${translateRole(summary.value.identity?.role || userStore.profile?.role)} / ${viewerName.value}`
));

const translatedTodos = computed(() => {
  const items = summary.value.todos?.length ? summary.value.todos : ["No pending tasks."];
  return items.map(translateTodo);
});

const statCards = computed(() => [
  {
    label: "身份角色",
    value: isGuest.value
      ? "访客"
      : translateRole(summary.value.identity?.role || userStore.profile?.role),
  },
  {
    label: "认证状态",
    value: isGuest.value
      ? "待登录"
      : translateVerification(summary.value.verificationStatus || userStore.profile?.verificationStatus),
  },
  {
    label: "未读通知",
    value: String(summary.value.unreadNotificationCount || 0),
  },
  {
    label: "今日待办",
    value: String(translatedTodos.value.length),
  },
]);

const strategyTracks = computed(() => [
  {
    eyebrow: "就业",
    title: "先判断窗口期，再安排投递动作",
    description: "把岗位情报、宣讲会和简历准备放在同一张桌面上，减少分散查找。",
    bullets: ["岗位更新集中浏览", "简历材料分阶段准备", "宣讲与面试节奏统一感知"],
    tone: "career",
    badge: findEntry("jobs")?.enabled ? "已开放岗位浏览" : badgeToText(findEntry("jobs")?.badge),
  },
  {
    eyebrow: "考研",
    title: "围绕目标院校整理资料与节奏",
    description: "把备考节点、资料收集和后续导师联系，收进一个更清晰的学习路径。",
    bullets: ["目标院校与方向对照", "材料与时间节点梳理", "后续课程与通知承接"],
    tone: "exam",
    badge: badgeToText(findEntry("resources")?.badge),
  },
  {
    eyebrow: "留学",
    title: "先做方向判断，再推进申请准备",
    description: "把语言、材料、时间线和申请提醒聚合展示，避免准备过程过早碎片化。",
    bullets: ["语言与材料前置提醒", "项目筛选与比较记录", "申请节奏与结果追踪"],
    tone: "abroad",
    badge: entryStatusText(findEntry("assessment")),
  },
]);

const serviceCards = computed(() => {
  const verificationStatus = summary.value.verificationStatus || userStore.profile?.verificationStatus || "UNVERIFIED";
  const cards = [
    {
      code: "Desk 00",
      title: "社区讨论",
      description: isGuest.value
        ? "先浏览经验帖和方向讨论，登录后再参与评论、点赞和收藏。"
        : "从社区继续查看经验帖、发帖补充观点，并沉淀你关注的话题。",
      path: "/community",
      enabled: true,
      metaLabel: isGuest.value ? "公开浏览 / 登录后互动" : "已开放社区主入口",
    },
    {
      code: "Desk 01",
      title: "个人中心",
      description: isGuest.value
        ? "登录后维护个人资料，提交学生认证申请。"
        : "维护资料、查看身份状态，并继续补全认证信息。",
      path: isGuest.value ? "/login" : "/profile",
      enabled: true,
      metaLabel: isGuest.value
        ? "登录后开启"
        : verificationStatus === "VERIFIED"
          ? "已完成身份认证"
          : verificationStatus === "PENDING"
            ? "认证审核中"
            : "建议优先提交认证",
    },
    {
      code: "Desk 02",
      title: "通知中心",
      description: isGuest.value
        ? "登录后集中查看审核结果、平台提醒与后续待办。"
        : "统一处理未读通知、审核反馈和阶段提醒。",
      path: isGuest.value ? "/login" : "/notifications",
      enabled: true,
      metaLabel: isGuest.value
        ? "登录后可用"
        : summary.value.unreadNotificationCount > 0
          ? `${summary.value.unreadNotificationCount} 条未读`
          : "当前已清空待读",
    },
    {
      code: "Track 03",
      title: "就业方向",
      description: "岗位、实习和宣讲信息会继续并入首页总览。",
      path: "/jobs",
      enabled: true,
      metaLabel: findEntry("jobs")?.enabled ? "进入岗位聚合" : badgeToText(findEntry("jobs")?.badge),
    },
    {
      code: "Track 04",
      title: "考研方向",
      description: "目标院校资料、课程安排与备考提醒正在接入。",
      path: "/resources",
      enabled: true,
      metaLabel: findEntry("resources")?.enabled ? "进入资料库" : badgeToText(findEntry("resources")?.badge),
    },
    {
      code: "Track 05",
      title: "留学方向",
      description: "语言、材料和申请时间线会在后续阶段补齐。",
      path: findEntry("assessment")?.enabled ? "/assessment" : "/login",
      enabled: Boolean(findEntry("assessment")?.enabled),
      metaLabel: entryStatusText(findEntry("assessment")),
    },
    {
      code: "Track 06",
      title: "路径分析",
      description: "把公开趋势、方向变化和你的当前状态放到同一个视图里，方便继续判断下一步。",
      path: "/analytics",
      enabled: Boolean(findEntry("analytics")?.enabled),
      metaLabel: entryStatusText(findEntry("analytics")),
    },
  ];

  if (canReviewVerifications.value) {
    cards.push({
      code: "Desk 06",
      title: "运营总览",
      description: "先看当日队列和计数，再进入具体审核与维护页面。",
      path: "/admin/dashboard",
      enabled: true,
      metaLabel: "进入总览",
    });
    cards.push({
      code: "Desk 07",
      title: "认证审核",
      description: "教师与管理员可统一审核学生认证申请。",
      path: "/admin/verifications",
      enabled: true,
      metaLabel: "进入审核队列",
    });
    cards.push({
      code: "Desk 08",
      title: "社区管理",
      description: "查看帖子状态和基础计数，并执行下架或删除操作。",
      path: "/admin/community",
      enabled: true,
      metaLabel: "管理员专属入口",
    });
    cards.push({
      code: "Desk 09",
      title: "岗位管理",
      description: "创建、编辑、发布和下线岗位卡片，保持前台岗位聚合数据可用。",
      path: "/admin/jobs",
      enabled: true,
      metaLabel: "管理员岗位维护入口",
    });
  }

  return cards;
});

const roadmapSignals = computed(() => (
  (summary.value.entries || []).map((item) => ({
    label: entryMetaMap[item.code]?.label || item.title,
    state: item.badge ? badgeToText(item.badge) : (item.enabled ? "已纳入首页" : "需登录"),
  }))
));

const latestNotifications = computed(() => summary.value.latestNotifications || []);
const discoverPreview = computed(() => summary.value.discoverPreview || { period: "WEEK", items: [] });
const discoverPreviewEmptyTitle = computed(() => (
  discoverPreview.value.period === "ALL"
    ? "趋势内容还在整理中"
    : "本周趋势还在更新中"
));
const discoverPreviewEmptyCopy = computed(() => (
  discoverPreview.value.period === "ALL"
    ? "全部时段还没有新的趋势内容。"
    : "本周还没有新的趋势内容，稍后再来看看。"
));
const homeDiscoverLink = computed(() => ({
  name: "discover",
  query: {
    tab: "ALL",
    period: "WEEK",
  },
}));

async function loadSummary() {
  loading.value = true;
  errorMessage.value = "";

  try {
    const data = await getHomeSummary();
    summary.value = data;

    if (data.identity) {
      userStore.applyHomeSummary(data);
    }
  } catch (error) {
    errorMessage.value = error.message || "首页聚合数据加载失败，请稍后重试。";
  } finally {
    loading.value = false;
  }
}

function submitSearch() {
  const keyword = searchKeyword.value.trim();

  if (!keyword) {
    return;
  }

  router.push({
    name: "search",
    query: {
      q: keyword,
      type: "ALL",
      sort: "RELEVANCE",
    },
  });
}

onMounted(loadSummary);
</script>

<template>
  <section class="flex flex-col gap-8 p-4 md:p-8">
    <div class="grid gap-8">
      <article class="bg-white rounded-xl shadow-sm border border-slate-100 p-8 md:p-12 hover:shadow-md transition-shadow relative overflow-hidden">
        <span class="text-violet-600 text-sm font-semibold tracking-wider uppercase mb-2 block">{{ heroEyebrow }}</span>
        <h1 class="text-4xl md:text-6xl font-bold text-slate-900 mb-6" data-test="home-hero-title">{{ heroTitle }}</h1>
        <hr class="border-t border-slate-200 my-6" />
        <p class="text-lg text-slate-600 mb-8 max-w-3xl" data-test="home-hero-copy">{{ heroCopy }}</p>

        <form class="grid gap-3 mt-8" data-test="home-search-form" @submit.prevent="submitSearch">
          <label class="text-slate-500 text-sm font-semibold tracking-wider uppercase" for="home-search" data-test="home-search-label">
            站内搜索
          </label>
          <div class="grid grid-cols-1 md:grid-cols-[1fr_auto] gap-3 items-center">
            <input
              id="home-search"
              v-model="searchKeyword"
              name="home-search"
              type="search"
              class="min-h-[52px] w-full px-5 border border-slate-200 rounded-full bg-slate-50 text-slate-900 focus:outline-none focus:ring-2 focus:ring-violet-500/50 focus:border-violet-500 transition-all"
              placeholder="搜索经验帖、岗位、院校、资料"
              autocomplete="off"
            />
            <button type="submit" class="bg-violet-600 text-white rounded-full px-8 py-3 hover:bg-violet-700 font-medium transition-colors flex items-center justify-center min-h-[52px]">
              搜索
            </button>
          </div>
        </form>

        <div class="flex flex-wrap gap-3 mt-8" style="margin-top: 24px;">
          <span class="inline-flex items-center px-3 py-1 rounded-full bg-green-100 text-green-700 text-sm font-medium" data-test="home-status-chip">
            {{ primaryStatusChip }}
          </span>
          <VerificationStatusBadge
            v-if="!isGuest"
            :status="summary.verificationStatus || userStore.profile?.verificationStatus || 'UNVERIFIED'"
          />
          <span
            v-if="!isGuest && summary.unreadNotificationCount > 0"
            class="inline-flex items-center px-3 py-1 rounded-full bg-amber-100 text-amber-700 text-sm font-medium"
            data-test="home-unread-chip"
          >
            {{ summary.unreadNotificationCount }} 条未读通知
          </span>
        </div>

        <div class="flex flex-wrap gap-4 mt-8" style="margin-top: 28px;">
          <RouterLink v-if="isGuest" to="/login" class="bg-violet-600 text-white rounded-lg px-6 py-3 hover:bg-violet-700 font-medium transition-colors" data-test="home-primary-cta">
            登录查看个人待办
          </RouterLink>
          <RouterLink v-if="isGuest" to="/register" class="bg-white text-violet-600 border border-violet-600 rounded-lg px-6 py-3 hover:bg-violet-50 font-medium transition-colors" data-test="home-secondary-cta">
            立即注册
          </RouterLink>
          <RouterLink v-if="!isGuest" to="/profile" class="bg-violet-600 text-white rounded-lg px-6 py-3 hover:bg-violet-700 font-medium transition-colors" data-test="home-primary-cta">
            进入个人中心
          </RouterLink>
          <RouterLink v-if="!isGuest" to="/notifications" class="bg-white text-violet-600 border border-violet-600 rounded-lg px-6 py-3 hover:bg-violet-50 font-medium transition-colors" data-test="home-secondary-cta">
            查看通知
          </RouterLink>
          <RouterLink v-if="canReviewVerifications" to="/admin/verifications" class="text-violet-600 hover:text-violet-700 font-medium hover:underline inline-flex items-center">
            进入认证审核台
          </RouterLink>
        </div>

        <div v-if="roadmapSignals.length" class="flex flex-wrap gap-3 mt-8">
          <span
            v-for="signal in roadmapSignals"
            :key="`${signal.label}-${signal.state}`"
            class="inline-flex items-center min-h-[34px] px-4 rounded-full border border-slate-200 bg-slate-50 text-slate-600 text-sm"
          >
            {{ signal.label }}：{{ signal.state }}
          </span>
        </div>
      </article>

      <article class="bg-white rounded-xl shadow-sm border border-slate-100 p-6 md:p-8 hover:shadow-md transition-shadow" data-test="home-section-snapshot">
        <div class="flex flex-col md:flex-row md:justify-between md:items-end gap-4 mb-8">
          <div>
            <span class="text-violet-600 text-sm font-semibold tracking-wider uppercase mb-2 block">今日概览</span>
            <h2 class="text-2xl md:text-3xl font-bold text-slate-900 mt-2" style="margin-top: 16px;">先看清你现在的状态和待办</h2>
          </div>
        </div>

        <div v-if="loading" class="text-slate-500 py-8 text-center bg-slate-50 rounded-xl border border-dashed border-slate-200">正在整理首页聚合数据...</div>

        <div v-else-if="errorMessage" class="grid gap-4">
          <p class="text-red-600 bg-red-50 p-4 rounded-lg border border-red-100" role="alert">{{ errorMessage }}</p>
          <button type="button" class="bg-white text-violet-600 border border-violet-600 rounded-lg px-6 py-3 hover:bg-violet-50 font-medium transition-colors" @click="loadSummary">
            重新加载
          </button>
        </div>

        <div v-else class="grid gap-6">
          <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
            <article
              v-for="card in statCards"
              :key="card.label"
              class="bg-slate-50 rounded-xl p-6 flex flex-col justify-end min-h-[138px] border border-slate-100"
            >
              <p class="text-slate-500 text-sm mb-2">{{ card.label }}</p>
              <strong class="text-3xl md:text-4xl font-bold text-slate-900">{{ card.value }}</strong>
            </article>
          </div>

          <article class="bg-slate-50 rounded-xl p-6 border border-slate-100">
            <p class="text-slate-500 text-sm mb-2">今日提醒</p>
            <ul class="list-disc pl-5 mt-4 space-y-2 text-slate-700">
              <li v-for="todo in translatedTodos" :key="todo">
                {{ todo }}
              </li>
            </ul>
          </article>
        </div>
      </article>
    </div>

    <article class="bg-white rounded-xl shadow-sm border border-slate-100 p-6 md:p-8 hover:shadow-md transition-shadow" data-test="home-section-entries">
      <div class="flex flex-col md:flex-row md:justify-between md:items-end gap-4 mb-8">
        <div>
          <span class="text-violet-600 text-sm font-semibold tracking-wider uppercase mb-2 block">常用入口</span>
          <h2 class="text-2xl md:text-3xl font-bold text-slate-900 mt-2" style="margin-top: 16px;">从首页直接进入当前最需要的入口</h2>
          <p class="text-slate-600 mt-2 max-w-3xl" style="margin-top: 16px;">
            先开放个人中心、通知中心和管理审核台，三条方向入口保留为后续阶段能力。
          </p>
        </div>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <HomeEntryCard
          v-for="card in serviceCards"
          :key="card.code"
          :entry="card"
        />
      </div>
    </article>

    <article class="bg-white rounded-xl shadow-sm border border-slate-100 p-6 md:p-8 hover:shadow-md transition-shadow" data-test="home-section-tracks">
      <div class="flex flex-col md:flex-row md:justify-between md:items-end gap-4 mb-8">
        <div>
          <span class="text-amber-500 text-sm font-semibold tracking-wider uppercase mb-2 block">成长方向</span>
          <h2 class="text-2xl md:text-3xl font-bold text-slate-900 mt-2" style="margin-top: 16px;">首页先讲方向，再进入模块</h2>
          <p class="text-slate-600 mt-2 max-w-3xl" style="margin-top: 16px;">
            就业、考研、留学三条主线在首页先做聚合说明，帮助学生先判断路径，再决定进入哪个具体功能。
          </p>
        </div>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-6 items-stretch">
        <article
          v-for="track in strategyTracks"
          :key="track.eyebrow"
          class="flex flex-col gap-4 p-6 md:p-8 rounded-xl border border-slate-100 shadow-sm hover:shadow-md transition-shadow"
          :class="{'bg-gradient-to-b from-orange-50 to-orange-100/50': track.tone === 'career', 'bg-gradient-to-b from-yellow-50 to-amber-50': track.tone === 'exam', 'bg-gradient-to-b from-teal-50 to-emerald-50': track.tone === 'abroad'}"
        >
          <p class="text-violet-600 text-sm font-semibold tracking-wider uppercase">{{ track.eyebrow }}</p>
          <h3 class="text-2xl font-bold text-slate-900">{{ track.title }}</h3>
          <p class="text-slate-600 leading-relaxed">{{ track.description }}</p>
          <ul class="list-disc pl-5 space-y-2 text-slate-600 mt-auto">
            <li v-for="bullet in track.bullets" :key="bullet">
              {{ bullet }}
            </li>
          </ul>
          <span class="inline-flex items-center min-h-[32px] px-4 rounded-full bg-slate-900/5 text-slate-900 text-sm font-semibold mt-4 self-start">{{ track.badge }}</span>
        </article>
      </div>
    </article>

    <article class="bg-white rounded-xl shadow-sm border border-slate-100 p-6 md:p-8 hover:shadow-md transition-shadow" data-test="home-section-discover">
      <div class="flex flex-col md:flex-row md:justify-between md:items-end gap-4 mb-8">
        <div>
          <span class="text-rose-500 text-sm font-semibold tracking-wider uppercase mb-2 block">本周趋势</span>
          <h2 class="text-2xl md:text-3xl font-bold text-slate-900 mt-2" style="margin-top: 16px;">看看这一周大家都在关注什么，再决定要不要深入查看</h2>
          <p class="text-slate-600 mt-2 max-w-3xl" style="margin-top: 16px;">
            首页先保留每周公开趋势，想看完整排序和更多内容，再进入趋势页。
          </p>
        </div>
        <RouterLink :to="homeDiscoverLink" class="app-link discover-preview__cta" data-test="home-discover-cta">
          查看全部趋势
        </RouterLink>
      </div>

      <div v-if="loading" class="text-slate-500 py-8 text-center bg-slate-50 rounded-xl border border-dashed border-slate-200">正在整理本周趋势...</div>
      <div v-else-if="discoverPreview.items.length === 0" class="empty-state discover-preview__empty">
        <strong>{{ discoverPreviewEmptyTitle }}</strong>
        <p class="text-slate-600 leading-relaxed">{{ discoverPreviewEmptyCopy }}</p>
      </div>
      <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <DiscoverItemCard
          v-for="item in discoverPreview.items"
          :key="`${item.type}-${item.id}`"
          :item="item"
        />
      </div>
    </article>

    <div class="grid md:grid-cols-2 gap-8 mt-8">
      <article class="bg-white rounded-xl shadow-sm border border-slate-100 p-6 md:p-8 hover:shadow-md transition-shadow" data-test="home-section-notifications">
        <div class="flex flex-col md:flex-row md:justify-between md:items-end gap-4 mb-8">
          <div>
            <span class="text-emerald-500 text-sm font-semibold tracking-wider uppercase mb-2 block">最新通知</span>
            <h2 class="text-2xl md:text-3xl font-bold text-slate-900 mt-2" style="margin-top: 16px;">及时查看通知与审核反馈</h2>
          </div>
          <RouterLink
            v-if="!isGuest"
            to="/notifications"
            class="text-violet-600 hover:text-violet-700 font-medium hover:underline inline-flex items-center"
          >
            查看全部通知
          </RouterLink>
        </div>

        <div v-if="loading" class="text-slate-500 py-8 text-center bg-slate-50 rounded-xl border border-dashed border-slate-200">正在同步最新通知...</div>
        <div v-else-if="latestNotifications.length === 0" class="text-slate-500 py-8 text-center bg-slate-50 rounded-xl border border-dashed border-slate-200">
          {{ isGuest ? "登录后可查看与你相关的通知和处理结果。" : "当前还没有新的通知。" }}
        </div>
        <div v-else class="grid gap-4">
          <article
            v-for="item in latestNotifications"
            :key="item.id"
            class="flex flex-col md:flex-row md:justify-between gap-4 p-5 md:p-6 rounded-xl border border-slate-200 bg-white/80 hover:bg-white transition-colors shadow-sm"
          >
            <div class="grid gap-2">
              <p class="text-slate-500 text-sm">{{ item.type || "系统通知" }}</p>
              <h3 class="text-xl font-bold text-slate-900">{{ item.title }}</h3>
              <p class="text-slate-600 leading-relaxed">{{ item.content }}</p>
            </div>
            <div class="flex md:flex-col justify-between md:justify-start items-center md:items-end gap-3 text-right">
              <span class="status-badge" :class="item.read ? 'approved' : 'pending'">
                {{ item.read ? "已读" : "未读" }}
              </span>
              <span class="text-slate-500 text-sm">{{ formatNotificationTime(item.createdAt) }}</span>
            </div>
          </article>
        </div>
      </article>

      <article class="bg-white rounded-xl shadow-sm border border-slate-100 p-6 md:p-8 hover:shadow-md transition-shadow">
        <div class="flex flex-col md:flex-row md:justify-between md:items-end gap-4 mb-8">
          <div>
            <span class="text-violet-600 text-sm font-semibold tracking-wider uppercase mb-2 block">首页说明</span>
            <h2 class="text-2xl md:text-3xl font-bold text-slate-900 mt-2" style="margin-top: 16px;">为什么这样安排首页结构</h2>
          </div>
        </div>

        <div class="grid gap-4">
          <article class="bg-slate-50 rounded-xl p-6 border border-slate-100">
            <strong>先总览，再深挖</strong>
            <p class="text-slate-600 leading-relaxed">
              学生先在首页看清三条方向和当前待办，再进入具体模块，能显著降低入口分散造成的切换成本。
            </p>
          </article>
          <article class="bg-slate-50 rounded-xl p-6 border border-slate-100">
            <strong>认证与通知形成闭环</strong>
            <p class="text-slate-600 leading-relaxed">
              个人中心提交认证申请，教师与管理员统一审核，结果最终回流到通知中心，路径更清晰。
            </p>
          </article>
          <article class="bg-slate-50 rounded-xl p-6 border border-slate-100">
            <strong>为后续方向功能留出位置</strong>
            <p class="text-slate-600 leading-relaxed">
              就业、考研、留学方向能力会继续向首页汇聚，当前先把结构和决策顺序固定下来。
            </p>
          </article>
        </div>
      </article>
    </div>
  </section>
</template>




