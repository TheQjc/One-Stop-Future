# Study-Career Platform Phase B Community Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first Phase B community vertical slice on top of the existing Phase A foundation: public browse, authenticated posting, comments, likes, favorites, profile-side post/favorite views, and minimal admin governance.

**Architecture:** Keep the current Spring Boot monolith and Vue SPA structure intact. Add a focused `community` domain in backend and frontend without introducing search, file upload, or new shell layers. Reuse existing JWT auth, `Result{code,message,data}` responses, admin role checks, profile shell, and home entry aggregation.

**Tech Stack:** Java 17, Spring Boot 3, Spring Security, MyBatis-Plus, MySQL 8, H2 test DB, JWT, Vue 3, Vue Router, Pinia, Axios, Vite, Vitest

---

## Context Before Starting

- Spec baseline: `docs/superpowers/specs/2026-04-16-study-career-platform-phase-b-community-design.md`
- Requirements baseline: `docs/superpowers/requirements/2026-04-15-study-career-platform-formal-requirements.md`
- Existing implementation baseline: `docs/superpowers/plans/2026-04-15-study-career-platform-phase-a-foundation-implementation.md`
- Current repo already has:
  - phone-code auth
  - profile and notification center
  - home aggregation page with `community` entry reserved
  - admin verification review backend and UI

## Scope Lock

This plan covers only the community first slice:

- public community list and detail
- authenticated create post
- first-level comments
- like / unlike
- favorite / unfavorite
- my posts
- my post favorites
- admin post list / hide / delete

This plan explicitly does not implement:

- image upload or attachment upload
- nested replies
- post review workflow
- hot ranking
- follow, DM, report
- search integration
- jobs or resource library integration

## Frontend Skill Rules

Every UI task in this plan must use:

- `@frontend-design`
  Use before editing layout, composition, spacing, typography, and visual hierarchy.
- `@ui-ux-pro-max`
  Use before closing each UI task to review clarity, mobile behavior, empty states, and action affordances.

Use the existing visual system as the base:

- theme: `editorial student decision desk`
- community variant: `campus editorial forum`
- keep warm paper backgrounds, deep navy text, muted teal support, rust/orange highlights
- keep the established homepage / profile shell patterns instead of introducing a second design language

## Planned File Structure

### Backend: Modify Existing

- Modify: `backend/src/main/resources/schema.sql`
- Modify: `backend/src/main/resources/data.sql`
- Modify: `backend/src/main/java/com/campus/config/SecurityConfig.java`
- Modify: `backend/src/main/java/com/campus/controller/UserController.java`
- Modify: `backend/src/main/java/com/campus/service/UserService.java`
- Modify: `backend/src/main/java/com/campus/service/HomeService.java`
- Modify: `backend/src/test/java/com/campus/CampusApplicationTests.java`
- Modify: `backend/src/test/java/com/campus/controller/UserControllerTests.java`

### Backend: Create

- Create: `backend/src/main/java/com/campus/common/CommunityTag.java`
- Create: `backend/src/main/java/com/campus/common/CommunityPostStatus.java`
- Create: `backend/src/main/java/com/campus/common/CommunityCommentStatus.java`
- Create: `backend/src/main/java/com/campus/common/FavoriteTargetType.java`
- Create: `backend/src/main/java/com/campus/entity/CommunityPost.java`
- Create: `backend/src/main/java/com/campus/entity/CommunityComment.java`
- Create: `backend/src/main/java/com/campus/entity/UserFavorite.java`
- Create: `backend/src/main/java/com/campus/mapper/CommunityPostMapper.java`
- Create: `backend/src/main/java/com/campus/mapper/CommunityCommentMapper.java`
- Create: `backend/src/main/java/com/campus/mapper/UserFavoriteMapper.java`
- Create: `backend/src/main/java/com/campus/dto/CreateCommunityPostRequest.java`
- Create: `backend/src/main/java/com/campus/dto/CreateCommunityCommentRequest.java`
- Create: `backend/src/main/java/com/campus/dto/CommunityPostListResponse.java`
- Create: `backend/src/main/java/com/campus/dto/CommunityPostDetailResponse.java`
- Create: `backend/src/main/java/com/campus/dto/AdminCommunityPostListResponse.java`
- Create: `backend/src/main/java/com/campus/controller/CommunityController.java`
- Create: `backend/src/main/java/com/campus/controller/admin/AdminCommunityController.java`
- Create: `backend/src/main/java/com/campus/service/CommunityService.java`
- Create: `backend/src/main/java/com/campus/service/AdminCommunityService.java`
- Create: `backend/src/test/java/com/campus/controller/CommunityControllerTests.java`
- Create: `backend/src/test/java/com/campus/controller/admin/AdminCommunityControllerTests.java`

### Frontend: Modify Existing

- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/views/HomeView.vue`
- Modify: `frontend/src/views/ProfileView.vue`
- Modify: `frontend/src/components/NavBar.vue`
- Modify: `frontend/src/App.spec.js`

### Frontend: Create

- Create: `frontend/src/api/community.js`
- Create: `frontend/src/components/CommunityPostCard.vue`
- Create: `frontend/src/components/CommunityCommentList.vue`
- Create: `frontend/src/components/CommunityFilterTabs.vue`
- Create: `frontend/src/views/CommunityListView.vue`
- Create: `frontend/src/views/CommunityDetailView.vue`
- Create: `frontend/src/views/CommunityCreateView.vue`
- Create: `frontend/src/views/ProfilePostsView.vue`
- Create: `frontend/src/views/ProfileFavoritesView.vue`
- Create: `frontend/src/views/admin/AdminCommunityManageView.vue`
- Create: `frontend/src/views/CommunityListView.spec.js`
- Create: `frontend/src/views/CommunityDetailView.spec.js`
- Create: `frontend/src/views/CommunityCreateView.spec.js`
- Create: `frontend/src/views/ProfilePostsView.spec.js`
- Create: `frontend/src/views/ProfileFavoritesView.spec.js`
- Create: `frontend/src/views/admin/AdminCommunityManageView.spec.js`

### Repo Docs

- Modify: `README.md`

## Task 1: Freeze the Community Schema and Persistence Model

**Files:**
- Modify: `backend/src/main/resources/schema.sql`
- Modify: `backend/src/main/resources/data.sql`
- Modify: `backend/src/test/java/com/campus/CampusApplicationTests.java`
- Create: `backend/src/main/java/com/campus/common/CommunityTag.java`
- Create: `backend/src/main/java/com/campus/common/CommunityPostStatus.java`
- Create: `backend/src/main/java/com/campus/common/CommunityCommentStatus.java`
- Create: `backend/src/main/java/com/campus/common/FavoriteTargetType.java`
- Create: `backend/src/main/java/com/campus/entity/CommunityPost.java`
- Create: `backend/src/main/java/com/campus/entity/CommunityComment.java`
- Create: `backend/src/main/java/com/campus/entity/UserFavorite.java`
- Create: `backend/src/main/java/com/campus/mapper/CommunityPostMapper.java`
- Create: `backend/src/main/java/com/campus/mapper/CommunityCommentMapper.java`
- Create: `backend/src/main/java/com/campus/mapper/UserFavoriteMapper.java`

- [ ] **Step 1: Write the failing schema smoke test**

```java
@SpringBootTest
class CampusApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Test
    void phaseBCommunityTablesExist() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            assertThat(metaData.getTables(null, null, "t_community_post", null).next()).isTrue();
            assertThat(metaData.getTables(null, null, "t_community_comment", null).next()).isTrue();
            assertThat(metaData.getTables(null, null, "t_user_favorite", null).next()).isTrue();
        }
    }
}
```

- [ ] **Step 2: Run the schema smoke test**

Run:

```powershell
cd backend
mvn -q -Dtest=CampusApplicationTests test
```

Expected: FAIL because the community tables do not exist yet.

- [ ] **Step 3: Add the Phase B community schema and enums**

Add these minimum tables:

```sql
CREATE TABLE t_community_post (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  author_id BIGINT NOT NULL,
  tag VARCHAR(20) NOT NULL,
  title VARCHAR(120) NOT NULL,
  content TEXT NOT NULL,
  status VARCHAR(20) NOT NULL,
  like_count INT NOT NULL DEFAULT 0,
  comment_count INT NOT NULL DEFAULT 0,
  favorite_count INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE t_community_comment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  post_id BIGINT NOT NULL,
  author_id BIGINT NOT NULL,
  content VARCHAR(1000) NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE t_user_favorite (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  target_type VARCHAR(20) NOT NULL,
  target_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_favorite_user_target (user_id, target_type, target_id)
);
```

Model rules:

- community tags: `CAREER`, `EXAM`, `ABROAD`, `CHAT`
- post status: `PUBLISHED`, `HIDDEN`, `DELETED`
- comment status: `VISIBLE`, `DELETED`
- favorite target type: `POST`

Seed `data.sql` with 2-3 sample published posts and no sample comments.

- [ ] **Step 4: Re-run the schema smoke test**

Run:

```powershell
cd backend
mvn -q -Dtest=CampusApplicationTests test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/resources/schema.sql backend/src/main/resources/data.sql backend/src/test/java/com/campus/CampusApplicationTests.java backend/src/main/java/com/campus/common/CommunityTag.java backend/src/main/java/com/campus/common/CommunityPostStatus.java backend/src/main/java/com/campus/common/CommunityCommentStatus.java backend/src/main/java/com/campus/common/FavoriteTargetType.java backend/src/main/java/com/campus/entity/CommunityPost.java backend/src/main/java/com/campus/entity/CommunityComment.java backend/src/main/java/com/campus/entity/UserFavorite.java backend/src/main/java/com/campus/mapper/CommunityPostMapper.java backend/src/main/java/com/campus/mapper/CommunityCommentMapper.java backend/src/main/java/com/campus/mapper/UserFavoriteMapper.java
git commit -m "feat: add phase b community persistence model"
```

## Task 2: Implement Public Community Backend APIs

**Files:**
- Modify: `backend/src/main/java/com/campus/config/SecurityConfig.java`
- Create: `backend/src/main/java/com/campus/dto/CreateCommunityPostRequest.java`
- Create: `backend/src/main/java/com/campus/dto/CreateCommunityCommentRequest.java`
- Create: `backend/src/main/java/com/campus/dto/CommunityPostListResponse.java`
- Create: `backend/src/main/java/com/campus/dto/CommunityPostDetailResponse.java`
- Create: `backend/src/main/java/com/campus/controller/CommunityController.java`
- Create: `backend/src/main/java/com/campus/service/CommunityService.java`
- Create: `backend/src/test/java/com/campus/controller/CommunityControllerTests.java`

- [ ] **Step 1: Write failing community controller tests**

```java
@SpringBootTest
@AutoConfigureMockMvc
class CommunityControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void guestCanReadCommunityList() throws Exception {
        mockMvc.perform(get("/api/community/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.posts").isArray());
    }

    @Test
    void guestCannotCreatePost() throws Exception {
        mockMvc.perform(post("/api/community/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"tag":"CAREER","title":"Offer 复盘","content":"这里是正文"}
                """))
            .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 2: Run the failing community controller tests**

Run:

```powershell
cd backend
mvn -q -Dtest=CommunityControllerTests test
```

Expected: FAIL because the controller and DTOs do not exist yet.

- [ ] **Step 3: Implement the public community API slice**

Required endpoints:

```java
GET    /api/community/posts
GET    /api/community/posts/{id}
POST   /api/community/posts
GET    /api/community/posts/mine
POST   /api/community/posts/{id}/comments
POST   /api/community/posts/{id}/like
DELETE /api/community/posts/{id}/like
POST   /api/community/posts/{id}/favorite
DELETE /api/community/posts/{id}/favorite
```

Implementation rules:

- list and detail are public
- create / comment / like / favorite require auth
- hidden and deleted posts are invisible to non-admins
- like and favorite are idempotent
- detail payload includes:
  - post core fields
  - author nickname
  - `likedByMe`
  - `favoritedByMe`
  - visible comment list
- create post validates:
  - title non-empty and max 120
  - content non-empty and max 10000
  - tag in allowed enum

- [ ] **Step 4: Re-run the community controller tests**

Run:

```powershell
cd backend
mvn -q -Dtest=CommunityControllerTests test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/campus/config/SecurityConfig.java backend/src/main/java/com/campus/dto/CreateCommunityPostRequest.java backend/src/main/java/com/campus/dto/CreateCommunityCommentRequest.java backend/src/main/java/com/campus/dto/CommunityPostListResponse.java backend/src/main/java/com/campus/dto/CommunityPostDetailResponse.java backend/src/main/java/com/campus/controller/CommunityController.java backend/src/main/java/com/campus/service/CommunityService.java backend/src/test/java/com/campus/controller/CommunityControllerTests.java
git commit -m "feat: add public community backend apis"
```

## Task 3: Add Favorites Query and Admin Community Governance Backend

**Files:**
- Modify: `backend/src/main/java/com/campus/controller/UserController.java`
- Modify: `backend/src/main/java/com/campus/service/UserService.java`
- Modify: `backend/src/main/java/com/campus/service/HomeService.java`
- Modify: `backend/src/test/java/com/campus/controller/UserControllerTests.java`
- Create: `backend/src/main/java/com/campus/dto/AdminCommunityPostListResponse.java`
- Create: `backend/src/main/java/com/campus/controller/admin/AdminCommunityController.java`
- Create: `backend/src/main/java/com/campus/service/AdminCommunityService.java`
- Create: `backend/src/test/java/com/campus/controller/admin/AdminCommunityControllerTests.java`

- [ ] **Step 1: Write failing admin and favorites tests**

```java
@Test
void myPostFavoritesReturnsPostTypeFavorites() throws Exception {
    mockMvc.perform(get("/api/users/me/favorites").param("type", "POST")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items").isArray());
}

@Test
void normalUserCannotOpenAdminCommunityList() throws Exception {
    mockMvc.perform(get("/api/admin/community/posts")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
        .andExpect(status().isForbidden());
}
```

- [ ] **Step 2: Run the failing admin and favorites tests**

Run:

```powershell
cd backend
mvn -q -Dtest=UserControllerTests,AdminCommunityControllerTests test
```

Expected: FAIL because these endpoints do not exist yet.

- [ ] **Step 3: Implement favorites query and admin governance**

Required endpoints:

```java
GET  /api/users/me/favorites?type=POST
GET  /api/admin/community/posts
POST /api/admin/community/posts/{id}/hide
POST /api/admin/community/posts/{id}/delete
```

Required behavior:

- favorites endpoint returns post summaries only for `type=POST`
- admin list includes published, hidden, and deleted items
- hide changes post status to `HIDDEN`
- delete changes post status to `DELETED`
- front-end public read APIs must stop exposing hidden and deleted posts
- home service keeps the `community` home entry enabled and pointed to `/community`

- [ ] **Step 4: Re-run the admin and favorites tests**

Run:

```powershell
cd backend
mvn -q -Dtest=UserControllerTests,AdminCommunityControllerTests test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/campus/controller/UserController.java backend/src/main/java/com/campus/service/UserService.java backend/src/main/java/com/campus/service/HomeService.java backend/src/test/java/com/campus/controller/UserControllerTests.java backend/src/main/java/com/campus/dto/AdminCommunityPostListResponse.java backend/src/main/java/com/campus/controller/admin/AdminCommunityController.java backend/src/main/java/com/campus/service/AdminCommunityService.java backend/src/test/java/com/campus/controller/admin/AdminCommunityControllerTests.java
git commit -m "feat: add admin community governance and post favorites"
```

## Task 4: Build the Frontend Community Public Experience

**Files:**
- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/components/NavBar.vue`
- Modify: `frontend/src/views/HomeView.vue`
- Create: `frontend/src/api/community.js`
- Create: `frontend/src/components/CommunityPostCard.vue`
- Create: `frontend/src/components/CommunityCommentList.vue`
- Create: `frontend/src/components/CommunityFilterTabs.vue`
- Create: `frontend/src/views/CommunityListView.vue`
- Create: `frontend/src/views/CommunityDetailView.vue`
- Create: `frontend/src/views/CommunityCreateView.vue`
- Create: `frontend/src/views/CommunityListView.spec.js`
- Create: `frontend/src/views/CommunityDetailView.spec.js`
- Create: `frontend/src/views/CommunityCreateView.spec.js`

- [ ] **Step 1: Write failing community view tests**

```js
import { mount } from "@vue/test-utils";
import CommunityListView from "./CommunityListView.vue";

test("community list shows fixed tag tabs", () => {
  const wrapper = mount(CommunityListView);
  expect(wrapper.text()).toContain("就业");
  expect(wrapper.text()).toContain("考研");
  expect(wrapper.text()).toContain("留学");
  expect(wrapper.text()).toContain("闲聊");
});
```

```js
import { mount } from "@vue/test-utils";
import CommunityCreateView from "./CommunityCreateView.vue";

test("create post view renders title tag and content controls", () => {
  const wrapper = mount(CommunityCreateView);
  expect(wrapper.find('input[name="title"]').exists()).toBe(true);
  expect(wrapper.find('select[name="tag"]').exists()).toBe(true);
  expect(wrapper.find('textarea[name="content"]').exists()).toBe(true);
});
```

- [ ] **Step 2: Run the failing community view tests**

Run:

```powershell
cd frontend
npm run test -- --run src/views/CommunityListView.spec.js src/views/CommunityCreateView.spec.js
```

Expected: FAIL because the views and routes do not exist yet.

- [ ] **Step 3: Build the community list / detail / create experience**

Before writing UI code:

- use `@frontend-design`
- then use `@ui-ux-pro-max`

Required routes:

```js
{ path: "/community", name: "community", component: CommunityListView }
{ path: "/community/create", name: "community-create", component: CommunityCreateView, meta: { requiresAuth: true } }
{ path: "/community/:id", name: "community-detail", component: CommunityDetailView }
```

Required behavior:

- homepage `community` entry now navigates to a real page
- list page shows fixed tags and post cards
- detail page carries like, favorite, and comment actions
- guest users can read but see login guidance for write actions
- create page posts then redirects to the new detail route

- [ ] **Step 4: Re-run the community view tests**

Run:

```powershell
cd frontend
npm run test -- --run src/views/CommunityListView.spec.js src/views/CommunityCreateView.spec.js src/views/CommunityDetailView.spec.js
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/router/index.js frontend/src/components/NavBar.vue frontend/src/views/HomeView.vue frontend/src/api/community.js frontend/src/components/CommunityPostCard.vue frontend/src/components/CommunityCommentList.vue frontend/src/components/CommunityFilterTabs.vue frontend/src/views/CommunityListView.vue frontend/src/views/CommunityDetailView.vue frontend/src/views/CommunityCreateView.vue frontend/src/views/CommunityListView.spec.js frontend/src/views/CommunityDetailView.spec.js frontend/src/views/CommunityCreateView.spec.js
git commit -m "feat: add community public frontend experience"
```

## Task 5: Extend Profile and Admin UI for Community

**Files:**
- Modify: `frontend/src/views/ProfileView.vue`
- Modify: `frontend/src/App.spec.js`
- Create: `frontend/src/views/ProfilePostsView.vue`
- Create: `frontend/src/views/ProfileFavoritesView.vue`
- Create: `frontend/src/views/admin/AdminCommunityManageView.vue`
- Create: `frontend/src/views/ProfilePostsView.spec.js`
- Create: `frontend/src/views/ProfileFavoritesView.spec.js`
- Create: `frontend/src/views/admin/AdminCommunityManageView.spec.js`

- [ ] **Step 1: Write failing profile/admin UI tests**

```js
import { mount } from "@vue/test-utils";
import ProfileView from "./ProfileView.vue";

test("profile view links to my posts and my favorites", () => {
  const wrapper = mount(ProfileView, {
    global: { stubs: ["RouterLink"] },
  });
  expect(wrapper.text()).toContain("我的发布");
  expect(wrapper.text()).toContain("我的收藏");
});
```

```js
import { mount } from "@vue/test-utils";
import AdminCommunityManageView from "./AdminCommunityManageView.vue";

test("admin community view shows hide and delete actions", () => {
  const wrapper = mount(AdminCommunityManageView);
  expect(wrapper.text()).toContain("下架");
  expect(wrapper.text()).toContain("删除");
});
```

- [ ] **Step 2: Run the failing profile/admin UI tests**

Run:

```powershell
cd frontend
npm run test -- --run src/views/ProfilePostsView.spec.js src/views/ProfileFavoritesView.spec.js src/views/admin/AdminCommunityManageView.spec.js
```

Expected: FAIL because the profile subpages and admin community page do not exist yet.

- [ ] **Step 3: Implement profile community subpages and admin manage page**

Before writing UI code:

- use `@frontend-design`
- then use `@ui-ux-pro-max`

Required behavior:

- profile page exposes entry links to `我的发布` and `我的收藏`
- `ProfilePostsView` reads `/api/community/posts/mine`
- `ProfileFavoritesView` reads `/api/users/me/favorites?type=POST`
- admin page reads `/api/admin/community/posts`
- admin page supports `下架` and `删除`
- non-admins are blocked by route meta and backend role checks

- [ ] **Step 4: Re-run the profile/admin UI tests**

Run:

```powershell
cd frontend
npm run test -- --run src/views/ProfilePostsView.spec.js src/views/ProfileFavoritesView.spec.js src/views/admin/AdminCommunityManageView.spec.js
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/ProfileView.vue frontend/src/App.spec.js frontend/src/views/ProfilePostsView.vue frontend/src/views/ProfileFavoritesView.vue frontend/src/views/admin/AdminCommunityManageView.vue frontend/src/views/ProfilePostsView.spec.js frontend/src/views/ProfileFavoritesView.spec.js frontend/src/views/admin/AdminCommunityManageView.spec.js
git commit -m "feat: add profile and admin community ui"
```

## Task 6: Local Verification and Documentation

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Update local-run docs**

Document:

- backend test command
- frontend test command
- local backend startup
- local frontend startup
- community routes
- guest vs login permissions
- admin community demo flow

- [ ] **Step 2: Run the backend test set**

Run:

```powershell
cd backend
mvn test
```

Expected: PASS.

- [ ] **Step 3: Run the frontend test set**

Run:

```powershell
cd frontend
npm run test -- --run
npm run build
```

Expected:

- frontend tests PASS
- frontend build PASS

- [ ] **Step 4: Run the local smoke pass**

Validate in this order:

1. Start backend locally with `mvn spring-boot:run`
2. Start frontend locally with `npm run dev -- --host 127.0.0.1`
3. Guest opens `/community` and `/community/:id`
4. Login user creates a post
5. Login user comments, likes, and favorites
6. `我的发布` and `我的收藏` show the new data
7. Admin opens `/admin/community`, hides the post
8. Guest or normal user can no longer open the hidden post

- [ ] **Step 5: Commit**

```bash
git add README.md
git commit -m "docs: add phase b community local run notes"
```

## Final Verification Set

After Task 6, run the full suite in this order:

```powershell
cd backend
mvn test
cd ../frontend
npm run test -- --run
npm run build
```

If any command fails, fix that failure before moving on to the next Phase B subproject.
