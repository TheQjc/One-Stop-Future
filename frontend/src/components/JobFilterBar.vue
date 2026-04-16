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
        Keyword
        <input
          :value="filters.keyword"
          class="field-control"
          name="keyword"
          type="text"
          placeholder="Title, company, or summary"
          @input="patchFilters({ keyword: $event.target.value })"
        />
      </label>

      <label class="field-label">
        City
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
        Type
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
        Education
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
        Source
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
        {{ loading ? "Loading..." : "Apply Filters" }}
      </button>
      <button type="button" class="ghost-btn" :disabled="loading" @click="handleReset">
        Reset
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
