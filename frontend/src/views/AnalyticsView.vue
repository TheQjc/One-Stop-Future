<script setup>
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import { getAnalyticsSummary } from "../api/analytics.js";

const supportedPeriods = [
  { code: "7D", label: "近7天" },
  { code: "30D", label: "近30天" },
];

const trendDefinitions = [
  {
    key: "posts",
    label: "发布帖子",
    eyebrow: "社区",
    tone: "posts",
  },
  {
    key: "jobs",
    label: "活跃岗位",
    eyebrow: "就业",
    tone: "jobs",
  },
  {
    key: "resources",
    label: "公开资料",
    eyebrow: "资料",
    tone: "resources",
  },
  {
    key: "assessments",
    label: "测评会话",
    eyebrow: "决策",
    tone: "assessments",
  },
];

const trackLabelMap = {
  CAREER: "就业",
  EXAM: "考研",
  ABROAD: "留学",
};

const loading = ref(true);
const errorMessage = ref("");
const period = ref("30D");
const summary = ref({
  publicOverview: {
    publishedPostCount: 0,
    activeJobCount: 0,
    publishedResourceCount: 0,
    assessmentSessionCount: 0,
  },
  publicTrends: [],
  decisionDistribution: {
    participantCount: 0,
    tracks: [],
  },
  personalStatus: "ANONYMOUS",
  personalMessage: null,
  personalSnapshot: null,
  personalHistory: [],
  nextActions: [],
});

const overviewCards = computed(() => [
  {
    label: "已发布帖子",
    value: summary.value.publicOverview?.publishedPostCount ?? 0,
    hint: "当前公开展示的社区经验内容数量。",
  },
  {
    label: "活跃岗位",
    value: summary.value.publicOverview?.activeJobCount ?? 0,
    hint: "目前仍在公开列表中的岗位数量。",
  },
  {
    label: "已发布资料",
    value: summary.value.publicOverview?.publishedResourceCount ?? 0,
    hint: "当前公开可见的备考与申请资料数量。",
  },
  {
    label: "测评会话",
    value: summary.value.publicOverview?.assessmentSessionCount ?? 0,
    hint: "所有参与者已保存的方向测评次数。",
  },
]);

const trendCards = computed(() => trendDefinitions.map((definition) => {
  const points = (summary.value.publicTrends || []).map((item) => ({
    date: item.date,
    value: item[definition.key] ?? 0,
  }));
  const maxValue = Math.max(...points.map((item) => item.value), 1);
  const total = points.reduce((accumulator, item) => accumulator + item.value, 0);
  const activeDays = points.filter((item) => item.value > 0).length;

  return {
    ...definition,
    total,
    activeDays,
    points: points.map((item) => ({
      ...item,
      height: item.value > 0 ? `${Math.max((item.value / maxValue) * 100, 18)}%` : "8%",
      label: formatShortDate(item.date),
    })),
  };
}));

const hasTrendActivity = computed(() => trendCards.value.some((card) => card.total > 0));

const distributionCards = computed(() => {
  const tracks = summary.value.decisionDistribution?.tracks || [];
  const ordered = ["CAREER", "EXAM", "ABROAD"].map((trackCode) => (
    tracks.find((item) => item.track === trackCode)
    || { track: trackCode, count: 0, percent: 0 }
  ));

  return ordered.map((item) => ({
    ...item,
    label: trackLabelMap[item.track] || item.track,
    width: `${Math.max(item.percent || 0, item.count > 0 ? 10 : 0)}%`,
  }));
});

const personalStatus = computed(() => summary.value.personalStatus || "ANONYMOUS");
const personalSnapshot = computed(() => summary.value.personalSnapshot || null);
const personalHistory = computed(() => summary.value.personalHistory || []);
const nextActions = computed(() => summary.value.nextActions || []);
const selectedPeriodLabel = computed(() => (
  supportedPeriods.find((item) => item.code === period.value)?.label || "近30天"
));

function formatShortDate(value) {
  if (!value) {
    return "";
  }

  const date = new Date(`${value}T00:00:00`);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("zh-CN", {
    month: "numeric",
    day: "numeric",
  }).format(date);
}

async function loadSummary(nextPeriod = period.value) {
  loading.value = true;
  errorMessage.value = "";
  period.value = nextPeriod;

  try {
    summary.value = await getAnalyticsSummary({ period: nextPeriod });
  } catch (error) {
    errorMessage.value = error.message || "分析概览加载失败，请重试。";
  } finally {
    loading.value = false;
  }
}

function switchPeriod(nextPeriod) {
  if (nextPeriod === period.value && !errorMessage.value) {
    return;
  }

  loadSummary(nextPeriod);
}

function displayTrack(track) {
  return trackLabelMap[track] || track || "未确定";
}

function displayActionLabel(action) {
  if (!action) {
    return "查看详情";
  }

  if (action.code === "START_ASSESSMENT" || action.path === "/assessment") {
    return "开始测评";
  }
  if (action.code === "OPEN_TIMELINE" || action.path === "/timeline") {
    return "查看时间线";
  }
  if (action.code === "COMPARE_SCHOOLS" || action.path === "/schools/compare") {
    return "对比院校";
  }

  return action.label || "查看详情";
}

onMounted(() => {
  loadSummary("30D");
});
</script>

<template>
  <section class="page-stack">
    <article class="section-card analytics-hero">
      <div class="analytics-hero__copy">
        <span class="section-eyebrow">决策支持</span>
        <h1 class="hero-title" style="margin-top: 18px;">数据看板</h1>
        <hr class="editorial-rule" />
        <p class="hero-copy">
          从公开内容供给、测评活跃度和三类方向分布，快速看清平台当前的整体动向。
        </p>
        <div class="action-row analytics-hero__actions">
          <RouterLink to="/" class="ghost-btn">返回首页</RouterLink>
          <RouterLink to="/assessment" class="app-link">前往方向测评</RouterLink>
        </div>
      </div>

      <div class="analytics-hero__panel">
        <div v-if="loading" class="empty-state">正在加载数据概览...</div>
        <div v-else-if="errorMessage" class="field-grid">
          <p class="field-error" role="alert">{{ errorMessage }}</p>
          <button type="button" class="ghost-btn" @click="loadSummary(period)">
            重试
          </button>
        </div>
        <div v-else class="stats-grid analytics-overview-grid">
          <article
            v-for="card in overviewCards"
            :key="card.label"
            class="panel-card analytics-overview-card"
          >
            <p class="analytics-overview-card__label">{{ card.label }}</p>
            <strong class="analytics-overview-card__value">{{ card.value }}</strong>
            <p class="meta-copy">{{ card.hint }}</p>
          </article>
        </div>
      </div>
    </article>

    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">平台趋势</span>
          <h2 class="page-title" style="margin-top: 16px;">{{ selectedPeriodLabel }}的平台活跃情况。</h2>
          <p class="page-subtitle" style="margin-top: 16px;">
            切换周期会重新请求后端统计，前端不会在本地伪造筛选结果。
          </p>
        </div>
        <div class="period-switcher" role="tablist" aria-label="分析周期切换">
          <button
            v-for="item in supportedPeriods"
            :key="item.code"
            :data-test="`period-${item.code}`"
            type="button"
            class="period-switcher__button"
            :class="{ active: period === item.code }"
            :aria-selected="period === item.code"
            @click="switchPeriod(item.code)"
          >
            {{ item.label }}
          </button>
        </div>
      </div>

      <div v-if="loading" class="empty-state">正在加载趋势数据...</div>
      <div v-else-if="errorMessage" class="empty-state">
        分析概览恢复后，这里的趋势数据会同步显示。
      </div>
      <div v-else class="field-grid">
        <div v-if="!hasTrendActivity" class="empty-state analytics-empty">
          当前周期还没有记录到公开活跃数据，下方仍会保留零值趋势桶。
        </div>

        <div class="trend-grid">
          <article
            v-for="card in trendCards"
            :key="card.key"
            class="panel-card trend-card"
            :class="`trend-card--${card.tone}`"
          >
            <div class="trend-card__header">
              <div>
                <p class="trend-card__eyebrow">{{ card.eyebrow }}</p>
                <h3 class="trend-card__title">{{ card.label }}</h3>
              </div>
              <div class="trend-card__stat">
                <strong>{{ card.total }}</strong>
                <span>{{ card.activeDays }} 天有活跃</span>
              </div>
            </div>
            <div
              class="trend-card__bars"
              :style="{ gridTemplateColumns: `repeat(${card.points.length}, minmax(14px, 1fr))` }"
              :aria-label="`${card.label}趋势柱状图`"
            >
              <div
                v-for="point in card.points"
                :key="`${card.key}-${point.date}`"
                class="trend-card__bar-wrap"
              >
                <span class="trend-card__bar" :style="{ height: point.height }" />
                <span class="trend-card__tick">{{ point.label }}</span>
              </div>
            </div>
          </article>
        </div>
      </div>
    </article>

    <div class="dashboard-grid analytics-bottom-grid">
      <article class="section-card">
        <div class="section-header">
          <div>
            <span class="section-eyebrow">方向分布</span>
            <h2 class="page-title" style="margin-top: 16px;">最近一次已保存测评结果的推荐分布。</h2>
            <p class="page-subtitle" style="margin-top: 16px;">
              每位参与者只计入最近一次已保存结果，避免重复测评放大权重。
            </p>
          </div>
        </div>

        <div v-if="loading" class="empty-state">正在加载方向分布...</div>
        <div v-else class="distribution-grid">
          <article
            v-for="track in distributionCards"
            :key="track.track"
            :data-test="`mix-${track.track}`"
            class="panel-card distribution-card"
          >
            <div class="distribution-card__top">
              <p class="distribution-card__label">{{ track.label }}</p>
              <strong class="distribution-card__value">{{ track.percent.toFixed(1) }}%</strong>
            </div>
            <div class="distribution-card__meter" aria-hidden="true">
              <span class="distribution-card__fill" :style="{ width: track.width }" />
            </div>
            <p class="meta-copy">
              最近一次结果中有 {{ track.count }} 人
            </p>
          </article>
        </div>

        <p class="meta-copy analytics-participants">
          参与人数基数：{{ summary.decisionDistribution?.participantCount ?? 0 }}
        </p>
      </article>

      <article class="section-card">
        <div class="section-header">
          <div>
            <span class="section-eyebrow">我的分析</span>
            <h2 class="page-title" style="margin-top: 16px;">你的个人路径分析会和公共看板一起展示。</h2>
            <p class="page-subtitle" style="margin-top: 16px;">
              未登录时这里会保持游客模式，登录并完成测评后才会展示你的个人结果。
            </p>
          </div>
        </div>

        <div v-if="loading" class="empty-state">正在加载个人分析...</div>
        <div v-else-if="personalStatus === 'ANONYMOUS'" class="panel-card personal-card">
          <p class="personal-card__eyebrow">游客模式</p>
          <h3 class="personal-card__title">登录后查看你的个人路径分析。</h3>
          <p class="meta-copy">
            公开看板对所有人开放。登录后可查看你的个人推荐、最近测评记录和下一步建议。
          </p>
          <div class="action-row">
            <RouterLink to="/login" class="app-btn">登录</RouterLink>
            <RouterLink to="/register" class="ghost-btn">注册</RouterLink>
          </div>
        </div>
        <div v-else-if="personalStatus === 'ERROR'" class="panel-card personal-card" data-test="personal-error">
          <p class="personal-card__eyebrow">个人分析</p>
          <h3 class="personal-card__title">个人分析暂时不可用。</h3>
          <p class="meta-copy">
            {{ summary.personalMessage || "个人分析暂时不可用。" }}
          </p>
          <p class="meta-copy">
            上方公开数据仍可正常查看，稍后可以再试。
          </p>
        </div>
        <div v-else-if="personalStatus === 'EMPTY'" class="panel-card personal-card" data-test="personal-empty">
          <p class="personal-card__eyebrow">个人分析</p>
          <h3 class="personal-card__title">还没有测评记录。</h3>
          <p class="meta-copy">
            完成一次方向测评后，这里会展示你的推荐结果和历史记录。
          </p>
          <div class="action-row">
            <RouterLink to="/assessment" class="app-btn">开始测评</RouterLink>
          </div>
        </div>
        <div v-else-if="personalStatus === 'READY'" class="personal-ready" data-test="personal-ready">
          <article class="panel-card personal-ready__snapshot">
            <p class="personal-card__eyebrow">最近结果</p>
            <h3 class="personal-card__title">
              推荐方向：{{ displayTrack(personalSnapshot?.recommendedTrack) || "—" }}
            </h3>
            <p v-if="personalSnapshot?.summaryText" class="meta-copy">
              {{ personalSnapshot.summaryText }}
            </p>
            <p v-if="personalSnapshot?.sessionDate" class="meta-copy">
              测评日期：{{ personalSnapshot.sessionDate }}
            </p>
            <div v-if="personalSnapshot?.scores" class="personal-ready__scores">
              <div class="personal-ready__score">
                <span>就业</span>
                <strong>{{ personalSnapshot.scores.career }}</strong>
              </div>
              <div class="personal-ready__score">
                <span>考研</span>
                <strong>{{ personalSnapshot.scores.exam }}</strong>
              </div>
              <div class="personal-ready__score">
                <span>留学</span>
                <strong>{{ personalSnapshot.scores.abroad }}</strong>
              </div>
            </div>
          </article>

          <article class="panel-card personal-ready__actions" v-if="nextActions.length">
            <p class="personal-card__eyebrow">下一步建议</p>
            <div class="chip-row" style="margin-top: 12px;">
              <RouterLink
                v-for="action in nextActions"
                :key="action.code"
                :to="action.path"
                class="app-link"
              >
                {{ displayActionLabel(action) }}
              </RouterLink>
            </div>
            <p v-if="nextActions[0]?.description" class="meta-copy" style="margin-top: 14px;">
              {{ nextActions[0].description }}
            </p>
          </article>

          <article class="panel-card personal-ready__history" v-if="personalHistory.length">
            <p class="personal-card__eyebrow">最近记录</p>
            <div class="history-table" style="margin-top: 12px;">
              <div class="history-table__head">
                <span>日期</span>
                <span>方向</span>
                <span>就业</span>
                <span>考研</span>
                <span>留学</span>
              </div>
              <div
                v-for="item in personalHistory"
                :key="`${item.sessionDate}-${item.recommendedTrack}`"
                class="history-table__row"
              >
                <span>{{ item.sessionDate }}</span>
                <span>{{ displayTrack(item.recommendedTrack) }}</span>
                <span>{{ item.careerScore }}</span>
                <span>{{ item.examScore }}</span>
                <span>{{ item.abroadScore }}</span>
              </div>
            </div>
          </article>
        </div>
        <div v-else class="empty-state">
          登录后的个人分析会显示在这里。
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.analytics-hero {
  display: grid;
  grid-template-columns: 1.12fr 0.88fr;
  gap: var(--cp-gap-6);
  background:
    linear-gradient(180deg, rgba(255, 252, 247, 0.94), rgba(245, 237, 223, 0.98)),
    radial-gradient(circle at top left, rgba(24, 38, 63, 0.12), transparent 34%),
    radial-gradient(circle at bottom right, rgba(76, 122, 116, 0.16), transparent 40%);
}

.analytics-hero__copy,
.analytics-hero__panel {
  display: grid;
  gap: var(--cp-gap-4);
}

.analytics-hero__panel {
  align-content: end;
}

.analytics-overview-grid {
  gap: var(--cp-gap-4);
}

.analytics-overview-card {
  min-height: 152px;
  display: grid;
  gap: 10px;
  align-content: end;
}

.analytics-overview-card__label,
.trend-card__eyebrow,
.distribution-card__label,
.personal-card__eyebrow {
  margin: 0;
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.analytics-overview-card__value {
  font-size: clamp(26px, 4vw, 38px);
  line-height: 1.08;
  font-family: var(--cp-font-display);
}

.period-switcher {
  display: inline-flex;
  align-items: center;
  gap: var(--cp-gap-2);
  padding: 6px;
  border-radius: var(--cp-radius-pill);
  border: 1px solid var(--cp-line);
  background: rgba(255, 255, 255, 0.74);
}

.period-switcher__button {
  min-height: var(--cp-touch-height);
  padding: 0 16px;
  border: 0;
  border-radius: var(--cp-radius-pill);
  background: transparent;
  color: var(--cp-ink-soft);
  cursor: pointer;
  transition:
    background-color var(--cp-transition),
    color var(--cp-transition),
    transform var(--cp-transition);
}

.period-switcher__button:hover {
  transform: translateY(-1px);
  color: var(--cp-ink);
}

.period-switcher__button.active {
  background: rgba(255, 248, 242, 0.95);
  color: var(--cp-accent-deep);
  box-shadow: 0 10px 24px rgba(197, 79, 45, 0.12);
}

.analytics-empty {
  padding: var(--cp-gap-5);
  text-align: left;
}

.trend-grid,
.distribution-grid {
  display: grid;
  gap: var(--cp-gap-4);
}

.trend-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.distribution-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.trend-card {
  min-height: 100%;
  display: grid;
  gap: var(--cp-gap-4);
}

.trend-card--posts {
  background:
    linear-gradient(180deg, rgba(255, 248, 242, 0.96), rgba(255, 244, 235, 0.96)),
    radial-gradient(circle at top right, rgba(197, 79, 45, 0.12), transparent 42%);
}

.trend-card--jobs {
  background:
    linear-gradient(180deg, rgba(250, 253, 251, 0.96), rgba(241, 247, 244, 0.96)),
    radial-gradient(circle at top right, rgba(76, 122, 116, 0.14), transparent 42%);
}

.trend-card--resources {
  background:
    linear-gradient(180deg, rgba(255, 254, 249, 0.98), rgba(246, 239, 225, 0.96)),
    radial-gradient(circle at top right, rgba(24, 38, 63, 0.08), transparent 42%);
}

.trend-card--assessments {
  background:
    linear-gradient(180deg, rgba(248, 250, 255, 0.98), rgba(239, 243, 252, 0.96)),
    radial-gradient(circle at top right, rgba(24, 38, 63, 0.12), transparent 42%);
}

.trend-card__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: start;
}

.trend-card__title,
.personal-card__title {
  margin: 0;
  font-family: var(--cp-font-display);
  line-height: 1.12;
}

.trend-card__title {
  font-size: 24px;
}

.trend-card__stat {
  display: grid;
  justify-items: end;
  gap: 4px;
  text-align: right;
}

.trend-card__stat strong,
.distribution-card__value {
  font-family: var(--cp-font-display);
  font-size: clamp(22px, 3vw, 30px);
  line-height: 1.08;
}

.trend-card__stat span {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.trend-card__bars {
  min-height: 224px;
  display: grid;
  gap: 6px;
  align-items: end;
  overflow-x: auto;
  overflow-y: hidden;
  padding: 12px 2px 2px;
  scrollbar-width: thin;
}

.trend-card__bar-wrap {
  min-width: 14px;
  height: 204px;
  display: grid;
  grid-template-rows: minmax(120px, 1fr) 56px;
  gap: 10px;
  align-items: end;
}

.trend-card__bar {
  display: block;
  width: 100%;
  min-height: 10px;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(24, 38, 63, 0.22), rgba(197, 79, 45, 0.76));
}

.trend-card__tick {
  display: block;
  color: var(--cp-ink-faint);
  font-size: 11px;
  text-align: center;
  line-height: 1;
  writing-mode: vertical-rl;
  transform: rotate(180deg);
}

.distribution-card {
  display: grid;
  gap: 14px;
}

.distribution-card__top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: baseline;
}

.distribution-card__meter {
  height: 12px;
  border-radius: 999px;
  background: rgba(24, 38, 63, 0.08);
  overflow: hidden;
}

.distribution-card__fill {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, rgba(24, 38, 63, 0.9), rgba(197, 79, 45, 0.82));
}

.analytics-participants {
  margin-top: var(--cp-gap-4);
}

.personal-card {
  display: grid;
  gap: var(--cp-gap-4);
  min-height: 100%;
  align-content: center;
}

.personal-card__title {
  font-size: clamp(24px, 3vw, 34px);
}

.personal-ready {
  display: grid;
  gap: var(--cp-gap-4);
}

.personal-ready__scores {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cp-gap-3);
  margin-top: var(--cp-gap-4);
}

.personal-ready__score {
  padding: 12px 14px;
  border-radius: var(--cp-radius-md);
  border: 1px solid rgba(24, 38, 63, 0.12);
  background: rgba(255, 255, 255, 0.56);
  display: grid;
  gap: 6px;
}

.personal-ready__score span {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.personal-ready__score strong {
  font-family: var(--cp-font-display);
  font-size: 20px;
  letter-spacing: -0.02em;
}

.history-table {
  display: grid;
  gap: 8px;
}

.history-table__head,
.history-table__row {
  display: grid;
  grid-template-columns: 1.2fr 0.9fr repeat(3, 0.6fr);
  gap: var(--cp-gap-3);
  align-items: center;
}

.history-table__head {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
  padding-bottom: 10px;
  border-bottom: 1px solid var(--cp-line);
}

.history-table__row {
  padding: 8px 0;
  border-bottom: 1px solid rgba(24, 38, 63, 0.08);
}

@media (max-width: 1023px) {
  .analytics-hero,
  .trend-grid,
  .distribution-grid,
  .analytics-bottom-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 767px) {
  .period-switcher {
    width: 100%;
    justify-content: stretch;
  }

  .period-switcher__button {
    flex: 1 1 0;
  }
}
</style>
