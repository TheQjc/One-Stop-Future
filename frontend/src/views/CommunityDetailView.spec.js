import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { beforeEach, expect, test, vi } from "vitest";
import CommunityDetailView from "./CommunityDetailView.vue";
import { useUserStore } from "../stores/user.js";
import {
  createCommunityComment,
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
        CommunityCommentList: {
          props: ["comments"],
          template: "<div class='comment-count'>{{ comments.length }}</div>",
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
        authorNickname: "SignedInUser",
        content: "Useful comparison.",
        createdAt: "2026-04-15T11:00:00",
      },
    ],
  });

  const wrapper = mountView(true);
  await flushPromises();

  await wrapper.find('textarea[name="comment"]').setValue("Useful comparison.");
  await wrapper.find("form").trigger("submit.prevent");
  await flushPromises();

  expect(createCommunityComment).toHaveBeenCalledWith(11, {
    content: "Useful comparison.",
  });
  expect(wrapper.find('textarea[name="comment"]').element.value).toBe("");
  expect(wrapper.find(".comment-count").text()).toBe("1");
});
