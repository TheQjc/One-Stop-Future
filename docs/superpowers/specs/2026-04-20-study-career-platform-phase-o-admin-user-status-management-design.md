# Study-Career Platform Phase O Admin User Status Management Design

> **Validation note:** This design was implemented and validated on 2026-04-20. Execution record: `docs/superpowers/plans/2026-04-20-study-career-platform-phase-o-admin-user-status-management-implementation.md`. Documented verification covered `AdminUserControllerTests`, `AuthControllerTests`, `UserControllerTests`, `HomeServiceTests`, `AdminUsersView.spec.js`, and `NavBar.spec.js`.

## 1. Goal

Phase O adds a bounded admin-only user-status management slice so administrators can review current account status, ban or restore non-admin accounts, and reliably enforce the resulting state across the existing authentication and signed-in business surfaces.

This phase delivers:

- a new admin-only route at `/admin/users`
- admin-only backend endpoints:
  - `GET /api/admin/users`
  - `POST /api/admin/users/{id}/ban`
  - `POST /api/admin/users/{id}/unban`
- account totals for:
  - total users
  - active users
  - banned users
  - verified users
- a protected admin row model where admin accounts remain visible but cannot be status-toggled
- admin home-entry activation for the user desk
- admin top-navigation activation for the user desk
- consistent banned-user rejection on authenticated business APIs through the shared identity lookup path
- frontend forced logout and redirect when the backend reports `account is banned`

## 2. Current Conclusion

### 2.1 Gap Before Phase O

Before this phase, the repo already had:

- a persisted user `status` field with `ACTIVE` and `BANNED`
- login-time rejection for banned users in the phone-code auth flow
- multiple admin-only workbenches for verification, community, jobs, and resources

The missing delivery slice was operational control:

- there was no admin-only user-status workbench
- there was no backend admin API for listing users or toggling status
- already-authenticated users could still reach business APIs unless the shared identity lookup enforced status centrally
- there was no dedicated admin entry for the user desk in the home summary or top navigation

### 2.2 Chosen Approach

The chosen approach is to reuse the existing `t_user.status` model and add one small admin-only management surface rather than creating a new moderation subsystem.

That means:

- keep status writes inside a focused `AdminUserService`
- protect admin rows at the service layer, not only in the UI
- route signed-in identity checks through `UserService.requireByIdentity(...)` so banned accounts are rejected consistently after login
- keep the frontend surface as one dedicated admin ledger page instead of embedding inline user actions into the dashboard

## 3. Scope Definition

### 3.1 In Scope

This phase includes:

- one backend admin list endpoint returning summary counts plus recent user rows
- one backend ban endpoint for non-admin users
- one backend unban endpoint for non-admin users
- one frontend admin route and page for user-status management
- one admin navigation link and one admin home entry for the user desk
- backend enforcement for banned users on authenticated business APIs
- frontend auth-state reset when the backend returns `403 account is banned`

### 3.2 Explicitly Out of Scope

- creating or deleting users from the admin desk
- editing roles, verification status, nickname, or profile fields from this phase
- search, filtering, pagination, bulk actions, or CSV export
- self-service appeals, audit history, or moderation comments
- changing admin account status
- dashboard charts or analytics for user-status changes

## 4. Backend Contract

### 4.1 Permission Model

All Phase O admin endpoints are admin-only.

Expected behavior:

- normal users receive `403` on `/api/admin/users`
- admin users can list, ban, and unban manageable accounts
- attempts to change an admin account return a business error:
  - `admin account status cannot be changed`

### 4.2 API Surface

`GET /api/admin/users` returns:

- `total`
- `activeCount`
- `bannedCount`
- `verifiedCount`
- `users[]`

Each `users[]` row includes:

- `id`
- `phone`
- `nickname`
- `realName`
- `role`
- `status`
- `verificationStatus`
- `studentId`
- `createdAt`
- `updatedAt`

`POST /api/admin/users/{id}/ban` returns the updated user row.

`POST /api/admin/users/{id}/unban` returns the updated user row.

### 4.3 Status Enforcement Rules

Phase O relies on two complementary enforcement points:

- login remains blocked for banned users through the existing auth flow
- authenticated business APIs now reject banned users through `UserService.requireByIdentity(...)`

This keeps one consistent user-facing error message:

- `account is banned`

## 5. Frontend Surface

### 5.1 Route and Navigation

Phase O adds:

- admin-only route:
  - `/admin/users`
- admin nav link:
  - `User Desk`
- admin home entry:
  - `admin-users`

### 5.2 Admin User Desk Behavior

The page should behave as a quiet operational ledger, not a punitive moderation console.

Required UI behavior:

- show summary cards for total, active, banned, and verified counts
- show protected admin rows in the same table/card list
- show `Ban` only for active non-admin users
- show `Restore` only for banned non-admin users
- show `Protected` for admin rows
- support loading, retry, success, and action-error states
- keep the same information available in both table and mobile-card layouts

### 5.3 Banned Session Handling

If an already-authenticated account becomes banned, the frontend must:

- clear persisted auth state
- redirect to `/login`
- treat `403 account is banned` the same as a forced auth invalidation event

## 6. Verification Strategy

Backend verification for this phase covers:

- admin-only list access and count shaping
- banning and restoring a normal user
- preventing admin-row status changes
- rejecting banned users during authenticated profile access
- preserving the admin home entry for the new desk
- preserving banned-user login rejection

Frontend verification for this phase covers:

- admin-only route registration for `/admin/users`
- loading and rendering protected plus actionable rows
- ban action reload behavior
- restore action reload behavior
- admin navigation exposure for `/admin/users`

This phase remains complete when:

- admins can open `/admin/users` and manage non-admin account status
- banned users cannot log in
- already-authenticated banned users are rejected on business APIs and forced back to login
- admin accounts remain visible but immutable from the Phase O desk
