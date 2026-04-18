<script setup>
import { computed, ref, watch } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { applyToJob, favoriteJob, getJobDetail, unfavoriteJob } from "../api/jobs.js";
import { getMyResumes } from "../api/resumes.js";
import { useUserStore } from "../stores/user.js";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const loading = ref(false);
const errorMessage = ref("");
const actionError = ref("");
const actionMessage = ref("");
const favoriteLoading = ref(false);
const applySubmitting = ref(false);
const resumesLoading = ref(false);
const resumesLoaded = ref(false);
const applyPanelOpen = ref(false);
const resumes = ref([]);
const resumesError = ref("");
const selectedResumeId = ref("");
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
      value:
        educationLabels[detail.value.educationRequirement]
        || detail.value.educationRequirement
        || "Requirement",
    },
    { label: "Source", value: detail.value.sourcePlatform || "TBD" },
    { label: "Deadline", value: formatDate(detail.value.deadlineAt) },
  ];
});

const applyButtonLabel = computed(() => {
  if (detail.value?.appliedByMe) {
    return "Applied";
  }
  if (applySubmitting.value) {
    return "Submitting...";
  }
  return applyPanelOpen.value ? "Hide Apply Panel" : "Apply In Platform";
});

function normalizeDetail(payload) {
  return {
    appliedByMe: false,
    applicationId: null,
    favoritedByMe: false,
    ...payload,
  };
}

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
  actionError.value = "";
  actionMessage.value = "";
  applyPanelOpen.value = false;
  resumesError.value = "";
  selectedResumeId.value = "";

  try {
    detail.value = normalizeDetail(await getJobDetail(route.params.id));
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

async function ensureResumesLoaded(force = false) {
  if (!userStore.isAuthenticated) {
    return;
  }

  if (resumesLoaded.value && !force) {
    return;
  }

  resumesLoading.value = true;
  resumesError.value = "";

  try {
    const payload = await getMyResumes();
    resumes.value = payload?.resumes || [];
    resumesLoaded.value = true;

    if (resumes.value.length === 1) {
      selectedResumeId.value = String(resumes.value[0].id);
    }
  } catch (error) {
    resumesError.value = error.message || "Resume library loading failed. Please try again.";
  } finally {
    resumesLoading.value = false;
  }
}

async function handleToggleFavorite() {
  if (!ensureAuthenticated()) {
    return;
  }

  actionError.value = "";
  actionMessage.value = "";
  favoriteLoading.value = true;

  try {
    const nextDetail = detail.value?.favoritedByMe
      ? await unfavoriteJob(detail.value.id)
      : await favoriteJob(detail.value.id);

    detail.value = normalizeDetail({
      ...detail.value,
      ...nextDetail,
    });
  } catch (error) {
    actionError.value = error.message || "Save action failed. Please try again.";
  } finally {
    favoriteLoading.value = false;
  }
}

async function handleToggleApply() {
  if (!ensureAuthenticated()) {
    return;
  }

  if (detail.value?.appliedByMe) {
    return;
  }

  actionError.value = "";
  actionMessage.value = "";
  applyPanelOpen.value = !applyPanelOpen.value;

  if (applyPanelOpen.value) {
    await ensureResumesLoaded();
  }
}

async function handleApplySubmit() {
  if (!detail.value?.id || detail.value.appliedByMe) {
    return;
  }

  actionError.value = "";
  actionMessage.value = "";

  if (!selectedResumeId.value) {
    actionError.value = "Select a resume first.";
    return;
  }

  applySubmitting.value = true;

  try {
    const response = await applyToJob(detail.value.id, {
      resumeId: Number(selectedResumeId.value),
    });

    detail.value = normalizeDetail({
      ...detail.value,
      appliedByMe: true,
      applicationId: response.id,
    });
    applyPanelOpen.value = false;
    actionMessage.value = "Application submitted.";
  } catch (error) {
    actionError.value = error.message || "Application submission failed. Please try again.";
  } finally {
    applySubmitting.value = false;
  }
}

watch(
  () => route.params.id,
  () => {
    loadDetail();
  },
  { immediate: true },
);
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
            <span v-if="detail.appliedByMe" class="status-badge approved">Applied</span>
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
                data-testid="apply-toggle"
                type="button"
                class="app-btn"
                :disabled="detail.appliedByMe || applySubmitting"
                @click="handleToggleApply"
              >
                {{ applyButtonLabel }}
              </button>
              <a
                data-testid="source-link"
                class="ghost-btn"
                :href="detail.sourceUrl"
                target="_blank"
                rel="noreferrer"
              >
                Source Link
              </a>
              <button
                data-testid="favorite-toggle"
                type="button"
                class="ghost-btn"
                :disabled="favoriteLoading"
                @click="handleToggleFavorite"
              >
                {{ detail.favoritedByMe ? "Remove From Favorites" : "Save This Job" }}
              </button>
              <RouterLink to="/jobs" class="ghost-btn">
                Back To Jobs
              </RouterLink>
            </div>
          </article>

          <article v-if="applyPanelOpen && !detail.appliedByMe" class="panel-card apply-panel">
            <strong>Apply In Platform</strong>

            <div v-if="resumesLoading" class="empty-state apply-panel__state">
              Loading your resume library...
            </div>
            <div v-else-if="resumesError" class="field-grid">
              <p class="field-error" role="alert">{{ resumesError }}</p>
              <button type="button" class="ghost-btn" @click="ensureResumesLoaded(true)">
                Retry Resume Load
              </button>
            </div>
            <div v-else-if="!resumes.length" class="field-grid apply-panel__state">
              <p class="meta-copy">
                Upload a resume first so the platform can store a stable snapshot for this
                application.
              </p>
              <RouterLink to="/profile/resumes" class="app-link">
                Open Resume Library
              </RouterLink>
            </div>
            <div v-else class="field-grid" style="margin-top: 16px;">
              <p class="meta-copy">Choose one resume file for this application.</p>

              <label
                v-for="resume in resumes"
                :key="resume.id"
                class="panel-card apply-option"
              >
                <input
                  v-model="selectedResumeId"
                  type="radio"
                  name="resumeId"
                  :value="String(resume.id)"
                />
                <span class="apply-option__copy">
                  <strong>{{ resume.title }}</strong>
                  <small>{{ resume.fileName || "Resume file" }}</small>
                </span>
              </label>

              <div class="inline-form-actions">
                <button
                  data-testid="submit-application"
                  type="button"
                  class="app-btn"
                  :disabled="applySubmitting"
                  @click="handleApplySubmit"
                >
                  {{ applySubmitting ? "Submitting..." : "Submit Application" }}
                </button>
                <RouterLink to="/profile/resumes" class="ghost-btn">
                  Manage Resumes
                </RouterLink>
              </div>
            </div>
          </article>

          <article v-if="detail.appliedByMe" class="panel-card apply-panel apply-panel--success">
            <strong>Applied</strong>
            <p class="meta-copy" style="margin-top: 12px;">
              This job already has your in-platform application on file. Review the record any
              time from your application history.
            </p>
            <RouterLink to="/profile/applications" class="app-link" style="margin-top: 16px;">
              Open My Applications
            </RouterLink>
          </article>

          <article class="panel-card">
            <strong>Source Reminder</strong>
            <p class="meta-copy" style="margin-top: 12px;">
              Keep the source listing for cross-checking details. The platform submission flow and
              the original source link stay available side by side.
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

.apply-panel {
  display: grid;
  gap: var(--cp-gap-4);
}

.apply-panel__state {
  min-height: 128px;
  align-content: center;
}

.apply-panel--success {
  background:
    linear-gradient(180deg, rgba(244, 250, 246, 0.92), rgba(237, 246, 241, 0.98)),
    radial-gradient(circle at top right, rgba(37, 98, 77, 0.12), transparent 36%);
}

.apply-option {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 12px;
  align-items: start;
  cursor: pointer;
}

.apply-option input {
  margin-top: 4px;
}

.apply-option__copy {
  display: grid;
  gap: 6px;
}

.apply-option__copy small {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

@media (max-width: 1023px) {
  .job-detail,
  .job-detail__meta-grid {
    grid-template-columns: 1fr;
  }
}
</style>
