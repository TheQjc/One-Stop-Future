import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, expect, test, vi } from "vitest";
import ProfileResourcesView from "./ProfileResourcesView.vue";
import { getMyResources } from "../api/resources.js";

vi.mock("../api/resources.js", () => ({
  getMyResources: vi.fn(),
}));

beforeEach(() => {
  vi.clearAllMocks();
  window.localStorage.clear();
});

test("loads and renders the current user's resources", async () => {
  getMyResources.mockResolvedValue({
    total: 2,
    resources: [
      {
        id: 1,
        title: "Resume Template Pack",
        status: "PENDING",
        category: "RESUME_TEMPLATE",
      },
      {
        id: 2,
        title: "Interview Notes",
        status: "PUBLISHED",
        category: "INTERVIEW_EXPERIENCE",
      },
    ],
  });

  const wrapper = mount(ProfileResourcesView);
  await flushPromises();

  expect(getMyResources).toHaveBeenCalledTimes(1);
  expect(wrapper.text()).toContain("Resume Template Pack");
  expect(wrapper.text()).toContain("Interview Notes");
  expect(wrapper.text()).toContain("PENDING");
});
