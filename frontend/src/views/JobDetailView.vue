<script setup>
import { computed, ref, watch } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { favoriteJob, getJobDetail, unfavoriteJob } from "../api/jobs.js";
import { useUserStore } from "../stores/user.js";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const loading = ref(false);
const errorMessage = ref("");
const actionError = ref("");
const actionLoading = ref("");
const detail = ref(null);

const typeLabels = {
  INTERNSHIP: "Internship",
  FULL_TIME: "Full Time",
  CAMPUS: "Campus",
};

const educationLabels = {
  ANY: "Any",
  BACHELOR: "Bachelor",
  MASTER: "Master",
  DOCTOR: "Doctor",
};

const metaItems = computed(() => {
  if (!detail.value) {
    return [];
  }

  return [
    { label: "Company", value: detail.value.companyName || "TBD" },
    { label: "City", value: detail.value.city || "TBD" },
    { label: "Type", value: typeLabels[detail.value.jobType] || detail.value.jobType || "Role" },
    {
      label: "Education",
      value: educationLabels[detail.value.educationRequirement] || detail.value.educationRequirement || "Requirement",
    },
    { label: "Source", value: detail.value.sourcePlatform || "TBD" },
    { label: "Deadline", value: formatDate(detail.value.deadlineAt) },
  ];
});

function formatDate(value) {
  if (!value) {
    return "Open";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "Open";
  }

  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "numeric",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

async function loadDetail() {
  loading.value = true;
  errorMessage.value = "";

  try {
    detail.value = await getJobDetail(route.params.id);
  } catch (error) {
    errorMessage.value = error.message || "Job detail loading failed. Please try again.";
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
  actionLoading.value = "favorite";

  try {
    detail.value = detail.value?.favoritedByMe
      ? await unfavoriteJob(detail.value.id)
      : await favoriteJob(detail.value.id);
  } catch (error) {
    actionError.value = error.message || "Save action failed. Please try again.";
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
      <div v-if="loading" class="empty-state">Loading job detail...</div>
      <div v-else-if="errorMessage" class="field-grid">
        <p class="field-error" role="alert">{{ errorMessage }}</p>
        <button type="button" class="ghost-btn" @click="loadDetail">
          Retry
        </button>
      </div>
      <div v-else-if="detail" class="job-detail">
        <div class="job-detail__main">
          <div class="chip-row">
            <span class="section-eyebrow">Opportunity Card</span>
            <span v-if="detail.favoritedByMe" class="status-badge approved">Saved</span>
          </div>

          <h1 class="hero-title" style="margin-top: 18px;">{{ detail.title }}</h1>
          <hr class="editorial-rule" />
          <p class="hero-copy">{{ detail.summary }}</p>

          <div class="job-detail__meta-grid">
            <article
              v-for="item in metaItems"
              :key="item.label"
              class="panel-card job-detail__meta-card"
            >
              <span class="job-detail__meta-label">{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </article>
          </div>

          <article class="panel-card">
            <span class="section-eyebrow">Expanded Note</span>
            <p class="job-detail__body">{{ detail.content || detail.summary }}</p>
          </article>
        </div>

        <aside class="job-detail__aside">
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
                {{ detail.favoritedByMe ? "Remove From Favorites" : "Save This Job" }}
              </button>
              <a
                data-testid="source-link"
                class="app-btn"
                :href="detail.sourceUrl"
                target="_blank"
                rel="noreferrer"
              >
                Source Link
              </a>
              <RouterLink to="/jobs" class="ghost-btn">
                Back To Jobs
              </RouterLink>
            </div>
          </article>

          <article class="panel-card">
            <strong>Source Reminder</strong>
            <p class="meta-copy" style="margin-top: 12px;">
              This module aggregates cards and sends you to the original listing for the final application step.
            </p>
          </article>

          <p v-if="actionError" class="field-error" role="alert">{{ actionError }}</p>
        </aside>
      </div>
    </article>
  </section>
</template>

<style scoped>
.job-detail {
  display: grid;
  grid-template-columns: 1.25fr 0.75fr;
  gap: var(--cp-gap-6);
}

.job-detail__main,
.job-detail__aside {
  display: grid;
  gap: var(--cp-gap-4);
}

.job-detail__meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cp-gap-4);
}

.job-detail__meta-card {
  min-height: 116px;
  display: grid;
  gap: 8px;
  align-content: end;
}

.job-detail__meta-label {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.job-detail__meta-card strong {
  font-family: var(--cp-font-display);
  font-size: 22px;
  line-height: 1.2;
}

.job-detail__body {
  margin: 16px 0 0;
  white-space: pre-wrap;
  line-height: 1.8;
  color: var(--cp-ink);
}

@media (max-width: 1023px) {
  .job-detail,
  .job-detail__meta-grid {
    grid-template-columns: 1fr;
  }
}
</style>
