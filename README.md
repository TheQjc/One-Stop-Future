# One-Stop Future

Current repo status: `Phase A foundation + Phase B community + Phase C jobs + Phase D resource library first slice + Phase E unified search first slice + Phase F discover ranking first slice + Phase G resource lifecycle completion first slice + Phase H resource preview expansion first slice + Phase I MinIO raw resource storage first slice + Phase J historical local resource MinIO migration first slice + Phase K decision support first slice + Phase L decision analytics first slice + Phase M admin dashboard first slice + Phase N job application and resume workflow first slice + Phase O admin user status management first slice + Phase P community hot ranking first slice + Phase Q community experience post structure first slice + Phase R community threaded replies first slice + Phase S DOCX resource preview first slice + Phase T MinIO preview artifact storage first slice + Phase U admin batch job import first slice + Phase V third-party job sync first slice + Phase W historical preview artifact MinIO migration first slice + Phase X preview artifact runtime dual-read fallback first slice + Phase Y preview artifact cleanup first slice + Phase Z resume online preview first slice + Phase AA application snapshot online preview first slice`.

## Current Scope

Implemented now:

- phone-code register / login
- account status control with banned-login enforcement
- independent aggregation home
- profile center and student verification apply flow
- notification center
- community list / detail / create / comment / reply / like / favorite
- community hot ranking with `DAY / WEEK / ALL` boards inside `/community`
- optional community experience-post structure in create / list / detail flows
- one-level community threaded replies with reply notifications
- my posts / my favorites
- jobs list / detail / filters / source jump
- job favorite / unfavorite
- resume library upload / list / preview / download / delete
- in-platform one-time job apply with resume snapshot
- my applications history with snapshot preview / download actions
- admin applications read-only workbench with snapshot preview / download
- public resource list / detail / upload
- resource preview / download / favorite / unfavorite
- my resources / resource favorites / rejected edit-resubmit
- admin verification review
- admin community moderation
- admin jobs create / edit / publish / offline / delete
- admin batch job import from UTF-8 CSV on `/admin/jobs`, with imported rows created as `DRAFT` and whole-file rollback on validation failure
- admin third-party job sync from one fixed HTTP JSON feed on `/admin/jobs`, with new rows created as `DRAFT`, existing non-`DELETED` rows updated by `sourceUrl`, and skipped / invalid items reported in the same sync summary
- admin resource publish / reject / offline review workspace
- MinIO-backed raw resource storage for non-`local` runtimes and independently selectable MinIO-backed preview artifact storage for newly generated `PPTX` / `DOCX` / `ZIP` preview artifacts
- admin historical local-resource MinIO migration with dry-run and bounded batch execution
- admin historical preview-artifact MinIO migration with dry-run and bounded batch execution
- admin dashboard read-only summary overview with handoff to existing workbenches
- admin user status workbench with ban / restore controls for non-admin accounts
- unified search across published posts / jobs / resources
- discover board across published posts / jobs / resources
- homepage discover preview with weekly public picks
- discover / homepage post ranking gives experience posts a small deterministic boost without changing community-feed ordering
- authenticated decision assessment questions / submit / latest result
- authenticated direction timeline with stable anchor-date fallback
- public school candidate listing and 2-4 school comparison for `EXAM` / `ABROAD`
- authenticated homepage assessment entry activation
- public decision analytics desk at `/analytics` with `7D / 30D` overview, trend, and direction-mix views
- authenticated personal decision analytics snapshot / history / next actions on the same `/analytics` page
- homepage analytics entry activation for both guests and authenticated users
- PDF inline preview for visible resources
- PPTX inline preview via cached PDF conversion
- DOCX inline preview via cached PDF conversion through configurable `soffice`
- ZIP directory-tree preview for visible resources

Explicitly not implemented yet:

- full admin operations dashboards, DAU / funnel metrics, or exportable analytics reports
- version history, chunk upload, or resume rename / replace

## Project Structure

- `backend/`: Spring Boot 3, Spring Security, MyBatis-Plus, JWT
- `frontend/`: Vue 3, Pinia, Vue Router, Axios, Vite, Vitest
- `docs/superpowers/`: requirements, specs, plans
- `backend/.local-storage/resources/`: default local raw resource storage in the `local` profile
- `backend/.local-storage/previews/`: default cached PPTX-to-PDF, DOCX-to-PDF, and ZIP preview artifacts in the `local` profile; non-`local` runtimes can instead write newly generated preview artifacts to MinIO through `RESOURCE_PREVIEW_TYPE=minio`
  - current preview behavior invalidates preview cache by fingerprinting and writing a new artifact
  - switching preview storage from local to MinIO does not automatically migrate historical local preview artifacts
  - runtime reads can use `MinIO first -> local historical fallback` only when `RESOURCE_PREVIEW_READ_FALLBACK_LOCAL_ENABLED=true`
  - admins can instead trigger historical preview-artifact MinIO migration through `POST /api/admin/resources/migrate-preview-artifacts-to-minio`
  - stale derived preview artifacts are cleaned up best-effort after rejected-resource resubmission when the old logical preview key is known exactly
  - passive resource interactions and admin status transitions no longer rotate preview artifact keys through unrelated `updatedAt` writes
  - this phase still does not introduce recursive preview-root scanning or scheduled preview garbage collection
  - to reset derived preview state during local development, stop the backend and delete `backend/.local-storage/previews/`

## Local Run

### Backend

```bash
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

Notes:

- local development must use the `local` profile
- `local` uses embedded H2, seeded demo data, local filesystem resource storage, and local filesystem preview artifact storage
- current defaults are relative paths; with `cd backend` they resolve to `backend/.local-storage/resources/` and `backend/.local-storage/previews/`
- `application-local.yml` pins `RESOURCE_PREVIEW_TYPE=local`, so local development keeps preview artifacts on disk
- resource-library `DOCX` preview requires LibreOffice `soffice` on `PATH`, or `RESOURCE_PREVIEW_DOCX_SOFFICE_COMMAND` pointing to the installed binary
- local backend address: `http://127.0.0.1:8080`

### Frontend

```bash
cd frontend
npm install
npm run dev -- --host 127.0.0.1
```

Notes:

- frontend address is usually `http://127.0.0.1:5173`
- if `5173` is occupied, Vite will select the next free port; use the actual port printed in the dev server log
- Vite proxies `/api/**` to `http://127.0.0.1:8080`
- current recommendation is local backend + local frontend, not Docker

### Optional Docker Stack

If Docker and Docker Compose are available locally, an optional deployment scaffold is included:

```bash
docker compose up --build
```

Notes:

- frontend is exposed on `http://127.0.0.1:5173`
- MySQL is exposed on `127.0.0.1:3306`
- MinIO API is exposed on `http://127.0.0.1:9000`
- MinIO console is exposed on `http://127.0.0.1:9001`
- backend is only exposed inside the Compose network and is reached through the frontend Nginx proxy
- backend stores raw resource files in MinIO and keeps preview artifacts in the `backend-data` named volume
- current `docker-compose.yml` does not set `RESOURCE_PREVIEW_TYPE=minio`, so this Compose scaffold still keeps preview artifacts local
- current backend image does not install LibreOffice `soffice`, so DOCX preview generation is not container-ready by default
- current recommendation for day-to-day development is still the local backend + local frontend flow above
- switching an existing local-file database to MinIO is a manual admin-triggered backend migration flow in this phase, not an automatic runtime cutover

## Preview Artifact Storage

Backend preview-artifact storage is configured independently from raw resource storage:

- `RESOURCE_PREVIEW_TYPE=local|minio`
- `RESOURCE_PREVIEW_LOCAL_ROOT=.local-storage/previews`
- `RESOURCE_PREVIEW_MINIO_PREFIX=preview-artifacts`
- `RESOURCE_PREVIEW_READ_FALLBACK_LOCAL_ENABLED=false`
- `RESOURCE_PREVIEW_DOCX_SOFFICE_COMMAND=soffice`

Current behavior:

- `RESOURCE_PREVIEW_TYPE=local` keeps preview artifacts on the local filesystem
- `RESOURCE_PREVIEW_TYPE=minio` requires `MINIO_ENABLED=true` and reuses the shared `MINIO_BUCKET`
- preview-artifact storage selection is independent from `RESOURCE_STORAGE_TYPE`
- only newly generated preview artifacts are written to MinIO after switching preview storage to `minio`
- when `RESOURCE_PREVIEW_TYPE=minio` and `RESOURCE_PREVIEW_READ_FALLBACK_LOCAL_ENABLED=true`, runtime preview reads first check MinIO and then fall back to the existing local preview root for historical `PPTX`, `DOCX`, and `ZIP` artifacts
- fallback hits remain read-only and do not automatically copy artifacts into MinIO
- MinIO infrastructure failures are not masked by local fallback
- admins can call `POST /api/admin/resources/migrate-preview-artifacts-to-minio` to dry-run or execute historical preview-artifact migration into MinIO
- migration targets only the current logical `PPTX`, `DOCX`, or `ZIP` preview artifact for each eligible resource
- successful migration keeps local source preview artifacts in place
- stale derived preview artifacts are cleaned up best-effort after rejected-resource resubmission when the old logical preview key is known exactly
- passive resource interactions and admin status transitions no longer rotate preview artifact keys through unrelated `updatedAt` writes
- this phase still does not introduce recursive preview-root scanning or scheduled preview garbage collection

## Local Demo Accounts

When running the backend with the `local` profile, the seeded accounts are:

- admin: `13800000000` (`PlatformAdmin`)
- normal user: `13800000001` (`NormalUser`)
- verified user: `13800000002` (`VerifiedUser`)

Login uses phone-code authentication:

- open `/login`
- request a login code for the target phone number
- in the local profile, the backend returns a debug code that can be used immediately

## Test And Build

### Backend

```bash
cd backend
mvn -q test
```

### Frontend Tests

```bash
cd frontend
npm run test
```

### Frontend Build

```bash
cd frontend
npm run build
```

## Key Routes

Public / user:

- `/`
- `/login`
- `/register`
- `/search`
- `/discover`
- `/community`
- `/community/:id`
- `/community/create`
- `/jobs`
- `/jobs/:id`
- `/resources`
- `/resources/:id`
- `/resources/:id/edit`
- `/resources/upload`
- `/assessment`
- `/analytics`
- `/timeline`
- `/schools/compare`
- `/profile`
- `/profile/posts`
- `/profile/favorites`
- `/profile/resumes`
- `/profile/applications`
- `/profile/resources`
- `/notifications`

Admin:

- `/admin/dashboard`
- `/admin/users`
- `/admin/applications`
- `/admin/verifications`
- `/admin/community`
- `/admin/jobs`
- `/admin/resources`

## Foundation Auth, Home, Profile, Notifications, And Verification

Backend endpoints:

- `POST /api/auth/codes/send`
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/home/summary`
- `GET /api/users/me`
- `PUT /api/users/me`
- `POST /api/verifications`
- `GET /api/notifications`
- `POST /api/notifications/{id}/read`
- `POST /api/notifications/read-all`

Frontend routes:

- `/`
- `/login`
- `/register`
- `/profile`
- `/notifications`

Current Phase A scope:

- guests can open the independent home page, request a phone code, and enter the auth flow from `/login` or `/register`
- authenticated users receive the same home summary entry with identity state, unread counts, and next-step guidance
- `/profile` exposes the current account profile and the verification-status view of the signed-in user
- one user can submit one active verification application at a time through the first verification flow
- `/notifications` supports list, single-read, and read-all behavior for welcome, verification, and later workflow notifications
- the admin-side verification review loop stays on `/admin/verifications`; approval and rejection both flow back into user-visible status and notifications

## Resource Library, Lifecycle, And Review

Backend endpoints:

- `GET /api/resources`
- `GET /api/resources/mine`
- `GET /api/resources/{id}`
- `POST /api/resources`
- `PUT /api/resources/{id}`
- `POST /api/resources/{id}/favorite`
- `DELETE /api/resources/{id}/favorite`
- `GET /api/resources/{id}/download`
- `GET /api/resources/{id}/preview`
- `GET /api/resources/{id}/preview-zip`
- `GET /api/admin/resources`
- `POST /api/admin/resources/{id}/publish`
- `POST /api/admin/resources/{id}/reject`
- `POST /api/admin/resources/{id}/offline`

Frontend routes:

- `/resources`
- `/resources/upload`
- `/resources/:id`
- `/resources/:id/edit`
- `/profile/resources`
- `/admin/resources`

Current Phase D + G + H + S scope:

- public resources list and detail pages support keyword/category browsing, visible-detail viewing, and favorite / unfavorite behavior
- authenticated users can upload resources, revisit them in `/profile/resources`, and resubmit rejected records through `/resources/:id/edit`
- resource review stays inside `/admin/resources`, where admins can publish, reject, or offline submitted records
- PDF preview is supported inline, while PPTX and DOCX preview flow through cached PDF conversion and ZIP preview uses a directory-tree response
- raw-resource and preview-artifact MinIO migration details stay documented in the dedicated storage sections below

## Jobs Browsing, Detail, And Favorites

Backend endpoints:

- `GET /api/jobs`
- `GET /api/jobs/{id}`
- `POST /api/jobs/{id}/favorite`
- `DELETE /api/jobs/{id}/favorite`

Frontend routes:

- `/jobs`
- `/jobs/:id`

Current Phase C scope:

- public jobs list supports keyword, city, job type, education requirement, and source-platform filtering
- job detail keeps the public source link and reflects the signed-in user's current favorite state
- authenticated users can favorite and unfavorite visible job cards without leaving the jobs list or detail context
- in-platform apply, resume selection, and application history remain documented in the dedicated workflow section below

## Job Application And Resume Workflow

Backend endpoints:

- `GET /api/resumes/mine`
- `POST /api/resumes`
- `GET /api/resumes/{id}/preview`
- `GET /api/resumes/{id}/download`
- `DELETE /api/resumes/{id}`
- `POST /api/jobs/{id}/apply`
- `GET /api/applications/mine`
- `GET /api/applications/{id}/resume/preview`
- `GET /api/applications/{id}/resume/download`
- `GET /api/admin/applications`
- `GET /api/admin/applications/{id}/resume/preview`
- `GET /api/admin/applications/{id}/resume/download`

Current Phase N + Phase AA scope:

- authenticated users can keep multiple resume files in `/profile/resumes`
- supported resume formats are `PDF`, `DOC`, and `DOCX`
- authenticated users can preview their own `PDF` and `DOCX` resumes from `/profile/resumes`
- `DOC` resumes remain download-only in this phase
- published job detail pages keep the external `Source Link` and also expose in-platform apply
- one user can apply to the same job at most once
- each application stores an immutable resume snapshot so deleting the live resume does not break historical download
- `/profile/applications` is applicant-facing and read-only, and now supports snapshot preview for `PDF` / `DOCX` plus snapshot download for `PDF` / `DOCX` / `DOC`
- `/admin/applications` is admin-only and read-only, and now supports snapshot preview for `PDF` / `DOCX` plus snapshot download for `PDF` / `DOCX` / `DOC`
- application snapshot preview remains separate from live resume preview on `/profile/resumes`
- `DOC` application snapshots remain download-only in this phase

## Admin Dashboard

Admin backend endpoint:

- `GET /api/admin/dashboard/summary`

Admin frontend route:

- `/admin/dashboard`

Current admin-dashboard scope:

- admin-only access boundary on both the backend summary endpoint and frontend route
- read-only overview for key admin work areas; no inline moderation or editing actions on the dashboard itself
- summary covers verification, community, jobs, and resources with counts, recent items, and handoff entry points
- dashboard cards hand off to the existing admin workbenches for verification review, community moderation, job management, and resource review
- local verification path: log in as admin, open `/admin/dashboard`, and verify both the homepage entry and main nav entry open the same dashboard route

## Admin User Status Management

Admin backend endpoints:

- `GET /api/admin/users`
- `POST /api/admin/users/{id}/ban`
- `POST /api/admin/users/{id}/unban`

Admin frontend route:

- `/admin/users`

Current Phase O scope:

- admin-only user-status workbench with account totals and current status rows
- admin accounts stay visible but protected from status changes in this phase
- non-admin accounts can be banned and restored from the same admin surface
- banned users are blocked at login and also rejected on authenticated business APIs until restored

## Admin Job Management, Import, And Sync

Admin backend endpoints:

- `GET /api/admin/jobs`
- `POST /api/admin/jobs`
- `PUT /api/admin/jobs/{id}`
- `POST /api/admin/jobs/{id}/publish`
- `POST /api/admin/jobs/{id}/offline`
- `POST /api/admin/jobs/{id}/delete`
- `POST /api/admin/jobs/import`
- `POST /api/admin/jobs/sync`

Admin frontend route:

- `/admin/jobs`

Current Phase C + U + V scope:

- admin-only jobs workbench keeps the existing create / edit / publish / offline / delete lifecycle in one place
- CSV batch import accepts one `UTF-8` file through the existing `/admin/jobs` page and creates valid imported rows as `DRAFT`
- CSV import is all-or-nothing in this phase; validation failure rolls back the whole file instead of partially importing rows
- third-party sync pulls one fixed server-side HTTP JSON feed from the same `/admin/jobs` workbench with no separate admin route
- newly synced jobs are created as `DRAFT`, while existing non-`DELETED` rows update in place by `sourceUrl`
- local `DELETED` jobs are skipped and reported instead of being recreated automatically
- neither CSV import nor third-party sync auto-publishes jobs in this phase

## Admin Verification Review

Admin backend endpoints:

- `GET /api/admin/verifications/dashboard`
- `GET /api/admin/verifications`
- `POST /api/admin/verifications/{id}/review`

Admin frontend route:

- `/admin/verifications`

Current Phase A scope:

- admin-only verification dashboard surfaces pending and recently reviewed application counts
- the existing `/admin/verifications` workbench lists submitted verification applications for review
- admins can approve or reject one application at a time from the same workbench
- approval upgrades the applicant to `VERIFIED`, while rejection returns the user to the unverified path
- review outcomes generate user-facing notifications and keep the review loop closed inside the admin verification desk

## Admin Community Moderation

Admin backend endpoints:

- `GET /api/admin/community/posts`
- `POST /api/admin/community/posts/{id}/hide`
- `POST /api/admin/community/posts/{id}/delete`

Admin frontend route:

- `/admin/community`

Current Phase B scope:

- admin-only community moderation stays inside the existing `/admin/community` workbench
- admins can review submitted community posts from one moderation list
- hiding a post removes it from public visibility without treating it as a hard delete in the UI contract
- deleting a post is an explicit admin moderation action for content that should not remain available

## Community Feed And Interaction

Backend endpoints:

- `GET /api/community/posts`
- `GET /api/community/posts/mine`
- `GET /api/community/posts/{id}`
- `POST /api/community/posts`
- `POST /api/community/posts/{id}/comments`
- `POST /api/community/comments/{id}/replies`
- `POST /api/community/posts/{id}/like`
- `DELETE /api/community/posts/{id}/like`
- `POST /api/community/posts/{id}/favorite`
- `DELETE /api/community/posts/{id}/favorite`

Frontend routes:

- `/community`
- `/community/create`
- `/community/:id`
- `/profile/posts`
- `/profile/favorites`

Current Phase B scope:

- the public community feed supports list and detail browsing for published posts
- authenticated users can create posts, publish top-level comments, like posts, and favorite posts
- `/profile/posts` and `/profile/favorites` provide the signed-in user's authored-post and favorite-post views
- the dedicated hot-ranking, experience-post, and threaded-reply refinements remain documented in the community sections below

## Community Hot Ranking

Public backend endpoint:

- `GET /api/community/hot`

Supported query params:

- `period`
- `limit`

Supported period values:

- `DAY`
- `WEEK`
- `ALL`

Current Phase P scope:

- `/community` now includes a dedicated public hot-ranking block above the latest-post feed
- hot ranking is community-only and does not reuse the discover page contract
- only published posts participate in the board
- `DAY` means posts published in the last rolling 24 hours, ranked by current cumulative heat
- `WEEK` means posts published in the last rolling 7 days, ranked by current cumulative heat
- `ALL` means all published history
- current heat formula uses `likeCount * 3 + commentCount * 4 + favoriteCount * 5 + verifiedAuthorBonus + freshnessBonus`
- there is no Redis cache, interaction-event delta ranking, or separate `/community/hot` page in this phase

## Community Experience Posts

Public backend endpoints:

- `GET /api/community/posts`
- `GET /api/community/posts/{id}`

Authenticated backend endpoint:

- `POST /api/community/posts`

Current Phase Q scope:

- community posts can optionally set `experiencePost=true` plus `experienceTargetLabel`, `experienceOutcomeLabel`, `experienceTimelineSummary`, and `experienceActionSummary`
- non-experience posts keep the same behavior and return `experience.enabled=false`
- community list cards and detail pages render the structured experience summary when the flag is enabled
- discover ranking and homepage discover preview give experience posts a small deterministic score bonus
- the bonus does not change `/community` latest-feed ordering, `/community/hot`, or unified search ordering

## Community Threaded Replies

Public backend endpoint:

- `GET /api/community/posts/{id}`

Authenticated backend endpoints:

- `POST /api/community/posts/{id}/comments`
- `POST /api/community/comments/{id}/replies`

Current Phase R scope:

- first-level comments remain supported through the existing post comment endpoint
- community post detail now returns top-level comments with nested `replies`
- replies are limited to one additional level under a top-level comment
- replying to a reply is rejected in this phase
- replying to another user's top-level comment creates `COMMUNITY_REPLY_RECEIVED`
- replying to your own top-level comment does not create a notification
- the notification center maps the new reply notification type to a readable community-reply label
- infinite-depth threads, reply editing, reply deletion, and comment-anchor deep links remain out of scope

## Unified Search

Public backend endpoint:

- `GET /api/search`

Supported query params:

- `q`
- `type`
- `sort`

Supported search types:

- `ALL`
- `POST`
- `JOB`
- `RESOURCE`

Supported sort types:

- `RELEVANCE`
- `LATEST`

Current search scope:

- published community posts only
- published jobs only
- published resources only
- guest-accessible public results only

## Discover Ranking

Public backend endpoint:

- `GET /api/discover`

Supported query params:

- `tab`
- `period`
- `limit`

Supported discover tabs:

- `ALL`
- `POST`
- `JOB`
- `RESOURCE`

Supported period values:

- `WEEK`
- `ALL`

Current discover scope:

- ranks published community posts, published jobs, and published resources on one public board
- `WEEK` covers the last 7 rolling days and sorts by current cumulative heat
- `ALL` covers all published history
- homepage includes a `discoverPreview` payload for the weekly board

## Decision Support And Analytics

Backend endpoints:

- `GET /api/decision/assessment/questions`
- `POST /api/decision/assessment/submissions`
- `GET /api/decision/assessment/latest`
- `GET /api/decision/timeline?track=CAREER|EXAM|ABROAD&anchorDate=YYYY-MM-DD`
- `GET /api/decision/schools?track=EXAM|ABROAD&keyword=...`
- `POST /api/decision/schools/compare`
- `GET /api/analytics/summary?period=7D|30D`

Current decision-support and analytics scope:

- assessment is authenticated and backed by seeded backend-owned questions
- submit persists the latest assessment result and returns deterministic scores / ranking / next actions
- timeline is authenticated and uses explicit `anchorDate` first, otherwise the latest assessment session date
- timeline returns `assessmentRequired=true` when neither explicit anchor nor latest result exists
- school candidate list and compare are public read-only endpoints for `EXAM` and `ABROAD`
- school compare enforces `2-4` schools, preserves accepted request order, and returns explicit missing-value markers
- analytics is a public mixed desk at `/analytics`
- guests can open `/analytics` and read public overview cards, trend cards, and direction mix
- authenticated users get the same public board plus personal snapshot, recent history, and backend-provided next actions
- analytics period switching is backend-owned and supports only `7D` and `30D`
- homepage `assessment` entry is live for authenticated users and `LOGIN_REQUIRED` for guests
- homepage `analytics` is live for guests and authenticated users
- admin dashboard overview is admin-only and read-only
- full admin operations dashboards remain out of scope in this phase

## Historical Local Resource MinIO Migration

Admin backend endpoint:

- `POST /api/admin/resources/migrate-to-minio`

Current migration scope:

- admin-triggered and backend-only; there is no frontend migration UI in this phase
- supports dry-run and bounded batch execution for historical raw resource files
- keeps the existing `storageKey` and leaves the original local files in place after successful upload
- reads source files from `app.resource-storage.local-root`
- requires `platform.integrations.minio.enabled=true` even when active raw resource storage is still local
- environment-variable-based deployments supply that enablement through the existing mapping `MINIO_ENABLED=true`
- this migration flow covers raw resource files only; preview artifacts use the separate admin endpoint `POST /api/admin/resources/migrate-preview-artifacts-to-minio`

## Permissions

Guest:

- can browse home, community, jobs, and published resources
- can use unified search for published posts, jobs, and resources
- can browse the public discover board
- can open `/analytics` and read the public decision analytics board
- can browse public school candidates and school comparison for `EXAM` / `ABROAD`
- can preview published PDF resources inline
- can preview published PPTX resources inline as converted PDF
- can preview published DOCX resources inline as converted PDF
- can preview published ZIP resources as directory trees
- cannot create content, save favorites, or download resource files

Authenticated user:

- can create community posts
- can comment / reply / like / favorite community posts
- can favorite jobs
- can manage their own resume library at `/profile/resumes`, preview `PDF` / `DOCX` resumes inline, and download stored resume files
- can apply to a published job once with one selected resume
- can review their application history at `/profile/applications`, preview `PDF` / `DOCX` snapshot resumes inline, and download stored snapshot resumes
- can complete the decision assessment and view the latest result
- can open `/analytics` and view personal snapshot / history / next actions when available
- can open the direction timeline after assessment
- can upload resources
- can preview published PDFs, visible PPTX resources, visible DOCX resources, and visible ZIP directory trees
- can favorite, unfavorite, and download published resources
- can edit and resubmit their own rejected resources from `/resources/:id/edit`
- can view profile favorites for `POST`, `JOB`, and `RESOURCE`
- can view `/profile/resources` with lifecycle actions

Admin:

- can open `/admin/dashboard` for a read-only overview and use it to enter existing admin workbenches
- can open `/admin/applications` and preview `PDF` / `DOCX` application snapshot resumes inline, or download snapshot resumes
- can review verification applications
- can moderate community posts
- can maintain job cards, import UTF-8 CSV jobs, and trigger fixed-feed job sync from `/admin/jobs`
- can review resources through publish / reject / offline actions
- can preview PDF, PPTX, and DOCX resources inline and ZIP contents from the admin resource board

## Current Data Shapes

Favorite target types:

- `POST`
- `JOB`
- `RESOURCE`

Job statuses:

- `DRAFT`
- `PUBLISHED`
- `OFFLINE`
- `DELETED`

Job types:

- `INTERNSHIP`
- `FULL_TIME`
- `CAMPUS`

Education requirements:

- `ANY`
- `BACHELOR`
- `MASTER`
- `DOCTOR`

Resource categories:

- `EXAM_PAPER`
- `LANGUAGE_TEST`
- `RESUME_TEMPLATE`
- `INTERVIEW_EXPERIENCE`
- `OTHER`

Resource statuses:

- `PENDING`
- `PUBLISHED`
- `REJECTED`
- `OFFLINE`

## Manual Smoke Checklist

1. Start backend with `mvn spring-boot:run "-Dspring-boot.run.profiles=local"`.
2. Start frontend with `npm run dev -- --host 127.0.0.1`.
3. As guest, open `/community` and confirm the hot board renders above the latest-post feed.
4. As guest, switch the community hot board across `DAY`, `WEEK`, and `ALL`.
5. As a signed-in user, open a community detail page, publish a top-level comment, and confirm it appears in the comment list.
6. As a different signed-in user, reply to that top-level comment and confirm the reply renders nested under the correct comment.
7. Confirm replying to a reply is not available in the UI and is rejected by the backend contract in this phase.
8. Open `/notifications` as the replied-to user and confirm the new reply notification appears with the community-reply label.
9. As guest, open `/jobs` and `/resources`.
10. As guest, open a published PDF `/resources/:id` and confirm `Preview` works without login.
11. As guest, open a published PPTX `/resources/:id` and confirm `Preview` opens converted PDF output without login.
12. As guest, open a published ZIP `/resources/:id` and confirm `Preview Contents` loads a directory tree inline.
13. As guest, open a published DOCX `/resources/:id` and confirm `Preview` opens converted PDF output without login.
14. As guest, confirm the download action is still blocked by login.
15. Log in as the normal user `13800000001` and upload a resource from `/resources/upload`.
16. Open `/profile/resources` and confirm the new file appears as `PENDING`.
17. Log in as the admin `13800000000` and open `/admin/resources`.
18. Reject a pending resource with a clear review note.
19. Log back in as the owner, open `/profile/resources`, click `Edit And Resubmit`, revise metadata, and submit without replacing the file.
20. Repeat the resubmission flow with a PDF replacement file and confirm the record returns to `PENDING`.
21. Repeat the resubmission flow with a PPTX or DOCX replacement file and confirm the next preview opens regenerated PDF output instead of stale cached content.
22. As the owner, preview an unpublished visible PDF, PPTX, or DOCX from `/profile/resources` or `/resources/:id`.
23. As admin, confirm preview is shown for visible PDF / PPTX / DOCX rows and `Preview Contents` is shown for ZIP rows in `/admin/resources`.
24. Publish the pending resource and confirm it appears in the public `/resources` list.
25. Favorite and download a published resource as a normal user.
26. Open `/profile/favorites` and switch between `POST`, `JOB`, and `RESOURCE`.
27. Use the homepage search box or `/search` to search `resume`.
28. Switch `ALL / POST / JOB / RESOURCE` and `RELEVANCE / LATEST`.
29. Refresh `/search` and confirm the search state stays in the URL.
30. Open `/discover` as a guest and confirm the page loads a ranked public board.
31. Switch discover `ALL / POST / JOB / RESOURCE` and `WEEK / ALL`.
32. Refresh `/discover?tab=JOB&period=ALL` and confirm the state stays in the URL.
33. Return to `/` and confirm the homepage discover preview shows items or a graceful empty state.
34. Log in as `13800000001`, open `/assessment`, answer all questions, and submit one result.
35. Confirm the result renders a recommended track plus links into `/timeline` and `/schools/compare`.
36. Open `/timeline` and confirm it defaults to the recommended track and renders milestone cards.
37. Switch `/timeline` among `CAREER`, `EXAM`, and `ABROAD` and confirm the milestone list reloads.
38. Open `/schools/compare` as either guest or authenticated user, select `2-4` schools, and confirm compare table + chart region render.
39. As guest, open `/analytics` and confirm public overview, trend cards, and decision mix render.
40. Log in as `13800000001`, open `/analytics`, and confirm either the personal snapshot/history or the assessment CTA renders.
41. Return to `/` and confirm both `assessment` and `analytics` entries are live.
42. Log in as the admin `13800000000`, open `/admin/dashboard`, and confirm the page renders a read-only overview.
43. From both the homepage admin entry and the main nav admin entry, confirm navigation lands on `/admin/dashboard`.
44. From `/admin/dashboard`, confirm the handoff links route into the existing admin workbenches.
45. Log in as `13800000001`, open `/profile/resumes`, upload at least one `PDF` or `DOCX` resume, and confirm the row shows both `Preview` and `Download`.
46. Use `Preview` from `/profile/resumes`, confirm the resume opens inline, then open `/jobs/1`, confirm `Source Link` is still present, and submit one in-platform application with that previewable resume.
47. Open `/profile/applications` and confirm the new record shows job title, company, city, status, submitted time, the resume snapshot title, plus both `Preview` and `Download`.
48. Use `Preview` from `/profile/applications`, confirm the snapshot opens inline, then delete the original live resume from `/profile/resumes`, return to `/profile/applications`, and confirm the record still renders.
49. Log in as admin `13800000000`, open `/admin/applications`, and confirm the same record renders with applicant info, resume snapshot file name, and both `Preview` and `Download Resume`.
50. Use admin `Preview` or `Download Resume` from `/admin/applications` and confirm the snapshot is still available after the live resume deletion.
51. Return to `/jobs/1` as the applicant and confirm the page still shows the applied state.
52. Log in as admin `13800000000`, open `/admin/users`, and confirm the list shows total, active, banned, and verified counts.
53. Ban the normal user `13800000001` from `/admin/users` and confirm the row status becomes `BANNED`.
54. Try to open `/profile` or log in again as `13800000001` and confirm the app blocks the banned account with an explicit error.
55. Restore `13800000001` from `/admin/users` and confirm the user can log in again.

## Targeted Community Hot Ranking Verification

### Backend

```bash
cd backend
mvn -q -Dtest=CommunityControllerTests test
```

### Frontend

```bash
cd frontend
npx vitest run src/views/CommunityListView.spec.js
```

## Targeted Community Experience Post Verification

### Backend

```bash
cd backend
mvn -q "-Dtest=CommunityControllerTests,DiscoverControllerTests,HomeControllerTests" test
```

### Frontend

```bash
cd frontend
npx vitest run src/views/CommunityCreateView.spec.js src/views/CommunityDetailView.spec.js src/views/CommunityListView.spec.js src/views/HomeView.spec.js src/views/DiscoverView.spec.js
```

## Targeted Community Threaded Reply Verification

### Backend

```bash
cd backend
mvn -q "-Dtest=CommunityControllerTests,NotificationControllerTests,HomeControllerTests" test
```

### Frontend

```bash
cd frontend
npx vitest run src/views/CommunityDetailView.spec.js src/views/NotificationCenterView.spec.js src/views/HomeView.spec.js
```

## Targeted Admin User Status Verification

### Backend

```bash
cd backend
mvn -q "-Dtest=AdminUserControllerTests,AuthControllerTests,UserControllerTests,HomeControllerTests,HomeServiceTests" test
```

### Frontend

```bash
cd frontend
npx vitest run src/views/admin/AdminUsersView.spec.js src/components/NavBar.spec.js
```

## Targeted Admin Dashboard Verification

### Backend

```bash
cd backend
mvn -q "-Dtest=AdminDashboardControllerTests,HomeServiceTests,HomeControllerTests" test
```

### Frontend

```bash
cd frontend
npx vitest run src/views/admin/AdminDashboardView.spec.js src/views/HomeView.spec.js src/components/NavBar.spec.js
```

## Targeted Admin Job Management Verification

### Backend

```bash
cd backend
mvn -q "-Dtest=AdminJobControllerTests,AdminJobImportControllerTests,AdminJobSyncControllerTests,JobImportCsvParserTests,JobBatchImportServiceTests,ThirdPartyJobSyncServiceTests,JobControllerTests" test
```

### Frontend

```bash
cd frontend
npx vitest run src/views/admin/AdminJobManageView.spec.js src/components/NavBar.spec.js
```

## Targeted Admin Verification Review Verification

### Backend

```bash
cd backend
mvn -q -Dtest=AdminVerificationControllerTests test
```

### Frontend

```bash
cd frontend
npx vitest run src/views/admin/AdminVerificationReviewView.spec.js
```

## Targeted Admin Community Moderation Verification

### Backend

```bash
cd backend
mvn -q -Dtest=AdminCommunityControllerTests test
```

### Frontend

```bash
cd frontend
npx vitest run src/views/admin/AdminCommunityManageView.spec.js
```

## Targeted Foundation User Flow Verification

### Backend

```bash
cd backend
mvn -q "-Dtest=AuthControllerTests,HomeControllerTests,UserControllerTests,VerificationControllerTests,NotificationControllerTests" test
```

### Frontend

```bash
cd frontend
npx vitest run src/App.spec.js src/views/LoginView.spec.js src/views/HomeView.spec.js src/views/ProfileView.spec.js src/views/NotificationCenterView.spec.js
```

## Targeted Resource Lifecycle Verification

### Backend

```bash
cd backend
mvn -q "-Dtest=ResourceControllerTests,AdminResourceControllerTests" test
```

### Frontend

```bash
cd frontend
npx vitest run src/views/ResourcesListView.spec.js src/views/ResourceUploadView.spec.js src/views/ResourceEditView.spec.js src/views/ResourceDetailView.spec.js src/views/ProfileResourcesView.spec.js src/views/admin/AdminResourceManageView.spec.js
```

## Targeted Jobs Browsing Verification

### Backend

```bash
cd backend
mvn -q -Dtest=JobControllerTests test
```

### Frontend

```bash
cd frontend
npx vitest run src/views/JobsListView.spec.js src/views/JobDetailView.spec.js src/views/ProfileFavoritesView.spec.js
```

## Targeted Community Feed Verification

### Backend

```bash
cd backend
mvn -q -Dtest=CommunityControllerTests test
```

### Frontend

```bash
cd frontend
npx vitest run src/views/CommunityListView.spec.js src/views/CommunityCreateView.spec.js src/views/CommunityDetailView.spec.js src/views/ProfilePostsView.spec.js src/views/ProfileFavoritesView.spec.js
```

## Targeted Resource Preview Verification

### Backend

```bash
cd backend
mvn -q "-Dtest=ResourceControllerTests,AdminResourceControllerTests,ResourcePreviewServiceTests,ApachePoiPptxPreviewGeneratorTests,SofficeDocxPreviewGeneratorTests" test
```

### Frontend

```bash
cd frontend
npx vitest run src/components/ResourceZipPreviewPanel.spec.js src/views/ResourceDetailView.spec.js src/views/ProfileResourcesView.spec.js src/views/admin/AdminResourceManageView.spec.js
```

## Targeted Unified Search Verification

### Backend

```bash
cd backend
mvn -q -Dtest=SearchControllerTests,DiscoverControllerTests,HomeControllerTests,HomeServiceTests test
```

### Frontend

```bash
cd frontend
npx vitest run src/views/SearchView.spec.js src/views/DiscoverView.spec.js src/views/HomeView.spec.js src/components/NavBar.spec.js
```

## Targeted Decision Support Verification

### Backend

```bash
cd backend
mvn -q "-Dtest=DecisionAssessmentServiceTests,DecisionAssessmentControllerTests,DecisionTimelineServiceTests,DecisionTimelineControllerTests,DecisionSchoolServiceTests,DecisionSchoolControllerTests,HomeServiceTests,HomeControllerTests" test
```

### Frontend

```bash
cd frontend
npx vitest run src/views/AssessmentView.spec.js src/views/TimelineView.spec.js src/views/SchoolCompareView.spec.js src/views/HomeView.spec.js
```

## Targeted Decision Analytics Verification

### Backend

```bash
cd backend
mvn -q "-Dtest=AnalyticsServiceTests,AnalyticsControllerTests,HomeServiceTests,HomeControllerTests" test
```

### Frontend

```bash
cd frontend
npm run test -- src/views/AnalyticsView.spec.js src/views/HomeView.spec.js
```

## Targeted Job Application And Resume Workflow Verification

### Backend

```bash
cd backend
mvn -q "-Dtest=ResumeControllerTests,JobApplicationControllerTests,AdminJobApplicationControllerTests,JobControllerTests" test
```

### Frontend

```bash
cd frontend
npx vitest run src/views/ProfileResumesView.spec.js src/views/ProfileApplicationsView.spec.js src/views/JobDetailView.spec.js src/views/admin/AdminApplicationsView.spec.js src/views/ProfileView.spec.js src/components/NavBar.spec.js
```
