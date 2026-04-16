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
  INTERNSHIP: "Internship",
  FULL_TIME: "Full Time",
  CAMPUS: "Campus",
};

const educationLabels = {
  ANY: "Any",
  BACHELOR: "Bachelor",
  MASTER: "Master",
  DOCTOR: "Doctor",
};

const localizedType = computed(() => typeLabels[props.job.jobType] || props.job.jobType || "Role");
const localizedEducation = computed(() => (
  educationLabels[props.job.educationRequirement] || props.job.educationRequirement || "Requirement"
));

function formatDate(value) {
  if (!value) {
    return "Open";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "Open";
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
      <span v-if="job.favoritedByMe" class="status-badge approved">Saved</span>
    </div>

    <div class="job-posting-card__header">
      <h3 class="job-posting-card__title">{{ job.title }}</h3>
      <p class="job-posting-card__company">{{ job.companyName }}</p>
    </div>

    <p class="job-posting-card__summary">{{ job.summary || "No summary yet." }}</p>

    <div class="job-posting-card__meta">
      <span>{{ job.city || "City TBD" }}</span>
      <span>{{ localizedEducation }}</span>
      <span>{{ job.sourcePlatform || "Source TBD" }}</span>
      <span>Deadline {{ formatDate(job.deadlineAt) }}</span>
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
