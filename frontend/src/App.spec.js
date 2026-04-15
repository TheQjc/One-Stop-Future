import { expect, test } from "vitest";
import { mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import App from "./App.vue";

test("renders the phase a shell", () => {
  setActivePinia(createPinia());

  const wrapper = mount(App, {
    global: {
      stubs: {
        RouterView: {
          template: "<div>view</div>",
        },
      },
    },
  });

  expect(wrapper.text()).toContain("One-Stop Future");
});
