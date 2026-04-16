import { createRouter, createWebHistory } from "vue-router";
import pinia from "../stores/pinia.js";
import { useUserStore } from "../stores/user.js";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", name: "home", component: () => import("../views/HomeView.vue") },
    {
      path: "/jobs",
      name: "jobs",
      component: () => import("../views/JobsListView.vue"),
    },
    {
      path: "/jobs/:id",
      name: "job-detail",
      component: () => import("../views/JobDetailView.vue"),
    },
    {
      path: "/community",
      name: "community",
      component: () => import("../views/CommunityListView.vue"),
    },
    {
      path: "/community/create",
      name: "community-create",
      component: () => import("../views/CommunityCreateView.vue"),
      meta: { requiresAuth: true },
    },
    {
      path: "/community/:id",
      name: "community-detail",
      component: () => import("../views/CommunityDetailView.vue"),
    },
    {
      path: "/login",
      name: "login",
      component: () => import("../views/LoginView.vue"),
      meta: { guestOnly: true },
    },
    {
      path: "/register",
      name: "register",
      component: () => import("../views/RegisterView.vue"),
      meta: { guestOnly: true },
    },
    {
      path: "/profile",
      name: "profile",
      component: () => import("../views/ProfileView.vue"),
      meta: { requiresAuth: true },
    },
    {
      path: "/profile/posts",
      name: "profile-posts",
      component: () => import("../views/ProfilePostsView.vue"),
      meta: { requiresAuth: true },
    },
    {
      path: "/profile/favorites",
      name: "profile-favorites",
      component: () => import("../views/ProfileFavoritesView.vue"),
      meta: { requiresAuth: true },
    },
    {
      path: "/notifications",
      name: "notifications",
      component: () => import("../views/NotificationCenterView.vue"),
      meta: { requiresAuth: true },
    },
    {
      path: "/admin/verifications",
      name: "admin-verifications",
      component: () => import("../views/admin/AdminVerificationReviewView.vue"),
      meta: { requiresAuth: true, roles: ["ADMIN"] },
    },
    {
      path: "/admin/community",
      name: "admin-community",
      component: () => import("../views/admin/AdminCommunityManageView.vue"),
      meta: { requiresAuth: true, roles: ["ADMIN"] },
    },
    {
      path: "/admin/jobs",
      name: "admin-jobs",
      component: () => import("../views/admin/AdminJobManageView.vue"),
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
