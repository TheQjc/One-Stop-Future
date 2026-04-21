# Study Career Platform Phase U Admin Batch Job Import Design

## 1. Goal

Phase U adds the first admin-side batch import flow for jobs while preserving the current Spring Boot monolith, Vue SPA, existing jobs board, existing admin jobs desk, and the current job publish/offline lifecycle.

This phase delivers one narrow admin productivity slice:

- admins can upload one `UTF-8 CSV` file from the existing `/admin/jobs` page
- the backend validates the entire file before writing any records
- valid imported jobs are created as `DRAFT`
- imported jobs immediately appear in the existing admin jobs list after success

This phase intentionally does not turn job import into a generic file-ingestion platform, async task framework, or third-party synchronization system.

## 2. User-Validated Scope

The following decisions were chosen for this phase during design discussion:

- this phase is `Phase U: Admin batch job import first slice`
- the first import format is `CSV` only
- uploaded CSV files must be `UTF-8` with one header row
- imported jobs default to `DRAFT`
- import happens inside the existing `/admin/jobs` workbench
- the backend uses one synchronous import request
- validation failure causes the whole import to fail
- partial-success import is out of scope
- duplicate handling is strict:
  - duplicate `sourceUrl` inside the file fails the import
  - duplicate `sourceUrl` against existing non-`DELETED` jobs fails the import

## 3. Non-Goals

This phase does not implement:

- `xlsx` or Excel import
- async import jobs, progress polling, or import history
- auto-publish after import
- partial-success row skipping
- upsert, merge, or overwrite modes
- duplicate-resolution configuration
- front-office job-list or job-detail changes
- application-flow changes
- third-party job sync

## 4. Chosen Approach

### 4.1 Recommendation

Add one dedicated admin import endpoint and one dedicated batch-import service that parse, validate, and persist imported jobs independently from the existing single-job create/update endpoints.

Recommended runtime shape:

- frontend keeps using `/admin/jobs`
- backend adds `POST /api/admin/jobs/import`
- the request accepts one `multipart/form-data` file field
- the response returns a compact import summary on success
- validation errors return row-level detail in the existing response envelope

### 4.2 Why This Approach

This approach fits the current codebase best:

- it preserves the existing admin jobs workbench instead of creating another admin route
- it keeps file parsing concerns out of `AdminJobService`
- it reuses the existing job model, status flow, and publish validation rules
- it minimizes rollout risk by landing imported jobs as `DRAFT`
- it keeps future `xlsx` import or external sync possible without changing public jobs behavior

### 4.3 Rejected Alternatives

#### Reuse The Existing Single-Job Create Endpoint In A Loop

Rejected because it would:

- mix file parsing with controller orchestration
- make row-level error reporting awkward
- increase the chance of partial writes if one later row fails

#### Support CSV And Excel In The First Slice

Rejected because it would:

- expand parser complexity before the import contract is proven
- add more edge cases around workbook shape, sheets, and cell typing
- slow down the delivery of the simplest useful admin workflow

#### Build An Async Import Task System First

Rejected because it would:

- introduce task status, persistence, and polling before they are needed
- enlarge the slice far beyond the current admin jobs desk gap
- duplicate infrastructure that may be shaped differently once third-party sync is designed

## 5. Functional Scope

### 5.1 Admin Import Entry

Admins can open the existing route:

- `/admin/jobs`

The page gains one new import surface:

- file picker for one CSV file
- short template guidance
- import action button
- success summary or validation-error summary

### 5.2 Imported Job Lifecycle

For every valid row in a valid file:

- a new `JobPosting` record is created
- `status` is set to `DRAFT`
- `publishedAt` remains `null`
- `createdBy` and `updatedBy` are set to the importing admin
- `createdAt` and `updatedAt` use the import execution time

Imported jobs do not become visible to public jobs pages until the admin later publishes them through the existing workflow.

### 5.3 Import Atomicity

Phase U uses all-or-nothing import semantics:

1. the backend parses the uploaded file
2. the backend validates every row
3. if any row fails validation, no jobs are inserted
4. if all rows pass validation, all rows are inserted in one transaction

This keeps retry behavior simple and avoids accidental duplicate creation in the first slice.

## 6. CSV Contract

### 6.1 File Format

The first import contract is:

- file type: `.csv`
- encoding: `UTF-8`
- first row: required header row
- delimiter: comma

Supported header names:

```csv
title,companyName,city,jobType,educationRequirement,sourcePlatform,sourceUrl,summary,content,deadlineAt
```

### 6.2 Required And Optional Columns

Required columns:

- `title`
- `companyName`
- `city`
- `jobType`
- `educationRequirement`
- `sourcePlatform`
- `sourceUrl`
- `summary`

Optional columns:

- `content`
- `deadlineAt`

### 6.3 Field Rules

The CSV column rules should align with the existing job request DTO limits:

- `title`
  - required
  - max `120`
- `companyName`
  - required
  - max `80`
- `city`
  - required
  - max `80`
- `jobType`
  - required
  - accepted values:
    - `INTERNSHIP`
    - `FULL_TIME`
    - `CAMPUS`
- `educationRequirement`
  - required
  - accepted values:
    - `ANY`
    - `BACHELOR`
    - `MASTER`
    - `DOCTOR`
- `sourcePlatform`
  - required
  - max `50`
- `sourceUrl`
  - required
  - max `500`
  - must be `http` or `https`
- `summary`
  - required
  - max `300`
- `content`
  - optional
  - max `10000`
- `deadlineAt`
  - optional
  - accepted format:
    - `yyyy-MM-dd HH:mm:ss`

String normalization rules:

- trim leading and trailing whitespace
- treat `jobType` and `educationRequirement` case-insensitively before enum normalization

### 6.4 Batch Limits

To keep the first slice bounded:

- one file per request
- maximum `200` data rows per file

## 7. Backend Design

### 7.1 New Units

Phase U introduces these focused backend units:

- `AdminJobImportController`
  - accepts the multipart upload request
  - delegates to the batch-import service
- `JobBatchImportService`
  - coordinates parse, validation, duplicate checks, and persistence
- `JobImportRowParser`
  - parses the CSV file into row objects
  - reports file-shape and row-shape issues
- `JobPostingDraftFactory`
  - converts one validated import row into a `JobPosting` draft
  - centralizes reusable normalization rules for import-created jobs

### 7.2 Existing Units Reused

Phase U should reuse:

- `JobPosting`
- `JobPostingMapper`
- `AdminJobController`
- `AdminJobService`
- current `JobType` and `JobEducationRequirement` enums
- current admin authorization boundary on `/api/admin/**`
- current `/admin/jobs` page and list refresh flow

### 7.3 Boundary Responsibilities

Recommended responsibility split:

- import controller
  - request boundary only
- row parser
  - CSV structure only
- draft factory
  - row-to-domain conversion
- batch-import service
  - domain validation and transactional persistence
- existing admin job service
  - keep ownership of single-job create, update, publish, offline, delete

This keeps the batch-import feature isolated without weakening current single-job flows.

## 8. API Contract

### 8.1 Endpoint

Add one new admin endpoint:

- `POST /api/admin/jobs/import`

### 8.2 Request Shape

Request type:

- `multipart/form-data`

Form fields:

- `file`

### 8.3 Success Response

Success keeps the existing result envelope:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "fileName": "jobs.csv",
    "totalRows": 12,
    "importedCount": 12,
    "defaultStatus": "DRAFT"
  }
}
```

### 8.4 Validation Error Response

Validation failure should also keep the existing result envelope:

```json
{
  "code": 400,
  "message": "job import validation failed",
  "data": {
    "fileName": "jobs.csv",
    "totalRows": 3,
    "importedCount": 0,
    "errors": [
      {
        "rowNumber": 2,
        "column": "jobType",
        "message": "invalid job type"
      },
      {
        "rowNumber": 4,
        "column": "sourceUrl",
        "message": "duplicate source url in file"
      }
    ]
  }
}
```

Rules:

- `rowNumber` uses CSV line numbers
- the header row is line `1`
- the first data row is line `2`
- errors are sorted by `rowNumber`

## 9. Validation And Duplicate Strategy

### 9.1 File-Level Validation

Reject the whole import when:

- the file is missing
- the file extension is not `.csv`
- the file is empty
- the file is not readable as `UTF-8`
- the header row is missing required columns
- the number of data rows exceeds `200`

### 9.2 Row-Level Validation

Reject the whole import when any row has:

- missing required value
- oversized text value
- invalid `jobType`
- invalid `educationRequirement`
- invalid `sourceUrl`
- invalid `deadlineAt`

### 9.3 Duplicate Rules

Reject the whole import when:

- two rows in the same file have the same normalized `sourceUrl`
- one imported row has the same normalized `sourceUrl` as an existing job whose status is not `DELETED`

This phase does not treat duplicates as updates.

## 10. Frontend Design

### 10.1 Admin Jobs Desk Integration

The existing admin jobs page gains one additional card or panel:

- file selection
- import button
- short CSV template guidance
- import result area

No new route is added.

### 10.2 Success Interaction

On successful import:

- show the import summary
- clear any previous validation errors
- refresh the admin jobs list
- keep the admin on the same `/admin/jobs` page

### 10.3 Failure Interaction

On validation failure:

- show the backend error summary
- show row-level issues in a readable list
- keep the current jobs list unchanged
- keep the admin on the same page so they can retry quickly

## 11. Error Handling

### 11.1 Authorization

- guest access to the import endpoint returns `401`
- non-admin authenticated access returns `403`

### 11.2 Validation Failures

Validation failures return:

- `code=400`
- `message=job import validation failed`
- row-level details in `data.errors`

### 11.3 Runtime Failures

Unexpected server failures should:

- return the existing business-error envelope style
- not leave partial imported rows in the database

### 11.4 Rollback Behavior

Any failure after parsing starts but before commit must roll back the entire batch.

## 12. Testing Strategy

### 12.1 Backend

Add focused backend coverage for:

- successful import of a valid CSV with multiple rows
- imported jobs defaulting to `DRAFT`
- missing required header
- empty file
- file-row limit exceeded
- invalid enum values
- invalid URL
- invalid deadline format
- duplicate `sourceUrl` inside the file
- duplicate `sourceUrl` against an existing non-`DELETED` job
- transaction rollback when one row fails
- non-admin access rejection

### 12.2 Frontend

Add focused frontend coverage for:

- choosing a file and submitting import from `/admin/jobs`
- success summary rendering
- error summary rendering
- admin jobs list refresh after success

## 13. Acceptance Criteria

This design is complete when the implementation can demonstrate:

1. admins can upload one valid `UTF-8 CSV` file from `/admin/jobs` and import multiple jobs in one request.
2. all imported jobs are created as `DRAFT` and are not auto-published.
3. any validation failure causes the whole import to fail with zero inserted rows.
4. validation errors identify row number, column, and reason.
5. the existing admin jobs list refreshes and shows imported drafts after success.
6. existing single-job admin workflows, public job browsing, favorites, and job application flows remain unchanged.
