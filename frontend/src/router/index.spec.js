import { beforeEach, expect, test, vi } from "vitest";
import router from "./index.js";
import { getProfile } from "../api/user.js";
import { useUserStore } from "../stores/user.js";
import pinia from "../stores/pinia.js";

vi.mock("../api/user.js", () => ({
  getProfile: vi.fn(),
  updateProfile: vi.fn(),
}));

vi.mock("../api/auth.js", () => ({
  login: vi.fn(),
  logout: vi.fn(),
  register: vi.fn(),
}));

beforeEach(async () => {
  vi.clearAllMocks();
  window.localStorage.clear();
  const userStore = useUserStore(pinia);
  userStore.clearAuth();
  await router.push("/");
  await router.isReady();
});

test("navigation validates cached auth even when a cached profile exists", async () => {
  const userStore = useUserStore(pinia);

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
  getProfile.mockRejectedValue(new Error("Unauthorized"));

  await router.push("/analytics");

  expect(getProfile).toHaveBeenCalledTimes(1);
  expect(userStore.isAuthenticated).toBe(false);
  expect(router.currentRoute.value.name).toBe("analytics");
});
