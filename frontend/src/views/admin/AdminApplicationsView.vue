<script setup>
import { onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import { downloadAdminApplicationResume, getAdminApplications } from "../../api/admin.js";

const loading = ref(true);
const errorMessage = ref("");
const actionMessage = ref("");
const actionError = ref("");
const actionLoadingId = ref("");
const summary = ref({
  total: 0,
  submittedToday: 0,
  uniqueApplicants: 0,
  uniqueJobs: 0,
  applications: [],
});

const statCards = [
  { key: "total", label: "All Applications" },
  { key: "submittedToday", label: "Submitted Today" },
  { key: "uniqueApplicants", label: "Unique Applicants" },
  { key: "uniqueJobs", label: "Unique Jobs" },
];

function formatTime(value) {
  if (!value) {
    return "Unknown time";
  }

  return String(value).replace("T", " ").slice(0, 16);
}

async function loadApplications() {
  loading.value = true;
  errorMessage.value = "";

  try {
    summary.value = await getAdminApplications();
  } catch (error) {
    errorMessage.value = error.message || "Admin applications loading failed. Please try again.";
  } finally {
    loading.value = false;
  }
}

async function handleDownload(application) {
  actionMessage.value = "";
  actionError.value = "";
  actionLoadingId.value = `download-${application.id}`;

  try {
    const fileName = await downloadAdminApplicationResume(application.id);
    actionMessage.value = `Download started for ${fileName || application.resumeFileNameSnapshot || `application-${application.id}`}.`;
  } catch (error) {
    actionError.value = error.message || "Resume snapshot download failed. Please try again.";
  } finally {
    actionLoadingId.value = "";
  }
}

onMounted(loadApplications);
</script>

<template>
  <section class="page-stack">
    <article v-if="loading" class="section-card">
      <div class="empty-state">Loading admin applications...</div>
    </article>

    <article v-else-if="errorMessage" class="section-card">
      <div class="field-grid">
        <p class="field-error" role="alert">{{ errorMessage }}</p>
        <button type="button" class="ghost-btn" @click="loadApplications">
          Retry
        </button>
      </div>
    </article>

    <template v-else>
      <article class="section-card">
        <div class="section-header">
          <div>
            <span class="section-eyebrow">Admin Applications Desk</span>
            <h1 class="page-title" style="margin-top: 16px;">Application workbench</h1>
            <p class="page-subtitle" style="margin-top: 16px;">
              Read the latest application records, download resume snapshots, and jump back to the
              original job card. This board stays read-only in this phase.
            </p>
          </div>
        </div>

        <div class="stats-grid admin-applications-stats">
          <article
            v-for="card in statCards"
            :key="card.key"
            class="panel-card admin-applications-stat"
          >
            <span class="admin-applications-stat__label">{{ card.label }}</span>
            <strong>{{ summary[card.key] || 0 }}</strong>
          </article>
        </div>

        <p v-if="actionMessage" class="field-hint" style="margin-top: 20px;">{{ actionMessage }}</p>
        <p v-if="actionError" class="field-error" role="alert" style="margin-top: 12px;">
          {{ actionError }}
        </p>
      </article>

      <article class="section-card">
        <div v-if="!summary.applications.length" class="empty-state">
          No application records are available on the board yet.
        </div>
        <div v-else>
          <table class="app-table">
            <thead>
              <tr>
                <th>Application</th>
                <th>Applicant</th>
                <th>Job</th>
                <th>Resume Snapshot</th>
                <th>Submitted</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="application in summary.applications" :key="application.id">
                <td>#{{ application.id }}</td>
                <td>
                  <div class="admin-applications-table__identity">
                    <strong>{{ application.applicantNickname || "Unknown user" }}</strong>
                    <span>User ID {{ application.applicantUserId }}</span>
                  </div>
                </td>
                <td>
                  <div class="admin-applications-table__job">
                    <strong>{{ application.jobTitle || "Untitled job" }}</strong>
                    <span>{{ application.companyName || "Unknown company" }}</span>
                  </div>
                </td>
                <td>{{ application.resumeFileNameSnapshot || "No snapshot file" }}</td>
                <td>{{ formatTime(application.submittedAt) }}</td>
                <td>{{ application.status }}</td>
                <td>
                  <div class="inline-form-actions">
                    <button
                      :data-testid="`download-application-resume-${application.id}`"
                      type="button"
                      class="ghost-btn"
                      :disabled="actionLoadingId === `download-${application.id}`"
                      @click="handleDownload(application)"
                    >
                      {{ actionLoadingId === `download-${application.id}` ? "Preparing..." : "Download Resume" }}
                    </button>
                    <RouterLink :to="`/jobs/${application.jobId}`" class="app-link">
                      Open Job
                    </RouterLink>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>

          <div class="mobile-table-cards">
            <article
              v-for="application in summary.applications"
              :key="`mobile-${application.id}`"
              class="table-card admin-application-card"
            >
              <div class="admin-application-card__header">
                <div>
                  <p class="admin-application-card__eyebrow">Application #{{ application.id }}</p>
                  <strong>{{ application.jobTitle || "Untitled job" }}</strong>
                </div>
                <span class="status-badge pending">{{ application.status }}</span>
              </div>

              <p class="meta-copy">
                {{ application.companyName || "Unknown company" }} / {{ application.applicantNickname || "Unknown user" }}
              </p>
              <p class="meta-copy">
                User ID {{ application.applicantUserId }} / {{ application.resumeFileNameSnapshot || "No snapshot file" }}
              </p>
              <p class="meta-copy">Submitted {{ formatTime(application.submittedAt) }}</p>

              <div class="inline-form-actions" style="margin-top: 12px;">
                <button
                  :data-testid="`download-application-resume-${application.id}`"
                  type="button"
                  class="ghost-btn"
                  :disabled="actionLoadingId === `download-${application.id}`"
                  @click="handleDownload(application)"
                >
                  {{ actionLoadingId === `download-${application.id}` ? "Preparing..." : "Download Resume" }}
                </button>
                <RouterLink :to="`/jobs/${application.jobId}`" class="app-link">
                  Open Job
                </RouterLink>
              </div>
            </article>
          </div>
        </div>
      </article>
    </template>
  </section>
</template>

<style scoped>
.admin-applications-stats {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.admin-applications-stat {
  min-height: 128px;
  display: grid;
  gap: 8px;
  align-content: end;
}

.admin-applications-stat__label {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.admin-applications-stat strong {
  font-family: var(--cp-font-display);
  font-size: clamp(24px, 4vw, 32px);
}

.admin-applications-table__identity,
.admin-applications-table__job,
.admin-application-card {
  display: grid;
  gap: 6px;
}

.admin-applications-table__identity span,
.admin-applications-table__job span,
.admin-application-card__eyebrow {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.admin-application-card__header {
  display: flex;
  justify-content: space-between;
  gap: var(--cp-gap-4);
  align-items: start;
}

@media (max-width: 1023px) {
  .admin-applications-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .admin-applications-stats {
    grid-template-columns: 1fr;
  }

  .admin-application-card__header {
    flex-direction: column;
  }
}
</style>
