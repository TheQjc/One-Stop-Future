# Study-Career Platform Phase A Foundation Design

> **Validation note:** This design was implemented and validated on 2026-04-16 using the approved execution record at `docs/superpowers/plans/2026-04-15-study-career-platform-phase-a-foundation-implementation.md`. Documented verification covered `CampusApplicationTests`, `JwtUtilTests`, `AuthControllerTests`, `UserControllerTests`, `HomeControllerTests`, `VerificationControllerTests`, `NotificationControllerTests`, `AdminVerificationControllerTests`, `App.spec.js`, `LoginView.spec.js`, `ProfileView.spec.js`, `HomeView.spec.js`, `NotificationCenterView.spec.js`, `AdminVerificationReviewView.spec.js`, full backend/frontend regression, and the deployment/config checks recorded in the execution record.

## 1. Goal

Phase A establishes the active foundation baseline for the regenerated study-career platform.

Its purpose is to replace the earlier legacy notice-oriented prototype with a thin but complete vertical slice that supports:

- phone-code authentication
- independent home aggregation
- personal center
- student verification submission
- notification center
- minimal admin verification review

This phase is the baseline that later phases build on. Community, jobs, resources, discover, analytics, decision support, and preview flows all assume the auth, identity, home, notification, and admin-review foundation delivered here.

## 2. Problem Statement

Before Phase A, the repo had a structural mismatch with the regenerated product direction:

- backend domain shape still leaned on the old notice workflow
- auth still assumed the legacy username/password direction
- frontend routing and pages still centered on the notice prototype
- the new study-career baseline required a different first slice:
  - phone-based identity
  - independent home entry
  - profile and verification workflow
  - minimal admin backbone

Phase A solves that mismatch by resetting the core domain to the new baseline instead of extending the old notice prototype further.

## 3. Scope Definition

### 3.1 In Scope

Phase A includes:

- phone-code send, register, login, and logout
- JWT-based authenticated session handling
- shared `message`-style API envelope alignment
- independent home aggregation endpoint and page
- personal profile read/update
- student verification apply flow
- site notification listing and read state
- admin verification dashboard, list, and approve/reject flow
- admin home-entry activation for verification review
- local development baseline and deployment/config scaffolding needed to run the new slice safely

### 3.2 Explicitly Out of Scope

Phase A does not implement:

- community posting, commenting, likes, favorites, or rankings
- job aggregation or job-management workflows
- resource upload/download or public resource browsing
- unified search
- discover ranking
- decision support, analytics, school comparison, or preview pipelines
- broader admin operations dashboards beyond the minimal verification-review surface

## 4. Chosen Approach

### 4.1 Reset The Foundation Domain

The chosen approach is not to evolve the old notice prototype into the new platform.
Instead, Phase A introduces the minimum regenerated foundation directly:

- user identity keyed by phone
- verification code workflow for register/login
- home summary as the first post-login and guest-facing working entry
- verification application plus admin review state machine
- site notifications as the shared feedback channel

### 4.2 Keep Infrastructure Thin But Expandable

Phase A keeps the architecture intentionally narrow:

- Spring Boot monolith backend with modular services
- Vue SPA frontend with guarded routes and shared auth state
- H2-backed test profile for repeatable backend verification
- Redis, Elasticsearch, and MinIO reserved as future extension points rather than active Phase A dependencies

The goal is a stable base for later slices, not an early all-in-one platform.

## 5. Backend Contract

### 5.1 Auth And Identity

Phase A introduces the backend auth surface:

- `POST /api/auth/codes/send`
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`

Core contract decisions:

- registration and login both use phone + verification code
- successful auth returns stable identity fields plus JWT
- new users default to:
  - role `USER`
  - status `ACTIVE`
  - verification status `UNVERIFIED`

### 5.2 Home, Profile, Verification, And Notifications

Phase A introduces the first regenerated working APIs:

- `GET /api/home/summary`
- `GET /api/users/me`
- `PUT /api/users/me`
- `POST /api/verifications`
- `GET /api/notifications`
- `POST /api/notifications/{id}/read`
- `POST /api/notifications/read-all`

Required Phase A behavior:

- guest home must explain the platform and route users into auth
- authenticated home must surface identity state, unread counts, and next-step guidance
- users can submit one active verification application at a time
- notifications carry verification outcomes and welcome messaging

### 5.3 Minimal Admin Backbone

Phase A adds only the minimum admin review slice needed to close the verification loop:

- `GET /api/admin/verifications/dashboard`
- `GET /api/admin/verifications`
- `POST /api/admin/verifications/{id}/review`

Required admin behavior:

- admin-only access boundary
- overview of pending and recently reviewed verification work
- approve action upgrades the user to `VERIFIED`
- reject action requires a reason and restores the user to the unverified path
- both outcomes generate user-facing notifications

## 6. Frontend Surface

Phase A establishes the first regenerated frontend routes and interaction model:

- `/`
- `/login`
- `/register`
- `/profile`
- `/notifications`
- `/admin/verifications`

Required frontend behavior:

- independent home acts as the primary working entry
- auth screens follow the phone-code workflow
- profile page surfaces identity and verification state
- notification center lets users review and clear unread items
- admin verification review is reachable only for admin accounts
- non-admin users must not see admin entry points

## 7. Foundation Constraints

Phase A locks several foundation constraints that later phases depend on:

- unified response structure uses `{code, message, data}`
- user status and verification status become first-class domain fields
- verification applications are deduplicated while one pending row exists
- notifications become the shared feedback mechanism for core user-state changes
- admin-only surfaces are enforced in both backend security and frontend route/nav exposure

## 8. Verification Summary

Phase A is complete when all of the following are true:

- guest users can request a code, register, log in, and reach the home flow
- authenticated users can open profile, submit verification, and consume notifications
- admin users can review verification applications through a dedicated admin surface
- approval and rejection both update user state and produce notifications correctly
- later integrated regression runs can continue building on this slice without reopening the legacy notice baseline

This phase remains the active foundation baseline referenced by later slices even though the original execution record still documents the temporary transitional cleanup from the legacy prototype.
