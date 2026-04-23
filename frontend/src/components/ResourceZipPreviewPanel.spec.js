import { mount } from "@vue/test-utils";
import { expect, test } from "vitest";
import ResourceZipPreviewPanel from "./ResourceZipPreviewPanel.vue";

test("shows loading, error, empty, and success states for zip previews", async () => {
  const loadingWrapper = mount(ResourceZipPreviewPanel, {
    props: {
      loading: true,
    },
  });
  expect(loadingWrapper.text()).toContain("正在加载压缩包目录...");

  const errorWrapper = mount(ResourceZipPreviewPanel, {
    props: {
      errorMessage: "目录预览失败。",
    },
  });
  expect(errorWrapper.text()).toContain("目录预览失败。");

  const emptyWrapper = mount(ResourceZipPreviewPanel, {
    props: {
      preview: {
        fileName: "empty.zip",
        entryCount: 0,
        entries: [],
      },
    },
  });
  expect(emptyWrapper.text()).toContain("这个压缩包里还没有文件。");

  const successWrapper = mount(ResourceZipPreviewPanel, {
    props: {
      preview: {
        fileName: "notes.zip",
        entryCount: 2,
        entries: [
          { path: "backend/", name: "backend", directory: true, size: null },
          { path: "backend/questions.md", name: "questions.md", directory: false, size: 1834 },
        ],
      },
    },
  });
  expect(successWrapper.text()).toContain("notes.zip");
  expect(successWrapper.text()).toContain("backend/questions.md");
  expect(successWrapper.text()).toContain("2 项");
  expect(successWrapper.text()).toContain("2 KB");
});
