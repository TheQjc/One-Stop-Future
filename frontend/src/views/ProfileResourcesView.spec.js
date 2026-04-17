import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, expect, test, vi } from "vitest";
import ProfileResourcesView from "./ProfileResourcesView.vue";
import { getMyResources, previewResource, previewZipResource } from "../api/resources.js";

vi.mock("../api/resources.js", () => ({
  getMyResources: vi.fn(),
  previewResource: vi.fn(),
  previewZipResource: vi.fn(),
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

test("profile resources map preview labels from previewKind", async () => {
  getMyResources.mockResolvedValue({
    total: 2,
    resources: [
      {
        id: 3,
        title: "Rejected pack",
        status: "REJECTED",
        category: "RESUME_TEMPLATE",
        editable: true,
        previewAvailable: true,
        previewKind: "FILE",
      },
      {
        id: 4,
        title: "Archive bundle",
        status: "PUBLISHED",
        category: "INTERVIEW_EXPERIENCE",
        editable: false,
        previewAvailable: true,
        previewKind: "ZIP_TREE",
      },
    ],
  });
  previewResource.mockResolvedValue("blob:resource-preview");
  previewZipResource.mockResolvedValue({
    resourceId: 4,
    fileName: "archive.zip",
    entryCount: 1,
    entries: [
      { path: "backend/questions.md", name: "questions.md", directory: false, size: 1834 },
    ],
  });

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
  expect(wrapper.text()).toContain("Preview");
  expect(wrapper.text()).toContain("Preview Contents");

  await wrapper.find('[data-testid="preview-action-3"]').trigger("click");
  await flushPromises();

  expect(previewResource).toHaveBeenCalledWith(3);

  await wrapper.find('[data-testid="preview-action-4"]').trigger("click");
  await flushPromises();

  expect(previewZipResource).toHaveBeenCalledWith(4);
  expect(wrapper.text()).toContain("backend/questions.md");
});

test("docx resources still do not expose preview actions", async () => {
  getMyResources.mockResolvedValue({
    total: 1,
    resources: [
      {
        id: 5,
        title: "Workbook",
        status: "PENDING",
        category: "OTHER",
        previewAvailable: false,
        previewKind: "NONE",
      },
    ],
  });

  const wrapper = mount(ProfileResourcesView);
  await flushPromises();

  expect(wrapper.find(".preview-action").exists()).toBe(false);
});
