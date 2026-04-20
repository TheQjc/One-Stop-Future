<script setup>
import { computed, ref, watch } from "vue";
import { RouterLink } from "vue-router";
import { getCommunityHotPosts, getCommunityPosts } from "../api/community.js";
import CommunityFilterTabs from "../components/CommunityFilterTabs.vue";
import CommunityPostCard from "../components/CommunityPostCard.vue";
import { useUserStore } from "../stores/user.js";

const userStore = useUserStore();

const TAG_OPTIONS = [
  { value: "", label: "All", eyebrow: "Overview" },
  { value: "CAREER", label: "Career", eyebrow: "Jobs" },
  { value: "EXAM", label: "Exam", eyebrow: "Study" },
  { value: "ABROAD", label: "Abroad", eyebrow: "Global" },
  { value: "CHAT", label: "Chat", eyebrow: "Open" },
];

const HOT_PERIOD_OPTIONS = [
  { value: "DAY", label: "Today" },
  { value: "WEEK", label: "This Week" },
  { value: "ALL", label: "All Time" },
];

const loading = ref(false);
const errorMessage = ref("");
const selectedTag = ref("");
const summary = ref({
  total: 0,
  posts: [],
});

const hotLoading = ref(false);
const hotErrorMessage = ref("");
const hotPeriod = ref("WEEK");
const hotBoard = ref({
  period: "WEEK",
  total: 0,
  items: [],
});

const currentTagLabel = computed(() => (
  TAG_OPTIONS.find((item) => item.value === selectedTag.value)?.label || "All"
));

const currentHotPeriodLabel = computed(() => (
  HOT_PERIOD_OPTIONS.find((item) => item.value === hotPeriod.value)?.label || "This Week"
));

async function loadPosts() {
  loading.value = true;
  errorMessage.value = "";

  try {
    summary.value = await getCommunityPosts({
      ...(selectedTag.value ? { tag: selectedTag.value } : {}),
    });
  } catch (error) {
    errorMessage.value = error.message || "Community list loading failed. Please try again.";
  } finally {
    loading.value = false;
  }
}

async function loadHotBoard() {
  hotLoading.value = true;
  hotErrorMessage.value = "";

  try {
    hotBoard.value = await getCommunityHotPosts({
      period: hotPeriod.value,
      limit: 3,
    });
  } catch (error) {
    hotErrorMessage.value = error.message || "Community hot board loading failed. Please try again.";
    hotBoard.value = {
      period: hotPeriod.value,
      total: 0,
      items: [],
    };
  } finally {
    hotLoading.value = false;
  }
}

function retryHotBoard() {
  loadHotBoard();
}

function formatDate(value) {
  if (!value) {
    return "Just now";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "Just now";
  }

  return new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

watch(selectedTag, () => {
  loadPosts();
}, { immediate: true });

watch(hotPeriod, () => {
  loadHotBoard();
}, { immediate: true });
</script>

<template>
  <section class="page-stack">
    <article class="section-card community-hero">
      <div class="community-hero__copy">
        <span class="section-eyebrow">Campus Editorial Forum</span>
        <h1 class="hero-title" style="margin-top: 18px;">One board for direction notes, application rhythm, and lived student advice.</h1>
        <hr class="editorial-rule" />
        <p class="hero-copy">
          Browse public discussion first, then move into posting, commenting, and collecting the threads
          that actually help you decide what to do next.
        </p>
      </div>

      <div class="community-hero__panel">
        <div class="panel-card community-hero__stats">
          <span class="community-hero__label">Current tag</span>
          <strong>{{ currentTagLabel }}</strong>
        </div>
        <div class="panel-card community-hero__stats">
          <span class="community-hero__label">Visible posts</span>
          <strong>{{ summary.total }}</strong>
        </div>
        <RouterLink
          :to="userStore.isAuthenticated ? '/community/create' : '/login'"
          class="app-btn"
        >
          {{ userStore.isAuthenticated ? "Write a post" : "Log in to join" }}
        </RouterLink>
      </div>
    </article>

    <article class="section-card hot-board">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">Community Hot Board</span>
          <h2 class="page-title" style="margin-top: 16px;">What is getting traction {{ currentHotPeriodLabel.toLowerCase() }}.</h2>
          <p class="page-subtitle" style="margin-top: 16px;">
            The hot board uses likes, comments, favorites, and a small verified-author trust bonus. It
            stays inside the community desk instead of sending you to discover.
          </p>
        </div>
        <span class="status-badge approved">Top {{ hotBoard.items.length || 0 }} / {{ hotBoard.total }}</span>
      </div>

      <div class="chip-row hot-board__chips">
        <button
          v-for="option in HOT_PERIOD_OPTIONS"
          :key="option.value"
          type="button"
          class="hot-board__chip"
          :class="{
            'hot-board__chip--active': hotPeriod === option.value,
            'period-day': option.value === 'DAY',
            'period-week': option.value === 'WEEK',
            'period-all': option.value === 'ALL',
          }"
          @click="hotPeriod = option.value"
        >
          {{ option.label }}
        </button>
      </div>

      <div v-if="hotLoading" class="empty-state">Loading community hot board...</div>
      <div v-else-if="hotErrorMessage" class="field-grid">
        <p class="field-error" role="alert">{{ hotErrorMessage }}</p>
        <button type="button" class="ghost-btn retry-hot-board" @click="retryHotBoard">
          Retry
        </button>
      </div>
      <div v-else-if="!hotBoard.items.length" class="empty-state hot-board__empty">
        <strong>No public posts have entered this board yet.</strong>
        <p class="meta-copy">Try another period or publish the first thread for this desk.</p>
      </div>
      <div v-else class="hot-board-grid">
        <RouterLink
          v-for="(item, index) in hotBoard.items"
          :key="item.id"
          class="hot-board-card"
          :to="`/community/${item.id}`"
        >
          <div class="hot-board-card__topline">
            <span class="hot-board-card__rank">#{{ index + 1 }}</span>
            <span class="hot-board-card__label">{{ item.hotLabel }}</span>
          </div>

          <div class="hot-board-card__body">
            <h3 class="hot-board-card__title">{{ item.title }}</h3>
            <p class="hot-board-card__summary">
              {{ item.contentPreview || "No summary attached to this post yet." }}
            </p>
          </div>

          <div class="hot-board-card__meta">
            <span>{{ item.authorNickname || "Unknown User" }}</span>
            <span>{{ item.tag || "General" }}</span>
            <span>{{ formatDate(item.createdAt) }}</span>
          </div>

          <div class="hot-board-card__footer">
            <span>Likes {{ item.likeCount || 0 }}</span>
            <span>Comments {{ item.commentCount || 0 }}</span>
            <span>Favorites {{ item.favoriteCount || 0 }}</span>
            <strong>Heat {{ Math.round(Number(item.hotScore || 0)) }}</strong>
          </div>
        </RouterLink>
      </div>
    </article>

    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">Tag Filter</span>
          <h2 class="page-title" style="margin-top: 16px;">Browse discussion by direction.</h2>
        </div>
      </div>

      <CommunityFilterTabs v-model="selectedTag" :options="TAG_OPTIONS" />
    </article>

    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">Latest Posts</span>
          <h2 class="page-title" style="margin-top: 16px;">Newest community posts</h2>
          <p class="page-subtitle" style="margin-top: 16px;">
            Hot ranking handles momentum. The feed below stays focused on recent publishing order and tag browsing.
          </p>
        </div>
      </div>

      <div v-if="loading" class="empty-state">Loading community posts...</div>
      <div v-else-if="errorMessage" class="field-grid">
        <p class="field-error" role="alert">{{ errorMessage }}</p>
        <button type="button" class="ghost-btn" @click="loadPosts">
          Retry
        </button>
      </div>
      <div v-else-if="!summary.posts.length" class="empty-state">
        No posts match this tag yet. Publish the first one for this lane.
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

.hot-board {
  background:
    linear-gradient(180deg, rgba(255, 253, 248, 0.96), rgba(249, 242, 233, 0.98)),
    radial-gradient(circle at top right, rgba(184, 130, 35, 0.12), transparent 26%),
    radial-gradient(circle at bottom left, rgba(24, 38, 63, 0.08), transparent 34%);
}

.hot-board__chips {
  margin-bottom: 24px;
}

.hot-board__chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: var(--cp-touch-height);
  padding: 0 16px;
  border: 1px solid var(--cp-line-strong);
  border-radius: var(--cp-radius-pill);
  background: rgba(255, 255, 255, 0.78);
  color: var(--cp-ink-soft);
  cursor: pointer;
  transition:
    transform var(--cp-transition),
    border-color var(--cp-transition),
    background-color var(--cp-transition),
    color var(--cp-transition);
}

.hot-board__chip:hover {
  transform: translateY(-1px);
  border-color: rgba(197, 79, 45, 0.3);
}

.hot-board__chip--active {
  background: var(--cp-ink);
  color: #fff9f1;
  border-color: var(--cp-ink);
}

.hot-board__empty {
  display: grid;
  gap: 8px;
}

.hot-board-grid,
.community-post-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cp-gap-4);
}

.community-post-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.hot-board-card {
  min-height: 100%;
  padding: 22px;
  display: grid;
  gap: 14px;
  border-radius: var(--cp-radius-md);
  border: 1px solid rgba(24, 38, 63, 0.12);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.84), rgba(255, 249, 241, 0.98)),
    radial-gradient(circle at top right, rgba(197, 79, 45, 0.12), transparent 40%);
  box-shadow: var(--cp-shadow-soft);
  transition:
    transform var(--cp-transition),
    border-color var(--cp-transition),
    box-shadow var(--cp-transition);
}

.hot-board-card:hover {
  transform: translateY(-2px);
  border-color: rgba(197, 79, 45, 0.24);
  box-shadow: var(--cp-shadow-card);
}

.hot-board-card__topline,
.hot-board-card__meta,
.hot-board-card__footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.hot-board-card__rank,
.hot-board-card__label {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border-radius: var(--cp-radius-pill);
  font-size: var(--cp-text-sm);
}

.hot-board-card__rank {
  background: rgba(24, 38, 63, 0.92);
  color: #fff9f1;
  font-weight: 700;
}

.hot-board-card__label {
  background: rgba(255, 255, 255, 0.82);
  color: var(--cp-ink-soft);
  border: 1px solid rgba(24, 38, 63, 0.08);
}

.hot-board-card__body {
  display: grid;
  gap: 10px;
}

.hot-board-card__title {
  margin: 0;
  font-family: var(--cp-font-display);
  font-size: clamp(24px, 3vw, 30px);
  line-height: 1.14;
}

.hot-board-card__summary,
.hot-board-card__meta,
.hot-board-card__footer {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.hot-board-card__summary {
  margin: 0;
  line-height: 1.7;
}

.hot-board-card__footer strong {
  color: var(--cp-ink);
}

@media (max-width: 1023px) {
  .community-hero,
  .hot-board-grid,
  .community-post-grid {
    grid-template-columns: 1fr;
  }
}
</style>
