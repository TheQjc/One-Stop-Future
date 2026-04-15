import { defineStore } from "pinia";
import { login, logout, register } from "../api/auth.js";
import { getProfile, updateProfile } from "../api/user.js";

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
    canReviewVerifications: (state) => ["ADMIN", "TEACHER"].includes(state.profile?.role),
    isVerified: (state) => state.profile?.verificationStatus === "VERIFIED",
    unreadCount: (state) => state.profile?.unreadNotificationCount || 0,
    role: (state) => state.profile?.role || "GUEST",
    roleLabel: (state) => {
      const roleMap = {
        USER: "普通用户",
        ADMIN: "管理员",
        TEACHER: "教师",
        GUEST: "访客",
      };

      return roleMap[state.profile?.role || "GUEST"] || "访客";
    },
    canManageNotices: (state) => state.profile?.role === "ADMIN",
  },
  actions: {
    persistProfile(payload) {
      this.profile = payload ? normalizeProfile(payload) : null;

      if (this.profile) {
        window.localStorage.setItem(PROFILE_KEY, JSON.stringify(this.profile));
      } else {
        window.localStorage.removeItem(PROFILE_KEY);
      }

      return this.profile;
    },
    mergeProfile(payload) {
      if (!payload) {
        return this.profile;
      }

      return this.persistProfile({
        ...(this.profile || {}),
        ...payload,
      });
    },
    setAuth(payload) {
      this.token = payload.token;
      window.localStorage.setItem(TOKEN_KEY, payload.token);
      this.persistProfile(payload.profile || payload);
    },
    clearAuth() {
      this.token = "";
      window.localStorage.removeItem(TOKEN_KEY);
      this.persistProfile(null);
    },
    applyHomeSummary(summary) {
      if (!summary?.identity) {
        return null;
      }

      return this.mergeProfile({
        ...summary.identity,
        verificationStatus:
          summary.verificationStatus
          ?? summary.identity.verificationStatus
          ?? this.profile?.verificationStatus,
        unreadNotificationCount:
          summary.unreadNotificationCount
          ?? this.profile?.unreadNotificationCount
          ?? 0,
      });
    },
    setUnreadNotificationCount(count) {
      return this.mergeProfile({
        unreadNotificationCount: count,
      });
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

      return this.mergeProfile(await getProfile());
    },
    async saveProfile(form) {
      return this.mergeProfile(await updateProfile(form));
    },
    async logout() {
      await logout();
      this.clearAuth();
    },
  },
});
