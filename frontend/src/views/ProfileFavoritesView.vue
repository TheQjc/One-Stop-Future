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
    label: "经验帖",
    emptyText: "你还没有收藏任何社区帖子。",
    load: getMyPostFavorites,
  },
  JOB: {
    label: "岗位",
    emptyText: "你还没有收藏任何岗位。",
    load: getMyJobFavorites,
  },
  RESOURCE: {
    label: "资源",
    emptyText: "你还没有收藏任何资源文件。",
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
    errorMessage.value = error.message || "收藏内容加载失败，请稍后重试。";
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
      <span class="section-eyebrow">我的收藏</span>
      <h1 class="page-title" style="margin-top: 16px;">收藏总览</h1>
      <p class="page-subtitle" style="margin-top: 16px;">
        把帖子、岗位和资源放在同一个回看入口里，下一次继续决策时就不用再回忆它们最初出现在哪一页。
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

      <div v-if="loading" class="empty-state">正在加载你的收藏内容...</div>
      <div v-else-if="errorMessage" class="field-grid">
        <p class="field-error" role="alert">{{ errorMessage }}</p>
        <button type="button" class="ghost-btn" @click="loadFavorites">
          重试
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
