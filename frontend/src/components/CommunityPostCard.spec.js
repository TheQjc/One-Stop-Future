import { mount } from "@vue/test-utils";
import { expect, test } from "vitest";
import CommunityPostCard from "./CommunityPostCard.vue";

function mountCard(post) {
  return mount(CommunityPostCard, {
    props: { post },
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

test("localizes the shared experience badge and fallback copy into Chinese", () => {
  const wrapper = mountCard({
    id: 1,
    tag: "CAREER",
    title: "求职经验总结",
    status: "PUBLISHED",
    experience: {
      enabled: true,
      targetLabel: null,
      outcomeLabel: null,
      timelineSummary: null,
    },
  });

  expect(wrapper.get(".community-post-card__tag").text()).toBe("就业");
  expect(wrapper.get(".community-post-card__experience-badge").text()).toBe("经验贴");
  expect(wrapper.get(".community-post-card__preview").text()).toBe("暂无正文摘要");
  expect(wrapper.get(".community-post-card__meta").text()).toContain("匿名用户");
  expect(wrapper.get(".community-post-card__stats").text()).toContain("赞 0");
  expect(wrapper.get(".community-post-card__stats").text()).toContain("评 0");
  expect(wrapper.get(".community-post-card__stats").text()).toContain("藏 0");
});

test("keeps existing Chinese tag and status labels on the shared post card", () => {
  const wrapper = mountCard({
    id: 2,
    tag: "ABROAD",
    title: "留学申请节奏",
    status: "HIDDEN",
    contentPreview: "先准备材料，再安排语言考试。",
    authorNickname: "小林",
    createdAt: "2026-04-16T08:00:00",
    likeCount: 3,
    commentCount: 2,
    favoriteCount: 1,
    experience: {
      enabled: false,
    },
  });

  expect(wrapper.get(".community-post-card__tag").text()).toBe("留学");
  expect(wrapper.get(".status-badge").text()).toBe("已下架");
  expect(wrapper.get(".community-post-card__preview").text()).toBe("先准备材料，再安排语言考试。");
  expect(wrapper.get(".community-post-card__meta").text()).toContain("小林");
});
