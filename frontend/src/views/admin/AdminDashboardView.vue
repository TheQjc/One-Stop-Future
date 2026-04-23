<script setup>
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import { getAdminDashboardSummary } from "../../api/admin.js";

function createEmptySummary() {
  return {
    verification: {
      pendingCount: 0,
      reviewedToday: 0,
      latestPendingApplications: [],
    },
    community: {
      totalCount: 0,
      publishedCount: 0,
      hiddenCount: 0,
      deletedCount: 0,
      latestPosts: [],
    },
    jobs: {
      totalCount: 0,
      draftCount: 0,
      publishedCount: 0,
      offlineCount: 0,
      latestActionableJobs: [],
    },
    resources: {
      totalCount: 0,
      pendingCount: 0,
      publishedCount: 0,
      closedCount: 0,
      latestPendingResources: [],
    },
  };
}

const loading = ref(true);
const pageError = ref("");
const summary = ref(createEmptySummary());

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

function communityStatusLabel(status) {
  const labelMap = {
    PUBLISHED: "已发布",
    HIDDEN: "已隐藏",
    DELETED: "已删除",
  };

  return labelMap[status] || status || "处理中";
}

function communityStatusTone(status) {
  if (status === "DELETED") {
    return "danger";
  }
  if (status === "HIDDEN") {
    return "quiet";
  }
  return "success";
}

function jobStatusLabel(status) {
  const labelMap = {
    DRAFT: "草稿",
    PUBLISHED: "已发布",
    OFFLINE: "已下线",
  };

  return labelMap[status] || status || "处理中";
}

function jobStatusTone(status) {
  if (status === "OFFLINE") {
    return "quiet";
  }
  if (status === "PUBLISHED") {
    return "success";
  }
  return "warm";
}

function resourceStatusLabel(status) {
  const labelMap = {
    PENDING: "待审核",
    PUBLISHED: "已发布",
    REJECTED: "已驳回",
    OFFLINE: "已下线",
  };

  return labelMap[status] || status || "处理中";
}

function resourceStatusTone(status) {
  if (status === "PUBLISHED") {
    return "success";
  }
  if (status === "REJECTED") {
    return "danger";
  }
  if (status === "OFFLINE") {
    return "quiet";
  }
  return "warm";
}

const sections = computed(() => [
  {
    key: "verification",
    eyebrow: "认证审核",
    title: "认证队列",
    intro: "先在总览里查看待审数量和今日处理量，再进入完整审核台处理具体申请。",
    statsClass: "admin-desk-card__stats--compact",
    stats: [
      { label: "待审核", value: summary.value.verification.pendingCount },
      { label: "今日已审", value: summary.value.verification.reviewedToday },
    ],
    items: summary.value.verification.latestPendingApplications.map((item) => ({
      id: item.id,
      eyebrow: `申请 #${item.id}`,
      title: item.applicantNickname || item.realName || "未知申请人",
      meta: [
        item.realName || "未填写姓名",
        item.studentId || "未填写学号",
        `提交于 ${formatTime(item.createdAt)}`,
      ].join(" · "),
      status: "待审核",
      tone: "warm",
    })),
    emptyText: "当前没有待审核申请。",
    ctaTo: "/admin/verifications",
    ctaLabel: "进入认证审核台",
  },
  {
    key: "community",
    eyebrow: "社区治理",
    title: "社区看板",
    intro: "先看当前帖子状态分布，再按需要进入治理页处理具体内容。",
    statsClass: "admin-desk-card__stats--wide",
    stats: [
      { label: "全部帖子", value: summary.value.community.totalCount },
      { label: "已发布", value: summary.value.community.publishedCount },
      { label: "已隐藏", value: summary.value.community.hiddenCount },
      { label: "已删除", value: summary.value.community.deletedCount },
    ],
    items: summary.value.community.latestPosts.map((item) => ({
      id: item.id,
      eyebrow: `帖子 #${item.id}`,
      title: item.title || "未命名帖子",
      meta: [
        item.authorNickname || "未知作者",
        item.tag || "未分类",
        `赞 ${item.likeCount || 0}`,
        `提交于 ${formatTime(item.createdAt)}`,
      ].join(" · "),
      status: communityStatusLabel(item.status),
      tone: communityStatusTone(item.status),
    })),
    emptyText: "当前没有最近帖子需要展示。",
    ctaTo: "/admin/community",
    ctaLabel: "进入社区治理页",
  },
  {
    key: "jobs",
    eyebrow: "岗位管理",
    title: "岗位看板",
    intro: "先查看草稿、已发布和已下线岗位的分布，再进入编辑页处理具体记录。",
    statsClass: "admin-desk-card__stats--wide",
    stats: [
      { label: "全部岗位", value: summary.value.jobs.totalCount },
      { label: "草稿", value: summary.value.jobs.draftCount },
      { label: "已发布", value: summary.value.jobs.publishedCount },
      { label: "已下线", value: summary.value.jobs.offlineCount },
    ],
    items: summary.value.jobs.latestActionableJobs.map((item) => ({
      id: item.id,
      eyebrow: `岗位 #${item.id}`,
      title: item.title || "未命名岗位",
      meta: [
        item.companyName || "未知公司",
        item.city || "城市未填写",
        item.sourcePlatform || "来源未填写",
        `更新于 ${formatTime(item.updatedAt || item.publishedAt || item.deadlineAt)}`,
      ].join(" · "),
      status: jobStatusLabel(item.status),
      tone: jobStatusTone(item.status),
    })),
    emptyText: "当前没有草稿或待处理岗位。",
    ctaTo: "/admin/jobs",
    ctaLabel: "进入岗位管理页",
  },
  {
    key: "resources",
    eyebrow: "资源审核",
    title: "资源看板",
    intro: "把待审核、已发布和已关闭资源集中展示，便于快速进入具体审核流程。",
    statsClass: "admin-desk-card__stats--wide",
    stats: [
      { label: "全部记录", value: summary.value.resources.totalCount },
      { label: "待审核", value: summary.value.resources.pendingCount },
      { label: "已发布", value: summary.value.resources.publishedCount },
      { label: "已关闭", value: summary.value.resources.closedCount },
    ],
    items: summary.value.resources.latestPendingResources.map((item) => ({
      id: item.id,
      eyebrow: `资源 #${item.id}`,
      title: item.title || "未命名资源",
      meta: [
        item.uploaderNickname || "未知上传者",
        item.fileName || "文件待补充",
        `提交于 ${formatTime(item.createdAt)}`,
      ].join(" · "),
      status: resourceStatusLabel(item.status),
      tone: resourceStatusTone(item.status),
    })),
    emptyText: "当前没有待处理资源。",
    ctaTo: "/admin/resources",
    ctaLabel: "进入资源审核页",
  },
]);

async function loadSummary() {
  loading.value = true;
  pageError.value = "";

  try {
    summary.value = await getAdminDashboardSummary();
  } catch (error) {
    pageError.value = error.message || "管理总览加载失败，请稍后重试。";
  } finally {
    loading.value = false;
  }
}

onMounted(loadSummary);
</script>

<template>
  <section class="page-stack">
    <article v-if="loading" class="section-card">
      <div class="empty-state admin-dashboard__page-state">
        正在加载管理总览...
      </div>
    </article>

    <article v-else-if="pageError" class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">管理总览</span>
          <h1 class="page-title" style="margin-top: 16px;">运营看板</h1>
          <p class="page-subtitle" style="margin-top: 16px;">
            总览数据暂时未返回，请先在这里重试，再继续进入具体管理页面。
          </p>
        </div>
      </div>

      <div class="field-grid admin-dashboard__page-state">
        <p class="field-error" role="alert">{{ pageError }}</p>
        <button type="button" class="ghost-btn" @click="loadSummary">
          重试
        </button>
      </div>
    </article>

    <template v-else>
      <article class="section-card">
        <div class="section-header">
          <div>
            <span class="section-eyebrow">管理总览</span>
            <h1 class="page-title" style="margin-top: 16px;">运营看板</h1>
            <p class="page-subtitle" style="margin-top: 16px;">
              先看各条队列的状态和今日变化，再进入对应管理页处理具体事项。
            </p>
          </div>
        </div>

        <div class="dashboard-grid admin-dashboard__lead">
          <article class="panel-card admin-dashboard__lead-card">
            <p class="admin-dashboard__lead-label">今日概览</p>
            <strong>四类工作台，一页总览。</strong>
            <p class="meta-copy">
              认证、社区、岗位和资源会先在这里集中展示，再分流到各自管理页面。
            </p>
          </article>

          <article class="panel-card admin-dashboard__lead-card">
            <p class="admin-dashboard__lead-label">边界说明</p>
            <strong>这里只做总览，不直接改动数据。</strong>
            <p class="meta-copy">
              这个页面只负责指向完整工作台，不在总览页直接放置破坏性操作按钮。
            </p>
          </article>
        </div>
      </article>

      <div class="dashboard-grid admin-dashboard__sections">
        <article
          v-for="section in sections"
          :key="section.key"
          class="section-card admin-desk-card"
        >
          <div class="section-header">
            <div>
              <span class="section-eyebrow">{{ section.eyebrow }}</span>
              <h2 class="page-title" style="margin-top: 16px;">{{ section.title }}</h2>
              <p class="page-subtitle" style="margin-top: 16px;">
                {{ section.intro }}
              </p>
            </div>
          </div>

          <div class="stats-grid admin-desk-card__stats" :class="section.statsClass">
            <article
              v-for="stat in section.stats"
              :key="`${section.key}-${stat.label}`"
              class="panel-card admin-desk-card__stat"
            >
              <span class="admin-desk-card__stat-label">{{ stat.label }}</span>
              <strong>{{ stat.value }}</strong>
            </article>
          </div>

          <div class="field-grid admin-desk-card__body">
            <div class="field-grid admin-desk-card__recent">
              <h3 class="admin-desk-card__subhead">最新动态</h3>

              <div
                v-if="section.items.length === 0"
                class="empty-state admin-desk-card__empty"
                role="status"
              >
                {{ section.emptyText }}
              </div>

              <div v-else class="admin-desk-card__list">
                <article
                  v-for="item in section.items"
                  :key="`${section.key}-${item.id}`"
                  class="panel-card admin-desk-card__list-item"
                >
                  <div class="admin-desk-card__list-header">
                    <div>
                      <p class="admin-desk-card__item-eyebrow">{{ item.eyebrow }}</p>
                      <h3 class="admin-desk-card__item-title">{{ item.title }}</h3>
                      <p class="meta-copy admin-desk-card__item-meta">{{ item.meta }}</p>
                    </div>
                    <span class="admin-desk-card__item-status" :class="item.tone">
                      {{ item.status }}
                    </span>
                  </div>
                </article>
              </div>
            </div>

            <RouterLink :to="section.ctaTo" class="app-link admin-desk-card__cta">
              {{ section.ctaLabel }}
            </RouterLink>
          </div>
        </article>
      </div>
    </template>
  </section>
</template>

<style scoped>
.admin-dashboard__page-state {
  min-height: 280px;
  display: grid;
  align-content: center;
}

.admin-dashboard__lead {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.admin-dashboard__lead-card {
  min-height: 164px;
  display: grid;
  gap: var(--cp-gap-3);
  align-content: start;
}

.admin-dashboard__lead-label {
  margin: 0;
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.admin-dashboard__lead-card strong {
  font-family: var(--cp-font-display);
  font-size: clamp(24px, 4vw, 32px);
  line-height: 1.15;
}

.admin-dashboard__sections {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.admin-desk-card {
  display: grid;
  gap: var(--cp-gap-6);
}

.admin-desk-card__stats--compact {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.admin-desk-card__stats--wide {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.admin-desk-card__stat {
  min-height: 118px;
  display: grid;
  gap: 8px;
  align-content: end;
}

.admin-desk-card__stat-label {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.admin-desk-card__stat strong {
  font-family: var(--cp-font-display);
  font-size: clamp(24px, 4vw, 32px);
}

.admin-desk-card__body {
  align-content: start;
}

.admin-desk-card__recent {
  align-content: start;
}

.admin-desk-card__subhead {
  margin: 0;
  font-family: var(--cp-font-display);
  font-size: 22px;
  line-height: 1.2;
}

.admin-desk-card__empty {
  min-height: 92px;
  display: grid;
  align-content: center;
}

.admin-desk-card__list {
  display: grid;
  gap: var(--cp-gap-4);
}

.admin-desk-card__list-item {
  background: rgba(255, 255, 255, 0.76);
}

.admin-desk-card__list-header {
  display: flex;
  justify-content: space-between;
  gap: var(--cp-gap-4);
  align-items: start;
}

.admin-desk-card__item-eyebrow {
  margin: 0;
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.admin-desk-card__item-title {
  margin: 8px 0 0;
  font-family: var(--cp-font-display);
  font-size: clamp(22px, 4vw, 28px);
  line-height: 1.16;
}

.admin-desk-card__item-meta {
  margin-top: 12px;
}

.admin-desk-card__item-status {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border-radius: var(--cp-radius-pill);
  border: 1px solid transparent;
  font-size: var(--cp-text-sm);
  white-space: nowrap;
}

.admin-desk-card__item-status.warm {
  color: var(--cp-warning);
  background: rgba(145, 98, 28, 0.12);
}

.admin-desk-card__item-status.success {
  color: var(--cp-success);
  background: rgba(37, 98, 77, 0.12);
}

.admin-desk-card__item-status.quiet {
  color: var(--cp-ink-soft);
  background: rgba(24, 38, 63, 0.08);
}

.admin-desk-card__item-status.danger {
  color: var(--cp-danger);
  background: rgba(141, 43, 43, 0.12);
}

.admin-desk-card__cta {
  justify-self: start;
}

@media (max-width: 1279px) {
  .admin-desk-card__stats--wide {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .admin-desk-card__list-header {
    flex-direction: column;
  }

  .admin-desk-card__stats--compact,
  .admin-desk-card__stats--wide {
    grid-template-columns: 1fr;
  }
}
</style>
