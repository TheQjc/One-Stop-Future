<script setup>
import { computed, ref, watch } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { applyToJob, favoriteJob, getJobDetail, unfavoriteJob } from "../api/jobs.js";
import { getMyResumes } from "../api/resumes.js";
import { useUserStore } from "../stores/user.js";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const loading = ref(false);
const errorMessage = ref("");
const actionError = ref("");
const actionMessage = ref("");
const favoriteLoading = ref(false);
const applySubmitting = ref(false);
const resumesLoading = ref(false);
const resumesLoaded = ref(false);
const applyPanelOpen = ref(false);
const resumes = ref([]);
const resumesError = ref("");
const selectedResumeId = ref("");
const detail = ref(null);

const typeLabels = {
  INTERNSHIP: "实习",
  FULL_TIME: "全职",
  CAMPUS: "校招",
};

const educationLabels = {
  ANY: "不限",
  BACHELOR: "本科",
  MASTER: "硕士",
  DOCTOR: "博士",
};

const metaItems = computed(() => {
  if (!detail.value) {
    return [];
  }

  return [
    { label: "公司", value: detail.value.companyName || "待补充" },
    { label: "城市", value: detail.value.city || "待补充" },
    { label: "类型", value: typeLabels[detail.value.jobType] || detail.value.jobType || "岗位" },
    {
      label: "学历要求",
      value:
        educationLabels[detail.value.educationRequirement]
        || detail.value.educationRequirement
        || "以岗位要求为准",
    },
    { label: "来源渠道", value: detail.value.sourcePlatform || "待补充" },
    { label: "截止时间", value: formatDate(detail.value.deadlineAt) },
  ];
});

const applyButtonLabel = computed(() => {
  if (detail.value?.appliedByMe) {
    return "已投递";
  }
  if (applySubmitting.value) {
    return "提交中...";
  }
  return applyPanelOpen.value ? "收起投递面板" : "站内投递";
});

function normalizeDetail(payload) {
  return {
    appliedByMe: false,
    applicationId: null,
    favoritedByMe: false,
    ...payload,
  };
}

function formatDate(value) {
  if (!value) {
    return "长期开放";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "长期开放";
  }

  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "numeric",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

async function loadDetail() {
  loading.value = true;
  errorMessage.value = "";
  actionError.value = "";
  actionMessage.value = "";
  applyPanelOpen.value = false;
  resumesError.value = "";
  selectedResumeId.value = "";

  try {
    detail.value = normalizeDetail(await getJobDetail(route.params.id));
  } catch (error) {
    errorMessage.value = error.message || "岗位详情加载失败，请稍后重试。";
  } finally {
    loading.value = false;
  }
}

function redirectToLogin() {
  router.push({
    name: "login",
    query: { redirect: route.fullPath },
  });
}

function ensureAuthenticated() {
  if (userStore.isAuthenticated) {
    return true;
  }

  redirectToLogin();
  return false;
}

async function ensureResumesLoaded(force = false) {
  if (!userStore.isAuthenticated) {
    return;
  }

  if (resumesLoaded.value && !force) {
    return;
  }

  resumesLoading.value = true;
  resumesError.value = "";

  try {
    const payload = await getMyResumes();
    resumes.value = payload?.resumes || [];
    resumesLoaded.value = true;

    if (resumes.value.length === 1) {
      selectedResumeId.value = String(resumes.value[0].id);
    }
  } catch (error) {
    resumesError.value = error.message || "简历库加载失败，请稍后重试。";
  } finally {
    resumesLoading.value = false;
  }
}

async function handleToggleFavorite() {
  if (!ensureAuthenticated()) {
    return;
  }

  actionError.value = "";
  actionMessage.value = "";
  favoriteLoading.value = true;

  try {
    const nextDetail = detail.value?.favoritedByMe
      ? await unfavoriteJob(detail.value.id)
      : await favoriteJob(detail.value.id);

    detail.value = normalizeDetail({
      ...detail.value,
      ...nextDetail,
    });
  } catch (error) {
    actionError.value = error.message || "收藏操作失败，请稍后重试。";
  } finally {
    favoriteLoading.value = false;
  }
}

async function handleToggleApply() {
  if (!ensureAuthenticated()) {
    return;
  }

  if (detail.value?.appliedByMe) {
    return;
  }

  actionError.value = "";
  actionMessage.value = "";
  applyPanelOpen.value = !applyPanelOpen.value;

  if (applyPanelOpen.value) {
    await ensureResumesLoaded();
  }
}

async function handleApplySubmit() {
  if (!detail.value?.id || detail.value.appliedByMe) {
    return;
  }

  actionError.value = "";
  actionMessage.value = "";

  if (!selectedResumeId.value) {
    actionError.value = "请先选择一份简历。";
    return;
  }

  applySubmitting.value = true;

  try {
    const response = await applyToJob(detail.value.id, {
      resumeId: Number(selectedResumeId.value),
    });

    detail.value = normalizeDetail({
      ...detail.value,
      appliedByMe: true,
      applicationId: response.id,
    });
    applyPanelOpen.value = false;
    actionMessage.value = "投递已提交。";
  } catch (error) {
    actionError.value = error.message || "投递提交失败，请稍后重试。";
  } finally {
    applySubmitting.value = false;
  }
}

watch(
  () => route.params.id,
  () => {
    loadDetail();
  },
  { immediate: true },
);
</script>

<template>
  <section class="page-stack">
    <article class="section-card">
      <div v-if="loading" class="empty-state">正在加载岗位详情...</div>
      <div v-else-if="errorMessage" class="field-grid">
        <p class="field-error" role="alert">{{ errorMessage }}</p>
        <button type="button" class="ghost-btn" @click="loadDetail">
          重试
        </button>
      </div>
      <div v-else-if="detail" class="job-detail">
        <div class="job-detail__main">
          <div class="chip-row">
            <span class="section-eyebrow">岗位详情</span>
            <span v-if="detail.favoritedByMe" class="status-badge approved">已收藏</span>
            <span v-if="detail.appliedByMe" class="status-badge approved">已投递</span>
          </div>

          <h1 class="hero-title" style="margin-top: 18px;">{{ detail.title }}</h1>
          <hr class="editorial-rule" />
          <p class="hero-copy">{{ detail.summary }}</p>

          <div class="job-detail__meta-grid">
            <article
              v-for="item in metaItems"
              :key="item.label"
              class="panel-card job-detail__meta-card"
            >
              <span class="job-detail__meta-label">{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </article>
          </div>

          <article class="panel-card">
            <span class="section-eyebrow">岗位说明</span>
            <p class="job-detail__body">{{ detail.content || detail.summary }}</p>
          </article>
        </div>

        <aside class="job-detail__aside">
          <article class="panel-card">
            <span class="section-eyebrow">常用操作</span>
            <div class="field-grid" style="margin-top: 16px;">
              <button
                data-testid="apply-toggle"
                type="button"
                class="app-btn"
                :disabled="detail.appliedByMe || applySubmitting"
                @click="handleToggleApply"
              >
                {{ applyButtonLabel }}
              </button>
              <a
                data-testid="source-link"
                class="ghost-btn"
                :href="detail.sourceUrl"
                target="_blank"
                rel="noreferrer"
              >
                查看原链接
              </a>
              <button
                data-testid="favorite-toggle"
                type="button"
                class="ghost-btn"
                :disabled="favoriteLoading"
                @click="handleToggleFavorite"
              >
                {{ detail.favoritedByMe ? "取消收藏" : "收藏岗位" }}
              </button>
              <RouterLink to="/jobs" class="ghost-btn">
                返回岗位列表
              </RouterLink>
            </div>
          </article>

          <article v-if="applyPanelOpen && !detail.appliedByMe" class="panel-card apply-panel">
            <strong>站内投递</strong>

            <div v-if="resumesLoading" class="empty-state apply-panel__state">
              正在加载你的简历库...
            </div>
            <div v-else-if="resumesError" class="field-grid">
              <p class="field-error" role="alert">{{ resumesError }}</p>
              <button type="button" class="ghost-btn" @click="ensureResumesLoaded(true)">
                重试加载简历
              </button>
            </div>
            <div v-else-if="!resumes.length" class="field-grid apply-panel__state">
              <p class="meta-copy">
                请先上传一份简历，这样系统才能为这次投递保存稳定的快照记录。
              </p>
              <RouterLink to="/profile/resumes" class="app-link">
                打开简历库
              </RouterLink>
            </div>
            <div v-else class="field-grid" style="margin-top: 16px;">
              <p class="meta-copy">请选择一份简历用于本次投递。</p>

              <label
                v-for="resume in resumes"
                :key="resume.id"
                class="panel-card apply-option"
              >
                <input
                  v-model="selectedResumeId"
                  type="radio"
                  name="resumeId"
                  :value="String(resume.id)"
                />
                <span class="apply-option__copy">
                  <strong>{{ resume.title }}</strong>
                  <small>{{ resume.fileName || "简历文件" }}</small>
                </span>
              </label>

              <div class="inline-form-actions">
                <button
                  data-testid="submit-application"
                  type="button"
                  class="app-btn"
                  :disabled="applySubmitting"
                  @click="handleApplySubmit"
                >
                  {{ applySubmitting ? "提交中..." : "确认投递" }}
                </button>
                <RouterLink to="/profile/resumes" class="ghost-btn">
                  管理简历
                </RouterLink>
              </div>
            </div>
          </article>

          <article v-if="detail.appliedByMe" class="panel-card apply-panel apply-panel--success">
            <strong>已投递</strong>
            <p class="meta-copy" style="margin-top: 12px;">
              这个岗位已经保存了你的站内投递记录，你可以随时在投递记录里查看对应快照和状态。
            </p>
            <RouterLink to="/profile/applications" class="app-link" style="margin-top: 16px;">
              查看我的投递
            </RouterLink>
          </article>

          <article class="panel-card">
            <strong>来源提醒</strong>
            <p class="meta-copy" style="margin-top: 12px;">
              建议同时保留原始招聘页，方便核对岗位细节。站内投递和外部来源链接会并行保留，便于回看。
            </p>
          </article>

          <p v-if="actionMessage" class="field-hint">{{ actionMessage }}</p>
          <p v-if="actionError" class="field-error" role="alert">{{ actionError }}</p>
        </aside>
      </div>
    </article>
  </section>
</template>

<style scoped>
.job-detail {
  display: grid;
  grid-template-columns: 1.25fr 0.75fr;
  gap: var(--cp-gap-6);
}

.job-detail__main,
.job-detail__aside {
  display: grid;
  gap: var(--cp-gap-4);
}

.job-detail__meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cp-gap-4);
}

.job-detail__meta-card {
  min-height: 116px;
  display: grid;
  gap: 8px;
  align-content: end;
}

.job-detail__meta-label {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.job-detail__meta-card strong {
  font-family: var(--cp-font-display);
  font-size: 22px;
  line-height: 1.2;
}

.job-detail__body {
  margin: 16px 0 0;
  white-space: pre-wrap;
  line-height: 1.8;
  color: var(--cp-ink);
}

.apply-panel {
  display: grid;
  gap: var(--cp-gap-4);
}

.apply-panel__state {
  min-height: 128px;
  align-content: center;
}

.apply-panel--success {
  background:
    linear-gradient(180deg, rgba(244, 250, 246, 0.92), rgba(237, 246, 241, 0.98)),
    radial-gradient(circle at top right, rgba(37, 98, 77, 0.12), transparent 36%);
}

.apply-option {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 12px;
  align-items: start;
  cursor: pointer;
}

.apply-option input {
  margin-top: 4px;
}

.apply-option__copy {
  display: grid;
  gap: 6px;
}

.apply-option__copy small {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

@media (max-width: 1023px) {
  .job-detail,
  .job-detail__meta-grid {
    grid-template-columns: 1fr;
  }
}
</style>
