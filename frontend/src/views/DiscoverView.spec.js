import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { ref } from "vue";
import { beforeEach, expect, test, vi } from "vitest";
import DiscoverView from "./DiscoverView.vue";
import { getDiscoverResults } from "../api/discover.js";

const routeState = ref({
  path: "/discover",
  name: "discover",
  query: {},
});

const routerReplace = vi.fn(async ({ name, query }) => {
  routeState.value = {
    ...routeState.value,
    name: name || "discover",
    query: { ...query },
  };
});

const routerPush = vi.fn(async ({ name, query }) => {
  routeState.value = {
    ...routeState.value,
    name: name || "discover",
    query: { ...query },
  };
});

vi.mock("vue-router", async () => {
  const actual = await vi.importActual("vue-router");

  return {
    ...actual,
    useRoute: () => routeState.value,
    useRouter: () => ({
      currentRoute: routeState,
      replace: routerReplace,
      push: routerPush,
    }),
  };
});

vi.mock("../api/discover.js", () => ({
  getDiscoverResults: vi.fn(),
}));

beforeEach(() => {
  vi.clearAllMocks();
  getDiscoverResults.mockReset();
  window.localStorage.clear();
  routeState.value = {
    path: "/discover",
    name: "discover",
    query: {},
  };
});

async function mountAt(path) {
  setActivePinia(createPinia());
  const [pathname, queryString = ""] = path.split("?");
  const query = Object.fromEntries(new URLSearchParams(queryString).entries());
  routeState.value = {
    path: pathname,
    name: "discover",
    query,
  };

  const wrapper = mount(DiscoverView, {
    global: {
      stubs: {
        RouterLink: {
          props: ["to"],
          template: "<a :data-to='typeof to === \"string\" ? to : JSON.stringify(to)'><slot /></a>",
        },
      },
    },
  });

  await flushPromises();
  return { wrapper };
}

test("discover view defaults to all plus week and hits the api", async () => {
  getDiscoverResults.mockResolvedValue({
    tab: "ALL",
    period: "WEEK",
    total: 1,
    items: [
      {
        id: 1,
        type: "RESOURCE",
        title: "Hot resource",
        summary: "A weekly resource leader.",
        primaryMeta: "Career Desk",
        secondaryMeta: "RESUME_TEMPLATE",
        path: "/resources/1",
        publishedAt: "2026-04-16T08:00:00",
        hotScore: 22,
        hotLabel: "Weekly resource lead",
      },
    ],
  });

  const { wrapper } = await mountAt("/discover");

  expect(getDiscoverResults).toHaveBeenCalledWith({ tab: "ALL", period: "WEEK", limit: 20 });
  expect(wrapper.text()).toContain("趋势");
  expect(wrapper.text()).toContain("按时间范围和内容类型切换趋势");
  expect(wrapper.text()).toContain("当前共有 1 条公开内容进入本周趋势榜。");
  expect(wrapper.text()).toContain("Hot resource");
  expect(wrapper.text()).toContain("Weekly resource lead");
});

test("tab and period toggles sync the url-backed discover state", async () => {
  getDiscoverResults
    .mockResolvedValueOnce({
      tab: "ALL",
      period: "WEEK",
      total: 2,
      items: [
        { id: 1, type: "RESOURCE", title: "Resource leader", path: "/resources/1", hotLabel: "Weekly resource lead" },
        { id: 2, type: "JOB", title: "Job leader", path: "/jobs/2", hotLabel: "Weekly attention" },
      ],
    })
    .mockResolvedValueOnce({
      tab: "JOB",
      period: "WEEK",
      total: 1,
      items: [
        { id: 2, type: "JOB", title: "Job leader", path: "/jobs/2", hotLabel: "Weekly attention" },
      ],
    })
    .mockResolvedValueOnce({
      tab: "JOB",
      period: "ALL",
      total: 1,
      items: [
        { id: 2, type: "JOB", title: "Job leader", path: "/jobs/2", hotLabel: "Sustained attention" },
      ],
    });

  const { wrapper } = await mountAt("/discover?tab=ALL&period=WEEK");

  const buttons = wrapper.findAll("button.discover-chip");
  expect(wrapper.text()).toContain("时间范围");
  expect(wrapper.text()).toContain("内容类型");

  await buttons.find((button) => button.text() === "岗位").trigger("click");
  await flushPromises();

  expect(routeState.value.query).toMatchObject({
    tab: "JOB",
    period: "WEEK",
  });
  expect(getDiscoverResults).toHaveBeenLastCalledWith({
    tab: "JOB",
    period: "WEEK",
    limit: 20,
  });

  await buttons.find((button) => button.text() === "全部时段").trigger("click");
  await flushPromises();

  expect(routeState.value.query).toMatchObject({
    tab: "JOB",
    period: "ALL",
  });
  expect(getDiscoverResults).toHaveBeenLastCalledWith({
    tab: "JOB",
    period: "ALL",
    limit: 20,
  });
});

test("discover view can render a graceful empty state", async () => {
  getDiscoverResults.mockResolvedValue({
    tab: "POST",
    period: "ALL",
    total: 0,
    items: [],
  });

  const { wrapper } = await mountAt("/discover?tab=POST&period=ALL");

  expect(wrapper.text()).toContain("趋势内容还在整理中");
  expect(wrapper.text()).toContain("当前筛选条件下还没有新的公开内容进入趋势榜。");
});

test("error state can retry the same discover query", async () => {
  let callCount = 0;
  getDiscoverResults.mockImplementation(async () => {
    callCount += 1;

    if (callCount === 1) {
      throw new Error("Discover temporarily unavailable");
    }

    return {
      tab: "ALL",
      period: "WEEK",
      total: 1,
      items: [
        { id: 1, type: "RESOURCE", title: "Recovered board item", path: "/resources/1", hotLabel: "Weekly resource lead" },
      ],
    };
  });

  const { wrapper } = await mountAt("/discover?tab=ALL&period=WEEK");

  expect(wrapper.text()).toContain("Discover temporarily unavailable");
  expect(wrapper.find("button.ghost-btn").text()).toBe("重试");

  await wrapper.find("button.ghost-btn").trigger("click");
  await flushPromises();

  expect(getDiscoverResults).toHaveBeenLastCalledWith({
    tab: "ALL",
    period: "WEEK",
    limit: 20,
  });
  expect(wrapper.text()).toContain("Recovered board item");
});
