import { defineStore } from "pinia";
import { login, logout, register } from "../api/auth.js";
import { changePassword, getProfile, updateProfile } from "../api/user.js";

const TOKEN_KEY = "one-stop-future-token";
const PROFILE_KEY = "one-stop-future-profile";

function readJson(key, fallback) {
  try {
    const raw = window.localStorage.getItem(key);
    return raw ? JSON.parse(raw) : fallback;
  } catch (error) {
    return fallback;
  }
}

function normalizeProfile(payload) {
  if (!payload) {
    return null;
  }

  const identifier = payload.id ?? payload.userId ?? null;

  return {
    id: identifier,
    userId: identifier,
    phone: payload.phone ?? payload.username ?? "",
    username: payload.username ?? payload.phone ?? "",
    nickname: payload.nickname ?? payload.realName ?? payload.username ?? payload.phone ?? "",
    realName: payload.realName ?? "",
    role: payload.role ?? "GUEST",
    status: payload.status ?? "ACTIVE",
    verificationStatus: payload.verificationStatus ?? "UNVERIFIED",
    studentId: payload.studentId ?? null,
    unreadNotificationCount: payload.unreadNotificationCount ?? 0,
    email: payload.email ?? "",
    bio: payload.bio ?? "",
  };
}

export const useUserStore = defineStore("user", {
  state: () => ({
    token: window.localStorage.getItem(TOKEN_KEY) || "",
    profile: readJson(PROFILE_KEY, null),
    loading: false,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
    isAdmin: (state) => state.profile?.role === "ADMIN",
    isVerified: (state) => state.profile?.verificationStatus === "VERIFIED",
    unreadCount: (state) => state.profile?.unreadNotificationCount || 0,
    role: (state) => state.profile?.role || "GUEST",
    roleLabel: (state) => {
      const roleMap = {
        USER: "普通用户",
        ADMIN: "管理员",
        GUEST: "游客",
      };
      return roleMap[state.profile?.role || "GUEST"];
    },
    canManageNotices: (state) => state.profile?.role === "ADMIN",
  },
  actions: {
    setAuth(payload) {
      this.token = payload.token;
      this.profile = normalizeProfile(payload.profile || payload);
      window.localStorage.setItem(TOKEN_KEY, payload.token);
      window.localStorage.setItem(PROFILE_KEY, JSON.stringify(this.profile));
    },
    clearAuth() {
      this.token = "";
      this.profile = null;
      window.localStorage.removeItem(TOKEN_KEY);
      window.localStorage.removeItem(PROFILE_KEY);
    },
    async login(form) {
      this.loading = true;
      try {
        const auth = await login(form);
        this.setAuth(auth);
        return auth;
      } finally {
        this.loading = false;
      }
    },
    async register(form) {
      this.loading = true;
      try {
        const auth = await register(form);
        this.setAuth(auth);
        return auth;
      } finally {
        this.loading = false;
      }
    },
    async fetchProfile() {
      if (!this.token) {
        return null;
      }

      const profile = normalizeProfile(await getProfile());
      this.profile = profile;
      window.localStorage.setItem(PROFILE_KEY, JSON.stringify(profile));
      return this.profile;
    },
    async saveProfile(form) {
      const profile = normalizeProfile(await updateProfile(form));
      this.profile = { ...this.profile, ...profile };
      window.localStorage.setItem(PROFILE_KEY, JSON.stringify(this.profile));
      return this.profile;
    },
    async updatePassword(form) {
      return changePassword(form);
    },
    async logout() {
      await logout();
      this.clearAuth();
    },
  },
});
