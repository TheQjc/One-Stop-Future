<script setup>
import { onMounted, reactive, ref } from "vue";
import { useUserStore } from "../stores/user.js";

const userStore = useUserStore();
const profileForm = reactive({
  realName: "",
  email: "",
  bio: "",
});
const passwordForm = reactive({
  oldPassword: "",
  newPassword: "",
});
const profileMessage = ref("");
const passwordMessage = ref("");
const profileError = ref("");
const passwordError = ref("");

onMounted(async () => {
  if (!userStore.profile) {
    await userStore.fetchProfile();
  }

  profileForm.realName = userStore.profile?.realName || "";
  profileForm.email = userStore.profile?.email || "";
  profileForm.bio = userStore.profile?.bio || "";
});

async function submitProfile() {
  profileMessage.value = "";
  profileError.value = "";

  try {
    await userStore.saveProfile(profileForm);
    profileMessage.value = "个人信息已更新。";
  } catch (error) {
    profileError.value = error.message || "保存失败，请稍后重试。";
  }
}

async function submitPassword() {
  passwordMessage.value = "";
  passwordError.value = "";

  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    passwordError.value = "请填写旧密码和新密码。";
    return;
  }

  try {
    await userStore.updatePassword(passwordForm);
    passwordMessage.value = "密码已更新。";
    passwordForm.oldPassword = "";
    passwordForm.newPassword = "";
  } catch (error) {
    passwordError.value = error.message || "密码修改失败。";
  }
}
</script>

<template>
  <section class="page-stack">
    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">个人中心</span>
          <h1 class="page-title" style="margin-top: 16px;">个人信息</h1>
          <p class="page-subtitle" style="margin-top: 16px;">
            管理个人资料与密码。当前登录角色为
            <strong>{{ userStore.roleLabel }}</strong>
            ，用户名
            <strong>{{ userStore.profile?.username || "--" }}</strong>
            。
          </p>
        </div>
      </div>
    </article>

    <div class="two-col-grid">
      <article class="section-card">
        <h2 class="page-title" style="font-size: 28px;">资料维护</h2>
        <form class="field-grid" style="margin-top: 24px;" @submit.prevent="submitProfile">
          <label class="field-label">
            姓名
            <input v-model.trim="profileForm.realName" class="field-control" type="text" />
          </label>
          <label class="field-label">
            邮箱
            <input v-model.trim="profileForm.email" class="field-control" type="email" />
          </label>
          <label class="field-label">
            个人简介
            <textarea v-model.trim="profileForm.bio" class="field-textarea" placeholder="可填写职能方向、兴趣或联系方式" />
          </label>

          <p v-if="profileMessage" class="field-hint">{{ profileMessage }}</p>
          <p v-if="profileError" class="field-error" role="alert">{{ profileError }}</p>

          <div class="inline-form-actions">
            <button type="submit" class="app-btn">保存资料</button>
          </div>
        </form>
      </article>

      <article class="section-card">
        <h2 class="page-title" style="font-size: 28px;">密码更新</h2>
        <form class="field-grid" style="margin-top: 24px;" @submit.prevent="submitPassword">
          <label class="field-label">
            旧密码
            <input
              v-model.trim="passwordForm.oldPassword"
              class="field-control"
              type="password"
              placeholder="请输入当前密码"
            />
          </label>
          <label class="field-label">
            新密码
            <input
              v-model.trim="passwordForm.newPassword"
              class="field-control"
              type="password"
              placeholder="请输入新的登录密码"
            />
          </label>
          <p class="field-hint">建议使用与校园其他系统不同的密码组合，避免弱口令。</p>
          <p v-if="passwordMessage" class="field-hint">{{ passwordMessage }}</p>
          <p v-if="passwordError" class="field-error" role="alert">{{ passwordError }}</p>

          <div class="inline-form-actions">
            <button type="submit" class="app-btn">更新密码</button>
          </div>
        </form>
      </article>
    </div>
  </section>
</template>
