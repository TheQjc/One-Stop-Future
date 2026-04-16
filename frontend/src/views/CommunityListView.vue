<script setup>
import { computed, ref, watch } from "vue";
import { RouterLink } from "vue-router";
import { getCommunityPosts } from "../api/community.js";
import CommunityFilterTabs from "../components/CommunityFilterTabs.vue";
import CommunityPostCard from "../components/CommunityPostCard.vue";
import { useUserStore } from "../stores/user.js";

const userStore = useUserStore();

const loading = ref(false);
const errorMessage = ref("");
const selectedTag = ref("");
const summary = ref({
  total: 0,
  posts: [],
});

const tagOptions = [
  { value: "", label: "全部", eyebrow: "All" },
  { value: "CAREER", label: "就业", eyebrow: "Career" },
  { value: "EXAM", label: "考研", eyebrow: "Exam" },
  { value: "ABROAD", label: "留学", eyebrow: "Abroad" },
  { value: "CHAT", label: "闲聊", eyebrow: "Chat" },
];

const currentTagLabel = computed(() => (
  tagOptions.find((item) => item.value === selectedTag.value)?.label || "全部"
));

async function loadPosts() {
  loading.value = true;
  errorMessage.value = "";

  try {
    summary.value = await getCommunityPosts({
      ...(selectedTag.value ? { tag: selectedTag.value } : {}),
    });
  } catch (error) {
    errorMessage.value = error.message || "社区列表加载失败，请稍后重试。";
  } finally {
    loading.value = false;
  }
}

watch(selectedTag, () => {
  loadPosts();
}, { immediate: true });
</script>

<template>
  <section class="page-stack">
    <article class="section-card community-hero">
      <div class="community-hero__copy">
        <span class="section-eyebrow">Campus Editorial Forum</span>
        <h1 class="hero-title" style="margin-top: 18px;">把就业、考研、留学讨论收进同一张校内讨论栏。</h1>
        <hr class="editorial-rule" />
        <p class="hero-copy">
          社区首期先开放经验帖、方向讨论和轻量互动。游客可以先看，登录后再发帖、评论、点赞和收藏。
        </p>
      </div>

      <div class="community-hero__panel">
        <div class="panel-card community-hero__stats">
          <span class="community-hero__label">当前筛选</span>
          <strong>{{ currentTagLabel }}</strong>
        </div>
        <div class="panel-card community-hero__stats">
          <span class="community-hero__label">帖子总数</span>
          <strong>{{ summary.total }}</strong>
        </div>
        <RouterLink
          :to="userStore.isAuthenticated ? '/community/create' : '/login'"
          class="app-btn"
        >
          {{ userStore.isAuthenticated ? "发布新帖" : "登录后参与讨论" }}
        </RouterLink>
      </div>
    </article>

    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">Tag Filter</span>
          <h2 class="page-title" style="margin-top: 16px;">按方向浏览讨论</h2>
        </div>
      </div>

      <CommunityFilterTabs v-model="selectedTag" :options="tagOptions" />
    </article>

    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">Latest Posts</span>
          <h2 class="page-title" style="margin-top: 16px;">社区帖子</h2>
          <p class="page-subtitle" style="margin-top: 16px;">
            先做最新发布视图，后续再叠加搜索和更复杂的排序能力。
          </p>
        </div>
      </div>

      <div v-if="loading" class="empty-state">正在加载社区帖子...</div>
      <div v-else-if="errorMessage" class="field-grid">
        <p class="field-error" role="alert">{{ errorMessage }}</p>
        <button type="button" class="ghost-btn" @click="loadPosts">
          重新加载
        </button>
      </div>
      <div v-else-if="!summary.posts.length" class="empty-state">
        当前筛选下还没有帖子，先去发布第一篇内容。
      </div>
      <div v-else class="community-post-grid">
        <CommunityPostCard
          v-for="post in summary.posts"
          :key="post.id"
          :post="post"
        />
      </div>
    </article>
  </section>
</template>

<style scoped>
.community-hero {
  display: grid;
  grid-template-columns: 1.35fr 0.75fr;
  gap: var(--cp-gap-6);
  background:
    linear-gradient(180deg, rgba(255, 251, 245, 0.9), rgba(252, 245, 236, 0.96)),
    radial-gradient(circle at top left, rgba(197, 79, 45, 0.12), transparent 28%),
    radial-gradient(circle at bottom right, rgba(76, 122, 116, 0.14), transparent 30%);
}

.community-hero__copy,
.community-hero__panel {
  display: grid;
  gap: var(--cp-gap-4);
}

.community-hero__panel {
  align-content: end;
}

.community-hero__stats {
  min-height: 132px;
  display: grid;
  gap: 8px;
  align-content: end;
}

.community-hero__label {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.community-hero__stats strong {
  font-size: clamp(26px, 4vw, 34px);
  font-family: var(--cp-font-display);
}

.community-post-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cp-gap-4);
}

@media (max-width: 1023px) {
  .community-hero,
  .community-post-grid {
    grid-template-columns: 1fr;
  }
}
</style>
