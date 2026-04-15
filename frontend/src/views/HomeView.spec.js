import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { beforeEach, expect, test, vi } from "vitest";
import HomeView from "./HomeView.vue";
import { useUserStore } from "../stores/user.js";
import { getHomeSummary } from "../api/home.js";

vi.mock("../api/home.js", () => ({
  getHomeSummary: vi.fn(),
}));

const guestSummary = {
  viewerType: "GUEST",
  identity: null,
  roleLabel: "Guest",
  verificationStatus: null,
  unreadNotificationCount: 0,
  todos: ["Sign in to unlock profile, verification, and notifications."],
  entries: [
    { code: "jobs", title: "Jobs", path: "/jobs", enabled: true, badge: "COMING_SOON" },
    { code: "resources", title: "Resources", path: "/resources", enabled: true, badge: "COMING_SOON" },
    { code: "assessment", title: "Assessment", path: "/assessment", enabled: false, badge: "LOGIN_REQUIRED" },
  ],
  latestNotifications: [],
};

const authenticatedSummary = {
  viewerType: "USER",
  identity: {
    userId: 2,
    phone: "13800000001",
    nickname: "已登录同学",
    role: "USER",
    verificationStatus: "PENDING",
  },
  roleLabel: "User",
  verificationStatus: "PENDING",
  unreadNotificationCount: 3,
  todos: [
    "Student verification is under review.",
    "You have 3 unread notifications.",
  ],
  entries: [
    { code: "jobs", title: "Jobs", path: "/jobs", enabled: true, badge: "COMING_SOON" },
    { code: "resources", title: "Resources", path: "/resources", enabled: true, badge: "COMING_SOON" },
    { code: "assessment", title: "Assessment", path: "/assessment", enabled: true, badge: "COMING_SOON" },
  ],
  latestNotifications: [
    {
      id: 1,
      type: "SYSTEM",
      title: "认证状态更新",
      content: "你的认证申请正在审核中。",
      read: false,
      createdAt: "2026-04-15T08:30:00",
    },
  ],
};

beforeEach(() => {
  vi.clearAllMocks();
  window.localStorage.clear();
});

test("renders guest aggregation home", async () => {
  setActivePinia(createPinia());
  getHomeSummary.mockResolvedValue(guestSummary);

  const wrapper = mount(HomeView, {
    global: {
      stubs: {
        RouterLink: {
          props: ["to"],
          template: "<a :data-to='to'><slot /></a>",
        },
      },
    },
  });

  await flushPromises();

  expect(wrapper.text()).toContain("就业");
  expect(wrapper.text()).toContain("考研");
  expect(wrapper.text()).toContain("留学");
  expect(wrapper.text()).toContain("登录后即可解锁个人资料");
});

test("hydrates authenticated summary into store", async () => {
  setActivePinia(createPinia());
  const userStore = useUserStore();

  userStore.token = "demo-token";
  userStore.profile = {
    id: 2,
    userId: 2,
    phone: "13800000001",
    username: "13800000001",
    nickname: "原始昵称",
    role: "USER",
    verificationStatus: "UNVERIFIED",
    unreadNotificationCount: 0,
  };

  getHomeSummary.mockResolvedValue(authenticatedSummary);

  const wrapper = mount(HomeView, {
    global: {
      stubs: {
        RouterLink: {
          props: ["to"],
          template: "<a :data-to='to'><slot /></a>",
        },
      },
    },
  });

  await flushPromises();

  expect(wrapper.text()).toContain("已登录同学");
  expect(wrapper.text()).toContain("认证申请审核中");
  expect(wrapper.text()).toContain("认证状态更新");
  expect(userStore.profile.nickname).toBe("已登录同学");
  expect(userStore.unreadCount).toBe(3);
});
