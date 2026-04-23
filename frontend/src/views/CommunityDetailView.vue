<script setup>
import { computed, reactive, ref, watch } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import CommunityCommentList from "../components/CommunityCommentList.vue";
import {
  createCommunityComment,
  createCommunityReply,
  favoriteCommunityPost,
  getCommunityPostDetail,
  likeCommunityPost,
  unfavoriteCommunityPost,
  unlikeCommunityPost,
} from "../api/community.js";
import { useUserStore } from "../stores/user.js";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const loading = ref(false);
const errorMessage = ref("");
const actionError = ref("");
const actionLoading = ref("");
const commentForm = reactive({
  content: "",
});
const openReplyForms = ref([]);
const replyDrafts = reactive({});
const detail = ref(null);

const tagLabels = {
  CAREER: "求职",
  EXAM: "升学",
  ABROAD: "留学",
  CHAT: "交流",
};

const localizedTag = computed(() => (
  tagLabels[detail.value?.tag] || detail.value?.tag || "未分类"
));

const experienceItems = computed(() => {
  if (!detail.value?.experience?.enabled) {
    return [];
  }

  return [
    { label: "目标方向", value: detail.value.experience.targetLabel },
    { label: "阶段结果", value: detail.value.experience.outcomeLabel },
    { label: "时间安排", value: detail.value.experience.timelineSummary },
    { label: "行动笔记", value: detail.value.experience.actionSummary },
  ].filter((item) => item.value);
});

async function loadDetail() {
  loading.value = true;
  errorMessage.value = "";

  try {
    detail.value = await getCommunityPostDetail(route.params.id);
  } catch (error) {
    errorMessage.value = error.message || "帖子详情加载失败，请稍后重试。";
  } finally {
    loading.value = false;
  }
}

function redirectToLogin() {
  router.push({
    name: "login",
    query: { redirect: route.fullPath },
  });
}

function ensureAuthenticated() {
  if (userStore.isAuthenticated) {
    return true;
  }

  redirectToLogin();
  return false;
}

async function handleToggleLike() {
  if (!ensureAuthenticated()) {
    return;
  }

  actionError.value = "";
  actionLoading.value = "like";

  try {
    detail.value = detail.value?.likedByMe
      ? await unlikeCommunityPost(detail.value.id)
      : await likeCommunityPost(detail.value.id);
  } catch (error) {
    actionError.value = error.message || "点赞操作失败，请稍后重试。";
  } finally {
    actionLoading.value = "";
  }
}

async function handleToggleFavorite() {
  if (!ensureAuthenticated()) {
    return;
  }

  actionError.value = "";
  actionLoading.value = "favorite";

  try {
    detail.value = detail.value?.favoritedByMe
      ? await unfavoriteCommunityPost(detail.value.id)
      : await favoriteCommunityPost(detail.value.id);
  } catch (error) {
    actionError.value = error.message || "收藏操作失败，请稍后重试。";
  } finally {
    actionLoading.value = "";
  }
}

async function handleSubmitComment() {
  if (!ensureAuthenticated()) {
    return;
  }

  actionError.value = "";

  if (!commentForm.content.trim()) {
    actionError.value = "请输入评论内容。";
    return;
  }

  actionLoading.value = "comment";

  try {
    detail.value = await createCommunityComment(detail.value.id, {
      content: commentForm.content.trim(),
    });
    commentForm.content = "";
  } catch (error) {
    actionError.value = error.message || "评论发布失败，请稍后重试。";
  } finally {
    actionLoading.value = "";
  }
}

function handleToggleReplyForm(commentId) {
  if (!ensureAuthenticated()) {
    return;
  }

  actionError.value = "";

  if (openReplyForms.value.includes(commentId)) {
    openReplyForms.value = openReplyForms.value.filter((id) => id !== commentId);
    return;
  }

  openReplyForms.value = [...openReplyForms.value, commentId];
  replyDrafts[commentId] = replyDrafts[commentId] || "";
}

function handleUpdateReplyDraft({ commentId, value }) {
  replyDrafts[commentId] = value;
}

async function handleSubmitReply(commentId) {
  if (!ensureAuthenticated()) {
    return;
  }

  actionError.value = "";
  const content = (replyDrafts[commentId] || "").trim();

  if (!content) {
    actionError.value = "请输入回复内容。";
    return;
  }

  actionLoading.value = `reply-${commentId}`;

  try {
    detail.value = await createCommunityReply(commentId, { content });
    replyDrafts[commentId] = "";
    openReplyForms.value = openReplyForms.value.filter((id) => id !== commentId);
  } catch (error) {
    actionError.value = error.message || "回复发布失败，请稍后重试。";
  } finally {
    actionLoading.value = "";
  }
}

watch(() => route.params.id, () => {
  loadDetail();
}, { immediate: true });
</script>

<template>
  <section class="page-stack">
    <article class="section-card">
      <div v-if="loading" class="empty-state">正在加载帖子详情...</div>
      <div v-else-if="errorMessage" class="field-grid">
        <p class="field-error" role="alert">{{ errorMessage }}</p>
        <button type="button" class="ghost-btn" @click="loadDetail">
          重试
        </button>
      </div>
      <div v-else-if="detail" class="community-detail">
        <div class="community-detail__main">
          <div class="chip-row">
            <span class="section-eyebrow">{{ localizedTag }}</span>
            <span class="status-badge approved">{{ detail.author?.nickname || "匿名同学" }}</span>
          </div>

          <h1 class="hero-title" style="margin-top: 18px;">{{ detail.title }}</h1>

          <article v-if="experienceItems.length" class="panel-card experience-summary">
            <span class="section-eyebrow">经验摘要</span>
            <div class="experience-summary__grid">
              <div
                v-for="item in experienceItems"
                :key="item.label"
                class="experience-summary__item"
              >
                <strong>{{ item.label }}</strong>
                <p>{{ item.value }}</p>
              </div>
            </div>
          </article>

          <hr class="editorial-rule" />
          <p class="community-detail__body">{{ detail.content }}</p>

          <div class="community-detail__meta">
            <span>发布于 {{ new Date(detail.createdAt).toLocaleString("zh-CN") }}</span>
            <span>赞 {{ detail.likeCount }}</span>
            <span>评论 {{ detail.commentCount }}</span>
            <span>收藏 {{ detail.favoriteCount }}</span>
          </div>
        </div>

        <aside class="community-detail__aside">
          <article class="panel-card">
            <span class="section-eyebrow">常用操作</span>
            <div class="field-grid" style="margin-top: 16px;">
              <button
                type="button"
                class="app-link"
                :disabled="actionLoading === 'like'"
                @click="handleToggleLike"
              >
                {{ detail.likedByMe ? "取消点赞" : "点赞" }}
              </button>
              <button
                type="button"
                class="ghost-btn"
                :disabled="actionLoading === 'favorite'"
                @click="handleToggleFavorite"
              >
                {{ detail.favoritedByMe ? "取消收藏" : "收藏帖子" }}
              </button>
              <RouterLink to="/community" class="ghost-btn">
                返回社区
              </RouterLink>
            </div>
          </article>

          <article class="panel-card">
            <strong>参与说明</strong>
            <p class="meta-copy" style="margin-top: 12px;">
              游客可以浏览讨论内容；评论、回复、点赞和收藏需要登录后才能使用。
            </p>
          </article>
        </aside>
      </div>
    </article>

    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">评论区</span>
          <h2 class="page-title" style="margin-top: 16px;">评论区</h2>
        </div>
      </div>

      <form class="field-grid community-detail__comment-form" @submit.prevent="handleSubmitComment">
        <label class="field-label">
          发表评论
          <textarea
            v-model.trim="commentForm.content"
            class="field-textarea"
            name="comment"
            placeholder="分享你的经验、提醒或补充观点。"
            :disabled="actionLoading === 'comment'"
          />
        </label>

        <p v-if="actionError" class="field-error" role="alert">{{ actionError }}</p>

        <div class="inline-form-actions">
          <button
            type="submit"
            class="app-btn"
            :disabled="actionLoading === 'comment'"
          >
            {{ actionLoading === "comment" ? "发布中..." : "发布评论" }}
          </button>
          <button
            v-if="!userStore.isAuthenticated"
            type="button"
            class="ghost-btn"
            @click="redirectToLogin"
          >
            登录后参与
          </button>
        </div>
      </form>

      <div style="margin-top: 24px;">
        <CommunityCommentList
          :comments="detail?.comments || []"
          :open-reply-forms="openReplyForms"
          :reply-drafts="replyDrafts"
          :action-loading="actionLoading"
          @toggle-reply="handleToggleReplyForm"
          @update-reply="handleUpdateReplyDraft"
          @submit-reply="handleSubmitReply"
        />
      </div>
    </article>
  </section>
</template>

<style scoped>
.community-detail {
  display: grid;
  grid-template-columns: 1.3fr 0.7fr;
  gap: var(--cp-gap-6);
}

.community-detail__main,
.community-detail__aside {
  display: grid;
  gap: var(--cp-gap-4);
}

.community-detail__body {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.8;
  color: var(--cp-ink);
}

.community-detail__meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cp-gap-3);
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.experience-summary {
  display: grid;
  gap: 16px;
}

.experience-summary__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.experience-summary__item {
  display: grid;
  gap: 8px;
  padding: 16px;
  border-radius: var(--cp-radius-md);
  background: rgba(255, 249, 241, 0.78);
  border: 1px solid rgba(197, 79, 45, 0.14);
}

.experience-summary__item p {
  margin: 0;
  color: var(--cp-ink-soft);
  line-height: 1.6;
}

@media (max-width: 1023px) {
  .community-detail,
  .experience-summary__grid {
    grid-template-columns: 1fr;
  }
}
</style>
