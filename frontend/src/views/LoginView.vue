<script setup>
import { computed, onBeforeUnmount, reactive, ref } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { sendCode } from "../api/auth.js";
import { useUserStore } from "../stores/user.js";

const userStore = useUserStore();
const route = useRoute();
const router = useRouter();

const form = reactive({
  phone: "",
  verificationCode: "",
});

const demoAccounts = [
  {
    phone: "13800000000",
    label: "管理员账号",
    note: "可直接进入认证审核台",
  },
  {
    phone: "13800000001",
    label: "普通用户账号",
    note: "用于验证游客登录后的首页视图",
  },
  {
    phone: "13800000002",
    label: "已认证用户账号",
    note: "用于查看已认证状态与通知入口",
  },
  {
    phone: "13800000003",
    label: "教师审核账号",
    note: "可进入认证审核台并处理待审申请",
  },
];

const errorMessage = ref("");
const debugCode = ref("");
const sendingCode = ref(false);
const cooldown = ref(0);

let timerId = null;

const sendCodeLabel = computed(() => (
  cooldown.value > 0 ? `${cooldown.value}s 后重试` : "获取验证码"
));

function clearCooldown() {
  if (timerId) {
    window.clearInterval(timerId);
    timerId = null;
  }
}

function startCooldown(seconds = 60) {
  clearCooldown();
  cooldown.value = seconds;

  timerId = window.setInterval(() => {
    cooldown.value -= 1;

    if (cooldown.value <= 0) {
      cooldown.value = 0;
      clearCooldown();
    }
  }, 1000);
}

async function handleSendCode() {
  errorMessage.value = "";
  debugCode.value = "";

  if (!/^\d{11}$/.test(form.phone)) {
    errorMessage.value = "请输入 11 位手机号";
    return;
  }

  sendingCode.value = true;

  try {
    const result = await sendCode({
      phone: form.phone,
      purpose: "LOGIN",
    });

    debugCode.value = result.debugCode || "";
    startCooldown();
  } catch (error) {
    errorMessage.value = error.message || "验证码发送失败，请稍后重试";
  } finally {
    sendingCode.value = false;
  }
}

async function handleSubmit() {
  errorMessage.value = "";

  if (!/^\d{11}$/.test(form.phone)) {
    errorMessage.value = "请输入 11 位手机号";
    return;
  }

  if (!/^\d{6}$/.test(form.verificationCode)) {
    errorMessage.value = "请输入 6 位验证码";
    return;
  }

  try {
    await userStore.login(form);
    router.push(route.query.redirect || "/");
  } catch (error) {
    errorMessage.value = error.message || "登录失败，请稍后重试";
  }
}

onBeforeUnmount(() => {
  clearCooldown();
});
</script>

<template>
  <section class="page-stack">
    <div class="hero-grid">
      <article class="section-card">
        <span class="section-eyebrow">Phone Code Login</span>
        <h1 class="hero-title">先进入统一入口，再决定今天优先处理哪条主线</h1>
        <hr class="editorial-rule" />
        <p class="hero-copy">
          登录后，首页会聚合展示就业、考研、留学三条方向，以及你的认证状态、未读通知和当前待办。
        </p>

        <div class="stats-grid" style="margin-top: 24px;">
          <article class="panel-card">
            <strong class="stat-value">3</strong>
            <p class="meta-copy">就业 / 考研 / 留学三条主线统一聚合</p>
          </article>
          <article class="panel-card">
            <strong class="stat-value">Phase A</strong>
            <p class="meta-copy">首页、认证申请、通知中心与审核台已接通</p>
          </article>
        </div>

        <article class="panel-card helper-card">
          <strong>测试账号</strong>
          <ul class="demo-list">
            <li v-for="account in demoAccounts" :key="account.phone">
              <span>{{ account.phone }}</span>
              <strong>{{ account.label }}</strong>
              <small>{{ account.note }}</small>
            </li>
          </ul>
        </article>
      </article>

      <article class="section-card">
        <div class="section-header">
          <div>
            <span class="section-eyebrow">登录</span>
            <h2 class="page-title" style="margin-top: 16px;">手机号验证码登录</h2>
            <p class="page-subtitle" style="margin-top: 16px;">
              当前测试环境会直接返回调试验证码，后续可无缝切换到真实短信通道。
            </p>
          </div>
        </div>

        <form class="field-grid" @submit.prevent="handleSubmit">
          <label class="field-label">
            手机号
            <input
              v-model.trim="form.phone"
              class="field-control"
              name="phone"
              type="tel"
              inputmode="numeric"
              autocomplete="tel"
              placeholder="请输入 11 位手机号"
            />
          </label>

          <label class="field-label">
            验证码
            <div class="code-row">
              <input
                v-model.trim="form.verificationCode"
                class="field-control"
                name="verificationCode"
                type="text"
                inputmode="numeric"
                autocomplete="one-time-code"
                placeholder="请输入 6 位验证码"
              />
              <button
                type="button"
                class="ghost-btn code-btn"
                :disabled="sendingCode || cooldown > 0"
                @click="handleSendCode"
              >
                {{ sendCodeLabel }}
              </button>
            </div>
          </label>

          <p class="field-hint">
            输入示例账号手机号后获取验证码即可登录，不再使用单独的用户名和密码。
          </p>
          <p v-if="debugCode" class="debug-note">本次调试验证码：{{ debugCode }}</p>
          <p v-if="errorMessage" class="field-error" role="alert">{{ errorMessage }}</p>

          <div class="inline-form-actions">
            <button type="submit" class="app-btn" :disabled="userStore.loading">
              {{ userStore.loading ? "登录中..." : "登录" }}
            </button>
            <RouterLink to="/register" class="app-link">去注册</RouterLink>
          </div>
        </form>
      </article>
    </div>
  </section>
</template>

<style scoped>
.stat-value {
  font-size: 28px;
  font-family: var(--cp-font-display);
}

.helper-card {
  margin-top: 24px;
  display: grid;
  gap: var(--cp-gap-4);
}

.demo-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: var(--cp-gap-3);
}

.demo-list li {
  display: grid;
  gap: 4px;
  padding-bottom: 12px;
  border-bottom: 1px dashed var(--cp-line);
}

.demo-list li:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.demo-list span,
.demo-list small {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.code-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
}

.code-btn {
  min-width: 132px;
}

.debug-note {
  margin: 0;
  color: var(--cp-accent-deep);
  font-size: var(--cp-text-sm);
  font-weight: 600;
}

@media (max-width: 767px) {
  .code-row {
    grid-template-columns: 1fr;
  }

  .code-btn {
    width: 100%;
  }
}
</style>
