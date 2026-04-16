<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { RouterLink } from "vue-router";
import { getJobs } from "../api/jobs.js";
import JobFilterBar from "../components/JobFilterBar.vue";
import JobPostingCard from "../components/JobPostingCard.vue";
import { useUserStore } from "../stores/user.js";

const userStore = useUserStore();

const loading = ref(false);
const errorMessage = ref("");
const filters = reactive({
  keyword: "",
  city: "",
  jobType: "",
  educationRequirement: "",
  sourcePlatform: "",
});
const summary = ref({
  total: 0,
  jobs: [],
});

const cityOptions = [
  { value: "", label: "All Cities" },
  { value: "Shenzhen", label: "Shenzhen" },
  { value: "Guangzhou", label: "Guangzhou" },
  { value: "Shanghai", label: "Shanghai" },
  { value: "Hangzhou", label: "Hangzhou" },
];

const jobTypeOptions = [
  { value: "", label: "All Types" },
  { value: "INTERNSHIP", label: "Internship" },
  { value: "FULL_TIME", label: "Full Time" },
  { value: "CAMPUS", label: "Campus" },
];

const educationOptions = [
  { value: "", label: "All Levels" },
  { value: "ANY", label: "Any" },
  { value: "BACHELOR", label: "Bachelor" },
  { value: "MASTER", label: "Master" },
  { value: "DOCTOR", label: "Doctor" },
];

const sourcePlatformOptions = [
  { value: "", label: "All Sources" },
  { value: "Official Site", label: "Official Site" },
  { value: "WeCom Channel", label: "WeCom Channel" },
  { value: "Internal Referral", label: "Internal Referral" },
];

const activeFilterLabels = computed(() => {
  const items = [];
  if (filters.keyword) items.push(`Keyword: ${filters.keyword}`);
  if (filters.city) items.push(`City: ${filters.city}`);
  if (filters.jobType) items.push(`Type: ${filters.jobType}`);
  if (filters.educationRequirement) items.push(`Education: ${filters.educationRequirement}`);
  if (filters.sourcePlatform) items.push(`Source: ${filters.sourcePlatform}`);
  return items;
});

function buildParams() {
  return Object.fromEntries(
    Object.entries(filters).filter(([, value]) => value),
  );
}

async function loadJobs() {
  loading.value = true;
  errorMessage.value = "";

  try {
    summary.value = await getJobs(buildParams());
  } catch (error) {
    errorMessage.value = error.message || "Jobs loading failed. Please try again.";
  } finally {
    loading.value = false;
  }
}

function updateFilters(nextFilters) {
  Object.assign(filters, nextFilters);
}

function resetFilters() {
  Object.assign(filters, {
    keyword: "",
    city: "",
    jobType: "",
    educationRequirement: "",
    sourcePlatform: "",
  });
  loadJobs();
}

onMounted(loadJobs);
</script>

<template>
  <section class="page-stack">
    <article class="section-card jobs-hero">
      <div class="jobs-hero__copy">
        <span class="section-eyebrow">Opportunity Desk</span>
        <h1 class="hero-title" style="margin-top: 18px;">Browse live job cards before you fragment your search again.</h1>
        <hr class="editorial-rule" />
        <p class="hero-copy">
          The jobs first slice is intentionally focused: curated cards, structured filters, a clean summary,
          and a direct jump back to the original source. No application workflow, no noisy ranking layer.
        </p>

        <div v-if="activeFilterLabels.length" class="chip-row" style="margin-top: 24px;">
          <span
            v-for="item in activeFilterLabels"
            :key="item"
            class="status-badge pending"
          >
            {{ item }}
          </span>
        </div>
      </div>

      <div class="jobs-hero__panel">
        <div class="panel-card jobs-hero__stat">
          <span class="jobs-hero__label">Published Cards</span>
          <strong>{{ summary.total }}</strong>
        </div>
        <div class="panel-card jobs-hero__stat">
          <span class="jobs-hero__label">Filter Fields</span>
          <strong>5</strong>
        </div>
        <RouterLink :to="userStore.isAuthenticated ? '/profile/favorites' : '/login'" class="app-btn">
          {{ userStore.isAuthenticated ? "Open My Favorites" : "Sign In To Save Jobs" }}
        </RouterLink>
      </div>
    </article>

    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">Filter Desk</span>
          <h2 class="page-title" style="margin-top: 16px;">Shape the shortlist before you leave the page.</h2>
        </div>
      </div>

      <JobFilterBar
        :filters="filters"
        :loading="loading"
        :city-options="cityOptions"
        :job-type-options="jobTypeOptions"
        :education-options="educationOptions"
        :source-platform-options="sourcePlatformOptions"
        @update:filters="updateFilters"
        @submit="loadJobs"
        @reset="resetFilters"
      />
    </article>

    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">Current Board</span>
          <h2 class="page-title" style="margin-top: 16px;">Jobs</h2>
          <p class="page-subtitle" style="margin-top: 16px;">
            Cards stay intentionally compact so you can compare city, requirement, source, and deadline without
            drilling into every row.
          </p>
        </div>
      </div>

      <div v-if="loading" class="empty-state">Loading job cards...</div>
      <div v-else-if="errorMessage" class="field-grid">
        <p class="field-error" role="alert">{{ errorMessage }}</p>
        <button type="button" class="ghost-btn" @click="loadJobs">
          Retry
        </button>
      </div>
      <div v-else-if="!summary.jobs.length" class="empty-state">
        No jobs matched the current filters. Reset and widen the search.
      </div>
      <div v-else class="jobs-grid">
        <JobPostingCard
          v-for="job in summary.jobs"
          :key="job.id"
          :job="job"
        />
      </div>
    </article>
  </section>
</template>

<style scoped>
.jobs-hero {
  display: grid;
  grid-template-columns: 1.25fr 0.75fr;
  gap: var(--cp-gap-6);
  background:
    linear-gradient(180deg, rgba(255, 250, 244, 0.92), rgba(251, 245, 237, 0.98)),
    radial-gradient(circle at top left, rgba(197, 79, 45, 0.12), transparent 28%),
    radial-gradient(circle at bottom right, rgba(76, 122, 116, 0.16), transparent 32%);
}

.jobs-hero__copy,
.jobs-hero__panel {
  display: grid;
  gap: var(--cp-gap-4);
}

.jobs-hero__panel {
  align-content: end;
}

.jobs-hero__stat {
  min-height: 132px;
  display: grid;
  gap: 8px;
  align-content: end;
}

.jobs-hero__label {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.jobs-hero__stat strong {
  font-size: clamp(26px, 4vw, 34px);
  font-family: var(--cp-font-display);
}

.jobs-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cp-gap-4);
}

@media (max-width: 1023px) {
  .jobs-hero,
  .jobs-grid {
    grid-template-columns: 1fr;
  }
}
</style>
