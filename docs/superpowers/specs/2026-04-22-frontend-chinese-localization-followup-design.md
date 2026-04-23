# Frontend Chinese Localization Follow-up Design

## 1. Goal

This follow-up design extends the approved Chinese-first frontend localization to the remaining user-facing detail pages, profile sub-pages, shared preview panel, and the still-English admin surfaces.

The goal is to remove the most visible leftover English copy while preserving routes, API contracts, auth rules, role behavior, and page-level interaction flow.

## 2. Validated Continuation Scope

This design continues the already approved frontend localization direction:

- Chinese-first wording for Chinese users
- practical, supportive campus-growth tone
- `One-Stop Future` stays secondary when shown
- no backend, route, or permission changes

The follow-up slice covers:

- community detail page
- job detail page
- resource detail page
- profile favorites, resources, resumes, and applications pages
- resource upload and resource edit pages
- shared ZIP preview panel
- remaining admin dashboard and review/manage pages with user-facing English copy

## 3. Non-Goals

This follow-up does not include:

- route renaming
- API schema changes
- new i18n infrastructure
- layout redesign beyond text-fitting adjustments already supported by current components
- test-data-only English that is not user-visible

## 4. Current Problems

The remaining untranslated pages share the same issues:

- action buttons still use English labels such as `Preview`, `Download`, `Delete`, `Retry`, and `Back To`
- detail pages still describe content with editorial words such as `Archive`, `Opportunity`, and `Desk`
- admin pages still present management surfaces in English even though the main frontend is already Chinese-first
- shared ZIP preview UI still exposes English loading, empty, and type labels

## 5. Approach Options

### Option A: User-Side First, Then Admin

First localize the shared preview component and the remaining user-side detail/profile pages, then localize admin surfaces in a second pass.

Pros:

- fixes the most visible user-facing gaps first
- allows reuse of shared wording in detail and profile pages
- keeps validation simpler by grouping pages by audience

Cons:

- admin pages stay mixed-language until the second pass in the same implementation cycle

### Option B: Shared Component First, Then All Remaining Pages By Area

Localize shared components first, then batch user and admin pages together by page family.

Pros:

- maximizes reuse from shared copy helpers
- reduces repeated edits for preview-related wording

Cons:

- mixes user and admin validation in a less narrative order

### Option C: Literal Sweep Over Every Remaining English String

Run a direct page-by-page translation pass wherever leftover English appears.

Pros:

- fast to start

Cons:

- risks inconsistent wording
- does not preserve the product voice established in the approved design
- makes tests harder to reason about

## 6. Recommendation

Use a hybrid of Option A and Option B:

- localize the shared ZIP preview component first
- localize remaining user-side detail/profile/resource workflow pages next
- localize remaining admin pages last

This sequence matches the most visible user experience gaps while still letting shared wording improvements land once and flow outward.

## 7. Design Details

### 7.1 Copy Direction

Use clear Chinese product language:

- `Archive` -> `资源` / `资料`
- `Opportunity` -> `岗位` / `机会` depending on context
- `Saved` / `Favorite` -> `已收藏` / `收藏`
- `Preview` / `Download` / `Delete` -> `预览` / `下载` / `删除`
- `Retry` / `Back To` -> `重试` / `返回...`

For admin pages, prefer `管理台`, `审核`, `记录`, and `处理` over editorial words like `Desk` and `Board`.

### 7.2 Shared Component Rule

`ResourceZipPreviewPanel.vue` should become the stable Chinese copy source for:

- title and eyebrow
- loading, empty, and error-adjacent states
- directory/file badges
- entry count wording
- size fallback wording

### 7.3 User-Side Page Rule

Detail and profile pages should:

- use Chinese section titles and metadata labels
- convert success/error/loading/action copy to Chinese
- keep current behavior for login redirects, favorite toggles, preview, apply, and download flows

### 7.4 Admin Page Rule

Admin surfaces should:

- keep the same CRUD and review flows
- replace English section labels, statuses, buttons, and helper copy with Chinese wording
- keep technical status codes and API payload handling unchanged

## 8. Testing

Update existing Vitest coverage for every touched page/component so that:

- localized Chinese labels are pinned in tests
- existing routing, auth, preview, and action behavior remains unchanged
- full frontend test and production build still pass
