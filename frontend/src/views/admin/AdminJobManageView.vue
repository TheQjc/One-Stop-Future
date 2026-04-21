<script setup>
import { computed, reactive, ref } from "vue";
import {
  createAdminJob,
  deleteAdminJob,
  getAdminJobs,
  importAdminJobs,
  offlineAdminJob,
  publishAdminJob,
  syncAdminJobs,
  updateAdminJob,
} from "../../api/admin.js";

const loading = ref(true);
const errorMessage = ref("");
const actionMessage = ref("");
const actionLoadingId = ref("");
const selectedJobId = ref(null);
const importFile = ref(null);
const importLoading = ref(false);
const importSummary = ref(null);
const importErrors = ref([]);
const importErrorMessage = ref("");
const importInputRef = ref(null);
const syncLoading = ref(false);
const syncSummary = ref(null);
const syncIssues = ref([]);
const syncErrorMessage = ref("");
const summary = ref({
  total: 0,
  jobs: [],
});

const form = reactive({
  title: "",
  companyName: "",
  city: "",
  jobType: "INTERNSHIP",
  educationRequirement: "ANY",
  sourcePlatform: "Official Site",
  sourceUrl: "",
  summary: "",
  content: "",
  deadlineAt: "",
});

const statusCards = computed(() => {
  const jobs = summary.value.jobs || [];

  return [
    { label: "All Jobs", value: jobs.length },
    { label: "Draft", value: jobs.filter((item) => item.status === "DRAFT").length },
    { label: "Published", value: jobs.filter((item) => item.status === "PUBLISHED").length },
    { label: "Offline", value: jobs.filter((item) => item.status === "OFFLINE").length },
  ];
});

const editingJob = computed(() => (
  summary.value.jobs.find((item) => item.id === selectedJobId.value) || null
));

function resetForm() {
  selectedJobId.value = null;
  form.title = "";
  form.companyName = "";
  form.city = "";
  form.jobType = "INTERNSHIP";
  form.educationRequirement = "ANY";
  form.sourcePlatform = "Official Site";
  form.sourceUrl = "";
  form.summary = "";
  form.content = "";
  form.deadlineAt = "";
}

function applyJobToForm(job) {
  selectedJobId.value = job.id;
  form.title = job.title || "";
  form.companyName = job.companyName || "";
  form.city = job.city || "";
  form.jobType = job.jobType || "INTERNSHIP";
  form.educationRequirement = job.educationRequirement || "ANY";
  form.sourcePlatform = job.sourcePlatform || "Official Site";
  form.sourceUrl = job.sourceUrl || "";
  form.summary = job.summary || "";
  form.content = job.content || "";
  form.deadlineAt = job.deadlineAt ? String(job.deadlineAt).slice(0, 16) : "";
}

function resetImportInput() {
  importFile.value = null;
  if (importInputRef.value) {
    importInputRef.value.value = "";
  }
}

function handleImportFileChange(event) {
  importFile.value = event.target.files?.[0] || null;
}

async function handleImportJobs() {
  importErrorMessage.value = "";
  importErrors.value = [];
  importSummary.value = null;

  if (!importFile.value) {
    importErrorMessage.value = "Choose one CSV file first.";
    return;
  }

  const formData = new FormData();
  formData.append("file", importFile.value);
  importLoading.value = true;

  try {
    importSummary.value = await importAdminJobs(formData);
    resetImportInput();
    await loadJobs();
  } catch (error) {
    importErrorMessage.value = error.message || "Job import failed. Please try again.";
    importErrors.value = Array.isArray(error.data?.errors) ? error.data.errors : [];
  } finally {
    importLoading.value = false;
  }
}

async function handleSyncJobs() {
  syncLoading.value = true;
  syncSummary.value = null;
  syncIssues.value = [];
  syncErrorMessage.value = "";

  try {
    const result = await syncAdminJobs();
    syncSummary.value = result;
    syncIssues.value = Array.isArray(result.issues) ? result.issues : [];
    await loadJobs();
  } catch (error) {
    syncErrorMessage.value = error.message || "Job sync failed. Please try again.";
  } finally {
    syncLoading.value = false;
  }
}

function toPayload() {
  return {
    title: form.title.trim(),
    companyName: form.companyName.trim(),
    city: form.city.trim(),
    jobType: form.jobType,
    educationRequirement: form.educationRequirement,
    sourcePlatform: form.sourcePlatform.trim(),
    sourceUrl: form.sourceUrl.trim(),
    summary: form.summary.trim(),
    content: form.content.trim(),
    deadlineAt: form.deadlineAt ? `${form.deadlineAt}:00` : null,
  };
}

async function loadJobs() {
  loading.value = true;
  errorMessage.value = "";

  try {
    summary.value = await getAdminJobs();
  } catch (error) {
    errorMessage.value = error.message || "Jobs admin list loading failed. Please try again.";
  } finally {
    loading.value = false;
  }
}

async function handleSaveDraft() {
  actionMessage.value = "";
  errorMessage.value = "";
  actionLoadingId.value = selectedJobId.value ? `save-${selectedJobId.value}` : "create";

  try {
    const payload = toPayload();
    const saved = selectedJobId.value
      ? await updateAdminJob(selectedJobId.value, payload)
      : await createAdminJob(payload);
    actionMessage.value = selectedJobId.value ? "Draft updated." : "Draft created.";
    applyJobToForm(saved);
    await loadJobs();
  } catch (error) {
    errorMessage.value = error.message || "Draft save failed. Please try again.";
  } finally {
    actionLoadingId.value = "";
  }
}

async function handlePublish(id) {
  actionMessage.value = "";
  errorMessage.value = "";
  actionLoadingId.value = `publish-${id}`;

  try {
    const saved = await publishAdminJob(id);
    actionMessage.value = "Job published.";
    applyJobToForm(saved);
    await loadJobs();
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
    const saved = await offlineAdminJob(id);
    actionMessage.value = "Job offlined.";
    applyJobToForm(saved);
    await loadJobs();
  } catch (error) {
    errorMessage.value = error.message || "Offline failed. Please try again.";
  } finally {
    actionLoadingId.value = "";
  }
}

async function handleDelete(id) {
  actionMessage.value = "";
  errorMessage.value = "";
  actionLoadingId.value = `delete-${id}`;

  try {
    await deleteAdminJob(id);
    actionMessage.value = "Job deleted.";
    if (selectedJobId.value === id) {
      resetForm();
    }
    await loadJobs();
  } catch (error) {
    errorMessage.value = error.message || "Delete failed. Please try again.";
  } finally {
    actionLoadingId.value = "";
  }
}

loadJobs();
</script>

<template>
  <section class="page-stack">
    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">Admin Jobs Desk</span>
          <h1 class="page-title" style="margin-top: 16px;">Manage the jobs board from one working surface.</h1>
          <p class="page-subtitle" style="margin-top: 16px;">
            Draft, edit, publish, offline, and delete job cards without leaving the same page.
          </p>
        </div>
      </div>

      <div class="stats-grid admin-jobs-stats">
        <article
          v-for="card in statusCards"
          :key="card.label"
          class="panel-card admin-jobs-stat"
        >
          <span class="admin-jobs-stat__label">{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
        </article>
      </div>
    </article>

    <div class="dashboard-grid">
      <article class="section-card">
        <div class="section-header">
          <div>
            <span class="section-eyebrow">Sync</span>
            <h2 class="page-title" style="margin-top: 16px;">Pull jobs from the configured partner feed</h2>
            <p class="page-subtitle" style="margin-top: 16px;">
              The backend fetches one fixed HTTP JSON feed and creates new partner rows as drafts.
            </p>
          </div>
        </div>

        <div class="field-grid">
          <p class="field-hint">
            Sync updates existing non-deleted jobs by source URL, skips deleted matches, and reports invalid partner rows.
          </p>
          <p v-if="syncSummary" class="field-hint">
            {{ syncSummary.sourceName }}: Created {{ syncSummary.createdCount }}, updated {{ syncSummary.updatedCount }},
            skipped {{ syncSummary.skippedCount }}, invalid {{ syncSummary.invalidCount }}.
          </p>
          <p v-if="syncErrorMessage" class="field-error" role="alert">{{ syncErrorMessage }}</p>

          <ul v-if="syncIssues.length" class="import-error-list">
            <li v-for="item in syncIssues" :key="`${item.itemIndex}-${item.type}-${item.sourceUrl}`">
              Item {{ item.itemIndex }} / {{ item.type }} / {{ item.sourceUrl || "no sourceUrl" }}: {{ item.message }}
            </li>
          </ul>

          <div class="inline-form-actions">
            <button
              type="button"
              class="app-btn"
              data-testid="job-sync-button"
              :disabled="syncLoading"
              @click="handleSyncJobs"
            >
              {{ syncLoading ? "Syncing..." : "Sync Feed" }}
            </button>
          </div>
        </div>
      </article>

      <article class="section-card">
        <div class="section-header">
          <div>
            <span class="section-eyebrow">Import</span>
            <h2 class="page-title" style="margin-top: 16px;">Batch import job drafts</h2>
            <p class="page-subtitle" style="margin-top: 16px;">
              Upload one UTF-8 CSV file to create multiple job drafts in one pass.
            </p>
          </div>
        </div>

        <form data-testid="job-import-form" class="field-grid" @submit.prevent="handleImportJobs">
          <label class="field-label">
            CSV File
            <input
              ref="importInputRef"
              class="field-control"
              name="jobImportFile"
              type="file"
              accept=".csv,text/csv"
              @change="handleImportFileChange"
            />
          </label>

          <p class="field-hint">
            Header order: title, companyName, city, jobType, educationRequirement, sourcePlatform,
            sourceUrl, summary, content, deadlineAt.
          </p>
          <p class="field-hint">
            Required: title, companyName, city, jobType, educationRequirement, sourcePlatform,
            sourceUrl, summary. Optional: content, deadlineAt.
          </p>
          <p v-if="importSummary" class="field-hint">
            Imported {{ importSummary.importedCount }} jobs as {{ importSummary.defaultStatus }} from
            {{ importSummary.fileName }}.
          </p>
          <p v-if="importErrorMessage" class="field-error" role="alert">{{ importErrorMessage }}</p>

          <ul v-if="importErrors.length" class="import-error-list">
            <li v-for="item in importErrors" :key="`${item.rowNumber}-${item.column}-${item.message}`">
              Row {{ item.rowNumber }} / {{ item.column }}: {{ item.message }}
            </li>
          </ul>

          <div class="inline-form-actions">
            <button type="submit" class="app-btn" :disabled="importLoading">
              {{ importLoading ? "Importing..." : "Import CSV" }}
            </button>
          </div>
        </form>
      </article>

      <article class="section-card">
        <div class="section-header">
          <div>
            <span class="section-eyebrow">Editor</span>
            <h2 class="page-title" style="margin-top: 16px;">
              {{ editingJob ? "Edit Selected Job" : "Create New Draft" }}
            </h2>
          </div>
        </div>

        <form class="field-grid" @submit.prevent="handleSaveDraft">
          <label class="field-label">
            Title
            <input v-model.trim="form.title" class="field-control" name="title" type="text" maxlength="120" />
          </label>

          <label class="field-label">
            Company
            <input v-model.trim="form.companyName" class="field-control" name="companyName" type="text" maxlength="80" />
          </label>

          <label class="field-label">
            City
            <input v-model.trim="form.city" class="field-control" name="city" type="text" maxlength="80" />
          </label>

          <label class="field-label">
            Source URL
            <input v-model.trim="form.sourceUrl" class="field-control" name="sourceUrl" type="url" maxlength="500" />
          </label>

          <label class="field-label">
            Job Type
            <select v-model="form.jobType" class="field-select" name="jobType">
              <option value="INTERNSHIP">Internship</option>
              <option value="FULL_TIME">Full Time</option>
              <option value="CAMPUS">Campus</option>
            </select>
          </label>

          <label class="field-label">
            Education
            <select v-model="form.educationRequirement" class="field-select" name="educationRequirement">
              <option value="ANY">Any</option>
              <option value="BACHELOR">Bachelor</option>
              <option value="MASTER">Master</option>
              <option value="DOCTOR">Doctor</option>
            </select>
          </label>

          <label class="field-label">
            Source Platform
            <input v-model.trim="form.sourcePlatform" class="field-control" name="sourcePlatform" type="text" maxlength="50" />
          </label>

          <label class="field-label">
            Deadline
            <input v-model="form.deadlineAt" class="field-control" name="deadlineAt" type="datetime-local" />
          </label>

          <label class="field-label">
            Summary
            <textarea v-model.trim="form.summary" class="field-textarea" name="summary" maxlength="300" />
          </label>

          <label class="field-label">
            Content
            <textarea v-model.trim="form.content" class="field-textarea" name="content" maxlength="10000" />
          </label>

          <p v-if="actionMessage" class="field-hint">{{ actionMessage }}</p>
          <p v-if="errorMessage" class="field-error" role="alert">{{ errorMessage }}</p>

          <div class="inline-form-actions">
            <button type="submit" class="app-btn" :disabled="Boolean(actionLoadingId)">
              {{ selectedJobId ? "Save Draft" : "Create Draft" }}
            </button>
            <button type="button" class="ghost-btn" :disabled="Boolean(actionLoadingId)" @click="resetForm">
              Reset
            </button>
            <button
              v-if="selectedJobId"
              type="button"
              class="ghost-btn"
              :disabled="Boolean(actionLoadingId)"
              @click="handlePublish(selectedJobId)"
            >
              Publish Selected
            </button>
          </div>
        </form>
      </article>

      <article class="section-card">
        <div class="section-header">
          <div>
            <span class="section-eyebrow">Board</span>
            <h2 class="page-title" style="margin-top: 16px;">Current job cards</h2>
          </div>
        </div>

        <div v-if="loading" class="empty-state">Loading admin jobs...</div>
        <div v-else-if="errorMessage && !summary.jobs.length" class="field-grid">
          <p class="field-error" role="alert">{{ errorMessage }}</p>
          <button type="button" class="ghost-btn" @click="loadJobs">
            Retry
          </button>
        </div>
        <div v-else>
          <table class="app-table">
            <thead>
              <tr>
                <th>Title</th>
                <th>Company</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="job in summary.jobs" :key="job.id">
                <td>{{ job.title }}</td>
                <td>{{ job.companyName }}</td>
                <td>{{ job.status }}</td>
                <td>
                  <div class="inline-form-actions">
                    <button type="button" class="ghost-btn" @click="applyJobToForm(job)">
                      Edit
                    </button>
                    <button
                      type="button"
                      class="ghost-btn publish-action"
                      :disabled="job.status === 'PUBLISHED' || actionLoadingId === `publish-${job.id}`"
                      @click="handlePublish(job.id)"
                    >
                      Publish
                    </button>
                    <button
                      type="button"
                      class="ghost-btn offline-action"
                      :disabled="job.status !== 'PUBLISHED' || actionLoadingId === `offline-${job.id}`"
                      @click="handleOffline(job.id)"
                    >
                      Offline
                    </button>
                    <button
                      type="button"
                      class="danger-btn"
                      :disabled="job.status === 'DELETED' || actionLoadingId === `delete-${job.id}`"
                      @click="handleDelete(job.id)"
                    >
                      Delete
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>

          <div class="mobile-table-cards">
            <article
              v-for="job in summary.jobs"
              :key="`mobile-${job.id}`"
              class="table-card"
            >
              <strong>{{ job.title }}</strong>
              <p class="meta-copy">{{ job.companyName }} / {{ job.status }}</p>
              <div class="inline-form-actions" style="margin-top: 12px;">
                <button type="button" class="ghost-btn" @click="applyJobToForm(job)">
                  Edit
                </button>
                <button
                  type="button"
                  class="ghost-btn publish-action"
                  :disabled="job.status === 'PUBLISHED' || actionLoadingId === `publish-${job.id}`"
                  @click="handlePublish(job.id)"
                >
                  Publish
                </button>
                <button
                  type="button"
                  class="ghost-btn offline-action"
                  :disabled="job.status !== 'PUBLISHED' || actionLoadingId === `offline-${job.id}`"
                  @click="handleOffline(job.id)"
                >
                  Offline
                </button>
                <button
                  type="button"
                  class="danger-btn"
                  :disabled="job.status === 'DELETED' || actionLoadingId === `delete-${job.id}`"
                  @click="handleDelete(job.id)"
                >
                  Delete
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
.admin-jobs-stats {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.admin-jobs-stat {
  min-height: 128px;
  display: grid;
  gap: 8px;
  align-content: end;
}

.admin-jobs-stat__label {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.admin-jobs-stat strong {
  font-family: var(--cp-font-display);
  font-size: clamp(24px, 4vw, 32px);
}

.import-error-list {
  margin: 0;
  padding-left: 20px;
  color: var(--cp-danger, #b42318);
}

@media (max-width: 1023px) {
  .admin-jobs-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .admin-jobs-stats {
    grid-template-columns: 1fr;
  }
}
</style>
