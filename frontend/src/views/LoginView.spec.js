import { expect, test } from "vitest";
import { mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import LoginView from "./LoginView.vue";

test("renders login form fields", () => {
  setActivePinia(createPinia());

  const wrapper = mount(LoginView);

  expect(wrapper.find('input[name="username"]').exists()).toBe(true);
  expect(wrapper.find('input[name="password"]').exists()).toBe(true);
  expect(wrapper.find("button[type='submit']").exists()).toBe(true);
});
