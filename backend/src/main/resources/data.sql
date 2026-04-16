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
