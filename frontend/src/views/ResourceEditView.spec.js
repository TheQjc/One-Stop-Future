import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, expect, test, vi } from "vitest";
import ResourceEditView from "./ResourceEditView.vue";
import { getResourceDetail, updateResource } from "../api/resources.js";

const routeMock = {
  params: { id: "3" },
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
  getResourceDetail: vi.fn(),
  updateResource: vi.fn(),
}));

beforeEach(() => {
  vi.clearAllMocks();
  window.localStorage.clear();
});

function mountView() {
  return mount(ResourceEditView, {
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

test("edit view preloads a rejected resource and resubmits without replacing the file", async () => {
  getResourceDetail.mockResolvedValue({
    id: 3,
    title: "Rejected pack",
    category: "RESUME_TEMPLATE",
    summary: "Needs revision",
    description: "Old copy",
    fileName: "rejected-pack.pdf",
    fileSize: 524288,
    rejectReason: "Clarify the summary",
    editableByMe: true,
    previewAvailable: true,
    status: "REJECTED",
  });
  updateResource.mockResolvedValue({ id: 3, status: "PENDING" });

  const wrapper = mountView();
  await flushPromises();

  expect(wrapper.find('input[name="title"]').element.value).toBe("Rejected pack");
  expect(wrapper.find('select[name="category"]').element.value).toBe("RESUME_TEMPLATE");
  expect(wrapper.find('textarea[name="summary"]').element.value).toBe("Needs revision");
  expect(wrapper.text()).toContain("Clarify the summary");
  expect(wrapper.text()).toContain("rejected-pack.pdf");

  await wrapper.find('textarea[name="summary"]').setValue("Revised summary");
  await wrapper.find("form").trigger("submit.prevent");
  await flushPromises();

  expect(updateResource).toHaveBeenCalledTimes(1);
  expect(updateResource).toHaveBeenCalledWith("3", expect.any(FormData));
  const payload = updateResource.mock.calls[0][1];
  expect(payload.get("title")).toBe("Rejected pack");
  expect(payload.get("summary")).toBe("Revised summary");
  expect(payload.has("file")).toBe(false);
  expect(routerPush).toHaveBeenCalledWith("/profile/resources");
});

test("edit view blocks when the resource is not editable by the current user", async () => {
  getResourceDetail.mockResolvedValue({
    id: 3,
    title: "Rejected pack",
    editableByMe: false,
    previewAvailable: true,
    status: "REJECTED",
  });

  const wrapper = mountView();
  await flushPromises();

  expect(wrapper.text()).toContain("This resource cannot be edited");
  expect(wrapper.find("form").exists()).toBe(false);
  expect(updateResource).not.toHaveBeenCalled();
});
