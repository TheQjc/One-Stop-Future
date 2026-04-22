<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { createResume, deleteResume, downloadResume, getMyResumes, previewResume } from "../api/resumes.js";

const loading = ref(true);
const errorMessage = ref("");
const actionMessage = ref("");
const actionError = ref("");
const uploading = ref(false);
const actionLoadingId = ref("");
const selectedFile = ref(null);
const fileInputRef = ref(null);
const summary = ref({
  total: 0,
  resumes: [],
});

const form = reactive({
  title: "",
});

const statCards = computed(() => [
  {
    label: "Total Resumes",
    value: summary.value.total || summary.value.resumes.length,
  },
]);

function formatTime(value) {
  if (!value) {
    return "Unknown time";
  }

  return String(value).replace("T", " ").slice(0, 16);
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

async function loadResumes() {
  loading.value = true;
  errorMessage.value = "";

  try {
    summary.value = await getMyResumes();
  } catch (error) {
    errorMessage.value = error.message || "Resume library loading failed. Please try again.";
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  form.title = "";
  selectedFile.value = null;

  if (fileInputRef.value) {
    fileInputRef.value.value = "";
  }
}

function handleFileChange(event) {
  selectedFile.value = event.target.files?.[0] || null;
}

async function handleUpload() {
  actionMessage.value = "";
  actionError.value = "";

  if (!form.title.trim()) {
    actionError.value = "Resume title is required.";
    return;
  }

  if (!selectedFile.value) {
    actionError.value = "Choose one resume file first.";
    return;
  }

  const formData = new FormData();
  formData.append("title", form.title.trim());
  formData.append("file", selectedFile.value);

  uploading.value = true;

  try {
    await createResume(formData);
    actionMessage.value = "Resume uploaded.";
    resetForm();
    await loadResumes();
  } catch (error) {
    actionError.value = error.message || "Resume upload failed. Please try again.";
  } finally {
    uploading.value = false;
  }
}

async function handleDownload(resume) {
  actionMessage.value = "";
  actionError.value = "";
  actionLoadingId.value = `download-${resume.id}`;

  try {
    const fileName = await downloadResume(resume.id);
    actionMessage.value = `Download started for ${fileName || resume.fileName || resume.title}.`;
  } catch (error) {
    actionError.value = error.message || "Resume download failed. Please try again.";
  } finally {
    actionLoadingId.value = "";
  }
}

async function handlePreview(resume) {
  actionMessage.value = "";
  actionError.value = "";
  actionLoadingId.value = `preview-${resume.id}`;

  try {
    await previewResume(resume.id);
  } catch (error) {
    actionError.value = error.message || "Resume preview failed. Please try again.";
  } finally {
    actionLoadingId.value = "";
  }
}

async function handleDelete(resume) {
  actionMessage.value = "";
  actionError.value = "";
  actionLoadingId.value = `delete-${resume.id}`;

  try {
    await deleteResume(resume.id);
    actionMessage.value = `Deleted ${resume.title}.`;
    await loadResumes();
  } catch (error) {
    actionError.value = error.message || "Resume delete failed. Please try again.";
  } finally {
    actionLoadingId.value = "";
  }
}

onMounted(loadResumes);
</script>

<template>
  <section class="page-stack">
    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">My Resumes</span>
          <h1 class="page-title" style="margin-top: 16px;">Resume library</h1>
          <p class="page-subtitle" style="margin-top: 16px;">
            Keep multiple application files ready on one calm board. This phase supports upload,
            download, and delete only.
          </p>
        </div>
      </div>

      <div class="stats-grid profile-resume-stats">
        <article
          v-for="card in statCards"
          :key="card.label"
          class="panel-card profile-resume-stat"
        >
          <span class="profile-resume-stat__label">{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
        </article>
      </div>
    </article>

    <div class="dashboard-grid">
      <article class="section-card">
        <div class="section-header">
          <div>
            <span class="section-eyebrow">Upload</span>
            <h2 class="page-title" style="margin-top: 16px;">Add one resume file</h2>
          </div>
        </div>

        <form class="field-grid" @submit.prevent="handleUpload">
          <label class="field-label">
            Title
            <input
              v-model.trim="form.title"
              class="field-control"
              name="title"
              type="text"
              maxlength="100"
              placeholder="Intern Resume"
            />
          </label>

          <label class="field-label">
            File
            <input
              ref="fileInputRef"
              class="field-control"
              name="file"
              type="file"
              accept=".pdf,.doc,.docx,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
              @change="handleFileChange"
            />
          </label>

          <p class="field-hint">
            Supported in this phase: PDF, DOC, and DOCX. PDF and DOCX files support online preview;
            DOC remains download-only.
          </p>
          <p v-if="actionMessage" class="field-hint">{{ actionMessage }}</p>
          <p v-if="actionError" class="field-error" role="alert">{{ actionError }}</p>

          <div class="inline-form-actions">
            <button type="submit" class="app-btn" :disabled="uploading">
              {{ uploading ? "Uploading..." : "Upload Resume" }}
            </button>
          </div>
        </form>
      </article>

      <article class="section-card">
        <div class="section-header">
          <div>
            <span class="section-eyebrow">Library</span>
            <h2 class="page-title" style="margin-top: 16px;">Current files</h2>
          </div>
        </div>

        <div v-if="loading" class="empty-state">Loading your resume library...</div>
        <div v-else-if="errorMessage" class="field-grid">
          <p class="field-error" role="alert">{{ errorMessage }}</p>
          <button type="button" class="ghost-btn" @click="loadResumes">
            Retry
          </button>
        </div>
        <div v-else-if="!summary.resumes.length" class="empty-state">
          You have not uploaded any resumes yet.
        </div>
        <div v-else class="resume-record-list">
          <article
            v-for="resume in summary.resumes"
            :key="resume.id"
            class="panel-card resume-record-card"
          >
            <div class="resume-record-card__header">
              <div>
                <p class="resume-record-card__eyebrow">Resume #{{ resume.id }}</p>
                <h2 class="resume-record-card__title">{{ resume.title }}</h2>
              </div>
            </div>

            <div class="resume-record-card__meta">
              <span>{{ resume.fileName || "Resume file" }}</span>
              <span>{{ formatSize(resume.fileSize) }}</span>
              <span>Uploaded {{ formatTime(resume.createdAt) }}</span>
            </div>

            <div class="inline-form-actions">
              <button
                v-if="resume.previewAvailable && resume.previewKind === 'FILE'"
                :data-testid="`preview-resume-${resume.id}`"
                type="button"
                class="ghost-btn"
                :disabled="actionLoadingId === `preview-${resume.id}`"
                @click="handlePreview(resume)"
              >
                {{ actionLoadingId === `preview-${resume.id}` ? "Opening Preview..." : "Preview" }}
              </button>
              <button
                :data-testid="`download-resume-${resume.id}`"
                type="button"
                class="ghost-btn"
                :disabled="actionLoadingId === `download-${resume.id}`"
                @click="handleDownload(resume)"
              >
                {{ actionLoadingId === `download-${resume.id}` ? "Preparing Download..." : "Download" }}
              </button>
              <button
                :data-testid="`delete-resume-${resume.id}`"
                type="button"
                class="danger-btn"
                :disabled="actionLoadingId === `delete-${resume.id}`"
                @click="handleDelete(resume)"
              >
                {{ actionLoadingId === `delete-${resume.id}` ? "Deleting..." : "Delete" }}
              </button>
            </div>
          </article>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.profile-resume-stats {
  grid-template-columns: minmax(0, 240px);
}

.profile-resume-stat {
  min-height: 128px;
  display: grid;
  gap: 8px;
  align-content: end;
}

.profile-resume-stat__label {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.profile-resume-stat strong {
  font-family: var(--cp-font-display);
  font-size: clamp(24px, 4vw, 32px);
}

.resume-record-list {
  display: grid;
  gap: var(--cp-gap-4);
}

.resume-record-card {
  display: grid;
  gap: var(--cp-gap-4);
}

.resume-record-card__header {
  display: flex;
  justify-content: space-between;
  gap: var(--cp-gap-4);
  align-items: start;
}

.resume-record-card__eyebrow {
  margin: 0;
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.resume-record-card__title {
  margin: 8px 0 0;
  font-family: var(--cp-font-display);
  font-size: clamp(24px, 4vw, 30px);
  line-height: 1.14;
}

.resume-record-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cp-gap-3);
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

@media (max-width: 767px) {
  .profile-resume-stats {
    grid-template-columns: 1fr;
  }
}
</style>
