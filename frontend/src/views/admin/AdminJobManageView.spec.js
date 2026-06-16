import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, expect, test, vi } from "vitest";
import AdminJobManageView from "./AdminJobManageView.vue";
import {
  createAdminJob,
  deleteAdminJob,
  getAdminJobs,
  importAdminJobs,
  offlineAdminJob,
  publishAdminJob,
  syncAdminJobs,
  updateAdminJob,
} from "../../api/admin.js";

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
  title: "待发布岗位",
  companyName: "校园未来中心",
  status: "DRAFT",
};

const publishedJob = {
  id: 31,
  title: "待发布岗位",
  companyName: "校园未来中心",
  status: "PUBLISHED",
};

beforeEach(() => {
  vi.resetAllMocks();
  window.localStorage.clear();
});

async function fillJobForm(wrapper, overrides = {}) {
  await wrapper.find('input[name="title"]').setValue(overrides.title || "新岗位草稿");
  await wrapper.find('input[name="companyName"]').setValue(overrides.companyName || "校园未来中心");
  await wrapper.find('input[name="city"]').setValue(overrides.city || "杭州");
  await wrapper.find('input[name="sourceUrl"]').setValue(overrides.sourceUrl || "https://jobs.example.com/admin-created");
  await wrapper.find('select[name="jobType"]').setValue(overrides.jobType || "INTERNSHIP");
  await wrapper.find('select[name="educationRequirement"]').setValue(overrides.educationRequirement || "BACHELOR");
  await wrapper.find('input[name="sourcePlatform"]').setValue(overrides.sourcePlatform || "官方渠道");
  await wrapper.find('textarea[name="summary"]').setValue(overrides.summary || "岗位摘要");
  await wrapper.find('textarea[name="content"]').setValue(overrides.content || "岗位正文");
}

test("creates a draft job from the editor action", async () => {
  getAdminJobs
    .mockResolvedValueOnce({
      total: 0,
      jobs: [],
    })
    .mockResolvedValueOnce({
      total: 1,
      jobs: [{ ...draftJob }],
    });
  createAdminJob.mockResolvedValue({ ...draftJob });

  const wrapper = mount(AdminJobManageView);
  await flushPromises();

  await fillJobForm(wrapper);
  await wrapper.find('[data-testid="job-save-button"]').trigger("submit");
  await flushPromises();

  expect(createAdminJob).toHaveBeenCalledWith(expect.objectContaining({
    title: "新岗位草稿",
    companyName: "校园未来中心",
    city: "杭州",
    jobType: "INTERNSHIP",
    educationRequirement: "BACHELOR",
    sourcePlatform: "官方渠道",
    sourceUrl: "https://jobs.example.com/admin-created",
    summary: "岗位摘要",
    content: "岗位正文",
  }));
  expect(getAdminJobs).toHaveBeenCalledTimes(2);
});

test("edits a row job and saves changes from the editor action", async () => {
  getAdminJobs
    .mockResolvedValueOnce({
      total: 1,
      jobs: [{ ...draftJob }],
    })
    .mockResolvedValueOnce({
      total: 1,
      jobs: [{ ...draftJob, title: "更新后的岗位" }],
    });
  updateAdminJob.mockResolvedValue({ ...draftJob, title: "更新后的岗位" });

  const wrapper = mount(AdminJobManageView);
  await flushPromises();

  await wrapper.find('[data-testid="edit-job-row-31"]').trigger("click");
  await wrapper.find('input[name="title"]').setValue("更新后的岗位");
  await wrapper.find('[data-testid="job-save-button"]').trigger("submit");
  await flushPromises();

  expect(updateAdminJob).toHaveBeenCalledWith(31, expect.objectContaining({
    title: "更新后的岗位",
  }));
  expect(getAdminJobs).toHaveBeenCalledTimes(2);
});

test("deletes a row job and reloads the board", async () => {
  getAdminJobs
    .mockResolvedValueOnce({
      total: 1,
      jobs: [{ ...draftJob }],
    })
    .mockResolvedValueOnce({
      total: 0,
      jobs: [],
    });
  deleteAdminJob.mockResolvedValue(true);

  const wrapper = mount(AdminJobManageView);
  await flushPromises();

  await wrapper.find('[data-testid="delete-job-row-31"]').trigger("click");
  await flushPromises();

  expect(deleteAdminJob).toHaveBeenCalledWith(31);
  expect(getAdminJobs).toHaveBeenCalledTimes(2);
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

  expect(wrapper.text()).toContain("待发布岗位");

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
        title: "数据运营实习生",
        companyName: "校园未来中心",
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
  const requestError = new Error("岗位导入校验失败");
  requestError.code = 400;
  requestError.data = {
    fileName: "jobs.csv",
    totalRows: 1,
    importedCount: 0,
    errors: [{
      rowNumber: 2,
      column: "jobType",
      message: "岗位类型无效",
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
  expect(wrapper.text()).toContain("岗位导入校验失败");
  expect(wrapper.text()).toContain("第 2 行");
  expect(wrapper.text()).toContain("岗位类型无效");
});

test("syncs the configured feed and reloads the board", async () => {
  getAdminJobs
    .mockResolvedValueOnce({
      total: 1,
      jobs: [{
        id: 1,
        title: "后端开发实习生",
        companyName: "未来校园科技",
        status: "PUBLISHED",
      }],
    })
    .mockResolvedValueOnce({
      total: 2,
      jobs: [
        {
          id: 1,
          title: "后端开发实习生（更新）",
          companyName: "未来校园科技",
          status: "PUBLISHED",
        },
        {
          id: 99,
          title: "合作方数据分析师",
          companyName: "北湖工作室",
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
      title: "后端开发实习生",
      companyName: "未来校园科技",
      status: "PUBLISHED",
    }],
  });
  syncAdminJobs.mockRejectedValue(new Error("岗位同步数据源无效"));

  const wrapper = mount(AdminJobManageView);
  await flushPromises();

  await wrapper.find('[data-testid="job-sync-button"]').trigger("click");
  await flushPromises();

  expect(getAdminJobs).toHaveBeenCalledTimes(1);
  expect(wrapper.text()).toContain("岗位同步数据源无效");
});
