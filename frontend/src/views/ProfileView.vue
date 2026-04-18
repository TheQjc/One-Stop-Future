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
    return "Student verification is complete. No further action is required.";
  }

  if (verificationStatus.value === "PENDING") {
    return "Your verification request is under review. Results will return through notifications.";
  }

  return "Submit your real name and student ID to enter the review queue.";
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
    pageError.value = error.message || "Profile loading failed. Please try again.";
  } finally {
    loading.value = false;
  }
}

async function submitProfile() {
  profileMessage.value = "";
  profileError.value = "";

  if (!profileForm.nickname.trim()) {
    profileError.value = "Nickname is required.";
    return;
  }

  try {
    await userStore.saveProfile({
      nickname: profileForm.nickname.trim(),
      realName: profileForm.realName.trim(),
    });

    syncForms();
    profileMessage.value = "Profile updated.";
  } catch (error) {
    profileError.value = error.message || "Profile save failed. Please try again.";
  }
}

async function handleSubmitVerification() {
  verificationMessage.value = "";
  verificationError.value = "";

  if (!verificationForm.realName.trim()) {
    verificationError.value = "Real name is required.";
    return;
  }

  if (!verificationForm.studentId.trim()) {
    verificationError.value = "Student ID is required.";
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
    verificationMessage.value = "Verification request submitted.";
  } catch (error) {
    verificationError.value = error.message || "Verification submission failed. Please try again.";
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
          <h1 class="page-title" style="margin-top: 16px;">Personal Center</h1>
          <p class="page-subtitle" style="margin-top: 16px;">
            Manage identity details, review your saved records, and keep the student-verification
            flow on one working surface.
          </p>
        </div>
        <RouterLink to="/notifications" class="app-link">
          Open Notifications
        </RouterLink>
      </div>

      <div v-if="loading" class="empty-state">Preparing your profile desk...</div>
      <div v-else-if="pageError" class="field-grid">
        <p class="field-error" role="alert">{{ pageError }}</p>
        <button type="button" class="ghost-btn" @click="initialize">
          Retry
        </button>
      </div>
      <div v-else class="identity-grid">
        <article class="panel-card identity-card">
          <p class="identity-card__label">Phone</p>
          <strong>{{ userStore.profile?.phone || "--" }}</strong>
        </article>
        <article class="panel-card identity-card">
          <p class="identity-card__label">Role</p>
          <strong>{{ userStore.roleLabel }}</strong>
        </article>
        <article class="panel-card identity-card">
          <p class="identity-card__label">Verification</p>
          <VerificationStatusBadge :status="verificationStatus" />
        </article>
        <article class="panel-card identity-card">
          <p class="identity-card__label">Student ID</p>
          <strong>{{ userStore.profile?.studentId || "Not submitted" }}</strong>
        </article>
      </div>
    </article>

    <article class="section-card">
      <div class="section-header">
        <div>
          <span class="section-eyebrow">Workspace Links</span>
          <h2 class="page-title" style="margin-top: 16px;">Return surfaces</h2>
          <p class="page-subtitle" style="margin-top: 16px;">
            Keep posting, collecting, and uploading reachable without jumping back through list
            pages.
          </p>
        </div>
      </div>

      <div class="quick-link-grid">
        <RouterLink to="/profile/posts" class="panel-card profile-link-card">
          <span class="profile-link-card__eyebrow">My Posts</span>
          <strong>Published Posts</strong>
          <p class="meta-copy">Review community posts you have already published.</p>
        </RouterLink>

        <RouterLink to="/profile/favorites" class="panel-card profile-link-card">
          <span class="profile-link-card__eyebrow">My Favorites</span>
          <strong>Saved Board</strong>
          <p class="meta-copy">Keep posts, jobs, and resources ready for the next revisit.</p>
        </RouterLink>

        <RouterLink to="/profile/resources" class="panel-card profile-link-card">
          <span class="profile-link-card__eyebrow">My Resources</span>
          <strong>Resource Records</strong>
          <p class="meta-copy">Track uploaded files, status changes, and rejection notes.</p>
        </RouterLink>

        <RouterLink to="/profile/resumes" class="panel-card profile-link-card">
          <span class="profile-link-card__eyebrow">My Resumes</span>
          <strong>Resume Library</strong>
          <p class="meta-copy">Keep multiple resume files ready for application.</p>
        </RouterLink>

        <RouterLink to="/profile/applications" class="panel-card profile-link-card">
          <span class="profile-link-card__eyebrow">My Applications</span>
          <strong>Application Records</strong>
          <p class="meta-copy">Review where you have already applied and which resume was used.</p>
        </RouterLink>

        <RouterLink to="/community/create" class="panel-card profile-link-card">
          <span class="profile-link-card__eyebrow">Write</span>
          <strong>New Community Post</strong>
          <p class="meta-copy">Jump straight into a new discussion without leaving the desk.</p>
        </RouterLink>
      </div>
    </article>

    <div class="two-col-grid">
      <article class="section-card">
        <div class="section-header">
          <div>
            <span class="section-eyebrow">Basic Info</span>
            <h2 class="page-title" style="margin-top: 16px;">Profile details</h2>
          </div>
        </div>

        <form class="field-grid" @submit.prevent="submitProfile">
          <label class="field-label">
            Nickname
            <input
              v-model.trim="profileForm.nickname"
              class="field-control"
              name="nickname"
              type="text"
              autocomplete="nickname"
              placeholder="Enter a display name"
            />
          </label>

          <label class="field-label">
            Real Name
            <input
              v-model.trim="profileForm.realName"
              class="field-control"
              name="realName"
              type="text"
              autocomplete="name"
              placeholder="Optional before verification"
            />
          </label>

          <p class="field-hint">
            Keep this section limited to the identity fields already supported by the current
            backend slice.
          </p>
          <p v-if="profileMessage" class="field-hint">{{ profileMessage }}</p>
          <p v-if="profileError" class="field-error" role="alert">{{ profileError }}</p>

          <div class="inline-form-actions">
            <button type="submit" class="app-btn">
              Save Profile
            </button>
          </div>
        </form>
      </article>

      <article class="section-card">
        <div class="section-header">
          <div>
            <span class="section-eyebrow">Student Verification</span>
            <h2 class="page-title" style="margin-top: 16px;">Verification queue</h2>
          </div>
        </div>

        <div class="verification-panel">
          <article class="panel-card">
            <strong>Review Notice</strong>
            <p class="meta-copy" style="margin-top: 12px;">
              Teacher or admin reviewers handle the request after submission.
            </p>
          </article>

          <article class="panel-card">
            <strong>Current State</strong>
            <p class="meta-copy" style="margin-top: 12px;">
              {{ verificationHint }}
            </p>
          </article>
        </div>

        <form class="field-grid" style="margin-top: 24px;" @submit.prevent="handleSubmitVerification">
          <label class="field-label">
            Real Name
            <input
              v-model.trim="verificationForm.realName"
              class="field-control"
              name="verificationRealName"
              type="text"
              autocomplete="name"
              placeholder="Enter your real name"
              :disabled="verificationLocked"
            />
          </label>

          <label class="field-label">
            Student ID
            <input
              v-model.trim="verificationForm.studentId"
              class="field-control"
              name="studentId"
              type="text"
              placeholder="Enter your student ID"
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
                  ? (verificationStatus === "VERIFIED" ? "Verified" : "Under Review")
                  : (verificationSubmitting ? "Submitting..." : "Submit Verification")
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
