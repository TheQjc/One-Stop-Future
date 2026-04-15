import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { beforeEach, expect, test } from "vitest";
import ProfileView from "./ProfileView.vue";
import { useUserStore } from "../stores/user.js";

const USER_KEY = "one-stop-future-demo-users";

beforeEach(() => {
  window.localStorage.clear();
});

test("submits verification request and updates profile status", async () => {
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

  await wrapper.find('input[name="verificationRealName"]').setValue("张三");
  await wrapper.find('input[name="studentId"]').setValue("20241234");
  await wrapper.findAll("form")[1].trigger("submit.prevent");
  await flushPromises();

  expect(wrapper.text()).toContain("认证申请已提交");
  expect(userStore.profile.verificationStatus).toBe("PENDING");
  expect(userStore.profile.studentId).toBe("20241234");
});
