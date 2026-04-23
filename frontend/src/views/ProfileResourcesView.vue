<script setup>
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import ResourceZipPreviewPanel from "../components/ResourceZipPreviewPanel.vue";
import { getMyResources, previewResource, previewZipResource } from "../api/resources.js";

const loading = ref(true);
const errorMessage = ref("");
const actionError = ref("");
const actionMessage = ref("");
const actionLoadingId = ref("");
const zipPreviewError = ref("");
const zipPreviewResourceId = ref(null);
const zipPreviewData = ref(null);
const summary = ref({
  total: 0,
  resources: [],
});

const statusCards = computed(() => {
  const resources = summary.value.resources || [];

  return [
    { label: "全部记录", value: resources.length },
    { label: "待审核", value: resources.filter((item) => item.status === "PENDING").length },
    { label: "已发布", value: resources.filter((item) => item.status === "PUBLISHED").length },
    {
      label: "待处理",
      value: resources.filter((item) => ["REJECTED", "OFFLINE"].includes(item.status)).length,
    },
  ];
});

function formatCategory(category) {
  const labelMap = {
    EXAM_PAPER: "考试真题",
    LANGUAGE_TEST: "语言考试",
    RESUME_TEMPLATE: "简历模板",
    INTERVIEW_EXPERIENCE: "面经资料",
    OTHER: "其他",
  };

  return labelMap[category] || category || "资源";
}

function formatTime(value) {
  if (!value) {
    return "暂未发布";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "暂未发布";
  }

  return new Intl.DateTimeFormat("zh-CN", {
    month: "numeric",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
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

function statusClass(status) {
  if (status === "PUBLISHED") {
    return "approved";
  }
  if (status === "REJECTED") {
    return "rejected";
  }
  return "pending";
}

function statusLabel(status) {
  const labels = {
    PENDING: "待审核",
    PUBLISHED: "已发布",
    REJECTED: "已退回",
    OFFLINE: "已下线",
  };

  return labels[status] || status || "处理中";
}

function previewKindOf(resource) {
  const kind = resource?.previewKind;
  if (kind === "FILE" || kind === "ZIP_TREE" || kind === "NONE") {
    return kind;
  }
  return resource?.previewAvailable ? "FILE" : "NONE";
}

function canPreview(resource) {
  const kind = previewKindOf(resource);
  return kind === "FILE" || kind === "ZIP_TREE";
}

async function loadResources() {
  loading.value = true;
  errorMessage.value = "";
  zipPreviewError.value = "";
  zipPreviewResourceId.value = null;
  zipPreviewData.value = null;

  try {
    summary.value = await getMyResources();
  } catch (error) {
    errorMessage.value = error.message || "资源记录加载失败，请稍后重试。";
  } finally {
    loading.value = false;
  }
}

async function handlePreview(resource) {
  const kind = previewKindOf(resource);
  if (kind === "NONE") {
    return;
  }

  actionError.value = "";
  actionMessage.value = "";
  actionLoadingId.value = `preview-${resource.id}`;
  zipPreviewError.value = "";

  try {
    if (kind === "ZIP_TREE") {
      zipPreviewData.value = await previewZipResource(resource.id);
      zipPreviewResourceId.value = resource.id;
      actionMessage.value = `已加载《${resource.title}》的目录内容。`;
      return;
    }

    zipPreviewResourceId.value = null;
    zipPreviewData.value = null;
    await previewResource(resource.id);
    actionMessage.value = `已打开《${resource.title}》的预览。`;
  } catch (error) {
    if (kind === "ZIP_TREE") {
      zipPreviewResourceId.value = resource.id;
      zipPreviewError.value = error.message || "目录预览失败，请稍后重试。";
    }
    actionError.value = error.message || "预览失败，请稍后重试。";
  } finally {
    actionLoadingId.value = "";
  }
}

function previewLabel(resource) {
  return previewKindOf(resource) === "ZIP_TREE" ? "查看目录" : "预览";
}

function showZipPreview(resource) {
  return previewKindOf(resource) === "ZIP_TREE" && zipPreviewResourceId.value === resource.id;
}

onMounted(loadResources);
</script>

<template>
  <section class="page-stack">
    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">我的资源</span>
          <h1 class="page-title" style="margin-top: 16px;">资源记录</h1>
          <p class="page-subtitle" style="margin-top: 16px;">
            这里先集中展示状态、发布时间和退回说明，方便你快速回看每条资源的进展，再决定是否需要修改重提。
          </p>
        </div>
        <RouterLink to="/resources/upload" class="app-btn">
          上传资源
        </RouterLink>
      </div>

      <div class="stats-grid profile-resource-stats">
        <article
          v-for="card in statusCards"
          :key="card.label"
          class="panel-card profile-resource-stat"
        >
          <span class="profile-resource-stat__label">{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
        </article>
      </div>
    </article>

    <article class="section-card">
      <div v-if="loading" class="empty-state">正在加载你的资源记录...</div>
      <div v-else-if="errorMessage" class="field-grid">
        <p class="field-error" role="alert">{{ errorMessage }}</p>
        <button type="button" class="ghost-btn" @click="loadResources">
          重试
        </button>
      </div>
      <div v-else-if="!summary.resources.length" class="empty-state">
        你还没有上传任何资源文件。
      </div>
      <div v-else class="resource-record-list">
        <p v-if="actionMessage" class="field-hint">{{ actionMessage }}</p>
        <p v-if="actionError" class="field-error" role="alert">{{ actionError }}</p>

        <article
          v-for="resource in summary.resources"
          :key="resource.id"
          class="panel-card resource-record-card"
        >
          <div class="resource-record-card__header">
            <div>
              <p class="resource-record-card__eyebrow">资源 #{{ resource.id }}</p>
              <h2 class="resource-record-card__title">{{ resource.title }}</h2>
            </div>
            <span class="status-badge" :class="statusClass(resource.status)">
              {{ statusLabel(resource.status) }}
            </span>
          </div>

          <div class="resource-record-card__meta">
            <span>{{ formatCategory(resource.category) }}</span>
            <span>{{ resource.fileName || "资源文件" }}</span>
            <span>{{ formatSize(resource.fileSize) }}</span>
            <span>创建于 {{ formatTime(resource.createdAt) }}</span>
            <span>发布于 {{ formatTime(resource.publishedAt) }}</span>
          </div>

          <p class="meta-copy">
            {{ resource.summary || "这条记录暂时没有摘要。" }}
          </p>

          <div
            v-if="resource.editable || canPreview(resource)"
            class="inline-form-actions resource-record-card__actions"
          >
            <RouterLink
              v-if="resource.editable"
              :to="`/resources/${resource.id}/edit`"
              class="app-link"
            >
              编辑并重新提交
            </RouterLink>
            <button
              v-if="canPreview(resource)"
              type="button"
              class="ghost-btn preview-action"
              :data-testid="`preview-action-${resource.id}`"
              :disabled="actionLoadingId === `preview-${resource.id}`"
              @click="handlePreview(resource)"
            >
              {{ actionLoadingId === `preview-${resource.id}`
                ? (previewKindOf(resource) === "ZIP_TREE" ? "正在加载目录..." : "正在打开预览...")
                : previewLabel(resource) }}
            </button>
          </div>

          <ResourceZipPreviewPanel
            v-if="showZipPreview(resource) && (zipPreviewData || zipPreviewError || actionLoadingId === `preview-${resource.id}`)"
            :loading="actionLoadingId === `preview-${resource.id}`"
            :error-message="showZipPreview(resource) ? zipPreviewError : ''"
            :preview="showZipPreview(resource) ? zipPreviewData : null"
          />

          <article
            v-if="resource.rejectReason"
            class="panel-card resource-record-card__note"
          >
            <strong>审核说明</strong>
            <p class="meta-copy" style="margin-top: 12px;">{{ resource.rejectReason }}</p>
          </article>
        </article>
      </div>
    </article>
  </section>
</template>

<style scoped>
.profile-resource-stats {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-top: 24px;
}

.profile-resource-stat {
  min-height: 128px;
  display: grid;
  gap: 8px;
  align-content: end;
}

.profile-resource-stat__label {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.profile-resource-stat strong {
  font-family: var(--cp-font-display);
  font-size: clamp(24px, 4vw, 32px);
}

.resource-record-list {
  display: grid;
  gap: var(--cp-gap-4);
}

.resource-record-card {
  display: grid;
  gap: var(--cp-gap-4);
}

.resource-record-card__actions {
  margin-top: -4px;
}

.resource-record-card__header {
  display: flex;
  justify-content: space-between;
  gap: var(--cp-gap-4);
  align-items: start;
}

.resource-record-card__eyebrow {
  margin: 0;
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.resource-record-card__title {
  margin: 8px 0 0;
  font-family: var(--cp-font-display);
  font-size: clamp(24px, 4vw, 30px);
  line-height: 1.14;
}

.resource-record-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cp-gap-3);
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.resource-record-card__note {
  background:
    linear-gradient(180deg, rgba(255, 247, 241, 0.84), rgba(255, 240, 236, 0.96)),
    radial-gradient(circle at top right, rgba(197, 79, 45, 0.14), transparent 36%);
}

@media (max-width: 1023px) {
  .profile-resource-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .profile-resource-stats {
    grid-template-columns: 1fr;
  }

  .resource-record-card__header {
    flex-direction: column;
  }
}
</style>
