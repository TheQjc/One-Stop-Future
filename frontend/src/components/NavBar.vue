<script setup>
import { computed } from "vue";
import { RouterLink, useRouter } from "vue-router";
import NotificationBell from "./NotificationBell.vue";
import VerificationStatusBadge from "./VerificationStatusBadge.vue";
import { useUserStore } from "../stores/user.js";

const userStore = useUserStore();
const router = useRouter();

const navItems = computed(() => {
  const items = [
    { to: "/", label: "Home" },
    { to: "/community", label: "Community" },
    { to: "/discover", label: "Discover" },
    { to: "/search", label: "Search" },
  ];

  if (userStore.isAuthenticated) {
    items.push({ to: "/profile", label: "Profile" });
    items.push({ to: "/notifications", label: "Notifications" });
  }

  if (userStore.canReviewVerifications) {
    items.push({ to: "/admin/dashboard", label: "Operations" });
    items.push({ to: "/admin/users", label: "User Desk" });
    items.push({ to: "/admin/applications", label: "Applications" });
    items.push({ to: "/admin/verifications", label: "Verifications" });
    items.push({ to: "/admin/community", label: "Community Desk" });
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
        <span class="site-brand__mark">Editorial Student Decision Desk</span>
        <strong>One-Stop Future</strong>
        <small>Career / Exam / Abroad in one working entrance</small>
      </RouterLink>

      <nav class="site-nav" aria-label="Primary">
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
        <div v-if="userStore.isAuthenticated" class="site-user">
          <div class="site-user__topline">
            <NotificationBell :count="userStore.unreadCount" />
            <VerificationStatusBadge :status="userStore.profile?.verificationStatus" />
          </div>
          <span class="site-user__role">{{ userStore.roleLabel }}</span>
          <span class="site-user__name">
            {{ userStore.profile?.nickname || userStore.profile?.phone }}
          </span>
        </div>

        <RouterLink v-if="!userStore.isAuthenticated" to="/login" class="app-link">
          Log In
        </RouterLink>
        <RouterLink v-if="!userStore.isAuthenticated" to="/register" class="ghost-btn">
          Register
        </RouterLink>
        <button v-else type="button" class="ghost-btn" @click="handleLogout">
          Log Out
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
  gap: var(--cp-gap-3);
  align-items: center;
  justify-content: end;
}

.site-user {
  display: grid;
  gap: 6px;
  justify-items: end;
}

.site-user__topline {
  display: flex;
  gap: 10px;
  align-items: center;
}

.site-user__role {
  font-size: var(--cp-text-sm);
  color: var(--cp-teal-deep);
}

.site-user__name {
  font-weight: 600;
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
    justify-items: start;
  }
}

@media (max-width: 767px) {
  .site-header__inner {
    width: min(calc(100% - 20px), var(--cp-max-width));
  }

  .site-nav__link {
    padding: 0 12px;
  }

  .site-user__topline {
    flex-wrap: wrap;
  }
}
</style>
