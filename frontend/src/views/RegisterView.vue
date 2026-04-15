<script setup>
import { reactive, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import { useUserStore } from "../stores/user.js";

const router = useRouter();
const userStore = useUserStore();

const form = reactive({
  username: "",
  password: "",
  realName: "",
  email: "",
  role: "STUDENT",
});

const errorMessage = ref("");

async function handleSubmit() {
  errorMessage.value = "";

  if (!form.username || !form.password || !form.realName) {
    errorMessage.value = "请完整填写用户名、姓名和密码。";
    return;
  }

  try {
    await userStore.register(form);
    router.push("/");
  } catch (error) {
    errorMessage.value = error.message || "注册失败，请稍后重试。";
  }
}
</script>

<template>
  <section class="page-stack">
    <div class="two-col-grid">
      <article class="section-card">
        <span class="section-eyebrow">新用户注册</span>
        <h1 class="page-title" style="margin-top: 18px;">创建校园平台账号</h1>
        <p class="page-subtitle" style="margin-top: 16px;">
          当前支持学生与教师自主注册。教师账号进入平台后即可查看待审核公告，并参与内容审核。
        </p>
        <div class="notice-list" style="margin-top: 24px;">
          <div class="panel-card">
            <strong>学生账号</strong>
            <p class="meta-copy">用于查看首页聚合、通知公告、个人信息以及后续课表与活动入口。</p>
          </div>
          <div class="panel-card">
            <strong>教师账号</strong>
            <p class="meta-copy">用于发布通知、管理公告、参与审核，并为后续课表教师视角打基础。</p>
          </div>
        </div>
      </article>

      <article class="section-card">
        <form class="field-grid" @submit.prevent="handleSubmit">
          <label class="field-label">
            姓名
            <input v-model.trim="form.realName" class="field-control" type="text" placeholder="请输入真实姓名" />
          </label>

          <label class="field-label">
            用户名
            <input v-model.trim="form.username" class="field-control" type="text" placeholder="建议使用工号/学号风格" />
          </label>

          <label class="field-label">
            邮箱
            <input v-model.trim="form.email" class="field-control" type="email" placeholder="用于接收后续通知" />
          </label>

          <label class="field-label">
            注册角色
            <select v-model="form.role" class="field-select">
              <option value="STUDENT">学生</option>
              <option value="TEACHER">教师</option>
            </select>
          </label>

          <label class="field-label">
            密码
            <input v-model.trim="form.password" class="field-control" type="password" placeholder="至少 8 位字符" />
          </label>

          <p class="field-hint">教师允许自主注册，用户名需唯一；注册后将自动进入首页。</p>
          <p v-if="errorMessage" class="field-error" role="alert">{{ errorMessage }}</p>

          <div class="inline-form-actions">
            <button type="submit" class="app-btn" :disabled="userStore.loading">
              {{ userStore.loading ? "提交中..." : "完成注册" }}
            </button>
            <RouterLink to="/login" class="ghost-btn">返回登录</RouterLink>
          </div>
        </form>
      </article>
    </div>
  </section>
</template>
