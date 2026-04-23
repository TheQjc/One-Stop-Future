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
    label: "简历总数",
    value: summary.value.total || summary.value.resumes.length,
  },
]);

function formatTime(value) {
  if (!value) {
    return "时间未知";
  }

  return String(value).replace("T", " ").slice(0, 16);
}

function formatSize(value) {
  const size = Number(value || 0);
  if (!size) {
    return "大小未知";
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
    errorMessage.value = error.message || "简历库加载失败，请稍后重试。";
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
    actionError.value = "请输入简历标题。";
    return;
  }

  if (!selectedFile.value) {
    actionError.value = "请先选择一份简历文件。";
    return;
  }

  const formData = new FormData();
  formData.append("title", form.title.trim());
  formData.append("file", selectedFile.value);

  uploading.value = true;

  try {
    await createResume(formData);
    actionMessage.value = "简历已上传。";
    resetForm();
    await loadResumes();
  } catch (error) {
    actionError.value = error.message || "简历上传失败，请稍后重试。";
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
    actionMessage.value = `已开始下载 ${fileName || resume.fileName || resume.title}。`;
  } catch (error) {
    actionError.value = error.message || "简历下载失败，请稍后重试。";
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
    actionError.value = error.message || "简历预览失败，请稍后重试。";
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
    actionMessage.value = `已删除 ${resume.title}。`;
    await loadResumes();
  } catch (error) {
    actionError.value = error.message || "简历删除失败，请稍后重试。";
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
          <span class="section-eyebrow">我的简历</span>
          <h1 class="page-title" style="margin-top: 16px;">简历库</h1>
          <p class="page-subtitle" style="margin-top: 16px;">
            把不同用途的投递简历集中放在这里，方便随时切换。当前阶段支持上传、预览、下载和删除。
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
            <span class="section-eyebrow">上传简历</span>
            <h2 class="page-title" style="margin-top: 16px;">新增一份简历</h2>
          </div>
        </div>

        <form class="field-grid" @submit.prevent="handleUpload">
          <label class="field-label">
            标题
            <input
              v-model.trim="form.title"
              class="field-control"
              name="title"
              type="text"
              maxlength="100"
              placeholder="例如：实习投递版简历"
            />
          </label>

          <label class="field-label">
            文件
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
            当前支持 PDF、DOC、DOCX。PDF 和 DOCX 可在线预览，DOC 仅支持下载。
          </p>
          <p v-if="actionMessage" class="field-hint">{{ actionMessage }}</p>
          <p v-if="actionError" class="field-error" role="alert">{{ actionError }}</p>

          <div class="inline-form-actions">
            <button type="submit" class="app-btn" :disabled="uploading">
              {{ uploading ? "上传中..." : "上传简历" }}
            </button>
          </div>
        </form>
      </article>

      <article class="section-card">
        <div class="section-header">
          <div>
            <span class="section-eyebrow">文件列表</span>
            <h2 class="page-title" style="margin-top: 16px;">已上传文件</h2>
          </div>
        </div>

        <div v-if="loading" class="empty-state">正在加载你的简历库...</div>
        <div v-else-if="errorMessage" class="field-grid">
          <p class="field-error" role="alert">{{ errorMessage }}</p>
          <button type="button" class="ghost-btn" @click="loadResumes">
            重试
          </button>
        </div>
        <div v-else-if="!summary.resumes.length" class="empty-state">
          你还没有上传任何简历。
        </div>
        <div v-else class="resume-record-list">
          <article
            v-for="resume in summary.resumes"
            :key="resume.id"
            class="panel-card resume-record-card"
          >
            <div class="resume-record-card__header">
              <div>
                <p class="resume-record-card__eyebrow">简历 #{{ resume.id }}</p>
                <h2 class="resume-record-card__title">{{ resume.title }}</h2>
              </div>
            </div>

            <div class="resume-record-card__meta">
              <span>{{ resume.fileName || "简历文件" }}</span>
              <span>{{ formatSize(resume.fileSize) }}</span>
              <span>上传于 {{ formatTime(resume.createdAt) }}</span>
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
                {{ actionLoadingId === `preview-${resume.id}` ? "预览中..." : "预览" }}
              </button>
              <button
                :data-testid="`download-resume-${resume.id}`"
                type="button"
                class="ghost-btn"
                :disabled="actionLoadingId === `download-${resume.id}`"
                @click="handleDownload(resume)"
              >
                {{ actionLoadingId === `download-${resume.id}` ? "准备下载中..." : "下载" }}
              </button>
              <button
                :data-testid="`delete-resume-${resume.id}`"
                type="button"
                class="danger-btn"
                :disabled="actionLoadingId === `delete-${resume.id}`"
                @click="handleDelete(resume)"
              >
                {{ actionLoadingId === `delete-${resume.id}` ? "删除中..." : "删除" }}
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
