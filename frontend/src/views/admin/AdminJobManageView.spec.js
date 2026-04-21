import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, expect, test, vi } from "vitest";
import AdminJobManageView from "./AdminJobManageView.vue";
import { getAdminJobs, importAdminJobs, offlineAdminJob, publishAdminJob } from "../../api/admin.js";

vi.mock("../../api/admin.js", () => ({
  createAdminJob: vi.fn(),
  deleteAdminJob: vi.fn(),
  getAdminJobs: vi.fn(),
  importAdminJobs: vi.fn(),
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

test("imports a csv file and reloads the board", async () => {
  getAdminJobs
    .mockResolvedValueOnce({
      total: 0,
      jobs: [],
    })
    .mockResolvedValueOnce({
      total: 1,
      jobs: [{
        id: 88,
        title: "Data Intern",
        companyName: "Campus Future",
        status: "DRAFT",
      }],
    });
  importAdminJobs.mockResolvedValue({
    fileName: "jobs.csv",
    totalRows: 1,
    importedCount: 1,
    defaultStatus: "DRAFT",
  });

  const wrapper = mount(AdminJobManageView);
  await flushPromises();

  const file = new File(["csv"], "jobs.csv", { type: "text/csv" });
  const input = wrapper.find('input[name="jobImportFile"]');
  Object.defineProperty(input.element, "files", {
    configurable: true,
    value: [file],
  });
  await input.trigger("change");
  await wrapper.find('[data-testid="job-import-form"]').trigger("submit.prevent");
  await flushPromises();

  expect(importAdminJobs).toHaveBeenCalledTimes(1);
  expect(getAdminJobs).toHaveBeenCalledTimes(2);
  expect(wrapper.text()).toContain("Imported 1 jobs as DRAFT");
});

test("renders row-level import errors returned by the backend", async () => {
  getAdminJobs.mockResolvedValue({
    total: 0,
    jobs: [],
  });
  const requestError = new Error("job import validation failed");
  requestError.code = 400;
  requestError.data = {
    fileName: "jobs.csv",
    totalRows: 1,
    importedCount: 0,
    errors: [{
      rowNumber: 2,
      column: "jobType",
      message: "invalid job type",
    }],
  };
  importAdminJobs.mockRejectedValue(requestError);

  const wrapper = mount(AdminJobManageView);
  await flushPromises();

  const file = new File(["csv"], "jobs.csv", { type: "text/csv" });
  const input = wrapper.find('input[name="jobImportFile"]');
  Object.defineProperty(input.element, "files", {
    configurable: true,
    value: [file],
  });
  await input.trigger("change");
  await wrapper.find('[data-testid="job-import-form"]').trigger("submit.prevent");
  await flushPromises();

  expect(getAdminJobs).toHaveBeenCalledTimes(1);
  expect(wrapper.text()).toContain("job import validation failed");
  expect(wrapper.text()).toContain("Row 2");
  expect(wrapper.text()).toContain("invalid job type");
});
