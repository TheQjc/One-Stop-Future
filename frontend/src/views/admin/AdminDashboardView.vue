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
    return "Just now";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "Just now";
  }

  return new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

function communityStatusLabel(status) {
  const labelMap = {
    PUBLISHED: "Published",
    HIDDEN: "Hidden",
    DELETED: "Deleted",
  };

  return labelMap[status] || status || "Open";
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
    DRAFT: "Draft",
    PUBLISHED: "Published",
    OFFLINE: "Offline",
  };

  return labelMap[status] || status || "Open";
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
    PENDING: "Pending",
    PUBLISHED: "Published",
    REJECTED: "Rejected",
    OFFLINE: "Offline",
  };

  return labelMap[status] || status || "Open";
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
    eyebrow: "Verification Desk",
    title: "Verification queue",
    intro: "Keep the student-verification line moving from one quiet overview before opening the full review desk.",
    statsClass: "admin-desk-card__stats--compact",
    stats: [
      { label: "Pending", value: summary.value.verification.pendingCount },
      { label: "Reviewed Today", value: summary.value.verification.reviewedToday },
    ],
    items: summary.value.verification.latestPendingApplications.map((item) => ({
      id: item.id,
      eyebrow: `Application #${item.id}`,
      title: item.applicantNickname || item.realName || "Unknown applicant",
      meta: [
        item.realName || "No real name filed",
        item.studentId || "No student ID",
        `Submitted ${formatTime(item.createdAt)}`,
      ].join(" · "),
      status: "Pending",
      tone: "warm",
    })),
    emptyText: "No applications are waiting on the line.",
    ctaTo: "/admin/verifications",
    ctaLabel: "Open verification desk",
  },
  {
    key: "community",
    eyebrow: "Community Desk",
    title: "Community watch",
    intro: "Read the current post mix, then move into the moderation surface only when a thread needs action.",
    statsClass: "admin-desk-card__stats--wide",
    stats: [
      { label: "All Posts", value: summary.value.community.totalCount },
      { label: "Published", value: summary.value.community.publishedCount },
      { label: "Hidden", value: summary.value.community.hiddenCount },
      { label: "Deleted", value: summary.value.community.deletedCount },
    ],
    items: summary.value.community.latestPosts.map((item) => ({
      id: item.id,
      eyebrow: `Post #${item.id}`,
      title: item.title || "Untitled post",
      meta: [
        item.authorNickname || "Unknown author",
        item.tag || "General",
        `${item.likeCount || 0} likes`,
        `Filed ${formatTime(item.createdAt)}`,
      ].join(" · "),
      status: communityStatusLabel(item.status),
      tone: communityStatusTone(item.status),
    })),
    emptyText: "No recent posts have reached the desk.",
    ctaTo: "/admin/community",
    ctaLabel: "Open community desk",
  },
  {
    key: "jobs",
    eyebrow: "Jobs Desk",
    title: "Jobs board",
    intro: "Track how much of the jobs shelf is draft, live, or resting offline before entering the editor.",
    statsClass: "admin-desk-card__stats--wide",
    stats: [
      { label: "All Jobs", value: summary.value.jobs.totalCount },
      { label: "Draft", value: summary.value.jobs.draftCount },
      { label: "Published", value: summary.value.jobs.publishedCount },
      { label: "Offline", value: summary.value.jobs.offlineCount },
    ],
    items: summary.value.jobs.latestActionableJobs.map((item) => ({
      id: item.id,
      eyebrow: `Job #${item.id}`,
      title: item.title || "Untitled job",
      meta: [
        item.companyName || "Unknown company",
        item.city || "City not set",
        item.sourcePlatform || "Source not set",
        `Updated ${formatTime(item.updatedAt || item.publishedAt || item.deadlineAt)}`,
      ].join(" · "),
      status: jobStatusLabel(item.status),
      tone: jobStatusTone(item.status),
    })),
    emptyText: "No draft or offline jobs are on the board.",
    ctaTo: "/admin/jobs",
    ctaLabel: "Open jobs desk",
  },
  {
    key: "resources",
    eyebrow: "Resources Desk",
    title: "Resources shelf",
    intro: "Hold the pending resource line, published stock, and closed records on one readable board.",
    statsClass: "admin-desk-card__stats--wide",
    stats: [
      { label: "All Records", value: summary.value.resources.totalCount },
      { label: "Pending", value: summary.value.resources.pendingCount },
      { label: "Published", value: summary.value.resources.publishedCount },
      { label: "Closed", value: summary.value.resources.closedCount },
    ],
    items: summary.value.resources.latestPendingResources.map((item) => ({
      id: item.id,
      eyebrow: `Resource #${item.id}`,
      title: item.title || "Untitled resource",
      meta: [
        item.uploaderNickname || "Unknown uploader",
        item.fileName || "File pending",
        `Filed ${formatTime(item.createdAt)}`,
      ].join(" · "),
      status: resourceStatusLabel(item.status),
      tone: resourceStatusTone(item.status),
    })),
    emptyText: "No resources are waiting on the shelf.",
    ctaTo: "/admin/resources",
    ctaLabel: "Open resources desk",
  },
]);

async function loadSummary() {
  loading.value = true;
  pageError.value = "";

  try {
    summary.value = await getAdminDashboardSummary();
  } catch (error) {
    pageError.value = error.message || "The operations board could not be loaded. Please try again.";
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
        Loading the operations board...
      </div>
    </article>

    <article v-else-if="pageError" class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">Admin Desk</span>
          <h1 class="page-title" style="margin-top: 16px;">Operations board</h1>
          <p class="page-subtitle" style="margin-top: 16px;">
            The overview did not arrive, so the board is holding on a single retry surface.
          </p>
        </div>
      </div>

      <div class="field-grid admin-dashboard__page-state">
        <p class="field-error" role="alert">{{ pageError }}</p>
        <button type="button" class="ghost-btn" @click="loadSummary">
          Retry board
        </button>
      </div>
    </article>

    <template v-else>
      <article class="section-card">
        <div class="section-header">
          <div>
            <span class="section-eyebrow">Admin Desk</span>
            <h1 class="page-title" style="margin-top: 16px;">Operations board</h1>
            <p class="page-subtitle" style="margin-top: 16px;">
              Read the active queues, note what has moved today, and step into the right desk only
              when the board says it is time.
            </p>
          </div>
        </div>

        <div class="dashboard-grid admin-dashboard__lead">
          <article class="panel-card admin-dashboard__lead-card">
            <p class="admin-dashboard__lead-label">Morning read</p>
            <strong>Four desks, one quiet surface.</strong>
            <p class="meta-copy">
              Verification, community, jobs, and resources stay readable here before deeper work
              begins elsewhere.
            </p>
          </article>

          <article class="panel-card admin-dashboard__lead-card">
            <p class="admin-dashboard__lead-label">Scope</p>
            <strong>Overview only.</strong>
            <p class="meta-copy">
              This board is intentionally non-destructive. It points to the full desks instead of
              placing mutation controls on the page.
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
              <h3 class="admin-desk-card__subhead">Recent line</h3>

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
