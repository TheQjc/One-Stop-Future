import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { beforeEach, expect, test, vi } from "vitest";
import JobDetailView from "./JobDetailView.vue";
import { useUserStore } from "../stores/user.js";
import { applyToJob, favoriteJob, getJobDetail, unfavoriteJob } from "../api/jobs.js";
import { getMyResumes } from "../api/resumes.js";

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
  applyToJob: vi.fn(),
  favoriteJob: vi.fn(),
  getJobDetail: vi.fn(),
  unfavoriteJob: vi.fn(),
}));

vi.mock("../api/resumes.js", () => ({
  getMyResumes: vi.fn(),
}));

const baseDetail = {
  id: 11,
  title: "求职数据分析实习生",
  companyName: "未来校园科技",
  city: "深圳",
  jobType: "INTERNSHIP",
  educationRequirement: "BACHELOR",
  sourcePlatform: "官网",
  sourceUrl: "https://jobs.example.com/offer-analyst-intern",
  summary: "协助整理校招投递和面试数据。",
  content: "和产品、运营同学一起分析岗位转化，输出面向学生的求职建议。",
  deadlineAt: "2026-05-20T18:00:00",
  favoritedByMe: false,
};

beforeEach(() => {
  vi.clearAllMocks();
  window.localStorage.clear();
  getJobDetail.mockResolvedValue({ ...baseDetail, appliedByMe: false, applicationId: null });
  getMyResumes.mockResolvedValue({ total: 0, resumes: [] });
  applyToJob.mockResolvedValue({
    id: 1001,
    jobId: 11,
    status: "SUBMITTED",
    resumeTitleSnapshot: "Intern Resume",
    resumeFileNameSnapshot: "intern-resume.pdf",
    submittedAt: "2026-04-18T10:00:00",
  });
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

test("guest apply click redirects to login", async () => {
  const wrapper = mountView();
  await flushPromises();

  await wrapper.find('[data-testid="apply-toggle"]').trigger("click");

  expect(routerPush).toHaveBeenCalledWith({
    name: "login",
    query: { redirect: "/jobs/11" },
  });
});

test("authenticated user with no resumes sees upload guidance before applying", async () => {
  getMyResumes.mockResolvedValue({ total: 0, resumes: [] });

  const wrapper = mountView(true);
  await flushPromises();

  await wrapper.find('[data-testid="apply-toggle"]').trigger("click");
  await flushPromises();

  expect(wrapper.text()).toContain("请先上传一份简历");
  expect(wrapper.html()).toContain('data-to="/profile/resumes"');
});

test("authenticated user can apply with a selected resume and then sees applied state", async () => {
  getMyResumes.mockResolvedValue({
    total: 1,
    resumes: [{ id: 21, title: "Intern Resume", fileName: "intern-resume.pdf" }],
  });

  const wrapper = mountView(true);
  await flushPromises();

  await wrapper.find('[data-testid="apply-toggle"]').trigger("click");
  await flushPromises();
  await wrapper.find('input[type="radio"][value="21"]').setValue();
  await wrapper.find('[data-testid="submit-application"]').trigger("click");
  await flushPromises();

  expect(applyToJob).toHaveBeenCalledWith(11, { resumeId: 21 });
  expect(wrapper.text()).toContain("已投递");
  expect(wrapper.html()).toContain('data-to="/profile/applications"');
});

test("favorites a job for authenticated users", async () => {
  favoriteJob.mockResolvedValue({
    ...baseDetail,
    appliedByMe: false,
    favoritedByMe: true,
  });

  const wrapper = mountView(true);
  await flushPromises();

  await wrapper.find('[data-testid="favorite-toggle"]').trigger("click");
  await flushPromises();

  expect(favoriteJob).toHaveBeenCalledWith(11);
  expect(wrapper.text()).toContain("取消收藏");
  expect(unfavoriteJob).not.toHaveBeenCalled();
});
