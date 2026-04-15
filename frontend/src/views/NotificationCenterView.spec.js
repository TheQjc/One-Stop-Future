import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { beforeEach, expect, test } from "vitest";
import NotificationCenterView from "./NotificationCenterView.vue";
import { useUserStore } from "../stores/user.js";

beforeEach(() => {
  window.localStorage.clear();
});

test("marks unread notifications as read and syncs unread count", async () => {
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
    unreadNotificationCount: 0,
  });

  const wrapper = mount(NotificationCenterView);
  await flushPromises();

  expect(wrapper.text()).toContain("欢迎进入平台");
  expect(userStore.unreadCount).toBe(1);

  const markReadButton = wrapper.findAll("button").find((node) => node.text().includes("标记已读"));
  await markReadButton.trigger("click");
  await flushPromises();

  expect(userStore.unreadCount).toBe(0);
  expect(wrapper.text()).toContain("已读");
});
