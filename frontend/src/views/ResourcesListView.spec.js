import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { beforeEach, expect, test, vi } from "vitest";
import ResourcesListView from "./ResourcesListView.vue";
import { getResources } from "../api/resources.js";

vi.mock("../api/resources.js", () => ({
  getResources: vi.fn(),
}));

beforeEach(() => {
  vi.clearAllMocks();
  window.localStorage.clear();
});

function mountView() {
  setActivePinia(createPinia());

  return mount(ResourcesListView, {
    global: {
      stubs: {
        RouterLink: {
          props: ["to"],
          template: "<a :data-to='to'><slot /></a>",
        },
        ResourceCard: {
          props: ["resource"],
          template: "<article class='stub-resource'>{{ resource.title }}</article>",
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
  getResources.mockImplementationOnce(() => deferred.promise);

  const wrapper = mountView();
  await wrapper.vm.$nextTick();

  expect(wrapper.text()).toContain("资料库");
  expect(wrapper.text()).toContain("把常用资料集中整理好，查阅前先看清分类");
  expect(wrapper.text()).toContain("登录后上传");
  expect(wrapper.text()).toContain("资料筛选");
  expect(wrapper.text()).toContain("当前资料");
  expect(wrapper.text()).toContain("正在加载资料...");
  expect(wrapper.text()).toContain("筛选中...");

  deferred.resolve({
    total: 0,
    resources: [],
  });
  await flushPromises();
});

test("loads resources, refetches with filters, and shows localized filter summary", async () => {
  getResources
    .mockResolvedValueOnce({
      total: 1,
      resources: [{ id: 1, title: "Resume Template Pack" }],
    })
    .mockResolvedValueOnce({
      total: 1,
      resources: [{ id: 2, title: "Filtered Resource" }],
    });

  const wrapper = mountView();
  await flushPromises();

  expect(getResources).toHaveBeenNthCalledWith(1, {});
  expect(wrapper.text()).toContain("Resume Template Pack");

  await wrapper.find('input[name="keyword"]').setValue("resume");
  await wrapper.find('select[name="category"]').setValue("RESUME_TEMPLATE");
  await wrapper.find("form").trigger("submit.prevent");
  await flushPromises();

  expect(getResources).toHaveBeenNthCalledWith(2, {
    keyword: "resume",
    category: "RESUME_TEMPLATE",
  });
  expect(wrapper.text()).toContain("Filtered Resource");
  expect(wrapper.text()).toContain("关键词：resume");
  expect(wrapper.text()).toContain("分类：简历模板");
});

test("shows localized error and retry copy", async () => {
  getResources
    .mockRejectedValueOnce(new Error())
    .mockResolvedValueOnce({
      total: 0,
      resources: [],
    });

  const wrapper = mountView();
  await flushPromises();

  expect(wrapper.text()).toContain("资料库加载失败，请稍后重试。");
  expect(wrapper.text()).toContain("重试");

  await wrapper.find("button.ghost-btn").trigger("click");
  await flushPromises();

  expect(getResources).toHaveBeenCalledTimes(2);
});
