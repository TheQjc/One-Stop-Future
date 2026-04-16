<script setup>
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { getDiscoverResults } from "../api/discover.js";
import DiscoverItemCard from "../components/DiscoverItemCard.vue";

const route = useRoute();
const router = useRouter();
const currentRoute = computed(() => router.currentRoute?.value || route);

const TAB_OPTIONS = [
  { value: "ALL", label: "All" },
  { value: "POST", label: "Posts" },
  { value: "JOB", label: "Jobs" },
  { value: "RESOURCE", label: "Resources" },
];

const PERIOD_OPTIONS = [
  { value: "WEEK", label: "This Week" },
  { value: "ALL", label: "All Time" },
];

const loading = ref(false);
const errorMessage = ref("");
const board = ref({
  tab: "ALL",
  period: "WEEK",
  total: 0,
  items: [],
});

function sanitizeEnum(value, allowedValues, fallback) {
  return allowedValues.includes(value) ? value : fallback;
}

const normalizedRouteState = computed(() => ({
  tab: sanitizeEnum(currentRoute.value.query?.tab, TAB_OPTIONS.map((item) => item.value), "ALL"),
  period: sanitizeEnum(currentRoute.value.query?.period, PERIOD_OPTIONS.map((item) => item.value), "WEEK"),
}));

const summaryLine = computed(() => {
  const label = normalizedRouteState.value.period === "WEEK" ? "weekly board" : "all-time board";
  return `${board.value.total} public item${board.value.total === 1 ? "" : "s"} currently sit on the ${label}.`;
});

async function fetchBoard() {
  loading.value = true;
  errorMessage.value = "";

  try {
    const data = await getDiscoverResults({
      tab: normalizedRouteState.value.tab,
      period: normalizedRouteState.value.period,
      limit: 20,
    });
    board.value = data || {
      tab: normalizedRouteState.value.tab,
      period: normalizedRouteState.value.period,
      total: 0,
      items: [],
    };
  } catch (error) {
    errorMessage.value = error.message || "Discover loading failed. Please try again.";
    board.value = {
      tab: normalizedRouteState.value.tab,
      period: normalizedRouteState.value.period,
      total: 0,
      items: [],
    };
  } finally {
    loading.value = false;
  }
}

async function syncRoute(nextState) {
  const currentState = normalizedRouteState.value;
  const merged = {
    tab: sanitizeEnum(nextState.tab ?? currentState.tab, TAB_OPTIONS.map((item) => item.value), "ALL"),
    period: sanitizeEnum(nextState.period ?? currentState.period, PERIOD_OPTIONS.map((item) => item.value), "WEEK"),
  };

  if (currentState.tab === merged.tab && currentState.period === merged.period) {
    return;
  }

  const navigate = typeof router.push === "function" ? router.push.bind(router) : router.replace.bind(router);

  await navigate({
    name: "discover",
    query: {
      tab: merged.tab,
      period: merged.period,
    },
  });
}

async function selectTab(tab) {
  await syncRoute({ tab });
}

async function selectPeriod(period) {
  await syncRoute({ period });
}

async function retryDiscover() {
  await fetchBoard();
}

watch(
  normalizedRouteState,
  () => {
    fetchBoard();
  },
  { immediate: true },
);
</script>

<template>
  <section class="page-stack">
    <article class="section-card discover-hero">
      <div class="discover-hero__copy">
        <span class="section-eyebrow">Discover Board</span>
        <h1 class="hero-title" style="margin-top: 18px;">Move from search mode to signal mode.</h1>
        <hr class="editorial-rule" />
        <p class="hero-copy">
          Discover keeps public posts, jobs, and resources on one board so students can spot what is gaining momentum before they know the exact query.
        </p>
      </div>

      <div class="discover-hero__aside">
        <p class="discover-hero__metric-label">Current board</p>
        <strong class="discover-hero__metric-value">{{ board.total }}</strong>
        <p class="meta-copy">{{ summaryLine }}</p>
      </div>
    </article>

    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">Board Controls</span>
          <h2 class="page-title" style="margin-top: 16px;">Switch period and desk without losing the URL.</h2>
          <p class="page-subtitle" style="margin-top: 16px;">
            `/discover` keeps `tab` and `period` in sync with the route so refresh and share behave predictably.
          </p>
        </div>
      </div>

      <div class="discover-toolbar">
        <div class="field-grid">
          <span class="discover-toolbar__label">Period</span>
          <div class="chip-row">
            <button
              v-for="option in PERIOD_OPTIONS"
              :key="option.value"
              type="button"
              class="discover-chip"
              :class="{ 'discover-chip--active': normalizedRouteState.period === option.value }"
              @click="selectPeriod(option.value)"
            >
              {{ option.label }}
            </button>
          </div>
        </div>

        <div class="field-grid">
          <span class="discover-toolbar__label">Desk</span>
          <div class="chip-row">
            <button
              v-for="option in TAB_OPTIONS"
              :key="option.value"
              type="button"
              class="discover-chip"
              :class="{ 'discover-chip--active': normalizedRouteState.tab === option.value }"
              @click="selectTab(option.value)"
            >
              {{ option.label }}
            </button>
          </div>
        </div>
      </div>
    </article>

    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">Ranked Public Picks</span>
          <h2 class="page-title" style="margin-top: 16px;">Discover results</h2>
        </div>
      </div>

      <div v-if="loading" class="empty-state">Loading discover board...</div>
      <div v-else-if="errorMessage" class="field-grid">
        <p class="field-error" role="alert">{{ errorMessage }}</p>
        <button type="button" class="ghost-btn" @click="retryDiscover">
          Retry
        </button>
      </div>
      <div v-else-if="!board.items.length" class="empty-state discover-empty">
        <strong>Board waiting for momentum</strong>
        <p class="meta-copy">No public items have entered this board yet.</p>
      </div>
      <div v-else class="discover-results-grid">
        <DiscoverItemCard
          v-for="item in board.items"
          :key="`${item.type}-${item.id}`"
          :item="item"
        />
      </div>
    </article>
  </section>
</template>

<style scoped>
.discover-hero {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: var(--cp-gap-6);
  background:
    linear-gradient(180deg, rgba(255, 252, 247, 0.94), rgba(245, 237, 223, 0.98)),
    radial-gradient(circle at top left, rgba(184, 130, 35, 0.18), transparent 28%),
    radial-gradient(circle at bottom right, rgba(24, 38, 63, 0.1), transparent 34%);
}

.discover-hero__copy,
.discover-hero__aside {
  display: grid;
  gap: var(--cp-gap-4);
}

.discover-hero__aside {
  align-content: end;
}

.discover-hero__metric-label {
  margin: 0;
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.discover-hero__metric-value {
  font-family: var(--cp-font-display);
  font-size: clamp(54px, 10vw, 88px);
  line-height: 0.92;
}

.discover-toolbar {
  display: grid;
  gap: var(--cp-gap-5);
}

.discover-toolbar__label {
  color: var(--cp-ink);
  font-weight: 600;
}

.discover-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: var(--cp-touch-height);
  padding: 0 16px;
  border: 1px solid var(--cp-line-strong);
  border-radius: var(--cp-radius-pill);
  background: rgba(255, 255, 255, 0.78);
  color: var(--cp-ink-soft);
  cursor: pointer;
  transition:
    transform var(--cp-transition),
    border-color var(--cp-transition),
    background-color var(--cp-transition),
    color var(--cp-transition);
}

.discover-chip:hover {
  transform: translateY(-1px);
  border-color: rgba(197, 79, 45, 0.3);
}

.discover-chip:focus-visible {
  outline: none;
  box-shadow: var(--cp-shadow-focus);
}

.discover-chip--active {
  background: var(--cp-ink);
  color: #fff9f1;
  border-color: var(--cp-ink);
}

.discover-results-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cp-gap-4);
}

.discover-empty {
  display: grid;
  gap: 8px;
}

@media (max-width: 1023px) {
  .discover-hero,
  .discover-results-grid {
    grid-template-columns: 1fr;
  }
}
</style>
