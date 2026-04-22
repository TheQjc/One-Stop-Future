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
  POST: "社区",
  JOB: "岗位",
  RESOURCE: "资料",
};

const typeClasses = {
  POST: "discover-item-card__type--post",
  JOB: "discover-item-card__type--job",
  RESOURCE: "discover-item-card__type--resource",
};

const localizedType = computed(() => typeLabels[props.item.type] || props.item.type || "内容");
const typeClassName = computed(() => typeClasses[props.item.type] || "");

function formatDate(value) {
  if (!value) {
    return "待发布";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "待发布";
  }

  return new Intl.DateTimeFormat("zh-CN", {
    month: "numeric",
    day: "numeric",
  }).format(date);
}
</script>

<template>
  <RouterLink class="discover-item-card" :to="item.path">
    <div class="discover-item-card__topline">
      <span class="discover-item-card__type" :class="typeClassName">{{ localizedType }}</span>
      <span class="discover-item-card__label">{{ item.hotLabel || "本周推荐" }}</span>
    </div>

    <div class="discover-item-card__body">
      <h3 class="discover-item-card__title">{{ item.title }}</h3>
      <p class="discover-item-card__summary">{{ item.summary || "暂未提供内容摘要" }}</p>
    </div>

    <div class="discover-item-card__meta">
      <span>{{ item.primaryMeta || "One-Stop Future" }}</span>
      <span>{{ item.secondaryMeta || formatDate(item.publishedAt) }}</span>
    </div>

    <div class="discover-item-card__footer">
      <span class="discover-item-card__date">{{ formatDate(item.publishedAt) }}</span>
      <span class="discover-item-card__score">热度 {{ Math.round(Number(item.hotScore || 0)) }}</span>
    </div>
  </RouterLink>
</template>

<style scoped>
.discover-item-card {
  min-height: 100%;
  padding: 22px;
  display: grid;
  gap: 14px;
  border-radius: var(--cp-radius-md);
  border: 1px solid rgba(24, 38, 63, 0.12);
  background:
    linear-gradient(180deg, rgba(255, 252, 247, 0.96), rgba(248, 240, 229, 0.98)),
    radial-gradient(circle at top right, rgba(197, 79, 45, 0.1), transparent 40%),
    radial-gradient(circle at bottom left, rgba(76, 122, 116, 0.12), transparent 34%);
  box-shadow: var(--cp-shadow-soft);
  transition:
    transform var(--cp-transition),
    border-color var(--cp-transition),
    box-shadow var(--cp-transition);
}

.discover-item-card:hover {
  transform: translateY(-2px);
  border-color: rgba(197, 79, 45, 0.24);
  box-shadow: var(--cp-shadow-card);
}

.discover-item-card__topline,
.discover-item-card__meta,
.discover-item-card__footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.discover-item-card__type,
.discover-item-card__label,
.discover-item-card__meta,
.discover-item-card__footer {
  font-size: var(--cp-text-sm);
}

.discover-item-card__type,
.discover-item-card__label {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: var(--cp-radius-pill);
}

.discover-item-card__type {
  background: rgba(24, 38, 63, 0.08);
  color: var(--cp-ink);
}

.discover-item-card__type--post {
  background: rgba(24, 38, 63, 0.08);
}

.discover-item-card__type--job {
  background: rgba(197, 79, 45, 0.12);
  color: var(--cp-accent-deep);
}

.discover-item-card__type--resource {
  background: rgba(76, 122, 116, 0.12);
  color: var(--cp-teal-deep);
}

.discover-item-card__label {
  background: rgba(255, 255, 255, 0.76);
  color: var(--cp-ink-soft);
  border: 1px solid rgba(24, 38, 63, 0.08);
}

.discover-item-card__body {
  display: grid;
  gap: 8px;
}

.discover-item-card__title {
  margin: 0;
  font-family: var(--cp-font-display);
  font-size: clamp(24px, 4vw, 30px);
  line-height: 1.14;
}

.discover-item-card__summary,
.discover-item-card__meta,
.discover-item-card__date,
.discover-item-card__score {
  margin: 0;
  color: var(--cp-ink-soft);
}

.discover-item-card__summary {
  line-height: 1.7;
}

.discover-item-card__score {
  font-weight: 600;
  color: var(--cp-ink);
}
</style>
