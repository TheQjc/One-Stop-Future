INSERT INTO t_user (id, phone, nickname, real_name, role, status, verification_status, student_id, created_at, updated_at)
VALUES
  (1, '13800000000', 'PlatformAdmin', 'Admin User', 'ADMIN', 'ACTIVE', 'UNVERIFIED', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, '13800000001', 'NormalUser', 'Normal User', 'USER', 'ACTIVE', 'UNVERIFIED', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (3, '13800000002', 'VerifiedUser', 'Verified User', 'USER', 'ACTIVE', 'VERIFIED', '20260001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO t_community_post (id, author_id, tag, title, content, status, like_count, comment_count, favorite_count, created_at, updated_at)
VALUES
  (1, 2, 'CAREER', 'Offer timeline notes', 'Collected steps for internship and offer preparation.', 'PUBLISHED', 2, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 3, 'EXAM', 'Exam planning checklist', 'A compact checklist for target school planning and review rhythm.', 'PUBLISHED', 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (3, 2, 'ABROAD', 'Language prep starter', 'A basic preparation outline for language tests and application timing.', 'PUBLISHED', 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO t_resource_item (
  id, title, category, summary, description, status, uploader_id, reviewed_by, reject_reason,
  file_name, file_ext, content_type, file_size, storage_key, download_count, favorite_count,
  published_at, reviewed_at, created_at, updated_at
)
VALUES
  (
    1, '2026 Resume Template Pack', 'RESUME_TEMPLATE',
    'A concise resume starter pack for internship and campus-recruitment applications.',
    'Includes a one-page resume structure, naming suggestions, and a submission checklist.',
    'PUBLISHED', 3, 1, NULL,
    'resume-template-pack.pdf', 'pdf', 'application/pdf', 524288,
    'seed/2026/04/resume-template-pack.pdf', 12, 2,
    TIMESTAMPADD(DAY, -4, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -4, CURRENT_TIMESTAMP),
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  ),
  (
    2, 'Interview Experience Notes Collection', 'INTERVIEW_EXPERIENCE',
    'A structured collection of common campus interview questions and response patterns.',
    'Focuses on backend, product, and operations interview reflection templates.',
    'PUBLISHED', 2, 1, NULL,
    'interview-experience-notes.zip', 'zip', 'application/zip', 1048576,
    'seed/2026/04/interview-experience-notes.zip', 7, 1,
    TIMESTAMPADD(DAY, -2, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -2, CURRENT_TIMESTAMP),
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  ),
  (
    3, 'IELTS Writing Drill Workbook', 'LANGUAGE_TEST',
    'Workbook upload awaiting review before it can be shown publicly.',
    'Contains topic grouping and weekly drill pages for writing practice.',
    'PENDING', 2, NULL, NULL,
    'ielts-writing-drill.docx', 'docx', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 786432,
    'seed/2026/04/ielts-writing-drill.docx', 0, 0,
    NULL, NULL,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  );

INSERT INTO t_job_posting (
  id, title, company_name, city, job_type, education_requirement, source_platform, source_url,
  summary, content, deadline_at, published_at, status, created_by, updated_by, created_at, updated_at
)
VALUES
  (
    1, 'Java Backend Intern', 'Future Campus Tech', 'Shenzhen', 'INTERNSHIP', 'BACHELOR', 'Official Site',
    'https://jobs.example.com/future-campus-tech/backend-intern',
    'Work on Spring Boot services, internal tooling, and campus product delivery.',
    'Join the backend engineering team to support student-facing platform modules and internal admin tools.',
    TIMESTAMPADD(DAY, 20, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -3, CURRENT_TIMESTAMP), 'PUBLISHED', 1, 1,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  ),
  (
    2, 'Campus Recruitment Product Manager', 'North Bay Education', 'Guangzhou', 'CAMPUS', 'BACHELOR', 'WeCom Channel',
    'https://jobs.example.com/north-bay-education/campus-pm',
    'Coordinate recruitment campaigns, student research, and cross-team delivery for campus growth.',
    'Focus on campus recruitment planning, content coordination, and basic data analysis for hiring funnels.',
    TIMESTAMPADD(DAY, 30, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP), 'PUBLISHED', 1, 1,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  ),
  (
    3, 'AI Research Assistant', 'Delta Lab', 'Shenzhen', 'FULL_TIME', 'MASTER', 'Internal Referral',
    'https://jobs.example.com/delta-lab/ai-research-assistant',
    'Draft job card awaiting final review before publication.',
    'Support model evaluation, experiment organization, and research material synthesis.',
    TIMESTAMPADD(DAY, 45, CURRENT_TIMESTAMP), NULL, 'DRAFT', 1, 1,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  );
