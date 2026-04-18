import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { beforeEach, expect, test, vi } from "vitest";
import TimelineView from "./TimelineView.vue";
import { getDecisionTimeline, getLatestDecisionResult } from "../api/decision.js";

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

  return mount(TimelineView, {
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

test("shows assessment-required empty state when latest result is absent", async () => {
  getLatestDecisionResult.mockResolvedValue({ hasResult: false });

  const wrapper = mountView();
  await flushPromises();

  expect(getDecisionTimeline).not.toHaveBeenCalled();
  expect(wrapper.text()).toContain("Complete the assessment first");
  expect(wrapper.find('[data-test="timeline-empty"]').exists()).toBe(true);
});

test("latest result defaults active track and loads timeline; switching track reloads timeline", async () => {
  getLatestDecisionResult.mockResolvedValue({ hasResult: true, recommendedTrack: "EXAM" });
  getDecisionTimeline
    .mockResolvedValueOnce({
      track: "EXAM",
      anchorDate: "2026-06-01",
      assessmentRequired: false,
      items: [
        {
          phaseCode: "EXAM_P0",
          phaseLabel: "Baseline",
          title: "Set targets",
          summary: "Summary",
          targetDate: "2026-06-01",
          remainingDays: 10,
          actionChecklist: ["a"],
          resourceHint: "hint",
        },
      ],
    })
    .mockResolvedValueOnce({
      track: "CAREER",
      anchorDate: "2026-06-01",
      assessmentRequired: false,
      items: [
        {
          phaseCode: "CAREER_P0",
          phaseLabel: "Baseline",
          title: "Career targets",
          summary: "Career summary",
          targetDate: "2026-06-01",
          remainingDays: 12,
          actionChecklist: [],
          resourceHint: "",
        },
      ],
    });

  const wrapper = mountView();
  await flushPromises();

  expect(getDecisionTimeline).toHaveBeenCalledWith({ track: "EXAM" });
  expect(wrapper.text()).toContain("EXAM_P0");
  expect(wrapper.text()).toContain("2026-06-01");
  expect(wrapper.text()).toContain("10 days");

  const tabs = wrapper.findAll("button.track-tab");
  await tabs.find((btn) => btn.text() === "Career").trigger("click");
  await flushPromises();

  expect(getDecisionTimeline).toHaveBeenLastCalledWith({ track: "CAREER" });
  expect(wrapper.text()).toContain("CAREER_P0");
});

