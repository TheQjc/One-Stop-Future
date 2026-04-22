import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { beforeEach, expect, test } from "vitest";
import ProfileView from "./ProfileView.vue";
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
      nickname: "NormalUser",
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
    nickname: "NormalUser",
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

  await wrapper.find('input[name="verificationRealName"]').setValue("Student User");
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
    nickname: "NormalUser",
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
