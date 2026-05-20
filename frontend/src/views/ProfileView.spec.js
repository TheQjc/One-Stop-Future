import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { beforeEach, expect, test } from "vitest";
import ProfileView from "./ProfileView.vue";
import { login, sendCode } from "../api/auth.js";
import { useUserStore } from "../stores/user.js";

const USER_KEY = "one-stop-future-demo-users";

beforeEach(() => {
  window.localStorage.clear();
});

test("shows the my resources link and submits verification request", async () => {
  setActivePinia(createPinia());
  const userStore = useUserStore();

  window.localStorage.setItem(USER_KEY, JSON.stringify([
    {
      id: 2,
      phone: "13800000001",
      nickname: "普通同学",
      role: "USER",
      status: "ACTIVE",
      verificationStatus: "UNVERIFIED",
      unreadNotificationCount: 1,
    },
  ]));

  userStore.token = "demo-token";
  userStore.persistProfile({
    id: 2,
    userId: 2,
    phone: "13800000001",
    nickname: "普通同学",
    role: "USER",
    verificationStatus: "UNVERIFIED",
    unreadNotificationCount: 1,
  });

  const wrapper = mount(ProfileView, {
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

  expect(wrapper.text()).toContain("个人中心");
  expect(wrapper.text()).toContain("查看通知");
  expect(wrapper.text()).toContain("认证申请");
  expect(wrapper.text()).toContain("我的资料");
  expect(wrapper.text()).toContain("上传记录");
  expect(wrapper.html()).toContain('data-to="/profile/resources"');

  await wrapper.find('input[name="verificationRealName"]').setValue("张同学");
  await wrapper.find('input[name="studentId"]').setValue("20241234");
  await wrapper.findAll("form")[1].trigger("submit.prevent");
  await flushPromises();

  expect(wrapper.text()).toContain("认证申请已提交。");
  expect(userStore.profile.verificationStatus).toBe("PENDING");
  expect(userStore.profile.studentId).toBe("20241234");
});

test("profile desk exposes resumes and applications workspace links", async () => {
  setActivePinia(createPinia());
  const userStore = useUserStore();

  userStore.token = "demo-token";
  userStore.persistProfile({
    id: 2,
    userId: 2,
    phone: "13800000001",
    nickname: "普通同学",
    role: "USER",
    verificationStatus: "UNVERIFIED",
    unreadNotificationCount: 1,
  });

  const wrapper = mount(ProfileView, {
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

  expect(wrapper.text()).toContain("常用入口");
  expect(wrapper.text()).toContain("我的简历");
  expect(wrapper.text()).toContain("申请记录");
  expect(wrapper.text()).toContain("保存资料");
  expect(wrapper.html()).toContain('data-to="/profile/resumes"');
  expect(wrapper.html()).toContain('data-to="/profile/applications"');
});

test("uses default demo users when profile refresh happens before demo user storage exists", async () => {
  setActivePinia(createPinia());
  const userStore = useUserStore();

  expect(window.localStorage.getItem(USER_KEY)).toBeNull();

  userStore.token = "demo-token";
  userStore.persistProfile({
    id: 3,
    userId: 3,
    phone: "13800000002",
    nickname: "Niudeyipi",
    role: "USER",
    verificationStatus: "VERIFIED",
    unreadNotificationCount: 0,
  });

  const wrapper = mount(ProfileView, {
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

  expect(wrapper.text()).toContain("20260001");

  await sendCode({ phone: "13800000002", purpose: "LOGIN" });
  const auth = await login({
    phone: "13800000002",
    verificationCode: "123456",
  });

  expect(auth.studentId).toBe("20260001");
});

test("patches old demo verified users that were stored before student ids existed", async () => {
  setActivePinia(createPinia());
  const userStore = useUserStore();

  window.localStorage.setItem(USER_KEY, JSON.stringify([
    {
      id: 3,
      phone: "13800000002",
      nickname: "Niudeyipi",
      role: "USER",
      status: "ACTIVE",
      verificationStatus: "VERIFIED",
      unreadNotificationCount: 0,
    },
  ]));

  userStore.token = "demo-token";
  userStore.persistProfile({
    id: 3,
    userId: 3,
    phone: "13800000002",
    nickname: "Niudeyipi",
    role: "USER",
    verificationStatus: "VERIFIED",
    unreadNotificationCount: 0,
  });

  const wrapper = mount(ProfileView, {
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

  expect(wrapper.text()).toContain("20260001");
  expect(userStore.profile.studentId).toBe("20260001");
});

test("refreshes a pending profile when the locked verification form has no student id", async () => {
  setActivePinia(createPinia());
  const userStore = useUserStore();

  window.localStorage.setItem(USER_KEY, JSON.stringify([
    {
      id: 2,
      phone: "13800000001",
      nickname: "PendingUser",
      role: "USER",
      status: "ACTIVE",
      verificationStatus: "PENDING",
      studentId: "20260009",
      unreadNotificationCount: 0,
    },
  ]));

  userStore.token = "demo-token";
  userStore.persistProfile({
    id: 2,
    userId: 2,
    phone: "13800000001",
    nickname: "PendingUser",
    role: "USER",
    verificationStatus: "PENDING",
    unreadNotificationCount: 0,
  });

  const wrapper = mount(ProfileView, {
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

  expect(wrapper.text()).toContain("20260009");
  expect(userStore.profile.studentId).toBe("20260009");
});

test("refreshes a verified profile so the student id is not hidden by the login snapshot", async () => {
  setActivePinia(createPinia());
  const userStore = useUserStore();

  window.localStorage.setItem(USER_KEY, JSON.stringify([
    {
      id: 3,
      phone: "13800000002",
      nickname: "Niudeyipi",
      role: "USER",
      status: "ACTIVE",
      verificationStatus: "VERIFIED",
      studentId: "20260001",
      unreadNotificationCount: 0,
    },
  ]));

  userStore.token = "demo-token";
  userStore.persistProfile({
    id: 3,
    userId: 3,
    phone: "13800000002",
    nickname: "Niudeyipi",
    role: "USER",
    verificationStatus: "VERIFIED",
    unreadNotificationCount: 0,
  });

  const wrapper = mount(ProfileView, {
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

  expect(wrapper.text()).toContain("20260001");
  expect(wrapper.text()).not.toContain("暂未提交");
  expect(userStore.profile.studentId).toBe("20260001");
});
