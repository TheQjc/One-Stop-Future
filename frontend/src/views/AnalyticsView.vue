<script setup>
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import { getAnalyticsSummary } from "../api/analytics.js";

const supportedPeriods = [
  { code: "7D", label: "Last 7 days" },
  { code: "30D", label: "Last 30 days" },
];

const trendDefinitions = [
  {
    key: "posts",
    label: "Published Posts",
    eyebrow: "Community",
    tone: "posts",
  },
  {
    key: "jobs",
    label: "Active Jobs",
    eyebrow: "Career",
    tone: "jobs",
  },
  {
    key: "resources",
    label: "Published Resources",
    eyebrow: "Resources",
    tone: "resources",
  },
  {
    key: "assessments",
    label: "Assessment Sessions",
    eyebrow: "Decision",
    tone: "assessments",
  },
];

const trackLabelMap = {
  CAREER: "Career",
  EXAM: "Exam",
  ABROAD: "Abroad",
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
    label: "Published posts",
    value: summary.value.publicOverview?.publishedPostCount ?? 0,
    hint: "Public community notes now visible across the platform.",
  },
  {
    label: "Active jobs",
    value: summary.value.publicOverview?.activeJobCount ?? 0,
    hint: "Current live roles under the public listing rules.",
  },
  {
    label: "Published resources",
    value: summary.value.publicOverview?.publishedResourceCount ?? 0,
    hint: "Study and preparation resources in the public desk.",
  },
  {
    label: "Assessment sessions",
    value: summary.value.publicOverview?.assessmentSessionCount ?? 0,
    hint: "Saved decision sessions across all participants.",
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
const selectedPeriodLabel = computed(() => (
  supportedPeriods.find((item) => item.code === period.value)?.label || "Last 30 days"
));

function formatShortDate(value) {
  if (!value) {
    return "";
  }

  const date = new Date(`${value}T00:00:00`);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("en-US", {
    month: "short",
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
    errorMessage.value = error.message || "Analytics summary failed to load. Please retry.";
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

onMounted(() => {
  loadSummary("30D");
});
</script>

<template>
  <section class="page-stack">
    <article class="section-card analytics-hero">
      <div class="analytics-hero__copy">
        <span class="section-eyebrow">Decision Desk</span>
        <h1 class="hero-title" style="margin-top: 18px;">Analytics</h1>
        <hr class="editorial-rule" />
        <p class="hero-copy">
          A public reading of how the platform is moving right now: content supply, assessment
          activity, and the current direction mix across career, exam, and abroad.
        </p>
        <div class="action-row analytics-hero__actions">
          <RouterLink to="/" class="ghost-btn">Back Home</RouterLink>
          <RouterLink to="/assessment" class="app-link">Open Assessment</RouterLink>
        </div>
      </div>

      <div class="analytics-hero__panel">
        <div v-if="loading" class="empty-state">Loading analytics summary...</div>
        <div v-else-if="errorMessage" class="field-grid">
          <p class="field-error" role="alert">{{ errorMessage }}</p>
          <button type="button" class="ghost-btn" @click="loadSummary(period)">
            Retry
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
          <span class="section-eyebrow">Public Trends</span>
          <h2 class="page-title" style="margin-top: 16px;">Platform activity over {{ selectedPeriodLabel.toLowerCase() }}.</h2>
          <p class="page-subtitle" style="margin-top: 16px;">
            Period switching always refetches backend-owned analytics. The desk does not fake the filter in the client.
          </p>
        </div>
        <div class="period-switcher" role="tablist" aria-label="Analytics period tabs">
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
            {{ item.code }}
          </button>
        </div>
      </div>

      <div v-if="loading" class="empty-state">Loading trend lines...</div>
      <div v-else-if="errorMessage" class="empty-state">
        Trend data will reappear after the summary reloads.
      </div>
      <div v-else class="field-grid">
        <div v-if="!hasTrendActivity" class="empty-state analytics-empty">
          No public activity was recorded for this period yet. Zero-filled buckets are still shown below.
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
                <span>{{ card.activeDays }} active days</span>
              </div>
            </div>
            <div class="trend-card__bars" :aria-label="`${card.label} spark bars`">
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
            <span class="section-eyebrow">Decision Mix</span>
            <h2 class="page-title" style="margin-top: 16px;">Current recommendation mix across the latest saved sessions.</h2>
            <p class="page-subtitle" style="margin-top: 16px;">
              Each participant contributes only the latest saved assessment result, which keeps retakes from distorting the public board.
            </p>
          </div>
        </div>

        <div v-if="loading" class="empty-state">Loading decision mix...</div>
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
              {{ track.count }} people in the latest-session mix
            </p>
          </article>
        </div>

        <p class="meta-copy analytics-participants">
          Participant base: {{ summary.decisionDistribution?.participantCount ?? 0 }}
        </p>
      </article>

      <article class="section-card">
        <div class="section-header">
          <div>
            <span class="section-eyebrow">Personal Desk</span>
            <h2 class="page-title" style="margin-top: 16px;">Your path analysis sits beside the public board.</h2>
            <p class="page-subtitle" style="margin-top: 16px;">
              This panel stays guest-safe and turns into a personal desk once you log in and run the assessment.
            </p>
          </div>
        </div>

        <div v-if="loading" class="empty-state">Loading personal desk...</div>
        <div v-else-if="personalStatus === 'ANONYMOUS'" class="panel-card personal-card">
          <p class="personal-card__eyebrow">Guest mode</p>
          <h3 class="personal-card__title">Log in to unlock your personal path analysis.</h3>
          <p class="meta-copy">
            The public board remains open to everyone. Sign in when you want a personal recommendation, recent assessment history,
            and guided next steps.
          </p>
          <div class="action-row">
            <RouterLink to="/login" class="app-btn">Log In</RouterLink>
            <RouterLink to="/register" class="ghost-btn">Create Account</RouterLink>
          </div>
        </div>
        <div v-else class="empty-state">
          Personal analytics will populate here for authenticated viewers.
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
  min-height: 168px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(12px, 1fr));
  gap: 6px;
  align-items: end;
}

.trend-card__bar-wrap {
  min-height: 168px;
  display: grid;
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
