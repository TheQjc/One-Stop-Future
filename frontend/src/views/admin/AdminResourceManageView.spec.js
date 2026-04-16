import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, expect, test, vi } from "vitest";
import AdminResourceManageView from "./AdminResourceManageView.vue";
import {
  getAdminResources,
  offlineAdminResource,
  publishAdminResource,
  rejectAdminResource,
} from "../../api/admin.js";

vi.mock("../../api/admin.js", () => ({
  getAdminResources: vi.fn(),
  offlineAdminResource: vi.fn(),
  publishAdminResource: vi.fn(),
  rejectAdminResource: vi.fn(),
}));

const pendingResource = {
  id: 41,
  title: "Pending archive",
  uploaderNickname: "NormalUser",
  status: "PENDING",
};

const publishedResource = {
  id: 41,
  title: "Pending archive",
  uploaderNickname: "NormalUser",
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
