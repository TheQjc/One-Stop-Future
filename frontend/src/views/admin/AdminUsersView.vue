<script setup>
import { onMounted, ref } from "vue";
import { banAdminUser, getAdminUsers, unbanAdminUser } from "../../api/admin.js";

const loading = ref(true);
const errorMessage = ref("");
const actionMessage = ref("");
const actionError = ref("");
const actionLoadingId = ref("");
const summary = ref({
  total: 0,
  activeCount: 0,
  bannedCount: 0,
  verifiedCount: 0,
  users: [],
});

const statCards = [
  { key: "total", label: "全部账号" },
  { key: "activeCount", label: "正常账号" },
  { key: "bannedCount", label: "已封禁" },
  { key: "verifiedCount", label: "已认证" },
];

function formatTime(value) {
  if (!value) {
    return "时间未知";
  }

  return String(value).replace("T", " ").slice(0, 16);
}

function isProtectedUser(user) {
  return user.role === "ADMIN";
}

function statusTone(status) {
  return status === "BANNED" ? "rejected" : "approved";
}

function verificationTone(status) {
  if (status === "VERIFIED") {
    return "approved";
  }
  if (status === "PENDING") {
    return "pending";
  }
  return "";
}

function statusLabel(status) {
  const labels = {
    ACTIVE: "正常",
    BANNED: "已封禁",
  };

  return labels[status] || status || "正常";
}

function verificationLabel(status) {
  const labels = {
    VERIFIED: "已认证",
    PENDING: "待认证",
    UNVERIFIED: "未认证",
  };

  return labels[status] || status || "未认证";
}

function roleLabel(role) {
  const labels = {
    ADMIN: "管理员",
    USER: "普通用户",
  };

  return labels[role] || role || "普通用户";
}

async function loadUsers() {
  loading.value = true;
  errorMessage.value = "";

  try {
    summary.value = await getAdminUsers();
  } catch (error) {
    errorMessage.value = error.message || "用户列表加载失败，请稍后重试。";
  } finally {
    loading.value = false;
  }
}

async function handleBan(user) {
  actionMessage.value = "";
  actionError.value = "";
  actionLoadingId.value = `ban-${user.id}`;

  try {
    await banAdminUser(user.id);
    actionMessage.value = `已封禁 ${user.nickname || user.phone || `user-${user.id}`}。`;
    await loadUsers();
  } catch (error) {
    actionError.value = error.message || "封禁失败，请稍后重试。";
  } finally {
    actionLoadingId.value = "";
  }
}

async function handleUnban(user) {
  actionMessage.value = "";
  actionError.value = "";
  actionLoadingId.value = `unban-${user.id}`;

  try {
    await unbanAdminUser(user.id);
    actionMessage.value = `已恢复 ${user.nickname || user.phone || `user-${user.id}`}。`;
    await loadUsers();
  } catch (error) {
    actionError.value = error.message || "恢复失败，请稍后重试。";
  } finally {
    actionLoadingId.value = "";
  }
}

onMounted(loadUsers);
</script>

<template>
  <section class="page-stack">
    <article v-if="loading" class="section-card">
      <div class="empty-state">正在加载用户列表...</div>
    </article>

    <article v-else-if="errorMessage" class="section-card">
      <div class="field-grid">
        <p class="field-error" role="alert">{{ errorMessage }}</p>
        <button type="button" class="ghost-btn" @click="loadUsers">
          重试
        </button>
      </div>
    </article>

    <template v-else>
      <article class="section-card">
        <div class="section-header">
          <div>
            <span class="section-eyebrow">用户管理</span>
            <h1 class="page-title" style="margin-top: 16px;">账号状态工作台</h1>
            <p class="page-subtitle" style="margin-top: 16px;">
              在这里查看账号状态、认证情况和受保护账户，并对普通用户执行封禁或恢复操作。
            </p>
          </div>
        </div>

        <div class="stats-grid admin-users-stats">
          <article
            v-for="card in statCards"
            :key="card.key"
            class="panel-card admin-users-stat"
          >
            <span class="admin-users-stat__label">{{ card.label }}</span>
            <strong>{{ summary[card.key] || 0 }}</strong>
          </article>
        </div>

        <p class="meta-copy" style="margin-top: 20px;">
          当前阶段管理员账号受保护，不支持在这个页面直接切换状态。
        </p>
        <p v-if="actionMessage" class="field-hint" style="margin-top: 16px;">{{ actionMessage }}</p>
        <p v-if="actionError" class="field-error" role="alert" style="margin-top: 12px;">
          {{ actionError }}
        </p>
      </article>

      <article class="section-card">
        <div v-if="!summary.users.length" class="empty-state">
          当前还没有可管理的用户账号。
        </div>
        <div v-else>
          <table class="app-table">
            <thead>
              <tr>
                <th>用户</th>
                <th>角色</th>
                <th>认证</th>
                <th>账号状态</th>
                <th>学号</th>
                <th>更新时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in summary.users" :key="user.id">
                <td>
                  <div class="admin-users-table__identity">
                    <strong>{{ user.nickname || user.phone || `用户 ${user.id}` }}</strong>
                    <span>用户 ID {{ user.id }} / {{ user.phone || "未填写手机号" }}</span>
                    <span v-if="user.realName">{{ user.realName }}</span>
                  </div>
                </td>
                <td>{{ roleLabel(user.role) }}</td>
                <td>
                  <span class="status-badge" :class="verificationTone(user.verificationStatus)">
                    {{ verificationLabel(user.verificationStatus) }}
                  </span>
                </td>
                <td>
                  <span class="status-badge" :class="statusTone(user.status)">
                    {{ statusLabel(user.status) }}
                  </span>
                </td>
                <td>{{ user.studentId || "未登记" }}</td>
                <td>{{ formatTime(user.updatedAt || user.createdAt) }}</td>
                <td>
                  <div class="inline-form-actions">
                    <span v-if="isProtectedUser(user)" class="status-badge">受保护</span>
                    <button
                      v-else-if="user.status === 'BANNED'"
                      :data-testid="`unban-user-${user.id}`"
                      type="button"
                      class="ghost-btn"
                      :disabled="actionLoadingId === `unban-${user.id}`"
                      @click="handleUnban(user)"
                    >
                      {{ actionLoadingId === `unban-${user.id}` ? "恢复中..." : "恢复" }}
                    </button>
                    <button
                      v-else
                      :data-testid="`ban-user-${user.id}`"
                      type="button"
                      class="danger-btn"
                      :disabled="actionLoadingId === `ban-${user.id}`"
                      @click="handleBan(user)"
                    >
                      {{ actionLoadingId === `ban-${user.id}` ? "封禁中..." : "封禁" }}
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>

          <div class="mobile-table-cards">
            <article
              v-for="user in summary.users"
              :key="`mobile-${user.id}`"
              class="table-card admin-user-card"
            >
              <div class="admin-user-card__header">
                <div>
                  <p class="admin-user-card__eyebrow">用户 #{{ user.id }}</p>
                  <strong>{{ user.nickname || user.phone || `用户 ${user.id}` }}</strong>
                </div>
                <span class="status-badge" :class="statusTone(user.status)">
                  {{ statusLabel(user.status) }}
                </span>
              </div>

              <p class="meta-copy">{{ user.phone || "未填写手机号" }} / {{ roleLabel(user.role) }}</p>
              <p class="meta-copy">
                认证 {{ verificationLabel(user.verificationStatus) }} / 学号 {{ user.studentId || "未登记" }}
              </p>
              <p class="meta-copy">更新于 {{ formatTime(user.updatedAt || user.createdAt) }}</p>

              <div class="inline-form-actions" style="margin-top: 12px;">
                <span v-if="isProtectedUser(user)" class="status-badge">受保护</span>
                <button
                  v-else-if="user.status === 'BANNED'"
                  :data-testid="`unban-user-${user.id}`"
                  type="button"
                  class="ghost-btn"
                  :disabled="actionLoadingId === `unban-${user.id}`"
                  @click="handleUnban(user)"
                >
                  {{ actionLoadingId === `unban-${user.id}` ? "恢复中..." : "恢复" }}
                </button>
                <button
                  v-else
                  :data-testid="`ban-user-${user.id}`"
                  type="button"
                  class="danger-btn"
                  :disabled="actionLoadingId === `ban-${user.id}`"
                  @click="handleBan(user)"
                >
                  {{ actionLoadingId === `ban-${user.id}` ? "封禁中..." : "封禁" }}
                </button>
              </div>
            </article>
          </div>
        </div>
      </article>
    </template>
  </section>
</template>

<style scoped>
.admin-users-stats {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.admin-users-stat {
  min-height: 128px;
  display: grid;
  gap: 8px;
  align-content: end;
}

.admin-users-stat__label {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.admin-users-stat strong {
  font-family: var(--cp-font-display);
  font-size: clamp(24px, 4vw, 32px);
}

.admin-users-table__identity,
.admin-user-card {
  display: grid;
  gap: 6px;
}

.admin-users-table__identity span,
.admin-user-card__eyebrow {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.admin-user-card__header {
  display: flex;
  justify-content: space-between;
  gap: var(--cp-gap-4);
  align-items: start;
}

@media (max-width: 1023px) {
  .admin-users-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .admin-users-stats {
    grid-template-columns: 1fr;
  }

  .admin-user-card__header {
    flex-direction: column;
  }
}
</style>
