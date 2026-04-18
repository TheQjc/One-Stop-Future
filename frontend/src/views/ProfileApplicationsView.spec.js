import { flushPromises, mount } from "@vue/test-utils";
import { expect, test, vi } from "vitest";
import ProfileApplicationsView from "./ProfileApplicationsView.vue";
import { getMyApplications } from "../api/applications.js";

vi.mock("../api/applications.js", () => ({
  getMyApplications: vi.fn(),
}));

test("renders my application history", async () => {
  getMyApplications.mockResolvedValue({
    total: 1,
    applications: [
      {
        id: 11,
        jobId: 1,
        jobTitle: "Java Backend Intern",
        companyName: "Future Campus Tech",
        city: "Shenzhen",
        status: "SUBMITTED",
        resumeTitleSnapshot: "Intern Resume",
        resumeFileNameSnapshot: "intern-resume.pdf",
        submittedAt: "2026-04-18T10:30:00",
      },
    ],
  });

  const wrapper = mount(ProfileApplicationsView, {
    global: {
      stubs: {
        RouterLink: {
          props: ["to"],
          template: "<a :data-to='to'><slot /></a>",
        },
      },
    },
  });
  await flushPromises();

  expect(wrapper.text()).toContain("Java Backend Intern");
  expect(wrapper.text()).toContain("Future Campus Tech");
  expect(wrapper.text()).toContain("Shenzhen");
  expect(wrapper.text()).toContain("SUBMITTED");
  expect(wrapper.text()).toContain("Intern Resume");
  expect(wrapper.text()).toContain("2026-04-18");
  expect(wrapper.html()).toContain('data-to="/jobs/1"');
});
