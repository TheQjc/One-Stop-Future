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

export async function submitVerification(payload) {
  if (preferMock) {
    const users = readUsers();
    const index = users.findIndex((item) => (item.phone || item.username) === currentPhone());

    if (index < 0) {
      throw new Error("当前用户不存在");
    }

    users[index] = {
      ...users[index],
      realName: payload.realName,
      studentId: payload.studentId,
      verificationStatus: "PENDING",
    };

    writeUsers(users);
    return toProfile(users[index]);
  }

  const { data } = await http.post("/verifications", payload);
  return data.data;
}
