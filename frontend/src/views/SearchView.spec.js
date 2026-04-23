import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { ref } from "vue";
import { beforeEach, expect, test, vi } from "vitest";
import SearchView from "./SearchView.vue";
import { getSearchResults } from "../api/search.js";

const routeState = ref({
  path: "/search",
  name: "search",
  query: {},
});

const routerReplace = vi.fn(async ({ name, query }) => {
  routeState.value = {
    ...routeState.value,
    name: name || "search",
    query: { ...query },
  };
});

const routerPush = vi.fn(async ({ name, query }) => {
  routeState.value = {
    ...routeState.value,
    name: name || "search",
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

vi.mock("../api/search.js", () => ({
  getSearchResults: vi.fn(),
}));

beforeEach(() => {
  vi.clearAllMocks();
  getSearchResults.mockReset();
  window.localStorage.clear();
  routeState.value = {
    path: "/search",
    name: "search",
    query: {},
  };
});

async function mountAt(path) {
  setActivePinia(createPinia());
  const [pathname, queryString = ""] = path.split("?");
  const query = Object.fromEntries(new URLSearchParams(queryString).entries());
  routeState.value = {
    path: pathname,
    name: "search",
    query,
  };

  const wrapper = mount(SearchView, {
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

test("route query drives api fetch and result rendering", async () => {
  getSearchResults.mockResolvedValue({
    query: "简历",
    type: "ALL",
    sort: "RELEVANCE",
    totals: { all: 1, post: 0, job: 0, resource: 1 },
    results: [
      {
        id: 1,
        type: "RESOURCE",
        title: "2026 简历模板包",
        summary: "一组整理好的常用模板。",
        metaPrimary: "平台推荐",
        metaSecondary: "简历模板",
        path: "/resources/1",
        publishedAt: "2026-04-15T08:00:00",
      },
    ],
  });

  const { wrapper } = await mountAt("/search?q=简历&type=ALL&sort=RELEVANCE");

  expect(getSearchResults).toHaveBeenCalledWith({
    q: "简历",
    type: "ALL",
    sort: "RELEVANCE",
  });
  expect(wrapper.text()).toContain("站内搜索");
  expect(wrapper.text()).toContain("结果快照");
  expect(wrapper.text()).toContain("当前共找到 1 条与“简历”相关的结果。");
  expect(wrapper.text()).toContain("2026 简历模板包");
  expect(wrapper.find('input[name="q"]').attributes("placeholder")).toBe("搜索经验帖、岗位、院校、资料");
});

test("blank query stays in guided empty state and does not hit the api", async () => {
  const { wrapper } = await mountAt("/search");

  expect(getSearchResults).not.toHaveBeenCalled();
  expect(wrapper.text()).toContain("先输入关键词");
  expect(wrapper.text()).toContain("输入关键词后，页面才会开始整理搜索结果。");
});

test("type and sort chips update the url-backed fetch state", async () => {
  getSearchResults
    .mockResolvedValueOnce({
      query: "简历",
      type: "ALL",
      sort: "RELEVANCE",
      totals: { all: 2, post: 0, job: 1, resource: 1 },
      results: [
        { id: 1, type: "RESOURCE", title: "资料命中", path: "/resources/1" },
        { id: 2, type: "JOB", title: "岗位命中", path: "/jobs/2" },
      ],
    })
    .mockResolvedValueOnce({
      query: "简历",
      type: "JOB",
      sort: "RELEVANCE",
      totals: { all: 2, post: 0, job: 1, resource: 1 },
      results: [
        { id: 2, type: "JOB", title: "岗位命中", path: "/jobs/2" },
      ],
    })
    .mockResolvedValueOnce({
      query: "简历",
      type: "JOB",
      sort: "LATEST",
      totals: { all: 2, post: 0, job: 1, resource: 1 },
      results: [
        { id: 2, type: "JOB", title: "岗位命中", path: "/jobs/2" },
      ],
    });

  const { wrapper } = await mountAt("/search?q=简历&type=ALL&sort=RELEVANCE");

  const buttons = wrapper.findAll("button.search-chip");
  expect(wrapper.text()).toContain("内容类型");
  expect(wrapper.text()).toContain("排序方式");

  await buttons.find((button) => button.text() === "岗位").trigger("click");
  await flushPromises();

  expect(routerPush).toHaveBeenCalled();
  expect(routerReplace).not.toHaveBeenCalled();
  expect(routeState.value.query).toMatchObject({
    q: "简历",
    type: "JOB",
    sort: "RELEVANCE",
  });
  expect(getSearchResults).toHaveBeenLastCalledWith({
    q: "简历",
    type: "JOB",
    sort: "RELEVANCE",
  });

  await buttons.find((button) => button.text() === "最新").trigger("click");
  await flushPromises();

  expect(routeState.value.query).toMatchObject({
    q: "简历",
    type: "JOB",
    sort: "LATEST",
  });
  expect(getSearchResults).toHaveBeenLastCalledWith({
    q: "简历",
    type: "JOB",
    sort: "LATEST",
  });
});

test("error state can retry the same route-backed query", async () => {
  let callCount = 0;
  getSearchResults.mockImplementation(async () => {
    callCount += 1;

    if (callCount === 1) {
      throw new Error("搜索服务暂时不可用");
    }

    return {
      query: "简历",
      type: "ALL",
      sort: "RELEVANCE",
      totals: { all: 1, post: 0, job: 0, resource: 1 },
      results: [{ id: 1, type: "RESOURCE", title: "恢复后的结果", path: "/resources/1" }],
    };
  });

  const { wrapper } = await mountAt("/search?q=简历&type=ALL&sort=RELEVANCE");

  expect(wrapper.text()).toContain("搜索服务暂时不可用");
  expect(wrapper.find("button.ghost-btn").text()).toBe("重试");

  await wrapper.find("button.ghost-btn").trigger("click");
  await flushPromises();

  expect(getSearchResults.mock.calls.length).toBeGreaterThanOrEqual(2);
  expect(getSearchResults).toHaveBeenLastCalledWith({
    q: "简历",
    type: "ALL",
    sort: "RELEVANCE",
  });
  expect(wrapper.text()).toContain("恢复后的结果");
});
