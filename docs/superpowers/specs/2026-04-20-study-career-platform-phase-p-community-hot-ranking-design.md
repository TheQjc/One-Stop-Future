# Study-Career Platform Phase P Community Hot Ranking Design

## 1. Document Goal

This document defines the smallest delivery slice for `FR-COMMUNITY-005` after Phase O.
The goal is to complete the missing community-only hot-post ranking capability without reopening the broader discover scope or introducing a new ranking infrastructure.

This phase should deliver:

- a public community hot-ranking endpoint
- support for `DAY`, `WEEK`, and `ALL`
- a community-page hot-ranking module inside `/community`
- reuse of the current community counters and author-verification signal

This phase should not deliver:

- Redis ranking cache
- event-level interaction history
- a separate community hot page
- recommendation explanations beyond a simple label
- admin curation or manual pinning

## 2. Current Conclusion

### 2.1 Requirement Fit

The formal requirement `FR-COMMUNITY-005` states:

- the platform must support community hot-post ranking
- the ranking must provide daily, weekly, and all-time boards

Current repo status does not satisfy this yet:

- Phase B community intentionally excluded hot ranking
- Phase F discover ranking covers cross-domain public discover only
- current `/community` still defaults to latest-post browsing

### 2.2 Recommended Approach

Three implementation approaches were considered:

#### Approach A: Community-Specific Hot Endpoint Plus `/community` Hot Module

- add `GET /api/community/hot`
- keep ranking logic in `CommunityService`
- render hot boards directly in `/community`

Pros:

- clean API semantics for `FR-COMMUNITY-005`
- minimal change surface
- no impact on discover contract
- easy to test and reason about

Cons:

- ranking logic partially overlaps the existing discover post-ranking formula

#### Approach B: Reuse `/api/discover?tab=POST`

- route community hot ranking through discover
- extend discover with `DAY`

Pros:

- less backend code at first glance

Cons:

- discover scope becomes muddier
- community requirement depends on a cross-domain surface
- adding `DAY` to discover changes an existing public contract unnecessarily

#### Approach C: Build a Dedicated `/community/hot` Full Page

- add a new page and separate browsing flow

Pros:

- visually stronger standalone entry

Cons:

- larger frontend slice than needed
- duplicates the existing `/community` browse surface

Chosen direction: `Approach A`.

## 3. Scope Definition

### 3.1 In Scope

This phase includes:

- new backend enum for community hot period
- new backend DTO for community hot board
- public endpoint `GET /api/community/hot`
- support for `period=DAY|WEEK|ALL`
- support for optional `limit`
- backend ranking over published posts only
- frontend API client for community hot board
- `/community` page hot board block above the latest-post list
- period switching between `DAY`, `WEEK`, and `ALL`
- loading, empty, and error states
- README updates and targeted verification notes

### 3.2 Explicitly Out of Scope

- ranking based on interaction deltas in the last 24 hours or 7 days
- separate URL query state for the hot board
- new database tables, snapshots, or scheduled jobs
- personalized ranking
- homepage hot-community preview
- discover page changes

## 4. Route and Permission Model

### 4.1 Backend Endpoint

- `GET /api/community/hot`

Access:

- public for guests and authenticated users
- only published posts are visible

### 4.2 Frontend Surface

- `/community`

Behavior:

- keep the current latest-post list and tag filter
- add a new hot-ranking block before the latest-post section
- hot board period switching should not require login

## 5. Ranking Model

### 5.1 Source Data

Use existing `t_community_post` fields only:

- `like_count`
- `comment_count`
- `favorite_count`
- `created_at`

Use author verification as a small trust bonus:

- verified author bonus = `2`
- unverified author bonus = `0`

### 5.2 Score Formula

Use the same core post formula already proven in Phase F discover ranking:

- `rawHeat = likeCount * 3 + commentCount * 4 + favoriteCount * 5 + verifiedAuthorBonus`
- `hotScore = rawHeat + freshnessBonus`
- `freshnessBonus = max(0, 14 - ageDays)`

This keeps public community ranking aligned with the discover post-ranking behavior while avoiding duplicated semantics.

### 5.3 Period Semantics

Because the current system stores cumulative counters on the post row rather than interaction events, this phase defines rolling windows by publish time:

- `DAY`
  - published in the last rolling 24 hours
  - sorted by current cumulative heat
- `WEEK`
  - published in the last rolling 7 days
  - sorted by current cumulative heat
- `ALL`
  - all published history

This is intentionally not:

- all posts ranked by last-24-hour interaction deltas
- all posts ranked by last-7-day interaction deltas

### 5.4 Sort Order

Use:

- `hotScore DESC`
- `createdAt DESC`
- `id DESC`

## 6. Backend Contract

### 6.1 Request

- `GET /api/community/hot`

Supported query params:

- `period`
  - optional
  - default `WEEK`
  - allowed: `DAY`, `WEEK`, `ALL`
- `limit`
  - optional
  - default `3`
  - allowed range: `1 - 10`

### 6.2 Response Shape

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "period": "WEEK",
    "total": 2,
    "items": [
      {
        "id": 1,
        "tag": "CAREER",
        "title": "Offer timeline notes",
        "contentPreview": "Collected steps for internship and offer preparation.",
        "authorId": 2,
        "authorNickname": "NormalUser",
        "likeCount": 2,
        "commentCount": 0,
        "favoriteCount": 1,
        "createdAt": "2026-04-20T08:00:00",
        "hotScore": 15.0,
        "hotLabel": "Weekly discussion"
      }
    ]
  }
}
```

### 6.3 Label Mapping

- `DAY -> Today spotlight`
- `WEEK -> Weekly discussion`
- `ALL -> Sustained discussion`

## 7. Frontend Design

### 7.1 Community Page Structure

`/community` should render in this order:

1. existing hero
2. new hot board block
3. existing tag filter block
4. existing latest-post block

### 7.2 Hot Board Interaction

The hot board should:

- default to `WEEK`
- provide three period chips
- request the backend board immediately on mount
- re-request on period switch
- show top ranked items with position numbers
- allow direct navigation to post detail

### 7.3 Visual Direction

Use the existing community editorial forum language instead of creating a second visual system:

- same paper-like cards
- same display typography
- same accent colors
- clearer emphasis for the top three ranks

## 8. Error Handling

Backend:

- invalid `period` -> business error `400` with message `invalid community hot period`
- invalid `limit` -> business error `400` with message `invalid community hot limit`

Frontend:

- loading state inside the hot board only
- retry button on error
- empty state when no posts fall into the selected period
- latest-post list must continue working even if hot board loading fails

## 9. Testing and Acceptance

### 9.1 Backend Tests

Must cover:

- default `WEEK` behavior
- `DAY`, `WEEK`, and `ALL` filtering
- published-only visibility
- score ordering and tie-break behavior
- invalid period and invalid limit handling

### 9.2 Frontend Tests

Must cover:

- hot board loads on community page mount
- period switching triggers a second fetch
- ranking items render in order
- hot board error state renders retry affordance

### 9.3 Acceptance

This phase is acceptable when:

- guests can open `/community` and see a hot board
- users can switch `DAY`, `WEEK`, and `ALL`
- only published posts appear in ranking
- post cards still navigate to `/community/:id`
- the existing latest-post flow remains intact
