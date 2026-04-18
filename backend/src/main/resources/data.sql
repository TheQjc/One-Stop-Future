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

INSERT INTO t_decision_assessment_question (id, code, prompt, description, display_order, is_active, created_at, updated_at)
VALUES
  (1, 'DECISION_Q1', 'Which outcome matters most to you right now?', 'Pick the option that best matches your current priority.', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 'DECISION_Q2', 'How do you prefer to measure progress?', 'Choose what feels most motivating and realistic.', 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (3, 'DECISION_Q3', 'What is your available weekly focus time?', 'Be honest so the recommendation stays practical.', 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (4, 'DECISION_Q4', 'What kind of uncertainty can you tolerate?', 'Different tracks have different kinds of risk and ambiguity.', 4, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (5, 'DECISION_Q5', 'Which learning style fits you best?', 'Your preferred learning mode impacts what you can sustain.', 5, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (6, 'DECISION_Q6', 'When do you need a clear next step by?', 'A deadline helps choose a track with the right pace.', 6, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO t_decision_assessment_option (
  id, question_id, code, label, description, display_order,
  career_score, exam_score, abroad_score, is_active, created_at, updated_at
)
VALUES
  (11, 1, 'Q1_A', 'Improve exam performance', 'Prioritize structured learning and score improvements.', 1, 0, 3, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (12, 1, 'Q1_B', 'Get a job offer sooner', 'Prioritize employability and interviews.', 2, 3, 0, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (13, 1, 'Q1_C', 'Prepare for studying abroad', 'Prioritize language tests and applications.', 3, 0, 0, 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (14, 1, 'Q1_D', 'Keep options open', 'Prefer a balanced plan while you explore.', 4, 1, 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

  (21, 2, 'Q2_A', 'Build a portfolio and ship projects', 'Progress is visible through outputs and iterations.', 1, 3, 0, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (22, 2, 'Q2_B', 'Hit clear score targets', 'Progress is visible through measurable scores.', 2, 0, 3, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (23, 2, 'Q2_C', 'Complete applications and documents', 'Progress is visible through submitted milestones.', 3, 0, 0, 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (24, 2, 'Q2_D', 'Mix of small wins', 'Progress comes from both output and study habits.', 4, 1, 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

  (31, 3, 'Q3_A', '10-15 hours', 'Enough time for a structured study plan.', 1, 0, 3, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (32, 3, 'Q3_B', '5-8 hours', 'Better for targeted interview practice and small projects.', 2, 3, 0, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (33, 3, 'Q3_C', '15-20 hours', 'Enough time for language prep and application work.', 3, 0, 0, 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (34, 3, 'Q3_D', 'It varies week to week', 'A flexible approach is required.', 4, 1, 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

  (41, 4, 'Q4_A', 'Prefer predictable milestones', 'I work best with clear checkpoints and feedback.', 1, 0, 3, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (42, 4, 'Q4_B', 'Prefer fast iteration under ambiguity', 'I can handle shifting requirements and quick cycles.', 2, 3, 0, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (43, 4, 'Q4_C', 'Prefer longer-term planning', 'I can manage longer timelines and paperwork.', 3, 0, 0, 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (44, 4, 'Q4_D', 'Some uncertainty is fine', 'I can adapt with a balanced plan.', 4, 1, 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

  (51, 5, 'Q5_A', 'Practice with past papers and drills', 'Repetition and feedback help me improve.', 1, 0, 3, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (52, 5, 'Q5_B', 'Learn by building real things', 'Projects and real tasks help me learn.', 2, 3, 0, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (53, 5, 'Q5_C', 'Learn by reading and writing', 'Documents, essays, and plans are my strength.', 3, 0, 0, 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (54, 5, 'Q5_D', 'Mix and match', 'I prefer variety across learning modes.', 4, 1, 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

  (61, 6, 'Q6_A', 'Within 1-2 months', 'Short timeline; choose a structured near-term plan.', 1, 0, 3, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (62, 6, 'Q6_B', 'Within 3-6 months', 'Medium timeline; interviews and projects can fit.', 2, 3, 0, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (63, 6, 'Q6_C', '6-12 months', 'Longer timeline; applications and tests can fit.', 3, 0, 0, 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (64, 6, 'Q6_D', 'No fixed deadline', 'Choose a balanced exploration plan.', 4, 1, 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
