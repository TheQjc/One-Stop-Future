<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { RouterLink } from "vue-router";
import VerificationStatusBadge from "../components/VerificationStatusBadge.vue";
import { submitVerification } from "../api/verification.js";
import { useUserStore } from "../stores/user.js";

const userStore = useUserStore();

const loading = ref(true);
const pageError = ref("");
const profileMessage = ref("");
const verificationMessage = ref("");
const profileError = ref("");
const verificationError = ref("");
const verificationSubmitting = ref(false);

const profileForm = reactive({
  nickname: "",
  realName: "",
});

const verificationForm = reactive({
  realName: "",
  studentId: "",
});

const verificationStatus = computed(() => (
  userStore.profile?.verificationStatus || "UNVERIFIED"
));

const verificationLocked = computed(() => (
  verificationStatus.value === "PENDING" || verificationStatus.value === "VERIFIED"
));

const verificationHint = computed(() => {
  if (verificationStatus.value === "VERIFIED") {
    return "当前已完成学生身份认证，无需重复提交。";
  }

  if (verificationStatus.value === "PENDING") {
    return "认证申请已提交，教师或管理员审核后会通过通知中心反馈结果。";
  }

  return "请填写真实姓名和学号提交学生认证，审核通过后将解锁后续能力。";
});

function syncForms() {
  profileForm.nickname = userStore.profile?.nickname || "";
  profileForm.realName = userStore.profile?.realName || "";
  verificationForm.realName = userStore.profile?.realName || "";
  verificationForm.studentId = userStore.profile?.studentId || "";
}

async function initialize() {
  loading.value = true;
  pageError.value = "";

  try {
    if (!userStore.profile) {
      await userStore.fetchProfile();
    }

    syncForms();
  } catch (error) {
    pageError.value = error.message || "个人资料加载失败，请稍后重试。";
  } finally {
    loading.value = false;
  }
}

async function submitProfile() {
  profileMessage.value = "";
  profileError.value = "";

  if (!profileForm.nickname.trim()) {
    profileError.value = "请输入昵称";
    return;
  }

  try {
    await userStore.saveProfile({
      nickname: profileForm.nickname.trim(),
      realName: profileForm.realName.trim(),
    });

    syncForms();
    profileMessage.value = "个人资料已更新。";
  } catch (error) {
    profileError.value = error.message || "资料保存失败，请稍后重试。";
  }
}

async function handleSubmitVerification() {
  verificationMessage.value = "";
  verificationError.value = "";

  if (!verificationForm.realName.trim()) {
    verificationError.value = "请输入真实姓名";
    return;
  }

  if (!verificationForm.studentId.trim()) {
    verificationError.value = "请输入学号";
    return;
  }

  verificationSubmitting.value = true;

  try {
    const profile = await submitVerification({
      realName: verificationForm.realName.trim(),
      studentId: verificationForm.studentId.trim(),
    });

    userStore.mergeProfile(profile);
    syncForms();
    verificationMessage.value = "认证申请已提交，请等待教师或管理员审核。";
  } catch (error) {
    verificationError.value = error.message || "认证申请提交失败，请稍后重试。";
  } finally {
    verificationSubmitting.value = false;
  }
}

onMounted(initialize);
</script>

<template>
  <section class="page-stack">
    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">Profile Desk</span>
          <h1 class="page-title" style="margin-top: 16px;">个人中心</h1>
          <p class="page-subtitle" style="margin-top: 16px;">
            在这里维护基础资料、查看认证状态，并把学生认证申请送入统一审核流程。
          </p>
        </div>
        <RouterLink to="/notifications" class="app-link">
          查看通知中心
        </RouterLink>
      </div>

      <div v-if="loading" class="empty-state">正在整理个人资料...</div>
      <div v-else-if="pageError" class="field-grid">
        <p class="field-error" role="alert">{{ pageError }}</p>
        <button type="button" class="ghost-btn" @click="initialize">
          重新加载
        </button>
      </div>
      <div v-else class="identity-grid">
        <article class="panel-card identity-card">
          <p class="identity-card__label">手机号</p>
          <strong>{{ userStore.profile?.phone || "--" }}</strong>
        </article>
        <article class="panel-card identity-card">
          <p class="identity-card__label">当前角色</p>
          <strong>{{ userStore.roleLabel }}</strong>
        </article>
        <article class="panel-card identity-card">
          <p class="identity-card__label">认证状态</p>
          <VerificationStatusBadge :status="verificationStatus" />
        </article>
        <article class="panel-card identity-card">
          <p class="identity-card__label">学号</p>
          <strong>{{ userStore.profile?.studentId || "待补充" }}</strong>
        </article>
      </div>
    </article>

    <div class="two-col-grid">
      <article class="section-card">
        <div class="section-header">
          <div>
            <span class="section-eyebrow">Basic Info</span>
            <h2 class="page-title" style="margin-top: 16px;">资料维护</h2>
          </div>
        </div>

        <form class="field-grid" @submit.prevent="submitProfile">
          <label class="field-label">
            昵称
            <input
              v-model.trim="profileForm.nickname"
              class="field-control"
              name="nickname"
              type="text"
              autocomplete="nickname"
              placeholder="请输入你的展示昵称"
            />
          </label>

          <label class="field-label">
            真实姓名
            <input
              v-model.trim="profileForm.realName"
              class="field-control"
              name="realName"
              type="text"
              autocomplete="name"
              placeholder="如已知可先补充真实姓名"
            />
          </label>

          <p class="field-hint">
            Phase A 当前仅开放昵称和真实姓名维护，后续会把更多校园信息逐步汇总到这里。
          </p>
          <p v-if="profileMessage" class="field-hint">{{ profileMessage }}</p>
          <p v-if="profileError" class="field-error" role="alert">{{ profileError }}</p>

          <div class="inline-form-actions">
            <button type="submit" class="app-btn">
              保存资料
            </button>
          </div>
        </form>
      </article>

      <article class="section-card">
        <div class="section-header">
          <div>
            <span class="section-eyebrow">Student Verification</span>
            <h2 class="page-title" style="margin-top: 16px;">学生认证</h2>
          </div>
        </div>

        <div class="verification-panel">
          <article class="panel-card">
            <strong>审核说明</strong>
            <p class="meta-copy" style="margin-top: 12px;">
              认证申请提交后，将由教师或管理员统一审核，结果会通过通知中心反馈。
            </p>
          </article>

          <article class="panel-card">
            <strong>当前状态</strong>
            <p class="meta-copy" style="margin-top: 12px;">
              {{ verificationHint }}
            </p>
          </article>
        </div>

        <form class="field-grid" style="margin-top: 24px;" @submit.prevent="handleSubmitVerification">
          <label class="field-label">
            真实姓名
            <input
              v-model.trim="verificationForm.realName"
              class="field-control"
              name="verificationRealName"
              type="text"
              autocomplete="name"
              placeholder="请输入真实姓名"
              :disabled="verificationLocked"
            />
          </label>

          <label class="field-label">
            学号
            <input
              v-model.trim="verificationForm.studentId"
              class="field-control"
              name="studentId"
              type="text"
              placeholder="请输入学号"
              :disabled="verificationLocked"
            />
          </label>

          <p v-if="verificationMessage" class="field-hint">{{ verificationMessage }}</p>
          <p v-if="verificationError" class="field-error" role="alert">{{ verificationError }}</p>

          <div class="inline-form-actions">
            <button
              type="submit"
              class="app-btn"
              :disabled="verificationLocked || verificationSubmitting"
            >
              {{
                verificationLocked
                  ? (verificationStatus === "VERIFIED" ? "已完成认证" : "审核中")
                  : (verificationSubmitting ? "提交中..." : "提交认证申请")
              }}
            </button>
          </div>
        </form>
      </article>
    </div>
  </section>
</template>

<style scoped>
.identity-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cp-gap-4);
}

.identity-card {
  min-height: 132px;
  display: grid;
  gap: var(--cp-gap-2);
  align-content: end;
}

.identity-card__label {
  margin: 0;
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.identity-card strong {
  font-size: 24px;
  font-family: var(--cp-font-display);
  line-height: 1.16;
}

.verification-panel {
  display: grid;
  gap: var(--cp-gap-4);
}

@media (max-width: 1023px) {
  .identity-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .identity-grid {
    grid-template-columns: 1fr;
  }
}
</style>
