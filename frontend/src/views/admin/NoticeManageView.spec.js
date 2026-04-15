import { expect, test } from "vitest";
import { mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import NoticeManageView from "./NoticeManageView.vue";
import { useUserStore } from "../../stores/user.js";

test("shows review actions for teacher/admin", () => {
  setActivePinia(createPinia());
  const userStore = useUserStore();
  userStore.profile = {
    username: "teacher01",
    realName: "教师演示账号",
    role: "TEACHER",
    email: "teacher@example.com",
    bio: "",
  };
  userStore.token = "demo-token";

  const wrapper = mount(NoticeManageView);
  expect(wrapper.text()).toContain("审核");
});
