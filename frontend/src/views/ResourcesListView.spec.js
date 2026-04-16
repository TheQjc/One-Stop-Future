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

test("loads resources and refetches with filters", async () => {
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
});
