# One-Stop Future

Current repo status: `Phase A foundation + Phase B community + Phase C jobs + Phase D resource library first slice + Phase E unified search first slice + Phase F discover ranking first slice + Phase G resource lifecycle completion first slice + Phase H resource preview expansion first slice`.

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
- unified search across published posts / jobs / resources
- discover board across published posts / jobs / resources
- homepage discover preview with weekly public picks
- PDF inline preview for visible resources
- PPTX inline preview via cached PDF conversion
- ZIP directory-tree preview for visible resources

Explicitly not implemented yet:

- batch job import
- third-party job sync
- in-site application / resume workflow
- MinIO resource storage
- DOCX online preview
- version history, chunk upload, or resume upload

## Project Structure

- `backend/`: Spring Boot 3, Spring Security, MyBatis-Plus, JWT
- `frontend/`: Vue 3, Pinia, Vue Router, Axios, Vite, Vitest
- `docs/superpowers/`: requirements, specs, plans
- `backend/.local-storage/resources/`: default local resource file storage in the `local` profile
- `backend/.local-storage/previews/`: default cached PPTX-to-PDF and ZIP preview artifacts in the `local` profile
  - current Phase H behavior invalidates preview cache by fingerprinting and writing a new artifact; old preview artifacts are not garbage-collected automatically

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

- frontend address: `http://127.0.0.1:5173`
- Vite proxies `/api/**` to `http://127.0.0.1:8080`
- current recommendation is local backend + local frontend, not Docker

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

## Permissions

Guest:

- can browse home, community, jobs, and published resources
- can use unified search for published posts, jobs, and resources
- can browse the public discover board
- can preview published PDF resources inline
- can preview published PPTX resources inline as converted PDF
- can preview published ZIP resources as directory trees
- cannot create content, save favorites, or download resource files

Authenticated user:

- can create community posts
- can comment / like / favorite community posts
- can favorite jobs
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
5. As guest, open a published ZIP `/resources/:id` and confirm `Preview Contents` loads a directory tree inline.
6. As guest, confirm the download action is still blocked by login.
7. Log in as the normal user `13800000001` and upload a resource from `/resources/upload`.
8. Open `/profile/resources` and confirm the new file appears as `PENDING`.
9. Log in as the admin `13800000000` and open `/admin/resources`.
10. Reject a pending resource with a clear review note.
11. Log back in as the owner, open `/profile/resources`, click `Edit And Resubmit`, revise metadata, and submit without replacing the file.
12. Repeat the resubmission flow with a PDF replacement file and confirm the record returns to `PENDING`.
13. As the owner, preview an unpublished visible PDF or PPTX from `/profile/resources` or `/resources/:id`.
14. As admin, confirm preview is shown for visible PDF / PPTX rows and `Preview Contents` is shown for ZIP rows in `/admin/resources`.
15. Publish the pending resource and confirm it appears in the public `/resources` list.
16. Favorite and download a published resource as a normal user.
17. Open `/profile/favorites` and switch between `POST`, `JOB`, and `RESOURCE`.
18. Use the homepage search box or `/search` to search `resume`.
19. Switch `ALL / POST / JOB / RESOURCE` and `RELEVANCE / LATEST`.
20. Refresh `/search` and confirm the search state stays in the URL.
21. Open `/discover` as a guest and confirm the page loads a ranked public board.
22. Switch discover `ALL / POST / JOB / RESOURCE` and `WEEK / ALL`.
23. Refresh `/discover?tab=JOB&period=ALL` and confirm the state stays in the URL.
24. Return to `/` and confirm the homepage discover preview shows items or a graceful empty state.

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
