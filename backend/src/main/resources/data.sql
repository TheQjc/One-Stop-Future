INSERT INTO t_user (id, phone, nickname, real_name, role, status, verification_status, student_id, created_at, updated_at)
VALUES
  (1, '13800000000', 'PlatformAdmin', 'Admin User', 'ADMIN', 'ACTIVE', 'UNVERIFIED', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, '13800000001', 'NormalUser', 'Normal User', 'USER', 'ACTIVE', 'UNVERIFIED', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (3, '13800000002', 'VerifiedUser', 'Verified User', 'USER', 'ACTIVE', 'VERIFIED', '20260001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO t_community_post (
  id, author_id, tag, title, content, status, like_count, comment_count, favorite_count,
  is_experience_post, experience_target_label, experience_outcome_label,
  experience_timeline_summary, experience_action_summary, created_at, updated_at
)
VALUES
  (
    1, 2, 'CAREER', 'Offer timeline notes', 'Collected steps for internship and offer preparation.', 'PUBLISHED', 2, 0, 1,
    0, NULL, NULL, NULL, NULL,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  ),
  (
    2, 3, 'EXAM', 'Exam planning checklist', 'A compact checklist for target school planning and review rhythm.', 'PUBLISHED', 1, 0, 0,
    0, NULL, NULL, NULL, NULL,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  ),
  (
    3, 2, 'ABROAD', 'Language prep starter', 'A basic preparation outline for language tests and application timing.', 'PUBLISHED', 0, 0, 0,
    1, 'IELTS 7.5 sprint', 'Mock score improved from 6.0 to 7.5',
    'Month 1 basics, month 2 timed drills, month 3 full mocks',
    'Keep one mistake log, one speaking routine, and one weekly full test.',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  );

INSERT INTO t_community_comment (
  id, post_id, author_id, parent_comment_id, reply_to_user_id, content, status, created_at, updated_at
)
VALUES
  (
    1, 1, 2, NULL, NULL, 'Share your interview prep milestones here.', 'VISIBLE',
    TIMESTAMPADD(MINUTE, -2, CURRENT_TIMESTAMP), TIMESTAMPADD(MINUTE, -2, CURRENT_TIMESTAMP)
  ),
  (
    2, 1, 3, 1, 2, 'I used a weekly mock interview loop and it helped a lot.', 'VISIBLE',
    TIMESTAMPADD(MINUTE, -1, CURRENT_TIMESTAMP), TIMESTAMPADD(MINUTE, -1, CURRENT_TIMESTAMP)
  );

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

INSERT INTO t_decision_timeline_milestone (
  id, track, phase_code, phase_label, title, summary,
  offset_months, offset_days, action_checklist, resource_hint,
  display_order, is_active, created_at, updated_at
)
VALUES
  (1001, 'EXAM', 'EXAM_P0', 'Baseline', 'Set targets and baseline', 'Choose target schools, define score goals, and measure your current baseline.', 0, 0,
   'Pick 1-2 target schools and exam subjects\nCollect last 2 years of exam papers\nTake a baseline mock to measure weak areas',
   'Start with a simple weekly plan and 2 mock sessions.', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1002, 'EXAM', 'EXAM_P1', 'Routine', 'Build a weekly study routine', 'Turn the baseline into a repeatable weekly routine with drills and review.', 0, 14,
   'Create a weekly schedule (study blocks + review)\nDrill weak topics daily\nReview mistakes and update notes',
   'Use past papers and keep a mistake notebook.', 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1003, 'EXAM', 'EXAM_P2', 'Mock', 'Increase mock frequency', 'Increase mock-test frequency and stabilize score ranges.', 1, 0,
   'Run a full mock every week\nTrack scores by topic\nAdjust drills based on the last mock',
   'Keep metrics: topic accuracy, time per section.', 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1004, 'EXAM', 'EXAM_P3', 'Sprint', 'Final sprint and review', 'Focus on high-yield review and execution under time pressure.', 2, 0,
   'Prioritize high-yield topics\nPractice under timed conditions\nSleep and recovery checklist',
   'Reduce new topics; focus on execution.', 4, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

  (1101, 'CAREER', 'CAREER_P0', 'Baseline', 'Clarify direction and role', 'Decide the role scope and prepare a realistic plan based on your background.', 0, 0,
   'Pick 1-2 role types to focus on\nList 3 example job postings\nWrite your current skill gaps',
   'Use job descriptions to define skills.', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1102, 'CAREER', 'CAREER_P1', 'Portfolio', 'Build a proof-of-skill project', 'Ship a small project that demonstrates core skills for your target role.', 0, 21,
   'Define a small project scope\nShip a first usable version\nWrite a short README and demo notes',
   'Prefer projects that map to job requirements.', 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1103, 'CAREER', 'CAREER_P2', 'Interview', 'Start interview practice', 'Practice core interview questions and simulate real interviews.', 1, 0,
   'Prepare a self-introduction\nPractice 20 common questions\nRun 2 mock interviews with peers',
   'Track mistakes and iterate weekly.', 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1104, 'CAREER', 'CAREER_P3', 'Apply', 'Apply and iterate', 'Apply consistently and iterate based on feedback and outcomes.', 2, 0,
   'Submit 10 applications\nFollow up and track status\nRefine resume based on outcomes',
   'Use a simple pipeline tracker.', 4, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

  (1201, 'ABROAD', 'ABROAD_P0', 'Baseline', 'Choose track and baseline language', 'Clarify target regions and measure your language baseline.', 0, 0,
   'Pick 1-2 target countries/regions\nChoose target programs\nTake a baseline language test',
   'Start a document checklist early.', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1202, 'ABROAD', 'ABROAD_P1', 'Language', 'Build a language routine', 'Build a routine for language improvement and test familiarity.', 0, 28,
   'Daily listening and reading routine\nWeekly writing practice\n1 full mock test every 2 weeks',
   'Use a consistent routine over intensity.', 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1203, 'ABROAD', 'ABROAD_P2', 'Documents', 'Prepare application documents', 'Draft key documents and collect materials for applications.', 1, 0,
   'Draft personal statement outline\nCollect transcripts and certificates\nAsk for recommendation letters',
   'Keep versions organized and reviewed.', 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1204, 'ABROAD', 'ABROAD_P3', 'Submit', 'Submit applications', 'Submit applications and track decisions and follow-ups.', 2, 0,
   'Finalize program list\nSubmit applications\nTrack status and deadlines',
   'Create a deadline calendar.', 4, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO t_decision_school_profile (id, name, track, region, tier_label, is_active, created_at, updated_at)
VALUES
  (5001, 'Guangdong Exam University', 'EXAM', 'Guangdong', 'Tier A', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (5002, 'Shanghai Exam Institute', 'EXAM', 'Shanghai', 'Tier S', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (5003, 'Beijing Exam College', 'EXAM', 'Beijing', 'Tier A', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (6001, 'Pacific Abroad University', 'ABROAD', 'US West', 'Top 50', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (6002, 'Northern Abroad Institute', 'ABROAD', 'Canada', 'Top 100', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (6003, 'European Abroad College', 'ABROAD', 'EU', 'Top 80', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO t_decision_school_metric_definition (
  id, track, metric_code, metric_label, metric_unit, value_type, chartable, metric_order, is_active, created_at, updated_at
)
VALUES
  (7001, 'EXAM', 'COST_MONTHLY', 'Monthly Cost', 'CNY', 'NUMBER', 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (7002, 'EXAM', 'REPUTATION', 'Reputation', 'score', 'NUMBER', 1, 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (7003, 'EXAM', 'ADMISSION_RATE', 'Admission Rate', '%', 'PERCENT', 1, 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (7004, 'EXAM', 'NOTES', 'Notes', NULL, 'TEXT', 0, 4, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

  (8001, 'ABROAD', 'TUITION_YEARLY', 'Yearly Tuition', 'USD', 'NUMBER', 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (8002, 'ABROAD', 'RANKING', 'Ranking', 'rank', 'NUMBER', 1, 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (8003, 'ABROAD', 'LANGUAGE_REQ', 'Language Requirement', NULL, 'TEXT', 0, 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (8004, 'ABROAD', 'VISA_DIFFICULTY', 'Visa Difficulty', 'score', 'NUMBER', 1, 4, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO t_decision_school_metric (id, school_id, metric_code, metric_value, created_at, updated_at)
VALUES
  (9001, 5001, 'COST_MONTHLY', '3200', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9002, 5001, 'REPUTATION', '78', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9003, 5001, 'ADMISSION_RATE', '24', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9004, 5001, 'NOTES', 'Strong math department.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

  (9011, 5002, 'COST_MONTHLY', '5200', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9012, 5002, 'REPUTATION', '92', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9013, 5002, 'ADMISSION_RATE', '18', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9014, 5002, 'NOTES', 'Competitive but strong alumni.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

  (9021, 5003, 'COST_MONTHLY', '4100', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9022, 5003, 'REPUTATION', '85', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9023, 5003, 'NOTES', 'Admission rate varies by major.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

  (9101, 6001, 'TUITION_YEARLY', '32000', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9102, 6001, 'RANKING', '35', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9103, 6001, 'LANGUAGE_REQ', 'IELTS 7.0', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9104, 6001, 'VISA_DIFFICULTY', '60', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

  (9111, 6002, 'TUITION_YEARLY', '24000', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9112, 6002, 'RANKING', '90', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9113, 6002, 'LANGUAGE_REQ', 'IELTS 6.5', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9114, 6002, 'VISA_DIFFICULTY', '40', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

  (9121, 6003, 'TUITION_YEARLY', '28000', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9122, 6003, 'RANKING', '65', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9123, 6003, 'VISA_DIFFICULTY', '55', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
