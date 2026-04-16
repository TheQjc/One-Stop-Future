<script setup>
const props = defineProps({
  modelValue: {
    type: String,
    default: "",
  },
  options: {
    type: Array,
    default: () => [],
  },
});

const emit = defineEmits(["update:modelValue"]);

function selectOption(value) {
  emit("update:modelValue", value);
}
</script>

<template>
  <div class="community-filter-tabs" role="tablist" aria-label="Community tag filter">
    <button
      v-for="option in options"
      :key="option.value"
      type="button"
      class="community-filter-tabs__button"
      :class="{ 'community-filter-tabs__button--active': modelValue === option.value }"
      @click="selectOption(option.value)"
    >
      <span class="community-filter-tabs__eyebrow">{{ option.eyebrow }}</span>
      <strong>{{ option.label }}</strong>
    </button>
  </div>
</template>

<style scoped>
.community-filter-tabs {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: var(--cp-gap-3);
}

.community-filter-tabs__button {
  min-height: 88px;
  padding: 16px;
  display: grid;
  gap: 6px;
  text-align: left;
  border-radius: var(--cp-radius-md);
  border: 1px solid var(--cp-line);
  background: rgba(255, 255, 255, 0.68);
  color: var(--cp-ink);
  cursor: pointer;
  transition:
    transform var(--cp-transition),
    border-color var(--cp-transition),
    background-color var(--cp-transition),
    box-shadow var(--cp-transition);
}

.community-filter-tabs__button:hover {
  transform: translateY(-1px);
  border-color: rgba(197, 79, 45, 0.22);
  box-shadow: var(--cp-shadow-soft);
}

.community-filter-tabs__button--active {
  border-color: rgba(197, 79, 45, 0.28);
  background:
    linear-gradient(180deg, rgba(255, 251, 245, 0.96), rgba(255, 245, 238, 0.92)),
    radial-gradient(circle at top right, rgba(197, 79, 45, 0.12), transparent 48%);
}

.community-filter-tabs__eyebrow {
  color: var(--cp-accent-deep);
  font-size: 11px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

@media (max-width: 1023px) {
  .community-filter-tabs {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .community-filter-tabs {
    grid-template-columns: 1fr;
  }
}
</style>
