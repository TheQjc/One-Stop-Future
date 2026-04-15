<script setup>
import { computed } from "vue";

const props = defineProps({
  notice: {
    type: Object,
    required: true,
  },
});

const statusClass = computed(() => props.notice.status?.toLowerCase() || "pending");
</script>

<template>
  <article class="notice-card">
    <div class="notice-card__top">
      <div class="chip-row">
        <span class="section-eyebrow">{{ notice.category }}</span>
        <span :class="['status-badge', statusClass]">
          {{
            notice.status === "APPROVED"
              ? "已通过"
              : notice.status === "REJECTED"
                ? "已驳回"
                : "待审核"
          }}
        </span>
      </div>
      <span v-if="notice.isTop" class="notice-card__top-flag">置顶</span>
    </div>
    <h3 class="notice-card__title">{{ notice.title }}</h3>
    <p class="notice-card__summary">{{ notice.summary }}</p>
    <div class="meta-row notice-card__meta">
      <span>{{ notice.author }}</span>
      <span>{{ notice.publishAt }}</span>
    </div>
  </article>
</template>

<style scoped>
.notice-card {
  padding: var(--cp-gap-5);
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid var(--cp-line);
  border-radius: var(--cp-radius-md);
  box-shadow: var(--cp-shadow-soft);
}

.notice-card__top {
  display: flex;
  justify-content: space-between;
  gap: var(--cp-gap-3);
  align-items: center;
}

.notice-card__top-flag {
  color: var(--cp-accent-deep);
  font-weight: 700;
}

.notice-card__title {
  margin: var(--cp-gap-4) 0 var(--cp-gap-3);
  font-size: clamp(20px, 3vw, 24px);
  font-family: var(--cp-font-display);
}

.notice-card__summary {
  margin: 0 0 var(--cp-gap-4);
  color: var(--cp-ink-soft);
}

.notice-card__meta {
  color: var(--cp-ink-faint);
  font-size: var(--cp-text-sm);
}
</style>
