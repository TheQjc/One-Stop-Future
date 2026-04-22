import { flushPromises, mount } from "@vue/test-utils";
import { expect, test, vi } from "vitest";
import AdminApplicationsView from "./AdminApplicationsView.vue";
import {
  downloadAdminApplicationResume,
  getAdminApplications,
  previewAdminApplicationResume,
} from "../../api/admin.js";

vi.mock("../../api/admin.js", () => ({
  downloadAdminApplicationResume: vi.fn(),
  getAdminApplications: vi.fn(),
  previewAdminApplicationResume: vi.fn(),
}));

test("renders the read-only admin applications workbench", async () => {
  getAdminApplications.mockResolvedValue({
    total: 1,
    submittedToday: 1,
    uniqueApplicants: 1,
    uniqueJobs: 1,
    applications: [
      {
        id: 1001,
        jobId: 1,
        jobTitle: "Java Backend Intern",
        companyName: "Future Campus Tech",
        applicantUserId: 2,
        applicantNickname: "NormalUser",
        resumeFileNameSnapshot: "intern-resume.pdf",
        previewAvailable: true,
        previewKind: "FILE",
        status: "SUBMITTED",
        submittedAt: "2026-04-18T10:30:00",
      },
    ],
  });

  const wrapper = mount(AdminApplicationsView, {
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
  expect(wrapper.text()).toContain("2");
  expect(wrapper.text()).toContain("NormalUser");
  expect(wrapper.text()).toContain("intern-resume.pdf");
  expect(wrapper.text()).not.toContain("Approve");
  expect(wrapper.text()).not.toContain("Reject");
  expect(wrapper.html()).toContain('data-to="/jobs/1"');
});

test("download action calls the admin resume snapshot helper", async () => {
  getAdminApplications.mockResolvedValue({
    total: 1,
    submittedToday: 1,
    uniqueApplicants: 1,
    uniqueJobs: 1,
    applications: [
      {
        id: 1001,
        jobId: 1,
        jobTitle: "Java Backend Intern",
        companyName: "Future Campus Tech",
        applicantUserId: 2,
        applicantNickname: "NormalUser",
        resumeFileNameSnapshot: "intern-resume.pdf",
        previewAvailable: true,
        previewKind: "FILE",
        status: "SUBMITTED",
        submittedAt: "2026-04-18T10:30:00",
      },
    ],
  });

  const wrapper = mount(AdminApplicationsView, {
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

  await wrapper.find('[data-testid="download-application-resume-1001"]').trigger("click");

  expect(downloadAdminApplicationResume).toHaveBeenCalledWith(1001);
});

test("admin shows preview for previewable snapshots and hides it for doc", async () => {
  getAdminApplications.mockResolvedValue({
    total: 2,
    submittedToday: 2,
    uniqueApplicants: 1,
    uniqueJobs: 1,
    applications: [
      {
        id: 1001,
        jobId: 1,
        jobTitle: "Java Backend Intern",
        companyName: "Future Campus Tech",
        applicantUserId: 2,
        applicantNickname: "NormalUser",
        resumeFileNameSnapshot: "intern.docx",
        previewAvailable: true,
        previewKind: "FILE",
        status: "SUBMITTED",
        submittedAt: "2026-04-22T10:30:00",
      },
      {
        id: 1002,
        jobId: 1,
        jobTitle: "Java Backend Intern",
        companyName: "Future Campus Tech",
        applicantUserId: 2,
        applicantNickname: "NormalUser",
        resumeFileNameSnapshot: "intern.doc",
        previewAvailable: false,
        previewKind: "NONE",
        status: "SUBMITTED",
        submittedAt: "2026-04-22T10:35:00",
      },
    ],
  });

  const wrapper = mount(AdminApplicationsView, {
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

  expect(wrapper.find('[data-testid="preview-application-resume-1001"]').exists()).toBe(true);
  expect(wrapper.find('[data-testid="preview-application-resume-1002"]').exists()).toBe(false);
});

test("preview action calls the admin snapshot preview helper", async () => {
  getAdminApplications.mockResolvedValue({
    total: 1,
    submittedToday: 1,
    uniqueApplicants: 1,
    uniqueJobs: 1,
    applications: [
      {
        id: 1001,
        jobId: 1,
        jobTitle: "Java Backend Intern",
        companyName: "Future Campus Tech",
        applicantUserId: 2,
        applicantNickname: "NormalUser",
        resumeFileNameSnapshot: "intern.docx",
        previewAvailable: true,
        previewKind: "FILE",
        status: "SUBMITTED",
        submittedAt: "2026-04-22T10:30:00",
      },
    ],
  });
  previewAdminApplicationResume.mockResolvedValue("blob:admin-application-preview");

  const wrapper = mount(AdminApplicationsView, {
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

  await wrapper.find('[data-testid="preview-application-resume-1001"]').trigger("click");

  expect(previewAdminApplicationResume).toHaveBeenCalledWith(1001);
});
