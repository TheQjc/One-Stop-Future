import { vi } from "vitest";
import { config } from "@vue/test-utils";

vi.mock("vue-router", async () => {
  const actual = await vi.importActual("vue-router");

  return {
    ...actual,
    useRouter: () => ({
      push: vi.fn(),
    }),
    useRoute: () => ({
      query: {},
      params: {},
      fullPath: "/",
    }),
  };
});

config.global.stubs = {
  RouterLink: {
    props: ["to"],
    template: "<a :href=\"typeof to === 'string' ? to : '#'\"><slot /></a>",
  },
  RouterView: {
    template: "<div />",
  },
};

window.localStorage.clear();
