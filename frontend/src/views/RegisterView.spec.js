import { expect, test, vi } from "vitest";
import { mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import RegisterView from "./RegisterView.vue";

vi.mock("vue-router", () => ({
  RouterLink: {
    template: "<a><slot /></a>",
  },
  useRouter: () => ({
    push: vi.fn(),
  }),
}));

test("renders chinese-first register copy", () => {
  setActivePinia(createPinia());

  const wrapper = mount(RegisterView, {
    global: {
      stubs: {
        RouterLink: {
          template: "<a><slot /></a>",
        },
      },
    },
  });

  expect(wrapper.text()).toContain("新账号注册");
  expect(wrapper.text()).toContain("手机号验证码注册");
  expect(wrapper.text()).toContain("完成注册");
  expect(wrapper.text()).not.toContain("New Account");
});

test("shows nickname validation before submit", async () => {
  setActivePinia(createPinia());

  const wrapper = mount(RegisterView, {
    global: {
      stubs: {
        RouterLink: {
          template: "<a><slot /></a>",
        },
      },
    },
  });

  await wrapper.find('input[name="phone"]').setValue("13800000009");
  await wrapper.find('input[name="verificationCode"]').setValue("123456");
  await wrapper.find("form").trigger("submit.prevent");

  expect(wrapper.text()).toContain("请输入昵称");
});
