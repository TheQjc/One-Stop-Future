<script setup>
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import NoticeCard from "../components/NoticeCard.vue";
import { getNoticeList } from "../api/notice.js";
import { useUserStore } from "../stores/user.js";

const userStore = useUserStore();
const loading = ref(true);
const latestNotices = ref([]);

const welcomeText = computed(() => {
  if (!userStore.isAuthenticated) {
    return "访客入口";
  }

  return `${userStore.roleLabel} / ${userStore.profile?.realName || userStore.profile?.username}`;
});

onMounted(async () => {
  const data = await getNoticeList({ page: 1, pageSize: 3, includeAll: false });
  latestNotices.value = data.list;
  loading.value = false;
});
</script>

<template>
  <section class="page-stack">
    <div class="hero-grid">
      <article class="section-card">
        <span class="section-eyebrow">Independent Home</span>
        <h1 class="hero-title">校园一站式信息平台</h1>
        <hr class="editorial-rule" />
        <p class="hero-copy">
          以可信、清晰、信息密度高的校园公报风格组织首页。通知优先展示，课表与活动保持可见入口，减少分散查找成本。
        </p>
        <div class="chip-row" style="margin-top: 20px;">
          <span class="status-badge approved">{{ welcomeText }}</span>
          <span class="status-badge pending">当前阶段：P0 首页聚合 / 用户中心 / 公告</span>
        </div>
        <div class="quick-link-grid" style="margin-top: 28px;">
          <RouterLink to="/notices" class="panel-card">
            <strong>通知公告</strong>
            <p class="meta-copy">查看最新通知、分类筛选与详情页。</p>
          </RouterLink>
          <div class="panel-card">
            <strong>课表查询</strong>
            <p class="meta-copy">Phase 2 占位入口，后续支持学生课表与教师授课视图。</p>
          </div>
          <div class="panel-card">
            <strong>校园活动</strong>
            <p class="meta-copy">Phase 2 占位入口，后续支持活动浏览、报名与审核。</p>
          </div>
        </div>
      </article>

      <article class="section-card">
        <div class="section-header">
          <div>
            <span class="section-eyebrow">Quick Brief</span>
            <h2 class="page-title" style="font-size: 30px; margin-top: 16px;">今日看板</h2>
          </div>
        </div>
        <div class="notice-list">
          <div class="panel-card">
            <strong>通知流</strong>
            <p class="meta-copy">学生看到已通过公告；教师和管理员可进入审核台继续处理待审内容。</p>
          </div>
          <div class="panel-card">
            <strong>角色导航</strong>
            <p class="meta-copy">学生仅显示浏览与个人中心，教师和管理员额外显示公告管理入口。</p>
          </div>
          <RouterLink
            v-if="userStore.canManageNotices"
            to="/manage/notices"
            class="panel-card"
          >
            <strong>前往公告管理</strong>
            <p class="meta-copy">快速进入发布、编辑、删除与审核台。</p>
          </RouterLink>
          <RouterLink v-else-if="!userStore.isAuthenticated" to="/login" class="panel-card">
            <strong>登录后查看更多</strong>
            <p class="meta-copy">登录后可进入个人中心，并根据角色展示更多操作入口。</p>
          </RouterLink>
        </div>
      </article>
    </div>

    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">Latest Notices</span>
          <h2 class="page-title" style="margin-top: 16px;">最新公告</h2>
          <p class="page-subtitle" style="margin-top: 16px;">首页聚合展示最近发布的已通过公告。</p>
        </div>
        <RouterLink to="/notices" class="app-link">查看全部公告</RouterLink>
      </div>

      <div v-if="loading" class="empty-state">正在加载最新公告...</div>
      <div v-else class="notice-list">
        <RouterLink
          v-for="notice in latestNotices"
          :key="notice.id"
          :to="`/notices/${notice.id}`"
        >
          <NoticeCard :notice="notice" />
        </RouterLink>
      </div>
    </article>
  </section>
</template>
