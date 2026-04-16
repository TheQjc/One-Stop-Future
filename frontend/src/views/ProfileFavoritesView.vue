<script setup>
import { onMounted, ref, watch } from "vue";
import CommunityPostCard from "../components/CommunityPostCard.vue";
import JobPostingCard from "../components/JobPostingCard.vue";
import ResourceCard from "../components/ResourceCard.vue";
import { getMyPostFavorites } from "../api/community.js";
import { getMyJobFavorites } from "../api/jobs.js";
import { getMyResourceFavorites } from "../api/resources.js";

const loading = ref(true);
const errorMessage = ref("");
const favoriteType = ref("POST");
const summary = ref({
  total: 0,
  posts: [],
  jobs: [],
  resources: [],
});

const tabConfig = {
  POST: {
    label: "Posts",
    emptyText: "You have not saved any community posts yet.",
    load: getMyPostFavorites,
  },
  JOB: {
    label: "Jobs",
    emptyText: "You have not saved any job cards yet.",
    load: getMyJobFavorites,
  },
  RESOURCE: {
    label: "Resources",
    emptyText: "You have not saved any resource files yet.",
    load: getMyResourceFavorites,
  },
};

function currentItems() {
  if (favoriteType.value === "POST") {
    return summary.value.posts || [];
  }
  if (favoriteType.value === "JOB") {
    return summary.value.jobs || [];
  }
  return summary.value.resources || [];
}

async function loadFavorites() {
  loading.value = true;
  errorMessage.value = "";

  try {
    summary.value = await tabConfig[favoriteType.value].load();
  } catch (error) {
    errorMessage.value = error.message || "Favorites loading failed. Please try again.";
  } finally {
    loading.value = false;
  }
}

watch(favoriteType, () => {
  loadFavorites();
});

onMounted(loadFavorites);
</script>

<template>
  <section class="page-stack">
    <article class="section-card">
      <span class="section-eyebrow">My Favorites</span>
      <h1 class="page-title" style="margin-top: 16px;">Saved Board</h1>
      <p class="page-subtitle" style="margin-top: 16px;">
        Keep posts, jobs, and resource files on one return surface so the next decision does not
        depend on remembering where you found them.
      </p>
    </article>

    <article class="section-card">
      <div class="inline-form-actions" style="margin-bottom: 24px;">
        <button
          v-for="type in Object.keys(tabConfig)"
          :key="type"
          type="button"
          class="ghost-btn"
          :class="{ 'favorite-switch--active': favoriteType === type }"
          @click="favoriteType = type"
        >
          {{ tabConfig[type].label }}
        </button>
      </div>

      <div v-if="loading" class="empty-state">Loading your saved board...</div>
      <div v-else-if="errorMessage" class="field-grid">
        <p class="field-error" role="alert">{{ errorMessage }}</p>
        <button type="button" class="ghost-btn" @click="loadFavorites">
          Retry
        </button>
      </div>
      <div v-else-if="!currentItems().length" class="empty-state">
        {{ tabConfig[favoriteType].emptyText }}
      </div>
      <div v-else class="favorites-grid">
        <CommunityPostCard
          v-if="favoriteType === 'POST'"
          v-for="post in summary.posts"
          :key="post.id"
          :post="post"
          compact
        />
        <JobPostingCard
          v-if="favoriteType === 'JOB'"
          v-for="job in summary.jobs"
          :key="job.id"
          :job="job"
          compact
        />
        <ResourceCard
          v-if="favoriteType === 'RESOURCE'"
          v-for="resource in summary.resources"
          :key="resource.id"
          :resource="resource"
          compact
        />
      </div>
    </article>
  </section>
</template>

<style scoped>
.favorite-switch--active {
  border-color: rgba(197, 79, 45, 0.28);
  background: rgba(197, 79, 45, 0.08);
  color: var(--cp-accent-deep);
}

.favorites-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cp-gap-4);
}

@media (max-width: 1023px) {
  .favorites-grid {
    grid-template-columns: 1fr;
  }
}
</style>
