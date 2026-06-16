import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, expect, test, vi } from "vitest";
import ProfileResumesView from "./ProfileResumesView.vue";
import {
  createResume,
  deleteResume,
  downloadResume,
  getMyResumes,
  previewResume,
  updateResume,
} from "../api/resumes.js";

vi.mock("../api/resumes.js", () => ({
  createResume: vi.fn(),
  deleteResume: vi.fn(),
  downloadResume: vi.fn(),
  getMyResumes: vi.fn(),
  previewResume: vi.fn(),
  updateResume: vi.fn(),
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
          previewAvailable: true,
          previewKind: "FILE",
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
    previewAvailable: true,
    previewKind: "FILE",
  });
  deleteResume.mockResolvedValue(true);
  downloadResume.mockResolvedValue("intern-resume.pdf");

  const wrapper = mount(ProfileResumesView);
  await flushPromises();

  expect(wrapper.text()).toContain("你还没有上传任何简历。");

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

test("rejects unsupported resume files before upload", async () => {
  getMyResumes.mockResolvedValue({ total: 0, resumes: [] });

  const wrapper = mount(ProfileResumesView);
  await flushPromises();

  const file = new File(["archive"], "portfolio.zip", { type: "application/zip" });
  await wrapper.find('input[name="title"]').setValue("Portfolio Archive");
  const fileInput = wrapper.find('input[name="file"]');
  Object.defineProperty(fileInput.element, "files", {
    value: [file],
    configurable: true,
  });
  await fileInput.trigger("change");
  await wrapper.find("form").trigger("submit.prevent");
  await flushPromises();

  expect(createResume).not.toHaveBeenCalled();
  expect(wrapper.text()).toContain("仅支持 PDF、DOC、DOCX 简历文件。");
});

test("pdf and docx resumes show preview while doc stays download-only", async () => {
  getMyResumes.mockResolvedValue({
    total: 3,
    resumes: [
      {
        id: 1,
        title: "PDF Resume",
        fileName: "resume.pdf",
        previewAvailable: true,
        previewKind: "FILE",
      },
      {
        id: 2,
        title: "DOCX Resume",
        fileName: "resume.docx",
        previewAvailable: true,
        previewKind: "FILE",
      },
      {
        id: 3,
        title: "DOC Resume",
        fileName: "resume.doc",
        previewAvailable: false,
        previewKind: "NONE",
      },
    ],
  });
  previewResume.mockResolvedValue("blob:resume-preview");

  const wrapper = mount(ProfileResumesView);
  await flushPromises();

  expect(wrapper.find('[data-testid="preview-resume-1"]').exists()).toBe(true);
  expect(wrapper.find('[data-testid="preview-resume-2"]').exists()).toBe(true);
  expect(wrapper.find('[data-testid="preview-resume-3"]').exists()).toBe(false);

  await wrapper.find('[data-testid="preview-resume-2"]').trigger("click");

  expect(previewResume).toHaveBeenCalledWith(2);
});

test("renames and optionally replaces an existing resume", async () => {
  getMyResumes
    .mockResolvedValueOnce({
      total: 1,
      resumes: [
        {
          id: 8,
          title: "Old Resume",
          fileName: "old-resume.pdf",
          previewAvailable: true,
          previewKind: "FILE",
        },
      ],
    })
    .mockResolvedValueOnce({
      total: 1,
      resumes: [
        {
          id: 8,
          title: "New Resume",
          fileName: "new-resume.docx",
          previewAvailable: true,
          previewKind: "FILE",
        },
      ],
    });
  updateResume.mockResolvedValue({
    id: 8,
    title: "New Resume",
    fileName: "new-resume.docx",
  });

  const wrapper = mount(ProfileResumesView);
  await flushPromises();

  await wrapper.find('[data-testid="edit-resume-8"]').trigger("click");
  await wrapper.find('[data-testid="edit-resume-title-8"]').setValue("New Resume");
  const fileInput = wrapper.find('[data-testid="edit-resume-file-8"]');
  const file = new File(["docx"], "new-resume.docx", {
    type: "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
  });
  Object.defineProperty(fileInput.element, "files", {
    value: [file],
    configurable: true,
  });
  await fileInput.trigger("change");
  await wrapper.find('[data-testid="save-resume-8"]').trigger("submit");
  await flushPromises();

  expect(updateResume).toHaveBeenCalledTimes(1);
  expect(updateResume.mock.calls[0][0]).toBe(8);
  expect(updateResume.mock.calls[0][1].get("title")).toBe("New Resume");
  expect(updateResume.mock.calls[0][1].get("file")).toBe(file);
  expect(wrapper.text()).toContain("New Resume");
});
