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
  roleLabel: "Guest",
  verificationStatus: null,
  unreadNotificationCount: 0,
  todos: ["Sign in to unlock profile, verification, and notifications."],
  entries: [
    { code: "community", title: "Community", path: "/community", enabled: true, badge: null },
    { code: "jobs", title: "Jobs", path: "/jobs", enabled: true, badge: null },
    { code: "resources", title: "Resources", path: "/resources", enabled: true, badge: null },
    { code: "assessment", title: "Assessment", path: "/assessment", enabled: false, badge: "LOGIN_REQUIRED" },
    { code: "analytics", title: "Analytics", path: "/analytics", enabled: true, badge: null },
  ],
  latestNotifications: [],
  discoverPreview: {
    period: "WEEK",
    items: [
      {
        id: 11,
        type: "RESOURCE",
        title: "Resume Pack",
        summary: "A practical starter pack.",
        primaryMeta: "Career Desk",
        secondaryMeta: "RESUME_TEMPLATE",
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
    nickname: "SignedInUser",
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
    { code: "community", title: "Community", path: "/community", enabled: true, badge: null },
    { code: "jobs", title: "Jobs", path: "/jobs", enabled: true, badge: null },
    { code: "resources", title: "Resources", path: "/resources", enabled: true, badge: null },
    { code: "assessment", title: "Assessment", path: "/assessment", enabled: true, badge: null },
    { code: "analytics", title: "Analytics", path: "/analytics", enabled: true, badge: null },
  ],
  latestNotifications: [
    {
      id: 1,
      type: "SYSTEM",
      title: "Verification Update",
      content: "Your verification request is under review.",
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
        title: "Hiring Diary",
        summary: "A verified post with weekly traction.",
        primaryMeta: "SignedInUser",
        secondaryMeta: "CAREER",
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
    nickname: "AdminDesk",
    role: "ADMIN",
    verificationStatus: "VERIFIED",
  },
  roleLabel: "Administrator",
  verificationStatus: "VERIFIED",
  unreadNotificationCount: 2,
  todos: [
    "Review 2 pending verification applications.",
    "You have 2 unread notifications.",
  ],
  entries: [
    { code: "community", title: "Community", path: "/community", enabled: true, badge: null },
    { code: "jobs", title: "Jobs", path: "/jobs", enabled: true, badge: null },
    { code: "resources", title: "Resources", path: "/resources", enabled: true, badge: null },
    { code: "assessment", title: "Assessment", path: "/assessment", enabled: true, badge: null },
    { code: "analytics", title: "Analytics", path: "/analytics", enabled: true, badge: null },
    { code: "admin-dashboard", title: "Admin Dashboard", path: "/admin/dashboard", enabled: true, badge: null },
    { code: "admin-verifications", title: "Admin Verification Review", path: "/admin/verifications", enabled: true, badge: null },
  ],
  latestNotifications: [
    {
      id: 7,
      type: "SYSTEM",
      title: "Admin Daily Read",
      content: "Dashboard queues are ready to review.",
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
        title: "Desk Notes",
        summary: "A moderation note with current traction.",
        primaryMeta: "AdminDesk",
        secondaryMeta: "CAREER",
        path: "/community/31",
        publishedAt: "2026-04-17T09:30:00",
        hotScore: 9,
        hotLabel: "Weekly signal",
      },
    ],
  },
};

beforeEach(() => {
  vi.clearAllMocks();
  window.localStorage.clear();
  push.mockReset();
});

function mountView(summary) {
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

  const wrapper = mountView(guestSummary);
  await flushPromises();

  expect(getHomeSummary).toHaveBeenCalledTimes(1);
  expect(wrapper.get('[data-test="home-hero-title"]').text()).toBe("把就业、考研、留学放到一个首页里，先看方向，再做决定");
  expect(wrapper.get('[data-test="home-hero-copy"]').text()).toBe("公开内容、常用入口和成长方向会集中展示，先帮你看清选择，再进入具体模块。");
  expect(wrapper.get('[data-test="home-search-label"]').text()).toBe("站内搜索");
  expect(wrapper.get('input[name="home-search"]').attributes("placeholder")).toBe("搜索经验帖、岗位、院校、资料");
  expect(wrapper.get(".hero-search__submit").text()).toBe("搜索");
  expect(wrapper.get('[data-test="home-status-chip"]').text()).toBe("首页服务已开启");
  expect(wrapper.get('[data-test="home-primary-cta"]').text()).toBe("登录查看个人待办");
  expect(wrapper.get('[data-test="home-secondary-cta"]').text()).toBe("立即注册");
  expect(wrapper.html()).toContain('data-to="/resources"');
  expect(wrapper.html()).toContain('data-to="/jobs"');
  expect(wrapper.html()).toContain('data-to="/analytics"');
  expect(wrapper.text()).toContain("Resume Pack");
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
    nickname: "BeforeHydrate",
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

  expect(userStore.profile.nickname).toBe("SignedInUser");
  expect(userStore.unreadCount).toBe(3);
  expect(wrapper.get('[data-test="home-hero-title"]').text()).toBe("你好，SignedInUser，今天先从这几件事开始");
  expect(wrapper.get('[data-test="home-hero-copy"]').text()).toBe("认证进度、未读通知和常用入口都会集中在这里，帮你先处理当下，再继续规划下一步。");
  expect(wrapper.get('[data-test="home-status-chip"]').text()).toBe("普通用户 / SignedInUser");
  expect(wrapper.get('[data-test="home-unread-chip"]').text()).toBe("3 条未读通知");
  expect(wrapper.get('[data-test="home-primary-cta"]').text()).toBe("进入个人中心");
  expect(wrapper.get('[data-test="home-secondary-cta"]').text()).toBe("查看通知");
  expect(wrapper.html()).toContain('data-to="/resources"');
  expect(wrapper.html()).toContain('data-to="/assessment"');
  expect(wrapper.html()).toContain('data-to="/analytics"');
  expect(wrapper.text()).toContain("Verification Update");
  expect(wrapper.text()).toContain("Hiring Diary");
});

test("home keeps common entry section before growth directions", async () => {
  getHomeSummary.mockResolvedValue(guestSummary);

  const wrapper = mountView();
  await flushPromises();

  const orderedSections = wrapper.findAll('article.section-card[data-test]').map((node) => node.attributes("data-test"));

  expect(orderedSections).toContain("home-section-entries");
  expect(orderedSections).toContain("home-section-tracks");
  expect(orderedSections.indexOf("home-section-entries")).toBeLessThan(orderedSections.indexOf("home-section-tracks"));
  expect(wrapper.get('[data-test="home-section-entries"]').text()).toContain("常用入口");
  expect(wrapper.get('[data-test="home-section-tracks"]').text()).toContain("成长方向");
});

test("home search submits into the unified search page", async () => {
  getHomeSummary.mockResolvedValue(guestSummary);

  const wrapper = mountView();
  await flushPromises();

  await wrapper.find('input[name="home-search"]').setValue("  resume  ");
  await wrapper.find('[data-test="home-search-form"]').trigger("submit.prevent");

  expect(push).toHaveBeenCalledWith({
    name: "search",
    query: { q: "resume", type: "ALL", sort: "RELEVANCE" },
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

  expect(wrapper.text()).toContain("No discover picks have entered this weekly desk yet.");
});

test("admin home shows the dashboard link before the existing admin destinations", async () => {
  getHomeSummary.mockResolvedValue(adminSummary);

  const wrapper = mountView();
  await flushPromises();

  const linkTargets = wrapper.findAll(".service-grid a[data-to]").map((node) => node.attributes("data-to"));

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
