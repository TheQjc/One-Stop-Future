# One-Stop Future

Current repo status: `Phase A foundation + Phase B community + Phase C jobs + Phase D resource library first slice + Phase E unified search first slice + Phase F discover ranking first slice + Phase G resource lifecycle completion first slice + Phase H resource preview expansion first slice + Phase J historical local resource MinIO migration first slice + Phase K decision support first slice`.

## Current Scope

Implemented now:

- phone-code register / login
- independent aggregation home
- profile center and student verification apply flow
- notification center
- community list / detail / create / comment / like / favorite
- my posts / my favorites
- jobs list / detail / filters / source jump
- job favorite / unfavorite
- public resource list / detail / upload
- resource preview / download / favorite / unfavorite
- my resources / resource favorites / rejected edit-resubmit
- admin verification review
- admin community moderation
- admin jobs create / edit / publish / offline / delete
- admin resource publish / reject / offline review workspace
- admin historical local-resource MinIO migration with dry-run and bounded batch execution
- unified search across published posts / jobs / resources
- discover board across published posts / jobs / resources
- homepage discover preview with weekly public picks
- authenticated decision assessment questions / submit / latest result
- authenticated direction timeline with stable anchor-date fallback
- public school candidate listing and 2-4 school comparison for `EXAM` / `ABROAD`
- authenticated homepage assessment entry activation
- PDF inline preview for visible resources
- PPTX inline preview via cached PDF conversion
- ZIP directory-tree preview for visible resources

Explicitly not implemented yet:

- batch job import
- third-party job sync
- in-site application / resume workflow
- MinIO-backed preview artifact storage
- DOCX online preview
- decision analytics page
- version history, chunk upload, or resume upload

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
- `/timeline`
- `/schools/compare`
- `/profile`
- `/profile/posts`
- `/profile/favorites`
- `/profile/resources`
- `/notifications`

Admin:

- `/admin/verifications`
- `/admin/community`
- `/admin/jobs`
- `/admin/resources`

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

## Decision Support

Backend endpoints:

- `GET /api/decision/assessment/questions`
- `POST /api/decision/assessment/submissions`
- `GET /api/decision/assessment/latest`
- `GET /api/decision/timeline?track=CAREER|EXAM|ABROAD&anchorDate=YYYY-MM-DD`
- `GET /api/decision/schools?track=EXAM|ABROAD&keyword=...`
- `POST /api/decision/schools/compare`

Current decision-support scope:

- assessment is authenticated and backed by seeded backend-owned questions
- submit persists the latest assessment result and returns deterministic scores / ranking / next actions
- timeline is authenticated and uses explicit `anchorDate` first, otherwise the latest assessment session date
- timeline returns `assessmentRequired=true` when neither explicit anchor nor latest result exists
- school candidate list and compare are public read-only endpoints for `EXAM` and `ABROAD`
- school compare enforces `2-4` schools, preserves accepted request order, and returns explicit missing-value markers
- homepage `assessment` entry is live for authenticated users and `LOGIN_REQUIRED` for guests
- homepage `analytics` remains `COMING_SOON` in this phase

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
- can browse public school candidates and school comparison for `EXAM` / `ABROAD`
- can preview published PDF resources inline
- can preview published PPTX resources inline as converted PDF
- can preview published ZIP resources as directory trees
- cannot create content, save favorites, or download resource files

Authenticated user:

- can create community posts
- can comment / like / favorite community posts
- can favorite jobs
- can complete the decision assessment and view the latest result
- can open the direction timeline after assessment
- can upload resources
- can preview published PDFs, visible PPTX resources, and visible ZIP directory trees
- can favorite, unfavorite, and download published resources
- can edit and resubmit their own rejected resources from `/resources/:id/edit`
- can view profile favorites for `POST`, `JOB`, and `RESOURCE`
- can view `/profile/resources` with lifecycle actions

Admin:

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
3. As guest, open `/community`, `/jobs`, and `/resources`.
4. As guest, open a published PDF `/resources/:id` and confirm `Preview` works without login.
5. As guest, open a published PPTX `/resources/:id` and confirm `Preview` opens converted PDF output without login.
6. As guest, open a published ZIP `/resources/:id` and confirm `Preview Contents` loads a directory tree inline.
7. As guest, open a published DOCX `/resources/:id` and confirm no preview action is shown.
8. As guest, confirm the download action is still blocked by login.
9. Log in as the normal user `13800000001` and upload a resource from `/resources/upload`.
10. Open `/profile/resources` and confirm the new file appears as `PENDING`.
11. Log in as the admin `13800000000` and open `/admin/resources`.
12. Reject a pending resource with a clear review note.
13. Log back in as the owner, open `/profile/resources`, click `Edit And Resubmit`, revise metadata, and submit without replacing the file.
14. Repeat the resubmission flow with a PDF replacement file and confirm the record returns to `PENDING`.
15. Repeat the resubmission flow with a PPTX replacement file and confirm the next preview opens regenerated PDF output instead of stale cached content.
16. As the owner, preview an unpublished visible PDF or PPTX from `/profile/resources` or `/resources/:id`.
17. As admin, confirm preview is shown for visible PDF / PPTX rows and `Preview Contents` is shown for ZIP rows in `/admin/resources`.
18. Publish the pending resource and confirm it appears in the public `/resources` list.
19. Favorite and download a published resource as a normal user.
20. Open `/profile/favorites` and switch between `POST`, `JOB`, and `RESOURCE`.
21. Use the homepage search box or `/search` to search `resume`.
22. Switch `ALL / POST / JOB / RESOURCE` and `RELEVANCE / LATEST`.
23. Refresh `/search` and confirm the search state stays in the URL.
24. Open `/discover` as a guest and confirm the page loads a ranked public board.
25. Switch discover `ALL / POST / JOB / RESOURCE` and `WEEK / ALL`.
26. Refresh `/discover?tab=JOB&period=ALL` and confirm the state stays in the URL.
27. Return to `/` and confirm the homepage discover preview shows items or a graceful empty state.
28. Log in as `13800000001`, open `/assessment`, answer all questions, and submit one result.
29. Confirm the result renders a recommended track plus links into `/timeline` and `/schools/compare`.
30. Open `/timeline` and confirm it defaults to the recommended track and renders milestone cards.
31. Switch `/timeline` among `CAREER`, `EXAM`, and `ABROAD` and confirm the milestone list reloads.
32. Open `/schools/compare` as either guest or authenticated user, select `2-4` schools, and confirm compare table + chart region render.
33. Return to `/` and confirm `assessment` is live for the logged-in user while `analytics` still reads as coming soon.

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
