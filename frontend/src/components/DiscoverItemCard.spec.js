import { mount } from "@vue/test-utils";
import { expect, test } from "vitest";
import DiscoverItemCard from "./DiscoverItemCard.vue";

function mountCard(item) {
  return mount(DiscoverItemCard, {
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

test("localizes shared discover fallback copy into Chinese", () => {
  const wrapper = mountCard({
    path: "/discover/1",
    title: "本周内容",
    type: "POST",
  });

  expect(wrapper.get(".discover-item-card__type").text()).toBe("社区");
  expect(wrapper.get(".discover-item-card__label").text()).toBe("本周推荐");
  expect(wrapper.get(".discover-item-card__summary").text()).toBe("暂未提供内容摘要");
  expect(wrapper.get(".discover-item-card__meta").text()).toContain("平台推荐");
  expect(wrapper.text()).toContain("待发布");
  expect(wrapper.get(".discover-item-card__score").text()).toBe("热度 0");
});

test("maps discover item type badges to Chinese labels", () => {
  const jobCard = mountCard({
    path: "/discover/2",
    title: "校招岗位",
    type: "JOB",
    hotLabel: "重点关注",
    summary: "查看岗位更新。",
    publishedAt: "2026-04-16T08:00:00",
    hotScore: 15,
  });
  const resourceCard = mountCard({
    path: "/discover/3",
    title: "申请资料",
    type: "RESOURCE",
    hotLabel: "资料整理",
    summary: "汇总常用材料。",
    publishedAt: "2026-04-16T09:00:00",
    hotScore: 7,
  });

  expect(jobCard.get(".discover-item-card__type").text()).toBe("岗位");
  expect(resourceCard.get(".discover-item-card__type").text()).toBe("资料");
});
