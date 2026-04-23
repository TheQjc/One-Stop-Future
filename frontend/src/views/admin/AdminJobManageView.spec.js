import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, expect, test, vi } from "vitest";
import AdminJobManageView from "./AdminJobManageView.vue";
import { getAdminJobs, importAdminJobs, offlineAdminJob, publishAdminJob, syncAdminJobs } from "../../api/admin.js";

vi.mock("../../api/admin.js", () => ({
  createAdminJob: vi.fn(),
  deleteAdminJob: vi.fn(),
  getAdminJobs: vi.fn(),
  importAdminJobs: vi.fn(),
  offlineAdminJob: vi.fn(),
  publishAdminJob: vi.fn(),
  syncAdminJobs: vi.fn(),
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
  expect(wrapper.text()).toContain("已从 jobs.csv 导入 1 条岗位");
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
  expect(wrapper.text()).toContain("第 2 行");
  expect(wrapper.text()).toContain("invalid job type");
});

test("syncs the configured feed and reloads the board", async () => {
  getAdminJobs
    .mockResolvedValueOnce({
      total: 1,
      jobs: [{
        id: 1,
        title: "Java Backend Intern",
        companyName: "Future Campus Tech",
        status: "PUBLISHED",
      }],
    })
    .mockResolvedValueOnce({
      total: 2,
      jobs: [
        {
          id: 1,
          title: "Java Backend Intern Updated",
          companyName: "Future Campus Tech",
          status: "PUBLISHED",
        },
        {
          id: 99,
          title: "Partner Data Analyst",
          companyName: "North Lake Studio",
          status: "DRAFT",
        },
      ],
    });
  syncAdminJobs.mockResolvedValue({
    sourceName: "Partner Feed",
    fetchedCount: 4,
    createdCount: 1,
    updatedCount: 1,
    skippedCount: 1,
    invalidCount: 1,
    defaultCreatedStatus: "DRAFT",
    issues: [{
      itemIndex: 3,
      sourceUrl: "https://partner.example/jobs/deleted-role",
      type: "SKIPPED",
      message: "job is deleted locally",
    }],
  });

  const wrapper = mount(AdminJobManageView);
  await flushPromises();

  await wrapper.find('[data-testid="job-sync-button"]').trigger("click");
  await flushPromises();

  expect(syncAdminJobs).toHaveBeenCalledTimes(1);
  expect(getAdminJobs).toHaveBeenCalledTimes(2);
  expect(wrapper.text()).toContain("Partner Feed");
  expect(wrapper.text()).toContain("新增 1");
  expect(wrapper.text()).toContain("job is deleted locally");
});

test("renders sync failure without reloading the jobs board", async () => {
  getAdminJobs.mockResolvedValue({
    total: 1,
    jobs: [{
      id: 1,
      title: "Java Backend Intern",
      companyName: "Future Campus Tech",
      status: "PUBLISHED",
    }],
  });
  syncAdminJobs.mockRejectedValue(new Error("invalid job sync feed"));

  const wrapper = mount(AdminJobManageView);
  await flushPromises();

  await wrapper.find('[data-testid="job-sync-button"]').trigger("click");
  await flushPromises();

  expect(getAdminJobs).toHaveBeenCalledTimes(1);
  expect(wrapper.text()).toContain("invalid job sync feed");
});
