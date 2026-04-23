import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { beforeEach, expect, test } from "vitest";
import AdminVerificationReviewView from "./AdminVerificationReviewView.vue";
import { useUserStore } from "../../stores/user.js";

const USER_KEY = "one-stop-future-demo-users";

beforeEach(() => {
  window.localStorage.clear();
});

test("approves a pending verification application", async () => {
  setActivePinia(createPinia());
  const userStore = useUserStore();

  window.localStorage.setItem(USER_KEY, JSON.stringify([
    {
      id: 1,
      phone: "13800000000",
      nickname: "平台管理员",
      role: "ADMIN",
      status: "ACTIVE",
      verificationStatus: "UNVERIFIED",
    },
    {
      id: 2,
      phone: "13800000001",
      nickname: "普通同学",
      role: "USER",
      status: "ACTIVE",
      verificationStatus: "PENDING",
    },
    {
      id: 3,
      phone: "13800000002",
      nickname: "认证同学",
      role: "USER",
      status: "ACTIVE",
      verificationStatus: "VERIFIED",
      studentId: "20260001",
    },
  ]));

  userStore.token = "demo-token";
  userStore.persistProfile({
    id: 1,
    userId: 1,
    phone: "13800000000",
    nickname: "平台管理员",
    role: "ADMIN",
    verificationStatus: "UNVERIFIED",
  });

  const wrapper = mount(AdminVerificationReviewView);
  await flushPromises();

  expect(wrapper.text()).toContain("普通同学");
  expect(wrapper.text()).toContain("待审核");

  const approveButton = wrapper.findAll("button").find((node) => node.text().includes("通过"));
  await approveButton.trigger("click");
  await flushPromises();

  const updatedUsers = JSON.parse(window.localStorage.getItem(USER_KEY));
  const applicant = updatedUsers.find((item) => item.id === 2);

  expect(wrapper.text()).toContain("审核操作已完成");
  expect(applicant.verificationStatus).toBe("VERIFIED");
  expect(applicant.studentId).toBe("20260009");
});
