<script setup>
import { onMounted, ref } from "vue";
import CommunityPostCard from "../components/CommunityPostCard.vue";
import { getMyCommunityPosts } from "../api/community.js";

const loading = ref(true);
const errorMessage = ref("");
const summary = ref({
  total: 0,
  posts: [],
});

async function loadPosts() {
  loading.value = true;
  errorMessage.value = "";

  try {
    summary.value = await getMyCommunityPosts();
  } catch (error) {
    errorMessage.value = error.message || "我的发布加载失败，请稍后重试。";
  } finally {
    loading.value = false;
  }
}

onMounted(loadPosts);
</script>

<template>
  <section class="page-stack">
    <article class="section-card">
      <span class="section-eyebrow">我的帖子</span>
      <h1 class="page-title" style="margin-top: 16px;">我的发布</h1>
      <p class="page-subtitle" style="margin-top: 16px;">
        这里汇总你已经发布的帖子，后续如果被管理员下架，也会在这里保留状态。
      </p>
    </article>

    <article class="section-card">
      <div v-if="loading" class="empty-state">正在加载我的发布...</div>
      <div v-else-if="errorMessage" class="field-grid">
        <p class="field-error" role="alert">{{ errorMessage }}</p>
        <button type="button" class="ghost-btn" @click="loadPosts">
          重新加载
        </button>
      </div>
      <div v-else-if="!summary.posts.length" class="empty-state">
        你还没有发布任何帖子。
      </div>
      <div v-else class="community-post-grid">
        <CommunityPostCard
          v-for="post in summary.posts"
          :key="post.id"
          :post="post"
          compact
        />
      </div>
    </article>
  </section>
</template>

<style scoped>
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
