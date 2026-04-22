<script setup>
import { computed, ref, watch } from "vue";
import { RouterLink } from "vue-router";
import { getCommunityHotPosts, getCommunityPosts } from "../api/community.js";
import CommunityFilterTabs from "../components/CommunityFilterTabs.vue";
import CommunityPostCard from "../components/CommunityPostCard.vue";
import { useUserStore } from "../stores/user.js";

const userStore = useUserStore();

const TAG_OPTIONS = [
  { value: "", label: "全部内容", eyebrow: "社区总览" },
  { value: "CAREER", label: "就业", eyebrow: "求职方向" },
  { value: "EXAM", label: "考研", eyebrow: "升学规划" },
  { value: "ABROAD", label: "留学", eyebrow: "海外申请" },
  { value: "CHAT", label: "闲聊", eyebrow: "开放交流" },
];

const HOT_PERIOD_OPTIONS = [
  { value: "DAY", label: "今日" },
  { value: "WEEK", label: "本周" },
  { value: "ALL", label: "全部时间" },
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
  TAG_OPTIONS.find((item) => item.value === selectedTag.value)?.label || "全部内容"
));

const currentHotPeriodLabel = computed(() => (
  HOT_PERIOD_OPTIONS.find((item) => item.value === hotPeriod.value)?.label || "本周"
));

function withLocalizedError(prefix, detail) {
  return detail ? `${prefix}：${detail}` : prefix;
}

async function loadPosts() {
  loading.value = true;
  errorMessage.value = "";

  try {
    summary.value = await getCommunityPosts({
      ...(selectedTag.value ? { tag: selectedTag.value } : {}),
    });
  } catch (error) {
    errorMessage.value = withLocalizedError("社区帖子加载失败，请稍后再试", error.message);
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
    hotErrorMessage.value = withLocalizedError("社区热榜加载失败，请稍后再试", error.message);
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
    return "刚刚";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "刚刚";
  }

  return new Intl.DateTimeFormat("zh-CN", {
    month: "numeric",
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
        <span class="section-eyebrow">学生成长社区</span>
        <h1 class="hero-title" style="margin-top: 18px;">把经验、申请节奏和真实讨论放到一个社区里，先看他人经历，再决定下一步。</h1>
        <hr class="editorial-rule" />
        <p class="hero-copy">
          先浏览公开讨论，再进入发帖、评论和收藏，把真正对你有帮助的内容沉淀下来。
        </p>
      </div>

      <div class="community-hero__panel">
        <div class="panel-card community-hero__stats">
          <span class="community-hero__label">当前筛选</span>
          <strong>{{ currentTagLabel }}</strong>
        </div>
        <div class="panel-card community-hero__stats">
          <span class="community-hero__label">可见帖子</span>
          <strong>{{ summary.total }}</strong>
        </div>
        <RouterLink
          :to="userStore.isAuthenticated ? '/community/create' : '/login'"
          class="app-btn"
        >
          {{ userStore.isAuthenticated ? "发布帖子" : "登录后参与交流" }}
        </RouterLink>
      </div>
    </article>

    <article class="section-card hot-board">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">社区热榜</span>
          <h2 class="page-title" style="margin-top: 16px;">看看{{ currentHotPeriodLabel }}最受关注的话题。</h2>
          <p class="page-subtitle" style="margin-top: 16px;">
            热榜会综合点赞、评论、收藏和认证作者加权，帮你先快速看清社区里正在升温的讨论。
          </p>
        </div>
        <span class="status-badge approved">前 {{ hotBoard.items.length || 0 }} / {{ hotBoard.total }}</span>
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

      <div v-if="hotLoading" class="empty-state">正在加载社区热榜...</div>
      <div v-else-if="hotErrorMessage" class="field-grid">
        <p class="field-error" role="alert">{{ hotErrorMessage }}</p>
        <button type="button" class="ghost-btn retry-hot-board" @click="retryHotBoard">
          重新加载
        </button>
      </div>
      <div v-else-if="!hotBoard.items.length" class="empty-state hot-board__empty">
        <strong>当前时间范围内还没有帖子进入热榜。</strong>
        <p class="meta-copy">可以切换时间范围，或发布第一篇值得大家关注的帖子。</p>
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
              {{ item.contentPreview || "暂未提供内容摘要" }}
            </p>
          </div>

          <div class="hot-board-card__meta">
            <span>{{ item.authorNickname || "匿名用户" }}</span>
            <span>{{ item.tag || "未分类" }}</span>
            <span>{{ formatDate(item.createdAt) }}</span>
          </div>

          <div class="hot-board-card__footer">
            <span>赞 {{ item.likeCount || 0 }}</span>
            <span>评 {{ item.commentCount || 0 }}</span>
            <span>藏 {{ item.favoriteCount || 0 }}</span>
            <strong>热度 {{ Math.round(Number(item.hotScore || 0)) }}</strong>
          </div>
        </RouterLink>
      </div>
    </article>

    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">标签筛选</span>
          <h2 class="page-title" style="margin-top: 16px;">按方向浏览社区讨论</h2>
        </div>
      </div>

      <CommunityFilterTabs v-model="selectedTag" :options="TAG_OPTIONS" />
    </article>

    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">最新帖子</span>
          <h2 class="page-title" style="margin-top: 16px;">按发布时间查看社区更新</h2>
          <p class="page-subtitle" style="margin-top: 16px;">
            热榜帮你先看热度，下面的列表仍按最新发布时间展示，方便你继续按标签筛选。
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
        当前标签下还没有帖子，欢迎发布第一篇内容。
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
