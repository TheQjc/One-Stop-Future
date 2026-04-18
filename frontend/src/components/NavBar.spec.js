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

  expect(wrapper.html()).toContain('data-to="/discover"');
  expect(wrapper.html()).toContain('data-to="/search"');
  expect(wrapper.html()).toContain('data-to="/login"');
  expect(wrapper.html()).toContain('data-to="/register"');
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

  expect(wrapper.html()).toContain('data-to="/discover"');
  expect(wrapper.html()).toContain('data-to="/search"');
  expect(wrapper.html()).toContain('data-to="/profile"');
  expect(wrapper.html()).toContain('data-to="/notifications"');
  expect(wrapper.html()).toContain('data-to="/admin/dashboard"');
  expect(wrapper.html()).toContain('data-to="/admin/verifications"');
  expect(wrapper.html()).toContain('data-to="/admin/community"');
});
