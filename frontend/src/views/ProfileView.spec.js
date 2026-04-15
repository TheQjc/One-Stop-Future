import { expect, test } from "vitest";
import { mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import ProfileView from "./ProfileView.vue";
import { useUserStore } from "../stores/user.js";

test("renders profile form", () => {
  setActivePinia(createPinia());
  const userStore = useUserStore();
  userStore.profile = {
    username: "student01",
    realName: "学生演示账号",
    role: "STUDENT",
    email: "student@example.com",
    bio: "",
  };
  userStore.token = "demo-token";

  const wrapper = mount(ProfileView);
  expect(wrapper.text()).toContain("个人信息");
});
