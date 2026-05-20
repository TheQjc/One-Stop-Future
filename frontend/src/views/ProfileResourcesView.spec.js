import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, expect, test, vi } from "vitest";
import ProfileResourcesView from "./ProfileResourcesView.vue";
import { getMyResources, getResourceVersions, previewResource, previewZipResource } from "../api/resources.js";

vi.mock("../api/resources.js", () => ({
  getMyResources: vi.fn(),
  getResourceVersions: vi.fn(),
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
  expect(wrapper.text()).toContain("待审核");
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

  expect(wrapper.text()).toContain("编辑并重新提交");
  expect(wrapper.find('[data-to="/resources/3/edit"]').exists()).toBe(true);
  expect(wrapper.text()).toContain("预览");
  expect(wrapper.text()).toContain("查看目录");

  await wrapper.find('[data-testid="preview-action-3"]').trigger("click");
  await flushPromises();

  expect(previewResource).toHaveBeenCalledWith(3);

  await wrapper.find('[data-testid="preview-action-4"]').trigger("click");
  await flushPromises();

  expect(previewZipResource).toHaveBeenCalledWith(4);
  expect(wrapper.text()).toContain("backend/questions.md");
});

test("docx resources use the generic FILE preview action", async () => {
  getMyResources.mockResolvedValue({
    total: 1,
    resources: [
      {
        id: 5,
        title: "Workbook",
        status: "PENDING",
        category: "OTHER",
        fileName: "workbook.docx",
        previewAvailable: true,
        previewKind: "FILE",
      },
    ],
  });
  previewResource.mockResolvedValue("blob:docx-preview");

  const wrapper = mount(ProfileResourcesView);
  await flushPromises();

  expect(wrapper.find('[data-testid="preview-action-5"]').exists()).toBe(true);
  expect(wrapper.text()).toContain("预览");
  expect(wrapper.text()).not.toContain("查看目录");

  await wrapper.find('[data-testid="preview-action-5"]').trigger("click");
  await flushPromises();

  expect(previewResource).toHaveBeenCalledWith(5);
});

test("loads and renders resource version history", async () => {
  getMyResources.mockResolvedValue({
    total: 1,
    resources: [
      {
        id: 9,
        title: "Versioned Pack",
        status: "PENDING",
        category: "RESUME_TEMPLATE",
      },
    ],
  });
  getResourceVersions.mockResolvedValue({
    total: 1,
    versions: [
      {
        id: 101,
        versionNo: 1,
        changeType: "UPLOAD",
        title: "Versioned Pack",
        fileName: "versioned-pack.pdf",
        createdAt: "2026-04-18T10:00:00",
      },
    ],
  });

  const wrapper = mount(ProfileResourcesView);
  await flushPromises();

  await wrapper.find('[data-testid="versions-action-9"]').trigger("click");
  await flushPromises();

  expect(getResourceVersions).toHaveBeenCalledWith(9);
  expect(wrapper.text()).toContain("版本历史");
  expect(wrapper.text()).toContain("v1");
  expect(wrapper.text()).toContain("versioned-pack.pdf");
});

test("ignores a stale version history response after switching resources", async () => {
  getMyResources.mockResolvedValue({
    total: 2,
    resources: [
      {
        id: 9,
        title: "Versioned Pack",
        status: "PENDING",
        category: "RESUME_TEMPLATE",
      },
      {
        id: 10,
        title: "Fresh Pack",
        status: "PENDING",
        category: "RESUME_TEMPLATE",
      },
    ],
  });

  let resolveFirstRequest;
  getResourceVersions
    .mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          resolveFirstRequest = resolve;
        }),
    )
    .mockResolvedValueOnce({
      total: 1,
      versions: [
        {
          id: 202,
          versionNo: 2,
          changeType: "UPDATE",
          title: "Fresh Pack",
          fileName: "fresh-pack.pdf",
          createdAt: "2026-04-18T11:00:00",
        },
      ],
    });

  const wrapper = mount(ProfileResourcesView);
  await flushPromises();

  await wrapper.find('[data-testid="versions-action-9"]').trigger("click");
  await wrapper.find('[data-testid="versions-action-10"]').trigger("click");
  await flushPromises();

  resolveFirstRequest({
    total: 1,
    versions: [
      {
        id: 101,
        versionNo: 1,
        changeType: "UPLOAD",
        title: "Versioned Pack",
        fileName: "versioned-pack.pdf",
        createdAt: "2026-04-18T10:00:00",
      },
    ],
  });
  await flushPromises();

  expect(wrapper.text()).toContain("Fresh Pack");
  expect(wrapper.text()).toContain("fresh-pack.pdf");
  expect(wrapper.text()).not.toContain("versioned-pack.pdf");
});
