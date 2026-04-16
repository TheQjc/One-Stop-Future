# One-Stop Future

Current repo status: `Phase A foundation + Phase B community + Phase C jobs first slice`.

## Current Scope

Implemented now:

- phone-code register / login
- independent aggregation home
- profile center and student verification apply flow
- notification center
- community list / detail / create / comment / like / favorite
- my posts / my favorites
- admin verification review
- admin community moderation
- jobs list / detail / filters / source jump
- job favorite / unfavorite
- admin jobs create / edit / publish / offline / delete

Explicitly not implemented yet:

- batch job import
- third-party job sync
- in-site application / resume workflow
- resource library
- unified search
- recommendation / hot ranking

## Project Structure

- `backend/`: Spring Boot 3, Spring Security, MyBatis-Plus, JWT
- `frontend/`: Vue 3, Pinia, Vue Router, Axios, Vite, Vitest
- `docs/superpowers/`: requirements, specs, plans

## Local Run

### Backend

```bash
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

Notes:

- local development must use the `local` profile
- `local` uses embedded H2 and local seed data
- default `application.yml` still contains remote MySQL settings, but SQL init is disabled there
- local backend address: `http://127.0.0.1:8080`

### Frontend

```bash
cd frontend
npm install
npm run dev
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
npm run test -- --run
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
- `/profile`
- `/profile/posts`
- `/profile/favorites`
- `/notifications`

Admin:

- `/admin/verifications`
- `/admin/community`
- `/admin/jobs`

## Permissions

Guest:

- can browse home, community, and jobs
- cannot create content or save favorites

Authenticated user:

- can create community posts
- can comment / like / favorite community posts
- can favorite jobs
- can view profile favorites for both `POST` and `JOB`

Admin:

- can review verification applications
- can moderate community posts
- can maintain job cards

## Current Data Shapes

Community favorite type:

- `POST`

Job favorite type:

- `JOB`

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

## Manual Smoke Checklist

1. Start backend with `mvn spring-boot:run "-Dspring-boot.run.profiles=local"`.
2. Start frontend with `npm run dev`.
3. As guest, open `/community` and `/jobs`.
4. As guest, open `/jobs/:id` and confirm the source link is visible.
5. Log in as a normal user and favorite a published job.
6. Open `/profile/favorites` and switch between `POST` and `JOB`.
7. Log in as admin and open `/admin/jobs`.
8. Create a draft, publish it, confirm it appears in `/jobs`, then offline it.
9. Confirm an offlined job no longer opens for a guest on `/jobs/:id`.
