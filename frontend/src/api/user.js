import http from "./http.js";

const preferMock = import.meta.env.MODE === "test";
const PROFILE_KEY = "one-stop-future-profile";

function readUsers() {
  const raw = window.localStorage.getItem("campus-demo-users");
  return raw ? JSON.parse(raw) : [];
}

function writeUsers(users) {
  window.localStorage.setItem("campus-demo-users", JSON.stringify(users));
}

function currentUsername() {
  const raw = window.localStorage.getItem(PROFILE_KEY);

  if (!raw) {
    return "";
  }

  const profile = JSON.parse(raw);
  return profile.username || profile.phone || "";
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
    email: user.email || "",
    bio: user.bio || "",
  };
}

export async function getProfile() {
  if (preferMock) {
    const matched = readUsers().find((item) => item.username === currentUsername());

    if (!matched) {
      throw new Error("未找到当前用户信息");
    }

    return toProfile(matched);
  }

  try {
    const { data } = await http.get("/users/me");
    return data.data;
  } catch (error) {
    const matched = readUsers().find((item) => item.username === currentUsername());

    if (!matched) {
      throw new Error("未找到当前用户信息");
    }

    return toProfile(matched);
  }
}

export async function updateProfile(payload) {
  if (preferMock) {
    const users = readUsers();
    const index = users.findIndex((item) => item.username === currentUsername());

    if (index < 0) {
      throw new Error("当前用户不存在");
    }

    users[index] = { ...users[index], ...payload };
    writeUsers(users);
    return toProfile(users[index]);
  }

  try {
    const { data } = await http.put("/users/me", payload);
    return data.data;
  } catch (error) {
    const users = readUsers();
    const index = users.findIndex((item) => item.username === currentUsername());

    if (index < 0) {
      throw new Error("当前用户不存在");
    }

    users[index] = { ...users[index], ...payload };
    writeUsers(users);
    return toProfile(users[index]);
  }
}

export async function changePassword(payload) {
  if (preferMock) {
    const users = readUsers();
    const index = users.findIndex((item) => item.username === currentUsername());

    if (index < 0) {
      throw new Error("当前用户不存在");
    }

    if (users[index].password !== payload.oldPassword) {
      throw new Error("旧密码不正确");
    }

    users[index].password = payload.newPassword;
    writeUsers(users);
    return true;
  }

  const { data } = await http.put("/users/me/password", payload);
  return data.data;
}
