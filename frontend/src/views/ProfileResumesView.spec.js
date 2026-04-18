import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, expect, test, vi } from "vitest";
import ProfileResumesView from "./ProfileResumesView.vue";
import { createResume, deleteResume, downloadResume, getMyResumes } from "../api/resumes.js";

vi.mock("../api/resumes.js", () => ({
  createResume: vi.fn(),
  deleteResume: vi.fn(),
  downloadResume: vi.fn(),
  getMyResumes: vi.fn(),
}));

beforeEach(() => {
  vi.clearAllMocks();
});

test("loads resumes and supports upload/download/delete actions", async () => {
  getMyResumes
    .mockResolvedValueOnce({ total: 0, resumes: [] })
    .mockResolvedValueOnce({
      total: 1,
      resumes: [
        {
          id: 1,
          title: "Intern Resume",
          fileName: "intern-resume.pdf",
          fileSize: 1024,
          createdAt: "2026-04-18T10:00:00",
        },
      ],
    })
    .mockResolvedValueOnce({ total: 0, resumes: [] });

  createResume.mockResolvedValue({
    id: 1,
    title: "Intern Resume",
    fileName: "intern-resume.pdf",
  });
  deleteResume.mockResolvedValue(true);
  downloadResume.mockResolvedValue("intern-resume.pdf");

  const wrapper = mount(ProfileResumesView);
  await flushPromises();

  expect(wrapper.text()).toContain("You have not uploaded any resumes yet.");

  const file = new File(["resume"], "intern-resume.pdf", { type: "application/pdf" });
  await wrapper.find('input[name="title"]').setValue("Intern Resume");
  const fileInput = wrapper.find('input[name="file"]');
  Object.defineProperty(fileInput.element, "files", {
    value: [file],
    configurable: true,
  });
  await fileInput.trigger("change");
  await wrapper.find("form").trigger("submit.prevent");
  await flushPromises();

  expect(createResume).toHaveBeenCalledTimes(1);
  expect(wrapper.text()).toContain("Intern Resume");

  await wrapper.find('[data-testid="download-resume-1"]').trigger("click");

  expect(downloadResume).toHaveBeenCalledWith(1);

  await wrapper.find('[data-testid="delete-resume-1"]').trigger("click");
  await flushPromises();

  expect(deleteResume).toHaveBeenCalledWith(1);
});
