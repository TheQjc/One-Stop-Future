import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { beforeEach, expect, test, vi } from "vitest";
import CommunityDetailView from "./CommunityDetailView.vue";
import { useUserStore } from "../stores/user.js";
import {
  createCommunityComment,
  createCommunityReply,
  favoriteCommunityPost,
  getCommunityPostDetail,
  likeCommunityPost,
  unfavoriteCommunityPost,
  unlikeCommunityPost,
} from "../api/community.js";

const routeMock = {
  params: { id: "11" },
  fullPath: "/community/11",
};

const routerPush = vi.fn();

vi.mock("vue-router", async () => {
  const actual = await vi.importActual("vue-router");

  return {
    ...actual,
    useRoute: () => routeMock,
    useRouter: () => ({
      push: routerPush,
    }),
  };
});

vi.mock("../api/community.js", () => ({
  createCommunityComment: vi.fn(),
  createCommunityReply: vi.fn(),
  favoriteCommunityPost: vi.fn(),
  getCommunityPostDetail: vi.fn(),
  likeCommunityPost: vi.fn(),
  unfavoriteCommunityPost: vi.fn(),
  unlikeCommunityPost: vi.fn(),
}));

const baseDetail = {
  id: 11,
  tag: "CAREER",
  title: "Offer decision notes",
  content: "Compare the role, timeline, and city before choosing.",
  createdAt: "2026-04-15T10:00:00",
  likeCount: 0,
  commentCount: 0,
  favoriteCount: 0,
  likedByMe: false,
  favoritedByMe: false,
  author: {
    nickname: "Alice",
  },
  experience: {
    enabled: false,
    targetLabel: null,
    outcomeLabel: null,
    timelineSummary: null,
    actionSummary: null,
  },
  comments: [],
};

beforeEach(() => {
  vi.clearAllMocks();
  window.localStorage.clear();
  getCommunityPostDetail.mockResolvedValue({ ...baseDetail });
});

function mountView(authenticated = false) {
  setActivePinia(createPinia());
  const userStore = useUserStore();

  if (authenticated) {
    userStore.token = "demo-token";
    userStore.persistProfile({
      id: 7,
      userId: 7,
      nickname: "SignedInUser",
      role: "USER",
      verificationStatus: "VERIFIED",
    });
  }

  return mount(CommunityDetailView, {
    global: {
      stubs: {
        RouterLink: {
          props: ["to"],
          template: "<a :data-to='to'><slot /></a>",
        },
      },
    },
  });
}

test("redirects guests to login when they try to like a post", async () => {
  const wrapper = mountView();
  await flushPromises();

  expect(getCommunityPostDetail).toHaveBeenCalledWith("11");

  await wrapper.find("button.app-link").trigger("click");

  expect(routerPush).toHaveBeenCalledWith({
    name: "login",
    query: { redirect: "/community/11" },
  });
  expect(likeCommunityPost).not.toHaveBeenCalled();
});

test("submits a comment for authenticated users", async () => {
  createCommunityComment.mockResolvedValue({
    ...baseDetail,
    commentCount: 1,
    comments: [
      {
        id: 100,
        authorId: 7,
        authorNickname: "SignedInUser",
        content: "Useful comparison.",
        status: "VISIBLE",
        createdAt: "2026-04-15T11:00:00",
        mine: true,
        replies: [],
      },
    ],
  });

  const wrapper = mountView(true);
  await flushPromises();

  await wrapper.find('textarea[name="comment"]').setValue("Useful comparison.");
  await wrapper.find("form.community-detail__comment-form").trigger("submit.prevent");
  await flushPromises();

  expect(createCommunityComment).toHaveBeenCalledWith(11, {
    content: "Useful comparison.",
  });
  expect(wrapper.find('textarea[name="comment"]').element.value).toBe("");
  expect(wrapper.text()).toContain("Useful comparison.");
});

test("renders the structured experience summary block when available", async () => {
  getCommunityPostDetail.mockResolvedValue({
    ...baseDetail,
    experience: {
      enabled: true,
      targetLabel: "Backend internship sprint",
      outcomeLabel: "Received 2 interview invitations",
      timelineSummary: "Week 1 resume refresh, week 2 projects",
      actionSummary: "Refine one showcase project, then batch tailored applications.",
    },
  });

  const wrapper = mountView();
  await flushPromises();

  expect(wrapper.text()).toContain("经验摘要");
  expect(wrapper.text()).toContain("Backend internship sprint");
  expect(wrapper.text()).toContain("Received 2 interview invitations");
  expect(wrapper.text()).toContain("Week 1 resume refresh, week 2 projects");
});

test("renders nested replies under the matching top-level comment", async () => {
  getCommunityPostDetail.mockResolvedValue({
    ...baseDetail,
    comments: [
      {
        id: 100,
        authorId: 2,
        authorNickname: "Alice",
        content: "Top-level note",
        status: "VISIBLE",
        createdAt: "2026-04-15T11:00:00",
        mine: false,
        replies: [
          {
            id: 101,
            authorId: 3,
            authorNickname: "Bob",
            replyToUserId: 2,
            replyToUserNickname: "Alice",
            content: "Nested follow-up",
            status: "VISIBLE",
            createdAt: "2026-04-15T11:05:00",
            mine: false,
          },
        ],
      },
    ],
  });

  const wrapper = mountView(true);
  await flushPromises();

  expect(wrapper.text()).toContain("Top-level note");
  expect(wrapper.text()).toContain("回复给 Alice");
  expect(wrapper.text()).toContain("Nested follow-up");
});

test("authenticated users can expand and collapse an inline reply form", async () => {
  getCommunityPostDetail.mockResolvedValue({
    ...baseDetail,
    comments: [
      {
        id: 100,
        authorId: 2,
        authorNickname: "Alice",
        content: "Top-level note",
        status: "VISIBLE",
        createdAt: "2026-04-15T11:00:00",
        mine: false,
        replies: [],
      },
    ],
  });

  const wrapper = mountView(true);
  await flushPromises();

  await wrapper.find('[data-reply-trigger="100"]').trigger("click");
  expect(wrapper.find('[data-reply-field="100"]').exists()).toBe(true);

  await wrapper.find('[data-reply-trigger="100"]').trigger("click");
  expect(wrapper.find('[data-reply-field="100"]').exists()).toBe(false);
});

test("submits a reply and refreshes the nested thread", async () => {
  getCommunityPostDetail.mockResolvedValue({
    ...baseDetail,
    comments: [
      {
        id: 100,
        authorId: 2,
        authorNickname: "Alice",
        content: "Top-level note",
        status: "VISIBLE",
        createdAt: "2026-04-15T11:00:00",
        mine: false,
        replies: [],
      },
    ],
  });

  createCommunityReply.mockResolvedValue({
    ...baseDetail,
    commentCount: 2,
    comments: [
      {
        id: 100,
        authorId: 2,
        authorNickname: "Alice",
        content: "Top-level note",
        status: "VISIBLE",
        createdAt: "2026-04-15T11:00:00",
        mine: false,
        replies: [
          {
            id: 101,
            authorId: 7,
            authorNickname: "SignedInUser",
            replyToUserId: 2,
            replyToUserNickname: "Alice",
            content: "Helpful follow-up reply.",
            status: "VISIBLE",
            createdAt: "2026-04-15T11:05:00",
            mine: true,
          },
        ],
      },
    ],
  });

  const wrapper = mountView(true);
  await flushPromises();

  await wrapper.find('[data-reply-trigger="100"]').trigger("click");
  await wrapper.find('[data-reply-field="100"]').setValue("Helpful follow-up reply.");
  await wrapper.find('[data-reply-form="100"]').trigger("submit.prevent");
  await flushPromises();

  expect(createCommunityReply).toHaveBeenCalledWith(100, {
    content: "Helpful follow-up reply.",
  });
  expect(wrapper.text()).toContain("Helpful follow-up reply.");
  expect(wrapper.find('[data-reply-field="100"]').exists()).toBe(false);
});
