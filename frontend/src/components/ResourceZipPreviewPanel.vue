<script setup>
const props = defineProps({
  loading: { type: Boolean, default: false },
  errorMessage: { type: String, default: "" },
  preview: { type: Object, default: null },
});

function formatSize(value) {
  if (value == null) {
    return "Directory";
  }

  const size = Number(value || 0);
  if (!size) {
    return "0 B";
  }
  if (size >= 1024 * 1024) {
    return `${(size / (1024 * 1024)).toFixed(1)} MB`;
  }
  if (size >= 1024) {
    return `${Math.round(size / 1024)} KB`;
  }
  return `${size} B`;
}
</script>

<template>
  <article class="panel-card zip-preview-panel">
    <div class="zip-preview-panel__header">
      <div>
        <span class="section-eyebrow">Archive Contents</span>
        <h3 class="zip-preview-panel__title">
          {{ preview?.fileName || "ZIP Preview" }}
        </h3>
      </div>
      <span v-if="preview" class="status-badge pending">
        {{ preview.entryCount }} entries
      </span>
    </div>

    <div v-if="loading" class="empty-state">Loading archive contents...</div>
    <p v-else-if="errorMessage" class="field-error" role="alert">{{ errorMessage }}</p>
    <div v-else-if="!preview" class="empty-state">Preview not loaded yet.</div>
    <div v-else-if="!preview.entries?.length" class="empty-state">This archive is empty.</div>
    <div v-else class="zip-preview-panel__list">
      <article
        v-for="entry in preview.entries"
        :key="entry.path"
        class="zip-preview-panel__entry"
      >
        <div class="zip-preview-panel__entry-main">
          <span class="status-badge" :class="entry.directory ? 'pending' : 'approved'">
            {{ entry.directory ? "Dir" : "File" }}
          </span>
          <div>
            <strong class="zip-preview-panel__entry-name">{{ entry.name }}</strong>
            <p class="meta-copy zip-preview-panel__entry-path">{{ entry.path }}</p>
          </div>
        </div>
        <span class="zip-preview-panel__entry-size">{{ formatSize(entry.size) }}</span>
      </article>
    </div>
  </article>
</template>

<style scoped>
.zip-preview-panel {
  display: grid;
  gap: var(--cp-gap-4);
  background:
    linear-gradient(180deg, rgba(255, 250, 243, 0.96), rgba(246, 238, 227, 0.98)),
    radial-gradient(circle at top left, rgba(197, 79, 45, 0.1), transparent 30%);
}

.zip-preview-panel__header {
  display: flex;
  justify-content: space-between;
  gap: var(--cp-gap-4);
  align-items: start;
}

.zip-preview-panel__title {
  margin: 8px 0 0;
  font-family: var(--cp-font-display);
  font-size: clamp(22px, 3vw, 28px);
  line-height: 1.14;
}

.zip-preview-panel__list {
  display: grid;
  gap: 12px;
}

.zip-preview-panel__entry {
  display: flex;
  justify-content: space-between;
  gap: var(--cp-gap-4);
  align-items: center;
  padding: 14px 16px;
  border-radius: var(--cp-radius-sm);
  border: 1px solid rgba(24, 38, 63, 0.08);
  background: rgba(255, 255, 255, 0.72);
}

.zip-preview-panel__entry-main {
  min-width: 0;
  display: flex;
  gap: 12px;
  align-items: start;
}

.zip-preview-panel__entry-name {
  display: block;
}

.zip-preview-panel__entry-path {
  margin: 6px 0 0;
  word-break: break-all;
}

.zip-preview-panel__entry-size {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
  white-space: nowrap;
}

@media (max-width: 767px) {
  .zip-preview-panel__header,
  .zip-preview-panel__entry {
    flex-direction: column;
    align-items: start;
  }

  .zip-preview-panel__entry-size {
    white-space: normal;
  }
}
</style>
