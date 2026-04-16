<script setup>
const props = defineProps({
  comments: {
    type: Array,
    default: () => [],
  },
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
  <div v-if="!comments.length" class="empty-state">
    暂无评论，欢迎成为第一个补充经验的人。
  </div>

  <div v-else class="community-comment-list">
    <article
      v-for="comment in comments"
      :key="comment.id"
      class="community-comment"
    >
      <div class="community-comment__topline">
        <strong>{{ comment.authorNickname || "匿名用户" }}</strong>
        <span>{{ formatDate(comment.createdAt) }}</span>
      </div>
      <p class="community-comment__content">{{ comment.content }}</p>
    </article>
  </div>
</template>

<style scoped>
.community-comment-list {
  display: grid;
  gap: var(--cp-gap-4);
}

.community-comment {
  padding: 18px 20px;
  display: grid;
  gap: 10px;
  border-radius: var(--cp-radius-md);
  border: 1px solid var(--cp-line);
  background: rgba(255, 255, 255, 0.68);
}

.community-comment__topline {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: var(--cp-gap-2);
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.community-comment__content {
  margin: 0;
  color: var(--cp-ink);
}
</style>
