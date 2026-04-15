<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import {
  categoryOptions,
  createNotice,
  deleteNotice,
  getNoticeList,
  reviewNotice,
  updateNotice,
} from "../../api/notice.js";
import { useUserStore } from "../../stores/user.js";

const userStore = useUserStore();
const noticeForm = reactive({
  id: null,
  title: "",
  category: "教学通知",
  summary: "",
  content: "",
  isTop: false,
});
const message = ref("");
const errorMessage = ref("");
const notices = ref([]);
const loading = ref(true);

const canReview = computed(() => ["TEACHER", "ADMIN"].includes(userStore.role));
const formTitle = computed(() => (noticeForm.id ? "编辑公告" : "发布公告"));

function resetForm() {
  noticeForm.id = null;
  noticeForm.title = "";
  noticeForm.category = "教学通知";
  noticeForm.summary = "";
  noticeForm.content = "";
  noticeForm.isTop = false;
}

async function loadData() {
  loading.value = true;
  const data = await getNoticeList({
    page: 1,
    pageSize: 20,
    includeAll: true,
    role: userStore.role,
  });
  notices.value = data.list;
  loading.value = false;
}

function editNotice(notice) {
  noticeForm.id = notice.id;
  noticeForm.title = notice.title;
  noticeForm.category = notice.category;
  noticeForm.summary = notice.summary;
  noticeForm.content = notice.content;
  noticeForm.isTop = notice.isTop;
  message.value = "";
  errorMessage.value = "";
}

async function submitNotice() {
  message.value = "";
  errorMessage.value = "";

  if (!noticeForm.title || !noticeForm.summary || !noticeForm.content) {
    errorMessage.value = "请完整填写标题、摘要和正文。";
    return;
  }

  try {
    if (noticeForm.id) {
      await updateNotice(noticeForm.id, { ...noticeForm });
      message.value = "公告已更新，并重新进入待审核状态。";
    } else {
      await createNotice({ ...noticeForm }, userStore.profile);
      message.value = "公告已提交，等待审核。";
    }
    resetForm();
    await loadData();
  } catch (error) {
    errorMessage.value = error.message || "保存公告失败。";
  }
}

async function removeNotice(id) {
  await deleteNotice(id);
  message.value = "公告已删除。";
  await loadData();
}

async function doReview(id, status) {
  await reviewNotice(id, status);
  message.value = status === "APPROVED" ? "公告已审核通过。" : "公告已驳回。";
  await loadData();
}

onMounted(loadData);
</script>

<template>
  <section class="page-stack">
    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">Management Desk</span>
          <h1 class="page-title" style="margin-top: 16px;">教师 / 管理员公告管理</h1>
          <p class="page-subtitle" style="margin-top: 16px;">
            当前角色为
            <strong>{{ userStore.roleLabel }}</strong>
            ，支持发布、编辑、删除与审核。教师和管理员均具备审核权限。
          </p>
        </div>
      </div>

      <div class="dashboard-grid">
        <article class="panel-card">
          <h2 class="page-title" style="font-size: 28px;">{{ formTitle }}</h2>
          <form class="field-grid" style="margin-top: 20px;" @submit.prevent="submitNotice">
            <label class="field-label">
              标题
              <input v-model.trim="noticeForm.title" class="field-control" type="text" />
            </label>
            <label class="field-label">
              分类
              <select v-model="noticeForm.category" class="field-select">
                <option
                  v-for="item in categoryOptions().filter((item) => item !== '全部')"
                  :key="item"
                  :value="item"
                >
                  {{ item }}
                </option>
              </select>
            </label>
            <label class="field-label">
              摘要
              <textarea v-model.trim="noticeForm.summary" class="field-textarea" style="min-height: 96px;" />
            </label>
            <label class="field-label">
              正文
              <textarea v-model.trim="noticeForm.content" class="field-textarea" />
            </label>
            <label class="field-label" style="display: flex; align-items: center; gap: 12px;">
              <input v-model="noticeForm.isTop" type="checkbox" />
              置顶显示
            </label>

            <p v-if="message" class="field-hint">{{ message }}</p>
            <p v-if="errorMessage" class="field-error" role="alert">{{ errorMessage }}</p>

            <div class="inline-form-actions">
              <button type="submit" class="app-btn">保存公告</button>
              <button type="button" class="ghost-btn" @click="resetForm">重置表单</button>
            </div>
          </form>
        </article>

        <article class="panel-card">
          <h2 class="page-title" style="font-size: 28px;">审核说明</h2>
          <div class="notice-list" style="margin-top: 20px;">
            <div class="table-card">
              <strong>待审核</strong>
              <p class="meta-copy">新建或编辑后的公告自动进入待审核状态。</p>
            </div>
            <div class="table-card">
              <strong>通过规则</strong>
              <p class="meta-copy">教师与管理员均可审核通过或驳回，前台首页只显示已通过内容。</p>
            </div>
            <div class="table-card">
              <strong>页面策略</strong>
              <p class="meta-copy">保持与首页同一视觉系统，但信息密度更高，更适合日常运维处理。</p>
            </div>
          </div>
        </article>
      </div>
    </article>

    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">Review Queue</span>
          <h2 class="page-title" style="margin-top: 16px;">公告列表与审核</h2>
        </div>
      </div>

      <div v-if="loading" class="empty-state">正在加载管理列表...</div>
      <template v-else>
        <table class="app-table">
          <thead>
            <tr>
              <th>标题</th>
              <th>分类</th>
              <th>作者</th>
              <th>状态</th>
              <th>时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="notice in notices" :key="notice.id">
              <td>
                <strong>{{ notice.title }}</strong>
                <p class="meta-copy" style="margin-top: 8px;">{{ notice.summary }}</p>
              </td>
              <td>{{ notice.category }}</td>
              <td>{{ notice.author }}</td>
              <td>
                <span :class="['status-badge', notice.status.toLowerCase()]">
                  {{
                    notice.status === "APPROVED"
                      ? "已通过"
                      : notice.status === "REJECTED"
                        ? "已驳回"
                        : "待审核"
                  }}
                </span>
              </td>
              <td>{{ notice.publishAt }}</td>
              <td>
                <div class="action-row">
                  <button type="button" class="ghost-btn" @click="editNotice(notice)">编辑</button>
                  <button type="button" class="danger-btn" @click="removeNotice(notice.id)">删除</button>
                  <button
                    v-if="canReview"
                    type="button"
                    class="app-link"
                    @click="doReview(notice.id, 'APPROVED')"
                  >
                    审核通过
                  </button>
                  <button
                    v-if="canReview"
                    type="button"
                    class="ghost-btn"
                    @click="doReview(notice.id, 'REJECTED')"
                  >
                    驳回
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <div class="mobile-table-cards">
          <article v-for="notice in notices" :key="notice.id" class="table-card">
            <div class="chip-row">
              <span class="section-eyebrow">{{ notice.category }}</span>
              <span :class="['status-badge', notice.status.toLowerCase()]">
                {{
                  notice.status === "APPROVED"
                    ? "已通过"
                    : notice.status === "REJECTED"
                      ? "已驳回"
                      : "待审核"
                }}
              </span>
            </div>
            <h3 style="margin: 16px 0 8px;">{{ notice.title }}</h3>
            <p class="meta-copy">{{ notice.summary }}</p>
            <p class="meta-copy">{{ notice.author }} · {{ notice.publishAt }}</p>
            <div class="action-row" style="margin-top: 16px;">
              <button type="button" class="ghost-btn" @click="editNotice(notice)">编辑</button>
              <button type="button" class="danger-btn" @click="removeNotice(notice.id)">删除</button>
              <button
                v-if="canReview"
                type="button"
                class="app-link"
                @click="doReview(notice.id, 'APPROVED')"
              >
                审核通过
              </button>
            </div>
          </article>
        </div>
      </template>
    </article>
  </section>
</template>
