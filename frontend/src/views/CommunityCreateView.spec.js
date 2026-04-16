import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, expect, test, vi } from "vitest";
import CommunityCreateView from "./CommunityCreateView.vue";
import { createCommunityPost } from "../api/community.js";

const routerPush = vi.fn();

vi.mock("vue-router", () => ({
  useRouter: () => ({
    push: routerPush,
  }),
}));

vi.mock("../api/community.js", () => ({
  createCommunityPost: vi.fn(),
}));

beforeEach(() => {
  vi.clearAllMocks();
  window.localStorage.clear();
});

test("blocks empty submissions before calling the API", async () => {
  const wrapper = mount(CommunityCreateView);

  await wrapper.find("form").trigger("submit.prevent");
  await flushPromises();

  expect(createCommunityPost).not.toHaveBeenCalled();
  expect(wrapper.find('[role="alert"]').exists()).toBe(true);
});

test("creates a post and navigates to its detail page", async () => {
  createCommunityPost.mockResolvedValue({
    id: 55,
  });

  const wrapper = mount(CommunityCreateView);

  await wrapper.find('select[name="tag"]').setValue("ABROAD");
  await wrapper.find('input[name="title"]').setValue("Visa preparation checklist");
  await wrapper.find('textarea[name="content"]').setValue("Start with timeline and required documents.");
  await wrapper.find("form").trigger("submit.prevent");
  await flushPromises();

  expect(createCommunityPost).toHaveBeenCalledWith({
    tag: "ABROAD",
    title: "Visa preparation checklist",
    content: "Start with timeline and required documents.",
  });
  expect(routerPush).toHaveBeenCalledWith("/community/55");
});
