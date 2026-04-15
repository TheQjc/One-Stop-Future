import http from "./http.js";

const preferMock = import.meta.env.MODE === "test";
const NOTIFICATION_KEY = "one-stop-future-demo-notifications";
const USER_KEY = "one-stop-future-demo-users";
const PROFILE_KEY = "one-stop-future-profile";

const defaultNotifications = [
  {
    id: 1001,
    userId: 2,
    type: "WELCOME",
    title: "欢迎进入平台",
    content: "现在可以先完善个人资料，再提交学生认证申请。",
    read: false,
    createdAt: "2026-04-15T08:00:00",
    readAt: null,
  },
  {
    id: 1002,
    userId: 2,
    type: "SYSTEM",
    title: "首页聚合展示已开启",
    content: "就业、考研、留学三条主线已在首页集中展示。",
    read: true,
    createdAt: "2026-04-14T18:00:00",
    readAt: "2026-04-14T19:00:00",
  },
  {
    id: 1003,
    userId: 3,
    type: "VERIFICATION_APPROVED",
    title: "认证已通过",
    content: "你已经完成学生身份认证，可以继续使用后续功能。",
    read: false,
    createdAt: "2026-04-15T09:30:00",
    readAt: null,
  },
];

function readJson(key, fallback) {
  try {
    const raw = window.localStorage.getItem(key);
    return raw ? JSON.parse(raw) : fallback;
  } catch (error) {
    return fallback;
  }
}

function writeJson(key, value) {
  window.localStorage.setItem(key, JSON.stringify(value));
}

function readProfile() {
  return readJson(PROFILE_KEY, null);
}

function currentUserId() {
  const profile = readProfile();
  return profile?.id ?? profile?.userId ?? null;
}

function readNotifications() {
  const stored = readJson(NOTIFICATION_KEY, null);

  if (stored) {
    return stored;
  }

  writeJson(NOTIFICATION_KEY, defaultNotifications);
  return [...defaultNotifications];
}

function writeNotifications(notifications) {
  writeJson(NOTIFICATION_KEY, notifications);
}

function syncUnreadCount(unreadCount) {
  const profile = readProfile();

  if (profile) {
    writeJson(PROFILE_KEY, {
      ...profile,
      unreadNotificationCount: unreadCount,
    });
  }

  const users = readJson(USER_KEY, []);
  const index = users.findIndex((item) => item.id === currentUserId());

  if (index >= 0) {
    users[index] = {
      ...users[index],
      unreadNotificationCount: unreadCount,
    };
    writeJson(USER_KEY, users);
  }
}

function listCurrentUserNotifications() {
  const userId = currentUserId();

  if (!userId) {
    return [];
  }

  return readNotifications()
    .filter((item) => item.userId === userId)
    .sort((left, right) => new Date(right.createdAt) - new Date(left.createdAt));
}

export async function getNotifications() {
  if (preferMock) {
    const notifications = listCurrentUserNotifications();
    const unreadCount = notifications.filter((item) => !item.read).length;

    syncUnreadCount(unreadCount);

    return {
      unreadCount,
      total: notifications.length,
      notifications,
    };
  }

  const { data } = await http.get("/notifications");
  return data.data;
}

export async function markNotificationRead(id) {
  if (preferMock) {
    const notifications = readNotifications();
    const index = notifications.findIndex((item) => item.id === id && item.userId === currentUserId());

    if (index < 0) {
      throw new Error("通知不存在");
    }

    notifications[index] = {
      ...notifications[index],
      read: true,
      readAt: notifications[index].readAt || new Date().toISOString(),
    };

    writeNotifications(notifications);
    syncUnreadCount(listCurrentUserNotifications().filter((item) => !item.read).length);
    return true;
  }

  const { data } = await http.post(`/notifications/${id}/read`);
  return data.data;
}

export async function markAllNotificationsRead() {
  if (preferMock) {
    const notifications = readNotifications();
    const userId = currentUserId();
    let updatedCount = 0;

    const nextNotifications = notifications.map((item) => {
      if (item.userId !== userId || item.read) {
        return item;
      }

      updatedCount += 1;
      return {
        ...item,
        read: true,
        readAt: item.readAt || new Date().toISOString(),
      };
    });

    writeNotifications(nextNotifications);
    syncUnreadCount(0);
    return { updatedCount };
  }

  const { data } = await http.post("/notifications/read-all");
  return data.data;
}
