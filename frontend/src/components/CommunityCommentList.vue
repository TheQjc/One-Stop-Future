<script setup>
const props = defineProps({
  comments: {
    type: Array,
    default: () => [],
  },
  openReplyForms: {
    type: Array,
    default: () => [],
  },
  replyDrafts: {
    type: Object,
    default: () => ({}),
  },
  actionLoading: {
    type: String,
    default: "",
  },
});

const emit = defineEmits(["toggle-reply", "update-reply", "submit-reply"]);

function formatDate(value) {
  if (!value) {
    return "Just now";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "Just now";
  }

  return new Intl.DateTimeFormat("zh-CN", {
    month: "numeric",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

function isReplyFormOpen(commentId) {
  return props.openReplyForms.includes(commentId);
}

function replyDraft(commentId) {
  return props.replyDrafts?.[commentId] || "";
}

function isReplySubmitting(commentId) {
  return props.actionLoading === `reply-${commentId}`;
}
</script>

<template>
  <div v-if="!comments.length" class="empty-state">
    No comments yet. Be the first to add one.
  </div>

  <div v-else class="community-comment-list">
    <article
      v-for="comment in comments"
      :key="comment.id"
      class="community-comment"
    >
      <div class="community-comment__topline">
        <strong>{{ comment.authorNickname || "Anonymous" }}</strong>
        <span>{{ formatDate(comment.createdAt) }}</span>
      </div>
      <p class="community-comment__content">{{ comment.content }}</p>

      <div class="community-comment__actions">
        <button
          type="button"
          class="app-link"
          :data-reply-trigger="comment.id"
          @click="emit('toggle-reply', comment.id)"
        >
          Reply
        </button>
      </div>

      <form
        v-if="isReplyFormOpen(comment.id)"
        class="community-comment__reply-form field-grid"
        :data-reply-form="comment.id"
        @submit.prevent="emit('submit-reply', comment.id)"
      >
        <label class="field-label">
          Reply
          <textarea
            class="field-textarea"
            :data-reply-field="comment.id"
            :value="replyDraft(comment.id)"
            placeholder="Add a follow-up reply."
            :disabled="isReplySubmitting(comment.id)"
            @input="emit('update-reply', { commentId: comment.id, value: $event.target.value })"
          />
        </label>

        <div class="inline-form-actions">
          <button
            type="submit"
            class="app-btn"
            :data-reply-submit="comment.id"
            :disabled="isReplySubmitting(comment.id)"
          >
            {{ isReplySubmitting(comment.id) ? "Replying..." : "Post Reply" }}
          </button>
        </div>
      </form>

      <div
        v-if="comment.replies?.length"
        class="community-reply-list"
        :data-replies-count="comment.id"
      >
        <article
          v-for="reply in comment.replies"
          :key="reply.id"
          class="community-reply"
        >
          <div class="community-comment__topline">
            <strong>{{ reply.authorNickname || "Anonymous" }}</strong>
            <span>{{ formatDate(reply.createdAt) }}</span>
          </div>
          <p class="community-reply__meta">
            Reply to {{ reply.replyToUserNickname || "Unknown" }}
          </p>
          <p class="community-comment__content">{{ reply.content }}</p>
        </article>
      </div>
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
  line-height: 1.7;
}

.community-comment__actions {
  display: flex;
  justify-content: flex-start;
}

.community-comment__reply-form {
  margin-top: 4px;
}

.community-reply-list {
  display: grid;
  gap: var(--cp-gap-3);
  margin-top: 6px;
  padding: 16px;
  border-radius: var(--cp-radius-md);
  background: rgba(255, 249, 241, 0.72);
  border: 1px solid rgba(197, 79, 45, 0.12);
}

.community-reply {
  display: grid;
  gap: 8px;
  padding: 14px 16px;
  border-radius: var(--cp-radius-md);
  background: rgba(255, 255, 255, 0.7);
}

.community-reply__meta {
  margin: 0;
  color: var(--cp-accent-deep);
  font-size: var(--cp-text-sm);
}
</style>
