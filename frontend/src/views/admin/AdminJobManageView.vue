<script setup>
import { computed, reactive, ref } from "vue";
import {
  createAdminJob,
  deleteAdminJob,
  getAdminJobs,
  importAdminJobs,
  offlineAdminJob,
  publishAdminJob,
  syncAdminJobs,
  updateAdminJob,
} from "../../api/admin.js";

const loading = ref(true);
const errorMessage = ref("");
const actionMessage = ref("");
const actionLoadingId = ref("");
const selectedJobId = ref(null);
const importFile = ref(null);
const importLoading = ref(false);
const importSummary = ref(null);
const importErrors = ref([]);
const importErrorMessage = ref("");
const importInputRef = ref(null);
const syncLoading = ref(false);
const syncSummary = ref(null);
const syncIssues = ref([]);
const syncErrorMessage = ref("");
const summary = ref({
  total: 0,
  jobs: [],
});

const form = reactive({
  title: "",
  companyName: "",
  city: "",
  jobType: "INTERNSHIP",
  educationRequirement: "ANY",
  sourcePlatform: "官方渠道",
  sourceUrl: "",
  summary: "",
  content: "",
  deadlineAt: "",
});

const statusCards = computed(() => {
  const jobs = summary.value.jobs || [];

  return [
    { label: "全部岗位", value: jobs.length },
    { label: "草稿", value: jobs.filter((item) => item.status === "DRAFT").length },
    { label: "已发布", value: jobs.filter((item) => item.status === "PUBLISHED").length },
    { label: "已下线", value: jobs.filter((item) => item.status === "OFFLINE").length },
  ];
});

const editingJob = computed(() => (
  summary.value.jobs.find((item) => item.id === selectedJobId.value) || null
));

function resetForm() {
  selectedJobId.value = null;
  form.title = "";
  form.companyName = "";
  form.city = "";
  form.jobType = "INTERNSHIP";
  form.educationRequirement = "ANY";
  form.sourcePlatform = "官方渠道";
  form.sourceUrl = "";
  form.summary = "";
  form.content = "";
  form.deadlineAt = "";
}

function applyJobToForm(job) {
  selectedJobId.value = job.id;
  form.title = job.title || "";
  form.companyName = job.companyName || "";
  form.city = job.city || "";
  form.jobType = job.jobType || "INTERNSHIP";
  form.educationRequirement = job.educationRequirement || "ANY";
  form.sourcePlatform = job.sourcePlatform || "官方渠道";
  form.sourceUrl = job.sourceUrl || "";
  form.summary = job.summary || "";
  form.content = job.content || "";
  form.deadlineAt = job.deadlineAt ? String(job.deadlineAt).slice(0, 16) : "";
}

function resetImportInput() {
  importFile.value = null;
  if (importInputRef.value) {
    importInputRef.value.value = "";
  }
}

function handleImportFileChange(event) {
  importFile.value = event.target.files?.[0] || null;
}

function statusLabel(status) {
  const labels = {
    DRAFT: "草稿",
    PUBLISHED: "已发布",
    OFFLINE: "已下线",
    DELETED: "已删除",
  };

  return labels[status] || status || "处理中";
}

async function handleImportJobs() {
  importErrorMessage.value = "";
  importErrors.value = [];
  importSummary.value = null;

  if (!importFile.value) {
    importErrorMessage.value = "请先选择一个 CSV 文件。";
    return;
  }

  const formData = new FormData();
  formData.append("file", importFile.value);
  importLoading.value = true;

  try {
    importSummary.value = await importAdminJobs(formData);
    resetImportInput();
    await loadJobs();
  } catch (error) {
    importErrorMessage.value = error.message || "岗位导入失败，请稍后重试。";
    importErrors.value = Array.isArray(error.data?.errors) ? error.data.errors : [];
  } finally {
    importLoading.value = false;
  }
}

async function handleSyncJobs() {
  syncLoading.value = true;
  syncSummary.value = null;
  syncIssues.value = [];
  syncErrorMessage.value = "";

  try {
    const result = await syncAdminJobs();
    syncSummary.value = result;
    syncIssues.value = Array.isArray(result.issues) ? result.issues : [];
    await loadJobs();
  } catch (error) {
    syncErrorMessage.value = error.message || "岗位同步失败，请稍后重试。";
  } finally {
    syncLoading.value = false;
  }
}

function toPayload() {
  return {
    title: form.title.trim(),
    companyName: form.companyName.trim(),
    city: form.city.trim(),
    jobType: form.jobType,
    educationRequirement: form.educationRequirement,
    sourcePlatform: form.sourcePlatform.trim(),
    sourceUrl: form.sourceUrl.trim(),
    summary: form.summary.trim(),
    content: form.content.trim(),
    deadlineAt: form.deadlineAt ? `${form.deadlineAt}:00` : null,
  };
}

async function loadJobs() {
  loading.value = true;
  errorMessage.value = "";

  try {
    summary.value = await getAdminJobs();
  } catch (error) {
    errorMessage.value = error.message || "岗位管理列表加载失败，请稍后重试。";
  } finally {
    loading.value = false;
  }
}

async function handleSaveDraft() {
  actionMessage.value = "";
  errorMessage.value = "";
  actionLoadingId.value = selectedJobId.value ? `save-${selectedJobId.value}` : "create";

  try {
    const payload = toPayload();
    const saved = selectedJobId.value
      ? await updateAdminJob(selectedJobId.value, payload)
      : await createAdminJob(payload);
    actionMessage.value = selectedJobId.value ? "草稿已更新。" : "草稿已创建。";
    applyJobToForm(saved);
    await loadJobs();
  } catch (error) {
    errorMessage.value = error.message || "草稿保存失败，请稍后重试。";
  } finally {
    actionLoadingId.value = "";
  }
}

async function handlePublish(id) {
  actionMessage.value = "";
  errorMessage.value = "";
  actionLoadingId.value = `publish-${id}`;

  try {
    const saved = await publishAdminJob(id);
    actionMessage.value = "岗位已发布。";
    applyJobToForm(saved);
    await loadJobs();
  } catch (error) {
    errorMessage.value = error.message || "发布失败，请稍后重试。";
  } finally {
    actionLoadingId.value = "";
  }
}

async function handleOffline(id) {
  actionMessage.value = "";
  errorMessage.value = "";
  actionLoadingId.value = `offline-${id}`;

  try {
    const saved = await offlineAdminJob(id);
    actionMessage.value = "岗位已下线。";
    applyJobToForm(saved);
    await loadJobs();
  } catch (error) {
    errorMessage.value = error.message || "下线失败，请稍后重试。";
  } finally {
    actionLoadingId.value = "";
  }
}

async function handleDelete(id) {
  actionMessage.value = "";
  errorMessage.value = "";
  actionLoadingId.value = `delete-${id}`;

  try {
    await deleteAdminJob(id);
    actionMessage.value = "岗位已删除。";
    if (selectedJobId.value === id) {
      resetForm();
    }
    await loadJobs();
  } catch (error) {
    errorMessage.value = error.message || "删除失败，请稍后重试。";
  } finally {
    actionLoadingId.value = "";
  }
}

loadJobs();
</script>

<template>
  <section class="page-stack">
    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">岗位管理</span>
          <h1 class="page-title" style="margin-top: 16px;">在一个页面管理岗位看板</h1>
          <p class="page-subtitle" style="margin-top: 16px;">
            在同一页内完成岗位草稿创建、编辑、发布、下线和删除。
          </p>
        </div>
      </div>

      <div class="stats-grid admin-jobs-stats">
        <article
          v-for="card in statusCards"
          :key="card.label"
          class="panel-card admin-jobs-stat"
        >
          <span class="admin-jobs-stat__label">{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
        </article>
      </div>
    </article>

    <div class="dashboard-grid">
      <article class="section-card">
        <div class="section-header">
          <div>
            <span class="section-eyebrow">同步</span>
            <h2 class="page-title" style="margin-top: 16px;">从配置好的合作源同步岗位</h2>
            <p class="page-subtitle" style="margin-top: 16px;">
              后端会读取固定 HTTP JSON 数据源，并把新的合作岗位记录创建为草稿。
            </p>
          </div>
        </div>

        <div class="field-grid">
          <p class="field-hint">
            同步会按来源链接更新未删除岗位，跳过已删除匹配项，并输出无效数据行。
          </p>
          <p v-if="syncSummary" class="field-hint">
            {{ syncSummary.sourceName }}：新增 {{ syncSummary.createdCount }}，更新 {{ syncSummary.updatedCount }}，
            跳过 {{ syncSummary.skippedCount }}，无效 {{ syncSummary.invalidCount }}。
          </p>
          <p v-if="syncErrorMessage" class="field-error" role="alert">{{ syncErrorMessage }}</p>

          <ul v-if="syncIssues.length" class="import-error-list">
            <li v-for="item in syncIssues" :key="`${item.itemIndex}-${item.type}-${item.sourceUrl}`">
              第 {{ item.itemIndex }} 条 / {{ item.type }} / {{ item.sourceUrl || "无来源链接" }}：{{ item.message }}
            </li>
          </ul>

          <div class="inline-form-actions">
            <button
              type="button"
              class="app-btn"
              data-testid="job-sync-button"
              :disabled="syncLoading"
              @click="handleSyncJobs"
            >
              {{ syncLoading ? "同步中..." : "开始同步" }}
            </button>
          </div>
        </div>
      </article>

      <article class="section-card">
        <div class="section-header">
          <div>
            <span class="section-eyebrow">导入</span>
            <h2 class="page-title" style="margin-top: 16px;">批量导入岗位草稿</h2>
            <p class="page-subtitle" style="margin-top: 16px;">
              上传一个 UTF-8 编码的 CSV 文件，一次性创建多条岗位草稿。
            </p>
          </div>
        </div>

        <form data-testid="job-import-form" class="field-grid" @submit.prevent="handleImportJobs">
          <label class="field-label">
            CSV 文件
            <input
              ref="importInputRef"
              class="field-control"
              name="jobImportFile"
              type="file"
              accept=".csv,text/csv"
              @change="handleImportFileChange"
            />
          </label>

          <p class="field-hint">
            表头顺序：title、companyName、city、jobType、educationRequirement、sourcePlatform、
            sourceUrl、summary、content、deadlineAt。
          </p>
          <p class="field-hint">
            必填：title、companyName、city、jobType、educationRequirement、sourcePlatform、
            sourceUrl、summary。可选：content、deadlineAt。
          </p>
          <p v-if="importSummary" class="field-hint">
            已从 {{ importSummary.fileName }} 导入 {{ importSummary.importedCount }} 条岗位，默认状态为
            {{ statusLabel(importSummary.defaultStatus) }}。
          </p>
          <p v-if="importErrorMessage" class="field-error" role="alert">{{ importErrorMessage }}</p>

          <ul v-if="importErrors.length" class="import-error-list">
            <li v-for="item in importErrors" :key="`${item.rowNumber}-${item.column}-${item.message}`">
              第 {{ item.rowNumber }} 行 / {{ item.column }}：{{ item.message }}
            </li>
          </ul>

          <div class="inline-form-actions">
            <button type="submit" class="app-btn" :disabled="importLoading">
              {{ importLoading ? "导入中..." : "导入 CSV" }}
            </button>
          </div>
        </form>
      </article>

      <article class="section-card">
        <div class="section-header">
          <div>
            <span class="section-eyebrow">编辑器</span>
            <h2 class="page-title" style="margin-top: 16px;">
              {{ editingJob ? "编辑当前岗位" : "新建岗位草稿" }}
            </h2>
          </div>
        </div>

        <form class="field-grid" @submit.prevent="handleSaveDraft">
          <label class="field-label">
            标题
            <input v-model.trim="form.title" class="field-control" name="title" type="text" maxlength="120" />
          </label>

          <label class="field-label">
            公司
            <input v-model.trim="form.companyName" class="field-control" name="companyName" type="text" maxlength="80" />
          </label>

          <label class="field-label">
            城市
            <input v-model.trim="form.city" class="field-control" name="city" type="text" maxlength="80" />
          </label>

          <label class="field-label">
            来源链接
            <input v-model.trim="form.sourceUrl" class="field-control" name="sourceUrl" type="url" maxlength="500" />
          </label>

          <label class="field-label">
            岗位类型
            <select v-model="form.jobType" class="field-select" name="jobType">
              <option value="INTERNSHIP">实习</option>
              <option value="FULL_TIME">全职</option>
              <option value="CAMPUS">校招</option>
            </select>
          </label>

          <label class="field-label">
            学历要求
            <select v-model="form.educationRequirement" class="field-select" name="educationRequirement">
              <option value="ANY">不限</option>
              <option value="BACHELOR">本科</option>
              <option value="MASTER">硕士</option>
              <option value="DOCTOR">博士</option>
            </select>
          </label>

          <label class="field-label">
            来源平台
            <input v-model.trim="form.sourcePlatform" class="field-control" name="sourcePlatform" type="text" maxlength="50" />
          </label>

          <label class="field-label">
            截止时间
            <input v-model="form.deadlineAt" class="field-control" name="deadlineAt" type="datetime-local" />
          </label>

          <label class="field-label">
            摘要
            <textarea v-model.trim="form.summary" class="field-textarea" name="summary" maxlength="300" />
          </label>

          <label class="field-label">
            正文
            <textarea v-model.trim="form.content" class="field-textarea" name="content" maxlength="10000" />
          </label>

          <p v-if="actionMessage" class="field-hint">{{ actionMessage }}</p>
          <p v-if="errorMessage" class="field-error" role="alert">{{ errorMessage }}</p>

          <div class="inline-form-actions">
            <button type="submit" class="app-btn" :disabled="Boolean(actionLoadingId)">
              {{ selectedJobId ? "保存草稿" : "创建草稿" }}
            </button>
            <button type="button" class="ghost-btn" :disabled="Boolean(actionLoadingId)" @click="resetForm">
              重置
            </button>
            <button
              v-if="selectedJobId"
              type="button"
              class="ghost-btn"
              :disabled="Boolean(actionLoadingId)"
              @click="handlePublish(selectedJobId)"
            >
              发布当前岗位
            </button>
          </div>
        </form>
      </article>

      <article class="section-card">
        <div class="section-header">
          <div>
            <span class="section-eyebrow">岗位列表</span>
            <h2 class="page-title" style="margin-top: 16px;">当前岗位记录</h2>
          </div>
        </div>

        <div v-if="loading" class="empty-state">正在加载岗位列表...</div>
        <div v-else-if="errorMessage && !summary.jobs.length" class="field-grid">
          <p class="field-error" role="alert">{{ errorMessage }}</p>
          <button type="button" class="ghost-btn" @click="loadJobs">
            重试
          </button>
        </div>
        <div v-else>
          <table class="app-table">
            <thead>
              <tr>
                <th>标题</th>
                <th>公司</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="job in summary.jobs" :key="job.id">
                <td>{{ job.title }}</td>
                <td>{{ job.companyName }}</td>
                <td>{{ statusLabel(job.status) }}</td>
                <td>
                  <div class="inline-form-actions">
                    <button type="button" class="ghost-btn" @click="applyJobToForm(job)">
                      编辑
                    </button>
                    <button
                      type="button"
                      class="ghost-btn publish-action"
                      :disabled="job.status === 'PUBLISHED' || actionLoadingId === `publish-${job.id}`"
                      @click="handlePublish(job.id)"
                    >
                      发布
                    </button>
                    <button
                      type="button"
                      class="ghost-btn offline-action"
                      :disabled="job.status !== 'PUBLISHED' || actionLoadingId === `offline-${job.id}`"
                      @click="handleOffline(job.id)"
                    >
                      下线
                    </button>
                    <button
                      type="button"
                      class="danger-btn"
                      :disabled="job.status === 'DELETED' || actionLoadingId === `delete-${job.id}`"
                      @click="handleDelete(job.id)"
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
              v-for="job in summary.jobs"
              :key="`mobile-${job.id}`"
              class="table-card"
            >
              <strong>{{ job.title }}</strong>
              <p class="meta-copy">{{ job.companyName }} / {{ statusLabel(job.status) }}</p>
              <div class="inline-form-actions" style="margin-top: 12px;">
                <button type="button" class="ghost-btn" @click="applyJobToForm(job)">
                  编辑
                </button>
                <button
                  type="button"
                  class="ghost-btn publish-action"
                  :disabled="job.status === 'PUBLISHED' || actionLoadingId === `publish-${job.id}`"
                  @click="handlePublish(job.id)"
                >
                  发布
                </button>
                <button
                  type="button"
                  class="ghost-btn offline-action"
                  :disabled="job.status !== 'PUBLISHED' || actionLoadingId === `offline-${job.id}`"
                  @click="handleOffline(job.id)"
                >
                  下线
                </button>
                <button
                  type="button"
                  class="danger-btn"
                  :disabled="job.status === 'DELETED' || actionLoadingId === `delete-${job.id}`"
                  @click="handleDelete(job.id)"
                >
                  删除
                </button>
              </div>
            </article>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.admin-jobs-stats {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.admin-jobs-stat {
  min-height: 128px;
  display: grid;
  gap: 8px;
  align-content: end;
}

.admin-jobs-stat__label {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.admin-jobs-stat strong {
  font-family: var(--cp-font-display);
  font-size: clamp(24px, 4vw, 32px);
}

.import-error-list {
  margin: 0;
  padding-left: 20px;
  color: var(--cp-danger, #b42318);
}

@media (max-width: 1023px) {
  .admin-jobs-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .admin-jobs-stats {
    grid-template-columns: 1fr;
  }
}
</style>
