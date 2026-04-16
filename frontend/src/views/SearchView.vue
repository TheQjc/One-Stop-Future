<script setup>
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { getSearchResults } from "../api/search.js";
import SearchResultCard from "../components/SearchResultCard.vue";

const route = useRoute();
const router = useRouter();
const currentRoute = computed(() => router.currentRoute?.value || route);

const TYPE_OPTIONS = [
  { value: "ALL", label: "All" },
  { value: "POST", label: "Posts" },
  { value: "JOB", label: "Jobs" },
  { value: "RESOURCE", label: "Resources" },
];

const SORT_OPTIONS = [
  { value: "RELEVANCE", label: "Relevance" },
  { value: "LATEST", label: "Latest" },
];

const searchInput = ref("");
const loading = ref(false);
const errorMessage = ref("");
const results = ref({
  query: "",
  type: "ALL",
  sort: "RELEVANCE",
  totals: { all: 0, post: 0, job: 0, resource: 0 },
  results: [],
});

function createEmptyResults(state = normalizedRouteState.value) {
  return {
    query: state.q || "",
    type: state.type || "ALL",
    sort: state.sort || "RELEVANCE",
    totals: { all: 0, post: 0, job: 0, resource: 0 },
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
  { label: "All", value: results.value.totals?.all ?? 0 },
  { label: "Posts", value: results.value.totals?.post ?? 0 },
  { label: "Jobs", value: results.value.totals?.job ?? 0 },
  { label: "Resources", value: results.value.totals?.resource ?? 0 },
]);

const summaryLine = computed(() => {
  if (!results.value.query) {
    return "Unified search keeps published posts, jobs, and resources on one page.";
  }

  return `Showing ${results.value.results.length} result${results.value.results.length === 1 ? "" : "s"} for "${results.value.query}".`;
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
    errorMessage.value = error.message || "Search loading failed. Please try again.";
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
        <span class="section-eyebrow">Unified Search</span>
        <h1 class="hero-title" style="margin-top: 18px;">One query, three public desks, one shareable URL.</h1>
        <hr class="editorial-rule" />
        <p class="hero-copy">
          Search reads from the URL first so refresh, back-forward navigation, and shared links all hold the same published result state.
        </p>
      </div>

      <form class="search-hero__form" data-test="search-form" @submit.prevent="submitSearch">
        <label class="field-label" for="unified-search-input">
          Search Keyword
          <input
            id="unified-search-input"
            v-model="searchInput"
            name="q"
            class="field-control search-hero__input"
            type="search"
            placeholder="Search posts, jobs, and resources"
            autocomplete="off"
          />
        </label>

        <div class="inline-form-actions">
          <button type="submit" class="app-btn">
            Search
          </button>
          <span class="meta-copy">Current URL keeps `q`, `type`, and `sort` in sync.</span>
        </div>
      </form>
    </article>

    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">Search Controls</span>
          <h2 class="page-title" style="margin-top: 16px;">Refine the same query without leaving the page.</h2>
          <p class="page-subtitle" style="margin-top: 16px;">Switch content type and sort order directly from the URL-backed controls.</p>
        </div>
      </div>

      <div class="search-toolbar">
        <div class="field-grid">
          <span class="search-toolbar__label">Type</span>
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
          <span class="search-toolbar__label">Sort</span>
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
          <span class="section-eyebrow">Search Totals</span>
          <h2 class="page-title" style="margin-top: 16px;">Result snapshot</h2>
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
          <span class="section-eyebrow">Published Matches</span>
          <h2 class="page-title" style="margin-top: 16px;">Unified results</h2>
        </div>
      </div>

      <div v-if="isGuidedEmptyState" class="empty-state search-empty">
        <strong>Start with a keyword</strong>
        <p class="meta-copy">
          Try a role, company, topic, city, or resource phrase. Search will stay idle until the query is non-blank.
        </p>
      </div>
      <div v-else-if="loading" class="empty-state">Loading unified search results...</div>
      <div v-else-if="errorMessage" class="field-grid">
        <p class="field-error" role="alert">{{ errorMessage }}</p>
        <button type="button" class="ghost-btn" @click="retrySearch">
          Retry
        </button>
      </div>
      <div v-else-if="!results.results.length" class="empty-state search-empty">
        <strong>No published matches</strong>
        <p class="meta-copy">
          Widen the keyword, switch back to All, or change the sort to check newer content first.
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
