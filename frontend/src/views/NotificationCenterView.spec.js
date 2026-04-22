import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { beforeEach, expect, test } from "vitest";
import NotificationCenterView from "./NotificationCenterView.vue";
import { useUserStore } from "../stores/user.js";

beforeEach(() => {
  window.localStorage.clear();
});

function mountView() {
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

  return {
    wrapper: mount(NotificationCenterView),
    userStore,
  };
}

test("marks unread notifications as read and syncs unread count", async () => {
  const { wrapper, userStore } = mountView();
  await flushPromises();

  expect(wrapper.text()).toContain("消息工作台");
  expect(wrapper.text()).toContain("通知中心");
  expect(wrapper.text()).toContain("未读通知");
  expect(wrapper.text()).toContain("全部标记为已读");
  expect(wrapper.text()).toContain("仅看未读");
  expect(wrapper.text()).toContain("\u6B22\u8FCE\u8FDB\u5165\u5E73\u53F0");
  expect(userStore.unreadCount).toBe(1);

  const markReadButton = wrapper.findAll("button")
    .find((node) => node.text().includes("\u6807\u8BB0\u5DF2\u8BFB"));
  await markReadButton.trigger("click");
  await flushPromises();

  expect(userStore.unreadCount).toBe(0);
  expect(wrapper.text()).toContain("\u5DF2\u8BFB");
});

test("renders the community reply label for reply notifications", async () => {
  window.localStorage.setItem("one-stop-future-demo-notifications", JSON.stringify([
    {
      id: 2001,
      userId: 2,
      type: "COMMUNITY_REPLY_RECEIVED",
      title: "Your comment received a reply",
      content: "VerifiedUser replied to your comment under \"Offer timeline notes\"",
      read: false,
      createdAt: "2026-04-21T10:00:00",
      readAt: null,
    },
  ]));

  const { wrapper } = mountView();
  await flushPromises();

  expect(wrapper.text()).toContain("消息工作台");
  expect(wrapper.text()).toContain("通知中心");
  expect(wrapper.text()).toContain("\u8BC4\u8BBA\u56DE\u590D");
  expect(wrapper.text()).toContain("Your comment received a reply");
  expect(wrapper.text()).toContain("VerifiedUser replied to your comment under \"Offer timeline notes\"");
});
