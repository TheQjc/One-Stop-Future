# Phase R Community Threaded Replies Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add one-level threaded replies under community comments, keep first-level commenting unchanged, and notify the replied-to comment author when another user posts a reply.

**Architecture:** Extend the existing `t_community_comment` table with parent/target metadata instead of introducing a separate reply subsystem. Reuse `CreateCommunityCommentRequest` for both top-level comments and replies, reshape the community detail DTO into top-level comments with nested reply arrays, and trigger the new notification only from the reply-creation path.

**Tech Stack:** Java 17, Spring Boot 3, Spring Security, MyBatis-Plus, H2 test profile, Vue 3, Vue Router, Axios, Vite, Vitest

---

## Planned File Structure

### Backend: Modify Existing

- Modify: `backend/src/main/resources/schema.sql`
- Modify: `backend/src/main/resources/data.sql`
- Modify: `backend/src/main/java/com/campus/entity/CommunityComment.java`
- Modify: `backend/src/main/java/com/campus/dto/CommunityPostDetailResponse.java`
- Modify: `backend/src/main/java/com/campus/common/NotificationType.java`
- Modify: `backend/src/main/java/com/campus/controller/CommunityController.java`
- Modify: `backend/src/main/java/com/campus/service/CommunityService.java`
- Modify: `backend/src/test/java/com/campus/controller/CommunityControllerTests.java`
- Modify: `backend/src/test/java/com/campus/controller/NotificationControllerTests.java`

### Frontend: Modify Existing

- Modify: `frontend/src/api/community.js`
- Modify: `frontend/src/components/CommunityCommentList.vue`
- Modify: `frontend/src/views/CommunityDetailView.vue`
- Modify: `frontend/src/views/CommunityDetailView.spec.js`
- Modify: `frontend/src/views/NotificationCenterView.vue`
- Modify: `frontend/src/views/NotificationCenterView.spec.js`

## Task 1: Extend Community Comment Storage And Reply API

**Files:**

- Modify: `backend/src/main/resources/schema.sql`
- Modify: `backend/src/main/resources/data.sql`
- Modify: `backend/src/main/java/com/campus/entity/CommunityComment.java`
- Modify: `backend/src/main/java/com/campus/dto/CommunityPostDetailResponse.java`
- Modify: `backend/src/main/java/com/campus/controller/CommunityController.java`
- Modify: `backend/src/main/java/com/campus/service/CommunityService.java`
- Test: `backend/src/test/java/com/campus/controller/CommunityControllerTests.java`

- [ ] **Step 1: Write failing backend coverage for nested replies**

Add controller tests for:

- guest detail responses returning top-level comments with a nested `replies` array
- authenticated users still creating first-level comments through `POST /api/community/posts/{id}/comments`
- authenticated users creating replies through `POST /api/community/comments/{id}/replies`
- rejecting reply creation when the target comment is already a reply

- [ ] **Step 2: Run the backend community tests to confirm failure**

Run:

```powershell
cd backend
mvn -q -Dtest=CommunityControllerTests test
```

- [ ] **Step 3: Implement the threaded reply backend contract**

Add:

- `parent_comment_id` and `reply_to_user_id` columns to `t_community_comment`
- seeded top-level and reply fixture rows in `data.sql` so guest detail reads expose the new response shape
- matching entity fields in `CommunityComment`
- a new `POST /api/community/comments/{id}/replies` controller route that reuses `CreateCommunityCommentRequest`
- `CommunityService#createReply(...)` validation that the target comment exists, belongs to a published post, and is itself top-level
- nested `CommunityPostDetailResponse` comment/reply records, including `replyToUserId` and `replyToUserNickname` on replies
- ordered detail mapping that groups replies under their owning top-level comment
- unchanged `commentCount` semantics so all visible rows still count toward discussion volume

- [ ] **Step 4: Re-run the backend community tests**

Run:

```powershell
cd backend
mvn -q -Dtest=CommunityControllerTests test
```

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/resources/schema.sql backend/src/main/resources/data.sql backend/src/main/java/com/campus/entity/CommunityComment.java backend/src/main/java/com/campus/dto/CommunityPostDetailResponse.java backend/src/main/java/com/campus/controller/CommunityController.java backend/src/main/java/com/campus/service/CommunityService.java backend/src/test/java/com/campus/controller/CommunityControllerTests.java
git commit -m "feat: add community threaded reply api"
```

## Task 2: Trigger Reply Notifications For The Replied-To Author

**Files:**

- Modify: `backend/src/main/java/com/campus/common/NotificationType.java`
- Modify: `backend/src/main/java/com/campus/service/CommunityService.java`
- Test: `backend/src/test/java/com/campus/controller/CommunityControllerTests.java`
- Test: `backend/src/test/java/com/campus/controller/NotificationControllerTests.java`

- [ ] **Step 1: Write failing notification coverage**

Add tests proving that:

- replying to another user's top-level comment creates `COMMUNITY_REPLY_RECEIVED`
- replying to your own top-level comment does not create a notification
- the recipient notification list exposes the new type, title, content, and source post reference

- [ ] **Step 2: Run the backend notification-focused tests**

Run:

```powershell
cd backend
mvn -q "-Dtest=CommunityControllerTests,NotificationControllerTests" test
```

- [ ] **Step 3: Implement the minimal notification flow**

Update the backend to:

- add `COMMUNITY_REPLY_RECEIVED` to `NotificationType`
- create a notification only from the reply path, never from first-level comment creation
- skip notification creation when the replier and target comment author are the same user
- use the Phase R payload contract:
  - title: `Your comment received a reply`
  - content: `<reply author nickname> replied to your comment under "<post title>"`
  - sourceType: `COMMUNITY_POST`
  - sourceId: `<post id>`

- [ ] **Step 4: Re-run the backend notification-focused tests**

Run:

```powershell
cd backend
mvn -q "-Dtest=CommunityControllerTests,NotificationControllerTests" test
```

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/campus/common/NotificationType.java backend/src/main/java/com/campus/service/CommunityService.java backend/src/test/java/com/campus/controller/CommunityControllerTests.java backend/src/test/java/com/campus/controller/NotificationControllerTests.java
git commit -m "feat: notify users about community replies"
```

## Task 3: Add Inline Threaded Reply Interaction On Community Detail

**Files:**

- Modify: `frontend/src/api/community.js`
- Modify: `frontend/src/components/CommunityCommentList.vue`
- Modify: `frontend/src/views/CommunityDetailView.vue`
- Modify: `frontend/src/views/CommunityDetailView.spec.js`

- [ ] **Step 1: Write failing frontend detail tests**

Cover:

- rendering replies beneath the correct top-level comment
- expanding and collapsing an inline reply form from a top-level comment action
- submitting a reply through the new reply API and refreshing the detail state with the returned payload
- preserving the existing first-level comment submit flow

- [ ] **Step 2: Run the frontend detail tests to confirm failure**

Run:

```powershell
cd frontend
npx vitest run src/views/CommunityDetailView.spec.js
```

- [ ] **Step 3: Implement the threaded reply UI**

Add:

- `createCommunityReply(commentId, payload)` in `frontend/src/api/community.js`
- reply-form state in `CommunityDetailView.vue` that supports lightweight inline forms under top-level comments
- login gating and shared `actionError` handling for reply submission
- nested reply rendering in `CommunityCommentList.vue` with reply author, reply target nickname, content, and timestamp
- a visually lighter reply container so second-level discussion is distinct without changing the overall page structure

- [ ] **Step 4: Re-run the frontend detail tests**

Run:

```powershell
cd frontend
npx vitest run src/views/CommunityDetailView.spec.js
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/api/community.js frontend/src/components/CommunityCommentList.vue frontend/src/views/CommunityDetailView.vue frontend/src/views/CommunityDetailView.spec.js
git commit -m "feat: add community threaded reply ui"
```

## Task 4: Recognize Reply Notifications In The Notification Center And Verify Regressions

**Files:**

- Modify: `frontend/src/views/NotificationCenterView.vue`
- Modify: `frontend/src/views/NotificationCenterView.spec.js`

- [ ] **Step 1: Write failing notification-center coverage**

Add a frontend test proving that `COMMUNITY_REPLY_RECEIVED` renders with a readable label and still participates in the existing unread/read interaction flow.

- [ ] **Step 2: Run the notification-center test to confirm failure**

Run:

```powershell
cd frontend
npx vitest run src/views/NotificationCenterView.spec.js
```

- [ ] **Step 3: Implement the new notification label mapping**

Update `NotificationCenterView.vue` so the label map recognizes `COMMUNITY_REPLY_RECEIVED` with a community-reply-specific display label while leaving the existing notification actions untouched.

- [ ] **Step 4: Re-run the notification-center test**

Run:

```powershell
cd frontend
npx vitest run src/views/NotificationCenterView.spec.js
```

- [ ] **Step 5: Run targeted regression verification**

Run:

```powershell
cd backend
mvn -q "-Dtest=CommunityControllerTests,NotificationControllerTests,HomeControllerTests" test
```

```powershell
cd frontend
npx vitest run src/views/CommunityDetailView.spec.js src/views/NotificationCenterView.spec.js src/views/HomeView.spec.js
npm run build
```

- [ ] **Step 6: Commit**

```bash
git add frontend/src/views/NotificationCenterView.vue frontend/src/views/NotificationCenterView.spec.js
git commit -m "feat: surface community reply notifications"
```
