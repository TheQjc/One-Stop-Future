# One-Stop Future

Current repo status: `Phase A foundation + Phase B community + Phase C jobs + Phase D resource library first slice + Phase E unified search first slice + Phase F discover ranking first slice + Phase G resource lifecycle completion first slice + Phase H resource preview expansion first slice + Phase I MinIO raw resource storage first slice + Phase J historical local resource MinIO migration first slice + Phase K decision support first slice + Phase L decision analytics first slice + Phase M admin dashboard first slice + Phase N job application and resume workflow first slice + Phase O admin user status management first slice + Phase P community hot ranking first slice`.

## Current Scope

Implemented now:

- phone-code register / login
- account status control with banned-login enforcement
- independent aggregation home
- profile center and student verification apply flow
- notification center
- community list / detail / create / comment / like / favorite
- community hot ranking with `DAY / WEEK / ALL` boards inside `/community`
- my posts / my favorites
- jobs list / detail / filters / source jump
- job favorite / unfavorite
- resume library upload / list / download / delete
- in-platform one-time job apply with resume snapshot
- my applications history
- admin applications read-only workbench with snapshot download
- public resource list / detail / upload
- resource preview / download / favorite / unfavorite
- my resources / resource favorites / rejected edit-resubmit
- admin verification review
- admin community moderation
- admin jobs create / edit / publish / offline / delete
- admin resource publish / reject / offline review workspace
- MinIO-backed raw resource storage for non-`local` runtimes while preview artifacts remain local in this phase
- admin historical local-resource MinIO migration with dry-run and bounded batch execution
- admin dashboard read-only summary overview with handoff to existing workbenches
- admin user status workbench with ban / restore controls for non-admin accounts
- unified search across published posts / jobs / resources
- discover board across published posts / jobs / resources
- homepage discover preview with weekly public picks
- authenticated decision assessment questions / submit / latest result
- authenticated direction timeline with stable anchor-date fallback
- public school candidate listing and 2-4 school comparison for `EXAM` / `ABROAD`
- authenticated homepage assessment entry activation
- public decision analytics desk at `/analytics` with `7D / 30D` overview, trend, and direction-mix views
- authenticated personal decision analytics snapshot / history / next actions on the same `/analytics` page
- homepage analytics entry activation for both guests and authenticated users
- PDF inline preview for visible resources
- PPTX inline preview via cached PDF conversion
- ZIP directory-tree preview for visible resources

Explicitly not implemented yet:

- batch job import
- third-party job sync
- MinIO-backed preview artifact storage
- DOCX online preview
- full admin operations dashboards, DAU / funnel metrics, or exportable analytics reports
- version history, chunk upload, resume rename / replace, or online resume preview

## Project Structure

- `backend/`: Spring Boot 3, Spring Security, MyBatis-Plus, JWT
- `frontend/`: Vue 3, Pinia, Vue Router, Axios, Vite, Vitest
- `docs/superpowers/`: requirements, specs, plans
- `backend/.local-storage/resources/`: default local raw resource storage in the `local` profile
- `backend/.local-storage/previews/`: default cached PPTX-to-PDF and ZIP preview artifacts in the `local` profile; still local even when raw resource storage uses MinIO
  - current Phase H behavior invalidates preview cache by fingerprinting and writing a new artifact; old preview artifacts are not garbage-collected automatically
  - to reset derived preview state during local development, stop the backend and delete `backend/.local-storage/previews/`

## Local Run

### Backend

```bash
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

Notes:

- local development must use the `local` profile
- `local` uses embedded H2, seeded demo data, and local filesystem resource storage
- current defaults are relative paths; with `cd backend` they resolve to `backend/.local-storage/resources/` and `backend/.local-storage/previews/`
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
- current recommendation for day-to-day development is still the local backend + local frontend flow above
- switching an existing local-file database to MinIO is a manual admin-triggered backend migration flow in this phase, not an automatic runtime cutover

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

## Job Application And Resume Workflow

Backend endpoints:

- `GET /api/resumes/mine`
- `POST /api/resumes`
- `GET /api/resumes/{id}/download`
- `DELETE /api/resumes/{id}`
- `POST /api/jobs/{id}/apply`
- `GET /api/applications/mine`
- `GET /api/admin/applications`
- `GET /api/admin/applications/{id}/resume/download`

Current Phase N scope:

- authenticated users can keep multiple resume files in `/profile/resumes`
- supported resume formats are `PDF`, `DOC`, and `DOCX`
- published job detail pages keep the external `Source Link` and also expose in-platform apply
- one user can apply to the same job at most once
- each application stores an immutable resume snapshot so deleting the live resume does not break historical download
- `/profile/applications` is applicant-facing and read-only
- `/admin/applications` is admin-only and read-only

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
- preview artifacts remain out of scope; cached preview files are not migrated to MinIO in this phase

## Permissions

Guest:

- can browse home, community, jobs, and published resources
- can use unified search for published posts, jobs, and resources
- can browse the public discover board
- can open `/analytics` and read the public decision analytics board
- can browse public school candidates and school comparison for `EXAM` / `ABROAD`
- can preview published PDF resources inline
- can preview published PPTX resources inline as converted PDF
- can preview published ZIP resources as directory trees
- cannot create content, save favorites, or download resource files

Authenticated user:

- can create community posts
- can comment / like / favorite community posts
- can favorite jobs
- can manage their own resume library at `/profile/resumes`
- can apply to a published job once with one selected resume
- can review their application history at `/profile/applications`
- can complete the decision assessment and view the latest result
- can open `/analytics` and view personal snapshot / history / next actions when available
- can open the direction timeline after assessment
- can upload resources
- can preview published PDFs, visible PPTX resources, and visible ZIP directory trees
- can favorite, unfavorite, and download published resources
- can edit and resubmit their own rejected resources from `/resources/:id/edit`
- can view profile favorites for `POST`, `JOB`, and `RESOURCE`
- can view `/profile/resources` with lifecycle actions

Admin:

- can open `/admin/dashboard` for a read-only overview and use it to enter existing admin workbenches
- can open `/admin/applications` and download application snapshot resumes
- can review verification applications
- can moderate community posts
- can maintain job cards
- can review resources through publish / reject / offline actions
- can preview PDF and PPTX resources inline and ZIP contents from the admin resource board

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
5. As guest, open `/jobs` and `/resources`.
6. As guest, open a published PDF `/resources/:id` and confirm `Preview` works without login.
7. As guest, open a published PPTX `/resources/:id` and confirm `Preview` opens converted PDF output without login.
8. As guest, open a published ZIP `/resources/:id` and confirm `Preview Contents` loads a directory tree inline.
9. As guest, open a published DOCX `/resources/:id` and confirm no preview action is shown.
10. As guest, confirm the download action is still blocked by login.
11. Log in as the normal user `13800000001` and upload a resource from `/resources/upload`.
12. Open `/profile/resources` and confirm the new file appears as `PENDING`.
13. Log in as the admin `13800000000` and open `/admin/resources`.
14. Reject a pending resource with a clear review note.
15. Log back in as the owner, open `/profile/resources`, click `Edit And Resubmit`, revise metadata, and submit without replacing the file.
16. Repeat the resubmission flow with a PDF replacement file and confirm the record returns to `PENDING`.
17. Repeat the resubmission flow with a PPTX replacement file and confirm the next preview opens regenerated PDF output instead of stale cached content.
18. As the owner, preview an unpublished visible PDF or PPTX from `/profile/resources` or `/resources/:id`.
19. As admin, confirm preview is shown for visible PDF / PPTX rows and `Preview Contents` is shown for ZIP rows in `/admin/resources`.
20. Publish the pending resource and confirm it appears in the public `/resources` list.
21. Favorite and download a published resource as a normal user.
22. Open `/profile/favorites` and switch between `POST`, `JOB`, and `RESOURCE`.
23. Use the homepage search box or `/search` to search `resume`.
24. Switch `ALL / POST / JOB / RESOURCE` and `RELEVANCE / LATEST`.
25. Refresh `/search` and confirm the search state stays in the URL.
26. Open `/discover` as a guest and confirm the page loads a ranked public board.
27. Switch discover `ALL / POST / JOB / RESOURCE` and `WEEK / ALL`.
28. Refresh `/discover?tab=JOB&period=ALL` and confirm the state stays in the URL.
29. Return to `/` and confirm the homepage discover preview shows items or a graceful empty state.
30. Log in as `13800000001`, open `/assessment`, answer all questions, and submit one result.
31. Confirm the result renders a recommended track plus links into `/timeline` and `/schools/compare`.
32. Open `/timeline` and confirm it defaults to the recommended track and renders milestone cards.
33. Switch `/timeline` among `CAREER`, `EXAM`, and `ABROAD` and confirm the milestone list reloads.
34. Open `/schools/compare` as either guest or authenticated user, select `2-4` schools, and confirm compare table + chart region render.
35. As guest, open `/analytics` and confirm public overview, trend cards, and decision mix render.
36. Log in as `13800000001`, open `/analytics`, and confirm either the personal snapshot/history or the assessment CTA renders.
37. Return to `/` and confirm both `assessment` and `analytics` entries are live.
38. Log in as the admin `13800000000`, open `/admin/dashboard`, and confirm the page renders a read-only overview.
39. From both the homepage admin entry and the main nav admin entry, confirm navigation lands on `/admin/dashboard`.
40. From `/admin/dashboard`, confirm the handoff links route into the existing admin workbenches.
41. Log in as `13800000001`, open `/profile/resumes`, and upload two resume files.
42. Open `/jobs/1`, confirm `Source Link` is still present, and submit one in-platform application with a selected resume.
43. Open `/profile/applications` and confirm the new record shows job title, company, city, status, submitted time, and the resume snapshot title.
44. Delete the original live resume from `/profile/resumes`, return to `/profile/applications`, and confirm the application record still renders.
45. Log in as admin `13800000000`, open `/admin/applications`, and confirm the record renders with applicant info and resume snapshot file name.
46. Download the snapshot resume from `/admin/applications` and confirm the file is still available after the live resume deletion.
47. Return to `/jobs/1` as the applicant and confirm the page still shows the applied state.
48. Log in as admin `13800000000`, open `/admin/users`, and confirm the list shows total, active, banned, and verified counts.
49. Ban the normal user `13800000001` from `/admin/users` and confirm the row status becomes `BANNED`.
50. Try to open `/profile` or log in again as `13800000001` and confirm the app blocks the banned account with an explicit error.
51. Restore `13800000001` from `/admin/users` and confirm the user can log in again.

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

## Targeted Resource Preview Verification

### Backend

```bash
cd backend
mvn -q "-Dtest=ResourceControllerTests,AdminResourceControllerTests,ResourcePreviewServiceTests,ApachePoiPptxPreviewGeneratorTests" test
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
