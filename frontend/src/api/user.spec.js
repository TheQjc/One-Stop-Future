import { beforeEach, expect, test, vi } from "vitest";

vi.mock("./http.js", () => ({
  default: {
    get: vi.fn(),
    put: vi.fn(),
  },
}));

beforeEach(() => {
  vi.clearAllMocks();
  vi.resetModules();
  vi.unstubAllEnvs();
  window.localStorage.clear();
});

test("getProfile propagates real API failures instead of returning cached demo data", async () => {
  vi.stubEnv("MODE", "development");
  const { default: http } = await import("./http.js");
  const { getProfile } = await import("./user.js");
  window.localStorage.setItem("one-stop-future-profile", JSON.stringify({
    id: 2,
    phone: "13800000001",
    nickname: "Cached User",
    role: "USER",
  }));
  const error = new Error("Unauthorized");

  http.get.mockRejectedValue(error);

  await expect(getProfile()).rejects.toThrow("Unauthorized");
  expect(http.get).toHaveBeenCalledWith("/users/me", { skipAuthRedirect: true });
});
