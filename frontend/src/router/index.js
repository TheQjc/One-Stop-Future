import { createRouter, createWebHistory } from "vue-router";
import HomeView from "../views/HomeView.vue";
import LoginView from "../views/LoginView.vue";
import RegisterView from "../views/RegisterView.vue";
import ProfileView from "../views/ProfileView.vue";
import NotificationCenterView from "../views/NotificationCenterView.vue";
import AdminVerificationReviewView from "../views/admin/AdminVerificationReviewView.vue";
import pinia from "../stores/pinia.js";
import { useUserStore } from "../stores/user.js";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", name: "home", component: HomeView },
    { path: "/login", name: "login", component: LoginView, meta: { guestOnly: true } },
    { path: "/register", name: "register", component: RegisterView, meta: { guestOnly: true } },
    { path: "/profile", name: "profile", component: ProfileView, meta: { requiresAuth: true } },
    {
      path: "/notifications",
      name: "notifications",
      component: NotificationCenterView,
      meta: { requiresAuth: true },
    },
    {
      path: "/admin/verifications",
      name: "admin-verifications",
      component: AdminVerificationReviewView,
      meta: { requiresAuth: true, roles: ["ADMIN"] },
    },
  ],
  scrollBehavior() {
    return { top: 0 };
  },
});

router.beforeEach(async (to) => {
  const userStore = useUserStore(pinia);

  if (userStore.isAuthenticated && !userStore.profile) {
    try {
      await userStore.fetchProfile();
    } catch (error) {
      userStore.clearAuth();
    }
  }

  if (to.meta.requiresAuth && !userStore.isAuthenticated) {
    return { name: "login", query: { redirect: to.fullPath } };
  }

  if (to.meta.guestOnly && userStore.isAuthenticated) {
    return { name: "home" };
  }

  if (to.meta.roles && !to.meta.roles.includes(userStore.role)) {
    return { name: "home" };
  }

  return true;
});

export default router;
