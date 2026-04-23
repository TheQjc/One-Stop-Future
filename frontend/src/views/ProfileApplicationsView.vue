<script setup>
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import {
  downloadMyApplicationResume,
  getMyApplications,
  previewMyApplicationResume,
} from "../api/applications.js";

const loading = ref(true);
const errorMessage = ref("");
const actionMessage = ref("");
const actionError = ref("");
const actionLoadingId = ref("");
const summary = ref({
  total: 0,
  applications: [],
});

const statCards = computed(() => {
  const applications = summary.value.applications || [];

  return [
    {
      label: "全部投递",
      value: summary.value.total || applications.length,
    },
    {
      label: "已提交",
      value: applications.filter((item) => item.status === "SUBMITTED").length,
    },
  ];
});

function formatTime(value) {
  if (!value) {
    return "时间未知";
  }

  return String(value).replace("T", " ").slice(0, 16);
}

function statusClass(status) {
  if (["SUBMITTED", "APPROVED"].includes(status)) {
    return "approved";
  }
  if (["REJECTED", "FAILED"].includes(status)) {
    return "rejected";
  }
  return "pending";
}

function statusLabel(status) {
  const labels = {
    SUBMITTED: "已提交",
    APPROVED: "已通过",
    REJECTED: "未通过",
    FAILED: "失败",
  };

  return labels[status] || status || "处理中";
}

async function loadApplications() {
  loading.value = true;
  errorMessage.value = "";

  try {
    summary.value = await getMyApplications();
  } catch (error) {
    errorMessage.value = error.message || "投递记录加载失败，请稍后重试。";
  } finally {
    loading.value = false;
  }
}

async function handlePreview(application) {
  actionMessage.value = "";
  actionError.value = "";
  actionLoadingId.value = `preview-${application.id}`;

  try {
    await previewMyApplicationResume(application.id);
  } catch (error) {
    actionError.value = error.message || "简历快照预览失败，请稍后重试。";
  } finally {
    actionLoadingId.value = "";
  }
}

async function handleDownload(application) {
  actionMessage.value = "";
  actionError.value = "";
  actionLoadingId.value = `download-${application.id}`;

  try {
    const fileName = await downloadMyApplicationResume(application.id);
    actionMessage.value = `已开始下载 ${fileName || application.resumeFileNameSnapshot || `application-${application.id}` }。`;
  } catch (error) {
    actionError.value = error.message || "简历快照下载失败，请稍后重试。";
  } finally {
    actionLoadingId.value = "";
  }
}

onMounted(loadApplications);
</script>

<template>
  <section class="page-stack">
    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">我的申请</span>
          <h1 class="page-title" style="margin-top: 16px;">投递记录</h1>
          <p class="page-subtitle" style="margin-top: 16px;">
            在这里回看已经提交的岗位，预览或下载当时保存的简历快照，也能随时跳回对应岗位详情页。
          </p>
        </div>
      </div>

      <div class="stats-grid profile-application-stats">
        <article
          v-for="card in statCards"
          :key="card.label"
          class="panel-card profile-application-stat"
        >
          <span class="profile-application-stat__label">{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
        </article>
      </div>
    </article>

    <article class="section-card">
      <div v-if="loading" class="empty-state">正在加载你的投递记录...</div>
      <div v-else-if="errorMessage" class="field-grid">
        <p class="field-error" role="alert">{{ errorMessage }}</p>
        <button type="button" class="ghost-btn" @click="loadApplications">
          重试
        </button>
      </div>
      <div v-else-if="!summary.applications.length" class="empty-state">
        你还没有提交任何站内投递。
      </div>
      <div v-else class="application-record-list">
        <p v-if="actionMessage" class="field-hint">{{ actionMessage }}</p>
        <p v-if="actionError" class="field-error" role="alert">{{ actionError }}</p>
        <article
          v-for="application in summary.applications"
          :key="application.id"
          class="panel-card application-record-card"
        >
          <div class="application-record-card__header">
            <div>
              <p class="application-record-card__eyebrow">申请 #{{ application.id }}</p>
              <h2 class="application-record-card__title">{{ application.jobTitle || "未命名岗位" }}</h2>
            </div>
            <span class="status-badge" :class="statusClass(application.status)">
              {{ statusLabel(application.status) }}
            </span>
          </div>

          <div class="application-record-card__meta">
            <span>{{ application.companyName || "未知公司" }}</span>
            <span>{{ application.city || "城市未填写" }}</span>
            <span>投递于 {{ formatTime(application.submittedAt) }}</span>
          </div>

          <article class="panel-card application-record-card__snapshot">
            <strong>{{ application.resumeTitleSnapshot || "暂无简历快照" }}</strong>
            <p class="meta-copy" style="margin-top: 12px;">
              {{ application.resumeFileNameSnapshot || "未返回快照文件名。" }}
            </p>
          </article>

          <div class="inline-form-actions">
            <button
              v-if="application.previewAvailable && application.previewKind === 'FILE'"
              :data-testid="`preview-application-resume-${application.id}`"
              type="button"
              class="ghost-btn"
              :disabled="actionLoadingId === `preview-${application.id}`"
              @click="handlePreview(application)"
            >
              {{ actionLoadingId === `preview-${application.id}` ? "预览中..." : "预览" }}
            </button>
            <button
              :data-testid="`download-application-resume-${application.id}`"
              type="button"
              class="ghost-btn"
              :disabled="actionLoadingId === `download-${application.id}`"
              @click="handleDownload(application)"
            >
              {{ actionLoadingId === `download-${application.id}` ? "准备下载中..." : "下载" }}
            </button>
            <RouterLink :to="`/jobs/${application.jobId}`" class="app-link">
              查看岗位详情
            </RouterLink>
          </div>
        </article>
      </div>
    </article>
  </section>
</template>

<style scoped>
.profile-application-stats {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.profile-application-stat {
  min-height: 128px;
  display: grid;
  gap: 8px;
  align-content: end;
}

.profile-application-stat__label {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.profile-application-stat strong {
  font-family: var(--cp-font-display);
  font-size: clamp(24px, 4vw, 32px);
}

.application-record-list {
  display: grid;
  gap: var(--cp-gap-4);
}

.application-record-card {
  display: grid;
  gap: var(--cp-gap-4);
}

.application-record-card__header {
  display: flex;
  justify-content: space-between;
  gap: var(--cp-gap-4);
  align-items: start;
}

.application-record-card__eyebrow {
  margin: 0;
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.application-record-card__title {
  margin: 8px 0 0;
  font-family: var(--cp-font-display);
  font-size: clamp(24px, 4vw, 30px);
  line-height: 1.14;
}

.application-record-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cp-gap-3);
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.application-record-card__snapshot {
  background: rgba(255, 255, 255, 0.72);
}

@media (max-width: 767px) {
  .profile-application-stats {
    grid-template-columns: 1fr;
  }

  .application-record-card__header {
    flex-direction: column;
  }
}
</style>
