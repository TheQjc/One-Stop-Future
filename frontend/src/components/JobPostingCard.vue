<script setup>
import { computed } from "vue";
import { RouterLink } from "vue-router";

const props = defineProps({
  job: {
    type: Object,
    required: true,
  },
  compact: {
    type: Boolean,
    default: false,
  },
});

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

const cityLabels = {
  Shenzhen: "深圳",
  Guangzhou: "广州",
  Shanghai: "上海",
  Hangzhou: "杭州",
};

const sourceLabels = {
  "Official Site": "官网",
  "WeCom Channel": "企业微信",
  "Internal Referral": "内推",
};

const localizedType = computed(() => typeLabels[props.job.jobType] || props.job.jobType || "岗位");
const localizedEducation = computed(() => (
  educationLabels[props.job.educationRequirement] || props.job.educationRequirement || "学历要求"
));
const localizedCity = computed(() => cityLabels[props.job.city] || props.job.city || "城市待定");
const localizedSource = computed(() => (
  sourceLabels[props.job.sourcePlatform] || props.job.sourcePlatform || "来源待定"
));

function formatDate(value) {
  if (!value) {
    return "待定";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "待定";
  }

  return new Intl.DateTimeFormat("zh-CN", {
    month: "numeric",
    day: "numeric",
  }).format(date);
}
</script>

<template>
  <RouterLink
    class="job-posting-card"
    :class="{ 'job-posting-card--compact': compact }"
    :to="`/jobs/${job.id}`"
  >
    <div class="job-posting-card__topline">
      <span class="job-posting-card__pill">{{ localizedType }}</span>
      <span v-if="job.favoritedByMe" class="status-badge approved">已收藏</span>
    </div>

    <div class="job-posting-card__header">
      <h3 class="job-posting-card__title">{{ job.title }}</h3>
      <p class="job-posting-card__company">{{ job.companyName || "暂未提供公司信息" }}</p>
    </div>

    <p class="job-posting-card__summary">{{ job.summary || "暂未提供岗位摘要" }}</p>

    <div class="job-posting-card__meta">
      <span>{{ localizedCity }}</span>
      <span>{{ localizedEducation }}</span>
      <span>{{ localizedSource }}</span>
      <span>截止 {{ formatDate(job.deadlineAt) }}</span>
    </div>
  </RouterLink>
</template>

<style scoped>
.job-posting-card {
  min-height: 100%;
  padding: 22px;
  display: grid;
  gap: 14px;
  border-radius: var(--cp-radius-md);
  border: 1px solid rgba(24, 38, 63, 0.12);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.88), rgba(255, 248, 240, 0.96)),
    radial-gradient(circle at top right, rgba(197, 79, 45, 0.14), transparent 42%),
    radial-gradient(circle at bottom left, rgba(76, 122, 116, 0.12), transparent 32%);
  box-shadow: var(--cp-shadow-soft);
  transition:
    transform var(--cp-transition),
    border-color var(--cp-transition),
    box-shadow var(--cp-transition);
}

.job-posting-card:hover {
  transform: translateY(-2px);
  border-color: rgba(197, 79, 45, 0.24);
  box-shadow: var(--cp-shadow-card);
}

.job-posting-card--compact {
  padding: 18px;
}

.job-posting-card__topline,
.job-posting-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.job-posting-card__header {
  display: grid;
  gap: 6px;
}

.job-posting-card__pill,
.job-posting-card__company,
.job-posting-card__meta {
  font-size: var(--cp-text-sm);
}

.job-posting-card__pill {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: var(--cp-radius-pill);
  background: rgba(24, 38, 63, 0.08);
  color: var(--cp-ink);
}

.job-posting-card__title {
  margin: 0;
  font-family: var(--cp-font-display);
  font-size: clamp(24px, 4vw, 30px);
  line-height: 1.14;
}

.job-posting-card__company,
.job-posting-card__meta,
.job-posting-card__summary {
  margin: 0;
  color: var(--cp-ink-soft);
}

.job-posting-card__summary {
  line-height: 1.7;
}
</style>
