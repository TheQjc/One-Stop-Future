import { expect, test, vi } from "vitest";
import { mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import LoginView from "./LoginView.vue";

vi.mock("vue-router", () => ({
  RouterLink: {
    template: "<a><slot /></a>",
  },
  useRoute: () => ({
    query: {},
  }),
  useRouter: () => ({
    push: vi.fn(),
  }),
}));

test("renders phone code login form", async () => {
  setActivePinia(createPinia());

  const wrapper = mount(LoginView, {
    global: {
      stubs: {
        RouterLink: {
          template: "<a><slot /></a>",
        },
      },
    },
  });

  expect(wrapper.text()).toContain("手机号验证码登录");
  expect(wrapper.find('input[name="phone"]').exists()).toBe(true);
  expect(wrapper.find('input[name="verificationCode"]').exists()).toBe(true);
  expect(wrapper.find("button[type='submit']").exists()).toBe(true);

  await wrapper.find("button[type='button']").trigger("click");
  expect(wrapper.text()).toContain("11 位手机号");
});
