<script setup>
import { computed, ref, watch } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import ResourceEditorForm from "../components/ResourceEditorForm.vue";
import { getResourceDetail, updateResource } from "../api/resources.js";

const route = useRoute();
const router = useRouter();

const loading = ref(true);
const errorMessage = ref("");
const formError = ref("");
const formMessage = ref("");
const submitting = ref(false);
const detail = ref(null);

const initialValue = computed(() => ({
  title: detail.value?.title || "",
  category: detail.value?.category || "",
  summary: detail.value?.summary || "",
  description: detail.value?.description || "",
}));

const currentFileLabel = computed(() => {
  if (!detail.value?.fileName) {
    return "No archived file is attached yet.";
  }

  return `${detail.value.fileName} · ${formatSize(detail.value.fileSize)}`;
});

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

async function loadDetail() {
  loading.value = true;
  errorMessage.value = "";
  formError.value = "";
  formMessage.value = "";

  try {
    detail.value = await getResourceDetail(route.params.id);
  } catch (error) {
    detail.value = null;
    errorMessage.value = error.message || "Resource detail loading failed. Please try again.";
  } finally {
    loading.value = false;
  }
}

async function handleSubmit(payload) {
  formError.value = "";
  formMessage.value = "";
  submitting.value = true;

  try {
    await updateResource(route.params.id, payload);
    formMessage.value = "Revision submitted. Redirecting to your resource records...";
    router.push("/profile/resources");
  } catch (error) {
    formError.value = error.message || "Revision failed. Please try again.";
  } finally {
    submitting.value = false;
  }
}

watch(() => route.params.id, loadDetail, { immediate: true });
</script>

<template>
  <section class="page-stack">
    <article class="section-card edit-hero">
      <div class="edit-hero__copy">
        <span class="section-eyebrow">Revision Intake</span>
        <h1 class="hero-title" style="margin-top: 18px;">Edit And Resubmit</h1>
        <hr class="editorial-rule" />
        <p class="hero-copy">
          Tighten the metadata, keep the file if it is still usable, and send the record back into
          review without creating a parallel archive entry.
        </p>
      </div>

      <div class="edit-hero__panel">
        <article class="panel-card">
          <strong>Current Status</strong>
          <p class="meta-copy" style="margin-top: 12px;">
            {{ detail?.status || "Loading" }}
          </p>
        </article>
        <article class="panel-card edit-hero__note">
          <strong>Review Note</strong>
          <p class="meta-copy" style="margin-top: 12px;">
            {{ detail?.rejectReason || "No review note available." }}
          </p>
        </article>
        <article class="panel-card">
          <strong>Current File</strong>
          <p class="meta-copy" style="margin-top: 12px;">
            {{ currentFileLabel }}
          </p>
        </article>
        <RouterLink to="/profile/resources" class="ghost-btn">
          Back To My Records
        </RouterLink>
      </div>
    </article>

    <article v-if="loading" class="section-card">
      <div class="empty-state">Loading the rejected resource...</div>
    </article>

    <article v-else-if="errorMessage" class="section-card">
      <div class="field-grid">
        <p class="field-error" role="alert">{{ errorMessage }}</p>
        <button type="button" class="ghost-btn" @click="loadDetail">
          Retry
        </button>
      </div>
    </article>

    <article v-else-if="!detail?.editableByMe" class="section-card">
      <div class="field-grid">
        <p class="field-error" role="alert">This resource cannot be edited in the current state.</p>
        <p class="meta-copy">
          Only rejected records owned by you can be revised from this screen.
        </p>
        <div class="inline-form-actions">
          <RouterLink to="/profile/resources" class="ghost-btn">
            Return To My Records
          </RouterLink>
        </div>
      </div>
    </article>

    <ResourceEditorForm
      v-else
      mode="edit"
      :initial-value="initialValue"
      :current-file-label="currentFileLabel"
      :submitting="submitting"
      :error-message="formError"
      :info-message="formMessage"
      cancel-to="/profile/resources"
      cancel-label="Back To My Records"
      @submit="handleSubmit"
    />
  </section>
</template>

<style scoped>
.edit-hero {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: var(--cp-gap-6);
  background:
    linear-gradient(180deg, rgba(255, 250, 243, 0.96), rgba(246, 238, 227, 0.98)),
    radial-gradient(circle at top left, rgba(197, 79, 45, 0.12), transparent 28%),
    radial-gradient(circle at bottom right, rgba(24, 38, 63, 0.14), transparent 36%);
}

.edit-hero__copy,
.edit-hero__panel {
  display: grid;
  gap: var(--cp-gap-4);
}

.edit-hero__note {
  background:
    linear-gradient(180deg, rgba(255, 247, 241, 0.84), rgba(255, 240, 236, 0.96)),
    radial-gradient(circle at top right, rgba(197, 79, 45, 0.14), transparent 36%);
}

@media (max-width: 1023px) {
  .edit-hero {
    grid-template-columns: 1fr;
  }
}
</style>
