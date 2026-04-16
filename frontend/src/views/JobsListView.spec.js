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

test("loads job cards and refetches with filters", async () => {
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
});
