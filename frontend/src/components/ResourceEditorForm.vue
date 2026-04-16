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
    default: "Cancel",
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
  { value: "", label: "Select Category" },
  { value: "EXAM_PAPER", label: "Exam Paper" },
  { value: "LANGUAGE_TEST", label: "Language Test" },
  { value: "RESUME_TEMPLATE", label: "Resume Template" },
  { value: "INTERVIEW_EXPERIENCE", label: "Interview Notes" },
  { value: "OTHER", label: "Other" },
];

const modeCopy = computed(() => (
  props.mode === "edit"
    ? {
      eyebrow: "Revision Form",
      title: "Tighten the metadata, replace the file only if needed.",
      submitLabel: "Submit Revision",
      fileNote: "Leave the file field empty to keep the current archive file.",
      emptyFileLabel: "Keep the current file on record.",
    }
    : {
      eyebrow: "Submission Form",
      title: "Package the file so review is fast.",
      submitLabel: "Submit Upload",
      fileNote: "Add one file package for the first review pass.",
      emptyFileLabel: "No file selected yet.",
    }
));

const selectedFileLabel = computed(() => {
  if (form.file) {
    return `${form.file.name} · ${formatSize(form.file.size)}`;
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
    formError.value = "Title is required.";
    return false;
  }
  if (!form.category) {
    formError.value = "Category is required.";
    return false;
  }
  if (!form.summary.trim()) {
    formError.value = "Summary is required.";
    return false;
  }
  if (props.mode !== "edit" && !form.file) {
    formError.value = "File is required.";
    return false;
  }
  if (!form.file) {
    return true;
  }

  const extension = form.file.name.split(".").pop()?.toLowerCase() || "";
  if (!["pdf", "docx", "pptx", "zip"].includes(extension)) {
    formError.value = "Only PDF, DOCX, PPTX, and ZIP files are supported.";
    return false;
  }

  if (form.file.size > 100 * 1024 * 1024) {
    formError.value = "File size must stay under 100MB.";
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
          Title
          <input
            v-model="form.title"
            class="field-control"
            name="title"
            type="text"
            placeholder="e.g. 2026 Resume Template Pack"
          />
        </label>

        <label class="field-label">
          Category
          <select v-model="form.category" class="field-select" name="category">
            <option v-for="option in categoryOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>

        <label class="field-label">
          Summary
          <textarea
            v-model="form.summary"
            class="field-textarea"
            name="summary"
            rows="4"
            placeholder="Write the short archive summary readers should see in the list."
          />
        </label>

        <label class="field-label">
          Description
          <textarea
            v-model="form.description"
            class="field-textarea"
            name="description"
            rows="6"
            placeholder="Add context, usage notes, or preparation hints."
          />
        </label>

        <label class="field-label">
          {{ mode === "edit" ? "Replace File (Optional)" : "File" }}
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
          <strong>Current Selection</strong>
          <p class="meta-copy" style="margin-top: 12px;">{{ selectedFileLabel }}</p>
        </article>

        <article class="panel-card resource-editor-note resource-editor-note--lined">
          <strong>File Handling</strong>
          <p class="meta-copy" style="margin-top: 12px;">
            {{ modeCopy.fileNote }}
          </p>
          <p class="meta-copy" style="margin-top: 12px;">
            Accepted formats: PDF, DOCX, PPTX, ZIP · Max 100MB
          </p>
        </article>
      </aside>

      <p v-if="infoMessage" class="field-hint">{{ infoMessage }}</p>
      <p v-if="errorMessage || formError" class="field-error" role="alert">
        {{ errorMessage || formError }}
      </p>

      <div class="inline-form-actions">
        <button type="submit" class="app-btn" :disabled="submitting">
          {{ submitting ? "Submitting..." : modeCopy.submitLabel }}
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
