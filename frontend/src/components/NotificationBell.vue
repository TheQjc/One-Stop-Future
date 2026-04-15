<script setup>
import { computed } from "vue";
import { RouterLink } from "vue-router";

const props = defineProps({
  count: {
    type: Number,
    default: 0,
  },
});

const displayCount = computed(() => (props.count > 99 ? "99+" : String(props.count)));
</script>

<template>
  <RouterLink to="/notifications" class="notification-bell" aria-label="通知中心">
    <span class="notification-bell__icon" aria-hidden="true">•</span>
    <span class="notification-bell__label">通知</span>
    <span v-if="count > 0" class="notification-bell__badge">{{ displayCount }}</span>
  </RouterLink>
</template>

<style scoped>
.notification-bell {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: var(--cp-touch-height);
  padding: 0 14px;
  border-radius: var(--cp-radius-pill);
  border: 1px solid rgba(76, 122, 116, 0.18);
  background: rgba(248, 251, 249, 0.84);
  color: var(--cp-ink);
  transition:
    transform var(--cp-transition),
    border-color var(--cp-transition),
    background-color var(--cp-transition);
}

.notification-bell:hover {
  transform: translateY(-1px);
  border-color: rgba(76, 122, 116, 0.32);
}

.notification-bell__icon {
  color: var(--cp-teal);
  font-size: 10px;
}

.notification-bell__label {
  font-size: var(--cp-text-sm);
  font-weight: 600;
}

.notification-bell__badge {
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  border-radius: var(--cp-radius-pill);
  background: var(--cp-accent);
  color: #fffaf3;
  font-size: 11px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
</style>
