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
    { code: "analytics", title: "Analytics", path: "/analytics", enabled: true, badge: "COMING_SOON" },
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

test("renders guest aggregation home with a live resources link", async () => {
  getHomeSummary.mockResolvedValue(guestSummary);

  const wrapper = mountView(guestSummary);
  await flushPromises();

  expect(getHomeSummary).toHaveBeenCalledTimes(1);
  expect(wrapper.html()).toContain('data-to="/resources"');
  expect(wrapper.html()).toContain('data-to="/jobs"');
  expect(wrapper.text()).toContain("Resume Pack");
  expect(wrapper.html()).toContain('data-to="/resources/11"');
  expect(wrapper.html()).toContain('data-to="{&quot;name&quot;:&quot;discover&quot;,&quot;query&quot;:{&quot;tab&quot;:&quot;ALL&quot;,&quot;period&quot;:&quot;WEEK&quot;}}"');
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
  expect(wrapper.html()).toContain('data-to="/resources"');
  expect(wrapper.html()).toContain('data-to="/assessment"');
  expect(wrapper.text()).toContain("Verification Update");
  expect(wrapper.text()).toContain("Hiring Diary");
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
