import { createPinia, setActivePinia } from "pinia";
import { beforeEach, expect, test, vi } from "vitest";
import { useUserStore } from "./user.js";
import { getProfile } from "../api/user.js";

vi.mock("../api/user.js", () => ({
  getProfile: vi.fn(),
  updateProfile: vi.fn(),
}));

vi.mock("../api/auth.js", () => ({
  login: vi.fn(),
  logout: vi.fn(),
  register: vi.fn(),
}));

beforeEach(() => {
  vi.clearAllMocks();
  window.localStorage.clear();
  setActivePinia(createPinia());
});

test("ensureSessionFresh clears a stale cached session when profile validation fails", async () => {
  const userStore = useUserStore();
  const error = new Error("Unauthorized");

  userStore.setAuth({
    token: "expired-token",
    profile: {
      id: 2,
      phone: "13800000001",
      nickname: "Cached User",
      role: "USER",
    },
  });
  userStore.sessionChecked = false;
  getProfile.mockRejectedValue(error);

  await expect(userStore.ensureSessionFresh()).rejects.toThrow("Unauthorized");

  expect(userStore.isAuthenticated).toBe(false);
  expect(userStore.profile).toBeNull();
  expect(window.localStorage.getItem("one-stop-future-token")).toBeNull();
  expect(window.localStorage.getItem("one-stop-future-profile")).toBeNull();
});
