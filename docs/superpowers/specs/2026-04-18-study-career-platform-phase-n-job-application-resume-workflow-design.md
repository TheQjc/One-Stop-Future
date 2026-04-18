# Study-Career Platform Phase N Job Application And Resume Workflow Design

## 1. Goal

Phase N adds the first in-platform job application workflow and a user-owned resume library while preserving the current Spring Boot monolith, Vue SPA, existing jobs board, and existing raw file storage boundary.

This phase delivers one bounded slice:

- a user-owned resume library for authenticated users
- in-platform application submission from the job detail page
- one application record per user per job
- a dedicated "my applications" page for applicants
- a dedicated admin-only applications workbench
- application-owned resume snapshots so historical applications stay readable after resume-library changes

The phase is intentionally narrow. It creates a platform-owned application record and resume flow without adding a recruiter account system, application review pipeline, or resume-builder product.

## 2. User-Validated Scope

The following decisions were chosen for this phase:

- this phase is `Phase N: Job Application And Resume Workflow`
- in-platform applications are platform-owned records only
- there is no recruiter or employer account side in this phase
- resumes are upload-based, not structured online resumes
- users can keep multiple resumes in a dedicated resume library
- the first admin surface is a separate admin route:
  - `/admin/applications`
- duplicate application is not allowed:
  - one user can submit at most one application to the same job
- application status in this phase is minimal:
  - `SUBMITTED`
- all authenticated users can apply
- the jobs detail page keeps the existing external `Source Link`
- in-platform apply and external source jump coexist on the same job page
- applications store a resume snapshot, not only a live resume reference
- resume-library supported file types are:
  - `PDF`
  - `DOC`
  - `DOCX`
- users get a separate page for:
  - `/profile/resumes`
  - `/profile/applications`
- the resume-library management scope is:
  - upload
  - list
  - select during application
  - download
  - delete
- the apply form does not include extra message, self-introduction, or cover letter text

## 3. Non-Goals

This phase does not implement:

- recruiter or employer accounts
- application status progression beyond `SUBMITTED`
- admin-side approve, reject, advance, or note-taking actions for applications
- in-platform resume builder or structured resume editing
- resume rename, file replacement, or version history
- resume online preview
- application comments, cover letters, or self-introduction text
- batch application submit, export, or analytics
- notification flows for application submission or outcome
- homepage or admin-dashboard application summary cards in this first slice

## 4. Chosen Approach

### 4.1 Recommendation

Implement two new business domains with clear boundaries:

- `Resume`
  - user-owned uploaded resume records
- `JobApplication`
  - platform-owned application records with immutable resume snapshots

Both domains should reuse the existing raw file-storage infrastructure through `ResourceFileStorage`, but they should not reuse `ResourceService` or resource-review business semantics.

### 4.2 Why This Approach

This approach fits the current project structure best:

- it keeps resume and application semantics separate from the public resource library
- it preserves the existing jobs board and adds one new action on top of it
- it keeps future application workflow expansion possible without overloading `JobService` or `UserService`
- it reuses the proven local/MinIO raw-file storage boundary instead of duplicating storage logic
- it makes historical application files stable through snapshot ownership

### 4.3 Rejected Alternatives

#### Reuse The Resource Library As The Resume Library

Rejected because it would:

- pull in unrelated resource-review and publish/offline semantics
- create awkward coupling between personal resumes and public resources
- make future application logic harder to reason about

#### Put Resume State Inside The User Profile Only

Rejected because it would:

- force one user to one resume or one oversized profile payload
- make the profile domain absorb file-management concerns it does not currently own
- create poor boundaries for future resume expansion

#### Use Only Live Resume References Without Snapshots

Rejected because it would:

- make historical applications change when users delete or replace resumes
- create fragile admin review behavior for already-submitted applications
- undermine the explicit requirement that application history remain stable

## 5. Functional Scope

### 5.1 Resume Library

Authenticated users can:

- upload a resume file with a user-facing title
- view their own resume list
- download their own stored resume files
- delete their own resume records

Resume-library rules in this phase:

- supported extensions are `pdf`, `doc`, `docx`
- there is no preview action
- there is no rename action
- there is no replace-file action
- there is no public exposure of resumes

### 5.2 In-Platform Apply On Job Detail

The job detail page should keep the current external source-jump behavior and add one new platform action:

- `Apply In Platform`

Apply flow:

1. guest clicks apply -> redirect to login
2. authenticated user without resumes -> sees a clear empty-state prompt to upload a resume first
3. authenticated user with resumes -> selects one resume and submits
4. if already applied -> the page shows an applied state and does not allow a second submission

### 5.3 My Applications

Authenticated users can open:

- `/profile/applications`

The page should show:

- job title
- company name
- city
- submitted time
- current status
- chosen resume snapshot label
- link back to the original job detail page

This page is a read-only history board in this phase.

### 5.4 Admin Applications Workbench

Admins can open:

- `/admin/applications`

The page is read-only in this phase and should show:

- application list
- applicant identity context
- job identity context
- resume snapshot identity
- submitted time
- a download action for the resume snapshot
- a link back to the original job detail page

No application-state mutation actions are included in this phase.

## 6. Architecture

### 6.1 Existing Components Reused

- existing Spring Boot monolith and Vue SPA shell
- existing auth and route-guard behavior
- current `JobService` and jobs detail/list routes
- current `ResourceFileStorage` abstraction
- current local-vs-MinIO raw-file storage selection
- project-standard `{ code, message, data }` response wrapper

### 6.2 New Or Changed Backend Units

- `ResumeController`
  - user-facing resume upload, list, download, and delete endpoints
- `ResumeService`
  - owns resume validation, ownership checks, storage, and deletion
- `ResumeMapper`
  - persistence for resume records
- `JobApplicationController`
  - user-facing apply and my-applications endpoints
- `JobApplicationService`
  - owns duplicate-apply rules, resume snapshot creation, and list shaping
- `JobApplicationMapper`
  - persistence for application records
- `AdminJobApplicationController`
  - admin applications list and snapshot-download endpoints
- `AdminJobApplicationService`
  - admin-only read shaping over applications

Changed backend units:

- `JobService`
  - extends job-detail shaping with current-user application state
- `JobDetailResponse`
  - includes lightweight platform-application state for the viewer

### 6.3 New Or Changed Frontend Units

- new route for `/profile/resumes`
- new route for `/profile/applications`
- new route for `/admin/applications`
- new resume API adapter module
- new application API adapter module
- update job-detail API adapter and `JobDetailView`
- new profile resumes view
- new profile applications view
- new admin applications view
- navigation entry updates for the two profile routes and the admin route

### 6.4 Responsibility Boundaries

- resume business logic lives in `ResumeService`, not in `UserService`
- application business logic lives in `JobApplicationService`, not inside `JobService`
- `JobService` remains responsible for job visibility and job-detail shaping
- file persistence continues to use `ResourceFileStorage`, but the resume/application domains own their own records and errors
- admin applications remain a separate read-only workbench rather than an extension of admin jobs editing

## 7. Data Design

### 7.1 Resume Table

Recommended new table:

- `t_resume`

Recommended fields:

- `id`
- `user_id`
- `title`
- `file_name`
- `file_ext`
- `content_type`
- `file_size`
- `storage_key`
- `created_at`
- `updated_at`

Semantics:

- `title` is the user-owned label for choosing among resumes
- `storage_key` points to the raw resume file in the active storage backend
- there is no status field in this phase

### 7.2 Job Application Table

Recommended new table:

- `t_job_application`

Recommended fields:

- `id`
- `job_id`
- `applicant_user_id`
- `resume_id`
- `status`
- `resume_title_snapshot`
- `resume_file_name_snapshot`
- `resume_file_ext_snapshot`
- `resume_content_type_snapshot`
- `resume_file_size_snapshot`
- `resume_storage_key_snapshot`
- `submitted_at`
- `created_at`
- `updated_at`

Status scope:

- `SUBMITTED`

### 7.3 Duplicate-Apply Constraint

Database-level protection should enforce:

- unique key on `(job_id, applicant_user_id)`

The service layer should still validate before insert so the user gets a clear business message, but the database must remain the final consistency guard.

### 7.4 Snapshot Ownership

Application snapshot semantics in this phase:

- when an application is submitted, the selected resume file is copied into a new application-owned raw file
- the application row stores that copied file under `resume_storage_key_snapshot`
- later resume-library deletion must not break application-history file access

This means:

- deleting a resume record only affects the live resume-library record and its original file
- deleting a resume record does not delete any historical application snapshot files

## 8. API Design

### 8.1 Resume Endpoints

Recommended endpoints:

```http
GET /api/resumes/mine
POST /api/resumes
GET /api/resumes/{id}/download
DELETE /api/resumes/{id}
```

Behavior:

- all endpoints require authentication
- all operations are ownership-scoped to the authenticated user
- upload remains multipart-based, aligned with the current resource-upload style

### 8.2 Apply And My-Applications Endpoints

Recommended endpoints:

```http
POST /api/jobs/{id}/apply
GET /api/applications/mine
```

Recommended apply request:

```json
{
  "resumeId": 9001
}
```

Recommended apply behavior:

- require authenticated user
- require existing published job
- require resume owned by current user
- reject duplicate application to the same job
- create snapshot file first
- insert application record only after snapshot creation succeeds

### 8.3 Admin Application Endpoints

Recommended endpoints:

```http
GET /api/admin/applications
GET /api/admin/applications/{id}/resume/download
```

Behavior:

- admin-only
- read-only in this phase
- no mutation routes are added

### 8.4 Job Detail Contract Change

`GET /api/jobs/{id}` should extend its response with application-state fields for the current viewer:

- `appliedByMe`
- `applicationId` (optional, only when applied)

Recommended wrapped example:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 3001,
    "title": "Backend Intern",
    "companyName": "Campus Tech",
    "city": "Shanghai",
    "jobType": "INTERNSHIP",
    "educationRequirement": "BACHELOR",
    "sourcePlatform": "Official Site",
    "sourceUrl": "https://jobs.example.com/backend-intern",
    "summary": "Java backend internship",
    "content": "Long form description",
    "status": "PUBLISHED",
    "deadlineAt": "2026-05-30T18:00:00",
    "publishedAt": "2026-04-18T12:00:00",
    "createdAt": "2026-04-18T09:00:00",
    "updatedAt": "2026-04-18T12:00:00",
    "favoritedByMe": false,
    "appliedByMe": true,
    "applicationId": 7001
  }
}
```

## 9. Frontend Flow Design

### 9.1 Resume Library Page

Route:

- `/profile/resumes`

The page should follow the same calm record-board language as the current profile surfaces.

Recommended content:

- page header
- simple stats card for total resumes
- upload form with:
  - `title`
  - `file`
- resume cards with:
  - title
  - file name
  - file size
  - created time
  - actions:
    - `Download`
    - `Delete`

### 9.2 Job Detail Apply Interaction

The existing job-detail actions block should become:

- `Apply In Platform`
- `Source Link`
- `Save This Job`
- `Back To Jobs`

Recommended behavior:

- guest apply click redirects to login
- authenticated users without resumes see a compact empty state with link to `/profile/resumes`
- authenticated users with resumes can choose one resume and submit
- already-applied jobs show a stable applied state and link to `/profile/applications`

The design should remain inline on the page rather than opening a heavy modal workflow.

### 9.3 My Applications Page

Route:

- `/profile/applications`

Recommended page content:

- page header
- compact summary cards:
  - total applications
  - submitted count
- application cards with:
  - job title
  - company
  - city
  - submitted time
  - status
  - resume snapshot title
  - link to job detail

This page is intentionally read-only in the first slice.

### 9.4 Admin Applications Page

Route:

- `/admin/applications`

Recommended page content:

- page header
- four compact metrics:
  - all applications
  - submitted today
  - unique applicants
  - unique jobs
- read-only table or card list
- row content:
  - application id
  - applicant nickname
  - applicant user id
  - job title
  - company name
  - resume snapshot file name
  - submitted time
  - status
- row actions:
  - `Download Resume`
  - `Open Job`

The admin page must not expose edit, approve, reject, or delete actions in this phase.

## 10. Error Handling

### 10.1 Resume Errors

Resume upload should fail clearly when:

- no file is provided
- file extension is unsupported
- file size exceeds the configured limit
- file storage fails

Recommended supported types in this phase:

- `pdf`
- `doc`
- `docx`

### 10.2 Apply Errors

Recommended business failures:

- unauthenticated apply -> existing auth failure flow
- selected resume not found or not owned -> `404 resume not found`
- job not found or not published -> `404 job not found`
- no resume selected -> `400 resume is required`
- duplicate apply -> `400 already applied to this job`
- snapshot copy failure -> `500 failed to store application resume snapshot`

### 10.3 Delete Semantics

Deleting a live resume record should:

- remove the resume row
- remove the original resume file when it still exists
- never remove any application snapshot files

If the original resume file delete fails after the row update, the service should log the failure and preserve user-visible success semantics only if the database/file consistency strategy chosen in implementation supports that safely. The implementation plan should make this order explicit.

## 11. Testing Strategy

### 11.1 Backend Tests

Backend tests should verify:

- resume upload/list/download/delete for the owner
- unsupported resume file type is rejected
- apply succeeds with an owned resume and published job
- duplicate application is rejected
- applying with another user's resume is rejected
- applying to non-published job is rejected
- job detail returns `appliedByMe=true` after successful apply
- deleting the original resume does not break historical application snapshot download
- admin applications list is admin-only and read-only

### 11.2 Frontend Tests

Frontend tests should verify:

- `/profile/resumes` renders upload + list + empty state correctly
- `/profile/applications` renders history correctly
- `/admin/applications` renders read-only records correctly
- job detail redirects guests to login on apply
- job detail shows upload-resume guidance when the user has no resumes
- job detail can submit one selected resume
- job detail shows applied state after success
- admin page does not render mutation buttons

### 11.3 Manual Smoke

A local smoke pass should verify:

1. log in as `13800000001`
2. upload two resumes in `/profile/resumes`
3. open one published job detail page
4. apply with one selected resume
5. confirm the job page now shows applied state
6. open `/profile/applications` and confirm the new record renders
7. delete the original resume from `/profile/resumes`
8. confirm the application record still exists and the historical snapshot remains downloadable
9. log in as admin `13800000000`
10. open `/admin/applications`
11. confirm the read-only list renders and the resume snapshot can be downloaded

## 12. Acceptance Criteria

Phase N can be considered complete when all of the following are true:

- authenticated users can manage a multi-resume library at `/profile/resumes`
- supported resume formats are `PDF / DOC / DOCX`
- published job detail pages support in-platform application while keeping the external source link
- one user can submit at most one application to the same job
- successful applications create stable historical resume snapshots
- authenticated users can view their applications at `/profile/applications`
- admins can view applications at `/admin/applications`
- the admin page remains read-only
- automated backend and frontend verification for this slice pass
- one local smoke path passes

## 13. Implementation Handoff

The implementation plan for this phase should be written next at:

- `docs/superpowers/plans/2026-04-18-study-career-platform-phase-n-job-application-resume-workflow-implementation.md`

Recommended implementation order:

1. resume table, entity, mapper, DTOs, and service/controller
2. job-application table, entity, mapper, DTOs, and service/controller
3. job-detail response enrichment with current-user application state
4. profile resumes page and profile applications page
5. admin applications page
6. automated verification and local smoke
