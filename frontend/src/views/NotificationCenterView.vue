<script setup>
import { computed, onMounted, ref } from "vue";
import { getNotifications, markAllNotificationsRead, markNotificationRead } from "../api/notification.js";
import { useUserStore } from "../stores/user.js";

const userStore = useUserStore();

const loading = ref(true);
const errorMessage = ref("");
const activeFilter = ref("ALL");
const markingAll = ref(false);
const processingIds = ref([]);
const notificationState = ref({
  unreadCount: 0,
  total: 0,
  notifications: [],
});

const filterOptions = [
  { key: "ALL", label: "全部通知" },
  { key: "UNREAD", label: "仅看未读" },
];

function isProcessing(id) {
  return processingIds.value.includes(id);
}

function setProcessing(id, value) {
  if (value) {
    processingIds.value = [...new Set([...processingIds.value, id])];
    return;
  }

  processingIds.value = processingIds.value.filter((item) => item !== id);
}

function typeLabel(type) {
  const labelMap = {
    WELCOME: "欢迎",
    COMMUNITY_REPLY_RECEIVED: "评论回复",
    SYSTEM: "系统",
    VERIFICATION_APPROVED: "认证通过",
    VERIFICATION_REJECTED: "认证驳回",
  };

  return labelMap[type] || "通知";
}

function formatTime(value) {
  if (!value) {
    return "刚刚";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "刚刚";
  }

  return new Intl.DateTimeFormat("zh-CN", {
    month: "numeric",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

const filteredNotifications = computed(() => {
  if (activeFilter.value === "UNREAD") {
    return notificationState.value.notifications.filter((item) => !item.read);
  }

  return notificationState.value.notifications;
});

async function loadNotifications() {
  loading.value = true;
  errorMessage.value = "";

  try {
    const data = await getNotifications();
    notificationState.value = data;
    userStore.setUnreadNotificationCount(data.unreadCount);
  } catch (error) {
    errorMessage.value = error.message || "通知中心加载失败，请稍后重试。";
  } finally {
    loading.value = false;
  }
}

async function handleMarkRead(notification) {
  if (notification.read || isProcessing(notification.id)) {
    return;
  }

  setProcessing(notification.id, true);

  try {
    await markNotificationRead(notification.id);

    const notifications = notificationState.value.notifications.map((item) => (
      item.id === notification.id
        ? { ...item, read: true, readAt: item.readAt || new Date().toISOString() }
        : item
    ));
    const unreadCount = notifications.filter((item) => !item.read).length;

    notificationState.value = {
      unreadCount,
      total: notifications.length,
      notifications,
    };
    userStore.setUnreadNotificationCount(unreadCount);
  } catch (error) {
    errorMessage.value = error.message || "通知状态更新失败，请稍后重试。";
  } finally {
    setProcessing(notification.id, false);
  }
}

async function handleMarkAllRead() {
  if (markingAll.value || notificationState.value.unreadCount === 0) {
    return;
  }

  markingAll.value = true;

  try {
    await markAllNotificationsRead();

    const notifications = notificationState.value.notifications.map((item) => ({
      ...item,
      read: true,
      readAt: item.readAt || new Date().toISOString(),
    }));

    notificationState.value = {
      unreadCount: 0,
      total: notifications.length,
      notifications,
    };
    userStore.setUnreadNotificationCount(0);
  } catch (error) {
    errorMessage.value = error.message || "全部已读操作失败，请稍后重试。";
  } finally {
    markingAll.value = false;
  }
}

onMounted(loadNotifications);
</script>

<template>
  <section class="page-stack">
    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">消息工作台</span>
          <h1 class="page-title" style="margin-top: 16px;">通知中心</h1>
          <p class="page-subtitle" style="margin-top: 16px;">
            欢迎通知、认证结果和后续平台提醒都会回到这里，方便集中处理未读消息与审核反馈。
          </p>
        </div>
      </div>

      <div class="stats-grid">
        <article class="panel-card summary-card">
          <p class="summary-card__label">未读通知</p>
          <strong>{{ notificationState.unreadCount }}</strong>
        </article>
        <article class="panel-card summary-card">
          <p class="summary-card__label">通知总数</p>
          <strong>{{ notificationState.total }}</strong>
        </article>
      </div>

      <div class="toolbar">
        <div class="filter-row" role="tablist" aria-label="通知筛选">
          <button
            v-for="option in filterOptions"
            :key="option.key"
            type="button"
            class="filter-chip"
            :class="{ 'filter-chip--active': activeFilter === option.key }"
            @click="activeFilter = option.key"
          >
            {{ option.label }}
          </button>
        </div>

        <button
          type="button"
          class="ghost-btn"
          :disabled="markingAll || notificationState.unreadCount === 0"
          @click="handleMarkAllRead"
        >
          {{ markingAll ? "正在处理..." : "全部标记为已读" }}
        </button>
      </div>

      <div v-if="loading" class="empty-state">正在同步通知列表...</div>
      <div v-else-if="errorMessage" class="field-grid">
        <p class="field-error" role="alert">{{ errorMessage }}</p>
        <button type="button" class="ghost-btn" @click="loadNotifications">
          重新加载
        </button>
      </div>
      <div v-else-if="filteredNotifications.length === 0" class="empty-state">
        当前筛选条件下没有通知。
      </div>
      <div v-else class="notification-list">
        <article
          v-for="notification in filteredNotifications"
          :key="notification.id"
          class="notification-card"
          :class="{ 'notification-card--read': notification.read }"
        >
          <div class="notification-card__main">
            <div class="notification-card__topline">
              <span class="notification-card__type">{{ typeLabel(notification.type) }}</span>
              <span class="notification-card__time">{{ formatTime(notification.createdAt) }}</span>
            </div>
            <h2 class="notification-card__title">{{ notification.title }}</h2>
            <p class="meta-copy">{{ notification.content }}</p>
          </div>

          <div class="notification-card__aside">
            <span class="status-badge" :class="notification.read ? 'approved' : 'pending'">
              {{ notification.read ? "已读" : "未读" }}
            </span>
            <button
              v-if="!notification.read"
              type="button"
              class="app-link"
              :disabled="isProcessing(notification.id)"
              @click="handleMarkRead(notification)"
            >
              {{ isProcessing(notification.id) ? "正在处理..." : "标记已读" }}
            </button>
          </div>
        </article>
      </div>
    </article>
  </section>
</template>

<style scoped>
.summary-card {
  min-height: 132px;
  display: grid;
  gap: var(--cp-gap-2);
  align-content: end;
}

.summary-card__label {
  margin: 0;
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.summary-card strong {
  font-size: 30px;
  font-family: var(--cp-font-display);
}

.toolbar {
  margin-top: 24px;
  display: flex;
  flex-wrap: wrap;
  gap: var(--cp-gap-3);
  justify-content: space-between;
  align-items: center;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cp-gap-2);
}

.filter-chip {
  min-height: var(--cp-touch-height);
  padding: 0 16px;
  border-radius: var(--cp-radius-pill);
  border: 1px solid var(--cp-line-strong);
  background: transparent;
  color: var(--cp-ink-soft);
  cursor: pointer;
  transition:
    transform var(--cp-transition),
    background-color var(--cp-transition),
    border-color var(--cp-transition),
    color var(--cp-transition);
}

.filter-chip:hover {
  transform: translateY(-1px);
}

.filter-chip--active {
  border-color: rgba(197, 79, 45, 0.22);
  background: var(--cp-accent-soft);
  color: var(--cp-accent-deep);
}

.notification-list {
  margin-top: 24px;
  display: grid;
  gap: var(--cp-gap-4);
}

.notification-card {
  display: flex;
  justify-content: space-between;
  gap: var(--cp-gap-4);
  padding: 22px;
  border-radius: var(--cp-radius-md);
  border: 1px solid var(--cp-line);
  background: rgba(255, 255, 255, 0.76);
}

.notification-card--read {
  opacity: 0.76;
}

.notification-card__main {
  display: grid;
  gap: var(--cp-gap-3);
}

.notification-card__topline {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cp-gap-3);
  align-items: center;
}

.notification-card__type {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: var(--cp-radius-pill);
  background: rgba(24, 38, 63, 0.08);
  color: var(--cp-ink);
  font-size: var(--cp-text-sm);
}

.notification-card__time {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.notification-card__title {
  margin: 0;
  font-size: 28px;
  font-family: var(--cp-font-display);
  line-height: 1.14;
}

.notification-card__aside {
  display: grid;
  align-content: start;
  justify-items: end;
  gap: 12px;
}

@media (max-width: 767px) {
  .toolbar,
  .notification-card {
    flex-direction: column;
  }

  .notification-card__aside {
    justify-items: start;
  }
}
</style>
