<script setup>
import { computed, ref, watch } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import {
  downloadResource,
  favoriteResource,
  getResourceDetail,
  unfavoriteResource,
} from "../api/resources.js";
import { useUserStore } from "../stores/user.js";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const loading = ref(false);
const errorMessage = ref("");
const actionError = ref("");
const actionMessage = ref("");
const actionLoading = ref("");
const detail = ref(null);

const categoryLabels = {
  EXAM_PAPER: "Exam Paper",
  LANGUAGE_TEST: "Language Test",
  RESUME_TEMPLATE: "Resume Template",
  INTERVIEW_EXPERIENCE: "Interview Notes",
  OTHER: "Other",
};

const metaItems = computed(() => {
  if (!detail.value) {
    return [];
  }

  return [
    { label: "Category", value: categoryLabels[detail.value.category] || detail.value.category || "Archive" },
    { label: "Uploader", value: detail.value.uploaderNickname || "Archive Desk" },
    { label: "File Size", value: formatSize(detail.value.fileSize) },
    { label: "Downloads", value: `${detail.value.downloadCount || 0}` },
    { label: "Published", value: formatDate(detail.value.publishedAt) },
    { label: "File", value: detail.value.fileName || "Archive File" },
  ];
});

function formatDate(value) {
  if (!value) {
    return "Queued";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "Queued";
  }

  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "numeric",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

function formatSize(value) {
  const size = Number(value || 0);
  if (!size) {
    return "Unknown Size";
  }
  if (size >= 1024 * 1024) {
    return `${(size / (1024 * 1024)).toFixed(1)} MB`;
  }
  if (size >= 1024) {
    return `${Math.round(size / 1024)} KB`;
  }
  return `${size} B`;
}

async function loadDetail() {
  loading.value = true;
  errorMessage.value = "";

  try {
    detail.value = await getResourceDetail(route.params.id);
  } catch (error) {
    errorMessage.value = error.message || "Resource detail loading failed. Please try again.";
  } finally {
    loading.value = false;
  }
}

function redirectToLogin() {
  router.push({
    name: "login",
    query: { redirect: route.fullPath },
  });
}

function ensureAuthenticated() {
  if (userStore.isAuthenticated) {
    return true;
  }

  redirectToLogin();
  return false;
}

async function handleToggleFavorite() {
  if (!ensureAuthenticated()) {
    return;
  }

  actionError.value = "";
  actionMessage.value = "";
  actionLoading.value = "favorite";

  try {
    detail.value = detail.value?.favoritedByMe
      ? await unfavoriteResource(detail.value.id)
      : await favoriteResource(detail.value.id);
  } catch (error) {
    actionError.value = error.message || "Collection action failed. Please try again.";
  } finally {
    actionLoading.value = "";
  }
}

async function handleDownload() {
  if (!ensureAuthenticated()) {
    return;
  }

  actionError.value = "";
  actionMessage.value = "";
  actionLoading.value = "download";

  try {
    const fileName = await downloadResource(detail.value.id);
    actionMessage.value = `Download started for ${fileName}.`;
    detail.value = await getResourceDetail(route.params.id);
  } catch (error) {
    actionError.value = error.message || "Download failed. Please try again.";
  } finally {
    actionLoading.value = "";
  }
}

watch(() => route.params.id, () => {
  loadDetail();
}, { immediate: true });
</script>

<template>
  <section class="page-stack">
    <article class="section-card">
      <div v-if="loading" class="empty-state">Loading resource detail...</div>
      <div v-else-if="errorMessage" class="field-grid">
        <p class="field-error" role="alert">{{ errorMessage }}</p>
        <button type="button" class="ghost-btn" @click="loadDetail">
          Retry
        </button>
      </div>
      <div v-else-if="detail" class="resource-detail">
        <div class="resource-detail__main">
          <div class="chip-row">
            <span class="section-eyebrow">Archive Detail</span>
            <span v-if="detail.favoritedByMe" class="status-badge approved">Saved</span>
          </div>

          <h1 class="hero-title" style="margin-top: 18px;">{{ detail.title }}</h1>
          <hr class="editorial-rule" />
          <p class="hero-copy">{{ detail.summary }}</p>

          <div class="resource-detail__meta-grid">
            <article
              v-for="item in metaItems"
              :key="item.label"
              class="panel-card resource-detail__meta-card"
            >
              <span class="resource-detail__meta-label">{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </article>
          </div>

          <article class="panel-card">
            <span class="section-eyebrow">Archive Note</span>
            <p class="resource-detail__body">{{ detail.description || detail.summary }}</p>
          </article>
        </div>

        <aside class="resource-detail__aside">
          <article class="panel-card">
            <span class="section-eyebrow">Actions</span>
            <div class="field-grid" style="margin-top: 16px;">
              <button
                data-testid="favorite-toggle"
                type="button"
                class="ghost-btn"
                :disabled="actionLoading === 'favorite'"
                @click="handleToggleFavorite"
              >
                {{ detail.favoritedByMe ? "Remove From Collection" : "Save To Collection" }}
              </button>
              <button
                data-testid="download-action"
                type="button"
                class="app-btn"
                :disabled="actionLoading === 'download'"
                @click="handleDownload"
              >
                {{ actionLoading === "download" ? "Preparing Download..." : "Download Resource" }}
              </button>
              <RouterLink to="/resources" class="ghost-btn">
                Back To Archive
              </RouterLink>
            </div>
          </article>

          <article class="panel-card">
            <strong>Access Note</strong>
            <p class="meta-copy" style="margin-top: 12px;">
              Guests can read the archive card. Downloads and collection actions unlock after sign-in
              so the system can track file usage and favorites consistently.
            </p>
          </article>

          <p v-if="actionMessage" class="field-hint">{{ actionMessage }}</p>
          <p v-if="actionError" class="field-error" role="alert">{{ actionError }}</p>
        </aside>
      </div>
    </article>
  </section>
</template>

<style scoped>
.resource-detail {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: var(--cp-gap-6);
}

.resource-detail__main,
.resource-detail__aside {
  display: grid;
  gap: var(--cp-gap-4);
}

.resource-detail__meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cp-gap-4);
}

.resource-detail__meta-card {
  min-height: 116px;
  display: grid;
  gap: 8px;
  align-content: end;
}

.resource-detail__meta-label {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.resource-detail__meta-card strong {
  font-family: var(--cp-font-display);
  font-size: 22px;
  line-height: 1.2;
}

.resource-detail__body {
  margin: 16px 0 0;
  white-space: pre-wrap;
  line-height: 1.8;
  color: var(--cp-ink);
}

@media (max-width: 1023px) {
  .resource-detail,
  .resource-detail__meta-grid {
    grid-template-columns: 1fr;
  }
}
</style>
