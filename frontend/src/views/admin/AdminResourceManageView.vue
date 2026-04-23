<script setup>
import { computed, onMounted, ref } from "vue";
import ResourceZipPreviewPanel from "../../components/ResourceZipPreviewPanel.vue";
import {
  getAdminResources,
  offlineAdminResource,
  publishAdminResource,
  rejectAdminResource,
} from "../../api/admin.js";
import { previewResource, previewZipResource } from "../../api/resources.js";

const loading = ref(true);
const errorMessage = ref("");
const actionMessage = ref("");
const actionLoadingId = ref("");
const selectedResourceId = ref(null);
const rejectReason = ref("");
const zipPreviewError = ref("");
const zipPreviewData = ref(null);
const zipPreviewResourceId = ref(null);
const summary = ref({
  total: 0,
  resources: [],
});

const selectedResource = computed(() => (
  summary.value.resources.find((item) => item.id === selectedResourceId.value) || null
));

const statusCards = computed(() => {
  const resources = summary.value.resources || [];

  return [
    { label: "全部资源", value: resources.length },
    { label: "待审核", value: resources.filter((item) => item.status === "PENDING").length },
    { label: "已发布", value: resources.filter((item) => item.status === "PUBLISHED").length },
    {
      label: "已关闭",
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
    return "待处理";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "待处理";
  }

  return new Intl.DateTimeFormat("zh-CN", {
    month: "numeric",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
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
    REJECTED: "已驳回",
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

  try {
    summary.value = await getAdminResources();
  } catch (error) {
    errorMessage.value = error.message || "资源审核列表加载失败，请稍后重试。";
  } finally {
    loading.value = false;
  }
}

function selectResource(resource) {
  selectedResourceId.value = resource.id;
  rejectReason.value = resource.rejectReason || "";
  actionMessage.value = "";
  errorMessage.value = "";
  if (zipPreviewResourceId.value !== resource.id) {
    zipPreviewData.value = null;
    zipPreviewError.value = "";
  }
}

function clearSelection() {
  selectedResourceId.value = null;
  rejectReason.value = "";
  zipPreviewError.value = "";
  zipPreviewData.value = null;
  zipPreviewResourceId.value = null;
}

async function handlePublish(id) {
  actionMessage.value = "";
  errorMessage.value = "";
  actionLoadingId.value = `publish-${id}`;

  try {
    await publishAdminResource(id);
    actionMessage.value = "资源已发布。";
    await loadResources();
  } catch (error) {
    errorMessage.value = error.message || "发布失败，请稍后重试。";
  } finally {
    actionLoadingId.value = "";
  }
}

async function handleOffline(id) {
  actionMessage.value = "";
  errorMessage.value = "";
  actionLoadingId.value = `offline-${id}`;

  try {
    await offlineAdminResource(id);
    actionMessage.value = "资源已下线。";
    await loadResources();
  } catch (error) {
    errorMessage.value = error.message || "下线失败，请稍后重试。";
  } finally {
    actionLoadingId.value = "";
  }
}

async function handleReject() {
  if (!selectedResource.value) {
    errorMessage.value = "请先选择一条待审核资源。";
    return;
  }

  if (!rejectReason.value.trim()) {
    errorMessage.value = "请填写驳回原因。";
    return;
  }

  actionMessage.value = "";
  errorMessage.value = "";
  actionLoadingId.value = `reject-${selectedResource.value.id}`;

  try {
    await rejectAdminResource(selectedResource.value.id, {
      reason: rejectReason.value.trim(),
    });
    actionMessage.value = "资源已驳回。";
    await loadResources();
  } catch (error) {
    errorMessage.value = error.message || "驳回失败，请稍后重试。";
  } finally {
    actionLoadingId.value = "";
  }
}

async function handlePreview(id) {
  const resource = summary.value.resources.find((item) => item.id === id);
  const kind = previewKindOf(resource);
  if (kind === "NONE") {
    return;
  }

  actionMessage.value = "";
  errorMessage.value = "";
  actionLoadingId.value = `preview-${id}`;
  zipPreviewError.value = "";

  try {
    if (kind === "ZIP_TREE") {
      selectedResourceId.value = id;
      rejectReason.value = resource.rejectReason || "";
      zipPreviewData.value = await previewZipResource(id);
      zipPreviewResourceId.value = id;
      actionMessage.value = `已加载《${resource?.title || `资源 #${id}` }》的目录内容。`;
      return;
    }

    zipPreviewData.value = null;
    zipPreviewResourceId.value = null;
    await previewResource(id);
    actionMessage.value = `已打开《${resource?.title || `资源 #${id}` }》的预览。`;
  } catch (error) {
    if (kind === "ZIP_TREE") {
      zipPreviewResourceId.value = id;
      zipPreviewError.value = error.message || "目录预览失败，请稍后重试。";
    }
    errorMessage.value = error.message || "预览失败，请稍后重试。";
  } finally {
    actionLoadingId.value = "";
  }
}

function previewLabel(resource) {
  return previewKindOf(resource) === "ZIP_TREE" ? "查看目录" : "预览";
}

const showSelectedZipPreview = computed(() => (
  previewKindOf(selectedResource.value) === "ZIP_TREE"
    && zipPreviewResourceId.value === selectedResource.value?.id
));

onMounted(loadResources);
</script>

<template>
  <section class="page-stack">
    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">资源审核</span>
          <h1 class="page-title" style="margin-top: 16px;">在一个页面处理资源审核</h1>
          <p class="page-subtitle" style="margin-top: 16px;">
            这个页面集中处理资源审核动作，包括选择记录、发布、填写原因驳回，以及下线已发布资源。
          </p>
        </div>
      </div>

      <div class="stats-grid admin-resource-stats">
        <article
          v-for="card in statusCards"
          :key="card.label"
          class="panel-card admin-resource-stat"
        >
          <span class="admin-resource-stat__label">{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
        </article>
      </div>

      <p v-if="actionMessage" class="field-hint" style="margin-top: 20px;">{{ actionMessage }}</p>
      <p v-if="errorMessage && !loading" class="field-error" role="alert" style="margin-top: 12px;">
        {{ errorMessage }}
      </p>
    </article>

    <div class="dashboard-grid">
      <article class="section-card">
        <div class="section-header">
          <div>
            <span class="section-eyebrow">当前选中</span>
            <h2 class="page-title" style="margin-top: 16px;">审核面板</h2>
          </div>
        </div>

        <div v-if="!selectedResource" class="empty-state">
          从右侧列表选择一条资源后，就可以在这里审核、驳回或发布。
        </div>
        <div v-else class="field-grid">
          <article class="panel-card selected-resource-card">
            <div class="selected-resource-card__header">
              <div>
                <p class="selected-resource-card__eyebrow">资源 #{{ selectedResource.id }}</p>
                <h3 class="selected-resource-card__title">{{ selectedResource.title }}</h3>
              </div>
              <span class="status-badge" :class="statusClass(selectedResource.status)">
                {{ statusLabel(selectedResource.status) }}
              </span>
            </div>

            <div class="selected-resource-card__meta">
              <span>{{ formatCategory(selectedResource.category) }}</span>
              <span>{{ selectedResource.uploaderNickname || "未知上传者" }}</span>
              <span>{{ selectedResource.fileName || "资源文件" }}</span>
              <span>创建于 {{ formatTime(selectedResource.createdAt) }}</span>
              <span>审核于 {{ formatTime(selectedResource.reviewedAt) }}</span>
            </div>

            <p v-if="selectedResource.rejectReason" class="meta-copy">
              当前审核说明：{{ selectedResource.rejectReason }}
            </p>
          </article>

          <label v-if="selectedResource.status === 'PENDING'" class="field-label">
            驳回原因
            <textarea
              v-model.trim="rejectReason"
              class="field-textarea"
              name="rejectReason"
              placeholder="请说明发布前还需要修改的内容。"
            />
          </label>

          <div class="inline-form-actions">
            <button
              v-if="canPreview(selectedResource)"
              type="button"
              class="ghost-btn preview-action"
              data-testid="selected-preview-action"
              :disabled="Boolean(actionLoadingId)"
              @click="handlePreview(selectedResource.id)"
            >
              {{ actionLoadingId === `preview-${selectedResource.id}`
                ? (previewKindOf(selectedResource) === "ZIP_TREE" ? "正在加载目录..." : "正在打开预览...")
                : previewLabel(selectedResource) }}
            </button>
            <button
              v-if="selectedResource.status === 'PENDING' || selectedResource.status === 'OFFLINE'"
              type="button"
              class="app-btn publish-action"
              :disabled="Boolean(actionLoadingId)"
              @click="handlePublish(selectedResource.id)"
            >
              {{ actionLoadingId === `publish-${selectedResource.id}` ? "发布中..." : "发布" }}
            </button>
            <button
              v-if="selectedResource.status === 'PENDING'"
              type="button"
              class="danger-btn reject-action"
              :disabled="Boolean(actionLoadingId)"
              @click="handleReject"
            >
              {{ actionLoadingId === `reject-${selectedResource.id}` ? "驳回中..." : "驳回" }}
            </button>
            <button
              v-if="selectedResource.status === 'PUBLISHED'"
              type="button"
              class="ghost-btn offline-action"
              :disabled="Boolean(actionLoadingId)"
              @click="handleOffline(selectedResource.id)"
            >
              {{ actionLoadingId === `offline-${selectedResource.id}` ? "下线中..." : "下线" }}
            </button>
            <button type="button" class="ghost-btn" :disabled="Boolean(actionLoadingId)" @click="clearSelection">
              清空选择
            </button>
          </div>

          <ResourceZipPreviewPanel
            v-if="showSelectedZipPreview || (previewKindOf(selectedResource) === 'ZIP_TREE' && actionLoadingId === `preview-${selectedResource.id}`)"
            :loading="actionLoadingId === `preview-${selectedResource.id}`"
            :error-message="showSelectedZipPreview ? zipPreviewError : ''"
            :preview="showSelectedZipPreview ? zipPreviewData : null"
          />
        </div>
      </article>

      <article class="section-card">
        <div class="section-header">
          <div>
            <span class="section-eyebrow">资源队列</span>
            <h2 class="page-title" style="margin-top: 16px;">当前资源记录</h2>
          </div>
        </div>

        <div v-if="loading" class="empty-state">正在加载资源审核列表...</div>
        <div v-else-if="errorMessage && !summary.resources.length" class="field-grid">
          <p class="field-error" role="alert">{{ errorMessage }}</p>
          <button type="button" class="ghost-btn" @click="loadResources">
            重试
          </button>
        </div>
        <div v-else-if="!summary.resources.length" class="empty-state">
          当前没有可审核的资源记录。
        </div>
        <div v-else>
          <table class="app-table">
            <thead>
              <tr>
                <th>标题</th>
                <th>分类</th>
                <th>上传者</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="resource in summary.resources" :key="resource.id">
                <td>{{ resource.title }}</td>
                <td>{{ formatCategory(resource.category) }}</td>
                <td>{{ resource.uploaderNickname }}</td>
                <td>{{ statusLabel(resource.status) }}</td>
                <td>
                  <div class="inline-form-actions">
                    <button type="button" class="ghost-btn select-action" @click="selectResource(resource)">
                      选择
                    </button>
                    <button
                      v-if="canPreview(resource)"
                      type="button"
                      class="ghost-btn preview-action"
                      :disabled="actionLoadingId === `preview-${resource.id}`"
                      @click="handlePreview(resource.id)"
                    >
                      {{ actionLoadingId === `preview-${resource.id}`
                        ? (previewKindOf(resource) === "ZIP_TREE" ? "正在加载目录..." : "正在打开预览...")
                        : previewLabel(resource) }}
                    </button>
                    <button
                      type="button"
                      class="ghost-btn publish-action"
                      :disabled="!['PENDING', 'OFFLINE'].includes(resource.status) || actionLoadingId === `publish-${resource.id}`"
                      @click="handlePublish(resource.id)"
                    >
                      发布
                    </button>
                    <button
                      type="button"
                      class="ghost-btn offline-action"
                      :disabled="resource.status !== 'PUBLISHED' || actionLoadingId === `offline-${resource.id}`"
                      @click="handleOffline(resource.id)"
                    >
                      下线
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>

          <div class="mobile-table-cards">
            <article
              v-for="resource in summary.resources"
              :key="`mobile-${resource.id}`"
              class="table-card"
            >
              <strong>{{ resource.title }}</strong>
              <p class="meta-copy">{{ formatCategory(resource.category) }} / {{ resource.uploaderNickname }}</p>
              <p class="meta-copy">状态 {{ statusLabel(resource.status) }} / 创建于 {{ formatTime(resource.createdAt) }}</p>
              <div class="inline-form-actions" style="margin-top: 12px;">
                <button type="button" class="ghost-btn select-action" @click="selectResource(resource)">
                  选择
                </button>
                <button
                  v-if="canPreview(resource)"
                  type="button"
                  class="ghost-btn preview-action"
                  :disabled="actionLoadingId === `preview-${resource.id}`"
                  @click="handlePreview(resource.id)"
                >
                  {{ actionLoadingId === `preview-${resource.id}`
                    ? (previewKindOf(resource) === "ZIP_TREE" ? "正在加载目录..." : "正在打开预览...")
                    : previewLabel(resource) }}
                </button>
                <button
                  type="button"
                  class="ghost-btn publish-action"
                  :disabled="!['PENDING', 'OFFLINE'].includes(resource.status) || actionLoadingId === `publish-${resource.id}`"
                  @click="handlePublish(resource.id)"
                >
                  发布
                </button>
                <button
                  type="button"
                  class="ghost-btn offline-action"
                  :disabled="resource.status !== 'PUBLISHED' || actionLoadingId === `offline-${resource.id}`"
                  @click="handleOffline(resource.id)"
                >
                  下线
                </button>
              </div>
            </article>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.admin-resource-stats {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.admin-resource-stat {
  min-height: 128px;
  display: grid;
  gap: 8px;
  align-content: end;
}

.admin-resource-stat__label {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.admin-resource-stat strong {
  font-family: var(--cp-font-display);
  font-size: clamp(24px, 4vw, 32px);
}

.selected-resource-card {
  display: grid;
  gap: var(--cp-gap-4);
}

.selected-resource-card__header {
  display: flex;
  justify-content: space-between;
  gap: var(--cp-gap-4);
  align-items: start;
}

.selected-resource-card__eyebrow {
  margin: 0;
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.selected-resource-card__title {
  margin: 8px 0 0;
  font-family: var(--cp-font-display);
  font-size: clamp(24px, 4vw, 30px);
  line-height: 1.14;
}

.selected-resource-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cp-gap-3);
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

@media (max-width: 1023px) {
  .admin-resource-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .admin-resource-stats {
    grid-template-columns: 1fr;
  }

  .selected-resource-card__header {
    flex-direction: column;
  }
}
</style>
