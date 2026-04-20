import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, expect, test, vi } from "vitest";
import router from "../../router/index.js";
import AdminUsersView from "./AdminUsersView.vue";
import { banAdminUser, getAdminUsers, unbanAdminUser } from "../../api/admin.js";

vi.mock("../../api/admin.js", () => ({
  banAdminUser: vi.fn(),
  getAdminUsers: vi.fn(),
  unbanAdminUser: vi.fn(),
}));

function buildSummary() {
  return {
    total: 3,
    activeCount: 2,
    bannedCount: 1,
    verifiedCount: 1,
    users: [
      {
        id: 1,
        phone: "13800000000",
        nickname: "PlatformAdmin",
        realName: "Admin User",
        role: "ADMIN",
        status: "ACTIVE",
        verificationStatus: "VERIFIED",
        studentId: null,
        updatedAt: "2026-04-20T09:00:00",
      },
      {
        id: 2,
        phone: "13800000001",
        nickname: "NormalUser",
        realName: "Normal User",
        role: "USER",
        status: "ACTIVE",
        verificationStatus: "UNVERIFIED",
        studentId: null,
        updatedAt: "2026-04-20T09:10:00",
      },
      {
        id: 3,
        phone: "13800000002",
        nickname: "VerifiedUser",
        realName: "Verified User",
        role: "USER",
        status: "BANNED",
        verificationStatus: "VERIFIED",
        studentId: "20260001",
        updatedAt: "2026-04-20T09:20:00",
      },
    ],
  };
}

function mountView() {
  return mount(AdminUsersView);
}

beforeEach(() => {
  vi.clearAllMocks();
  window.localStorage.clear();
});

test("route exists as an admin-only route", () => {
  const route = router.resolve("/admin/users");

  expect(route.name).toBe("admin-users");
  expect(route.meta.requiresAuth).toBe(true);
  expect(route.meta.roles).toEqual(["ADMIN"]);
});

test("page loads summary and renders protected plus actionable rows", async () => {
  getAdminUsers.mockResolvedValue(buildSummary());

  const wrapper = mountView();
  await flushPromises();

  expect(getAdminUsers).toHaveBeenCalledTimes(1);
  expect(wrapper.text()).toContain("User status workbench");
  expect(wrapper.text()).toContain("PlatformAdmin");
  expect(wrapper.text()).toContain("NormalUser");
  expect(wrapper.text()).toContain("VerifiedUser");
  expect(wrapper.text()).toContain("Protected");
  expect(wrapper.find('[data-testid="ban-user-2"]').exists()).toBe(true);
  expect(wrapper.find('[data-testid="unban-user-3"]').exists()).toBe(true);
});

test("ban action calls the admin helper and reloads the desk", async () => {
  getAdminUsers
    .mockResolvedValueOnce(buildSummary())
    .mockResolvedValueOnce({
      ...buildSummary(),
      activeCount: 1,
      bannedCount: 2,
      users: buildSummary().users.map((user) => (user.id === 2 ? { ...user, status: "BANNED" } : user)),
    });
  banAdminUser.mockResolvedValue({ id: 2, status: "BANNED" });

  const wrapper = mountView();
  await flushPromises();

  await wrapper.find('[data-testid="ban-user-2"]').trigger("click");
  await flushPromises();

  expect(banAdminUser).toHaveBeenCalledWith(2);
  expect(getAdminUsers).toHaveBeenCalledTimes(2);
  expect(wrapper.text()).toContain("Account banned for NormalUser.");
});

test("restore action calls the admin helper and reloads the desk", async () => {
  getAdminUsers
    .mockResolvedValueOnce(buildSummary())
    .mockResolvedValueOnce({
      ...buildSummary(),
      activeCount: 3,
      bannedCount: 0,
      users: buildSummary().users.map((user) => (user.id === 3 ? { ...user, status: "ACTIVE" } : user)),
    });
  unbanAdminUser.mockResolvedValue({ id: 3, status: "ACTIVE" });

  const wrapper = mountView();
  await flushPromises();

  await wrapper.find('[data-testid="unban-user-3"]').trigger("click");
  await flushPromises();

  expect(unbanAdminUser).toHaveBeenCalledWith(3);
  expect(getAdminUsers).toHaveBeenCalledTimes(2);
  expect(wrapper.text()).toContain("Account restored for VerifiedUser.");
});
