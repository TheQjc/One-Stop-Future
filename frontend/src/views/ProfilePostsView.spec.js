import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, expect, test, vi } from "vitest";
import ProfilePostsView from "./ProfilePostsView.vue";
import { getMyCommunityPosts } from "../api/community.js";

vi.mock("../api/community.js", () => ({
  getMyCommunityPosts: vi.fn(),
}));

beforeEach(() => {
  vi.clearAllMocks();
  window.localStorage.clear();
});

test("loads and renders the current user's posts", async () => {
  getMyCommunityPosts.mockResolvedValue({
    total: 1,
    posts: [
      {
        id: 1,
        title: "My planning memo",
      },
    ],
  });

  const wrapper = mount(ProfilePostsView, {
    global: {
      stubs: {
        CommunityPostCard: {
          props: ["post"],
          template: "<article class='stub-post'>{{ post.title }}</article>",
        },
      },
    },
  });

  await flushPromises();

  expect(getMyCommunityPosts).toHaveBeenCalledTimes(1);
  expect(wrapper.findAll(".stub-post")).toHaveLength(1);
  expect(wrapper.text()).toContain("My planning memo");
});
