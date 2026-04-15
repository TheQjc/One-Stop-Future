import { expect, test } from "vitest";
import { mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import NoticeView from "./NoticeView.vue";

test("renders notice filter and list", () => {
  setActivePinia(createPinia());

  const wrapper = mount(NoticeView);
  expect(wrapper.text()).toContain("通知公告");
  expect(wrapper.text()).toContain("分类");
});
