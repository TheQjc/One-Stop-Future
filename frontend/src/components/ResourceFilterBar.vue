<script setup>
const props = defineProps({
  filters: {
    type: Object,
    required: true,
  },
  loading: {
    type: Boolean,
    default: false,
  },
  categoryOptions: {
    type: Array,
    default: () => [],
  },
});

const emit = defineEmits(["update:filters", "submit", "reset"]);

function patchFilters(nextValues) {
  emit("update:filters", {
    ...props.filters,
    ...nextValues,
  });
}
</script>

<template>
  <form class="resource-filter-bar" @submit.prevent="emit('submit')">
    <div class="field-grid resource-filter-bar__grid">
      <label class="field-label">
        关键词
        <input
          :value="filters.keyword"
          class="field-control"
          name="keyword"
          type="text"
          placeholder="搜索标题、摘要或资料说明"
          @input="patchFilters({ keyword: $event.target.value })"
        />
      </label>

      <label class="field-label">
        分类
        <select
          :value="filters.category"
          class="field-select"
          name="category"
          @change="patchFilters({ category: $event.target.value })"
        >
          <option v-for="option in categoryOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
      </label>
    </div>

    <div class="inline-form-actions">
      <button type="submit" class="app-btn" :disabled="loading">
        {{ loading ? "筛选中..." : "应用筛选" }}
      </button>
      <button type="button" class="ghost-btn" :disabled="loading" @click="emit('reset')">
        重置
      </button>
    </div>
  </form>
</template>

<style scoped>
.resource-filter-bar {
  display: grid;
  gap: var(--cp-gap-5);
}

.resource-filter-bar__grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

@media (max-width: 767px) {
  .resource-filter-bar__grid {
    grid-template-columns: 1fr;
  }
}
</style>
