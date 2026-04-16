<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { RouterLink } from "vue-router";
import { getResources } from "../api/resources.js";
import ResourceCard from "../components/ResourceCard.vue";
import ResourceFilterBar from "../components/ResourceFilterBar.vue";
import { useUserStore } from "../stores/user.js";

const userStore = useUserStore();

const loading = ref(false);
const errorMessage = ref("");
const filters = reactive({
  keyword: "",
  category: "",
});
const summary = ref({
  total: 0,
  resources: [],
});

const categoryOptions = [
  { value: "", label: "All Categories" },
  { value: "EXAM_PAPER", label: "Exam Paper" },
  { value: "LANGUAGE_TEST", label: "Language Test" },
  { value: "RESUME_TEMPLATE", label: "Resume Template" },
  { value: "INTERVIEW_EXPERIENCE", label: "Interview Notes" },
  { value: "OTHER", label: "Other" },
];

const activeFilterLabels = computed(() => {
  const items = [];
  if (filters.keyword) items.push(`Keyword: ${filters.keyword}`);
  if (filters.category) items.push(`Category: ${filters.category}`);
  return items;
});

const uploadTarget = computed(() => (
  userStore.isAuthenticated
    ? "/resources/upload"
    : { name: "login", query: { redirect: "/resources/upload" } }
));

function buildParams() {
  return Object.fromEntries(
    Object.entries(filters).filter(([, value]) => value),
  );
}

async function loadResources() {
  loading.value = true;
  errorMessage.value = "";

  try {
    summary.value = await getResources(buildParams());
  } catch (error) {
    errorMessage.value = error.message || "Resource library loading failed. Please try again.";
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
    category: "",
  });
  loadResources();
}

onMounted(loadResources);
</script>

<template>
  <section class="page-stack">
    <article class="section-card archive-hero">
      <div class="archive-hero__copy">
        <span class="section-eyebrow">Archive Desk</span>
        <h1 class="hero-title" style="margin-top: 18px;">Resources</h1>
        <hr class="editorial-rule" />
        <p class="hero-copy">
          Treat the library like a working table, not a dump. Published files stay browseable,
          category filters stay narrow, and every card keeps the summary, uploader, file weight,
          and download signal visible before you open the detail page.
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

      <div class="archive-hero__panel">
        <div class="panel-card archive-hero__stat">
          <span class="archive-hero__label">Published Files</span>
          <strong>{{ summary.total }}</strong>
        </div>
        <div class="panel-card archive-hero__stat">
          <span class="archive-hero__label">Download Access</span>
          <strong>{{ userStore.isAuthenticated ? "On" : "Login" }}</strong>
        </div>
        <RouterLink :to="uploadTarget" class="app-btn">
          {{ userStore.isAuthenticated ? "Upload Resource" : "Sign In To Upload" }}
        </RouterLink>
      </div>
    </article>

    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">Filter Desk</span>
          <h2 class="page-title" style="margin-top: 16px;">Slice the archive before opening files.</h2>
        </div>
      </div>

      <ResourceFilterBar
        :filters="filters"
        :loading="loading"
        :category-options="categoryOptions"
        @update:filters="updateFilters"
        @submit="loadResources"
        @reset="resetFilters"
      />
    </article>

    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">Published Shelf</span>
          <h2 class="page-title" style="margin-top: 16px;">Current Archive</h2>
          <p class="page-subtitle" style="margin-top: 16px;">
            Cards stay intentionally scannable so you can compare category, file weight,
            publisher, and download heat without leaving the grid.
          </p>
        </div>
      </div>

      <div v-if="loading" class="empty-state">Loading published resources...</div>
      <div v-else-if="errorMessage" class="field-grid">
        <p class="field-error" role="alert">{{ errorMessage }}</p>
        <button type="button" class="ghost-btn" @click="loadResources">
          Retry
        </button>
      </div>
      <div v-else-if="!summary.resources.length" class="empty-state">
        No resources matched the current filters. Reset and widen the archive.
      </div>
      <div v-else class="resources-grid">
        <ResourceCard
          v-for="resource in summary.resources"
          :key="resource.id"
          :resource="resource"
        />
      </div>
    </article>
  </section>
</template>

<style scoped>
.archive-hero {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: var(--cp-gap-6);
  background:
    linear-gradient(180deg, rgba(255, 252, 247, 0.94), rgba(245, 237, 223, 0.98)),
    radial-gradient(circle at top left, rgba(184, 130, 35, 0.2), transparent 30%),
    radial-gradient(circle at bottom right, rgba(76, 122, 116, 0.16), transparent 34%);
}

.archive-hero__copy,
.archive-hero__panel {
  display: grid;
  gap: var(--cp-gap-4);
}

.archive-hero__panel {
  align-content: end;
}

.archive-hero__stat {
  min-height: 132px;
  display: grid;
  gap: 8px;
  align-content: end;
}

.archive-hero__label {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.archive-hero__stat strong {
  font-size: clamp(26px, 4vw, 34px);
  font-family: var(--cp-font-display);
}

.resources-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cp-gap-4);
}

@media (max-width: 1023px) {
  .archive-hero,
  .resources-grid {
    grid-template-columns: 1fr;
  }
}
</style>
