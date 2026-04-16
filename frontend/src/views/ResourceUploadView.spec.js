import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { beforeEach, expect, test, vi } from "vitest";
import ResourceUploadView from "./ResourceUploadView.vue";
import { useUserStore } from "../stores/user.js";
import { createResourceUpload } from "../api/resources.js";

const routerPush = vi.fn();

vi.mock("vue-router", async () => {
  const actual = await vi.importActual("vue-router");

  return {
    ...actual,
    useRouter: () => ({
      push: routerPush,
    }),
  };
});

vi.mock("../api/resources.js", () => ({
  createResourceUpload: vi.fn(),
}));

beforeEach(() => {
  vi.clearAllMocks();
  window.localStorage.clear();
  createResourceUpload.mockResolvedValue({
    id: 9,
    status: "PENDING",
  });
});

function mountView() {
  setActivePinia(createPinia());
  const userStore = useUserStore();
  userStore.token = "demo-token";
  userStore.persistProfile({
    id: 2,
    userId: 2,
    nickname: "Uploader",
    role: "USER",
    verificationStatus: "VERIFIED",
  });

  return mount(ResourceUploadView, {
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

test("submits upload form and redirects to my resources", async () => {
  const wrapper = mountView();
  const file = new File(["demo"], "resume-template.pdf", { type: "application/pdf" });
  const fileInput = wrapper.find('input[type="file"]');

  await wrapper.find('input[name="title"]').setValue("2026 Resume Template");
  await wrapper.find('select[name="category"]').setValue("RESUME_TEMPLATE");
  await wrapper.find('textarea[name="summary"]').setValue("Minimal summary");
  await wrapper.find('textarea[name="description"]').setValue("One-page starter");
  Object.defineProperty(fileInput.element, "files", {
    value: [file],
    configurable: true,
  });
  await fileInput.trigger("change");
  await wrapper.find("form").trigger("submit.prevent");
  await flushPromises();

  const payload = createResourceUpload.mock.calls[0][0];
  expect(payload).toBeInstanceOf(FormData);
  expect(payload.get("title")).toBe("2026 Resume Template");
  expect(payload.get("category")).toBe("RESUME_TEMPLATE");
  expect(payload.get("summary")).toBe("Minimal summary");
  expect(payload.get("description")).toBe("One-page starter");
  expect(payload.get("file")).toBe(file);
  expect(routerPush).toHaveBeenCalledWith("/profile/resources");
});
