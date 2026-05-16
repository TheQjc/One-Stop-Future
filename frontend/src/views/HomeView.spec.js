import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { beforeEach, expect, test, vi } from "vitest";
import { useUserStore } from "../stores/user.js";
import { getHomeSummary } from "../api/home.js";

const push = vi.fn();

vi.mock("vue-router", async () => {
  const actual = await vi.importActual("vue-router");
  return {
    ...actual,
    useRouter: () => ({ push }),
  };
});

vi.mock("../api/home.js", () => ({
  getHomeSummary: vi.fn(),
}));

import HomeView from "./HomeView.vue";

const guestSummary = {
  viewerType: "GUEST",
  identity: null,
  roleLabel: "访客",
  verificationStatus: null,
  unreadNotificationCount: 0,
  todos: ["登录后可解锁个人中心、认证和通知。"],
  entries: [
    { code: "community", title: "社区", path: "/community", enabled: true, badge: null },
    { code: "jobs", title: "岗位", path: "/jobs", enabled: true, badge: null },
    { code: "resources", title: "资料", path: "/resources", enabled: true, badge: null },
    { code: "assessment", title: "测评", path: "/assessment", enabled: false, badge: "LOGIN_REQUIRED" },
    { code: "analytics", title: "分析", path: "/analytics", enabled: true, badge: null },
  ],
  latestNotifications: [],
  discoverPreview: {
    period: "WEEK",
    items: [
      {
        id: 11,
        type: "RESOURCE",
        title: "简历资料包",
        summary: "适合快速起步的一组求职资料。",
        primaryMeta: "平台推荐",
        secondaryMeta: "简历模板",
        path: "/resources/11",
        publishedAt: "2026-04-16T08:00:00",
        hotScore: 12,
        hotLabel: "本周高频下载",
      },
    ],
  },
};

const authenticatedSummary = {
  viewerType: "USER",
  identity: {
    userId: 2,
    phone: "13800000001",
    nickname: "普通同学",
    role: "USER",
    verificationStatus: "PENDING",
  },
  roleLabel: "普通用户",
  verificationStatus: "PENDING",
  unreadNotificationCount: 3,
  todos: [
    "学生认证正在审核中。",
    "你有 3 条未读通知。",
  ],
  entries: [
    { code: "community", title: "社区", path: "/community", enabled: true, badge: null },
    { code: "jobs", title: "岗位", path: "/jobs", enabled: true, badge: null },
    { code: "resources", title: "资料", path: "/resources", enabled: true, badge: null },
    { code: "assessment", title: "测评", path: "/assessment", enabled: true, badge: null },
    { code: "analytics", title: "分析", path: "/analytics", enabled: true, badge: null },
  ],
  latestNotifications: [
    {
      id: 1,
      type: "SYSTEM",
      title: "认证进度更新",
      content: "你的学生认证正在审核中。",
      read: false,
      createdAt: "2026-04-15T08:30:00",
    },
  ],
  discoverPreview: {
    period: "WEEK",
    items: [
      {
        id: 21,
        type: "POST",
        title: "秋招周记",
        summary: "一篇本周热度较高的经验帖。",
        primaryMeta: "普通同学",
        secondaryMeta: "就业",
        path: "/community/21",
        publishedAt: "2026-04-16T09:30:00",
        hotScore: 18,
        hotLabel: "本周热议",
      },
    ],
  },
};

const adminSummary = {
  viewerType: "ADMIN",
  identity: {
    userId: 1,
    phone: "13800000000",
    nickname: "平台管理员",
    role: "ADMIN",
    verificationStatus: "VERIFIED",
  },
  roleLabel: "管理员",
  verificationStatus: "VERIFIED",
  unreadNotificationCount: 2,
  todos: [
    "还有 2 条待审核的认证申请。",
    "你有 2 条未读通知。",
  ],
  entries: [
    { code: "community", title: "社区", path: "/community", enabled: true, badge: null },
    { code: "jobs", title: "岗位", path: "/jobs", enabled: true, badge: null },
    { code: "resources", title: "资料", path: "/resources", enabled: true, badge: null },
    { code: "assessment", title: "测评", path: "/assessment", enabled: true, badge: null },
    { code: "analytics", title: "分析", path: "/analytics", enabled: true, badge: null },
    { code: "admin-dashboard", title: "运营总览", path: "/admin/dashboard", enabled: true, badge: null },
    { code: "admin-verifications", title: "认证审核", path: "/admin/verifications", enabled: true, badge: null },
  ],
  latestNotifications: [
    {
      id: 7,
      type: "SYSTEM",
      title: "管理员日报",
      content: "今日队列已准备好，适合集中处理。",
      read: false,
      createdAt: "2026-04-17T08:30:00",
    },
  ],
  discoverPreview: {
    period: "WEEK",
    items: [
      {
        id: 31,
        type: "POST",
        title: "运营值班笔记",
        summary: "一条当前仍有热度的运营观察。",
        primaryMeta: "平台管理员",
        secondaryMeta: "就业",
        path: "/community/31",
        publishedAt: "2026-04-17T09:30:00",
        hotScore: 9,
        hotLabel: "本周关注",
      },
    ],
  },
};

beforeEach(() => {
  vi.clearAllMocks();
  window.localStorage.clear();
  push.mockReset();
});

function mountView() {
  setActivePinia(createPinia());

  return mount(HomeView, {
    global: {
      stubs: {
        RouterLink: {
          props: ["to"],
          template: "<a :data-to='typeof to === \"string\" ? to : JSON.stringify(to)'><slot /></a>",
        },
      },
    },
  });
}

test("renders guest hero copy in approved Chinese wording with live resources links", async () => {
  getHomeSummary.mockResolvedValue(guestSummary);

  const wrapper = mountView();
  await flushPromises();

  expect(getHomeSummary).toHaveBeenCalledTimes(1);
  expect(wrapper.get('[data-test="home-hero-title"]').text()).toBe("把就业、考研、留学放到一个首页里，先看方向，再做决定");
  expect(wrapper.get('[data-test="home-hero-copy"]').text()).toBe("公开内容、常用入口和成长方向会集中展示，先帮你看清选择，再进入具体模块。");
  expect(wrapper.get('[data-test="home-search-label"]').text()).toBe("站内搜索");
  expect(wrapper.get('input[name="home-search"]').attributes("placeholder")).toBe("搜索经验帖、岗位、院校、资料");
  expect(wrapper.get('button[type="submit"]').text()).toBe("搜索");
  expect(wrapper.get('[data-test="home-status-chip"]').text()).toBe("首页服务已开启");
  expect(wrapper.get('[data-test="home-primary-cta"]').text()).toBe("登录查看个人待办");
  expect(wrapper.get('[data-test="home-secondary-cta"]').text()).toBe("立即注册");
  expect(wrapper.get('[data-test="home-section-snapshot"]').text()).toContain("今日概览");
  expect(wrapper.get('[data-test="home-section-discover"]').text()).toContain("本周趋势");
  expect(wrapper.get('[data-test="home-section-notifications"]').text()).toContain("最新通知");
  expect(wrapper.get('[data-test="home-discover-cta"]').text()).toBe("查看全部趋势");
  expect(wrapper.html()).toContain('data-to="/resources"');
  expect(wrapper.html()).toContain('data-to="/jobs"');
  expect(wrapper.html()).toContain('data-to="/analytics"');
  expect(wrapper.text()).toContain("简历资料包");
  expect(wrapper.text()).toContain("资料");
  expect(wrapper.text()).toContain("热度 12");
  expect(wrapper.html()).toContain('data-to="/resources/11"');
  expect(wrapper.html()).toContain('data-to="{&quot;name&quot;:&quot;discover&quot;,&quot;query&quot;:{&quot;tab&quot;:&quot;ALL&quot;,&quot;period&quot;:&quot;WEEK&quot;}}"');
});

test("hydrates authenticated summary into store and shows signed-in hero copy", async () => {
  setActivePinia(createPinia());
  const userStore = useUserStore();

  userStore.token = "demo-token";
  userStore.profile = {
    id: 2,
    userId: 2,
    phone: "13800000001",
    username: "13800000001",
    nickname: "更新前昵称",
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
          template: "<a :data-to='typeof to === \"string\" ? to : JSON.stringify(to)'><slot /></a>",
        },
      },
    },
  });

  await flushPromises();

  expect(userStore.profile.nickname).toBe("普通同学");
  expect(userStore.unreadCount).toBe(3);
  expect(wrapper.get('[data-test="home-hero-title"]').text()).toBe("你好，普通同学，今天先从这几件事开始");
  expect(wrapper.get('[data-test="home-hero-copy"]').text()).toBe("认证进度、未读通知和常用入口都会集中在这里，帮你先处理当下，再继续规划下一步。");
  expect(wrapper.get('[data-test="home-status-chip"]').text()).toBe("普通用户 / 普通同学");
  expect(wrapper.get('[data-test="home-unread-chip"]').text()).toBe("3 条未读通知");
  expect(wrapper.get('[data-test="home-primary-cta"]').text()).toBe("进入个人中心");
  expect(wrapper.get('[data-test="home-secondary-cta"]').text()).toBe("查看通知");
  expect(wrapper.html()).toContain('data-to="/resources"');
  expect(wrapper.html()).toContain('data-to="/assessment"');
  expect(wrapper.html()).toContain('data-to="/analytics"');
  expect(wrapper.text()).toContain("认证进度更新");
  expect(wrapper.text()).toContain("秋招周记");
});

test("keeps signed-in hero actions when a public home summary is returned for a local session", async () => {
  setActivePinia(createPinia());
  const userStore = useUserStore();

  userStore.token = "demo-token";
  userStore.profile = {
    id: 2,
    userId: 2,
    phone: "13800000001",
    username: "13800000001",
    nickname: "Niudeyipi",
    role: "USER",
    verificationStatus: "PENDING",
    unreadNotificationCount: 0,
  };

  getHomeSummary.mockResolvedValue(guestSummary);

  const wrapper = mount(HomeView, {
    global: {
      stubs: {
        RouterLink: {
          props: ["to"],
          template: "<a :data-to='typeof to === \"string\" ? to : JSON.stringify(to)'><slot /></a>",
        },
      },
    },
  });

  await flushPromises();

  expect(wrapper.get('[data-test="home-status-chip"]').text()).toContain("Niudeyipi");
  expect(wrapper.get('[data-test="home-primary-cta"]').text()).toBe("进入个人中心");
  expect(wrapper.get('[data-test="home-secondary-cta"]').text()).toBe("查看通知");
  expect(wrapper.text()).not.toContain("登录查看个人待办");
});

test("home keeps common entry section before growth directions", async () => {
  getHomeSummary.mockResolvedValue(guestSummary);

  const wrapper = mountView();
  await flushPromises();

  const orderedSections = wrapper.findAll('article[data-test^="home-section-"][data-test]').map((node) => node.attributes("data-test"));

  expect(orderedSections).toContain("home-section-entries");
  expect(orderedSections).toContain("home-section-tracks");
  expect(orderedSections.indexOf("home-section-entries")).toBeLessThan(orderedSections.indexOf("home-section-tracks"));
  expect(wrapper.get('[data-test="home-section-entries"]').text()).toContain("常用入口");
  expect(wrapper.get('[data-test="home-section-tracks"]').text()).toContain("成长方向");
  expect(wrapper.text()).not.toContain("Desk 00");
  expect(wrapper.text()).not.toContain("Track 03");
});

test("home search submits into the unified search page", async () => {
  getHomeSummary.mockResolvedValue(guestSummary);

  const wrapper = mountView();
  await flushPromises();

  await wrapper.find('input[name="home-search"]').setValue("  简历  ");
  await wrapper.find('[data-test="home-search-form"]').trigger("submit.prevent");

  expect(push).toHaveBeenCalledWith({
    name: "search",
    query: { q: "简历", type: "ALL", sort: "RELEVANCE" },
  });
});

test("blank home search input does not navigate", async () => {
  getHomeSummary.mockResolvedValue(guestSummary);

  const wrapper = mountView();
  await flushPromises();

  await wrapper.find('input[name="home-search"]').setValue("   ");
  await wrapper.find('[data-test="home-search-form"]').trigger("submit.prevent");

  expect(push).not.toHaveBeenCalled();
});

test("home preview shows a graceful empty state when preview items are empty", async () => {
  getHomeSummary.mockResolvedValue({
    ...guestSummary,
    discoverPreview: {
      period: "WEEK",
      items: [],
    },
  });

  const wrapper = mountView();
  await flushPromises();

  expect(wrapper.get('[data-test="home-section-discover"]').text()).toContain("本周趋势还在更新中");
  expect(wrapper.get('[data-test="home-section-discover"]').text()).toContain("本周还没有新的趋势内容，稍后再来看看。");
});

test("guest home shows a Chinese notification empty state", async () => {
  getHomeSummary.mockResolvedValue({
    ...guestSummary,
    latestNotifications: [],
  });

  const wrapper = mountView();
  await flushPromises();

  expect(wrapper.get('[data-test="home-section-notifications"]').text()).toContain("登录后可查看与你相关的通知和处理结果。");
});

test("admin home shows the dashboard link before the existing admin destinations", async () => {
  getHomeSummary.mockResolvedValue(adminSummary);

  const wrapper = mountView();
  await flushPromises();

  const linkTargets = wrapper.findAll('[data-test="home-section-entries"] a[data-to]').map((node) => node.attributes("data-to"));

  expect(linkTargets).toEqual(expect.arrayContaining([
    "/admin/dashboard",
    "/admin/verifications",
    "/admin/community",
    "/admin/jobs",
  ]));
  expect(linkTargets.indexOf("/admin/dashboard")).toBeLessThan(linkTargets.indexOf("/admin/verifications"));
  expect(linkTargets.indexOf("/admin/dashboard")).toBeLessThan(linkTargets.indexOf("/admin/community"));
  expect(linkTargets.indexOf("/admin/dashboard")).toBeLessThan(linkTargets.indexOf("/admin/jobs"));
});
