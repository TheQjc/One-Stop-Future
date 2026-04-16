# One-Stop Future

Current repo status: `Phase A foundation + Phase B community + Phase C jobs + Phase D resource library first slice`.

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
- resource download / favorite / unfavorite
- my resources / resource favorites
- admin verification review
- admin community moderation
- admin jobs create / edit / publish / offline / delete
- admin resource publish / reject / offline review workspace

Explicitly not implemented yet:

- batch job import
- third-party job sync
- in-site application / resume workflow
- unified search
- recommendation / hot ranking
- MinIO resource storage
- online preview, chunk upload, or resume upload
- resource edit / resubmit after rejection

## Project Structure

- `backend/`: Spring Boot 3, Spring Security, MyBatis-Plus, JWT
- `frontend/`: Vue 3, Pinia, Vue Router, Axios, Vite, Vitest
- `docs/superpowers/`: requirements, specs, plans
- `.local-storage/resources/`: local resource file storage in the `local` profile

## Local Run

### Backend

```bash
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

Notes:

- local development must use the `local` profile
- `local` uses embedded H2, seeded demo data, and local filesystem resource storage
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
- `/community`
- `/community/:id`
- `/community/create`
- `/jobs`
- `/jobs/:id`
- `/resources`
- `/resources/:id`
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

## Permissions

Guest:

- can browse home, community, jobs, and published resources
- cannot create content, save favorites, or download resource files

Authenticated user:

- can create community posts
- can comment / like / favorite community posts
- can favorite jobs
- can upload, favorite, unfavorite, and download published resources
- can view profile favorites for `POST`, `JOB`, and `RESOURCE`
- can view read-only `/profile/resources`

Admin:

- can review verification applications
- can moderate community posts
- can maintain job cards
- can review resources through publish / reject / offline actions

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
4. As guest, open `/resources/:id` and confirm the download action is blocked by login.
5. Log in as a normal user and upload a resource from `/resources/upload`.
6. Open `/profile/resources` and confirm the new file appears as `PENDING`.
7. Open `/profile/favorites` and switch between `POST`, `JOB`, and `RESOURCE`.
8. Log in as admin and open `/admin/resources`.
9. Publish the pending resource and confirm it appears in the public `/resources` list.
10. Favorite and download a published resource as a normal user.
