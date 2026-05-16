<script setup>
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { getSearchResults } from "../api/search.js";
import SearchResultCard from "../components/SearchResultCard.vue";
import HighlightText from "../components/HighlightText.vue";

const route = useRoute();
const router = useRouter();
const currentRoute = computed(() => router.currentRoute?.value || route);

const TYPE_OPTIONS = [
  { value: "ALL", label: "全部" },
  { value: "POST", label: "帖子" },
  { value: "JOB", label: "岗位" },
  { value: "RESOURCE", label: "资料" },
  { value: "RESUME", label: "简历" },
  { value: "NOTIFICATION", label: "通知" },
  { value: "APPLICATION", label: "投递" },
];

const SORT_OPTIONS = [
  { value: "RELEVANCE", label: "相关度" },
  { value: "LATEST", label: "最新" },
];

const searchInput = ref("");
const loading = ref(false);
const errorMessage = ref("");
const results = ref({
  query: "",
  type: "ALL",
  sort: "RELEVANCE",
  totals: { all: 0, post: 0, job: 0, resource: 0, resume: 0, notification: 0, application: 0 },
  results: [],
});

function createEmptyResults(state = normalizedRouteState.value) {
  return {
    query: state.q || "",
    type: state.type || "ALL",
    sort: state.sort || "RELEVANCE",
    totals: { all: 0, post: 0, job: 0, resource: 0, resume: 0, notification: 0, application: 0 },
    results: [],
  };
}

function sanitizeQuery(value) {
  return typeof value === "string" ? value.trim() : "";
}

function sanitizeEnum(value, allowedValues, fallback) {
  return allowedValues.includes(value) ? value : fallback;
}

const normalizedRouteState = computed(() => ({
  q: sanitizeQuery(currentRoute.value.query?.q),
  type: sanitizeEnum(currentRoute.value.query?.type, TYPE_OPTIONS.map((item) => item.value), "ALL"),
  sort: sanitizeEnum(currentRoute.value.query?.sort, SORT_OPTIONS.map((item) => item.value), "RELEVANCE"),
}));

const isGuidedEmptyState = computed(() => !normalizedRouteState.value.q);

const totalCards = computed(() => [
  { label: "全部", value: results.value.totals?.all ?? 0 },
  { label: "帖子", value: results.value.totals?.post ?? 0 },
  { label: "岗位", value: results.value.totals?.job ?? 0 },
  { label: "资料", value: results.value.totals?.resource ?? 0 },
  { label: "简历", value: results.value.totals?.resume ?? 0 },
  { label: "通知", value: results.value.totals?.notification ?? 0 },
  { label: "投递", value: results.value.totals?.application ?? 0 },
]);

const summaryLine = computed(() => {
  if (!results.value.query) {
    return "站内搜索会把公开帖子、岗位和资料集中到同一个结果页。";
  }

  return `当前共找到 ${results.value.totals?.all ?? results.value.results.length} 条与“${results.value.query}”相关的结果。`;
});

async function fetchResults() {
  if (isGuidedEmptyState.value) {
    loading.value = false;
    errorMessage.value = "";
    results.value = createEmptyResults();
    return;
  }

  loading.value = true;
  errorMessage.value = "";

  try {
    const data = await getSearchResults(normalizedRouteState.value);
    results.value = data || createEmptyResults();
  } catch (error) {
    errorMessage.value = error.message || "搜索结果加载失败，请稍后重试。";
    results.value = createEmptyResults();
  } finally {
    loading.value = false;
  }
}

async function syncRoute(nextState) {
  const currentState = normalizedRouteState.value;
  const merged = {
    q: sanitizeQuery(nextState.q ?? currentState.q),
    type: sanitizeEnum(nextState.type ?? currentState.type, TYPE_OPTIONS.map((item) => item.value), "ALL"),
    sort: sanitizeEnum(nextState.sort ?? currentState.sort, SORT_OPTIONS.map((item) => item.value), "RELEVANCE"),
  };

  const currentQuery = {
    q: currentState.q,
    type: currentState.type,
    sort: currentState.sort,
  };

  if (
    currentQuery.q === merged.q
    && currentQuery.type === merged.type
    && currentQuery.sort === merged.sort
  ) {
    return;
  }

  const navigate = typeof router.push === "function" ? router.push.bind(router) : router.replace.bind(router);

  await navigate({
    name: "search",
    query: {
      ...(merged.q ? { q: merged.q } : {}),
      type: merged.type,
      sort: merged.sort,
    },
  });
}

async function submitSearch() {
  await syncRoute({ q: searchInput.value });
}

async function selectType(type) {
  await syncRoute({ type });
}

async function selectSort(sort) {
  await syncRoute({ sort });
}

async function retrySearch() {
  await fetchResults();
}

watch(
  normalizedRouteState,
  (state) => {
    searchInput.value = state.q;
    fetchResults();
  },
  { immediate: true },
);
</script>

<template>
  <section class="page-stack">
    <article class="section-card search-hero">
      <div class="search-hero__copy">
        <span class="section-eyebrow">站内搜索</span>
        <h1 class="hero-title" style="margin-top: 18px;">一个关键词，集中查看帖子、岗位和资料。</h1>
        <hr class="editorial-rule" />
        <p class="hero-copy">
          搜索页会优先读取 URL 中的查询条件，刷新、前进后退和分享链接时都能保持同一组搜索结果。
        </p>
      </div>

      <form class="search-hero__form" data-test="search-form" @submit.prevent="submitSearch">
        <label class="field-label" for="unified-search-input">
          搜索关键词
          <input
            id="unified-search-input"
            v-model="searchInput"
            name="q"
            class="field-control search-hero__input"
            type="search"
            placeholder="搜索经验帖、岗位、院校、资料"
            autocomplete="off"
          />
        </label>

        <div class="inline-form-actions">
          <button type="submit" class="app-btn">
            搜索
          </button>
          <span class="meta-copy">当前 URL 会同步保存 `q`、`type` 和 `sort`。</span>
        </div>
      </form>
    </article>

    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">搜索筛选</span>
          <h2 class="page-title" style="margin-top: 16px;">不离开当前页面，继续缩小搜索范围</h2>
          <p class="page-subtitle" style="margin-top: 16px;">按内容类型和排序方式切换，当前查询会继续保留在 URL 中。</p>
        </div>
      </div>

      <div class="search-toolbar">
        <div class="field-grid">
          <span class="search-toolbar__label">内容类型</span>
          <div class="chip-row">
            <button
              v-for="option in TYPE_OPTIONS"
              :key="option.value"
              type="button"
              class="search-chip"
              :class="{ 'search-chip--active': normalizedRouteState.type === option.value }"
              @click="selectType(option.value)"
            >
              {{ option.label }}
            </button>
          </div>
        </div>

        <div class="field-grid">
          <span class="search-toolbar__label">排序方式</span>
          <div class="chip-row">
            <button
              v-for="option in SORT_OPTIONS"
              :key="option.value"
              type="button"
              class="search-chip"
              :class="{ 'search-chip--active': normalizedRouteState.sort === option.value }"
              @click="selectSort(option.value)"
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
          <span class="section-eyebrow">搜索概览</span>
          <h2 class="page-title" style="margin-top: 16px;">结果快照</h2>
          <p class="page-subtitle" style="margin-top: 16px;">{{ summaryLine }}</p>
        </div>
      </div>

      <div class="stats-grid search-stats">
        <article
          v-for="card in totalCards"
          :key="card.label"
          class="panel-card search-stats__card"
        >
          <p class="search-stats__label">{{ card.label }}</p>
          <strong class="search-stats__value">{{ card.value }}</strong>
        </article>
      </div>
    </article>

    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">搜索结果</span>
          <h2 class="page-title" style="margin-top: 16px;">相关内容</h2>
        </div>
      </div>

      <div v-if="isGuidedEmptyState" class="empty-state search-empty">
        <strong>先输入关键词</strong>
        <p class="meta-copy">
          可以先试试岗位、城市、院校、话题或资料名称。输入关键词后，页面才会开始整理搜索结果。
        </p>
      </div>
      <div v-else-if="loading" class="empty-state">正在整理搜索结果...</div>
      <div v-else-if="errorMessage" class="field-grid">
        <p class="field-error" role="alert">{{ errorMessage }}</p>
        <button type="button" class="ghost-btn" @click="retrySearch">
          重试
        </button>
      </div>
      <div v-else-if="!results.results.length" class="empty-state search-empty">
        <strong>暂未找到相关结果</strong>
        <p class="meta-copy">
          可以尝试扩大关键词、切换回“全部”，或改成“最新”优先查看近期内容。
        </p>
      </div>
      <div v-else class="search-results-grid">
        <SearchResultCard
          v-for="item in results.results"
          :key="`${item.type}-${item.id}`"
          :item="item"
        />
      </div>
    </article>
  </section>
</template>

<style scoped>
.search-hero {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: var(--cp-gap-6);
  background:
    linear-gradient(180deg, rgba(255, 252, 247, 0.94), rgba(245, 237, 223, 0.98)),
    radial-gradient(circle at top left, rgba(184, 130, 35, 0.18), transparent 28%),
    radial-gradient(circle at bottom right, rgba(24, 38, 63, 0.1), transparent 34%);
}

.search-hero__copy,
.search-hero__form {
  display: grid;
  gap: var(--cp-gap-4);
}

.search-hero__form {
  align-content: end;
}

.search-hero__input {
  min-height: 56px;
}

.search-toolbar {
  display: grid;
  gap: var(--cp-gap-5);
}

.search-toolbar__label {
  color: var(--cp-ink);
  font-weight: 600;
}

.search-chip {
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

.search-chip:hover {
  transform: translateY(-1px);
  border-color: rgba(197, 79, 45, 0.3);
}

.search-chip:focus-visible {
  outline: none;
  box-shadow: var(--cp-shadow-focus);
}

.search-chip--active {
  background: var(--cp-ink);
  color: #fff9f1;
  border-color: var(--cp-ink);
}

.search-stats {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

@media (max-width: 1199px) {
  .search-stats {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

.search-stats__card {
  min-height: 132px;
  display: grid;
  gap: 8px;
  align-content: end;
}

.search-stats__label {
  margin: 0;
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.search-stats__value {
  font-size: clamp(26px, 4vw, 34px);
  font-family: var(--cp-font-display);
}

.search-results-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cp-gap-4);
}

.search-empty {
  display: grid;
  gap: 8px;
}

@media (max-width: 1023px) {
  .search-hero,
  .search-results-grid,
  .search-stats {
    grid-template-columns: 1fr;
  }
}
</style>
