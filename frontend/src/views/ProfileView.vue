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

const verificationStatus = computed(() => userStore.profile?.verificationStatus || "UNVERIFIED");

const verificationLocked = computed(() => (
  verificationStatus.value === "PENDING" || verificationStatus.value === "VERIFIED"
));

const verificationHint = computed(() => {
  if (verificationStatus.value === "VERIFIED") {
    return "学生认证已完成，无需重复提交。";
  }

  if (verificationStatus.value === "PENDING") {
    return "你的认证申请正在审核中，结果会通过通知中心告知。";
  }

  return "提交真实姓名和学号后即可进入审核队列。";
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
    pageError.value = error.message || "个人中心加载失败，请稍后重试。";
  } finally {
    loading.value = false;
  }
}

async function submitProfile() {
  profileMessage.value = "";
  profileError.value = "";

  if (!profileForm.nickname.trim()) {
    profileError.value = "请填写昵称。";
    return;
  }

  try {
    await userStore.saveProfile({
      nickname: profileForm.nickname.trim(),
      realName: profileForm.realName.trim(),
    });

    syncForms();
    profileMessage.value = "资料已更新。";
  } catch (error) {
    profileError.value = error.message || "保存资料失败，请稍后重试。";
  }
}

async function handleSubmitVerification() {
  verificationMessage.value = "";
  verificationError.value = "";

  if (!verificationForm.realName.trim()) {
    verificationError.value = "请填写真实姓名。";
    return;
  }

  if (!verificationForm.studentId.trim()) {
    verificationError.value = "请填写学号。";
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
    verificationMessage.value = "认证申请已提交。";
  } catch (error) {
    verificationError.value = error.message || "认证提交失败，请稍后重试。";
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
          <span class="section-eyebrow">个人成长工作台</span>
          <h1 class="page-title" style="margin-top: 16px;">个人中心</h1>
          <p class="page-subtitle" style="margin-top: 16px;">
            在这里管理身份信息、查看个人记录，并继续处理学生认证相关事项。
          </p>
        </div>
        <RouterLink to="/notifications" class="app-link">
          查看通知
        </RouterLink>
      </div>

      <div v-if="loading" class="empty-state">正在准备你的个人中心...</div>
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
          <p class="identity-card__label">身份角色</p>
          <strong>{{ userStore.roleLabel }}</strong>
        </article>
        <article class="panel-card identity-card">
          <p class="identity-card__label">认证状态</p>
          <VerificationStatusBadge :status="verificationStatus" />
        </article>
        <article class="panel-card identity-card">
          <p class="identity-card__label">学号</p>
          <strong>{{ userStore.profile?.studentId || "暂未提交" }}</strong>
        </article>
      </div>
    </article>

    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">常用入口</span>
          <h2 class="page-title" style="margin-top: 16px;">回到常用功能</h2>
          <p class="page-subtitle" style="margin-top: 16px;">
            发帖、收藏、资料上传与申请记录都可以从这里直接进入。
          </p>
        </div>
      </div>

      <div class="quick-link-grid">
        <RouterLink to="/profile/posts" class="panel-card profile-link-card">
          <span class="profile-link-card__eyebrow">我的帖子</span>
          <strong>已发布内容</strong>
          <p class="meta-copy">查看你已经发布的社区内容和互动情况。</p>
        </RouterLink>

        <RouterLink to="/profile/favorites" class="panel-card profile-link-card">
          <span class="profile-link-card__eyebrow">我的收藏</span>
          <strong>收藏内容</strong>
          <p class="meta-copy">把帖子、岗位和资料集中保留，方便下次继续查看。</p>
        </RouterLink>

        <RouterLink to="/profile/resources" class="panel-card profile-link-card">
          <span class="profile-link-card__eyebrow">我的资料</span>
          <strong>上传记录</strong>
          <p class="meta-copy">跟进已上传文件的状态变化、处理结果和驳回说明。</p>
        </RouterLink>

        <RouterLink to="/profile/resumes" class="panel-card profile-link-card">
          <span class="profile-link-card__eyebrow">我的简历</span>
          <strong>简历库</strong>
          <p class="meta-copy">为不同申请场景准备多份简历版本，随时调用。</p>
        </RouterLink>

        <RouterLink to="/profile/applications" class="panel-card profile-link-card">
          <span class="profile-link-card__eyebrow">我的申请</span>
          <strong>申请记录</strong>
          <p class="meta-copy">查看你投递过哪些岗位，以及使用了哪份简历。</p>
        </RouterLink>

        <RouterLink to="/community/create" class="panel-card profile-link-card">
          <span class="profile-link-card__eyebrow">立即发帖</span>
          <strong>发布社区内容</strong>
          <p class="meta-copy">不离开个人中心，也能直接发起新的讨论。</p>
        </RouterLink>
      </div>
    </article>

    <div class="two-col-grid">
      <article class="section-card">
        <div class="section-header">
          <div>
            <span class="section-eyebrow">基本资料</span>
            <h2 class="page-title" style="margin-top: 16px;">个人信息</h2>
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
              placeholder="输入你的显示昵称"
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
              placeholder="认证前可暂不填写"
            />
          </label>

          <p class="field-hint">
            当前仅支持编辑系统已接入的基础身份信息。
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
            <span class="section-eyebrow">学生认证</span>
            <h2 class="page-title" style="margin-top: 16px;">认证申请</h2>
          </div>
        </div>

        <div class="verification-panel">
          <article class="panel-card">
            <strong>审核说明</strong>
            <p class="meta-copy" style="margin-top: 12px;">
              提交后将由老师或管理员进入审核流程。
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
              placeholder="输入你的真实姓名"
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
              placeholder="输入你的学号"
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
                  ? (verificationStatus === "VERIFIED" ? "已认证" : "审核中")
                  : (verificationSubmitting ? "提交中..." : "提交认证")
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

.profile-link-card {
  min-height: 170px;
  display: grid;
  gap: 10px;
  align-content: end;
}

.profile-link-card__eyebrow {
  color: var(--cp-accent-deep);
  font-size: 11px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
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
