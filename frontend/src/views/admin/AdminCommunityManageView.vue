<script setup>
import { computed, onMounted, ref } from "vue";
import { deleteCommunityPost, getAdminCommunityPosts, hideCommunityPost } from "../../api/admin.js";

const loading = ref(true);
const errorMessage = ref("");
const actionMessage = ref("");
const actionLoadingId = ref("");
const summary = ref({
  total: 0,
  posts: [],
});

const statusCards = computed(() => {
  const posts = summary.value.posts || [];

  return [
    { label: "全部帖子", value: posts.length },
    { label: "已发布", value: posts.filter((item) => item.status === "PUBLISHED").length },
    { label: "已下架", value: posts.filter((item) => item.status === "HIDDEN").length },
    { label: "已删除", value: posts.filter((item) => item.status === "DELETED").length },
  ];
});

async function loadPosts() {
  loading.value = true;
  errorMessage.value = "";

  try {
    summary.value = await getAdminCommunityPosts();
  } catch (error) {
    errorMessage.value = error.message || "社区治理列表加载失败，请稍后重试。";
  } finally {
    loading.value = false;
  }
}

async function handleHide(id) {
  actionMessage.value = "";
  actionLoadingId.value = `hide-${id}`;

  try {
    await hideCommunityPost(id);
    actionMessage.value = "帖子已下架。";
    await loadPosts();
  } catch (error) {
    errorMessage.value = error.message || "下架失败，请稍后重试。";
  } finally {
    actionLoadingId.value = "";
  }
}

async function handleDelete(id) {
  actionMessage.value = "";
  actionLoadingId.value = `delete-${id}`;

  try {
    await deleteCommunityPost(id);
    actionMessage.value = "帖子已删除。";
    await loadPosts();
  } catch (error) {
    errorMessage.value = error.message || "删除失败，请稍后重试。";
  } finally {
    actionLoadingId.value = "";
  }
}

onMounted(loadPosts);
</script>

<template>
  <section class="page-stack">
    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">Admin Community Desk</span>
          <h1 class="page-title" style="margin-top: 16px;">社区治理台</h1>
          <p class="page-subtitle" style="margin-top: 16px;">
            首期只保留下架和删除两种最小治理动作，不引入举报流和复杂审核台。
          </p>
        </div>
      </div>

      <div class="stats-grid admin-community-stats">
        <article
          v-for="card in statusCards"
          :key="card.label"
          class="panel-card admin-community-stat"
        >
          <span class="admin-community-stat__label">{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
        </article>
      </div>
    </article>

    <article class="section-card">
      <div v-if="loading" class="empty-state">正在加载社区治理列表...</div>
      <div v-else-if="errorMessage" class="field-grid">
        <p class="field-error" role="alert">{{ errorMessage }}</p>
        <button type="button" class="ghost-btn" @click="loadPosts">
          重新加载
        </button>
      </div>
      <div v-else>
        <p v-if="actionMessage" class="field-hint" style="margin-top: 0;">{{ actionMessage }}</p>
        <table class="app-table">
          <thead>
            <tr>
              <th>标题</th>
              <th>标签</th>
              <th>作者</th>
              <th>状态</th>
              <th>数据</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="post in summary.posts" :key="post.id">
              <td>{{ post.title }}</td>
              <td>{{ post.tag }}</td>
              <td>{{ post.authorNickname }}</td>
              <td>{{ post.status }}</td>
              <td>赞 {{ post.likeCount }} / 评 {{ post.commentCount }} / 藏 {{ post.favoriteCount }}</td>
              <td>
                <div class="inline-form-actions">
                  <button
                    type="button"
                    class="ghost-btn"
                    :disabled="post.status !== 'PUBLISHED' || actionLoadingId === `hide-${post.id}`"
                    @click="handleHide(post.id)"
                  >
                    下架
                  </button>
                  <button
                    type="button"
                    class="danger-btn"
                    :disabled="post.status === 'DELETED' || actionLoadingId === `delete-${post.id}`"
                    @click="handleDelete(post.id)"
                  >
                    删除
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <div class="mobile-table-cards">
          <article
            v-for="post in summary.posts"
            :key="`mobile-${post.id}`"
            class="table-card"
          >
            <strong>{{ post.title }}</strong>
            <p class="meta-copy">标签 {{ post.tag }} · 作者 {{ post.authorNickname }}</p>
            <p class="meta-copy">状态 {{ post.status }}</p>
            <p class="meta-copy">赞 {{ post.likeCount }} / 评 {{ post.commentCount }} / 藏 {{ post.favoriteCount }}</p>
            <div class="inline-form-actions" style="margin-top: 12px;">
              <button
                type="button"
                class="ghost-btn"
                :disabled="post.status !== 'PUBLISHED' || actionLoadingId === `hide-${post.id}`"
                @click="handleHide(post.id)"
              >
                下架
              </button>
              <button
                type="button"
                class="danger-btn"
                :disabled="post.status === 'DELETED' || actionLoadingId === `delete-${post.id}`"
                @click="handleDelete(post.id)"
              >
                删除
              </button>
            </div>
          </article>
        </div>
      </div>
    </article>
  </section>
</template>

<style scoped>
.admin-community-stats {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.admin-community-stat {
  min-height: 128px;
  display: grid;
  gap: 8px;
  align-content: end;
}

.admin-community-stat__label {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.admin-community-stat strong {
  font-family: var(--cp-font-display);
  font-size: clamp(24px, 4vw, 32px);
}

@media (max-width: 1023px) {
  .admin-community-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .admin-community-stats {
    grid-template-columns: 1fr;
  }
}
</style>
