import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, expect, test, vi } from "vitest";
import ProfileResourcesView from "./ProfileResourcesView.vue";
import { getMyResources, previewResource } from "../api/resources.js";

vi.mock("../api/resources.js", () => ({
  getMyResources: vi.fn(),
  previewResource: vi.fn(),
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

test("profile resources show edit and preview actions from lifecycle flags", async () => {
  getMyResources.mockResolvedValue({
    total: 1,
    resources: [
      {
        id: 3,
        title: "Rejected pack",
        status: "REJECTED",
        category: "RESUME_TEMPLATE",
        editable: true,
        previewAvailable: true,
      },
    ],
  });
  previewResource.mockResolvedValue("blob:resource-preview");

  const wrapper = mount(ProfileResourcesView, {
    global: {
      stubs: {
        RouterLink: {
          props: ["to"],
          template: "<a :data-to='to'><slot /></a>",
        },
      },
    },
  });
  await flushPromises();

  expect(wrapper.text()).toContain("Edit And Resubmit");
  expect(wrapper.find('[data-to="/resources/3/edit"]').exists()).toBe(true);
  expect(wrapper.text()).toContain("Preview PDF");

  await wrapper.find(".preview-action").trigger("click");
  await flushPromises();

  expect(previewResource).toHaveBeenCalledWith(3);
});
