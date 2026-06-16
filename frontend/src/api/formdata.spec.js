import { beforeEach, expect, test, vi } from "vitest";
import { createResume, updateResume } from "./resumes.js";
import { importAdminJobs } from "./admin.js";
import { createResourceUpload, updateResource, uploadResourceChunk } from "./resources.js";

const http = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  delete: vi.fn(),
}));

vi.mock("./http.js", () => ({
  default: http,
}));

beforeEach(() => {
  vi.clearAllMocks();
  http.post.mockResolvedValue({ data: { data: {} } });
  http.put.mockResolvedValue({ data: { data: {} } });
});

test("multipart helpers leave content-type generation to the browser", async () => {
  const resumeFormData = new FormData();
  const resourceFormData = new FormData();
  const chunkFormData = new FormData();
  const jobImportFormData = new FormData();

  await createResume(resumeFormData);
  await updateResume(7, resumeFormData);
  await uploadResourceChunk(3, 2, chunkFormData);
  await updateResource(4, resourceFormData);
  await importAdminJobs(jobImportFormData);

  expect(http.post).toHaveBeenNthCalledWith(1, "/resumes", resumeFormData);
  expect(http.put).toHaveBeenNthCalledWith(1, "/resumes/7", resumeFormData);
  expect(http.post).toHaveBeenNthCalledWith(
    2,
    "/resources/chunk-uploads/3/chunks/2",
    chunkFormData,
    expect.objectContaining({ timeout: 30000 }),
  );
  expect(http.put).toHaveBeenNthCalledWith(2, "/resources/4", resourceFormData);
  expect(http.post).toHaveBeenNthCalledWith(3, "/admin/jobs/import", jobImportFormData);

  const chunkOptions = http.post.mock.calls[1][2];
  expect(chunkOptions).not.toHaveProperty("headers");
});

test("resource upload without a file fails with a Chinese message", async () => {
  const formData = new FormData();
  formData.append("title", "资料标题");

  await expect(createResourceUpload(formData)).rejects.toThrow("请先选择文件");
});
