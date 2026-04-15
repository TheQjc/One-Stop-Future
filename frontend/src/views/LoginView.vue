<script setup>
import { reactive, ref } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { useUserStore } from "../stores/user.js";

const userStore = useUserStore();
const route = useRoute();
const router = useRouter();

const form = reactive({
  username: "",
  password: "",
});

const errorMessage = ref("");

async function handleSubmit() {
  errorMessage.value = "";

  if (!form.username || !form.password) {
    errorMessage.value = "请填写用户名和密码。";
    return;
  }

  try {
    await userStore.login(form);
    router.push(route.query.redirect || "/");
  } catch (error) {
    errorMessage.value = error.message || "登录失败，请稍后重试。";
  }
}
</script>

<template>
  <section class="page-stack">
    <div class="hero-grid">
      <article class="section-card">
        <span class="section-eyebrow">Editorial Campus Bulletin</span>
        <h1 class="hero-title">校园一站式信息平台</h1>
        <hr class="editorial-rule" />
        <p class="hero-copy">
          以校园公告为首页核心，整合师生日常所需入口。当前阶段支持登录注册、个人中心、通知公告浏览与教师/管理员审核管理。
        </p>
        <div class="stats-grid" style="margin-top: 24px;">
          <div class="panel-card">
            <strong style="font-size: 28px; font-family: var(--cp-font-display);">3</strong>
            <p class="meta-copy">当前演示角色：学生、教师、管理员</p>
          </div>
          <div class="panel-card">
            <strong style="font-size: 28px; font-family: var(--cp-font-display);">P0</strong>
            <p class="meta-copy">首页聚合、用户中心、通知管理</p>
          </div>
        </div>
      </article>

      <article class="section-card">
        <div class="section-header">
          <div>
            <p class="section-eyebrow">登录入口</p>
            <h2 class="page-title" style="font-size: 34px; margin-top: 16px;">进入平台</h2>
          </div>
        </div>

        <form class="field-grid" @submit.prevent="handleSubmit">
          <label class="field-label">
            用户名
            <input
              v-model.trim="form.username"
              class="field-control"
              name="username"
              type="text"
              autocomplete="username"
              placeholder="例如：admin01 / teacher01 / student01"
            />
          </label>

          <label class="field-label">
            密码
            <input
              v-model.trim="form.password"
              class="field-control"
              name="password"
              type="password"
              autocomplete="current-password"
              placeholder="演示密码：secret123"
            />
          </label>

          <p class="field-hint">演示环境支持离线 mock 数据，可直接使用 README 中的示例账号。</p>
          <p v-if="errorMessage" class="field-error" role="alert">{{ errorMessage }}</p>

          <div class="inline-form-actions">
            <button type="submit" class="app-btn" :disabled="userStore.loading">
              {{ userStore.loading ? "登录中..." : "登录" }}
            </button>
            <RouterLink to="/register" class="app-link">教师/学生注册</RouterLink>
          </div>
        </form>
      </article>
    </div>
  </section>
</template>
