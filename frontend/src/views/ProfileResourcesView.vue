<script setup>
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import { getMyResources, previewResource } from "../api/resources.js";

const loading = ref(true);
const errorMessage = ref("");
const actionError = ref("");
const actionMessage = ref("");
const actionLoadingId = ref("");
const summary = ref({
  total: 0,
  resources: [],
});

const statusCards = computed(() => {
  const resources = summary.value.resources || [];

  return [
    { label: "All Records", value: resources.length },
    { label: "Pending", value: resources.filter((item) => item.status === "PENDING").length },
    { label: "Published", value: resources.filter((item) => item.status === "PUBLISHED").length },
    {
      label: "Needs Attention",
      value: resources.filter((item) => ["REJECTED", "OFFLINE"].includes(item.status)).length,
    },
  ];
});

function formatCategory(category) {
  const labelMap = {
    EXAM_PAPER: "Exam Paper",
    LANGUAGE_TEST: "Language Test",
    RESUME_TEMPLATE: "Resume Template",
    INTERVIEW_EXPERIENCE: "Interview Notes",
    OTHER: "Other",
  };

  return labelMap[category] || category || "Resource";
}

function formatTime(value) {
  if (!value) {
    return "Not published yet";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "Not published yet";
  }

  return new Intl.DateTimeFormat("zh-CN", {
    month: "numeric",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

function formatSize(value) {
  const size = Number(value || 0);
  if (!size) {
    return "Unknown size";
  }
  if (size >= 1024 * 1024) {
    return `${(size / (1024 * 1024)).toFixed(1)} MB`;
  }
  if (size >= 1024) {
    return `${Math.round(size / 1024)} KB`;
  }
  return `${size} B`;
}

function statusClass(status) {
  if (status === "PUBLISHED") {
    return "approved";
  }
  if (status === "REJECTED") {
    return "rejected";
  }
  return "pending";
}

async function loadResources() {
  loading.value = true;
  errorMessage.value = "";

  try {
    summary.value = await getMyResources();
  } catch (error) {
    errorMessage.value = error.message || "Resource records loading failed. Please try again.";
  } finally {
    loading.value = false;
  }
}

async function handlePreview(resource) {
  actionError.value = "";
  actionMessage.value = "";
  actionLoadingId.value = `preview-${resource.id}`;

  try {
    await previewResource(resource.id);
    actionMessage.value = `Preview opened for ${resource.title}.`;
  } catch (error) {
    actionError.value = error.message || "Preview failed. Please try again.";
  } finally {
    actionLoadingId.value = "";
  }
}

onMounted(loadResources);
</script>

<template>
  <section class="page-stack">
    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">My Resources</span>
          <h1 class="page-title" style="margin-top: 16px;">Resource records</h1>
          <p class="page-subtitle" style="margin-top: 16px;">
            This board is read-only on purpose. It keeps status, publish time, and rejection notes
            visible without turning the first resource slice into an edit workflow.
          </p>
        </div>
        <RouterLink to="/resources/upload" class="app-btn">
          Upload Resource
        </RouterLink>
      </div>

      <div class="stats-grid profile-resource-stats">
        <article
          v-for="card in statusCards"
          :key="card.label"
          class="panel-card profile-resource-stat"
        >
          <span class="profile-resource-stat__label">{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
        </article>
      </div>
    </article>

    <article class="section-card">
      <div v-if="loading" class="empty-state">Loading your resource records...</div>
      <div v-else-if="errorMessage" class="field-grid">
        <p class="field-error" role="alert">{{ errorMessage }}</p>
        <button type="button" class="ghost-btn" @click="loadResources">
          Retry
        </button>
      </div>
      <div v-else-if="!summary.resources.length" class="empty-state">
        You have not uploaded any resource files yet.
      </div>
      <div v-else class="resource-record-list">
        <p v-if="actionMessage" class="field-hint">{{ actionMessage }}</p>
        <p v-if="actionError" class="field-error" role="alert">{{ actionError }}</p>

        <article
          v-for="resource in summary.resources"
          :key="resource.id"
          class="panel-card resource-record-card"
        >
          <div class="resource-record-card__header">
            <div>
              <p class="resource-record-card__eyebrow">Resource #{{ resource.id }}</p>
              <h2 class="resource-record-card__title">{{ resource.title }}</h2>
            </div>
            <span class="status-badge" :class="statusClass(resource.status)">
              {{ resource.status }}
            </span>
          </div>

          <div class="resource-record-card__meta">
            <span>{{ formatCategory(resource.category) }}</span>
            <span>{{ resource.fileName || "Archive file" }}</span>
            <span>{{ formatSize(resource.fileSize) }}</span>
            <span>Created {{ formatTime(resource.createdAt) }}</span>
            <span>Published {{ formatTime(resource.publishedAt) }}</span>
          </div>

          <p class="meta-copy">
            {{ resource.summary || "No summary available for this record." }}
          </p>

          <div
            v-if="resource.editable || resource.previewAvailable"
            class="inline-form-actions resource-record-card__actions"
          >
            <RouterLink
              v-if="resource.editable"
              :to="`/resources/${resource.id}/edit`"
              class="app-link"
            >
              Edit And Resubmit
            </RouterLink>
            <button
              v-if="resource.previewAvailable"
              type="button"
              class="ghost-btn preview-action"
              :disabled="actionLoadingId === `preview-${resource.id}`"
              @click="handlePreview(resource)"
            >
              {{ actionLoadingId === `preview-${resource.id}` ? "Opening Preview..." : "Preview PDF" }}
            </button>
          </div>

          <article
            v-if="resource.rejectReason"
            class="panel-card resource-record-card__note"
          >
            <strong>Review Note</strong>
            <p class="meta-copy" style="margin-top: 12px;">{{ resource.rejectReason }}</p>
          </article>
        </article>
      </div>
    </article>
  </section>
</template>

<style scoped>
.profile-resource-stats {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-top: 24px;
}

.profile-resource-stat {
  min-height: 128px;
  display: grid;
  gap: 8px;
  align-content: end;
}

.profile-resource-stat__label {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.profile-resource-stat strong {
  font-family: var(--cp-font-display);
  font-size: clamp(24px, 4vw, 32px);
}

.resource-record-list {
  display: grid;
  gap: var(--cp-gap-4);
}

.resource-record-card {
  display: grid;
  gap: var(--cp-gap-4);
}

.resource-record-card__actions {
  margin-top: -4px;
}

.resource-record-card__header {
  display: flex;
  justify-content: space-between;
  gap: var(--cp-gap-4);
  align-items: start;
}

.resource-record-card__eyebrow {
  margin: 0;
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.resource-record-card__title {
  margin: 8px 0 0;
  font-family: var(--cp-font-display);
  font-size: clamp(24px, 4vw, 30px);
  line-height: 1.14;
}

.resource-record-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cp-gap-3);
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.resource-record-card__note {
  background:
    linear-gradient(180deg, rgba(255, 247, 241, 0.84), rgba(255, 240, 236, 0.96)),
    radial-gradient(circle at top right, rgba(197, 79, 45, 0.14), transparent 36%);
}

@media (max-width: 1023px) {
  .profile-resource-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .profile-resource-stats {
    grid-template-columns: 1fr;
  }

  .resource-record-card__header {
    flex-direction: column;
  }
}
</style>
