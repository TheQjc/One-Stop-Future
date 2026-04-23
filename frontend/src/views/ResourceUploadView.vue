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
    formMessage.value = "资源已提交，正在返回我的资源记录...";
    router.push("/profile/resources");
  } catch (error) {
    formError.value = error.message || "上传失败，请稍后重试。";
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <section class="page-stack">
    <article class="section-card upload-hero">
      <div class="upload-hero__copy">
        <span class="section-eyebrow">资源投稿</span>
        <h1 class="hero-title" style="margin-top: 18px;">上传资源</h1>
        <hr class="editorial-rule" />
        <p class="hero-copy">
          先提交整理好的正式版本。这一页有意保持精简，方便审核流程优先核对标题、分类、摘要和文件质量。
        </p>
      </div>

      <div class="upload-hero__panel">
        <article class="panel-card">
          <strong>当前上传人</strong>
          <p class="meta-copy" style="margin-top: 12px;">
            {{ userStore.profile?.nickname || "当前登录用户" }}
          </p>
        </article>
        <article class="panel-card">
          <strong>支持格式</strong>
          <p class="meta-copy" style="margin-top: 12px;">
            PDF、DOCX、PPTX、ZIP，单个文件不超过 100MB
          </p>
        </article>
        <RouterLink to="/resources" class="ghost-btn">
          返回资源列表
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
      cancel-label="返回资源列表"
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
