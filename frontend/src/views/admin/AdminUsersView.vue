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
  { key: "total", label: "All Accounts" },
  { key: "activeCount", label: "Active" },
  { key: "bannedCount", label: "Banned" },
  { key: "verifiedCount", label: "Verified" },
];

function formatTime(value) {
  if (!value) {
    return "Unknown time";
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

async function loadUsers() {
  loading.value = true;
  errorMessage.value = "";

  try {
    summary.value = await getAdminUsers();
  } catch (error) {
    errorMessage.value = error.message || "Admin user desk loading failed. Please try again.";
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
    actionMessage.value = `Account banned for ${user.nickname || user.phone || `user-${user.id}`}.`;
    await loadUsers();
  } catch (error) {
    actionError.value = error.message || "Account ban failed. Please try again.";
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
    actionMessage.value = `Account restored for ${user.nickname || user.phone || `user-${user.id}`}.`;
    await loadUsers();
  } catch (error) {
    actionError.value = error.message || "Account restore failed. Please try again.";
  } finally {
    actionLoadingId.value = "";
  }
}

onMounted(loadUsers);
</script>

<template>
  <section class="page-stack">
    <article v-if="loading" class="section-card">
      <div class="empty-state">Loading admin user desk...</div>
    </article>

    <article v-else-if="errorMessage" class="section-card">
      <div class="field-grid">
        <p class="field-error" role="alert">{{ errorMessage }}</p>
        <button type="button" class="ghost-btn" @click="loadUsers">
          Retry
        </button>
      </div>
    </article>

    <template v-else>
      <article class="section-card">
        <div class="section-header">
          <div>
            <span class="section-eyebrow">Admin User Desk</span>
            <h1 class="page-title" style="margin-top: 16px;">User status workbench</h1>
            <p class="page-subtitle" style="margin-top: 16px;">
              Review account status, keep protected admin rows visible, and ban or restore user
              accounts from one quiet ledger.
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
          Admin accounts stay protected in this phase and cannot be status-toggled from the desk.
        </p>
        <p v-if="actionMessage" class="field-hint" style="margin-top: 16px;">{{ actionMessage }}</p>
        <p v-if="actionError" class="field-error" role="alert" style="margin-top: 12px;">
          {{ actionError }}
        </p>
      </article>

      <article class="section-card">
        <div v-if="!summary.users.length" class="empty-state">
          No user accounts are available on the desk yet.
        </div>
        <div v-else>
          <table class="app-table">
            <thead>
              <tr>
                <th>User</th>
                <th>Role</th>
                <th>Verification</th>
                <th>Status</th>
                <th>Student ID</th>
                <th>Updated</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in summary.users" :key="user.id">
                <td>
                  <div class="admin-users-table__identity">
                    <strong>{{ user.nickname || user.phone || `User ${user.id}` }}</strong>
                    <span>User ID {{ user.id }} / {{ user.phone || "No phone" }}</span>
                    <span v-if="user.realName">{{ user.realName }}</span>
                  </div>
                </td>
                <td>{{ user.role || "USER" }}</td>
                <td>
                  <span class="status-badge" :class="verificationTone(user.verificationStatus)">
                    {{ user.verificationStatus || "UNVERIFIED" }}
                  </span>
                </td>
                <td>
                  <span class="status-badge" :class="statusTone(user.status)">
                    {{ user.status || "ACTIVE" }}
                  </span>
                </td>
                <td>{{ user.studentId || "Not filed" }}</td>
                <td>{{ formatTime(user.updatedAt || user.createdAt) }}</td>
                <td>
                  <div class="inline-form-actions">
                    <span v-if="isProtectedUser(user)" class="status-badge">Protected</span>
                    <button
                      v-else-if="user.status === 'BANNED'"
                      :data-testid="`unban-user-${user.id}`"
                      type="button"
                      class="ghost-btn"
                      :disabled="actionLoadingId === `unban-${user.id}`"
                      @click="handleUnban(user)"
                    >
                      {{ actionLoadingId === `unban-${user.id}` ? "Restoring..." : "Restore" }}
                    </button>
                    <button
                      v-else
                      :data-testid="`ban-user-${user.id}`"
                      type="button"
                      class="danger-btn"
                      :disabled="actionLoadingId === `ban-${user.id}`"
                      @click="handleBan(user)"
                    >
                      {{ actionLoadingId === `ban-${user.id}` ? "Banning..." : "Ban" }}
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
                  <p class="admin-user-card__eyebrow">User #{{ user.id }}</p>
                  <strong>{{ user.nickname || user.phone || `User ${user.id}` }}</strong>
                </div>
                <span class="status-badge" :class="statusTone(user.status)">
                  {{ user.status || "ACTIVE" }}
                </span>
              </div>

              <p class="meta-copy">{{ user.phone || "No phone" }} / {{ user.role || "USER" }}</p>
              <p class="meta-copy">
                Verification {{ user.verificationStatus || "UNVERIFIED" }} / Student ID {{ user.studentId || "Not filed" }}
              </p>
              <p class="meta-copy">Updated {{ formatTime(user.updatedAt || user.createdAt) }}</p>

              <div class="inline-form-actions" style="margin-top: 12px;">
                <span v-if="isProtectedUser(user)" class="status-badge">Protected</span>
                <button
                  v-else-if="user.status === 'BANNED'"
                  :data-testid="`unban-user-${user.id}`"
                  type="button"
                  class="ghost-btn"
                  :disabled="actionLoadingId === `unban-${user.id}`"
                  @click="handleUnban(user)"
                >
                  {{ actionLoadingId === `unban-${user.id}` ? "Restoring..." : "Restore" }}
                </button>
                <button
                  v-else
                  :data-testid="`ban-user-${user.id}`"
                  type="button"
                  class="danger-btn"
                  :disabled="actionLoadingId === `ban-${user.id}`"
                  @click="handleBan(user)"
                >
                  {{ actionLoadingId === `ban-${user.id}` ? "Banning..." : "Ban" }}
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
