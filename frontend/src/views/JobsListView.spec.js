import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { beforeEach, expect, test, vi } from "vitest";
import JobsListView from "./JobsListView.vue";
import { getJobs } from "../api/jobs.js";

vi.mock("../api/jobs.js", () => ({
  getJobs: vi.fn(),
}));

beforeEach(() => {
  vi.clearAllMocks();
  window.localStorage.clear();
});

function mountView() {
  setActivePinia(createPinia());

  return mount(JobsListView, {
    global: {
      stubs: {
        RouterLink: {
          props: ["to"],
          template: "<a :data-to='to'><slot /></a>",
        },
        JobPostingCard: {
          props: ["job"],
          template: "<article class='stub-job'>{{ job.title }}</article>",
        },
      },
    },
  });
}

function createDeferred() {
  let resolve;
  let reject;

  const promise = new Promise((res, rej) => {
    resolve = res;
    reject = rej;
  });

  return { promise, resolve, reject };
}

test("renders Chinese-first shell copy and loading state", async () => {
  const deferred = createDeferred();
  getJobs.mockImplementationOnce(() => deferred.promise);

  const wrapper = mountView();
  await wrapper.vm.$nextTick();

  expect(wrapper.text()).toContain("岗位专区");
  expect(wrapper.text()).toContain("先看清岗位条件，再决定要投哪一个机会");
  expect(wrapper.text()).toContain("登录后收藏岗位");
  expect(wrapper.text()).toContain("岗位筛选");
  expect(wrapper.text()).toContain("当前岗位");
  expect(wrapper.text()).toContain("正在加载岗位信息...");
  expect(wrapper.text()).toContain("筛选中...");

  deferred.resolve({
    total: 0,
    jobs: [],
  });
  await flushPromises();
});

test("loads job cards, refetches with filters, and shows localized filter summary", async () => {
  getJobs
    .mockResolvedValueOnce({
      total: 1,
      jobs: [{ id: 1, title: "Java Backend Intern" }],
    })
    .mockResolvedValueOnce({
      total: 1,
      jobs: [{ id: 2, title: "Filtered Job" }],
    });

  const wrapper = mountView();
  await flushPromises();

  expect(getJobs).toHaveBeenNthCalledWith(1, {});
  expect(wrapper.text()).toContain("Java Backend Intern");

  await wrapper.find('input[name="keyword"]').setValue("backend");
  await wrapper.find('select[name="city"]').setValue("Shenzhen");
  await wrapper.find("form").trigger("submit.prevent");
  await flushPromises();

  expect(getJobs).toHaveBeenNthCalledWith(2, {
    keyword: "backend",
    city: "Shenzhen",
  });
  expect(wrapper.text()).toContain("Filtered Job");
  expect(wrapper.text()).toContain("关键词：backend");
  expect(wrapper.text()).toContain("城市：深圳");
});

test("shows localized error and retry copy", async () => {
  getJobs
    .mockRejectedValueOnce(new Error())
    .mockResolvedValueOnce({
      total: 0,
      jobs: [],
    });

  const wrapper = mountView();
  await flushPromises();

  expect(wrapper.text()).toContain("岗位信息加载失败，请稍后重试。");
  expect(wrapper.text()).toContain("重试");

  await wrapper.find("button.ghost-btn").trigger("click");
  await flushPromises();

  expect(getJobs).toHaveBeenCalledTimes(2);
});
