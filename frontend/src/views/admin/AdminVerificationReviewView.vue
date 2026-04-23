<script setup>
import { computed, onMounted, ref } from "vue";
import {
  getVerificationApplications,
  getVerificationDashboard,
  reviewVerification,
} from "../../api/admin.js";

const loading = ref(true);
const pageError = ref("");
const reviewError = ref("");
const successMessage = ref("");
const reviewingId = ref(null);
const activeRejectId = ref(null);
const rejectReason = ref("");

const dashboard = ref({
  pendingCount: 0,
  reviewedToday: 0,
  latestPendingApplications: [],
});

const applications = ref([]);

const totalApplications = computed(() => applications.value.length);

function formatTime(value) {
  if (!value) {
    return "刚刚";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "刚刚";
  }

  return new Intl.DateTimeFormat("zh-CN", {
    month: "numeric",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

function statusLabel(status) {
  const labelMap = {
    PENDING: "待审核",
    APPROVED: "已通过",
    REJECTED: "已驳回",
  };

  return labelMap[status] || status;
}

function statusClass(status) {
  if (status === "APPROVED") {
    return "approved";
  }
  if (status === "REJECTED") {
    return "rejected";
  }
  return "pending";
}

async function loadData() {
  loading.value = true;
  pageError.value = "";

  try {
    const [dashboardData, applicationsData] = await Promise.all([
      getVerificationDashboard(),
      getVerificationApplications(),
    ]);

    dashboard.value = dashboardData;
    applications.value = applicationsData;
  } catch (error) {
    pageError.value = error.message || "审核数据加载失败，请稍后重试。";
  } finally {
    loading.value = false;
  }
}

function openReject(applicationId) {
  activeRejectId.value = applicationId;
  rejectReason.value = "";
  reviewError.value = "";
  successMessage.value = "";
}

function cancelReject() {
  activeRejectId.value = null;
  rejectReason.value = "";
}

async function performReview(application, action) {
  reviewError.value = "";
  successMessage.value = "";
  reviewingId.value = application.id;

  try {
    await reviewVerification(application.id, {
      action,
      reason: action === "REJECT" ? rejectReason.value.trim() : undefined,
    });

    activeRejectId.value = null;
    rejectReason.value = "";
    successMessage.value = "审核操作已完成，列表与统计已刷新。";
    await loadData();
  } catch (error) {
    reviewError.value = error.message || "审核操作失败，请稍后重试。";
  } finally {
    reviewingId.value = null;
  }
}

async function handleApprove(application) {
  await performReview(application, "APPROVE");
}

async function handleReject(application) {
  if (!rejectReason.value.trim()) {
    reviewError.value = "驳回时必须填写原因。";
    return;
  }

  await performReview(application, "REJECT");
}

onMounted(loadData);
</script>

<template>
  <section class="page-stack">
    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">认证审核</span>
          <h1 class="page-title" style="margin-top: 16px;">认证审核台</h1>
          <p class="page-subtitle" style="margin-top: 16px;">
            在一个集中视图里处理待审核申请、查看今日处理量，并直接完成通过或驳回操作。
          </p>
        </div>
      </div>

      <div class="stats-grid">
        <article class="panel-card summary-card">
          <p class="summary-card__label">待审申请</p>
          <strong>{{ dashboard.pendingCount }}</strong>
        </article>
        <article class="panel-card summary-card">
          <p class="summary-card__label">今日已审</p>
          <strong>{{ dashboard.reviewedToday }}</strong>
        </article>
        <article class="panel-card summary-card">
          <p class="summary-card__label">申请总数</p>
          <strong>{{ totalApplications }}</strong>
        </article>
      </div>

      <p v-if="successMessage" class="field-hint" style="margin-top: 20px;">{{ successMessage }}</p>
      <p v-if="reviewError" class="field-error" role="alert" style="margin-top: 12px;">{{ reviewError }}</p>
    </article>

    <div class="dashboard-grid">
      <article class="section-card">
        <div class="section-header">
          <div>
            <span class="section-eyebrow">待审预览</span>
            <h2 class="page-title" style="margin-top: 16px;">最新待审申请</h2>
          </div>
        </div>

        <div v-if="loading" class="empty-state">正在同步待审申请...</div>
        <div v-else-if="dashboard.latestPendingApplications.length === 0" class="empty-state">
          当前没有待审核申请。
        </div>
        <div v-else class="preview-list">
          <article
            v-for="application in dashboard.latestPendingApplications"
            :key="application.id"
            class="preview-card"
          >
            <div>
              <p class="preview-card__eyebrow">申请 #{{ application.id }}</p>
              <h3 class="preview-card__title">{{ application.applicantNickname }}</h3>
              <p class="meta-copy">{{ application.realName }} · {{ application.studentId }}</p>
            </div>
            <span class="status-badge pending">{{ statusLabel(application.status) }}</span>
          </article>
        </div>
      </article>

      <article class="section-card">
        <div class="section-header">
          <div>
            <span class="section-eyebrow">审核说明</span>
            <h2 class="page-title" style="margin-top: 16px;">审核基线</h2>
          </div>
        </div>

        <div class="field-grid">
          <article class="panel-card">
            <strong>通过时同步更新用户状态</strong>
            <p class="meta-copy" style="margin-top: 12px;">
              审核通过后，申请人的认证状态会切换为已认证，并写回真实姓名和学号。
            </p>
          </article>
          <article class="panel-card">
            <strong>驳回时必须给出原因</strong>
            <p class="meta-copy" style="margin-top: 12px;">
              驳回会把用户恢复到未认证状态，同时把驳回说明发送到通知中心。
            </p>
          </article>
        </div>
      </article>
    </div>

    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">申请队列</span>
          <h2 class="page-title" style="margin-top: 16px;">申请列表</h2>
          <p class="page-subtitle" style="margin-top: 16px;">
            列表按提交时间倒序展示，待审核申请可以直接在卡片内完成处理。
          </p>
        </div>
      </div>

      <div v-if="loading" class="empty-state">正在同步审核列表...</div>
      <div v-else-if="pageError" class="field-grid">
        <p class="field-error" role="alert">{{ pageError }}</p>
        <button type="button" class="ghost-btn" @click="loadData">
          重新加载
        </button>
      </div>
      <div v-else-if="applications.length === 0" class="empty-state">
        当前没有可展示的认证申请。
      </div>
      <div v-else class="review-list">
        <article
          v-for="application in applications"
          :key="application.id"
          class="review-card"
        >
          <div class="review-card__header">
            <div>
              <p class="review-card__eyebrow">申请 #{{ application.id }}</p>
              <h3 class="review-card__title">{{ application.applicantNickname }}</h3>
              <p class="meta-copy">{{ application.realName }} · {{ application.studentId }}</p>
            </div>
            <span class="status-badge" :class="statusClass(application.status)">
              {{ statusLabel(application.status) }}
            </span>
          </div>

          <div class="review-card__meta">
            <span>用户 ID {{ application.userId }}</span>
            <span>提交时间 {{ formatTime(application.createdAt) }}</span>
          </div>

          <div
            v-if="application.status === 'PENDING'"
            class="review-card__actions"
          >
            <button
              type="button"
              class="app-btn"
              :disabled="reviewingId === application.id"
              @click="handleApprove(application)"
            >
              {{ reviewingId === application.id ? "处理中..." : "通过" }}
            </button>
            <button
              type="button"
              class="danger-btn"
              :disabled="reviewingId === application.id"
              @click="openReject(application.id)"
            >
              驳回
            </button>
          </div>

          <div
            v-if="activeRejectId === application.id"
            class="field-grid reject-panel"
          >
            <label class="field-label">
              驳回原因
              <textarea
                v-model.trim="rejectReason"
                class="field-textarea"
                placeholder="请输入驳回说明"
              />
            </label>

            <div class="inline-form-actions">
              <button
                type="button"
                class="danger-btn"
                :disabled="reviewingId === application.id"
                @click="handleReject(application)"
              >
                {{ reviewingId === application.id ? "处理中..." : "确认驳回" }}
              </button>
              <button
                type="button"
                class="ghost-btn"
                :disabled="reviewingId === application.id"
                @click="cancelReject"
              >
                取消
              </button>
            </div>
          </div>
        </article>
      </div>
    </article>
  </section>
</template>

<style scoped>
.summary-card {
  min-height: 132px;
  display: grid;
  gap: var(--cp-gap-2);
  align-content: end;
}

.summary-card__label {
  margin: 0;
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.summary-card strong {
  font-size: 30px;
  font-family: var(--cp-font-display);
}

.preview-list,
.review-list {
  display: grid;
  gap: var(--cp-gap-4);
}

.preview-card,
.review-card {
  border-radius: var(--cp-radius-md);
  border: 1px solid var(--cp-line);
  background: rgba(255, 255, 255, 0.76);
}

.preview-card {
  padding: 20px;
  display: flex;
  justify-content: space-between;
  gap: var(--cp-gap-4);
  align-items: start;
}

.preview-card__eyebrow,
.review-card__eyebrow {
  margin: 0;
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.preview-card__title,
.review-card__title {
  margin: 8px 0 0;
  font-family: var(--cp-font-display);
  font-size: 28px;
  line-height: 1.14;
}

.review-card {
  padding: 22px;
  display: grid;
  gap: var(--cp-gap-4);
}

.review-card__header {
  display: flex;
  justify-content: space-between;
  gap: var(--cp-gap-4);
  align-items: start;
}

.review-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cp-gap-3);
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.review-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cp-gap-3);
}

.reject-panel {
  padding-top: var(--cp-gap-2);
  border-top: 1px dashed var(--cp-line);
}

@media (max-width: 767px) {
  .preview-card,
  .review-card__header {
    flex-direction: column;
  }
}
</style>
