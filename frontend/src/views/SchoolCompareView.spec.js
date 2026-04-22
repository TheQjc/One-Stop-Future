import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { beforeEach, expect, test, vi } from "vitest";
import SchoolCompareView from "./SchoolCompareView.vue";
import { compareDecisionSchools, listDecisionSchools } from "../api/decision.js";
import router from "../router/index.js";

vi.mock("../api/decision.js", () => ({
  listDecisionQuestions: vi.fn(),
  submitDecisionAnswers: vi.fn(),
  getLatestDecisionResult: vi.fn(),
  getDecisionTimeline: vi.fn(),
  listDecisionSchools: vi.fn(),
  compareDecisionSchools: vi.fn(),
}));

beforeEach(() => {
  vi.clearAllMocks();
  window.localStorage.clear();
});

function mountView() {
  setActivePinia(createPinia());

  return mount(SchoolCompareView, {
    global: {
      stubs: {
        RouterLink: {
          props: ["to"],
          template: "<a :data-to='typeof to === \"string\" ? to : JSON.stringify(to)'><slot /></a>",
        },
      },
    },
  });
}

test("router exposes a public schools compare route", () => {
  expect(router.resolve("/schools/compare").name).toBe("schools-compare");
  expect(router.resolve("/schools/compare").meta.requiresAuth).toBeUndefined();
});

test("enforces 2-4 school selection and renders compare result highlight", async () => {
  listDecisionSchools.mockResolvedValue({
    track: "EXAM",
    keyword: null,
    total: 5,
    schools: [
      { schoolId: 1, name: "A", track: "EXAM", region: "R", tierLabel: "T" },
      { schoolId: 2, name: "B", track: "EXAM", region: "R", tierLabel: "T" },
      { schoolId: 3, name: "C", track: "EXAM", region: "R", tierLabel: "T" },
      { schoolId: 4, name: "D", track: "EXAM", region: "R", tierLabel: "T" },
      { schoolId: 5, name: "E", track: "EXAM", region: "R", tierLabel: "T" },
    ],
  });

  compareDecisionSchools.mockResolvedValue({
    schools: [
      { schoolId: 2, name: "B", track: "EXAM", region: "R", tierLabel: "T" },
      { schoolId: 1, name: "A", track: "EXAM", region: "R", tierLabel: "T" },
    ],
    metricDefinitions: [],
    tableRows: [],
    chartSeries: [],
    highlightSummary: "School B is stronger on cost while School A leads on reputation.",
  });

  const wrapper = mountView();
  await flushPromises();

  expect(wrapper.text()).toContain("院校对比");
  expect(wrapper.text()).toContain("候选院校");

  const submit = wrapper.find('[data-test="compare-submit"]');
  expect(submit.attributes("disabled")).toBeDefined();

  const cards = wrapper.findAll("button.candidate-card");
  await cards[0].trigger("click");
  await flushPromises();
  expect(wrapper.find('[data-test="compare-submit"]').attributes("disabled")).toBeDefined();

  await cards[1].trigger("click");
  await flushPromises();
  expect(wrapper.find('[data-test="compare-submit"]').attributes("disabled")).toBeUndefined();

  await cards[2].trigger("click");
  await cards[3].trigger("click");
  await flushPromises();
  expect(wrapper.text()).not.toContain("最多选择 4 所学校。");

  await cards[4].trigger("click");
  await flushPromises();
  expect(wrapper.text()).toContain("最多选择 4 所学校。");

  await wrapper.find('[data-test="compare-submit"]').trigger("click");
  await flushPromises();

  expect(compareDecisionSchools).toHaveBeenCalledWith({ schoolIds: [1, 2, 3, 4] });
  expect(wrapper.find('[data-test="highlight"]').text())
    .toContain("School B is stronger on cost");
});

test("renders an explicit empty-chart state when chartSeries is empty", async () => {
  listDecisionSchools.mockResolvedValue({
    track: "EXAM",
    keyword: null,
    total: 2,
    schools: [
      { schoolId: 1, name: "A", track: "EXAM", region: "R", tierLabel: "T" },
      { schoolId: 2, name: "B", track: "EXAM", region: "R", tierLabel: "T" },
    ],
  });

  compareDecisionSchools.mockResolvedValue({
    schools: [
      { schoolId: 1, name: "A", track: "EXAM", region: "R", tierLabel: "T" },
      { schoolId: 2, name: "B", track: "EXAM", region: "R", tierLabel: "T" },
    ],
    metricDefinitions: [],
    tableRows: [],
    chartSeries: [],
    highlightSummary: "Ready.",
  });

  const wrapper = mountView();
  await flushPromises();

  const cards = wrapper.findAll("button.candidate-card");
  await cards[0].trigger("click");
  await cards[1].trigger("click");
  await flushPromises();

  await wrapper.find('[data-test="compare-submit"]').trigger("click");
  await flushPromises();

  expect(wrapper.find('[data-test="chart-panel"]').exists()).toBe(true);
  expect(wrapper.find('[data-test="empty-chart"]').exists()).toBe(true);
  expect(wrapper.text()).toContain("图表概览");
});
