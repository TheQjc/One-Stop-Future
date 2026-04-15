<script setup>
import { onMounted, ref } from "vue";
import { RouterLink, useRoute } from "vue-router";
import { getNoticeDetail } from "../api/notice.js";

const route = useRoute();
const notice = ref(null);
const errorMessage = ref("");

onMounted(async () => {
  try {
    notice.value = await getNoticeDetail(route.params.id);
  } catch (error) {
    errorMessage.value = error.message || "公告详情加载失败。";
  }
});
</script>

<template>
  <section class="page-stack">
    <article v-if="notice" class="section-card">
      <span class="section-eyebrow">{{ notice.category }}</span>
      <h1 class="page-title" style="margin-top: 18px;">{{ notice.title }}</h1>
      <div class="meta-row" style="margin-top: 18px; color: var(--cp-ink-soft);">
        <span>发布人：{{ notice.author }}</span>
        <span>发布时间：{{ notice.publishAt }}</span>
        <span :class="['status-badge', notice.status.toLowerCase()]">
          {{ notice.status === "APPROVED" ? "已通过" : notice.status === "REJECTED" ? "已驳回" : "待审核" }}
        </span>
      </div>
      <hr class="editorial-rule" />
      <p style="white-space: pre-line;">{{ notice.content }}</p>
      <div class="inline-form-actions" style="margin-top: 24px;">
        <RouterLink to="/notices" class="ghost-btn">返回公告列表</RouterLink>
      </div>
    </article>

    <article v-else class="section-card">
      <div class="empty-state">{{ errorMessage || "正在加载公告详情..." }}</div>
    </article>
  </section>
</template>
