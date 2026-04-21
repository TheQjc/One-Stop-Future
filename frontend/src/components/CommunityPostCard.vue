<script setup>
import { computed } from "vue";
import { RouterLink } from "vue-router";

const props = defineProps({
  post: {
    type: Object,
    required: true,
  },
  compact: {
    type: Boolean,
    default: false,
  },
});

const tagLabels = {
  CAREER: "就业",
  EXAM: "考研",
  ABROAD: "留学",
  CHAT: "闲聊",
};

const statusLabels = {
  PUBLISHED: "已发布",
  HIDDEN: "已下架",
  DELETED: "已删除",
};

const localizedTag = computed(() => tagLabels[props.post.tag] || props.post.tag || "未分类");
const localizedStatus = computed(() => statusLabels[props.post.status] || props.post.status || "已发布");
const experienceEnabled = computed(() => Boolean(props.post.experience?.enabled));

const experiencePreview = computed(() => {
  if (!experienceEnabled.value) {
    return props.post.contentPreview || props.post.content || "暂无正文摘要";
  }

  const fields = [
    props.post.experience?.targetLabel,
    props.post.experience?.outcomeLabel,
    props.post.experience?.timelineSummary,
  ].filter(Boolean);

  return fields.length ? fields.join(" · ") : (props.post.contentPreview || props.post.content || "暂无正文摘要");
});

function formatDate(value) {
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
</script>

<template>
  <RouterLink
    class="community-post-card"
    :class="{ 'community-post-card--compact': compact }"
    :to="`/community/${post.id}`"
  >
    <div class="community-post-card__topline">
      <div class="community-post-card__chips">
        <span class="community-post-card__tag">{{ localizedTag }}</span>
        <span v-if="experienceEnabled" class="community-post-card__experience-badge">Experience Post</span>
      </div>
      <span v-if="post.status && post.status !== 'PUBLISHED'" class="status-badge pending">
        {{ localizedStatus }}
      </span>
    </div>

    <h3 class="community-post-card__title">{{ post.title }}</h3>
    <p class="community-post-card__preview">{{ experiencePreview }}</p>

    <div class="community-post-card__meta">
      <span>{{ post.authorNickname || "匿名用户" }}</span>
      <span>{{ formatDate(post.createdAt) }}</span>
    </div>

    <div class="community-post-card__stats">
      <span>赞 {{ post.likeCount || 0 }}</span>
      <span>评 {{ post.commentCount || 0 }}</span>
      <span>藏 {{ post.favoriteCount || 0 }}</span>
    </div>
  </RouterLink>
</template>

<style scoped>
.community-post-card {
  min-height: 100%;
  padding: 22px;
  display: grid;
  gap: 14px;
  border-radius: var(--cp-radius-md);
  border: 1px solid rgba(24, 38, 63, 0.12);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.8), rgba(255, 250, 242, 0.95)),
    radial-gradient(circle at top right, rgba(197, 79, 45, 0.12), transparent 44%);
  box-shadow: var(--cp-shadow-soft);
  transition:
    transform var(--cp-transition),
    border-color var(--cp-transition),
    box-shadow var(--cp-transition);
}

.community-post-card:hover {
  transform: translateY(-2px);
  border-color: rgba(197, 79, 45, 0.2);
  box-shadow: var(--cp-shadow-card);
}

.community-post-card--compact {
  padding: 18px;
}

.community-post-card__topline,
.community-post-card__meta,
.community-post-card__stats {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: space-between;
  align-items: center;
}

.community-post-card__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.community-post-card__tag,
.community-post-card__meta,
.community-post-card__stats {
  font-size: var(--cp-text-sm);
  color: var(--cp-ink-soft);
}

.community-post-card__tag,
.community-post-card__experience-badge {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: var(--cp-radius-pill);
}

.community-post-card__tag {
  background: rgba(24, 38, 63, 0.08);
  color: var(--cp-ink);
}

.community-post-card__experience-badge {
  background: rgba(197, 79, 45, 0.12);
  color: #9a3e1f;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.community-post-card__title {
  margin: 0;
  font-family: var(--cp-font-display);
  font-size: clamp(24px, 4vw, 30px);
  line-height: 1.14;
}

.community-post-card__preview {
  margin: 0;
  color: var(--cp-ink-soft);
  line-height: 1.7;
}
</style>
