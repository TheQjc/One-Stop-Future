<script setup>
import { onMounted, ref, watch } from "vue";
import CommunityPostCard from "../components/CommunityPostCard.vue";
import JobPostingCard from "../components/JobPostingCard.vue";
import { getMyPostFavorites } from "../api/community.js";
import { getMyJobFavorites } from "../api/jobs.js";

const loading = ref(true);
const errorMessage = ref("");
const favoriteType = ref("POST");
const summary = ref({
  total: 0,
  posts: [],
  jobs: [],
});

async function loadFavorites() {
  loading.value = true;
  errorMessage.value = "";

  try {
    summary.value = favoriteType.value === "POST"
      ? await getMyPostFavorites()
      : await getMyJobFavorites();
  } catch (error) {
    errorMessage.value = error.message || "鎴戠殑鏀惰棌鍔犺浇澶辫触锛岃绋嶅悗閲嶈瘯銆?";
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
      <h1 class="page-title" style="margin-top: 16px;">鎴戠殑鏀惰棌</h1>
      <p class="page-subtitle" style="margin-top: 16px;">
        鏀惰棌鍏堟寜甯栧瓙绫诲瀷鑱氬悎锛屽悗缁矖浣嶅拰璧勬枡搴撲細澶嶇敤鍚屼竴濂楁敹钘忓叆鍙ｃ€?
      </p>
    </article>

    <article class="section-card">
      <div class="inline-form-actions" style="margin-bottom: 24px;">
        <button
          type="button"
          class="ghost-btn"
          :class="{ 'favorite-switch--active': favoriteType === 'POST' }"
          @click="favoriteType = 'POST'"
        >
          Posts
        </button>
        <button
          type="button"
          class="ghost-btn"
          :class="{ 'favorite-switch--active': favoriteType === 'JOB' }"
          @click="favoriteType = 'JOB'"
        >
          Jobs
        </button>
      </div>

      <div v-if="loading" class="empty-state">姝ｅ湪鍔犺浇鎴戠殑鏀惰棌...</div>
      <div v-else-if="errorMessage" class="field-grid">
        <p class="field-error" role="alert">{{ errorMessage }}</p>
        <button type="button" class="ghost-btn" @click="loadFavorites">
          閲嶆柊鍔犺浇
        </button>
      </div>
      <div
        v-else-if="favoriteType === 'POST' && !summary.posts.length"
        class="empty-state"
      >
        浣犺繕娌℃湁鏀惰棌浠讳綍甯栧瓙銆?
      </div>
      <div
        v-else-if="favoriteType === 'JOB' && !summary.jobs.length"
        class="empty-state"
      >
        You have not saved any jobs yet.
      </div>
      <div v-else class="community-post-grid">
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

.community-post-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cp-gap-4);
}

@media (max-width: 1023px) {
  .community-post-grid {
    grid-template-columns: 1fr;
  }
}
</style>
