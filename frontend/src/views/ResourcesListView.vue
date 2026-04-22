<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { RouterLink } from "vue-router";
import { getResources } from "../api/resources.js";
import ResourceCard from "../components/ResourceCard.vue";
import ResourceFilterBar from "../components/ResourceFilterBar.vue";
import { useUserStore } from "../stores/user.js";

const userStore = useUserStore();

const loading = ref(false);
const errorMessage = ref("");
const filters = reactive({
  keyword: "",
  category: "",
});
const summary = ref({
  total: 0,
  resources: [],
});

const categoryOptions = [
  { value: "", label: "全部分类" },
  { value: "EXAM_PAPER", label: "真题试卷" },
  { value: "LANGUAGE_TEST", label: "语言考试" },
  { value: "RESUME_TEMPLATE", label: "简历模板" },
  { value: "INTERVIEW_EXPERIENCE", label: "面试经验" },
  { value: "OTHER", label: "其他资料" },
];

function getOptionLabel(options, value) {
  return options.find((option) => option.value === value)?.label || value;
}

const activeFilterLabels = computed(() => {
  const items = [];
  if (filters.keyword) items.push(`关键词：${filters.keyword}`);
  if (filters.category) items.push(`分类：${getOptionLabel(categoryOptions, filters.category)}`);
  return items;
});

const uploadTarget = computed(() => (
  userStore.isAuthenticated
    ? "/resources/upload"
    : { name: "login", query: { redirect: "/resources/upload" } }
));

function buildParams() {
  return Object.fromEntries(
    Object.entries(filters).filter(([, value]) => value),
  );
}

async function loadResources() {
  loading.value = true;
  errorMessage.value = "";

  try {
    summary.value = await getResources(buildParams());
  } catch (error) {
    errorMessage.value = error.message || "资料库加载失败，请稍后重试。";
  } finally {
    loading.value = false;
  }
}

function updateFilters(nextFilters) {
  Object.assign(filters, nextFilters);
}

function resetFilters() {
  Object.assign(filters, {
    keyword: "",
    category: "",
  });
  loadResources();
}

onMounted(loadResources);
</script>

<template>
  <section class="page-stack">
    <article class="section-card archive-hero">
      <div class="archive-hero__copy">
        <span class="section-eyebrow">资料库</span>
        <h1 class="hero-title" style="margin-top: 18px;">把常用资料集中整理好，查阅前先看清分类。</h1>
        <hr class="editorial-rule" />
        <p class="hero-copy">
          资料页会把公开资料、分类筛选和下载信息放在同一页，方便你先比较摘要、上传者、
          文件大小和下载热度，再决定是否打开详情。
        </p>

        <div v-if="activeFilterLabels.length" class="chip-row" style="margin-top: 24px;">
          <span
            v-for="item in activeFilterLabels"
            :key="item"
            class="status-badge pending"
          >
            {{ item }}
          </span>
        </div>
      </div>

      <div class="archive-hero__panel">
        <div class="panel-card archive-hero__stat">
          <span class="archive-hero__label">已收录资料</span>
          <strong>{{ summary.total }}</strong>
        </div>
        <div class="panel-card archive-hero__stat">
          <span class="archive-hero__label">下载权限</span>
          <strong>{{ userStore.isAuthenticated ? "已开启" : "登录后可用" }}</strong>
        </div>
        <RouterLink :to="uploadTarget" class="app-btn">
          {{ userStore.isAuthenticated ? "上传资料" : "登录后上传" }}
        </RouterLink>
      </div>
    </article>

    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">资料筛选</span>
          <h2 class="page-title" style="margin-top: 16px;">先缩小范围，再打开具体资料。</h2>
        </div>
      </div>

      <ResourceFilterBar
        :filters="filters"
        :loading="loading"
        :category-options="categoryOptions"
        @update:filters="updateFilters"
        @submit="loadResources"
        @reset="resetFilters"
      />
    </article>

    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">资料列表</span>
          <h2 class="page-title" style="margin-top: 16px;">当前资料</h2>
          <p class="page-subtitle" style="margin-top: 16px;">
            资料卡片会集中展示分类、上传者、文件大小和下载热度，方便你不离开页面也能快速比较。
          </p>
        </div>
      </div>

      <div v-if="loading" class="empty-state">正在加载资料...</div>
      <div v-else-if="errorMessage" class="field-grid">
        <p class="field-error" role="alert">{{ errorMessage }}</p>
        <button type="button" class="ghost-btn" @click="loadResources">
          重试
        </button>
      </div>
      <div v-else-if="!summary.resources.length" class="empty-state">
        当前筛选条件下还没有匹配的资料，试试重置筛选后再看看。
      </div>
      <div v-else class="resources-grid">
        <ResourceCard
          v-for="resource in summary.resources"
          :key="resource.id"
          :resource="resource"
        />
      </div>
    </article>
  </section>
</template>

<style scoped>
.archive-hero {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: var(--cp-gap-6);
  background:
    linear-gradient(180deg, rgba(255, 252, 247, 0.94), rgba(245, 237, 223, 0.98)),
    radial-gradient(circle at top left, rgba(184, 130, 35, 0.2), transparent 30%),
    radial-gradient(circle at bottom right, rgba(76, 122, 116, 0.16), transparent 34%);
}

.archive-hero__copy,
.archive-hero__panel {
  display: grid;
  gap: var(--cp-gap-4);
}

.archive-hero__panel {
  align-content: end;
}

.archive-hero__stat {
  min-height: 132px;
  display: grid;
  gap: 8px;
  align-content: end;
}

.archive-hero__label {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.archive-hero__stat strong {
  font-size: clamp(26px, 4vw, 34px);
  font-family: var(--cp-font-display);
}

.resources-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cp-gap-4);
}

@media (max-width: 1023px) {
  .archive-hero,
  .resources-grid {
    grid-template-columns: 1fr;
  }
}
</style>
