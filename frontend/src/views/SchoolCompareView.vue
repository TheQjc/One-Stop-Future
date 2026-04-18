<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { RouterLink } from "vue-router";
import { compareDecisionSchools, listDecisionSchools } from "../api/decision.js";

const supportedTracks = [
  { code: "EXAM", label: "Exam" },
  { code: "ABROAD", label: "Abroad" },
];

const track = ref("EXAM");
const keyword = ref("");

const loading = reactive({
  list: true,
  compare: false,
});
const error = reactive({
  list: "",
  compare: "",
  limit: "",
});

const candidates = ref([]);
const selectedIds = ref([]);

const compareResult = ref(null);

const selectedCount = computed(() => selectedIds.value.length);
const canCompare = computed(() => selectedCount.value >= 2 && selectedCount.value <= 4 && !loading.compare);

const chartSeries = computed(() => compareResult.value?.chartSeries ?? []);
const hasChartSeries = computed(() => chartSeries.value.length > 0);

function clearCompareState() {
  compareResult.value = null;
  error.compare = "";
  error.limit = "";
  loading.compare = false;
}

function resetSelection() {
  selectedIds.value = [];
  clearCompareState();
}

function toggleSelected(id) {
  if (loading.compare) {
    return;
  }

  const next = [...selectedIds.value];
  const index = next.indexOf(id);

  if (index >= 0) {
    next.splice(index, 1);
    selectedIds.value = next;
    error.limit = "";
    return;
  }

  if (next.length >= 4) {
    error.limit = "Select at most 4 schools.";
    return;
  }

  error.limit = "";
  next.push(id);
  selectedIds.value = next;
}

async function loadSchools() {
  loading.list = true;
  error.list = "";
  candidates.value = [];

  try {
    const response = await listDecisionSchools({
      track: track.value,
      ...(keyword.value.trim() ? { keyword: keyword.value.trim() } : {}),
    });
    candidates.value = response.schools || [];
  } catch (e) {
    error.list = e.message || "School candidates failed to load. Please retry.";
  } finally {
    loading.list = false;
  }
}

async function submitCompare() {
  if (!canCompare.value) {
    return;
  }

  loading.compare = true;
  error.compare = "";
  compareResult.value = null;

  try {
    compareResult.value = await compareDecisionSchools({ schoolIds: selectedIds.value });
  } catch (e) {
    error.compare = e.message || "Compare failed. Please retry.";
  } finally {
    loading.compare = false;
  }
}

function selectTrack(nextTrack) {
  if (loading.compare) {
    return;
  }
  if (track.value === nextTrack) {
    return;
  }
  track.value = nextTrack;
  keyword.value = "";
  resetSelection();
}

watch(track, () => {
  loadSchools();
});

onMounted(loadSchools);
</script>

<template>
  <section class="page-stack">
    <article class="section-card compare-hero">
      <div class="compare-hero__copy">
        <span class="section-eyebrow">Decision Desk</span>
        <h1 class="hero-title" style="margin-top: 18px;">School Compare</h1>
        <hr class="editorial-rule" />
        <p class="hero-copy">
          Pick 2 to 4 candidates, then render a deterministic comparison table plus a lightweight chart strip.
          Missing values are shown explicitly so the desk stays honest.
        </p>
      </div>

      <div class="compare-hero__panel">
        <div class="panel-card compare-hero__stat">
          <span class="compare-hero__label">Track</span>
          <strong>{{ track }}</strong>
        </div>
        <div class="panel-card compare-hero__stat">
          <span class="compare-hero__label">Selected</span>
          <strong>{{ selectedCount }}/4</strong>
        </div>
        <RouterLink to="/timeline" class="ghost-btn">Back to Timeline</RouterLink>
      </div>
    </article>

    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">Candidates</span>
          <h2 class="page-title" style="margin-top: 16px;">Search and select 2-4 schools.</h2>
          <p class="page-subtitle" style="margin-top: 16px;">
            Track switching resets selection to avoid mixed-domain comparisons.
          </p>
        </div>
      </div>

      <div class="compare-toolbar">
        <div class="track-tabs" role="tablist" aria-label="School track tabs">
          <button
            v-for="t in supportedTracks"
            :key="t.code"
            type="button"
            class="track-tab"
            :class="{ active: track === t.code }"
            role="tab"
            :aria-selected="track === t.code"
            :disabled="loading.compare"
            @click="selectTrack(t.code)"
          >
            {{ t.label }}
          </button>
        </div>

        <form class="search-row" @submit.prevent="loadSchools">
          <input
            v-model="keyword"
            name="keyword"
            class="field-control"
            placeholder="Search by name or region..."
            :disabled="loading.list"
          />
          <button type="submit" class="ghost-btn" :disabled="loading.list">
            {{ loading.list ? "Loading..." : "Search" }}
          </button>
          <button type="button" class="ghost-btn" :disabled="loading.list || loading.compare" @click="keyword = ''; loadSchools();">
            Reset
          </button>
        </form>
      </div>

      <div v-if="loading.list" class="empty-state">Loading school candidates...</div>
      <div v-else-if="error.list" class="field-grid">
        <p class="field-error" role="alert">{{ error.list }}</p>
        <button type="button" class="ghost-btn" @click="loadSchools">Retry</button>
      </div>
      <div v-else-if="!candidates.length" class="empty-state">
        No schools matched the current query. Reset and broaden the search.
      </div>
      <div v-else class="candidate-grid">
        <button
          v-for="school in candidates"
          :key="school.schoolId"
          type="button"
          class="candidate-card"
          :class="{ selected: selectedIds.includes(school.schoolId) }"
          :disabled="loading.compare"
          @click="toggleSelected(school.schoolId)"
        >
          <div class="candidate-card__top">
            <strong class="candidate-card__name">{{ school.name }}</strong>
            <span class="candidate-card__tier">{{ school.tierLabel }}</span>
          </div>
          <p class="candidate-card__meta">
            {{ school.region }} · {{ school.track }}
          </p>
        </button>
      </div>

      <div class="compare-actions">
        <button
          type="button"
          class="app-btn"
          data-test="compare-submit"
          :disabled="!canCompare"
          @click="submitCompare"
        >
          {{ loading.compare ? "Comparing..." : "Compare Selected" }}
        </button>
        <button type="button" class="ghost-btn" :disabled="loading.compare" @click="resetSelection">
          Clear Selection
        </button>
        <span v-if="error.limit" class="field-error">{{ error.limit }}</span>
        <span v-else-if="error.compare" class="field-error">{{ error.compare }}</span>
        <span v-else class="meta-copy">
          {{ selectedCount < 2 ? "Pick at least 2 schools to run the comparison." : "Ready to compare." }}
        </span>
      </div>
    </article>

    <article v-if="compareResult" class="section-card" data-test="compare-result">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">Comparison</span>
          <h2 class="page-title" style="margin-top: 16px;">Side-by-side table + charts.</h2>
          <p v-if="compareResult.highlightSummary" class="page-subtitle" style="margin-top: 16px;" data-test="highlight">
            {{ compareResult.highlightSummary }}
          </p>
        </div>
      </div>

      <div class="panel-card chart-panel" data-test="chart-panel">
        <p class="chart-panel__label">Chart Strip</p>
        <div v-if="!hasChartSeries" class="empty-state chart-empty" data-test="empty-chart">
          No chartable metrics are available for the selected schools yet.
        </div>
        <div v-else class="chart-grid">
          <section
            v-for="series in chartSeries"
            :key="series.metricCode"
            class="chart-card"
          >
            <header class="chart-card__header">
              <strong>{{ series.metricLabel }}</strong>
              <span class="meta-copy">{{ series.metricUnit || series.valueType }}</span>
            </header>
            <div class="chart-card__points">
              <div
                v-for="point in series.points"
                :key="point.schoolId"
                class="chart-point"
              >
                <span class="chart-point__name">{{ point.schoolName }}</span>
                <span class="chart-point__value" :class="{ missing: point.isMissing }">
                  {{ point.displayValue }}
                </span>
              </div>
            </div>
          </section>
        </div>
      </div>

      <div class="table-wrap">
        <table class="app-table compare-table">
          <thead>
            <tr>
              <th>Metric</th>
              <th
                v-for="school in compareResult.schools"
                :key="school.schoolId"
              >
                {{ school.name }}
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in compareResult.tableRows" :key="row.metricCode">
              <td>
                <strong>{{ row.metricLabel }}</strong>
                <div class="meta-copy">{{ row.metricUnit || row.valueType }}</div>
              </td>
              <td
                v-for="cell in row.cells"
                :key="`${row.metricCode}-${cell.schoolId}`"
                :class="{ missing: cell.isMissing }"
              >
                {{ cell.displayValue }}
              </td>
            </tr>
          </tbody>
        </table>

        <div class="mobile-table-cards">
          <article v-for="row in compareResult.tableRows" :key="row.metricCode" class="table-card">
            <strong>{{ row.metricLabel }}</strong>
            <p class="meta-copy">{{ row.metricUnit || row.valueType }}</p>
            <div class="mobile-row">
              <div
                v-for="cell in row.cells"
                :key="`${row.metricCode}-m-${cell.schoolId}`"
                class="mobile-row__cell"
              >
                <span class="meta-copy">{{ compareResult.schools.find((s) => s.schoolId === cell.schoolId)?.name }}</span>
                <span :class="{ missing: cell.isMissing }">{{ cell.displayValue }}</span>
              </div>
            </div>
          </article>
        </div>
      </div>
    </article>
  </section>
</template>

<style scoped>
.compare-hero {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: var(--cp-gap-6);
  background:
    linear-gradient(180deg, rgba(255, 252, 247, 0.94), rgba(245, 237, 223, 0.98)),
    radial-gradient(circle at top left, rgba(24, 38, 63, 0.1), transparent 34%),
    radial-gradient(circle at bottom right, rgba(76, 122, 116, 0.16), transparent 40%);
}

.compare-hero__copy,
.compare-hero__panel {
  display: grid;
  gap: var(--cp-gap-4);
}

.compare-hero__panel {
  align-content: end;
}

.compare-hero__stat {
  min-height: 132px;
  display: grid;
  gap: 8px;
  align-content: end;
}

.compare-hero__label {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.compare-hero__stat strong {
  font-size: clamp(22px, 3vw, 30px);
  font-family: var(--cp-font-display);
  overflow-wrap: anywhere;
}

.compare-toolbar {
  display: grid;
  gap: var(--cp-gap-4);
  margin-bottom: var(--cp-gap-6);
}

.track-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cp-gap-3);
}

.track-tab {
  min-height: var(--cp-touch-height);
  padding: 0 16px;
  border-radius: var(--cp-radius-pill);
  border: 1px solid var(--cp-line-strong);
  background: rgba(255, 255, 255, 0.72);
  color: var(--cp-ink-soft);
  cursor: pointer;
  transition:
    transform var(--cp-transition),
    border-color var(--cp-transition),
    color var(--cp-transition),
    background-color var(--cp-transition);
}

.track-tab:hover {
  transform: translateY(-1px);
  border-color: rgba(197, 79, 45, 0.3);
  color: var(--cp-ink);
}

.track-tab.active {
  background: rgba(255, 250, 245, 0.9);
  border-color: rgba(197, 79, 45, 0.6);
  color: var(--cp-accent-deep);
}

.search-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: var(--cp-gap-3);
  align-items: center;
}

.candidate-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cp-gap-4);
}

.candidate-card {
  text-align: left;
  padding: 18px;
  border-radius: var(--cp-radius-md);
  border: 1px solid rgba(24, 38, 63, 0.14);
  background: rgba(255, 255, 255, 0.72);
  cursor: pointer;
  display: grid;
  gap: 10px;
  transition:
    transform var(--cp-transition),
    border-color var(--cp-transition),
    box-shadow var(--cp-transition),
    background-color var(--cp-transition);
}

.candidate-card:hover {
  transform: translateY(-1px);
  border-color: rgba(197, 79, 45, 0.26);
}

.candidate-card.selected {
  background: rgba(255, 250, 245, 0.92);
  border-color: rgba(197, 79, 45, 0.6);
  box-shadow: 0 18px 36px rgba(197, 79, 45, 0.12);
}

.candidate-card__top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: baseline;
}

.candidate-card__name {
  font-family: var(--cp-font-display);
  font-size: 22px;
  line-height: 1.2;
}

.candidate-card__tier {
  font-size: var(--cp-text-sm);
  color: var(--cp-accent-deep);
  font-family: var(--cp-font-mono);
}

.candidate-card__meta {
  margin: 0;
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.compare-actions {
  margin-top: var(--cp-gap-6);
  display: flex;
  flex-wrap: wrap;
  gap: var(--cp-gap-3);
  align-items: center;
}

.chart-panel {
  margin-bottom: var(--cp-gap-6);
  display: grid;
  gap: var(--cp-gap-4);
}

.chart-panel__label {
  margin: 0;
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cp-gap-4);
}

.chart-card {
  border-radius: var(--cp-radius-md);
  border: 1px solid var(--cp-line);
  background: rgba(255, 255, 255, 0.72);
  padding: 16px;
  display: grid;
  gap: 12px;
}

.chart-card__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.chart-card__points {
  display: grid;
  gap: 10px;
}

.chart-point {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid rgba(24, 38, 63, 0.08);
  padding-bottom: 8px;
}

.chart-point:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.chart-point__name {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.chart-point__value {
  font-family: var(--cp-font-mono);
}

.missing {
  color: var(--cp-ink-faint);
}

.table-wrap {
  width: 100%;
  overflow-x: auto;
}

.compare-table td.missing {
  color: var(--cp-ink-faint);
}

.mobile-row {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.mobile-row__cell {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

@media (max-width: 1023px) {
  .compare-hero,
  .candidate-grid,
  .chart-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 767px) {
  .search-row {
    grid-template-columns: 1fr;
  }
}
</style>
