import http from "./http.js";

const preferMock = import.meta.env.MODE === "test";

const demoUsers = [
  {
    id: 1,
    username: "student01",
    password: "secret123",
    realName: "学生演示账号",
    role: "STUDENT",
    email: "student@example.com",
    bio: "关注课程提醒与校园服务。",
  },
  {
    id: 2,
    username: "teacher01",
    password: "secret123",
    realName: "教师演示账号",
    role: "TEACHER",
    email: "teacher@example.com",
    bio: "负责通知发布与课程教学安排。",
  },
  {
    id: 3,
    username: "admin01",
    password: "secret123",
    realName: "管理员演示账号",
    role: "ADMIN",
    email: "admin@example.com",
    bio: "负责全站审核与配置维护。",
  },
];

function readUsers() {
  const raw = window.localStorage.getItem("campus-demo-users");

  if (!raw) {
    window.localStorage.setItem("campus-demo-users", JSON.stringify(demoUsers));
    return [...demoUsers];
  }

  return JSON.parse(raw);
}

function writeUsers(users) {
  window.localStorage.setItem("campus-demo-users", JSON.stringify(users));
}

function buildAuthPayload(user) {
  return {
    token: `demo-token-${user.username}`,
    profile: {
      id: user.id,
      username: user.username,
      realName: user.realName,
      role: user.role,
      email: user.email,
      bio: user.bio || "",
    },
  };
}

function normalizeAuthPayload(payload) {
  const profile = payload?.profile || payload?.user;

  if (!payload?.token || !profile) {
    throw new Error("登录响应格式不正确");
  }

  return {
    token: payload.token,
    profile: {
      id: profile.id,
      username: profile.username,
      realName: profile.realName,
      role: profile.role,
      email: profile.email,
      bio: profile.bio || "",
    },
  };
}

export async function login(payload) {
  if (preferMock) {
    const users = readUsers();
    const matched = users.find(
      (user) =>
        user.username === payload.username && user.password === payload.password,
    );

    if (!matched) {
      throw new Error("账号或密码不正确");
    }

    return buildAuthPayload(matched);
  }

  try {
    const { data } = await http.post("/auth/login", payload);
    return normalizeAuthPayload(data.data);
  } catch (error) {
    const users = readUsers();
    const matched = users.find(
      (user) =>
        user.username === payload.username && user.password === payload.password,
    );

    if (!matched) {
      throw new Error("账号或密码不正确");
    }

    return buildAuthPayload(matched);
  }
}

export async function register(payload) {
  if (preferMock) {
    const users = readUsers();
    const exists = users.some((user) => user.username === payload.username);

    if (exists) {
      throw new Error("用户名已存在");
    }

    const created = {
      id: Date.now(),
      username: payload.username,
      password: payload.password,
      realName: payload.realName || payload.username,
      role: payload.role,
      email: payload.email || "",
      bio: payload.role === "TEACHER" ? "新注册教师账号，待完善个人简介。" : "新注册学生账号。",
    };

    users.push(created);
    writeUsers(users);
    return buildAuthPayload(created);
  }

  try {
    await http.post("/auth/register", payload);
    return login({
      username: payload.username,
      password: payload.password,
    });
  } catch (error) {
    const users = readUsers();
    const exists = users.some((user) => user.username === payload.username);

    if (exists) {
      throw new Error("用户名已存在");
    }

    const created = {
      id: Date.now(),
      username: payload.username,
      password: payload.password,
      realName: payload.realName || payload.username,
      role: payload.role,
      email: payload.email || "",
      bio: payload.role === "TEACHER" ? "新注册教师账号，待完善个人简介。" : "新注册学生账号。",
    };

    users.push(created);
    writeUsers(users);
    return buildAuthPayload(created);
  }
}

export async function logout() {
  if (preferMock) {
    return true;
  }

  try {
    await http.post("/auth/logout");
  } catch (error) {
    return true;
  }

  return true;
}
