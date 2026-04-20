# Phase P Community Hot Ranking Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the missing community hot-post ranking capability required by `FR-COMMUNITY-005`, with public `DAY / WEEK / ALL` boards and a `/community` page hot-ranking block.

**Architecture:** Keep ranking logic inside the existing `community` backend boundary instead of routing through discover. Reuse current post counters and author verification status for score calculation, expose a small public read-only `/api/community/hot` contract, and render the board inline on the existing community page without adding a new route.

**Tech Stack:** Java 17, Spring Boot 3, Spring Security, MyBatis-Plus, H2 test profile, Vue 3, Vue Router, Axios, Vite, Vitest

---

## Planned File Structure

### Backend: Modify Existing

- Modify: `backend/src/main/java/com/campus/controller/CommunityController.java`
- Modify: `backend/src/main/java/com/campus/service/CommunityService.java`
- Modify: `backend/src/test/java/com/campus/controller/CommunityControllerTests.java`

### Backend: Create

- Create: `backend/src/main/java/com/campus/common/CommunityHotPeriodType.java`
- Create: `backend/src/main/java/com/campus/dto/CommunityHotPostListResponse.java`

### Frontend: Modify Existing

- Modify: `frontend/src/api/community.js`
- Modify: `frontend/src/views/CommunityListView.vue`
- Modify: `frontend/src/views/CommunityListView.spec.js`

### Repo Docs

- Modify: `README.md`

## Task 1: Add the Backend Community Hot Ranking Contract

**Files:**

- Create: `backend/src/main/java/com/campus/common/CommunityHotPeriodType.java`
- Create: `backend/src/main/java/com/campus/dto/CommunityHotPostListResponse.java`
- Modify: `backend/src/main/java/com/campus/controller/CommunityController.java`
- Modify: `backend/src/main/java/com/campus/service/CommunityService.java`
- Modify: `backend/src/test/java/com/campus/controller/CommunityControllerTests.java`

- [ ] **Step 1: Write failing controller coverage**

Add controller tests for:

- default `GET /api/community/hot`
- `period=DAY`
- invalid `period`
- ranking order with adjusted counters

- [ ] **Step 2: Run the failing backend tests**

Run:

```powershell
cd backend
mvn -q -Dtest=CommunityControllerTests test
```

- [ ] **Step 3: Implement the public hot endpoint**

Add:

- enum `CommunityHotPeriodType { DAY, WEEK, ALL }`
- DTO `CommunityHotPostListResponse`
- controller method `GET /api/community/hot`
- service method that:
  - filters published posts by rolling publish window
  - computes `hotScore`
  - sorts by `hotScore DESC, createdAt DESC, id DESC`
  - returns a capped list

- [ ] **Step 4: Re-run backend tests**

Run:

```powershell
cd backend
mvn -q -Dtest=CommunityControllerTests test
```

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/campus/common/CommunityHotPeriodType.java backend/src/main/java/com/campus/dto/CommunityHotPostListResponse.java backend/src/main/java/com/campus/controller/CommunityController.java backend/src/main/java/com/campus/service/CommunityService.java backend/src/test/java/com/campus/controller/CommunityControllerTests.java
git commit -m "feat: add community hot ranking api"
```

## Task 2: Add the Community Page Hot Board

**Files:**

- Modify: `frontend/src/api/community.js`
- Modify: `frontend/src/views/CommunityListView.vue`
- Modify: `frontend/src/views/CommunityListView.spec.js`

- [ ] **Step 1: Write failing frontend tests**

Cover:

- hot board loads on mount
- period switching triggers a refetch
- ranking cards render with returned titles
- error state renders retry action

- [ ] **Step 2: Run the failing frontend tests**

Run:

```powershell
cd frontend
npx vitest run src/views/CommunityListView.spec.js
```

- [ ] **Step 3: Implement the frontend board**

Add:

- `getCommunityHotPosts(params)` in `frontend/src/api/community.js`
- hot board state inside `CommunityListView.vue`
- `DAY / WEEK / ALL` chips
- loading, empty, and error states
- ranked card rendering above the existing latest-post list

- [ ] **Step 4: Re-run frontend tests**

Run:

```powershell
cd frontend
npx vitest run src/views/CommunityListView.spec.js
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/api/community.js frontend/src/views/CommunityListView.vue frontend/src/views/CommunityListView.spec.js
git commit -m "feat: add community hot board ui"
```

## Task 3: Update Docs and Verify

**Files:**

- Modify: `README.md`

- [ ] **Step 1: Update README**

Document:

- `GET /api/community/hot`
- supported `DAY / WEEK / ALL`
- `/community` hot board
- current rolling-window semantics

- [ ] **Step 2: Run targeted verification**

Run:

```powershell
cd backend
mvn -q -Dtest=CommunityControllerTests test
```

```powershell
cd frontend
npx vitest run src/views/CommunityListView.spec.js
npm run build
```

- [ ] **Step 3: Commit**

```bash
git add README.md
git commit -m "docs: add community hot ranking notes"
```
