<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { RouterLink } from "vue-router";
import NoticeCard from "../components/NoticeCard.vue";
import { categoryOptions, getNoticeList } from "../api/notice.js";
import { useUserStore } from "../stores/user.js";

const userStore = useUserStore();
const filters = reactive({
  category: "全部",
  keyword: "",
  page: 1,
  pageSize: 3,
});
const loading = ref(true);
const result = ref({
  list: [],
  total: 0,
  page: 1,
  pageSize: 3,
});

const pages = computed(() =>
  Math.max(1, Math.ceil(result.value.total / result.value.pageSize)),
);

async function loadData() {
  loading.value = true;
  result.value = await getNoticeList({
    ...filters,
    role: userStore.role,
    includeAll: false,
  });
  loading.value = false;
}

function resetFilters() {
  filters.category = "全部";
  filters.keyword = "";
  filters.page = 1;
  loadData();
}

function changePage(next) {
  filters.page = next;
  loadData();
}

onMounted(loadData);
</script>

<template>
  <section class="page-stack">
    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">Notice Board</span>
          <h1 class="page-title" style="margin-top: 16px;">通知公告</h1>
          <p class="page-subtitle" style="margin-top: 16px;">
            支持分页、分类筛选与关键字查询。学生默认只查看已审核通过内容。
          </p>
        </div>
      </div>

      <div class="two-col-grid">
        <label class="field-label">
          分类
          <select v-model="filters.category" class="field-select" @change="loadData">
            <option v-for="item in categoryOptions()" :key="item" :value="item">{{ item }}</option>
          </select>
        </label>
        <label class="field-label">
          关键字
          <input
            v-model.trim="filters.keyword"
            class="field-control"
            type="text"
            placeholder="搜索标题或摘要"
            @keyup.enter="loadData"
          />
        </label>
      </div>

      <div class="inline-form-actions" style="margin-top: 20px;">
        <button type="button" class="app-btn" @click="loadData">查询</button>
        <button type="button" class="ghost-btn" @click="resetFilters">重置</button>
      </div>
    </article>

    <article class="section-card">
      <div v-if="loading" class="empty-state">正在加载公告列表...</div>
      <div v-else-if="!result.list.length" class="empty-state">当前筛选条件下暂无公告。</div>
      <div v-else class="notice-list">
        <RouterLink
          v-for="notice in result.list"
          :key="notice.id"
          :to="`/notices/${notice.id}`"
        >
          <NoticeCard :notice="notice" />
        </RouterLink>
      </div>

      <div class="app-pagination" style="margin-top: 24px;">
        <button
          type="button"
          class="ghost-btn"
          :disabled="filters.page <= 1"
          @click="changePage(filters.page - 1)"
        >
          上一页
        </button>
        <span class="meta-copy">第 {{ filters.page }} / {{ pages }} 页</span>
        <button
          type="button"
          class="ghost-btn"
          :disabled="filters.page >= pages"
          @click="changePage(filters.page + 1)"
        >
          下一页
        </button>
      </div>
    </article>
  </section>
</template>
