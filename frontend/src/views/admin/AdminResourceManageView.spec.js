import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, expect, test, vi } from "vitest";
import AdminResourceManageView from "./AdminResourceManageView.vue";
import {
  getAdminResources,
  offlineAdminResource,
  publishAdminResource,
  rejectAdminResource,
} from "../../api/admin.js";
import { previewResource, previewZipResource } from "../../api/resources.js";

vi.mock("../../api/admin.js", () => ({
  getAdminResources: vi.fn(),
  offlineAdminResource: vi.fn(),
  publishAdminResource: vi.fn(),
  rejectAdminResource: vi.fn(),
}));

vi.mock("../../api/resources.js", () => ({
  previewResource: vi.fn(),
  previewZipResource: vi.fn(),
}));

const pendingResource = {
  id: 41,
  title: "Pending archive",
  uploaderNickname: "普通同学",
  status: "PENDING",
};

const publishedResource = {
  id: 41,
  title: "Pending archive",
  uploaderNickname: "普通同学",
  status: "PUBLISHED",
};

beforeEach(() => {
  vi.clearAllMocks();
  window.localStorage.clear();
});

test("publishes a pending resource and reloads the board", async () => {
  getAdminResources
    .mockResolvedValueOnce({
      total: 1,
      resources: [{ ...pendingResource }],
    })
    .mockResolvedValueOnce({
      total: 1,
      resources: [{ ...publishedResource }],
    });
  publishAdminResource.mockResolvedValue({
    ...publishedResource,
  });

  const wrapper = mount(AdminResourceManageView);
  await flushPromises();

  expect(wrapper.text()).toContain("Pending archive");

  await wrapper.find(".publish-action").trigger("click");
  await flushPromises();

  expect(publishAdminResource).toHaveBeenCalledWith(41);
  expect(getAdminResources).toHaveBeenCalledTimes(2);
});

test("rejects a selected pending resource with a reason", async () => {
  getAdminResources
    .mockResolvedValueOnce({
      total: 1,
      resources: [{ ...pendingResource }],
    })
    .mockResolvedValueOnce({
      total: 1,
      resources: [{ ...pendingResource, status: "REJECTED", rejectReason: "Need a clearer title" }],
    });
  rejectAdminResource.mockResolvedValue({
    ...pendingResource,
    status: "REJECTED",
    rejectReason: "Need a clearer title",
  });

  const wrapper = mount(AdminResourceManageView);
  await flushPromises();

  await wrapper.find(".select-action").trigger("click");
  await wrapper.find('textarea[name="rejectReason"]').setValue("Need a clearer title");
  await wrapper.find(".reject-action").trigger("click");
  await flushPromises();

  expect(rejectAdminResource).toHaveBeenCalledWith(41, { reason: "Need a clearer title" });
  expect(getAdminResources).toHaveBeenCalledTimes(2);
});

test("admin selected panel uses Preview for FILE resources", async () => {
  getAdminResources.mockResolvedValue({
    total: 1,
    resources: [
      {
        id: 41,
        title: "Pending archive",
        uploaderNickname: "普通同学",
        status: "PENDING",
        previewAvailable: true,
        previewKind: "FILE",
      },
    ],
  });
  previewResource.mockResolvedValue("blob:resource-preview");

  const wrapper = mount(AdminResourceManageView);
  await flushPromises();

  await wrapper.find(".select-action").trigger("click");
  await flushPromises();

  expect(wrapper.text()).toContain("预览");

  await wrapper.find('[data-testid="selected-preview-action"]').trigger("click");
  await flushPromises();

  expect(previewResource).toHaveBeenCalledWith(41);
});

test("admin selected panel shows Preview Contents for ZIP resources and renders inline tree", async () => {
  getAdminResources.mockResolvedValue({
    total: 1,
    resources: [
      {
        id: 41,
        title: "Archive bundle",
        uploaderNickname: "普通同学",
        status: "PENDING",
        previewAvailable: true,
        previewKind: "ZIP_TREE",
      },
    ],
  });
  previewZipResource.mockResolvedValue({
    resourceId: 41,
    fileName: "archive.zip",
    entryCount: 1,
    entries: [
      { path: "backend/questions.md", name: "questions.md", directory: false, size: 1834 },
    ],
  });

  const wrapper = mount(AdminResourceManageView);
  await flushPromises();

  await wrapper.find(".select-action").trigger("click");
  await flushPromises();

  expect(wrapper.text()).toContain("查看目录");

  await wrapper.find('[data-testid="selected-preview-action"]').trigger("click");
  await flushPromises();

  expect(previewZipResource).toHaveBeenCalledWith(41);
  expect(wrapper.text()).toContain("backend/questions.md");
});

test("docx resources use the generic FILE preview action in the selected panel", async () => {
  getAdminResources.mockResolvedValue({
    total: 1,
    resources: [
      {
        id: 41,
        title: "Workbook",
        uploaderNickname: "普通同学",
        status: "PENDING",
        fileName: "workbook.docx",
        previewAvailable: true,
        previewKind: "FILE",
      },
    ],
  });
  previewResource.mockResolvedValue("blob:docx-preview");

  const wrapper = mount(AdminResourceManageView);
  await flushPromises();

  await wrapper.find(".select-action").trigger("click");
  await flushPromises();

  expect(wrapper.find('[data-testid="selected-preview-action"]').exists()).toBe(true);
  expect(wrapper.text()).toContain("预览");
  expect(wrapper.text()).not.toContain("查看目录");

  await wrapper.find('[data-testid="selected-preview-action"]').trigger("click");
  await flushPromises();

  expect(previewResource).toHaveBeenCalledWith(41);
});
