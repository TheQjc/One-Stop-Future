import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, expect, test, vi } from "vitest";
import AdminCommunityManageView from "./AdminCommunityManageView.vue";
import {
  deleteCommunityPost,
  getAdminCommunityPosts,
  hideCommunityPost,
} from "../../api/admin.js";

vi.mock("../../api/admin.js", () => ({
  deleteCommunityPost: vi.fn(),
  getAdminCommunityPosts: vi.fn(),
  hideCommunityPost: vi.fn(),
}));

const publishedPost = {
  id: 21,
  title: "Moderation sample",
  tag: "CAREER",
  authorNickname: "CampusAdmin",
  status: "PUBLISHED",
  likeCount: 2,
  commentCount: 1,
  favoriteCount: 3,
};

beforeEach(() => {
  vi.clearAllMocks();
  window.localStorage.clear();
});

test("hides a published post and reloads the governance list", async () => {
  getAdminCommunityPosts
    .mockResolvedValueOnce({
      total: 1,
      posts: [{ ...publishedPost }],
    })
    .mockResolvedValueOnce({
      total: 1,
      posts: [{ ...publishedPost, status: "HIDDEN" }],
    });
  hideCommunityPost.mockResolvedValue(true);

  const wrapper = mount(AdminCommunityManageView);
  await flushPromises();

  expect(wrapper.text()).toContain("Moderation sample");

  await wrapper.find("button.ghost-btn").trigger("click");
  await flushPromises();

  expect(hideCommunityPost).toHaveBeenCalledWith(21);
  expect(getAdminCommunityPosts).toHaveBeenCalledTimes(2);
});

test("deletes a post and reloads the governance list", async () => {
  getAdminCommunityPosts
    .mockResolvedValueOnce({
      total: 1,
      posts: [{ ...publishedPost, id: 22, title: "Delete sample" }],
    })
    .mockResolvedValueOnce({
      total: 1,
      posts: [{ ...publishedPost, id: 22, title: "Delete sample", status: "DELETED" }],
    });
  deleteCommunityPost.mockResolvedValue(true);

  const wrapper = mount(AdminCommunityManageView);
  await flushPromises();

  await wrapper.find("button.danger-btn").trigger("click");
  await flushPromises();

  expect(deleteCommunityPost).toHaveBeenCalledWith(22);
  expect(getAdminCommunityPosts).toHaveBeenCalledTimes(2);
});
