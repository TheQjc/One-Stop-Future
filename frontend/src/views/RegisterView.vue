<script setup>
import { computed, onBeforeUnmount, reactive, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import { sendCode } from "../api/auth.js";
import { useUserStore } from "../stores/user.js";

const router = useRouter();
const userStore = useUserStore();

const form = reactive({
  phone: "",
  verificationCode: "",
  nickname: "",
});

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
      purpose: "REGISTER",
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

  if (!form.nickname.trim()) {
    errorMessage.value = "请输入昵称";
    return;
  }

  try {
    await userStore.register(form);
    router.push("/");
  } catch (error) {
    errorMessage.value = error.message || "注册失败，请稍后重试";
  }
}

onBeforeUnmount(() => {
  clearCooldown();
});
</script>

<template>
  <section class="page-stack">
    <div class="two-col-grid">
      <article class="section-card">
        <span class="section-eyebrow">New Account</span>
        <h1 class="page-title" style="margin-top: 18px;">先完成注册，再回到首页看清你的下一步</h1>
        <p class="page-subtitle" style="margin-top: 16px;">
          Phase A 默认开放普通用户自主注册。注册成功后会直接建立登录态，并把你带回独立首页聚合视图。
        </p>

        <div class="info-card-list" style="margin-top: 24px;">
          <article class="panel-card">
            <strong>手机号即账号</strong>
            <p class="meta-copy">
              登录与注册统一使用手机号验证码，不再维护独立的用户名和密码体系。
            </p>
          </article>
          <article class="panel-card">
            <strong>昵称先行，认证后补全</strong>
            <p class="meta-copy">
              注册时先填写公开昵称，真实姓名和更多资料可以在个人中心继续补充。
            </p>
          </article>
          <article class="panel-card">
            <strong>审核结果会回到通知中心</strong>
            <p class="meta-copy">
              提交认证申请后，教师与管理员统一审核，结果会在通知中心集中展示。
            </p>
          </article>
        </div>
      </article>

      <article class="section-card">
        <div class="section-header">
          <div>
            <span class="section-eyebrow">注册</span>
            <h2 class="page-title" style="margin-top: 16px;">手机号验证码注册</h2>
            <p class="page-subtitle" style="margin-top: 16px;">
              当前测试环境会直接显示调试验证码，便于快速验证注册流程。
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

          <label class="field-label">
            昵称
            <input
              v-model.trim="form.nickname"
              class="field-control"
              name="nickname"
              type="text"
              autocomplete="nickname"
              placeholder="请输入你的展示昵称"
            />
          </label>

          <p class="field-hint">
            后续可以在个人中心继续补充实名、学号、联系方式和认证材料。
          </p>
          <p v-if="debugCode" class="debug-note">本次调试验证码：{{ debugCode }}</p>
          <p v-if="errorMessage" class="field-error" role="alert">{{ errorMessage }}</p>

          <div class="inline-form-actions">
            <button type="submit" class="app-btn" :disabled="userStore.loading">
              {{ userStore.loading ? "注册中..." : "完成注册" }}
            </button>
            <RouterLink to="/login" class="ghost-btn">返回登录</RouterLink>
          </div>
        </form>
      </article>
    </div>
  </section>
</template>

<style scoped>
.info-card-list {
  display: grid;
  gap: var(--cp-gap-4);
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
