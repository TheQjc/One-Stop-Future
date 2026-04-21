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

test("submits structured experience fields when experience mode is enabled", async () => {
  createCommunityPost.mockResolvedValue({
    id: 88,
  });

  const wrapper = mount(CommunityCreateView);

  await wrapper.find('input[name="experience-post"]').setValue(true);
  await wrapper.find('input[name="title"]').setValue("Campus recruiting recap");
  await wrapper.find('textarea[name="content"]').setValue("Focus on project proof before pushing volume.");
  await wrapper.find('input[name="experience-target-label"]').setValue("  Backend internship sprint  ");
  await wrapper.find('input[name="experience-outcome-label"]').setValue("Received 2 interview invitations");
  await wrapper.find('textarea[name="experience-timeline-summary"]').setValue("Week 1 resume refresh, week 2 projects");
  await wrapper.find('textarea[name="experience-action-summary"]').setValue("Refine one showcase project, then batch tailored applications.");
  await wrapper.find("form").trigger("submit.prevent");
  await flushPromises();

  expect(createCommunityPost).toHaveBeenCalledWith({
    tag: "CAREER",
    title: "Campus recruiting recap",
    content: "Focus on project proof before pushing volume.",
    experiencePost: true,
    experienceTargetLabel: "Backend internship sprint",
    experienceOutcomeLabel: "Received 2 interview invitations",
    experienceTimelineSummary: "Week 1 resume refresh, week 2 projects",
    experienceActionSummary: "Refine one showcase project, then batch tailored applications.",
  });
  expect(routerPush).toHaveBeenCalledWith("/community/88");
});
