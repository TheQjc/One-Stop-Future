<script setup>
import { computed } from "vue";
import { RouterLink } from "vue-router";

const props = defineProps({
  resource: {
    type: Object,
    required: true,
  },
  compact: {
    type: Boolean,
    default: false,
  },
});

const categoryLabels = {
  EXAM_PAPER: "真题试卷",
  LANGUAGE_TEST: "语言考试",
  RESUME_TEMPLATE: "简历模板",
  INTERVIEW_EXPERIENCE: "面试经验",
  OTHER: "其他资料",
};

const localizedCategory = computed(() => (
  categoryLabels[props.resource.category] || props.resource.category || "资料"
));

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

function formatSize(value) {
  const size = Number(value || 0);
  if (!size) {
    return "大小未知";
  }
  if (size >= 1024 * 1024) {
    return `${(size / (1024 * 1024)).toFixed(1)} MB`;
  }
  if (size >= 1024) {
    return `${Math.round(size / 1024)} KB`;
  }
  return `${size} B`;
}
</script>

<template>
  <RouterLink
    class="resource-card"
    :class="{ 'resource-card--compact': compact }"
    :to="`/resources/${resource.id}`"
  >
    <div class="resource-card__topline">
      <span class="resource-card__pill">{{ localizedCategory }}</span>
      <span class="resource-card__file">{{ resource.fileName || "资料文件" }}</span>
      <span v-if="resource.favoritedByMe" class="status-badge approved">已收藏</span>
    </div>

    <div class="resource-card__header">
      <h3 class="resource-card__title">{{ resource.title }}</h3>
      <p class="resource-card__summary">{{ resource.summary || "暂未提供资料摘要" }}</p>
    </div>

    <div class="resource-card__meta">
      <span>{{ resource.uploaderNickname || "资料库" }}</span>
      <span>{{ formatSize(resource.fileSize) }}</span>
      <span>{{ resource.downloadCount || 0 }} 次下载</span>
      <span>发布 {{ formatDate(resource.publishedAt) }}</span>
    </div>
  </RouterLink>
</template>

<style scoped>
.resource-card {
  min-height: 100%;
  padding: 22px;
  display: grid;
  gap: 14px;
  border-radius: var(--cp-radius-md);
  border: 1px solid rgba(24, 38, 63, 0.12);
  background:
    linear-gradient(180deg, rgba(255, 252, 247, 0.94), rgba(248, 242, 232, 0.98)),
    radial-gradient(circle at top right, rgba(184, 130, 35, 0.18), transparent 42%),
    radial-gradient(circle at bottom left, rgba(76, 122, 116, 0.12), transparent 34%);
  box-shadow: var(--cp-shadow-soft);
  transition:
    transform var(--cp-transition),
    border-color var(--cp-transition),
    box-shadow var(--cp-transition);
}

.resource-card:hover {
  transform: translateY(-2px);
  border-color: rgba(184, 130, 35, 0.26);
  box-shadow: var(--cp-shadow-card);
}

.resource-card--compact {
  padding: 18px;
}

.resource-card__topline,
.resource-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.resource-card__header {
  display: grid;
  gap: 8px;
}

.resource-card__pill,
.resource-card__file,
.resource-card__meta {
  font-size: var(--cp-text-sm);
}

.resource-card__pill,
.resource-card__file {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: var(--cp-radius-pill);
}

.resource-card__pill {
  background: rgba(24, 38, 63, 0.08);
  color: var(--cp-ink);
}

.resource-card__file {
  background: rgba(184, 130, 35, 0.1);
  color: var(--cp-warning);
}

.resource-card__title {
  margin: 0;
  font-family: var(--cp-font-display);
  font-size: clamp(24px, 4vw, 30px);
  line-height: 1.14;
}

.resource-card__summary,
.resource-card__meta {
  margin: 0;
  color: var(--cp-ink-soft);
  line-height: 1.7;
}
</style>
