import { flushPromises, mount } from "@vue/test-utils";
import { expect, test, vi } from "vitest";
import ProfileApplicationsView from "./ProfileApplicationsView.vue";
import {
  downloadMyApplicationResume,
  getMyApplications,
  previewMyApplicationResume,
} from "../api/applications.js";

vi.mock("../api/applications.js", () => ({
  getMyApplications: vi.fn(),
  downloadMyApplicationResume: vi.fn(),
  previewMyApplicationResume: vi.fn(),
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
  expect(wrapper.text()).toContain("已提交");
  expect(wrapper.text()).toContain("Intern Resume");
  expect(wrapper.text()).toContain("2026-04-18");
  expect(wrapper.html()).toContain('data-to="/jobs/1"');
});

test("pdf and docx snapshots show preview and download while doc stays download-only", async () => {
  getMyApplications.mockResolvedValue({
    total: 3,
    applications: [
      {
        id: 11,
        jobId: 1,
        jobTitle: "Java Backend Intern",
        companyName: "Future Campus Tech",
        city: "Shenzhen",
        status: "SUBMITTED",
        resumeTitleSnapshot: "PDF Snapshot",
        resumeFileNameSnapshot: "intern.pdf",
        previewAvailable: true,
        previewKind: "FILE",
        submittedAt: "2026-04-22T10:30:00",
      },
      {
        id: 12,
        jobId: 1,
        jobTitle: "Java Backend Intern",
        companyName: "Future Campus Tech",
        city: "Shenzhen",
        status: "SUBMITTED",
        resumeTitleSnapshot: "DOCX Snapshot",
        resumeFileNameSnapshot: "intern.docx",
        previewAvailable: true,
        previewKind: "FILE",
        submittedAt: "2026-04-22T10:35:00",
      },
      {
        id: 13,
        jobId: 1,
        jobTitle: "Java Backend Intern",
        companyName: "Future Campus Tech",
        city: "Shenzhen",
        status: "SUBMITTED",
        resumeTitleSnapshot: "DOC Snapshot",
        resumeFileNameSnapshot: "intern.doc",
        previewAvailable: false,
        previewKind: "NONE",
        submittedAt: "2026-04-22T10:40:00",
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

  expect(wrapper.find('[data-testid="preview-application-resume-11"]').exists()).toBe(true);
  expect(wrapper.find('[data-testid="preview-application-resume-12"]').exists()).toBe(true);
  expect(wrapper.find('[data-testid="preview-application-resume-13"]').exists()).toBe(false);
  expect(wrapper.find('[data-testid="download-application-resume-13"]').exists()).toBe(true);
});

test("preview and download actions call the applicant snapshot helpers", async () => {
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
        resumeTitleSnapshot: "DOCX Snapshot",
        resumeFileNameSnapshot: "intern.docx",
        previewAvailable: true,
        previewKind: "FILE",
        submittedAt: "2026-04-22T10:35:00",
      },
    ],
  });
  previewMyApplicationResume.mockResolvedValue("blob:application-preview");
  downloadMyApplicationResume.mockResolvedValue("intern.docx");

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

  await wrapper.find('[data-testid="preview-application-resume-11"]').trigger("click");
  await wrapper.find('[data-testid="download-application-resume-11"]').trigger("click");

  expect(previewMyApplicationResume).toHaveBeenCalledWith(11);
  expect(downloadMyApplicationResume).toHaveBeenCalledWith(11);
});
