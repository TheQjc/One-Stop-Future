import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { beforeEach, expect, test, vi } from "vitest";
import JobDetailView from "./JobDetailView.vue";
import { useUserStore } from "../stores/user.js";
import { favoriteJob, getJobDetail, unfavoriteJob } from "../api/jobs.js";

const routeMock = {
  params: { id: "11" },
  fullPath: "/jobs/11",
};

const routerPush = vi.fn();

vi.mock("vue-router", async () => {
  const actual = await vi.importActual("vue-router");

  return {
    ...actual,
    useRoute: () => routeMock,
    useRouter: () => ({
      push: routerPush,
    }),
  };
});

vi.mock("../api/jobs.js", () => ({
  favoriteJob: vi.fn(),
  getJobDetail: vi.fn(),
  unfavoriteJob: vi.fn(),
}));

const baseDetail = {
  id: 11,
  title: "Offer Analyst Intern",
  companyName: "Future Campus Tech",
  city: "Shenzhen",
  jobType: "INTERNSHIP",
  educationRequirement: "BACHELOR",
  sourcePlatform: "Official Site",
  sourceUrl: "https://jobs.example.com/offer-analyst-intern",
  summary: "A short job summary.",
  content: "A longer explanation of the role.",
  deadlineAt: "2026-05-20T18:00:00",
  favoritedByMe: false,
};

beforeEach(() => {
  vi.clearAllMocks();
  window.localStorage.clear();
  getJobDetail.mockResolvedValue({ ...baseDetail });
});

function mountView(authenticated = false) {
  setActivePinia(createPinia());
  const userStore = useUserStore();

  if (authenticated) {
    userStore.token = "demo-token";
    userStore.persistProfile({
      id: 7,
      userId: 7,
      nickname: "SignedInUser",
      role: "USER",
      verificationStatus: "VERIFIED",
    });
  }

  return mount(JobDetailView, {
    global: {
      stubs: {
        RouterLink: {
          props: ["to"],
          template: "<a :data-to='to'><slot /></a>",
        },
      },
    },
  });
}

test("redirects guests to login when they try to favorite a job", async () => {
  const wrapper = mountView();
  await flushPromises();

  expect(getJobDetail).toHaveBeenCalledWith("11");
  expect(wrapper.find('[data-testid="source-link"]').attributes("href")).toBe(
    "https://jobs.example.com/offer-analyst-intern",
  );

  await wrapper.find('[data-testid="favorite-toggle"]').trigger("click");

  expect(routerPush).toHaveBeenCalledWith({
    name: "login",
    query: { redirect: "/jobs/11" },
  });
  expect(favoriteJob).not.toHaveBeenCalled();
});

test("favorites a job for authenticated users", async () => {
  favoriteJob.mockResolvedValue({
    ...baseDetail,
    favoritedByMe: true,
  });

  const wrapper = mountView(true);
  await flushPromises();

  await wrapper.find('[data-testid="favorite-toggle"]').trigger("click");
  await flushPromises();

  expect(favoriteJob).toHaveBeenCalledWith(11);
  expect(wrapper.text()).toContain("Remove From Favorites");
  expect(unfavoriteJob).not.toHaveBeenCalled();
});
