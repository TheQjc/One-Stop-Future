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
    nickname: "普通同学",
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
      title: "你的评论收到了回复",
      content: "认证同学回复了你在《录用时间线笔记》下的评论",
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
  expect(wrapper.text()).toContain("你的评论收到了回复");
  expect(wrapper.text()).toContain("认证同学回复了你在《录用时间线笔记》下的评论");
});

test("renders the community comment label for post comment notifications", async () => {
  window.localStorage.setItem("one-stop-future-demo-notifications", JSON.stringify([
    {
      id: 2006,
      userId: 2,
      type: "COMMUNITY_COMMENT_RECEIVED",
      title: "你的帖子收到评论",
      content: "认证同学评论了你的帖子《录用时间线笔记》",
      read: false,
      createdAt: "2026-04-21T10:00:00",
      readAt: null,
    },
  ]));

  const { wrapper } = mountView();
  await flushPromises();

  expect(wrapper.text()).toContain("帖子评论");
  expect(wrapper.text()).toContain("你的帖子收到评论");
  expect(wrapper.text()).toContain("认证同学评论了你的帖子《录用时间线笔记》");
});

test("renders resource and post interaction notification labels", async () => {
  window.localStorage.setItem("one-stop-future-demo-notifications", JSON.stringify([
    {
      id: 2002,
      userId: 2,
      type: "COMMUNITY_POST_LIKED",
      title: "你的帖子收到了点赞",
      content: "认证同学点赞了你的帖子《录用时间线笔记》",
      read: false,
      createdAt: "2026-04-21T11:00:00",
      readAt: null,
    },
    {
      id: 2003,
      userId: 2,
      type: "RESOURCE_APPROVED",
      title: "资料已通过审核",
      content: "你上传的资料已经发布。",
      read: false,
      createdAt: "2026-04-21T10:00:00",
      readAt: null,
    },
    {
      id: 2004,
      userId: 2,
      type: "RESOURCE_REJECTED",
      title: "资料需要修改",
      content: "请根据审核意见调整资料。",
      read: true,
      createdAt: "2026-04-21T09:00:00",
      readAt: "2026-04-21T09:30:00",
    },
    {
      id: 2005,
      userId: 2,
      type: "RESOURCE_OFFLINED",
      title: "资料已下线",
      content: "你的资料已被管理员下线。",
      read: true,
      createdAt: "2026-04-21T08:00:00",
      readAt: "2026-04-21T08:30:00",
    },
  ]));

  const { wrapper } = mountView();
  await flushPromises();

  expect(wrapper.text()).toContain("帖子点赞");
  expect(wrapper.text()).toContain("资料通过");
  expect(wrapper.text()).toContain("资料驳回");
  expect(wrapper.text()).toContain("资料下线");
});

test("renders labels for submission and upload notifications", async () => {
  window.localStorage.setItem("one-stop-future-demo-notifications", JSON.stringify([
    {
      id: 2007,
      userId: 2,
      type: "RESOURCE_UPLOADED",
      title: "资料已提交审核",
      content: "你上传的资料《求职资料包》已提交审核。",
      read: false,
      createdAt: "2026-04-21T12:00:00",
      readAt: null,
    },
    {
      id: 2008,
      userId: 2,
      type: "RESUME_UPLOADED",
      title: "简历上传成功",
      content: "你的简历《后端实习简历》已上传成功。",
      read: false,
      createdAt: "2026-04-21T12:10:00",
      readAt: null,
    },
    {
      id: 2009,
      userId: 2,
      type: "JOB_APPLICATION_SUBMITTED",
      title: "岗位申请已提交",
      content: "你对「后端实习生」的申请已提交。",
      read: true,
      createdAt: "2026-04-21T12:20:00",
      readAt: "2026-04-21T12:30:00",
    },
    {
      id: 2010,
      userId: 2,
      type: "COMMUNITY_POST_COMMENTED",
      title: "你的帖子收到评论",
      content: "同学评论了你的帖子《面试复盘》。",
      read: true,
      createdAt: "2026-04-21T12:40:00",
      readAt: "2026-04-21T12:50:00",
    },
  ]));

  const { wrapper } = mountView();
  await flushPromises();

  expect(wrapper.text()).toContain("资料上传");
  expect(wrapper.text()).toContain("简历上传");
  expect(wrapper.text()).toContain("岗位申请");
  expect(wrapper.text()).toContain("帖子评论");
});
