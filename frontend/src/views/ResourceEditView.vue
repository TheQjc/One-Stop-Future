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
    return "当前还没有关联文件。";
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
    errorMessage.value = error.message || "资源详情加载失败，请稍后重试。";
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
    formMessage.value = "修改已提交，正在返回我的资源记录...";
    router.push("/profile/resources");
  } catch (error) {
    formError.value = error.message || "修改提交失败，请稍后重试。";
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
        <span class="section-eyebrow">修改并重提</span>
        <h1 class="hero-title" style="margin-top: 18px;">编辑并重新提交</h1>
        <hr class="editorial-rule" />
        <p class="hero-copy">
          优化元信息，如果原文件仍可用就继续沿用，再把这条记录送回审核，而不是额外新建一条资源记录。
        </p>
      </div>

      <div class="edit-hero__panel">
        <article class="panel-card">
          <strong>当前状态</strong>
          <p class="meta-copy" style="margin-top: 12px;">
            {{ detail?.status || "正在加载" }}
          </p>
        </article>
        <article class="panel-card edit-hero__note">
          <strong>审核说明</strong>
          <p class="meta-copy" style="margin-top: 12px;">
            {{ detail?.rejectReason || "暂无审核说明。" }}
          </p>
        </article>
        <article class="panel-card">
          <strong>当前文件</strong>
          <p class="meta-copy" style="margin-top: 12px;">
            {{ currentFileLabel }}
          </p>
        </article>
        <RouterLink to="/profile/resources" class="ghost-btn">
          返回我的记录
        </RouterLink>
      </div>
    </article>

    <article v-if="loading" class="section-card">
      <div class="empty-state">正在加载被退回的资源...</div>
    </article>

    <article v-else-if="errorMessage" class="section-card">
      <div class="field-grid">
        <p class="field-error" role="alert">{{ errorMessage }}</p>
        <button type="button" class="ghost-btn" @click="loadDetail">
          重试
        </button>
      </div>
    </article>

    <article v-else-if="!detail?.editableByMe" class="section-card">
      <div class="field-grid">
        <p class="field-error" role="alert">当前状态下，这条资源暂时不能编辑。</p>
        <p class="meta-copy">
          只有你本人被退回的资源，才能在这个页面继续修改并重新提交。
        </p>
        <div class="inline-form-actions">
          <RouterLink to="/profile/resources" class="ghost-btn">
            返回我的记录
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
      cancel-label="返回我的记录"
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
