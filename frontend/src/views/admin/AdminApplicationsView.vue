<script setup>
import { onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import {
  downloadAdminApplicationResume,
  getAdminApplications,
  previewAdminApplicationResume,
} from "../../api/admin.js";

const loading = ref(true);
const errorMessage = ref("");
const actionMessage = ref("");
const actionError = ref("");
const actionLoadingId = ref("");
const summary = ref({
  total: 0,
  submittedToday: 0,
  uniqueApplicants: 0,
  uniqueJobs: 0,
  applications: [],
});

const statCards = [
  { key: "total", label: "全部申请" },
  { key: "submittedToday", label: "今日提交" },
  { key: "uniqueApplicants", label: "申请人数" },
  { key: "uniqueJobs", label: "涉及岗位" },
];

function formatTime(value) {
  if (!value) {
    return "时间未知";
  }

  return String(value).replace("T", " ").slice(0, 16);
}

function statusLabel(status) {
  const labels = {
    SUBMITTED: "已提交",
    APPROVED: "已通过",
    REJECTED: "未通过",
  };

  return labels[status] || status || "处理中";
}

async function loadApplications() {
  loading.value = true;
  errorMessage.value = "";

  try {
    summary.value = await getAdminApplications();
  } catch (error) {
    errorMessage.value = error.message || "管理端申请记录加载失败，请稍后重试。";
  } finally {
    loading.value = false;
  }
}

async function handleDownload(application) {
  actionMessage.value = "";
  actionError.value = "";
  actionLoadingId.value = `download-${application.id}`;

  try {
    const fileName = await downloadAdminApplicationResume(application.id);
    actionMessage.value = `已开始下载 ${fileName || application.resumeFileNameSnapshot || `application-${application.id}` }。`;
  } catch (error) {
    actionError.value = error.message || "简历快照下载失败，请稍后重试。";
  } finally {
    actionLoadingId.value = "";
  }
}

async function handlePreview(application) {
  actionMessage.value = "";
  actionError.value = "";
  actionLoadingId.value = `preview-${application.id}`;

  try {
    await previewAdminApplicationResume(application.id);
  } catch (error) {
    actionError.value = error.message || "简历快照预览失败，请稍后重试。";
  } finally {
    actionLoadingId.value = "";
  }
}

onMounted(loadApplications);
</script>

<template>
  <section class="page-stack">
    <article v-if="loading" class="section-card">
      <div class="empty-state">正在加载申请记录...</div>
    </article>

    <article v-else-if="errorMessage" class="section-card">
      <div class="field-grid">
        <p class="field-error" role="alert">{{ errorMessage }}</p>
        <button type="button" class="ghost-btn" @click="loadApplications">
          重试
        </button>
      </div>
    </article>

    <template v-else>
      <article class="section-card">
        <div class="section-header">
          <div>
            <span class="section-eyebrow">申请记录</span>
            <h1 class="page-title" style="margin-top: 16px;">站内申请工作台</h1>
            <p class="page-subtitle" style="margin-top: 16px;">
              这里集中查看最新申请记录、预览或下载简历快照，并回到对应岗位详情页核对上下文。
            </p>
          </div>
        </div>

        <div class="stats-grid admin-applications-stats">
          <article
            v-for="card in statCards"
            :key="card.key"
            class="panel-card admin-applications-stat"
          >
            <span class="admin-applications-stat__label">{{ card.label }}</span>
            <strong>{{ summary[card.key] || 0 }}</strong>
          </article>
        </div>

        <p v-if="actionMessage" class="field-hint" style="margin-top: 20px;">{{ actionMessage }}</p>
        <p v-if="actionError" class="field-error" role="alert" style="margin-top: 12px;">
          {{ actionError }}
        </p>
      </article>

      <article class="section-card">
        <div v-if="!summary.applications.length" class="empty-state">
          当前还没有可查看的申请记录。
        </div>
        <div v-else>
          <table class="app-table">
            <thead>
              <tr>
                <th>申请号</th>
                <th>申请人</th>
                <th>岗位</th>
                <th>简历快照</th>
                <th>提交时间</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="application in summary.applications" :key="application.id">
                <td>#{{ application.id }}</td>
                <td>
                  <div class="admin-applications-table__identity">
                    <strong>{{ application.applicantNickname || "未知用户" }}</strong>
                    <span>用户 ID {{ application.applicantUserId }}</span>
                  </div>
                </td>
                <td>
                  <div class="admin-applications-table__job">
                    <strong>{{ application.jobTitle || "未命名岗位" }}</strong>
                    <span>{{ application.companyName || "未知公司" }}</span>
                  </div>
                </td>
                <td>{{ application.resumeFileNameSnapshot || "未返回快照文件" }}</td>
                <td>{{ formatTime(application.submittedAt) }}</td>
                <td>{{ statusLabel(application.status) }}</td>
                <td>
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
                      {{ actionLoadingId === `download-${application.id}` ? "准备下载中..." : "下载简历" }}
                    </button>
                    <RouterLink :to="`/jobs/${application.jobId}`" class="app-link">
                      查看岗位
                    </RouterLink>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>

          <div class="mobile-table-cards">
            <article
              v-for="application in summary.applications"
              :key="`mobile-${application.id}`"
              class="table-card admin-application-card"
            >
              <div class="admin-application-card__header">
                <div>
                  <p class="admin-application-card__eyebrow">申请 #{{ application.id }}</p>
                  <strong>{{ application.jobTitle || "未命名岗位" }}</strong>
                </div>
                <span class="status-badge pending">{{ statusLabel(application.status) }}</span>
              </div>

              <p class="meta-copy">
                {{ application.companyName || "未知公司" }} / {{ application.applicantNickname || "未知用户" }}
              </p>
              <p class="meta-copy">
                用户 ID {{ application.applicantUserId }} / {{ application.resumeFileNameSnapshot || "未返回快照文件" }}
              </p>
              <p class="meta-copy">提交于 {{ formatTime(application.submittedAt) }}</p>

              <div class="inline-form-actions" style="margin-top: 12px;">
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
                  {{ actionLoadingId === `download-${application.id}` ? "准备下载中..." : "下载简历" }}
                </button>
                <RouterLink :to="`/jobs/${application.jobId}`" class="app-link">
                  查看岗位
                </RouterLink>
              </div>
            </article>
          </div>
        </div>
      </article>
    </template>
  </section>
</template>

<style scoped>
.admin-applications-stats {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.admin-applications-stat {
  min-height: 128px;
  display: grid;
  gap: 8px;
  align-content: end;
}

.admin-applications-stat__label {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.admin-applications-stat strong {
  font-family: var(--cp-font-display);
  font-size: clamp(24px, 4vw, 32px);
}

.admin-applications-table__identity,
.admin-applications-table__job,
.admin-application-card {
  display: grid;
  gap: 6px;
}

.admin-applications-table__identity span,
.admin-applications-table__job span,
.admin-application-card__eyebrow {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.admin-application-card__header {
  display: flex;
  justify-content: space-between;
  gap: var(--cp-gap-4);
  align-items: start;
}

@media (max-width: 1023px) {
  .admin-applications-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .admin-applications-stats {
    grid-template-columns: 1fr;
  }

  .admin-application-card__header {
    flex-direction: column;
  }
}
</style>
