import http from "./http.js";

const preferMock = import.meta.env.MODE === "test";
const USER_KEY = "one-stop-future-demo-users";
const PROFILE_KEY = "one-stop-future-profile";

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

function readUsers() {
  return readJson(USER_KEY, []);
}

function writeUsers(users) {
  writeJson(USER_KEY, users);
}

function readProfile() {
  return readJson(PROFILE_KEY, null);
}

function currentPhone() {
  const profile = readProfile();
  return profile?.phone || profile?.username || "";
}

function findCurrentUserIndex(users) {
  return users.findIndex((item) => (item.phone || item.username) === currentPhone());
}

function toProfile(user) {
  return {
    id: user.id,
    userId: user.id,
    phone: user.phone || user.username || "",
    username: user.username || user.phone || "",
    nickname: user.nickname || user.realName || user.username || user.phone || "",
    realName: user.realName || "",
    role: user.role,
    status: user.status || "ACTIVE",
    verificationStatus: user.verificationStatus || "UNVERIFIED",
    studentId: user.studentId || null,
    unreadNotificationCount: user.unreadNotificationCount || 0,
  };
}

function getFallbackProfile() {
  const users = readUsers();
  const index = findCurrentUserIndex(users);

  if (index < 0) {
    throw new Error("未找到当前用户信息");
  }

  return toProfile(users[index]);
}

export async function getProfile() {
  if (preferMock) {
    return getFallbackProfile();
  }

  try {
    const { data } = await http.get("/users/me");
    return data.data;
  } catch (error) {
    return getFallbackProfile();
  }
}

export async function updateProfile(payload) {
  if (preferMock) {
    const users = readUsers();
    const index = findCurrentUserIndex(users);

    if (index < 0) {
      throw new Error("当前用户不存在");
    }

    users[index] = {
      ...users[index],
      nickname: payload.nickname ?? users[index].nickname,
      realName: payload.realName ?? users[index].realName,
    };

    writeUsers(users);
    return toProfile(users[index]);
  }

  try {
    const { data } = await http.put("/users/me", payload);
    return data.data;
  } catch (error) {
    const users = readUsers();
    const index = findCurrentUserIndex(users);

    if (index < 0) {
      throw new Error("当前用户不存在");
    }

    users[index] = {
      ...users[index],
      nickname: payload.nickname ?? users[index].nickname,
      realName: payload.realName ?? users[index].realName,
    };

    writeUsers(users);
    return toProfile(users[index]);
  }
}
