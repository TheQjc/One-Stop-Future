<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { RouterLink } from "vue-router";
import { getJobs } from "../api/jobs.js";
import JobFilterBar from "../components/JobFilterBar.vue";
import JobPostingCard from "../components/JobPostingCard.vue";
import { useUserStore } from "../stores/user.js";

const userStore = useUserStore();

const loading = ref(false);
const errorMessage = ref("");
const filters = reactive({
  keyword: "",
  city: "",
  jobType: "",
  educationRequirement: "",
  sourcePlatform: "",
});
const summary = ref({
  total: 0,
  jobs: [],
});

const cityOptions = [
  { value: "", label: "全部城市" },
  { value: "Shenzhen", label: "深圳" },
  { value: "Guangzhou", label: "广州" },
  { value: "Shanghai", label: "上海" },
  { value: "Hangzhou", label: "杭州" },
];

const jobTypeOptions = [
  { value: "", label: "全部类型" },
  { value: "INTERNSHIP", label: "实习" },
  { value: "FULL_TIME", label: "全职" },
  { value: "CAMPUS", label: "校招" },
];

const educationOptions = [
  { value: "", label: "全部学历" },
  { value: "ANY", label: "不限" },
  { value: "BACHELOR", label: "本科" },
  { value: "MASTER", label: "硕士" },
  { value: "DOCTOR", label: "博士" },
];

const sourcePlatformOptions = [
  { value: "", label: "全部来源" },
  { value: "Official Site", label: "官网" },
  { value: "WeCom Channel", label: "企业微信" },
  { value: "Internal Referral", label: "内推" },
];

function getOptionLabel(options, value) {
  return options.find((option) => option.value === value)?.label || value;
}

const activeFilterLabels = computed(() => {
  const items = [];
  if (filters.keyword) items.push(`关键词：${filters.keyword}`);
  if (filters.city) items.push(`城市：${getOptionLabel(cityOptions, filters.city)}`);
  if (filters.jobType) items.push(`岗位类型：${getOptionLabel(jobTypeOptions, filters.jobType)}`);
  if (filters.educationRequirement) items.push(`学历要求：${getOptionLabel(educationOptions, filters.educationRequirement)}`);
  if (filters.sourcePlatform) items.push(`来源：${getOptionLabel(sourcePlatformOptions, filters.sourcePlatform)}`);
  return items;
});

function buildParams() {
  return Object.fromEntries(
    Object.entries(filters).filter(([, value]) => value),
  );
}

async function loadJobs() {
  loading.value = true;
  errorMessage.value = "";

  try {
    summary.value = await getJobs(buildParams());
  } catch (error) {
    errorMessage.value = error.message || "岗位信息加载失败，请稍后重试。";
  } finally {
    loading.value = false;
  }
}

function updateFilters(nextFilters) {
  Object.assign(filters, nextFilters);
}

function resetFilters() {
  Object.assign(filters, {
    keyword: "",
    city: "",
    jobType: "",
    educationRequirement: "",
    sourcePlatform: "",
  });
  loadJobs();
}

onMounted(loadJobs);
</script>

<template>
  <section class="page-stack">
    <article class="section-card jobs-hero">
      <div class="jobs-hero__copy">
        <span class="section-eyebrow">岗位专区</span>
        <h1 class="hero-title" style="margin-top: 18px;">先看清岗位条件，再决定要投哪一个机会。</h1>
        <hr class="editorial-rule" />
        <p class="hero-copy">
          岗位页会把公开岗位卡片、筛选条件和原始来源放在同一页，方便你先比较城市、类型、
          学历要求和截止时间，再决定是否继续查看详情。
        </p>

        <div v-if="activeFilterLabels.length" class="chip-row" style="margin-top: 24px;">
          <span
            v-for="item in activeFilterLabels"
            :key="item"
            class="status-badge pending"
          >
            {{ item }}
          </span>
        </div>
      </div>

      <div class="jobs-hero__panel">
        <div class="panel-card jobs-hero__stat">
          <span class="jobs-hero__label">已收录岗位</span>
          <strong>{{ summary.total }}</strong>
        </div>
        <div class="panel-card jobs-hero__stat">
          <span class="jobs-hero__label">筛选维度</span>
          <strong>5</strong>
        </div>
        <RouterLink :to="userStore.isAuthenticated ? '/profile/favorites' : '/login'" class="app-btn">
          {{ userStore.isAuthenticated ? "查看我的收藏" : "登录后收藏岗位" }}
        </RouterLink>
      </div>
    </article>

    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">岗位筛选</span>
          <h2 class="page-title" style="margin-top: 16px;">先缩小范围，再继续比较岗位。</h2>
        </div>
      </div>

      <JobFilterBar
        :filters="filters"
        :loading="loading"
        :city-options="cityOptions"
        :job-type-options="jobTypeOptions"
        :education-options="educationOptions"
        :source-platform-options="sourcePlatformOptions"
        @update:filters="updateFilters"
        @submit="loadJobs"
        @reset="resetFilters"
      />
    </article>

    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">岗位列表</span>
          <h2 class="page-title" style="margin-top: 16px;">当前岗位</h2>
          <p class="page-subtitle" style="margin-top: 16px;">
            岗位卡片会集中展示城市、学历要求、来源渠道和截止时间，方便你不离开页面也能快速比较。
          </p>
        </div>
      </div>

      <div v-if="loading" class="empty-state">正在加载岗位信息...</div>
      <div v-else-if="errorMessage" class="field-grid">
        <p class="field-error" role="alert">{{ errorMessage }}</p>
        <button type="button" class="ghost-btn" @click="loadJobs">
          重试
        </button>
      </div>
      <div v-else-if="!summary.jobs.length" class="empty-state">
        当前筛选条件下还没有匹配的岗位，试试重置筛选后再看看。
      </div>
      <div v-else class="jobs-grid">
        <JobPostingCard
          v-for="job in summary.jobs"
          :key="job.id"
          :job="job"
        />
      </div>
    </article>
  </section>
</template>

<style scoped>
.jobs-hero {
  display: grid;
  grid-template-columns: 1.25fr 0.75fr;
  gap: var(--cp-gap-6);
  background:
    linear-gradient(180deg, rgba(255, 250, 244, 0.92), rgba(251, 245, 237, 0.98)),
    radial-gradient(circle at top left, rgba(197, 79, 45, 0.12), transparent 28%),
    radial-gradient(circle at bottom right, rgba(76, 122, 116, 0.16), transparent 32%);
}

.jobs-hero__copy,
.jobs-hero__panel {
  display: grid;
  gap: var(--cp-gap-4);
}

.jobs-hero__panel {
  align-content: end;
}

.jobs-hero__stat {
  min-height: 132px;
  display: grid;
  gap: 8px;
  align-content: end;
}

.jobs-hero__label {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.jobs-hero__stat strong {
  font-size: clamp(26px, 4vw, 34px);
  font-family: var(--cp-font-display);
}

.jobs-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cp-gap-4);
}

@media (max-width: 1023px) {
  .jobs-hero,
  .jobs-grid {
    grid-template-columns: 1fr;
  }
}
</style>
