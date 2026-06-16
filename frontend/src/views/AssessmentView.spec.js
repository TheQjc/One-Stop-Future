import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { beforeEach, expect, test, vi } from "vitest";
import AssessmentView from "./AssessmentView.vue";
import {
  getLatestDecisionResult,
  listDecisionQuestions,
  submitDecisionAnswers,
} from "../api/decision.js";
import router from "../router/index.js";

vi.mock("../api/decision.js", () => ({
  listDecisionQuestions: vi.fn(),
  submitDecisionAnswers: vi.fn(),
  getLatestDecisionResult: vi.fn(),
  getDecisionTimeline: vi.fn(),
}));

beforeEach(() => {
  vi.clearAllMocks();
  window.localStorage.clear();
});

function mountView() {
  setActivePinia(createPinia());

  return mount(AssessmentView, {
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

test("router marks assessment and timeline routes as requiresAuth", () => {
  expect(router.resolve("/assessment").meta.requiresAuth).toBe(true);
  expect(router.resolve("/timeline").meta.requiresAuth).toBe(true);
});

test("loads decision questions and blocks submit until all answers are selected", async () => {
  getLatestDecisionResult.mockResolvedValue({ hasResult: false });
  listDecisionQuestions.mockResolvedValue({
    questions: [
      {
        id: 1,
        code: "DECISION_Q1",
        prompt: "Which outcome matters most to you right now?",
        description: "Pick the option that best matches your current priority.",
        displayOrder: 1,
        options: [
          { id: 11, code: "Q1_A", label: "Improve exam performance", description: "Prioritize structured learning and score improvements.", displayOrder: 1 },
          { id: 12, code: "Q1_B", label: "Get a job offer sooner", description: "Prioritize employability and interviews.", displayOrder: 2 },
        ],
      },
      {
        id: 2,
        code: "DECISION_Q2",
        prompt: "How do you prefer to measure progress?",
        description: "Choose what feels most motivating and realistic.",
        displayOrder: 2,
        options: [
          { id: 21, code: "Q2_A", label: "Build a portfolio and ship projects", description: "Progress is visible through outputs and iterations.", displayOrder: 1 },
          { id: 22, code: "Q2_B", label: "Hit clear score targets", description: "Progress is visible through measurable scores.", displayOrder: 2 },
        ],
      },
    ],
  });

  submitDecisionAnswers.mockResolvedValue({
    hasResult: true,
    recommendedTrack: "EXAM",
    summaryText: "Recommendation: EXAM",
    scores: { career: 0, exam: 6, abroad: 0 },
    ranking: [
      { track: "EXAM", score: 6 },
      { track: "CAREER", score: 0 },
      { track: "ABROAD", score: 0 },
    ],
    nextActions: [
      { code: "TIMELINE", label: "Go to Timeline", path: "/timeline" },
      { code: "COMPARE_SCHOOLS", label: "Compare Schools", path: "/schools/compare" },
    ],
  });

  const wrapper = mountView();
  await flushPromises();

  expect(wrapper.text()).toContain("方向测评");
  expect(wrapper.text()).toContain("每题选择一个最符合你的选项。");
  expect(wrapper.text()).toContain("目前哪种结果对你来说最重要？");
  expect(wrapper.text()).toContain("提升考试成绩");
  expect(wrapper.text()).not.toContain("Which outcome matters most to you right now?");
  expect(wrapper.text()).not.toContain("Improve exam performance");
  expect(wrapper.text()).not.toContain("DECISION_Q1");
  expect(wrapper.text()).not.toContain("Q1_A");

  const submitButton = wrapper.find('[data-test="assessment-submit"]');
  expect(submitButton.attributes("disabled")).toBeDefined();

  await wrapper.find('input[name="q-1"][value="11"]').setValue();
  await wrapper.find('input[name="q-2"][value="21"]').setValue();
  await flushPromises();

  expect(wrapper.find('[data-test="assessment-submit"]').attributes("disabled")).toBeUndefined();

  await wrapper.find("form").trigger("submit.prevent");
  await flushPromises();

  expect(submitDecisionAnswers).toHaveBeenCalledTimes(1);
  expect(wrapper.find('[data-test="assessment-result"]').exists()).toBe(true);
  const timelineLinks = wrapper.findAll('[data-to="/timeline"]');
  const timelineButtons = wrapper.findAll("button").filter((button) => button.text() === "查看时间线");
  expect(timelineLinks.length + timelineButtons.length).toBe(1);
  expect(wrapper.text()).toContain("推荐方向：考研");
  expect(wrapper.text()).toContain("推荐方向：升学考研/考公。");
  expect(wrapper.text()).not.toContain("Recommendation: EXAM");
  expect(wrapper.text()).not.toContain("Go to Timeline");
  expect(wrapper.text()).not.toContain("Compare Schools");
  expect(wrapper.text()).toContain("方向排序");
});
