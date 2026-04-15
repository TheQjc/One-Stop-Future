import http from "./http.js";

const preferMock = import.meta.env.MODE === "test";
const USER_KEY = "one-stop-future-demo-users";
const CODE_KEY = "one-stop-future-demo-codes";

const demoUsers = [
  {
    id: 1,
    phone: "13800000000",
    nickname: "PlatformAdmin",
    role: "ADMIN",
    status: "ACTIVE",
    verificationStatus: "UNVERIFIED",
  },
  {
    id: 2,
    phone: "13800000001",
    nickname: "NormalUser",
    role: "USER",
    status: "ACTIVE",
    verificationStatus: "UNVERIFIED",
  },
  {
    id: 3,
    phone: "13800000002",
    nickname: "VerifiedUser",
    role: "USER",
    status: "ACTIVE",
    verificationStatus: "VERIFIED",
  },
];

function readJson(key, fallback) {
  const raw = window.localStorage.getItem(key);
  return raw ? JSON.parse(raw) : fallback;
}

function writeJson(key, value) {
  window.localStorage.setItem(key, JSON.stringify(value));
}

function readUsers() {
  const stored = readJson(USER_KEY, null);

  if (stored) {
    return stored;
  }

  writeJson(USER_KEY, demoUsers);
  return [...demoUsers];
}

function writeUsers(users) {
  writeJson(USER_KEY, users);
}

function readCodes() {
  return readJson(CODE_KEY, []);
}

function writeCodes(codes) {
  writeJson(CODE_KEY, codes);
}

function buildAuthPayload(user) {
  return {
    token: `demo-token-${user.phone}`,
    userId: user.id,
    phone: user.phone,
    nickname: user.nickname,
    role: user.role,
    status: user.status,
    verificationStatus: user.verificationStatus,
  };
}

function normalizeAuthPayload(payload) {
  if (!payload?.token) {
    throw new Error("登录响应格式不正确");
  }

  return {
    token: payload.token,
    userId: payload.userId ?? payload.id,
    phone: payload.phone,
    nickname: payload.nickname,
    role: payload.role,
    status: payload.status,
    verificationStatus: payload.verificationStatus,
  };
}

function consumeCode(phone, purpose, verificationCode) {
  const codes = readCodes();
  const index = codes.findIndex(
    (item) =>
      item.phone === phone
      && item.purpose === purpose
      && item.code === verificationCode,
  );

  if (index < 0) {
    throw new Error("手机号或验证码不正确");
  }

  codes.splice(index, 1);
  writeCodes(codes);
}

export async function sendCode(payload) {
  if (preferMock) {
    const debugCode = "123456";
    const codes = readCodes().filter(
      (item) => !(item.phone === payload.phone && item.purpose === payload.purpose),
    );

    codes.push({
      phone: payload.phone,
      purpose: payload.purpose,
      code: debugCode,
    });
    writeCodes(codes);

    return {
      purpose: payload.purpose,
      debugCode,
      expiresInSeconds: 300,
    };
  }

  const { data } = await http.post("/auth/codes/send", payload);
  return data.data;
}

export async function login(payload) {
  if (preferMock) {
    consumeCode(payload.phone, "LOGIN", payload.verificationCode);
    const matched = readUsers().find((user) => user.phone === payload.phone);

    if (!matched) {
      throw new Error("手机号或验证码不正确");
    }

    return buildAuthPayload(matched);
  }

  const { data } = await http.post("/auth/login", payload);
  return normalizeAuthPayload(data.data);
}

export async function register(payload) {
  if (preferMock) {
    consumeCode(payload.phone, "REGISTER", payload.verificationCode);
    const users = readUsers();
    const exists = users.some((user) => user.phone === payload.phone);

    if (exists) {
      throw new Error("手机号已注册");
    }

    const created = {
      id: Date.now(),
      phone: payload.phone,
      nickname: payload.nickname,
      role: "USER",
      status: "ACTIVE",
      verificationStatus: "UNVERIFIED",
    };

    users.push(created);
    writeUsers(users);
    return buildAuthPayload(created);
  }

  const { data } = await http.post("/auth/register", payload);
  return normalizeAuthPayload(data.data);
}

export async function logout() {
  if (preferMock) {
    return true;
  }

  const { data } = await http.post("/auth/logout");
  return data.data;
}
