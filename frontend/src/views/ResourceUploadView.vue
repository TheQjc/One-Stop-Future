<script setup>
import { computed, reactive, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import { createResourceUpload } from "../api/resources.js";
import { useUserStore } from "../stores/user.js";

const router = useRouter();
const userStore = useUserStore();

const submitting = ref(false);
const formError = ref("");
const formMessage = ref("");
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

const selectedFileLabel = computed(() => {
  if (!form.file) {
    return "No file selected yet.";
  }

  return `${form.file.name} · ${formatSize(form.file.size)}`;
});

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
  if (!form.file) {
    formError.value = "File is required.";
    return false;
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

async function submitUpload() {
  formError.value = "";
  formMessage.value = "";

  if (!validateForm()) {
    return;
  }

  submitting.value = true;

  try {
    const payload = new FormData();
    payload.append("title", form.title.trim());
    payload.append("category", form.category);
    payload.append("summary", form.summary.trim());
    payload.append("description", form.description.trim());
    payload.append("file", form.file);

    await createResourceUpload(payload);
    formMessage.value = "Upload submitted. Redirecting to your resource records...";
    router.push("/profile/resources");
  } catch (error) {
    formError.value = error.message || "Upload failed. Please try again.";
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <section class="page-stack">
    <article class="section-card upload-hero">
      <div class="upload-hero__copy">
        <span class="section-eyebrow">Archive Intake</span>
        <h1 class="hero-title" style="margin-top: 18px;">Upload Resource</h1>
        <hr class="editorial-rule" />
        <p class="hero-copy">
          Submit the clean version first. This form is intentionally narrow so the review flow
          can keep title, category, summary, and file quality aligned before anything is published.
        </p>
      </div>

      <div class="upload-hero__panel">
        <article class="panel-card">
          <strong>Current Uploader</strong>
          <p class="meta-copy" style="margin-top: 12px;">
            {{ userStore.profile?.nickname || "Signed-in user" }}
          </p>
        </article>
        <article class="panel-card">
          <strong>Accepted Formats</strong>
          <p class="meta-copy" style="margin-top: 12px;">
            PDF, DOCX, PPTX, ZIP · Max 100MB
          </p>
        </article>
        <RouterLink to="/resources" class="ghost-btn">
          Back To Archive
        </RouterLink>
      </div>
    </article>

    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">Submission Form</span>
          <h2 class="page-title" style="margin-top: 16px;">Package the file so review is fast.</h2>
        </div>
      </div>

      <form class="field-grid" @submit.prevent="submitUpload">
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
            class="field-control"
            name="summary"
            rows="4"
            placeholder="Write the short archive summary readers should see in the list."
          />
        </label>

        <label class="field-label">
          Description
          <textarea
            v-model="form.description"
            class="field-control"
            name="description"
            rows="6"
            placeholder="Add context, usage notes, or preparation hints."
          />
        </label>

        <label class="field-label">
          File
          <input
            class="field-control"
            type="file"
            accept=".pdf,.docx,.pptx,.zip"
            @change="onFileChange"
          />
        </label>

        <article class="panel-card upload-note">
          <strong>Selected File</strong>
          <p class="meta-copy" style="margin-top: 12px;">{{ selectedFileLabel }}</p>
        </article>

        <p v-if="formMessage" class="field-hint">{{ formMessage }}</p>
        <p v-if="formError" class="field-error" role="alert">{{ formError }}</p>

        <div class="inline-form-actions">
          <button type="submit" class="app-btn" :disabled="submitting">
            {{ submitting ? "Submitting..." : "Submit Upload" }}
          </button>
          <RouterLink to="/resources" class="ghost-btn">
            Cancel
          </RouterLink>
        </div>
      </form>
    </article>
  </section>
</template>

<style scoped>
.upload-hero {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: var(--cp-gap-6);
  background:
    linear-gradient(180deg, rgba(255, 251, 244, 0.94), rgba(247, 240, 229, 0.98)),
    radial-gradient(circle at top left, rgba(24, 38, 63, 0.12), transparent 28%),
    radial-gradient(circle at bottom right, rgba(184, 130, 35, 0.18), transparent 34%);
}

.upload-hero__copy,
.upload-hero__panel {
  display: grid;
  gap: var(--cp-gap-4);
}

.upload-note {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.72), rgba(255, 249, 240, 0.94)),
    repeating-linear-gradient(
      180deg,
      rgba(24, 38, 63, 0.04),
      rgba(24, 38, 63, 0.04) 1px,
      transparent 1px,
      transparent 28px
    );
}

@media (max-width: 1023px) {
  .upload-hero {
    grid-template-columns: 1fr;
  }
}
</style>
