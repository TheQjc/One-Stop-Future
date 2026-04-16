import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, expect, test, vi } from "vitest";
import ProfileFavoritesView from "./ProfileFavoritesView.vue";
import { getMyPostFavorites } from "../api/community.js";
import { getMyJobFavorites } from "../api/jobs.js";
import { getMyResourceFavorites } from "../api/resources.js";

vi.mock("../api/community.js", () => ({
  getMyPostFavorites: vi.fn(),
}));

vi.mock("../api/jobs.js", () => ({
  getMyJobFavorites: vi.fn(),
}));

vi.mock("../api/resources.js", () => ({
  getMyResourceFavorites: vi.fn(),
}));

beforeEach(() => {
  vi.clearAllMocks();
  window.localStorage.clear();
});

test("loads and renders the current user's favorite posts", async () => {
  getMyPostFavorites.mockResolvedValue({
    total: 1,
    posts: [
      {
        id: 9,
        title: "Saved forum note",
      },
    ],
  });

  const wrapper = mount(ProfileFavoritesView, {
    global: {
      stubs: {
        CommunityPostCard: {
          props: ["post"],
          template: "<article class='stub-post'>{{ post.title }}</article>",
        },
        JobPostingCard: {
          props: ["job"],
          template: "<article class='stub-job'>{{ job.title }}</article>",
        },
        ResourceCard: {
          props: ["resource"],
          template: "<article class='stub-resource'>{{ resource.title }}</article>",
        },
      },
    },
  });

  await flushPromises();

  expect(getMyPostFavorites).toHaveBeenCalledTimes(1);
  expect(wrapper.findAll(".stub-post")).toHaveLength(1);
  expect(wrapper.text()).toContain("Saved forum note");
});

test("switches to the jobs collection", async () => {
  getMyPostFavorites.mockResolvedValue({
    total: 0,
    posts: [],
  });
  getMyJobFavorites.mockResolvedValue({
    total: 1,
    jobs: [
      {
        id: 19,
        title: "Saved job card",
      },
    ],
  });

  const wrapper = mount(ProfileFavoritesView, {
    global: {
      stubs: {
        CommunityPostCard: {
          props: ["post"],
          template: "<article class='stub-post'>{{ post.title }}</article>",
        },
        JobPostingCard: {
          props: ["job"],
          template: "<article class='stub-job'>{{ job.title }}</article>",
        },
        ResourceCard: {
          props: ["resource"],
          template: "<article class='stub-resource'>{{ resource.title }}</article>",
        },
      },
    },
  });

  await flushPromises();
  await wrapper.findAll("button.ghost-btn")[1].trigger("click");
  await flushPromises();

  expect(getMyJobFavorites).toHaveBeenCalledTimes(1);
  expect(wrapper.findAll(".stub-job")).toHaveLength(1);
  expect(wrapper.text()).toContain("Saved job card");
});

test("switches to the resources collection", async () => {
  getMyPostFavorites.mockResolvedValue({
    total: 0,
    posts: [],
  });
  getMyResourceFavorites.mockResolvedValue({
    total: 1,
    resources: [
      {
        id: 29,
        title: "Saved resource card",
      },
    ],
  });

  const wrapper = mount(ProfileFavoritesView, {
    global: {
      stubs: {
        CommunityPostCard: {
          props: ["post"],
          template: "<article class='stub-post'>{{ post.title }}</article>",
        },
        JobPostingCard: {
          props: ["job"],
          template: "<article class='stub-job'>{{ job.title }}</article>",
        },
        ResourceCard: {
          props: ["resource"],
          template: "<article class='stub-resource'>{{ resource.title }}</article>",
        },
      },
    },
  });

  await flushPromises();
  await wrapper.findAll("button.ghost-btn")[2].trigger("click");
  await flushPromises();

  expect(getMyResourceFavorites).toHaveBeenCalledTimes(1);
  expect(wrapper.findAll(".stub-resource")).toHaveLength(1);
  expect(wrapper.text()).toContain("Saved resource card");
});
