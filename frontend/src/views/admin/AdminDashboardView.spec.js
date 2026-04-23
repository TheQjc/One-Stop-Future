import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, expect, test, vi } from "vitest";
import router from "../../router/index.js";
import { getAdminDashboardSummary } from "../../api/admin.js";
import AdminDashboardView from "./AdminDashboardView.vue";

vi.mock("../../api/admin.js", () => ({
  getAdminDashboardSummary: vi.fn(),
}));

function buildSummary() {
  return {
    verification: {
      pendingCount: 3,
      reviewedToday: 1,
      latestPendingApplications: [
        {
          id: 501,
          applicantNickname: "Pending Student",
          realName: "Taylor Chen",
          studentId: "20260009",
          createdAt: "2026-04-18T09:15:00",
          status: "PENDING",
        },
      ],
    },
    community: {
      totalCount: 7,
      publishedCount: 5,
      hiddenCount: 1,
      deletedCount: 1,
      latestPosts: [
        {
          id: 41,
          tag: "CAREER",
          title: "First community post",
          status: "HIDDEN",
          authorNickname: "DeskEditor",
          likeCount: 8,
          commentCount: 2,
          favoriteCount: 1,
          createdAt: "2026-04-18T08:00:00",
        },
      ],
    },
    jobs: {
      totalCount: 4,
      draftCount: 1,
      publishedCount: 2,
      offlineCount: 1,
      latestActionableJobs: [
        {
          id: 61,
          title: "Campus operations intern",
          companyName: "One Stop Studio",
          city: "Shanghai",
          sourcePlatform: "Official Site",
          status: "DRAFT",
          updatedAt: "2026-04-18T07:30:00",
        },
      ],
    },
    resources: {
      totalCount: 6,
      pendingCount: 2,
      publishedCount: 3,
      closedCount: 1,
      latestPendingResources: [
        {
          id: 71,
          title: "Resume workshop pack",
          uploaderNickname: "ArchiveKeeper",
          fileName: "resume-pack.pdf",
          status: "PENDING",
          createdAt: "2026-04-18T06:45:00",
        },
      ],
    },
  };
}

function buildEmptyRecentSummary() {
  return {
    verification: {
      pendingCount: 3,
      reviewedToday: 1,
      latestPendingApplications: [],
    },
    community: {
      totalCount: 7,
      publishedCount: 5,
      hiddenCount: 1,
      deletedCount: 1,
      latestPosts: [],
    },
    jobs: {
      totalCount: 4,
      draftCount: 1,
      publishedCount: 2,
      offlineCount: 1,
      latestActionableJobs: [],
    },
    resources: {
      totalCount: 6,
      pendingCount: 2,
      publishedCount: 3,
      closedCount: 1,
      latestPendingResources: [],
    },
  };
}

function mountView() {
  return mount(AdminDashboardView, {
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

beforeEach(() => {
  vi.clearAllMocks();
  window.localStorage.clear();
});

test("route exists as an admin-only route", () => {
  const route = router.resolve("/admin/dashboard");

  expect(route.name).toBe("admin-dashboard");
  expect(route.meta.requiresAuth).toBe(true);
  expect(route.meta.roles).toEqual(["ADMIN"]);
});

test("page loads summary and renders four sections", async () => {
  getAdminDashboardSummary.mockResolvedValue(buildSummary());

  const wrapper = mountView();
  await flushPromises();

  expect(getAdminDashboardSummary).toHaveBeenCalledTimes(1);
  expect(wrapper.text()).toContain("运营看板");
  expect(wrapper.text()).toContain("认证队列");
  expect(wrapper.text()).toContain("社区看板");
  expect(wrapper.text()).toContain("岗位看板");
  expect(wrapper.text()).toContain("资源看板");
  expect(wrapper.text()).toContain("Pending Student");
  expect(wrapper.text()).toContain("First community post");
  expect(wrapper.text()).toContain("Campus operations intern");
  expect(wrapper.text()).toContain("Resume workshop pack");
});

test("page-level retry reloads the summary after an initial failure", async () => {
  getAdminDashboardSummary
    .mockRejectedValueOnce(new Error("Board loading failed."))
    .mockResolvedValueOnce(buildSummary());

  const wrapper = mountView();
  await flushPromises();

  expect(wrapper.text()).toContain("Board loading failed.");
  expect(wrapper.text()).not.toContain("认证队列");

  await wrapper.find("button.ghost-btn").trigger("click");
  await flushPromises();

  expect(getAdminDashboardSummary).toHaveBeenCalledTimes(2);
  expect(wrapper.text()).toContain("认证队列");
  expect(wrapper.text()).toContain("Pending Student");
});

test("CTA links point to each admin desk", async () => {
  getAdminDashboardSummary.mockResolvedValue(buildSummary());

  const wrapper = mountView();
  await flushPromises();

  const linkTargets = wrapper.findAll("a[data-to]").map((node) => node.attributes("data-to"));

  expect(linkTargets).toEqual(expect.arrayContaining([
    "/admin/verifications",
    "/admin/community",
    "/admin/jobs",
    "/admin/resources",
  ]));
});

test("empty recent lines keep metrics and desk links visible", async () => {
  getAdminDashboardSummary.mockResolvedValue(buildEmptyRecentSummary());

  const wrapper = mountView();
  await flushPromises();

  expect(wrapper.text()).toContain("待审核");
  expect(wrapper.text()).toContain("今日已审");
  expect(wrapper.text()).toContain("全部帖子");
  expect(wrapper.text()).toContain("全部岗位");
  expect(wrapper.text()).toContain("全部记录");
  expect(wrapper.text()).toContain("当前没有待审核申请。");
  expect(wrapper.text()).toContain("当前没有最近帖子需要展示。");
  expect(wrapper.text()).toContain("当前没有草稿或待处理岗位。");
  expect(wrapper.text()).toContain("当前没有待处理资源。");

  const linkTargets = wrapper.findAll("a[data-to]").map((node) => node.attributes("data-to"));

  expect(linkTargets).toEqual(expect.arrayContaining([
    "/admin/verifications",
    "/admin/community",
    "/admin/jobs",
    "/admin/resources",
  ]));
});
