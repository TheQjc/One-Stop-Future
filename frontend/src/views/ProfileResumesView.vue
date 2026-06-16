<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { createResume, deleteResume, downloadResume, getMyResumes, previewResume, updateResume } from "../api/resumes.js";

const loading = ref(true);
const errorMessage = ref("");
const actionMessage = ref("");
const actionError = ref("");
const uploading = ref(false);
const actionLoadingId = ref("");
const selectedFile = ref(null);
const fileInputRef = ref(null);
const editingResumeId = ref(null);
const editTitle = ref("");
const editFile = ref(null);
const summary = ref({
  total: 0,
  resumes: [],
});

const form = reactive({
  title: "",
});

const ALLOWED_RESUME_EXTENSIONS = new Set(["pdf", "doc", "docx"]);

const statCards = computed(() => [
  {
    label: "简历总数",
    value: summary.value.total || summary.value.resumes.length,
  },
]);
const selectedFileLabel = computed(() => selectedFile.value?.name || "还没有选择文件");
const editFileLabel = computed(() => editFile.value?.name || "未选择替换文件");

function formatTime(value) {
  if (!value) {
    return "时间未知";
  }

  return String(value).replace("T", " ").slice(0, 16);
}

function startEdit(resume) {
  editingResumeId.value = resume.id;
  editTitle.value = resume.title || "";
  editFile.value = null;
  actionError.value = "";
  actionMessage.value = "";
}

function cancelEdit() {
  editingResumeId.value = null;
  editTitle.value = "";
  editFile.value = null;
}

function handleEditFileChange(event) {
  editFile.value = event.target.files?.[0] || null;
}

async function handleUpdate(resume) {
  actionMessage.value = "";
  actionError.value = "";

  if (!editTitle.value.trim()) {
    actionError.value = "请输入简历标题。";
    return;
  }

  const formData = new FormData();
  formData.append("title", editTitle.value.trim());
  if (editFile.value) {
    formData.append("file", editFile.value);
  }

  actionLoadingId.value = `update-${resume.id}`;
  try {
    await updateResume(resume.id, formData);
    actionMessage.value = `已更新 ${editTitle.value.trim()}。`;
    cancelEdit();
    await loadResumes();
  } catch (error) {
    actionError.value = error.message || "简历更新失败，请稍后重试。";
  } finally {
    actionLoadingId.value = "";
  }
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

function isAllowedResumeFile(file) {
  const name = file?.name || "";
  const dotIndex = name.lastIndexOf(".");
  if (dotIndex < 0 || dotIndex === name.length - 1) {
    return false;
  }
  return ALLOWED_RESUME_EXTENSIONS.has(name.slice(dotIndex + 1).toLowerCase());
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

  if (!isAllowedResumeFile(selectedFile.value)) {
    actionError.value = "仅支持 PDF、DOC、DOCX 简历文件。";
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
            把不同用途的投递简历集中放在这里，上传、预览、下载和删除都可以直接处理。
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
            <span class="file-picker">
              <span
                class="ghost-btn file-picker__button"
                data-testid="resume-upload-file-label"
              >
                选择简历文件
              </span>
              <span
                class="file-picker__name"
                data-testid="resume-upload-file-name"
              >
                {{ selectedFileLabel }}
              </span>
              <input
                ref="fileInputRef"
                class="file-picker__input"
                name="file"
                type="file"
                accept=".pdf,.doc,.docx,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                @change="handleFileChange"
              />
            </span>
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

            <form
              v-if="editingResumeId === resume.id"
              class="field-grid resume-edit-form"
              @submit.prevent="handleUpdate(resume)"
            >
              <label class="field-label">
                标题
                <input
                  v-model.trim="editTitle"
                  class="field-control"
                  :data-testid="`edit-resume-title-${resume.id}`"
                  type="text"
                  maxlength="100"
                />
              </label>
              <label class="field-label">
                替换文件（可选）
                <span class="file-picker">
                  <span
                    class="ghost-btn file-picker__button"
                    :data-testid="`edit-resume-file-label-${resume.id}`"
                  >
                    选择替换文件
                  </span>
                  <span
                    class="file-picker__name"
                    :data-testid="`edit-resume-file-name-${resume.id}`"
                  >
                    {{ editFileLabel }}
                  </span>
                  <input
                    class="file-picker__input"
                    :data-testid="`edit-resume-file-${resume.id}`"
                    type="file"
                    accept=".pdf,.doc,.docx,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    @change="handleEditFileChange"
                  />
                </span>
              </label>
              <div class="inline-form-actions">
                <button
                  type="submit"
                  class="app-btn"
                  :data-testid="`save-resume-${resume.id}`"
                  :disabled="actionLoadingId === `update-${resume.id}`"
                >
                  {{ actionLoadingId === `update-${resume.id}` ? "保存中..." : "保存修改" }}
                </button>
                <button type="button" class="ghost-btn" @click="cancelEdit">
                  取消
                </button>
              </div>
            </form>

            <div class="inline-form-actions">
              <button
                :data-testid="`edit-resume-${resume.id}`"
                type="button"
                class="ghost-btn"
                @click="startEdit(resume)"
              >
                重命名 / 替换
              </button>
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

.resume-edit-form {
  padding-top: 4px;
}

.file-picker {
  position: relative;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.file-picker__button {
  cursor: pointer;
  flex: 0 0 auto;
}

.file-picker__name {
  min-width: 0;
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
  overflow-wrap: anywhere;
}

.file-picker__input {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
}

@media (max-width: 767px) {
  .profile-resume-stats {
    grid-template-columns: 1fr;
  }
}
</style>
