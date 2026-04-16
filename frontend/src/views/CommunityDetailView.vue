<script setup>
import { computed, reactive, ref, watch } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import CommunityCommentList from "../components/CommunityCommentList.vue";
import {
  createCommunityComment,
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
const detail = ref(null);

const tagLabels = {
  CAREER: "就业",
  EXAM: "考研",
  ABROAD: "留学",
  CHAT: "闲聊",
};

const localizedTag = computed(() => (
  tagLabels[detail.value?.tag] || detail.value?.tag || "未分类"
));

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
    actionError.value = error.message || "评论提交失败，请稍后重试。";
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
          重新加载
        </button>
      </div>
      <div v-else-if="detail" class="community-detail">
        <div class="community-detail__main">
          <div class="chip-row">
            <span class="section-eyebrow">{{ localizedTag }}</span>
            <span class="status-badge approved">{{ detail.author?.nickname || "匿名用户" }}</span>
          </div>

          <h1 class="hero-title" style="margin-top: 18px;">{{ detail.title }}</h1>
          <hr class="editorial-rule" />
          <p class="community-detail__body">{{ detail.content }}</p>

          <div class="community-detail__meta">
            <span>发布时间 {{ new Date(detail.createdAt).toLocaleString("zh-CN") }}</span>
            <span>赞 {{ detail.likeCount }}</span>
            <span>评 {{ detail.commentCount }}</span>
            <span>藏 {{ detail.favoriteCount }}</span>
          </div>
        </div>

        <aside class="community-detail__aside">
          <article class="panel-card">
            <span class="section-eyebrow">Actions</span>
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
                {{ detail.favoritedByMe ? "取消收藏" : "加入收藏" }}
              </button>
              <RouterLink to="/community" class="ghost-btn">
                返回社区列表
              </RouterLink>
            </div>
          </article>

          <article class="panel-card">
            <strong>互动提示</strong>
            <p class="meta-copy" style="margin-top: 12px;">
              游客可浏览详情；评论、点赞和收藏会在登录后开放。
            </p>
          </article>
        </aside>
      </div>
    </article>

    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">Comments</span>
          <h2 class="page-title" style="margin-top: 16px;">评论区</h2>
        </div>
      </div>

      <form class="field-grid" @submit.prevent="handleSubmitComment">
        <label class="field-label">
          添加评论
          <textarea
            v-model.trim="commentForm.content"
            class="field-textarea"
            name="comment"
            placeholder="补充你的经验、提醒或反对意见。"
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
            {{ actionLoading === "comment" ? "提交中..." : "发表评论" }}
          </button>
          <button
            v-if="!userStore.isAuthenticated"
            type="button"
            class="ghost-btn"
            @click="redirectToLogin"
          >
            登录后参与讨论
          </button>
        </div>
      </form>

      <div style="margin-top: 24px;">
        <CommunityCommentList :comments="detail?.comments || []" />
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

@media (max-width: 1023px) {
  .community-detail {
    grid-template-columns: 1fr;
  }
}
</style>
