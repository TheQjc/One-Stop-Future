import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { beforeEach, expect, test, vi } from "vitest";
import CommunityListView from "./CommunityListView.vue";
import { getCommunityPosts } from "../api/community.js";

vi.mock("../api/community.js", () => ({
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
        CommunityPostCard: {
          props: ["post"],
          template: "<article class='stub-post'>{{ post.title }}</article>",
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
  getCommunityPosts
    .mockResolvedValueOnce({
      total: 1,
      posts: [{ id: 1, title: "First post" }],
    })
    .mockResolvedValueOnce({
      total: 1,
      posts: [{ id: 2, title: "Career post" }],
    });

  const wrapper = mountView();
  await flushPromises();

  expect(getCommunityPosts).toHaveBeenNthCalledWith(1, {});
  expect(wrapper.text()).toContain("First post");

  await wrapper.find(".filter-career").trigger("click");
  await flushPromises();

  expect(getCommunityPosts).toHaveBeenNthCalledWith(2, { tag: "CAREER" });
  expect(wrapper.text()).toContain("Career post");
});
