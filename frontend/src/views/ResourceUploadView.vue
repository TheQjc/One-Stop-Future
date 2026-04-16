<script setup>
import { ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import ResourceEditorForm from "../components/ResourceEditorForm.vue";
import { createResourceUpload } from "../api/resources.js";
import { useUserStore } from "../stores/user.js";

const router = useRouter();
const userStore = useUserStore();

const submitting = ref(false);
const formError = ref("");
const formMessage = ref("");

async function submitUpload(payload) {
  formError.value = "";
  formMessage.value = "";
  submitting.value = true;

  try {
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

    <ResourceEditorForm
      mode="create"
      :initial-value="{ title: '', category: '', summary: '', description: '' }"
      :submitting="submitting"
      :error-message="formError"
      :info-message="formMessage"
      cancel-to="/resources"
      cancel-label="Cancel"
      @submit="submitUpload"
    />
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

@media (max-width: 1023px) {
  .upload-hero {
    grid-template-columns: 1fr;
  }
}
</style>
