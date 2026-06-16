import { beforeEach, expect, test, vi } from "vitest";
import axios from "axios";

const interceptors = vi.hoisted(() => ({
  requestHandler: null,
  responseSuccessHandler: null,
  responseErrorHandler: null,
}));

vi.mock("axios", () => ({
  default: {
    create: vi.fn(() => ({
      interceptors: {
        request: {
          use: vi.fn((handler) => {
            interceptors.requestHandler = handler;
          }),
        },
        response: {
          use: vi.fn((successHandler, errorHandler) => {
            interceptors.responseSuccessHandler = successHandler;
            interceptors.responseErrorHandler = errorHandler;
          }),
        },
      },
    })),
  },
}));

beforeEach(async () => {
  vi.clearAllMocks();
  vi.resetModules();
  window.localStorage.clear();
  interceptors.requestHandler = null;
  interceptors.responseSuccessHandler = null;
  interceptors.responseErrorHandler = null;
  await import("./http.js");
});

test("uses backend Chinese message when API payload is not successful", async () => {
  await expect(interceptors.responseSuccessHandler({
    data: {
      code: 403,
      message: "账号已被封禁",
    },
    config: {},
  })).rejects.toThrow("账号已被封禁");
});

test("uses Chinese fallback when backend response has no message", async () => {
  await expect(interceptors.responseErrorHandler({
    response: {
      status: 500,
      data: {},
    },
    config: {},
  })).rejects.toThrow("请求失败，请稍后重试");
});

test("creates one axios client for API requests", () => {
  expect(axios.create).toHaveBeenCalledWith({
    baseURL: "/api",
    timeout: 8000,
  });
});
