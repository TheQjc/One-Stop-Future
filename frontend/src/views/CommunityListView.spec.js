import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { beforeEach, expect, test, vi } from "vitest";
import CommunityListView from "./CommunityListView.vue";
import { getCommunityHotPosts, getCommunityPosts } from "../api/community.js";

vi.mock("../api/community.js", () => ({
  getCommunityHotPosts: vi.fn(),
  getCommunityPosts: vi.fn(),
}));

beforeEach(() => {
  vi.clearAllMocks();
  window.localStorage.clear();
});

function mountView() {
  setActivePinia(createPinia());

  return mount(CommunityListView, {
    global: {
      stubs: {
        RouterLink: {
          props: ["to"],
          template: "<a :data-to='to'><slot /></a>",
        },
        CommunityFilterTabs: {
          props: ["modelValue", "options"],
          emits: ["update:modelValue"],
          template: `
            <div class="stub-tabs">
              <button
                type="button"
                class="filter-career"
                @click="$emit('update:modelValue', 'CAREER')"
              >
                career
              </button>
            </div>
          `,
        },
      },
    },
  });
}

test("loads posts and refetches when the tag filter changes", async () => {
  getCommunityHotPosts.mockResolvedValue({
    period: "WEEK",
    total: 1,
    items: [{ id: 10, title: "Hot board leader", hotLabel: "Weekly discussion", hotScore: 15 }],
  });
  getCommunityPosts
    .mockResolvedValueOnce({
      total: 1,
      posts: [{
        id: 1,
        tag: "ABROAD",
        title: "First post",
        contentPreview: "Language prep recap",
        status: "PUBLISHED",
        authorNickname: "Alice",
        likeCount: 2,
        commentCount: 1,
        favoriteCount: 0,
        createdAt: "2026-04-15T10:00:00",
        experience: {
          enabled: true,
          targetLabel: "IELTS 7.5 sprint",
          outcomeLabel: "Score improved in six weeks",
          timelineSummary: "Week 1 basics, week 2 drills",
          actionSummary: "Track mistakes every day",
        },
      }],
    })
    .mockResolvedValueOnce({
      total: 1,
      posts: [{
        id: 2,
        tag: "CAREER",
        title: "Career post",
        contentPreview: "Plain summary",
        status: "PUBLISHED",
        authorNickname: "Bob",
        likeCount: 0,
        commentCount: 0,
        favoriteCount: 0,
        createdAt: "2026-04-16T10:00:00",
        experience: {
          enabled: false,
          targetLabel: null,
          outcomeLabel: null,
          timelineSummary: null,
          actionSummary: null,
        },
      }],
    });

  const wrapper = mountView();
  await flushPromises();

  expect(getCommunityHotPosts).toHaveBeenCalledWith({ period: "WEEK", limit: 3 });
  expect(getCommunityPosts).toHaveBeenNthCalledWith(1, {});
  expect(wrapper.text()).toContain("Hot board leader");
  expect(wrapper.text()).toContain("First post");
  expect(wrapper.text()).toContain("Experience Post");
  expect(wrapper.text()).toContain("IELTS 7.5 sprint");

  await wrapper.find(".filter-career").trigger("click");
  await flushPromises();

  expect(getCommunityPosts).toHaveBeenNthCalledWith(2, { tag: "CAREER" });
  expect(getCommunityHotPosts).toHaveBeenCalledTimes(1);
  expect(wrapper.text()).toContain("Career post");
});

test("switching the hot period refetches the hot board only", async () => {
  getCommunityHotPosts
    .mockResolvedValueOnce({
      period: "WEEK",
      total: 1,
      items: [{ id: 10, title: "Weekly leader", hotLabel: "Weekly discussion", hotScore: 15 }],
    })
    .mockResolvedValueOnce({
      period: "ALL",
      total: 1,
      items: [{ id: 11, title: "All time leader", hotLabel: "Sustained discussion", hotScore: 22 }],
    });
  getCommunityPosts.mockResolvedValue({
    total: 0,
    posts: [],
  });

  const wrapper = mountView();
  await flushPromises();

  await wrapper.find(".period-all").trigger("click");
  await flushPromises();

  expect(getCommunityHotPosts).toHaveBeenNthCalledWith(2, { period: "ALL", limit: 3 });
  expect(getCommunityPosts).toHaveBeenCalledTimes(1);
  expect(wrapper.text()).toContain("All time leader");
});

test("hot board error state can retry", async () => {
  getCommunityHotPosts
    .mockRejectedValueOnce(new Error("Hot board failed."))
    .mockResolvedValueOnce({
      period: "WEEK",
      total: 1,
      items: [{ id: 12, title: "Recovered leader", hotLabel: "Weekly discussion", hotScore: 18 }],
    });
  getCommunityPosts.mockResolvedValue({
    total: 0,
    posts: [],
  });

  const wrapper = mountView();
  await flushPromises();

  expect(wrapper.text()).toContain("Hot board failed.");

  await wrapper.find(".retry-hot-board").trigger("click");
  await flushPromises();

  expect(getCommunityHotPosts).toHaveBeenCalledTimes(2);
  expect(wrapper.text()).toContain("Recovered leader");
});
