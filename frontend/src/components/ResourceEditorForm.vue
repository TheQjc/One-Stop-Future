<script setup>
import { computed, reactive, ref, watch } from "vue";
import { RouterLink } from "vue-router";

const props = defineProps({
  mode: {
    type: String,
    required: true,
  },
  initialValue: {
    type: Object,
    default: () => ({
      title: "",
      category: "",
      summary: "",
      description: "",
    }),
  },
  currentFileLabel: {
    type: String,
    default: "",
  },
  submitting: {
    type: Boolean,
    default: false,
  },
  errorMessage: {
    type: String,
    default: "",
  },
  infoMessage: {
    type: String,
    default: "",
  },
  cancelTo: {
    type: String,
    required: true,
  },
  cancelLabel: {
    type: String,
    default: "取消",
  },
});

const emit = defineEmits(["submit"]);

const formError = ref("");
const form = reactive({
  title: "",
  category: "",
  summary: "",
  description: "",
  file: null,
});

const categoryOptions = [
  { value: "", label: "请选择分类" },
  { value: "EXAM_PAPER", label: "考试真题" },
  { value: "LANGUAGE_TEST", label: "语言考试" },
  { value: "RESUME_TEMPLATE", label: "简历模板" },
  { value: "INTERVIEW_EXPERIENCE", label: "面经资料" },
  { value: "OTHER", label: "其他" },
];

const modeCopy = computed(() => (
  props.mode === "edit"
    ? {
      eyebrow: "修改表单",
      title: "补齐元信息，只在确实需要时替换文件。",
      submitLabel: "提交修改",
      fileNote: "如果沿用原文件，可以把文件选择框留空。",
      emptyFileLabel: "继续沿用当前文件。",
    }
    : {
      eyebrow: "投稿表单",
      title: "把文件整理清楚，让审核更快完成。",
      submitLabel: "提交资源",
      fileNote: "请上传一份用于首轮审核的正式文件。",
      emptyFileLabel: "还没有选择文件。",
    }
));

const selectedFileLabel = computed(() => {
  if (form.file) {
    return `${form.file.name} / ${formatSize(form.file.size)}`;
  }

  if (props.currentFileLabel) {
    return props.currentFileLabel;
  }

  return modeCopy.value.emptyFileLabel;
});

watch(
  () => props.initialValue,
  (value) => {
    form.title = value?.title || "";
    form.category = value?.category || "";
    form.summary = value?.summary || "";
    form.description = value?.description || "";
    form.file = null;
    formError.value = "";
  },
  { immediate: true, deep: true },
);

function onFileChange(event) {
  form.file = event.target.files?.[0] || null;
}

function formatSize(value) {
  const size = Number(value || 0);
  if (!size) {
    return "0 B";
  }
  if (size >= 1024 * 1024) {
    return `${(size / (1024 * 1024)).toFixed(1)} MB`;
  }
  if (size >= 1024) {
    return `${Math.round(size / 1024)} KB`;
  }
  return `${size} B`;
}

function validateForm() {
  if (!form.title.trim()) {
    formError.value = "请输入标题。";
    return false;
  }
  if (!form.category) {
    formError.value = "请选择分类。";
    return false;
  }
  if (!form.summary.trim()) {
    formError.value = "请输入摘要。";
    return false;
  }
  if (props.mode !== "edit" && !form.file) {
    formError.value = "请上传文件。";
    return false;
  }
  if (!form.file) {
    return true;
  }

  const extension = form.file.name.split(".").pop()?.toLowerCase() || "";
  if (!["pdf", "docx", "pptx", "zip"].includes(extension)) {
    formError.value = "当前仅支持 PDF、DOCX、PPTX 和 ZIP 文件。";
    return false;
  }

  if (form.file.size > 100 * 1024 * 1024) {
    formError.value = "文件大小不能超过 100MB。";
    return false;
  }

  return true;
}

function submitForm() {
  formError.value = "";

  if (!validateForm()) {
    return;
  }

  const payload = new FormData();
  payload.append("title", form.title.trim());
  payload.append("category", form.category);
  payload.append("summary", form.summary.trim());
  payload.append("description", form.description.trim());
  if (form.file) {
    payload.append("file", form.file);
  }

  emit("submit", payload);
}
</script>

<template>
  <article class="section-card resource-editor-card">
    <div class="section-header resource-editor-card__header">
      <div>
        <span class="section-eyebrow">{{ modeCopy.eyebrow }}</span>
        <h2 class="page-title" style="margin-top: 16px;">{{ modeCopy.title }}</h2>
      </div>
    </div>

    <form class="resource-editor-form" @submit.prevent="submitForm">
      <div class="resource-editor-form__fields">
        <label class="field-label">
          标题
          <input
            v-model="form.title"
            class="field-control"
            name="title"
            type="text"
            placeholder="例如：2026 秋招简历模板包"
          />
        </label>

        <label class="field-label">
          分类
          <select v-model="form.category" class="field-select" name="category">
            <option v-for="option in categoryOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>

        <label class="field-label">
          摘要
          <textarea
            v-model="form.summary"
            class="field-textarea"
            name="summary"
            rows="4"
            placeholder="写一段列表页可直接展示的简短摘要。"
          />
        </label>

        <label class="field-label">
          详细说明
          <textarea
            v-model="form.description"
            class="field-textarea"
            name="description"
            rows="6"
            placeholder="补充背景、使用方式或准备建议。"
          />
        </label>

        <label class="field-label">
          {{ mode === "edit" ? "替换文件（可选）" : "文件" }}
          <input
            class="field-control"
            type="file"
            accept=".pdf,.docx,.pptx,.zip"
            @change="onFileChange"
          />
        </label>
      </div>

      <aside class="resource-editor-form__aside">
        <article class="panel-card resource-editor-note">
          <strong>当前文件</strong>
          <p class="meta-copy" style="margin-top: 12px;">{{ selectedFileLabel }}</p>
        </article>

        <article class="panel-card resource-editor-note resource-editor-note--lined">
          <strong>文件说明</strong>
          <p class="meta-copy" style="margin-top: 12px;">
            {{ modeCopy.fileNote }}
          </p>
          <p class="meta-copy" style="margin-top: 12px;">
            支持格式：PDF、DOCX、PPTX、ZIP，单个文件不超过 100MB
          </p>
        </article>
      </aside>

      <p v-if="infoMessage" class="field-hint">{{ infoMessage }}</p>
      <p v-if="errorMessage || formError" class="field-error" role="alert">
        {{ errorMessage || formError }}
      </p>

      <div class="inline-form-actions">
        <button type="submit" class="app-btn" :disabled="submitting">
          {{ submitting ? "提交中..." : modeCopy.submitLabel }}
        </button>
        <RouterLink :to="cancelTo" class="ghost-btn">
          {{ cancelLabel }}
        </RouterLink>
      </div>
    </form>
  </article>
</template>

<style scoped>
.resource-editor-card__header {
  margin-bottom: 0;
}

.resource-editor-form {
  display: grid;
  gap: var(--cp-gap-4);
}

.resource-editor-form__fields,
.resource-editor-form__aside {
  display: grid;
  gap: var(--cp-gap-4);
}

.resource-editor-note {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.72), rgba(255, 249, 240, 0.94)),
    radial-gradient(circle at top right, rgba(24, 38, 63, 0.08), transparent 38%);
}

.resource-editor-note--lined {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.72), rgba(255, 249, 240, 0.94)),
    repeating-linear-gradient(
      180deg,
      rgba(24, 38, 63, 0.04),
      rgba(24, 38, 63, 0.04) 1px,
      transparent 1px,
      transparent 26px
    );
}

@media (min-width: 1024px) {
  .resource-editor-form {
    grid-template-columns: minmax(0, 1.15fr) minmax(300px, 0.85fr);
    align-items: start;
  }

  .resource-editor-form > :nth-last-child(-n + 3) {
    grid-column: 1 / -1;
  }
}
</style>
