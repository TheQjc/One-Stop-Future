import { mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { beforeEach, expect, test, vi } from "vitest";
import { useUserStore } from "../stores/user.js";
import NavBar from "./NavBar.vue";

vi.mock("vue-router", async () => {
  const actual = await vi.importActual("vue-router");
  return {
    ...actual,
    useRouter: () => ({ push: vi.fn() }),
  };
});

function mountNavBar() {
  return mount(NavBar, {
    global: {
      stubs: {
        RouterLink: {
          props: ["to"],
          template: "<a :data-to='typeof to === \"string\" ? to : JSON.stringify(to)'><slot /></a>",
        },
        NotificationBell: {
          template: "<span data-test='notification-bell' />",
        },
        VerificationStatusBadge: {
          template: "<span data-test='verification-badge' />",
        },
      },
    },
  });
}

beforeEach(() => {
  vi.clearAllMocks();
  window.localStorage.clear();
  setActivePinia(createPinia());
});

test("navbar exposes discover and search entries for guests", () => {
  const wrapper = mountNavBar();
  const text = wrapper.text();

  expect(wrapper.html()).toContain('data-to="/discover"');
  expect(wrapper.html()).toContain('data-to="/search"');
  expect(wrapper.html()).toContain('data-to="/login"');
  expect(wrapper.html()).toContain('data-to="/register"');
  expect(text).toContain("学生成长服务平台");
  expect(text).toContain("一站式成长平台");
  expect(text).toContain("One-Stop Future");
  expect(text).toContain("首页");
  expect(text).toContain("社区");
  expect(text).toContain("趋势");
  expect(text).toContain("搜索");
  expect(text).toContain("登录");
  expect(text).toContain("注册");
  expect(text).not.toContain("Home");
  expect(text).not.toContain("Discover");
});

test("navbar keeps authenticated navigation while exposing discover, search, and the admin dashboard", () => {
  const userStore = useUserStore();
  userStore.token = "demo-token";
  userStore.profile = {
    id: 1,
    userId: 1,
    nickname: "Admin",
    phone: "13800000000",
    role: "ADMIN",
    verificationStatus: "VERIFIED",
    unreadNotificationCount: 2,
  };

  const wrapper = mountNavBar();
  const text = wrapper.text();
  const navHtml = wrapper.find("nav").html();

  expect(wrapper.html()).toContain('data-to="/discover"');
  expect(wrapper.html()).toContain('data-to="/search"');
  expect(wrapper.html()).toContain('data-to="/profile"');
  expect(wrapper.html()).toContain('data-to="/notifications"');
  expect(navHtml).not.toContain('data-to="/profile"');
  expect(wrapper.get(".site-user").attributes("data-to")).toBe("/profile");
  expect(wrapper.html()).toContain('data-to="/admin/dashboard"');
  expect(wrapper.html()).toContain('data-to="/admin/users"');
  expect(wrapper.html()).toContain('data-to="/admin/verifications"');
  expect(wrapper.html()).toContain('data-to="/admin/community"');
  expect(text).toContain("我的");
  expect(text).toContain("Admin");
  expect(text).toContain("通知");
  expect(text).toContain("运营总览");
  expect(text).toContain("用户管理");
  expect(text).toContain("认证审核");
  expect(text).toContain("社区管理");
  expect(text).toContain("退出登录");
  expect(text).not.toContain("Operations");
  expect(text).not.toContain("Notifications");
});

test("navbar shows the admin applications link for admins", () => {
  const userStore = useUserStore();
  userStore.token = "demo-token";
  userStore.profile = {
    id: 1,
    userId: 1,
    nickname: "Admin",
    phone: "13800000000",
    role: "ADMIN",
    verificationStatus: "VERIFIED",
    unreadNotificationCount: 2,
  };

  const wrapper = mountNavBar();
  const text = wrapper.text();

  expect(wrapper.html()).toContain('data-to="/admin/applications"');
  expect(wrapper.html()).toContain('data-to="/admin/users"');
  expect(text).toContain("申请管理");
  expect(text).toContain("用户管理");
  expect(text).not.toContain("Applications");
});
