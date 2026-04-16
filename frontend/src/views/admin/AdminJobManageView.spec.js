import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, expect, test, vi } from "vitest";
import AdminJobManageView from "./AdminJobManageView.vue";
import { getAdminJobs, offlineAdminJob, publishAdminJob } from "../../api/admin.js";

vi.mock("../../api/admin.js", () => ({
  createAdminJob: vi.fn(),
  deleteAdminJob: vi.fn(),
  getAdminJobs: vi.fn(),
  offlineAdminJob: vi.fn(),
  publishAdminJob: vi.fn(),
  updateAdminJob: vi.fn(),
}));

const draftJob = {
  id: 31,
  title: "Draft job",
  companyName: "Campus Future",
  status: "DRAFT",
};

const publishedJob = {
  id: 31,
  title: "Draft job",
  companyName: "Campus Future",
  status: "PUBLISHED",
};

beforeEach(() => {
  vi.clearAllMocks();
  window.localStorage.clear();
});

test("publishes a draft job and reloads the board", async () => {
  getAdminJobs
    .mockResolvedValueOnce({
      total: 1,
      jobs: [{ ...draftJob }],
    })
    .mockResolvedValueOnce({
      total: 1,
      jobs: [{ ...publishedJob }],
    });
  publishAdminJob.mockResolvedValue({
    ...publishedJob,
  });

  const wrapper = mount(AdminJobManageView);
  await flushPromises();

  expect(wrapper.text()).toContain("Draft job");

  await wrapper.find(".publish-action").trigger("click");
  await flushPromises();

  expect(publishAdminJob).toHaveBeenCalledWith(31);
  expect(getAdminJobs).toHaveBeenCalledTimes(2);
});

test("offlines a published job and reloads the board", async () => {
  getAdminJobs
    .mockResolvedValueOnce({
      total: 1,
      jobs: [{ ...publishedJob }],
    })
    .mockResolvedValueOnce({
      total: 1,
      jobs: [{ ...publishedJob, status: "OFFLINE" }],
    });
  offlineAdminJob.mockResolvedValue({
    ...publishedJob,
    status: "OFFLINE",
  });

  const wrapper = mount(AdminJobManageView);
  await flushPromises();

  await wrapper.find(".offline-action").trigger("click");
  await flushPromises();

  expect(offlineAdminJob).toHaveBeenCalledWith(31);
  expect(getAdminJobs).toHaveBeenCalledTimes(2);
});
