# Frontend Chinese Localization Follow-up Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish the remaining frontend Chinese-localization work for detail pages, profile sub-pages, shared preview UI, and admin surfaces without changing routes, APIs, or permissions.

**Architecture:** Reuse the existing Vue page/component boundaries and update one surface group at a time. Extend the existing Vitest coverage before or alongside each copy change so page behavior stays stable while user-visible wording becomes consistently Chinese-first.

**Tech Stack:** Vue 3, Vue Router 4, Pinia, Vitest, Vue Test Utils, Vite

---

## File Map

### Shared Preview

- Modify: `frontend/src/components/ResourceZipPreviewPanel.vue`
- Modify: `frontend/src/components/ResourceZipPreviewPanel.spec.js`

### Remaining User-Side Detail And Profile Pages

- Modify: `frontend/src/views/CommunityDetailView.vue`
- Modify: `frontend/src/views/CommunityDetailView.spec.js`
- Modify: `frontend/src/views/JobDetailView.vue`
- Modify: `frontend/src/views/JobDetailView.spec.js`
- Modify: `frontend/src/views/ResourceDetailView.vue`
- Modify: `frontend/src/views/ResourceDetailView.spec.js`
- Modify: `frontend/src/views/ProfileFavoritesView.vue`
- Modify: `frontend/src/views/ProfileFavoritesView.spec.js`
- Modify: `frontend/src/views/ProfileResourcesView.vue`
- Modify: `frontend/src/views/ProfileResourcesView.spec.js`
- Modify: `frontend/src/views/ProfileResumesView.vue`
- Modify: `frontend/src/views/ProfileResumesView.spec.js`
- Modify: `frontend/src/views/ProfileApplicationsView.vue`
- Modify: `frontend/src/views/ProfileApplicationsView.spec.js`
- Modify: `frontend/src/views/ResourceUploadView.vue`
- Modify: `frontend/src/views/ResourceEditView.vue`

### Remaining Admin Surfaces

- Modify: `frontend/src/views/admin/AdminDashboardView.vue`
- Modify: `frontend/src/views/admin/AdminDashboardView.spec.js`
- Modify: `frontend/src/views/admin/AdminApplicationsView.vue`
- Modify: `frontend/src/views/admin/AdminApplicationsView.spec.js`
- Modify: `frontend/src/views/admin/AdminCommunityManageView.vue`
- Modify: `frontend/src/views/admin/AdminCommunityManageView.spec.js`
- Modify: `frontend/src/views/admin/AdminJobManageView.vue`
- Modify: `frontend/src/views/admin/AdminJobManageView.spec.js`
- Modify: `frontend/src/views/admin/AdminResourceManageView.vue`
- Modify: `frontend/src/views/admin/AdminResourceManageView.spec.js`
- Modify: `frontend/src/views/admin/AdminUsersView.vue`
- Modify: `frontend/src/views/admin/AdminUsersView.spec.js`
- Modify: `frontend/src/views/admin/AdminVerificationReviewView.vue`

## Execution Order

### Task 1: Shared Preview Wording

- [x] Update `ResourceZipPreviewPanel.vue` to use Chinese title, count, loading, empty, type, and size-fallback wording.
- [x] Update `ResourceZipPreviewPanel.spec.js` to pin the Chinese shared wording.
- [x] Run: `npm --prefix frontend test -- src/components/ResourceZipPreviewPanel.spec.js`

### Task 2: User Detail Pages

- [x] Localize `CommunityDetailView.vue` and `CommunityDetailView.spec.js`.
- [x] Localize `JobDetailView.vue` and `JobDetailView.spec.js`.
- [x] Localize `ResourceDetailView.vue` and `ResourceDetailView.spec.js`.
- [x] Run: `npm --prefix frontend test -- src/views/CommunityDetailView.spec.js src/views/JobDetailView.spec.js src/views/ResourceDetailView.spec.js`

### Task 3: Profile Sub-Pages

- [x] Localize `ProfileFavoritesView.vue` and `ProfileFavoritesView.spec.js`.
- [x] Localize `ProfileResourcesView.vue` and `ProfileResourcesView.spec.js`.
- [x] Localize `ProfileResumesView.vue` and `ProfileResumesView.spec.js`.
- [x] Localize `ProfileApplicationsView.vue` and `ProfileApplicationsView.spec.js`.
- [x] Run: `npm --prefix frontend test -- src/views/ProfileFavoritesView.spec.js src/views/ProfileResourcesView.spec.js src/views/ProfileResumesView.spec.js src/views/ProfileApplicationsView.spec.js`

### Task 4: Resource Workflow Pages

- [x] Localize `ResourceUploadView.vue`.
- [x] Localize `ResourceEditView.vue`.
- [x] Run: `npm --prefix frontend test -- src/views/ResourceUploadView.spec.js src/views/ResourceEditView.spec.js`

### Task 5: Admin Pages

- [x] Localize `AdminDashboardView.vue` and `AdminDashboardView.spec.js`.
- [x] Localize `AdminApplicationsView.vue` and `AdminApplicationsView.spec.js`.
- [x] Localize `AdminCommunityManageView.vue` and `AdminCommunityManageView.spec.js`.
- [x] Localize `AdminJobManageView.vue` and `AdminJobManageView.spec.js`.
- [x] Localize `AdminResourceManageView.vue` and `AdminResourceManageView.spec.js`.
- [x] Localize `AdminUsersView.vue` and `AdminUsersView.spec.js`.
- [x] Localize `AdminVerificationReviewView.vue`.
- [x] Run: `npm --prefix frontend test -- src/views/admin/AdminDashboardView.spec.js src/views/admin/AdminApplicationsView.spec.js src/views/admin/AdminCommunityManageView.spec.js src/views/admin/AdminJobManageView.spec.js src/views/admin/AdminResourceManageView.spec.js src/views/admin/AdminUsersView.spec.js src/views/admin/AdminVerificationReviewView.spec.js`

### Task 6: Full Verification

- [x] Run: `npm --prefix frontend test`
- [x] Run: `npm --prefix frontend run build`
- [x] Re-run targeted English audit under `frontend/src` and summarize remaining out-of-scope strings if any

## Completion Notes

- Full frontend test suite passed: `38` files, `118` tests.
- Production build passed with `npm --prefix frontend run build`.
- Follow-up polish also localized the remaining English-first copy in `CommunityCreateView.vue`, `ProfilePostsView.vue`, and `PageFooter.vue`.
- Shared discover-card fallback metadata now uses Chinese-first copy (`平台推荐`) instead of the English brand as a generic fallback.
- Test/demo-facing mock identities were normalized to Chinese sample names in auth/admin flows so local demo and verification screens no longer surface `PlatformAdmin`, `NormalUser`, or `VerifiedUser`.
- Intentional residual English is limited to the secondary brand text `One-Stop Future` in the nav/footer and dynamic school names returned by data sources in the school comparison view.
