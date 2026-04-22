import { mount } from "@vue/test-utils";
import { expect, test } from "vitest";
import SearchResultCard from "./SearchResultCard.vue";

function mountCard(item) {
  return mount(SearchResultCard, {
    props: { item },
    global: {
      stubs: {
        RouterLink: {
          props: ["to"],
          template: "<a :data-to='typeof to === \"string\" ? to : JSON.stringify(to)'><slot /></a>",
        },
      },
    },
  });
}

test("localizes shared search-result fallback copy into Chinese", () => {
  const wrapper = mountCard({
    path: "/search/1",
    title: "搜索结果卡片",
    type: "POST",
  });

  expect(wrapper.get(".search-result-card__type").text()).toBe("社区");
  expect(wrapper.get(".search-result-card__summary").text()).toBe("暂未提供内容摘要");
  expect(wrapper.get(".search-result-card__date").text()).toBe("待发布");
  expect(wrapper.get(".search-result-card__meta").text()).toContain("站内搜索");
  expect(wrapper.get(".search-result-card__meta").text()).toContain("搜索结果");
});

test("maps shared search-result type badges to Chinese labels", () => {
  const jobCard = mountCard({
    path: "/search/2",
    title: "校招岗位",
    type: "JOB",
    summary: "查看岗位更新。",
    publishedAt: "2026-04-16T08:00:00",
  });
  const resourceCard = mountCard({
    path: "/search/3",
    title: "申请资料",
    type: "RESOURCE",
    summary: "汇总常用材料。",
    publishedAt: "2026-04-16T09:00:00",
  });

  expect(jobCard.get(".search-result-card__type").text()).toBe("岗位");
  expect(resourceCard.get(".search-result-card__type").text()).toBe("资料");
});
