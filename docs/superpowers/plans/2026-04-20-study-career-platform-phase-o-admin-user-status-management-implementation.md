# Phase O Admin User Status Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver an admin-only user-status workbench with list counts, ban and restore actions for non-admin accounts, and consistent banned-account enforcement across current authenticated surfaces.

**Architecture:** Reuse the existing `t_user.status` model instead of building a new moderation subsystem. Add one focused admin controller/service pair for account status management, extend the shared authenticated identity lookup so banned users are rejected after login as well as before it, then expose the workbench through one dedicated admin page plus admin home and navigation entry points.

**Tech Stack:** Java 17, Spring Boot 3, Spring Security, MyBatis-Plus, JUnit 5, MockMvc, H2, Vue 3, Vue Router, Axios, Vitest, Vite

---

## Context Before Starting

- Historical implementation anchor:
  - `d6edc16` `feat: add admin user status management`
- Spec baseline:
  - `docs/superpowers/specs/2026-04-20-study-career-platform-phase-o-admin-user-status-management-design.md`
- Existing status and identity model:
  - `backend/src/main/java/com/campus/entity/User.java`
  - `backend/src/main/java/com/campus/common/UserStatus.java`
  - `backend/src/main/java/com/campus/service/AuthService.java`
  - `backend/src/main/java/com/campus/service/UserService.java`
- Existing admin-surface patterns to mirror:
  - `backend/src/main/java/com/campus/controller/admin/AdminDashboardController.java`
  - `frontend/src/views/admin/AdminDashboardView.vue`
  - `frontend/src/components/NavBar.vue`
  - `frontend/src/router/index.js`

## Scope Lock

This plan covers only the approved Phase O slice:

- admin-only `GET /api/admin/users`
- admin-only `POST /api/admin/users/{id}/ban`
- admin-only `POST /api/admin/users/{id}/unban`
- one admin-only `/admin/users` route
- protected visibility for admin rows
- ban / restore for non-admin rows only
- admin home-entry activation for `admin-users`
- admin nav activation for `/admin/users`
- banned-user rejection on authenticated business APIs through `UserService.requireByIdentity(...)`
- frontend forced logout on `403 account is banned`

This plan explicitly does not implement:

- create, delete, or merge user accounts
- role editing
- verification review actions from the same desk
- pagination, filtering, search, or bulk operations
- self-service appeals or audit history
- changing admin account status

## Planned File Structure

### Backend: Create

- Create: `backend/src/main/java/com/campus/controller/admin/AdminUserController.java`
  Admin-only HTTP boundary for list, ban, and unban actions.
- Create: `backend/src/main/java/com/campus/dto/AdminUserListResponse.java`
  Summary counts and user-row response contract for the desk.
- Create: `backend/src/main/java/com/campus/service/AdminUserService.java`
  Read/write user-status orchestration with admin-row protection.
- Create: `backend/src/test/java/com/campus/controller/admin/AdminUserControllerTests.java`
  Controller coverage for access control, list counts, ban, unban, and protected admin rows.

### Backend: Modify Existing

- Modify: `backend/src/main/java/com/campus/service/UserService.java`
  Enforce banned-user rejection in the shared authenticated identity path.
- Modify: `backend/src/main/java/com/campus/service/HomeService.java`
  Add the `admin-users` home entry for admins.
- Modify: `backend/src/test/java/com/campus/controller/UserControllerTests.java`
  Cover already-authenticated banned-user rejection on business APIs.
- Modify: `backend/src/test/java/com/campus/service/HomeServiceTests.java`
  Cover the new admin home entry.

### Frontend: Create

- Create: `frontend/src/views/admin/AdminUsersView.vue`
  User-status workbench with counts, protected rows, and ban/restore actions.
- Create: `frontend/src/views/admin/AdminUsersView.spec.js`
  Route registration and user-desk interaction coverage.

### Frontend: Modify Existing

- Modify: `frontend/src/api/admin.js`
  Add admin user list, ban, and unban adapters.
- Modify: `frontend/src/api/http.js`
  Force logout when the backend returns `403 account is banned`.
- Modify: `frontend/src/router/index.js`
  Register the admin-only `/admin/users` route.
- Modify: `frontend/src/components/NavBar.vue`
  Add the admin `User Desk` nav link.
- Modify: `frontend/src/components/NavBar.spec.js`
  Cover the new admin navigation exposure.

### Docs: Modify Existing

- Modify: `README.md`
  Document Phase O scope, endpoints, route, and local verification steps.

## Task 1: Add the Backend Admin User Desk Contract

**Files:**

- Create: `backend/src/main/java/com/campus/controller/admin/AdminUserController.java`
- Create: `backend/src/main/java/com/campus/dto/AdminUserListResponse.java`
- Create: `backend/src/main/java/com/campus/service/AdminUserService.java`
- Create: `backend/src/test/java/com/campus/controller/admin/AdminUserControllerTests.java`

- [x] **Step 1: Add failing controller coverage for the admin user desk**

Cover:

- non-admin access denied
- admin list returns counts and rows
- admin can ban and restore a normal user
- admin account status cannot be changed

- [x] **Step 2: Run the targeted backend admin-user suite**

Run:

```powershell
cd backend
mvn -q "-Dtest=AdminUserControllerTests" test
```

Expected: PASS after implementation.

- [x] **Step 3: Implement the admin-only list, ban, and unban backend slice**

Add:

- `AdminUserListResponse`
- `AdminUserService`
- `AdminUserController`
- service-layer protection for admin rows
- summary counts for total, active, banned, and verified

- [x] **Step 4: Re-run the backend admin-user suite**

Run:

```powershell
cd backend
mvn -q "-Dtest=AdminUserControllerTests" test
```

Expected: PASS.

- [x] **Step 5: Record the historical landing anchor**

Historical feature landing:

```bash
git show --stat d6edc16
```

## Task 2: Enforce Banned Status On Existing Authenticated Surfaces

**Files:**

- Modify: `backend/src/main/java/com/campus/service/UserService.java`
- Modify: `backend/src/main/java/com/campus/service/HomeService.java`
- Modify: `backend/src/test/java/com/campus/controller/UserControllerTests.java`
- Modify: `backend/src/test/java/com/campus/service/HomeServiceTests.java`

- [x] **Step 1: Add coverage for authenticated banned-user rejection and admin home-entry activation**

Cover:

- `GET /api/users/me` returns `403 account is banned` after a user is banned
- admin home summary includes the `admin-users` entry between dashboard and verification review

- [x] **Step 2: Reuse the shared identity path for status enforcement**

Implement:

- `UserService.requireByIdentity(...)` rejects `BANNED`
- `HomeService` exposes `admin-users` for admins

- [x] **Step 3: Verify the shared enforcement slice**

Run:

```powershell
cd backend
mvn -q "-Dtest=UserControllerTests,HomeServiceTests,AuthControllerTests" test
```

Expected: PASS.

- [x] **Step 4: Keep login-time ban rejection as the same authoritative message**

Confirm:

- banned login still returns `account is banned`
- authenticated business APIs now return the same message through shared identity lookup

## Task 3: Add the Frontend Route, Navigation, and User Desk

**Files:**

- Modify: `frontend/src/api/admin.js`
- Modify: `frontend/src/api/http.js`
- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/components/NavBar.vue`
- Modify: `frontend/src/components/NavBar.spec.js`
- Create: `frontend/src/views/admin/AdminUsersView.vue`
- Create: `frontend/src/views/admin/AdminUsersView.spec.js`

- [x] **Step 1: Add failing frontend coverage**

Cover:

- admin-only `/admin/users` route exists
- page loads counts and protected rows
- ban action reloads the desk
- restore action reloads the desk
- nav includes `/admin/users` for admins

- [x] **Step 2: Implement the admin user desk frontend slice**

Add:

- `getAdminUsers()`
- `banAdminUser(id)`
- `unbanAdminUser(id)`
- admin-only `/admin/users`
- `User Desk` nav link
- forced logout when the backend reports `403 account is banned`
- the admin user desk page with loading, retry, success, and action-error states

- [x] **Step 3: Re-run the targeted frontend verification**

Run:

```powershell
cd frontend
npx vitest run src/views/admin/AdminUsersView.spec.js src/components/NavBar.spec.js
npm run build
```

Expected: PASS.

- [x] **Step 4: Keep the UI bounded**

Confirm:

- admin rows stay visible but protected
- no role editing or verification review actions appear on the desk
- no pagination, filter, or bulk-action UI slips into this phase

## Task 4: Update README And Run Final Verification

**Files:**

- Modify: `README.md`

- [x] **Step 1: Document the Phase O scope in README**

Document:

- admin endpoints:
  - `GET /api/admin/users`
  - `POST /api/admin/users/{id}/ban`
  - `POST /api/admin/users/{id}/unban`
- admin route:
  - `/admin/users`
- current Phase O scope
- local verification path for ban / restore and banned-user rejection

- [x] **Step 2: Run the targeted Phase O verification suites**

Run:

```powershell
cd backend
mvn -q "-Dtest=AdminUserControllerTests,AuthControllerTests,UserControllerTests,HomeServiceTests" test
```

```powershell
cd frontend
npx vitest run src/views/admin/AdminUsersView.spec.js src/components/NavBar.spec.js
npm run build
```

Expected: PASS.

- [x] **Step 3: Run full regression**

Run:

```powershell
cd backend
mvn test
```

```powershell
cd frontend
npm run test
npm run build
```

Expected: PASS across the full backend and frontend suites.

- [x] **Step 4: Manual smoke checklist**

1. log in as admin `13800000000`
2. open `/admin/users`
3. confirm total, active, banned, and verified counts render
4. confirm the admin row is visible and marked `Protected`
5. ban `13800000001`
6. confirm the row status becomes `BANNED`
7. confirm the banned account is blocked at login
8. confirm the banned account is also rejected on an authenticated business API until restored
9. restore `13800000001`
10. confirm the user can log in again

- [x] **Step 5: Record the historical feature commit**

Historical feature commit:

```bash
git show --stat d6edc16
```

## Execution Notes

- Keep the error message for banned users exactly `account is banned`.
- Keep admin accounts visible on the desk but do not allow status changes.
- Do not add role editing, verification review, or profile editing to this phase.
- Do not add filtering, pagination, or bulk actions to the user desk.
