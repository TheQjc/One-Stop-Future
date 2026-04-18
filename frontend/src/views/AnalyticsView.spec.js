import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { beforeEach, expect, test, vi } from "vitest";
import AnalyticsView from "./AnalyticsView.vue";
import router from "../router/index.js";
import { getAnalyticsSummary } from "../api/analytics.js";

vi.mock("../api/analytics.js", () => ({
  getAnalyticsSummary: vi.fn(),
}));

function buildTrendPoints(length = 30) {
  return Array.from({ length }, (_, index) => ({
    date: `2026-04-${String(index + 1).padStart(2, "0")}`,
    posts: index % 3 === 0 ? 1 : 0,
    jobs: index % 4 === 0 ? 1 : 0,
    resources: index % 5 === 0 ? 2 : 0,
    assessments: index % 6 === 0 ? 1 : 0,
  }));
}

function buildGuestSummary(overrides = {}) {
  return {
    publicOverview: {
      publishedPostCount: 3,
      activeJobCount: 2,
      publishedResourceCount: 4,
      assessmentSessionCount: 5,
    },
    publicTrends: buildTrendPoints(),
    decisionDistribution: {
      participantCount: 2,
      tracks: [
        { track: "CAREER", count: 1, percent: 50 },
        { track: "EXAM", count: 1, percent: 50 },
        { track: "ABROAD", count: 0, percent: 0 },
      ],
    },
    personalStatus: "ANONYMOUS",
    personalMessage: null,
    personalSnapshot: null,
    personalHistory: [],
    nextActions: [],
    ...overrides,
  };
}

beforeEach(() => {
  vi.clearAllMocks();
  window.localStorage.clear();
});

function mountView() {
  setActivePinia(createPinia());

  return mount(AnalyticsView, {
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

test("router exposes analytics as a public route", () => {
  expect(router.resolve("/analytics").name).toBe("analytics");
  expect(router.resolve("/analytics").meta.requiresAuth).toBeUndefined();
});

test("guest sees public analytics sections and a login prompt for personal insights", async () => {
  getAnalyticsSummary.mockResolvedValue(buildGuestSummary());

  const wrapper = mountView();
  await flushPromises();

  expect(getAnalyticsSummary).toHaveBeenCalledWith({ period: "30D" });
  expect(wrapper.text()).toContain("Decision Mix");
  expect(wrapper.text()).toContain("Log in to unlock your personal path analysis");
  expect(wrapper.text()).toContain("Published Posts");
  expect(wrapper.text()).toContain("Participant base: 2");
  expect(wrapper.html()).toContain('data-to="/login"');
});

test("period switching triggers a refetch with the selected period", async () => {
  getAnalyticsSummary.mockResolvedValue(buildGuestSummary());

  const wrapper = mountView();
  await flushPromises();

  await wrapper.find('[data-test="period-7D"]').trigger("click");
  await flushPromises();

  expect(getAnalyticsSummary).toHaveBeenLastCalledWith({ period: "7D" });
});

test("renders an explicit empty-trend note when the selected period has no activity", async () => {
  getAnalyticsSummary.mockResolvedValue(buildGuestSummary({
    publicTrends: buildTrendPoints().map((item) => ({
      ...item,
      posts: 0,
      jobs: 0,
      resources: 0,
      assessments: 0,
    })),
  }));

  const wrapper = mountView();
  await flushPromises();

  expect(wrapper.text()).toContain("No public activity was recorded for this period yet.");
});

test("ready personal status renders snapshot, history, and next actions", async () => {
  getAnalyticsSummary.mockResolvedValue(buildGuestSummary({
    personalStatus: "READY",
    personalSnapshot: {
      hasAssessment: true,
      recommendedTrack: "EXAM",
      summaryText: "Recommendation: EXAM",
      sessionDate: "2026-04-18",
      scores: { career: 1, exam: 9, abroad: 0 },
    },
    personalHistory: [
      { sessionDate: "2026-04-18", recommendedTrack: "EXAM", careerScore: 1, examScore: 9, abroadScore: 0 },
      { sessionDate: "2026-04-01", recommendedTrack: "CAREER", careerScore: 8, examScore: 2, abroadScore: 0 },
    ],
    nextActions: [
      { code: "START_ASSESSMENT", label: "Start Assessment", path: "/assessment", description: "desc" },
      { code: "OPEN_TIMELINE", label: "Open Timeline", path: "/timeline", description: "desc2" },
    ],
  }));

  const wrapper = mountView();
  await flushPromises();

  expect(wrapper.find('[data-test="personal-ready"]').exists()).toBe(true);
  expect(wrapper.text()).toContain("Recommended: EXAM");
  expect(wrapper.text()).toContain("2026-04-18");
  expect(wrapper.text()).toContain("Recent sessions");
  expect(wrapper.html()).toContain('data-to="/timeline"');
});

test("empty personal status shows assessment CTA", async () => {
  getAnalyticsSummary.mockResolvedValue(buildGuestSummary({
    personalStatus: "EMPTY",
    personalSnapshot: { hasAssessment: false },
    personalHistory: [],
    nextActions: [{ code: "START_ASSESSMENT", label: "Start Assessment", path: "/assessment", description: "desc" }],
  }));

  const wrapper = mountView();
  await flushPromises();

  expect(wrapper.find('[data-test="personal-empty"]').exists()).toBe(true);
  expect(wrapper.html()).toContain('data-to="/assessment"');
});

test("error personal status shows personalMessage while keeping public analytics", async () => {
  getAnalyticsSummary.mockResolvedValue(buildGuestSummary({
    personalStatus: "ERROR",
    personalMessage: "Personal analytics temporarily unavailable.",
    personalSnapshot: null,
    personalHistory: [],
    nextActions: [],
  }));

  const wrapper = mountView();
  await flushPromises();

  expect(wrapper.find('[data-test="personal-error"]').exists()).toBe(true);
  expect(wrapper.text()).toContain("Personal analytics temporarily unavailable.");
  expect(wrapper.text()).toContain("Decision Mix");
  expect(wrapper.text()).toContain("Published Posts");
});
