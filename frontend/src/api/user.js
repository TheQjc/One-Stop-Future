import http from "./http.js";
import { demoUsers } from "./auth.js";

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
  const stored = readJson(USER_KEY, null);

  if (!stored?.length) {
    writeUsers(demoUsers);
    return [...demoUsers];
  }

  const users = stored.map((user) => ({
    ...demoUsers.find((item) => item.id === user.id),
    ...user,
  }));
  writeUsers(users);
  return users;
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

function updateFallbackProfile(payload) {
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

export async function getProfile() {
  if (preferMock) {
    return getFallbackProfile();
  }

  const { data } = await http.get("/users/me", { skipAuthRedirect: true });
  return data.data;
}

export async function updateProfile(payload) {
  if (preferMock) {
    return updateFallbackProfile(payload);
  }

  const { data } = await http.put("/users/me", payload);
  return data.data;
}
