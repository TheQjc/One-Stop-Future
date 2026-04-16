<script setup>
import { computed } from "vue";
import { RouterLink } from "vue-router";

const props = defineProps({
  item: {
    type: Object,
    required: true,
  },
});

const typeLabels = {
  POST: "Community",
  JOB: "Job",
  RESOURCE: "Resource",
};

const typeClasses = {
  POST: "search-result-card__type--post",
  JOB: "search-result-card__type--job",
  RESOURCE: "search-result-card__type--resource",
};

const localizedType = computed(() => typeLabels[props.item.type] || props.item.type || "Result");
const typeClassName = computed(() => typeClasses[props.item.type] || "");

function formatDate(value) {
  if (!value) {
    return "Published date pending";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "Published date pending";
  }

  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "numeric",
    day: "numeric",
  }).format(date);
}
</script>

<template>
  <RouterLink class="search-result-card" :to="item.path">
    <div class="search-result-card__topline">
      <span class="search-result-card__type" :class="typeClassName">{{ localizedType }}</span>
      <span class="search-result-card__date">{{ formatDate(item.publishedAt) }}</span>
    </div>

    <div class="search-result-card__body">
      <h3 class="search-result-card__title">{{ item.title }}</h3>
      <p class="search-result-card__summary">{{ item.summary || "No summary provided yet." }}</p>
    </div>

    <div class="search-result-card__meta">
      <span>{{ item.metaPrimary || "Unified Search" }}</span>
      <span>{{ item.metaSecondary || "Published result" }}</span>
    </div>
  </RouterLink>
</template>

<style scoped>
.search-result-card {
  min-height: 100%;
  padding: 22px;
  display: grid;
  gap: 14px;
  border-radius: var(--cp-radius-md);
  border: 1px solid rgba(24, 38, 63, 0.12);
  background:
    linear-gradient(180deg, rgba(255, 253, 249, 0.96), rgba(249, 243, 234, 0.98)),
    radial-gradient(circle at top right, rgba(197, 79, 45, 0.12), transparent 42%),
    radial-gradient(circle at bottom left, rgba(76, 122, 116, 0.1), transparent 34%);
  box-shadow: var(--cp-shadow-soft);
  transition:
    transform var(--cp-transition),
    border-color var(--cp-transition),
    box-shadow var(--cp-transition);
}

.search-result-card:hover {
  transform: translateY(-2px);
  border-color: rgba(197, 79, 45, 0.24);
  box-shadow: var(--cp-shadow-card);
}

.search-result-card__topline,
.search-result-card__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.search-result-card__type,
.search-result-card__date,
.search-result-card__meta {
  font-size: var(--cp-text-sm);
}

.search-result-card__type {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: var(--cp-radius-pill);
  background: rgba(24, 38, 63, 0.08);
  color: var(--cp-ink);
}

.search-result-card__type--post {
  background: rgba(24, 38, 63, 0.08);
}

.search-result-card__type--job {
  background: rgba(197, 79, 45, 0.12);
  color: var(--cp-accent-deep);
}

.search-result-card__type--resource {
  background: rgba(76, 122, 116, 0.12);
  color: var(--cp-teal-deep);
}

.search-result-card__date,
.search-result-card__summary,
.search-result-card__meta {
  margin: 0;
  color: var(--cp-ink-soft);
}

.search-result-card__body {
  display: grid;
  gap: 8px;
}

.search-result-card__title {
  margin: 0;
  font-family: var(--cp-font-display);
  font-size: clamp(24px, 4vw, 30px);
  line-height: 1.14;
}

.search-result-card__summary {
  line-height: 1.7;
}
</style>
