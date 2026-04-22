# Campus Platform Formal Requirements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
> **Historical workflow note:** This plan produced the earlier campus-platform requirements artifact. Active implementation work now follows the regenerated study-career-platform baseline in `docs/superpowers/specs/2026-04-15-study-career-platform-regeneration-design.md` and `docs/superpowers/requirements/2026-04-15-study-career-platform-formal-requirements.md`.
> **Checklist status note:** Any remaining unchecked boxes are preserved as historical planning artifacts from the superseded campus-platform requirements workflow. They are not pending work for the active regenerated baseline and should not be completed retroactively.

**Goal:** Produce a formal, developer-facing requirements catalog for the campus platform by extracting explicit requirements, implicit constraints, and confirmed supplemental decisions from the approved source documents.

**Architecture:** The deliverable is a single Markdown requirements document under `docs/superpowers/requirements/` with stable requirement IDs, source traceability, module-level summaries, and implementation-relevant constraints. Work proceeds from structure first, then module requirements, then cross-cutting constraints, then a validation pass against the source document and approved design spec.

**Tech Stack:** Markdown, Git, PowerShell, `rg`, approved spec document, source product/technical plan document

---

## Planned File Structure

**Create**

- `docs/superpowers/requirements/2026-04-14-campus-platform-formal-requirements.md`

**Read-only References**

- `docs/校园一站式信息平台_产品规划与技术方案.md`
- `docs/superpowers/specs/2026-04-14-campus-platform-requirements-extraction-design.md`

**Responsibility Split**

- `docs/superpowers/requirements/2026-04-14-campus-platform-formal-requirements.md`
  Holds the final formal requirements catalog, including:
  - document scope and source notes
  - module-level overview
  - `FR-*` functional requirements
  - `CR-*` constraint requirements
  - confirmed supplemental requirements
  - source traceability and current issue status

## Validation Rules

Use these checks throughout execution instead of treating the document as free-form prose:

- No unresolved `TODO-*` markers remain in the final document.
- Every formal requirement starts with `FR-` or `CR-`.
- Every major module from the source document appears in the final requirements catalog.
- Confirmed decisions from the approved spec appear as formal requirements, not as open issues.
- If there are no unresolved questions, the issue section explicitly says there are currently no pending confirmation items.

### Task 1: Create the Requirements Catalog Skeleton

**Files:**
- Create: `docs/superpowers/requirements/2026-04-14-campus-platform-formal-requirements.md`
- Test: `docs/superpowers/requirements/2026-04-14-campus-platform-formal-requirements.md`

- [ ] **Step 1: Write the skeleton document with explicit placeholders**

```markdown
# 校园一站式信息平台正式需求清单

## 1. 文档目标与范围
TODO-SCOPE

## 2. 需求来源说明
TODO-SOURCES

## 3. 模块级需求总览
TODO-MODULE-OVERVIEW

## 4. 功能需求
### 4.1 通用平台能力
TODO-FR-COMMON
### 4.2 用户中心
TODO-FR-USER
### 4.3 通知公告
TODO-FR-NOTICE
### 4.4 校园活动
TODO-FR-ACTIVITY

## 5. 约束需求
TODO-CONSTRAINTS

## 6. 已确认补充需求
TODO-CONFIRMED

## 7. 待确认事项
TODO-ISSUES
```

- [ ] **Step 2: Run the placeholder check and verify the document is intentionally incomplete**

Run:

```powershell
rg "TODO-" "docs/superpowers/requirements/2026-04-14-campus-platform-formal-requirements.md"
```

Expected: multiple `TODO-*` matches, proving the document still needs content.

- [ ] **Step 3: Add the document header, numbering conventions, and requirement entry template**

```markdown
### Requirement Entry Template

- `FR-COMMON-001`
  - 名称：
  - 描述：系统应……
  - 类型：显式需求 / 隐含约束 / 已确认补充需求
  - 适用角色：
  - 优先级：
  - 来源：
```

- [ ] **Step 4: Re-run a heading check**

Run:

```powershell
rg "^## |^### " "docs/superpowers/requirements/2026-04-14-campus-platform-formal-requirements.md"
```

Expected: all planned top-level and second-level headings appear.

- [ ] **Step 5: Commit**

```bash
git add docs/superpowers/requirements/2026-04-14-campus-platform-formal-requirements.md
git commit -m "docs: scaffold formal requirements catalog"
```

### Task 2: Fill Scope, Sources, Roles, and Module Overview

**Files:**
- Modify: `docs/superpowers/requirements/2026-04-14-campus-platform-formal-requirements.md`
- Test: `docs/superpowers/requirements/2026-04-14-campus-platform-formal-requirements.md`

- [ ] **Step 1: Replace scope and source placeholders with final content**

Write these sections from the approved spec and source document:

```markdown
## 1. 文档目标与范围
- 本文档面向后续开发，整理校园一站式信息平台的正式需求。
- 本文档不新增未在现有材料中出现的新业务域。

## 2. 需求来源说明
- 主来源：`docs/校园一站式信息平台_产品规划与技术方案.md`
- 设计约束来源：`docs/superpowers/specs/2026-04-14-campus-platform-requirements-extraction-design.md`
```

- [ ] **Step 2: Run the targeted placeholder check**

Run:

```powershell
rg "TODO-(SCOPE|SOURCES|MODULE-OVERVIEW)" "docs/superpowers/requirements/2026-04-14-campus-platform-formal-requirements.md"
```

Expected: `TODO-SCOPE` and `TODO-SOURCES` are gone; `TODO-MODULE-OVERVIEW` may still remain until the next step.

- [ ] **Step 3: Write user roles, module overview, and priority mapping**

Include at minimum:

```markdown
## 3. 模块级需求总览
- 学生
- 教师
- 管理员

### 优先级
- P0：用户中心、通知公告
- P2：校园活动
```

Then add one short paragraph each for:

- 通用平台能力
- 用户中心
- 通知公告
- 校园活动

- [ ] **Step 4: Verify these placeholders are removed**

Run:

```powershell
rg "TODO-(SCOPE|SOURCES|MODULE-OVERVIEW)" "docs/superpowers/requirements/2026-04-14-campus-platform-formal-requirements.md"
```

Expected: no output.

- [ ] **Step 5: Commit**

```bash
git add docs/superpowers/requirements/2026-04-14-campus-platform-formal-requirements.md
git commit -m "docs: add scope roles and module overview"
```

### Task 3: Write Common Platform and User Center Requirements

**Files:**
- Modify: `docs/superpowers/requirements/2026-04-14-campus-platform-formal-requirements.md`
- Test: `docs/superpowers/requirements/2026-04-14-campus-platform-formal-requirements.md`

- [ ] **Step 1: Add common platform requirements with stable IDs**

At minimum, add entries shaped like:

```markdown
- `FR-COMMON-001`
  - 名称：独立首页聚合展示
  - 描述：系统应提供独立首页，作为用户登录后的统一入口页面，并聚合公告、身份状态与核心业务入口。
  - 类型：已确认补充需求
  - 适用角色：学生、教师、管理员
  - 优先级：P0
  - 来源：已确认补充需求

- `FR-COMMON-002`
  - 名称：登录态管理
  - 描述：系统应基于登录态控制用户访问受保护模块。
```

- [ ] **Step 2: Run a focused extraction check**

Run:

```powershell
rg "FR-COMMON-" "docs/superpowers/requirements/2026-04-14-campus-platform-formal-requirements.md"
```

Expected: at least the common platform requirement IDs appear.

- [ ] **Step 3: Add user center requirements**

Cover at least:

- `FR-USER-001` 注册
- `FR-USER-002` 登录
- `FR-USER-003` 登出
- `FR-USER-004` 个人信息查看
- `FR-USER-005` 个人信息修改
- `FR-USER-006` 密码修改
- `FR-USER-007` 角色区分
- `FR-USER-008` 教师自主注册

Use the same entry shape for each requirement and mark:

- 文档原文直接给出的内容为 `显式需求`
- 教师自主注册为 `已确认补充需求`

- [ ] **Step 4: Verify the section is complete**

Run:

```powershell
rg "TODO-(FR-COMMON|FR-USER)" "docs/superpowers/requirements/2026-04-14-campus-platform-formal-requirements.md"
```

Expected: no output.

- [ ] **Step 5: Commit**

```bash
git add docs/superpowers/requirements/2026-04-14-campus-platform-formal-requirements.md
git commit -m "docs: add common platform and user requirements"
```

### Task 4: Write Notice and Activity Functional Requirements

**Files:**
- Modify: `docs/superpowers/requirements/2026-04-14-campus-platform-formal-requirements.md`
- Test: `docs/superpowers/requirements/2026-04-14-campus-platform-formal-requirements.md`

- [ ] **Step 1: Add notice requirements**

Cover at minimum:

- `FR-NOTICE-001` 公告列表查询
- `FR-NOTICE-002` 公告详情查看
- `FR-NOTICE-003` 公告分页
- `FR-NOTICE-004` 公告按类别筛选
- `FR-NOTICE-005` 公告发布
- `FR-NOTICE-006` 公告编辑
- `FR-NOTICE-007` 公告删除
- `FR-NOTICE-008` 公告审核

- [ ] **Step 2: Add activity requirements**

Cover at minimum:

- `FR-ACTIVITY-001` 活动列表查看
- `FR-ACTIVITY-002` 活动详情查看
- `FR-ACTIVITY-003` 活动发布
- `FR-ACTIVITY-004` 活动审核
- `FR-ACTIVITY-005` 学生报名活动
- `FR-ACTIVITY-006` 学生取消报名
- `FR-ACTIVITY-007` 报名人数限制

- [ ] **Step 3: Verify all functional ID families exist**

Run:

```powershell
rg "FR-(NOTICE|ACTIVITY)-" "docs/superpowers/requirements/2026-04-14-campus-platform-formal-requirements.md"
```

Expected: both requirement families appear with no remaining functional placeholders.

- [ ] **Step 4: Commit**

```bash
git add docs/superpowers/requirements/2026-04-14-campus-platform-formal-requirements.md
git commit -m "docs: add module functional requirements"
```

### Task 5: Add Cross-Cutting Constraint Requirements

**Files:**
- Modify: `docs/superpowers/requirements/2026-04-14-campus-platform-formal-requirements.md`
- Test: `docs/superpowers/requirements/2026-04-14-campus-platform-formal-requirements.md`

- [ ] **Step 1: Add authentication and permission constraints**

Include at minimum:

- `CR-AUTH-001` JWT 登录凭证
- `CR-PERM-001` 三类角色权限区分
- `CR-PERM-002` 教师和管理员审核权限

Use entries like:

```markdown
- `CR-AUTH-001`
  - 名称：JWT 认证
  - 描述：系统应使用 JWT 作为登录凭证，并据此识别受保护接口的访问身份。
  - 类型：隐含约束
  - 适用角色：学生、教师、管理员
  - 优先级：P0
  - 来源：技术方案 / 接口设计
```

- [ ] **Step 2: Add API and list behavior constraints**

Include at minimum:

- `CR-API-001` 统一响应格式
- `CR-LIST-001` 公告列表分页
- `CR-LIST-002` 公告类别筛选

- [ ] **Step 3: Add data structure constraints**

Include at minimum:

- `CR-DATA-001` 用户名唯一
- `CR-DATA-002` 活动报名唯一约束
- `CR-DATA-003` 活动字段完整性
- `CR-DATA-004` 通知公告字段完整性

- [ ] **Step 4: Verify no constraint placeholders remain**

Run:

```powershell
rg "TODO-(CONSTRAINTS|CONFIRMED)" "docs/superpowers/requirements/2026-04-14-campus-platform-formal-requirements.md"
```

Expected: no output.

- [ ] **Step 5: Commit**

```bash
git add docs/superpowers/requirements/2026-04-14-campus-platform-formal-requirements.md
git commit -m "docs: add constraint requirements"
```

### Task 6: Run the Final Traceability and Completeness Pass

**Files:**
- Modify: `docs/superpowers/requirements/2026-04-14-campus-platform-formal-requirements.md`
- Test: `docs/superpowers/requirements/2026-04-14-campus-platform-formal-requirements.md`

- [ ] **Step 1: Add the final issue-status section**

If there are no unresolved items, write:

```markdown
## 7. 待确认事项

当前无待确认事项。
```

If a real unresolved ambiguity remains, list it explicitly and justify why it cannot yet be promoted to a formal requirement.

- [ ] **Step 2: Run the zero-placeholder check**

Run:

```powershell
rg "TODO-" "docs/superpowers/requirements/2026-04-14-campus-platform-formal-requirements.md"
```

Expected: no output.

- [ ] **Step 3: Run the source coverage check**

Run:

```powershell
rg "^#### 模块|^### 4\\.|^### 5\\.|^FR-|^CR-" "docs/superpowers/requirements/2026-04-14-campus-platform-formal-requirements.md"
```

Expected: the final document shows module sections plus populated `FR-*` and `CR-*` entries.

Then manually confirm coverage against:

- 用户中心
- 通知公告
- 校园活动
- 独立首页聚合展示
- JWT / 权限 / 统一响应 / 分页 / 数据约束

- [ ] **Step 4: Run a diff review**

Run:

```powershell
git diff -- "docs/superpowers/requirements/2026-04-14-campus-platform-formal-requirements.md"
```

Expected: the diff contains only the intended requirements catalog content with no placeholder text.

- [ ] **Step 5: Commit**

```bash
git add docs/superpowers/requirements/2026-04-14-campus-platform-formal-requirements.md
git commit -m "docs: finalize formal campus platform requirements"
```

## Execution Notes

- Do not invent new business modules not present in the approved source material.
- When a requirement comes from the approved conversation rather than the original project document, mark it as `已确认补充需求`.
- Keep requirement descriptions short, testable, and implementation-oriented.
- If a statement cannot be traced to either the source document or approved follow-up confirmation, do not turn it into a formal requirement.

