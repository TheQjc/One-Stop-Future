<script setup>
import { computed } from "vue";
import { RouterLink } from "vue-router";

const props = defineProps({
  entry: {
    type: Object,
    required: true,
  },
});

const cardMeta = computed(() => {
  if (props.entry.badge === "COMING_SOON") {
    return "即将开放";
  }

  if (props.entry.badge === "LOGIN_REQUIRED") {
    return "登录后可用";
  }

  return "立即进入";
});
</script>

<template>
  <component
    :is="entry.enabled ? RouterLink : 'div'"
    :to="entry.enabled ? entry.path : undefined"
    class="home-entry-card"
    :class="{ 'home-entry-card--muted': !entry.enabled }"
  >
    <p class="home-entry-card__code">{{ entry.code }}</p>
    <h3 class="home-entry-card__title">{{ entry.title }}</h3>
    <p class="home-entry-card__meta">{{ cardMeta }}</p>
  </component>
</template>

<style scoped>
.home-entry-card {
  min-height: 180px;
  padding: 22px;
  display: grid;
  align-content: end;
  gap: 10px;
  border-radius: 24px;
  border: 1px solid rgba(24, 38, 63, 0.12);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.8), rgba(255, 251, 244, 0.96)),
    radial-gradient(circle at top right, rgba(76, 122, 116, 0.12), transparent 40%);
  box-shadow: var(--cp-shadow-soft);
  transition:
    transform var(--cp-transition),
    border-color var(--cp-transition),
    box-shadow var(--cp-transition);
}

.home-entry-card:hover {
  transform: translateY(-2px);
  border-color: rgba(197, 79, 45, 0.2);
  box-shadow: var(--cp-shadow-card);
}

.home-entry-card--muted {
  opacity: 0.72;
}

.home-entry-card__code,
.home-entry-card__meta {
  margin: 0;
  font-size: var(--cp-text-sm);
  color: var(--cp-ink-soft);
}

.home-entry-card__code {
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.home-entry-card__title {
  margin: 0;
  font-family: var(--cp-font-display);
  font-size: 26px;
  line-height: 1.1;
}
</style>
