<script setup>
import { computed } from "vue";
import { RouterLink, useRouter } from "vue-router";
import NotificationBell from "./NotificationBell.vue";
import VerificationStatusBadge from "./VerificationStatusBadge.vue";
import { useUserStore } from "../stores/user.js";

const userStore = useUserStore();
const router = useRouter();
const userDisplayName = computed(() => userStore.profile?.nickname || userStore.profile?.phone || "我的");
const userInitial = computed(() => userDisplayName.value.trim().slice(0, 1).toUpperCase() || "我");

const navItems = computed(() => {
  const items = [
    { to: "/", label: "首页" },
    { to: "/community", label: "社区" },
    { to: "/discover", label: "趋势" },
    { to: "/search", label: "搜索" },
  ];

  if (userStore.isAuthenticated) {
    items.push({ to: "/notifications", label: "通知" });
  }

  if (userStore.canReviewVerifications) {
    items.push({ to: "/admin/dashboard", label: "运营总览" });
    items.push({ to: "/admin/users", label: "用户管理" });
    items.push({ to: "/admin/applications", label: "申请管理" });
    items.push({ to: "/admin/verifications", label: "认证审核" });
    items.push({ to: "/admin/community", label: "社区管理" });
  }

  return items;
});

async function handleLogout() {
  await userStore.logout();
  router.push("/login");
}
</script>

<template>
  <header class="site-header">
    <div class="site-header__inner">
      <RouterLink class="site-brand" to="/">
        <span class="site-brand__mark">学生成长服务平台</span>
        <strong>一站式成长平台</strong>
        <small>One-Stop Future</small>
      </RouterLink>

      <nav class="site-nav" aria-label="主导航">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="site-nav__link"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="site-header__actions">
        <template v-if="userStore.isAuthenticated">
          <NotificationBell :count="userStore.unreadCount" />
          <VerificationStatusBadge :status="userStore.profile?.verificationStatus" />
          <RouterLink class="site-user" to="/profile" aria-label="进入个人主页">
            <span class="site-user__avatar" aria-hidden="true">{{ userInitial }}</span>
            <span class="site-user__meta">
              <span class="site-user__role">我的</span>
              <span class="site-user__name">{{ userDisplayName }}</span>
            </span>
          </RouterLink>
        </template>

        <template v-if="!userStore.isAuthenticated">
          <RouterLink to="/login" class="app-link">
            登录
          </RouterLink>
          <RouterLink to="/register" class="ghost-btn">
            注册
          </RouterLink>
        </template>
        <button v-else type="button" class="ghost-btn" @click="handleLogout">
          退出登录
        </button>
      </div>
    </div>
  </header>
</template>

<style scoped>
.site-header {
  position: sticky;
  top: 0;
  z-index: 30;
  backdrop-filter: blur(18px);
  background: rgba(250, 245, 236, 0.82);
  border-bottom: 1px solid var(--cp-line);
}

.site-header__inner {
  width: min(calc(100% - 32px), var(--cp-max-width));
  min-height: 92px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: var(--cp-gap-4);
  align-items: center;
}

.site-brand {
  display: grid;
  gap: 4px;
}

.site-brand__mark {
  color: var(--cp-accent-deep);
  font-size: 11px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.site-brand strong {
  font-size: clamp(24px, 3vw, 30px);
  font-family: var(--cp-font-display);
}

.site-brand small {
  color: var(--cp-ink-soft);
  font-size: var(--cp-text-sm);
}

.site-nav {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: var(--cp-gap-2);
}

.site-nav__link {
  min-height: var(--cp-touch-height);
  padding: 0 16px;
  display: inline-flex;
  align-items: center;
  border-radius: var(--cp-radius-pill);
  color: var(--cp-ink-soft);
  font-weight: 600;
  transition:
    background-color var(--cp-transition),
    color var(--cp-transition),
    transform var(--cp-transition);
}

.site-nav__link:hover {
  transform: translateY(-1px);
}

.site-nav__link.router-link-exact-active {
  color: var(--cp-ink);
  background: rgba(24, 38, 63, 0.08);
}

.site-header__actions {
  display: flex;
  gap: var(--cp-gap-2);
  align-items: center;
  justify-content: end;
  min-width: 0;
}

.site-user {
  min-height: var(--cp-touch-height);
  display: inline-flex;
  gap: 10px;
  align-items: center;
  padding: 6px 12px 6px 8px;
  border: 1px solid rgba(24, 38, 63, 0.12);
  border-radius: var(--cp-radius-pill);
  background: rgba(255, 255, 255, 0.62);
  transition:
    background-color var(--cp-transition),
    border-color var(--cp-transition),
    box-shadow var(--cp-transition),
    transform var(--cp-transition);
  min-width: 0;
}

.site-user:hover {
  transform: translateY(-1px);
  border-color: rgba(24, 38, 63, 0.24);
  background: rgba(255, 255, 255, 0.88);
  box-shadow: var(--cp-shadow-soft);
}

.site-user.router-link-exact-active {
  border-color: rgba(197, 79, 45, 0.28);
  background: rgba(197, 79, 45, 0.08);
}

.site-user__avatar {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  border-radius: 50%;
  background: var(--cp-ink);
  color: #fff9f1;
  font-weight: 700;
}

.site-user__meta {
  display: grid;
  gap: 1px;
  min-width: 0;
}

.site-user__role {
  font-size: var(--cp-text-xs);
  color: var(--cp-teal-deep);
  line-height: 1.2;
}

.site-user__name {
  font-weight: 600;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.2;
}

.site-header__actions .ghost-btn {
  flex: 0 0 auto;
  white-space: nowrap;
}

@media (max-width: 1023px) {
  .site-header__inner {
    grid-template-columns: 1fr;
    padding: 12px 0 16px;
  }

  .site-nav,
  .site-header__actions {
    justify-content: start;
  }

  .site-user {
    max-width: 100%;
  }
}

@media (max-width: 767px) {
  .site-header__inner {
    width: min(calc(100% - 20px), var(--cp-max-width));
  }

  .site-nav__link {
    padding: 0 12px;
  }

  .site-user__name {
    max-width: 96px;
  }
}
</style>
