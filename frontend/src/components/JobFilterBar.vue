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
  cityOptions: {
    type: Array,
    default: () => [],
  },
  jobTypeOptions: {
    type: Array,
    default: () => [],
  },
  educationOptions: {
    type: Array,
    default: () => [],
  },
  sourcePlatformOptions: {
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

function handleReset() {
  emit("reset");
}
</script>

<template>
  <form class="job-filter-bar" @submit.prevent="emit('submit')">
    <div class="field-grid job-filter-bar__grid">
      <label class="field-label">
        关键词
        <input
          :value="filters.keyword"
          class="field-control"
          name="keyword"
          type="text"
          placeholder="搜索岗位名称、公司或摘要"
          @input="patchFilters({ keyword: $event.target.value })"
        />
      </label>

      <label class="field-label">
        城市
        <select
          :value="filters.city"
          class="field-select"
          name="city"
          @change="patchFilters({ city: $event.target.value })"
        >
          <option v-for="option in cityOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
      </label>

      <label class="field-label">
        类型
        <select
          :value="filters.jobType"
          class="field-select"
          name="jobType"
          @change="patchFilters({ jobType: $event.target.value })"
        >
          <option v-for="option in jobTypeOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
      </label>

      <label class="field-label">
        学历要求
        <select
          :value="filters.educationRequirement"
          class="field-select"
          name="educationRequirement"
          @change="patchFilters({ educationRequirement: $event.target.value })"
        >
          <option v-for="option in educationOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
      </label>

      <label class="field-label">
        来源
        <select
          :value="filters.sourcePlatform"
          class="field-select"
          name="sourcePlatform"
          @change="patchFilters({ sourcePlatform: $event.target.value })"
        >
          <option v-for="option in sourcePlatformOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
      </label>
    </div>

    <div class="inline-form-actions">
      <button type="submit" class="app-btn" :disabled="loading">
        {{ loading ? "筛选中..." : "应用筛选" }}
      </button>
      <button type="button" class="ghost-btn" :disabled="loading" @click="handleReset">
        重置
      </button>
    </div>
  </form>
</template>

<style scoped>
.job-filter-bar {
  display: grid;
  gap: var(--cp-gap-5);
}

.job-filter-bar__grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

@media (max-width: 767px) {
  .job-filter-bar__grid {
    grid-template-columns: 1fr;
  }
}
</style>
