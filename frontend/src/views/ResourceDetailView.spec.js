import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { beforeEach, expect, test, vi } from "vitest";
import ResourceDetailView from "./ResourceDetailView.vue";
import { useUserStore } from "../stores/user.js";
import {
  downloadResource,
  favoriteResource,
  getResourceDetail,
  previewResource,
  previewZipResource,
  unfavoriteResource,
} from "../api/resources.js";

const routeMock = {
  params: { id: "11" },
  fullPath: "/resources/11",
};

const routerPush = vi.fn();

vi.mock("vue-router", async () => {
  const actual = await vi.importActual("vue-router");

  return {
    ...actual,
    useRoute: () => routeMock,
    useRouter: () => ({
      push: routerPush,
    }),
  };
});

vi.mock("../api/resources.js", () => ({
  downloadResource: vi.fn(),
  favoriteResource: vi.fn(),
  getResourceDetail: vi.fn(),
  previewResource: vi.fn(),
  previewZipResource: vi.fn(),
  unfavoriteResource: vi.fn(),
}));

const baseDetail = {
  id: 11,
  title: "Interview Archive Bundle",
  category: "INTERVIEW_EXPERIENCE",
  summary: "A compact archive of interview notes.",
  description: "Detailed note set.",
  fileName: "interview-archive.zip",
  fileExt: "zip",
  contentType: "application/zip",
  fileSize: 2048,
  downloadCount: 5,
  favoriteCount: 1,
  uploaderNickname: "ArchiveEditor",
  publishedAt: "2026-04-16T10:00:00",
  favoritedByMe: false,
};

beforeEach(() => {
  vi.clearAllMocks();
  window.localStorage.clear();
  getResourceDetail.mockResolvedValue({ ...baseDetail });
});

function mountView(authenticated = false) {
  setActivePinia(createPinia());
  const userStore = useUserStore();

  if (authenticated) {
    userStore.token = "demo-token";
    userStore.persistProfile({
      id: 7,
      userId: 7,
      nickname: "SignedInUser",
      role: "USER",
      verificationStatus: "VERIFIED",
    });
  }

  return mount(ResourceDetailView, {
    global: {
      stubs: {
        RouterLink: {
          props: ["to"],
          template: "<a :data-to='to'><slot /></a>",
        },
      },
    },
  });
}

test("redirects guests to login when they try to download a resource", async () => {
  const wrapper = mountView();
  await flushPromises();

  expect(getResourceDetail).toHaveBeenCalledWith("11");
  expect(wrapper.text()).toContain("Interview Archive Bundle");

  await wrapper.find('[data-testid="download-action"]').trigger("click");

  expect(routerPush).toHaveBeenCalledWith({
    name: "login",
    query: { redirect: "/resources/11" },
  });
  expect(downloadResource).not.toHaveBeenCalled();
});

test("resource detail shows Preview for FILE resources and opens blob preview", async () => {
  getResourceDetail.mockResolvedValue({
    ...baseDetail,
    fileName: "resume-preview.pdf",
    fileExt: "pdf",
    contentType: "application/pdf",
    previewAvailable: true,
    previewKind: "FILE",
  });
  previewResource.mockResolvedValue("blob:resource-preview");

  const wrapper = mountView();
  await flushPromises();

  expect(wrapper.text()).toContain("Preview");
  expect(wrapper.text()).not.toContain("Preview PDF");

  await wrapper.find('[data-testid="preview-action"]').trigger("click");
  await flushPromises();

  expect(previewResource).toHaveBeenCalledWith(11);
});

test("resource detail shows Preview Contents for ZIP resources and loads the tree inline", async () => {
  getResourceDetail.mockResolvedValue({
    ...baseDetail,
    previewAvailable: true,
    previewKind: "ZIP_TREE",
  });
  previewZipResource.mockResolvedValue({
    resourceId: 11,
    fileName: "interview-archive.zip",
    entryCount: 2,
    entries: [
      { path: "backend/", name: "backend", directory: true, size: null },
      { path: "backend/questions.md", name: "questions.md", directory: false, size: 1834 },
    ],
  });

  const wrapper = mountView();
  await flushPromises();

  expect(wrapper.text()).toContain("Preview Contents");

  await wrapper.find('[data-testid="preview-action"]').trigger("click");
  await flushPromises();

  expect(previewZipResource).toHaveBeenCalledWith(11);
  expect(wrapper.text()).toContain("backend/questions.md");
});

test("resource detail does not show preview action for NONE preview kind", async () => {
  getResourceDetail.mockResolvedValue({
    ...baseDetail,
    fileName: "interview-notes.docx",
    fileExt: "docx",
    contentType: "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    previewAvailable: false,
    previewKind: "NONE",
  });

  const wrapper = mountView();
  await flushPromises();

  expect(wrapper.find('[data-testid="preview-action"]').exists()).toBe(false);
});

test("favorites and downloads a resource for authenticated users", async () => {
  favoriteResource.mockResolvedValue({
    ...baseDetail,
    favoritedByMe: true,
  });
  downloadResource.mockResolvedValue("interview-archive.zip");

  const wrapper = mountView(true);
  await flushPromises();

  await wrapper.find('[data-testid="favorite-toggle"]').trigger("click");
  await flushPromises();

  expect(favoriteResource).toHaveBeenCalledWith(11);
  expect(wrapper.text()).toContain("Remove From Collection");

  await wrapper.find('[data-testid="download-action"]').trigger("click");
  await flushPromises();

  expect(downloadResource).toHaveBeenCalledWith(11);
  expect(unfavoriteResource).not.toHaveBeenCalled();
});
