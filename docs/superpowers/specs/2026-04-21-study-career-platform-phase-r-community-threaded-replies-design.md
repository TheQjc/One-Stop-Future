# Study Career Platform Phase R Community Threaded Replies Design

> **Validation note:** This design was implemented and validated on 2026-04-21. Execution record: `docs/superpowers/plans/2026-04-21-study-career-platform-phase-r-community-threaded-replies-implementation.md`. Documented verification covered `CommunityControllerTests`, `NotificationControllerTests`, `HomeControllerTests`, `CommunityDetailView.spec.js`, `NotificationCenterView.spec.js`, `HomeView.spec.js`, and `frontend` production build.

## 1. Document Goal

This document defines the smallest delivery slice for `FR-COMMUNITY-003` after Phase Q.

The goal is not to introduce a full forum-thread engine. The goal is to extend the current community detail flow so users can:

- reply to a first-level comment with one additional level of discussion
- see those replies inline under the related comment
- generate a minimal in-platform notification when someone replies to their comment

This phase keeps the change set intentionally narrow so it fits the current community architecture and does not destabilize existing post, like, favorite, hot-board, or experience-post behavior.

## 2. Current Conclusion

### 2.1 Selected Subproject

Phase R implements `community threaded replies with minimal notification closure`.

Why this is the next slice:

1. `FR-COMMUNITY-003` remains partially uncovered because the current platform supports only first-level comments.
2. The existing community detail page, notification center, and post-detail refresh model already provide enough scaffolding for a focused threaded-reply slice.
3. This delivery adds visible user value without introducing a separate messaging system, complex moderation workflow, or generalized mention parsing.

### 2.2 Chosen Approach

This phase uses:

- a single extended `t_community_comment` table
- a dedicated reply-creation endpoint
- one reply depth only
- notifications only for the author of the replied-to first-level comment

This approach is preferred over a separate reply table or a generic recursive comment tree because it is the smallest extension that still keeps API semantics clear.

## 3. Scope Definition

### 3.1 In Scope

This phase includes:

- first-level community comments remaining unchanged
- second-level replies under first-level comments only
- reply creation from the community detail page
- inline nested reply rendering under each top-level comment
- reply notification creation for the target comment author
- notification center recognition of the new community-reply notification type

### 3.2 Explicitly Out Of Scope

This phase does not include:

- infinite-depth comment threads
- replying to a reply
- `@username` parsing or mention extraction
- reply editing or deletion
- separate reply moderation tools
- post-author notifications for every reply
- comment-like or reply-like notifications
- notification deep linking to a specific comment anchor
- threaded pagination or lazy loading inside the comment tree

## 4. Data Model Design

### 4.1 Storage Strategy

Replies stay in the existing `t_community_comment` table.

Reasoning:

1. The current comment volume and feature scope do not justify a second table.
2. A shared table preserves the existing service and stats model with minimal migration cost.
3. One additional depth is easy to express with parent-child metadata while still allowing simple validation.

### 4.2 New Fields

Add these nullable fields to `t_community_comment`:

- `parent_comment_id`
  - type: `BIGINT`
  - meaning: points to the owning first-level comment
  - for first-level comments: `NULL`
- `reply_to_user_id`
  - type: `BIGINT`
  - meaning: records which user this reply is directed to
  - for first-level comments: `NULL`

### 4.3 Semantic Rules

- first-level comment:
  - `parent_comment_id = NULL`
  - `reply_to_user_id = NULL`
- second-level reply:
  - `parent_comment_id = <first-level comment id>`
  - `reply_to_user_id = <first-level comment author id>`
- a reply target must:
  - exist
  - belong to the same published post context
  - itself be a first-level comment
- if the target comment already has `parent_comment_id != NULL`, the backend rejects reply creation

This is the enforcement mechanism that guarantees only one reply layer.

## 5. API And DTO Design

### 5.1 Keep Existing First-Level Comment API

Keep:

- `POST /api/community/posts/{id}/comments`

Request body remains:

```json
{
  "content": "Useful planning summary."
}
```

This endpoint continues to create only first-level comments.

### 5.2 Add Dedicated Reply API

Add:

- `POST /api/community/comments/{id}/replies`

Where `{id}` is the target first-level comment id.

Request body:

```json
{
  "content": "This is the follow-up reply."
}
```

The frontend does not send `parentCommentId` or `replyToUserId`. The backend derives both from the target comment record so the client cannot build an invalid tree.

### 5.3 Detail Response Shape

Community post detail keeps returning the whole post detail object after comment or reply creation.

The `comments` collection changes from a flat list into a top-level comment list where each item owns a `replies` array.

Top-level comment item:

- `id`
- `authorId`
- `authorNickname`
- `content`
- `status`
- `createdAt`
- `mine`
- `replies`

Reply item:

- `id`
- `authorId`
- `authorNickname`
- `replyToUserId`
- `replyToUserNickname`
- `content`
- `status`
- `createdAt`
- `mine`

Illustrative response fragment:

```json
{
  "comments": [
    {
      "id": 101,
      "authorId": 2,
      "authorNickname": "Alice",
      "content": "First-level comment",
      "status": "VISIBLE",
      "createdAt": "2026-04-21T09:00:00",
      "mine": false,
      "replies": [
        {
          "id": 201,
          "authorId": 3,
          "authorNickname": "Bob",
          "replyToUserId": 2,
          "replyToUserNickname": "Alice",
          "content": "Second-level reply",
          "status": "VISIBLE",
          "createdAt": "2026-04-21T09:05:00",
          "mine": true
        }
      ]
    }
  ]
}
```

### 5.4 Ordering

- first-level comments: ascending by `created_at`, then `id`
- replies under each top-level comment: ascending by `created_at`, then `id`

This matches the current low-complexity comment reading flow and avoids introducing hot or latest toggle logic inside the thread itself.

## 6. Notification Design

### 6.1 New Notification Type

Add:

- `COMMUNITY_REPLY_RECEIVED`

### 6.2 Trigger Rules

Create this notification only when:

- user A replies to a first-level comment written by user B
- `A != B`

Do not create a notification when:

- the user replies to their own comment
- the user creates a first-level comment
- the user likes or favorites content

### 6.3 Notification Payload

Recommended payload:

- `type`: `COMMUNITY_REPLY_RECEIVED`
- `title`: `Your comment received a reply`
- `content`: `<reply author nickname> replied to your comment under "<post title>"`
- `sourceType`: `COMMUNITY_POST`
- `sourceId`: `<post id>`

This is enough for notification-center display and future route handoff to the post detail page, without requiring comment-anchor logic in this phase.

## 7. Frontend Interaction Design

### 7.1 Community Detail Page

For each top-level comment:

- show a `Reply` action
- clicking `Reply` expands a lightweight inline reply form below that comment
- clicking again can collapse the form
- replies render inline beneath the same comment in a visually lighter nested container

Recommended reply row display:

- reply author nickname
- `Reply to <target nickname>` copy
- reply content
- timestamp

### 7.2 UI Behavior Boundaries

This phase keeps the interaction lightweight:

- multiple reply forms may be open at the same time
- submitting a reply refreshes the local detail state through the returned detail payload
- reply forms reuse the existing action error area pattern
- no per-reply dropdown menu, edit action, or delete action

### 7.3 Notification Center

The notification center only needs to:

- map `COMMUNITY_REPLY_RECEIVED` to a readable label
- render title and content like any other notification type

No route jump or source-aware CTA is required in this phase.

## 8. Validation And Error Handling

### 8.1 Backend Validation

Reply creation must reject:

- missing target comment: `404`
- target comment not found on a published post: `404`
- target comment is itself already a reply: `400`
- blank content: existing validation behavior
- overlength content: existing validation behavior

### 8.2 Frontend Error Handling

The detail page reuses the existing `actionError` display area for reply failures.

No new global error state is required.

## 9. Testing Strategy

### 9.1 Backend

Add coverage for:

- creating a first-level comment still works unchanged
- replying to a first-level comment creates a second-level reply
- detail response nests replies under the correct top-level comment
- replying to a reply is rejected
- replying to another user's comment creates `COMMUNITY_REPLY_RECEIVED`
- replying to your own comment does not create a notification
- notification list includes the new type for the recipient

### 9.2 Frontend

Add coverage for:

- rendering replies under the correct top-level comment
- expanding and collapsing an inline reply form
- submitting a reply via the new reply API
- refreshed detail view rendering the new reply
- notification center label mapping for the new type

### 9.3 Regression Focus

Re-check:

- existing first-level comment creation
- like and favorite actions on post detail
- unread notification count updates
- notification mark-read and mark-all-read flows

## 10. Compatibility And Rollout Notes

- existing comment rows stay valid because historical data defaults to `parent_comment_id = NULL`
- old clients that only understand flat first-level comments will need the updated detail rendering logic before this can be exposed safely
- this phase introduces a backward-compatible database change but a response-shape change for the community detail comment list, so backend and frontend should ship together
