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

test("resource detail shows a preview action when preview is available", async () => {
  getResourceDetail.mockResolvedValue({
    ...baseDetail,
    fileName: "resume-preview.pdf",
    fileExt: "pdf",
    contentType: "application/pdf",
    previewAvailable: true,
  });
  previewResource.mockResolvedValue("blob:resource-preview");

  const wrapper = mountView();
  await flushPromises();

  expect(wrapper.text()).toContain("Preview PDF");

  await wrapper.find('[data-testid="preview-action"]').trigger("click");
  await flushPromises();

  expect(previewResource).toHaveBeenCalledWith(11);
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
