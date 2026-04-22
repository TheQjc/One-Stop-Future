# Phase Q Community Experience Post Structure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add optional structured experience-post fields on top of existing community posts, render them in create/list/detail flows, and give those posts a small discover/home recommendation boost required by `FR-COMMUNITY-002`.

**Architecture:** Extend the existing `t_community_post` data model instead of introducing a separate experience-post subsystem. Keep the community API surface inside the current controller/service layer, add a reusable nested `experience` DTO block for list/detail responses, and integrate the display-weight change only inside discover scoring so community/search ordering remains unchanged.

**Tech Stack:** Java 17, Spring Boot 3, Spring Security, MyBatis-Plus, H2 test profile, Vue 3, Vue Router, Axios, Vite, Vitest

---

## Planned File Structure

### Backend: Modify Existing

- Modify: `backend/src/main/resources/schema.sql`
- Modify: `backend/src/main/resources/data.sql`
- Modify: `backend/src/main/java/com/campus/entity/CommunityPost.java`
- Modify: `backend/src/main/java/com/campus/dto/CreateCommunityPostRequest.java`
- Modify: `backend/src/main/java/com/campus/dto/CommunityPostListResponse.java`
- Modify: `backend/src/main/java/com/campus/dto/CommunityPostDetailResponse.java`
- Modify: `backend/src/main/java/com/campus/service/CommunityService.java`
- Modify: `backend/src/main/java/com/campus/service/DiscoverService.java`
- Modify: `backend/src/test/java/com/campus/controller/CommunityControllerTests.java`
- Modify: `backend/src/test/java/com/campus/controller/DiscoverControllerTests.java`

### Frontend: Modify Existing

- Modify: `frontend/src/api/community.js`
- Modify: `frontend/src/components/CommunityPostCard.vue`
- Modify: `frontend/src/views/CommunityCreateView.vue`
- Modify: `frontend/src/views/CommunityDetailView.vue`
- Modify: `frontend/src/views/CommunityCreateView.spec.js`
- Modify: `frontend/src/views/CommunityDetailView.spec.js`
- Modify: `frontend/src/views/CommunityListView.spec.js`

### Repo Docs

- Modify: `README.md`

## Task 1: Extend the Backend Community Post Contract

**Files:**

- Modify: `backend/src/main/resources/schema.sql`
- Modify: `backend/src/main/resources/data.sql`
- Modify: `backend/src/main/java/com/campus/entity/CommunityPost.java`
- Modify: `backend/src/main/java/com/campus/dto/CreateCommunityPostRequest.java`
- Modify: `backend/src/main/java/com/campus/dto/CommunityPostListResponse.java`
- Modify: `backend/src/main/java/com/campus/dto/CommunityPostDetailResponse.java`
- Modify: `backend/src/main/java/com/campus/service/CommunityService.java`
- Modify: `backend/src/test/java/com/campus/controller/CommunityControllerTests.java`

- [x] **Step 1: Write failing backend controller coverage**

Add tests for:

- creating an experience post with structured fields
- list/detail responses returning the nested `experience` block
- legacy non-experience posts still returning `experience.enabled = false`

- [x] **Step 2: Run backend tests to confirm failure**

Run:

```powershell
cd backend
mvn -q -Dtest=CommunityControllerTests test
```

- [x] **Step 3: Implement the backend contract**

Add:

- new `t_community_post` columns for the experience flag and four structured fields
- fixture data with at least one seeded experience post
- entity fields in `CommunityPost`
- request fields in `CreateCommunityPostRequest`
- nested `experience` DTO blocks in list/detail responses
- create/list/detail mapping logic in `CommunityService`

- [x] **Step 4: Re-run backend tests**

Run:

```powershell
cd backend
mvn -q -Dtest=CommunityControllerTests test
```

- [x] **Step 5: Commit**

```bash
git add backend/src/main/resources/schema.sql backend/src/main/resources/data.sql backend/src/main/java/com/campus/entity/CommunityPost.java backend/src/main/java/com/campus/dto/CreateCommunityPostRequest.java backend/src/main/java/com/campus/dto/CommunityPostListResponse.java backend/src/main/java/com/campus/dto/CommunityPostDetailResponse.java backend/src/main/java/com/campus/service/CommunityService.java backend/src/test/java/com/campus/controller/CommunityControllerTests.java
git commit -m "feat: add community experience post structure"
```

## Task 2: Apply the Discover/Home Experience Boost

**Files:**

- Modify: `backend/src/main/java/com/campus/service/DiscoverService.java`
- Modify: `backend/src/test/java/com/campus/controller/DiscoverControllerTests.java`

- [x] **Step 1: Write failing discover ranking coverage**

Add a discover test proving that an experience post receives a higher ranking than a near-tied normal post because of the new deterministic bonus.

- [x] **Step 2: Run the discover tests**

Run:

```powershell
cd backend
mvn -q -Dtest=DiscoverControllerTests,HomeControllerTests test
```

- [x] **Step 3: Implement the ranking boost**

Update `DiscoverService` to:

- add a constant experience bonus when `isExperiencePost = true`
- adjust post `secondaryMeta` / `hotLabel` so experience posts are recognizable in discover preview

- [x] **Step 4: Re-run discover tests**

Run:

```powershell
cd backend
mvn -q -Dtest=DiscoverControllerTests,HomeControllerTests test
```

- [x] **Step 5: Commit**

```bash
git add backend/src/main/java/com/campus/service/DiscoverService.java backend/src/test/java/com/campus/controller/DiscoverControllerTests.java
git commit -m "feat: boost experience posts in discover"
```

## Task 3: Add Frontend Experience Post Create/List/Detail Support

**Files:**

- Modify: `frontend/src/api/community.js`
- Modify: `frontend/src/components/CommunityPostCard.vue`
- Modify: `frontend/src/views/CommunityCreateView.vue`
- Modify: `frontend/src/views/CommunityDetailView.vue`
- Modify: `frontend/src/views/CommunityCreateView.spec.js`
- Modify: `frontend/src/views/CommunityDetailView.spec.js`
- Modify: `frontend/src/views/CommunityListView.spec.js`

- [x] **Step 1: Write failing frontend tests**

Cover:

- toggling experience-post mode on the create page
- submitting the structured payload
- rendering the experience badge / summary on the list card
- rendering the experience summary block on the detail page

- [x] **Step 2: Run frontend tests to confirm failure**

Run:

```powershell
cd frontend
npx vitest run src/views/CommunityCreateView.spec.js src/views/CommunityDetailView.spec.js src/views/CommunityListView.spec.js
```

- [x] **Step 3: Implement the frontend flow**

Add:

- new request payload fields in `createCommunityPost`
- an optional experience section in `CommunityCreateView.vue`
- experience badge and structured summary in `CommunityPostCard.vue`
- experience summary panel in `CommunityDetailView.vue`

- [x] **Step 4: Re-run frontend tests**

Run:

```powershell
cd frontend
npx vitest run src/views/CommunityCreateView.spec.js src/views/CommunityDetailView.spec.js src/views/CommunityListView.spec.js
```

- [x] **Step 5: Commit**

```bash
git add frontend/src/api/community.js frontend/src/components/CommunityPostCard.vue frontend/src/views/CommunityCreateView.vue frontend/src/views/CommunityDetailView.vue frontend/src/views/CommunityCreateView.spec.js frontend/src/views/CommunityDetailView.spec.js frontend/src/views/CommunityListView.spec.js
git commit -m "feat: add community experience post ui"
```

## Task 4: Update Docs and Verify

**Files:**

- Modify: `README.md`

- [x] **Step 1: Update README**

Document:

- the new community experience post fields
- discover/home recommendation boost behavior
- the fact that this is an optional overlay on normal community posts

- [x] **Step 2: Run targeted verification**

Run:

```powershell
cd backend
mvn -q -Dtest=CommunityControllerTests,DiscoverControllerTests,HomeControllerTests test
```

```powershell
cd frontend
npx vitest run src/views/CommunityCreateView.spec.js src/views/CommunityDetailView.spec.js src/views/CommunityListView.spec.js
npx vitest run src/views/HomeView.spec.js src/views/DiscoverView.spec.js
npm run build
```

- [x] **Step 3: Commit**

```bash
git add README.md
git commit -m "docs: add community experience post notes"
```
