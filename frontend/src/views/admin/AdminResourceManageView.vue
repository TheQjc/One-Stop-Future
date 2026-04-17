<script setup>
import { computed, onMounted, ref } from "vue";
import ResourceZipPreviewPanel from "../../components/ResourceZipPreviewPanel.vue";
import {
  getAdminResources,
  offlineAdminResource,
  publishAdminResource,
  rejectAdminResource,
} from "../../api/admin.js";
import { previewResource, previewZipResource } from "../../api/resources.js";

const loading = ref(true);
const errorMessage = ref("");
const actionMessage = ref("");
const actionLoadingId = ref("");
const selectedResourceId = ref(null);
const rejectReason = ref("");
const zipPreviewError = ref("");
const zipPreviewData = ref(null);
const zipPreviewResourceId = ref(null);
const summary = ref({
  total: 0,
  resources: [],
});

const selectedResource = computed(() => (
  summary.value.resources.find((item) => item.id === selectedResourceId.value) || null
));

const statusCards = computed(() => {
  const resources = summary.value.resources || [];

  return [
    { label: "All Resources", value: resources.length },
    { label: "Pending", value: resources.filter((item) => item.status === "PENDING").length },
    { label: "Published", value: resources.filter((item) => item.status === "PUBLISHED").length },
    {
      label: "Closed",
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
    return "Pending";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "Pending";
  }

  return new Intl.DateTimeFormat("zh-CN", {
    month: "numeric",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
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

function previewKindOf(resource) {
  const kind = resource?.previewKind;
  if (kind === "FILE" || kind === "ZIP_TREE" || kind === "NONE") {
    return kind;
  }
  return resource?.previewAvailable ? "FILE" : "NONE";
}

function canPreview(resource) {
  const kind = previewKindOf(resource);
  return kind === "FILE" || kind === "ZIP_TREE";
}

async function loadResources() {
  loading.value = true;
  errorMessage.value = "";

  try {
    summary.value = await getAdminResources();
  } catch (error) {
    errorMessage.value = error.message || "Admin resources loading failed. Please try again.";
  } finally {
    loading.value = false;
  }
}

function selectResource(resource) {
  selectedResourceId.value = resource.id;
  rejectReason.value = resource.rejectReason || "";
  actionMessage.value = "";
  errorMessage.value = "";
  if (zipPreviewResourceId.value !== resource.id) {
    zipPreviewData.value = null;
    zipPreviewError.value = "";
  }
}

function clearSelection() {
  selectedResourceId.value = null;
  rejectReason.value = "";
  zipPreviewError.value = "";
  zipPreviewData.value = null;
  zipPreviewResourceId.value = null;
}

async function handlePublish(id) {
  actionMessage.value = "";
  errorMessage.value = "";
  actionLoadingId.value = `publish-${id}`;

  try {
    await publishAdminResource(id);
    actionMessage.value = "Resource published.";
    await loadResources();
  } catch (error) {
    errorMessage.value = error.message || "Publish failed. Please try again.";
  } finally {
    actionLoadingId.value = "";
  }
}

async function handleOffline(id) {
  actionMessage.value = "";
  errorMessage.value = "";
  actionLoadingId.value = `offline-${id}`;

  try {
    await offlineAdminResource(id);
    actionMessage.value = "Resource offlined.";
    await loadResources();
  } catch (error) {
    errorMessage.value = error.message || "Offline failed. Please try again.";
  } finally {
    actionLoadingId.value = "";
  }
}

async function handleReject() {
  if (!selectedResource.value) {
    errorMessage.value = "Select a pending resource first.";
    return;
  }

  if (!rejectReason.value.trim()) {
    errorMessage.value = "Reject reason is required.";
    return;
  }

  actionMessage.value = "";
  errorMessage.value = "";
  actionLoadingId.value = `reject-${selectedResource.value.id}`;

  try {
    await rejectAdminResource(selectedResource.value.id, {
      reason: rejectReason.value.trim(),
    });
    actionMessage.value = "Resource rejected.";
    await loadResources();
  } catch (error) {
    errorMessage.value = error.message || "Reject failed. Please try again.";
  } finally {
    actionLoadingId.value = "";
  }
}

async function handlePreview(id) {
  const resource = summary.value.resources.find((item) => item.id === id);
  const kind = previewKindOf(resource);
  if (kind === "NONE") {
    return;
  }

  actionMessage.value = "";
  errorMessage.value = "";
  actionLoadingId.value = `preview-${id}`;
  zipPreviewError.value = "";

  try {
    if (kind === "ZIP_TREE") {
      selectedResourceId.value = id;
      rejectReason.value = resource.rejectReason || "";
      zipPreviewData.value = await previewZipResource(id);
      zipPreviewResourceId.value = id;
      actionMessage.value = `Contents loaded for ${resource?.title || `resource #${id}`}.`;
      return;
    }

    zipPreviewData.value = null;
    zipPreviewResourceId.value = null;
    await previewResource(id);
    actionMessage.value = `Preview opened for ${resource?.title || `resource #${id}`}.`;
  } catch (error) {
    if (kind === "ZIP_TREE") {
      zipPreviewResourceId.value = id;
      zipPreviewError.value = error.message || "Contents preview failed. Please try again.";
    }
    errorMessage.value = error.message || "Preview failed. Please try again.";
  } finally {
    actionLoadingId.value = "";
  }
}

function previewLabel(resource) {
  return previewKindOf(resource) === "ZIP_TREE" ? "Preview Contents" : "Preview";
}

const showSelectedZipPreview = computed(() => (
  previewKindOf(selectedResource.value) === "ZIP_TREE"
    && zipPreviewResourceId.value === selectedResource.value?.id
));

onMounted(loadResources);
</script>

<template>
  <section class="page-stack">
    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">Admin Resources Desk</span>
          <h1 class="page-title" style="margin-top: 16px;">Review the resource shelf from one board.</h1>
          <p class="page-subtitle" style="margin-top: 16px;">
            This workspace only covers review actions: select, publish, reject with reason, and
            offline already-published files.
          </p>
        </div>
      </div>

      <div class="stats-grid admin-resource-stats">
        <article
          v-for="card in statusCards"
          :key="card.label"
          class="panel-card admin-resource-stat"
        >
          <span class="admin-resource-stat__label">{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
        </article>
      </div>

      <p v-if="actionMessage" class="field-hint" style="margin-top: 20px;">{{ actionMessage }}</p>
      <p v-if="errorMessage && !loading" class="field-error" role="alert" style="margin-top: 12px;">
        {{ errorMessage }}
      </p>
    </article>

    <div class="dashboard-grid">
      <article class="section-card">
        <div class="section-header">
          <div>
            <span class="section-eyebrow">Selected Record</span>
            <h2 class="page-title" style="margin-top: 16px;">Review surface</h2>
          </div>
        </div>

        <div v-if="!selectedResource" class="empty-state">
          Pick a row from the queue to review or reject it from this panel.
        </div>
        <div v-else class="field-grid">
          <article class="panel-card selected-resource-card">
            <div class="selected-resource-card__header">
              <div>
                <p class="selected-resource-card__eyebrow">Resource #{{ selectedResource.id }}</p>
                <h3 class="selected-resource-card__title">{{ selectedResource.title }}</h3>
              </div>
              <span class="status-badge" :class="statusClass(selectedResource.status)">
                {{ selectedResource.status }}
              </span>
            </div>

            <div class="selected-resource-card__meta">
              <span>{{ formatCategory(selectedResource.category) }}</span>
              <span>{{ selectedResource.uploaderNickname || "Unknown uploader" }}</span>
              <span>{{ selectedResource.fileName || "Archive file" }}</span>
              <span>Created {{ formatTime(selectedResource.createdAt) }}</span>
              <span>Reviewed {{ formatTime(selectedResource.reviewedAt) }}</span>
            </div>

            <p v-if="selectedResource.rejectReason" class="meta-copy">
              Current review note: {{ selectedResource.rejectReason }}
            </p>
          </article>

          <label v-if="selectedResource.status === 'PENDING'" class="field-label">
            Reject Reason
            <textarea
              v-model.trim="rejectReason"
              class="field-textarea"
              name="rejectReason"
              placeholder="Explain what must change before this file can be published."
            />
          </label>

          <div class="inline-form-actions">
            <button
              v-if="canPreview(selectedResource)"
              type="button"
              class="ghost-btn preview-action"
              data-testid="selected-preview-action"
              :disabled="Boolean(actionLoadingId)"
              @click="handlePreview(selectedResource.id)"
            >
              {{ actionLoadingId === `preview-${selectedResource.id}`
                ? (previewKindOf(selectedResource) === "ZIP_TREE" ? "Loading Contents..." : "Opening Preview...")
                : previewLabel(selectedResource) }}
            </button>
            <button
              v-if="selectedResource.status === 'PENDING' || selectedResource.status === 'OFFLINE'"
              type="button"
              class="app-btn publish-action"
              :disabled="Boolean(actionLoadingId)"
              @click="handlePublish(selectedResource.id)"
            >
              {{ actionLoadingId === `publish-${selectedResource.id}` ? "Publishing..." : "Publish" }}
            </button>
            <button
              v-if="selectedResource.status === 'PENDING'"
              type="button"
              class="danger-btn reject-action"
              :disabled="Boolean(actionLoadingId)"
              @click="handleReject"
            >
              {{ actionLoadingId === `reject-${selectedResource.id}` ? "Rejecting..." : "Reject" }}
            </button>
            <button
              v-if="selectedResource.status === 'PUBLISHED'"
              type="button"
              class="ghost-btn offline-action"
              :disabled="Boolean(actionLoadingId)"
              @click="handleOffline(selectedResource.id)"
            >
              {{ actionLoadingId === `offline-${selectedResource.id}` ? "Offlining..." : "Offline" }}
            </button>
            <button type="button" class="ghost-btn" :disabled="Boolean(actionLoadingId)" @click="clearSelection">
              Clear Selection
            </button>
          </div>

          <ResourceZipPreviewPanel
            v-if="showSelectedZipPreview || (previewKindOf(selectedResource) === 'ZIP_TREE' && actionLoadingId === `preview-${selectedResource.id}`)"
            :loading="actionLoadingId === `preview-${selectedResource.id}`"
            :error-message="showSelectedZipPreview ? zipPreviewError : ''"
            :preview="showSelectedZipPreview ? zipPreviewData : null"
          />
        </div>
      </article>

      <article class="section-card">
        <div class="section-header">
          <div>
            <span class="section-eyebrow">Queue</span>
            <h2 class="page-title" style="margin-top: 16px;">Current resource records</h2>
          </div>
        </div>

        <div v-if="loading" class="empty-state">Loading admin resources...</div>
        <div v-else-if="errorMessage && !summary.resources.length" class="field-grid">
          <p class="field-error" role="alert">{{ errorMessage }}</p>
          <button type="button" class="ghost-btn" @click="loadResources">
            Retry
          </button>
        </div>
        <div v-else-if="!summary.resources.length" class="empty-state">
          No resource records are available for review.
        </div>
        <div v-else>
          <table class="app-table">
            <thead>
              <tr>
                <th>Title</th>
                <th>Category</th>
                <th>Uploader</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="resource in summary.resources" :key="resource.id">
                <td>{{ resource.title }}</td>
                <td>{{ formatCategory(resource.category) }}</td>
                <td>{{ resource.uploaderNickname }}</td>
                <td>{{ resource.status }}</td>
                <td>
                  <div class="inline-form-actions">
                    <button type="button" class="ghost-btn select-action" @click="selectResource(resource)">
                      Select
                    </button>
                    <button
                      v-if="canPreview(resource)"
                      type="button"
                      class="ghost-btn preview-action"
                      :disabled="actionLoadingId === `preview-${resource.id}`"
                      @click="handlePreview(resource.id)"
                    >
                      {{ actionLoadingId === `preview-${resource.id}`
                        ? (previewKindOf(resource) === "ZIP_TREE" ? "Loading Contents..." : "Opening Preview...")
                        : previewLabel(resource) }}
                    </button>
                    <button
                      type="button"
                      class="ghost-btn publish-action"
                      :disabled="!['PENDING', 'OFFLINE'].includes(resource.status) || actionLoadingId === `publish-${resource.id}`"
                      @click="handlePublish(resource.id)"
                    >
                      Publish
                    </button>
                    <button
                      type="button"
                      class="ghost-btn offline-action"
                      :disabled="resource.status !== 'PUBLISHED' || actionLoadingId === `offline-${resource.id}`"
                      @click="handleOffline(resource.id)"
                    >
                      Offline
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>

          <div class="mobile-table-cards">
            <article
              v-for="resource in summary.resources"
              :key="`mobile-${resource.id}`"
              class="table-card"
            >
              <strong>{{ resource.title }}</strong>
              <p class="meta-copy">{{ formatCategory(resource.category) }} / {{ resource.uploaderNickname }}</p>
              <p class="meta-copy">Status {{ resource.status }} / Created {{ formatTime(resource.createdAt) }}</p>
              <div class="inline-form-actions" style="margin-top: 12px;">
                <button type="button" class="ghost-btn select-action" @click="selectResource(resource)">
                  Select
                </button>
                <button
                  v-if="canPreview(resource)"
                  type="button"
                  class="ghost-btn preview-action"
                  :disabled="actionLoadingId === `preview-${resource.id}`"
                  @click="handlePreview(resource.id)"
                >
                  {{ actionLoadingId === `preview-${resource.id}`
                    ? (previewKindOf(resource) === "ZIP_TREE" ? "Loading Contents..." : "Opening Preview...")
                    : previewLabel(resource) }}
                </button>
                <button
                  type="button"
                  class="ghost-btn publish-action"
                  :disabled="!['PENDING', 'OFFLINE'].includes(resource.status) || actionLoadingId === `publish-${resource.id}`"
                  @click="handlePublish(resource.id)"
                >
                  Publish
                </button>
                <button
                  type="button"
                  class="ghost-btn offline-action"
                  :disabled="resource.status !== 'PUBLISHED' || actionLoadingId === `offline-${resource.id}`"
                  @click="handleOffline(resource.id)"
                >
                  Offline
                </button>
              </div>
            </article>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.admin-resource-stats {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.admin-resource-stat {
  min-height: 128px;
  display: grid;
  gap: 8px;
  align-content: end;
}

.admin-resource-stat__label {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.admin-resource-stat strong {
  font-family: var(--cp-font-display);
  font-size: clamp(24px, 4vw, 32px);
}

.selected-resource-card {
  display: grid;
  gap: var(--cp-gap-4);
}

.selected-resource-card__header {
  display: flex;
  justify-content: space-between;
  gap: var(--cp-gap-4);
  align-items: start;
}

.selected-resource-card__eyebrow {
  margin: 0;
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.selected-resource-card__title {
  margin: 8px 0 0;
  font-family: var(--cp-font-display);
  font-size: clamp(24px, 4vw, 30px);
  line-height: 1.14;
}

.selected-resource-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cp-gap-3);
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

@media (max-width: 1023px) {
  .admin-resource-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .admin-resource-stats {
    grid-template-columns: 1fr;
  }

  .selected-resource-card__header {
    flex-direction: column;
  }
}
</style>
