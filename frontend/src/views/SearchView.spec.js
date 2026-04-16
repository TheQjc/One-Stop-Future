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
    query: "resume",
    type: "ALL",
    sort: "RELEVANCE",
    totals: { all: 1, post: 0, job: 0, resource: 1 },
    results: [
      {
        id: 1,
        type: "RESOURCE",
        title: "2026 Resume Template Pack",
        summary: "A curated resource bundle.",
        metaPrimary: "Career Desk",
        metaSecondary: "Resume Template",
        path: "/resources/1",
        publishedAt: "2026-04-15T08:00:00",
      },
    ],
  });

  const { wrapper } = await mountAt("/search?q=resume&type=ALL&sort=RELEVANCE");

  expect(getSearchResults).toHaveBeenCalledWith({
    q: "resume",
    type: "ALL",
    sort: "RELEVANCE",
  });
  expect(wrapper.text()).toContain("2026 Resume Template Pack");
  expect(wrapper.text()).toContain('Showing 1 result for "resume".');
});

test("blank query stays in guided empty state and does not hit the api", async () => {
  const { wrapper } = await mountAt("/search");

  expect(getSearchResults).not.toHaveBeenCalled();
  expect(wrapper.text()).toContain("Start with a keyword");
});

test("type and sort chips update the url-backed fetch state", async () => {
  getSearchResults
    .mockResolvedValueOnce({
      query: "resume",
      type: "ALL",
      sort: "RELEVANCE",
      totals: { all: 2, post: 0, job: 1, resource: 1 },
      results: [
        { id: 1, type: "RESOURCE", title: "Resource hit", path: "/resources/1" },
        { id: 2, type: "JOB", title: "Job hit", path: "/jobs/2" },
      ],
    })
    .mockResolvedValueOnce({
      query: "resume",
      type: "JOB",
      sort: "RELEVANCE",
      totals: { all: 2, post: 0, job: 1, resource: 1 },
      results: [
        { id: 2, type: "JOB", title: "Job hit", path: "/jobs/2" },
      ],
    })
    .mockResolvedValueOnce({
      query: "resume",
      type: "JOB",
      sort: "LATEST",
      totals: { all: 2, post: 0, job: 1, resource: 1 },
      results: [
        { id: 2, type: "JOB", title: "Job hit", path: "/jobs/2" },
      ],
    });

  const { wrapper } = await mountAt("/search?q=resume&type=ALL&sort=RELEVANCE");

  const buttons = wrapper.findAll("button.search-chip");
  await buttons.find((button) => button.text() === "Jobs").trigger("click");
  await flushPromises();

  expect(routerPush).toHaveBeenCalled();
  expect(routerReplace).not.toHaveBeenCalled();
  expect(routeState.value.query).toMatchObject({
    q: "resume",
    type: "JOB",
    sort: "RELEVANCE",
  });
  expect(getSearchResults).toHaveBeenLastCalledWith({
    q: "resume",
    type: "JOB",
    sort: "RELEVANCE",
  });

  await buttons.find((button) => button.text() === "Latest").trigger("click");
  await flushPromises();

  expect(routeState.value.query).toMatchObject({
    q: "resume",
    type: "JOB",
    sort: "LATEST",
  });
  expect(getSearchResults).toHaveBeenLastCalledWith({
    q: "resume",
    type: "JOB",
    sort: "LATEST",
  });
});

test("error state can retry the same route-backed query", async () => {
  let callCount = 0;
  getSearchResults.mockImplementation(async () => {
    callCount += 1;

    if (callCount === 1) {
      throw new Error("Search temporarily unavailable");
    }

    return {
      query: "resume",
      type: "ALL",
      sort: "RELEVANCE",
      totals: { all: 1, post: 0, job: 0, resource: 1 },
      results: [{ id: 1, type: "RESOURCE", title: "Recovered result", path: "/resources/1" }],
    };
  });

  const { wrapper } = await mountAt("/search?q=resume&type=ALL&sort=RELEVANCE");

  expect(wrapper.text()).toContain("Search temporarily unavailable");

  await wrapper.find("button.ghost-btn").trigger("click");
  await flushPromises();

  expect(getSearchResults.mock.calls.length).toBeGreaterThanOrEqual(2);
  expect(getSearchResults).toHaveBeenLastCalledWith({
    q: "resume",
    type: "ALL",
    sort: "RELEVANCE",
  });
  expect(wrapper.text()).toContain("Recovered result");
});
