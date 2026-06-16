INSERT INTO t_user (id, phone, nickname, real_name, role, status, verification_status, student_id, created_at, updated_at)
VALUES
  (1, '13800000000', '平台管理员', '管理员用户', 'ADMIN', 'ACTIVE', 'UNVERIFIED', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, '13800000001', '普通用户', '普通用户', 'USER', 'ACTIVE', 'UNVERIFIED', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (3, '13800000002', '认证用户', '认证用户', 'USER', 'ACTIVE', 'VERIFIED', '20260001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (4, '13933334444', '新普通用户', '新用户', 'USER', 'ACTIVE', 'UNVERIFIED', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO t_community_post (
  id, author_id, tag, title, content, status, like_count, comment_count, favorite_count,
  is_experience_post, experience_target_label, experience_outcome_label,
  experience_timeline_summary, experience_action_summary, created_at, updated_at
)
VALUES
  (
    1, 2, 'CAREER', '实习求职时间线记录', '整理的实习与Offer准备步骤。', 'PUBLISHED', 2, 0, 1,
    0, NULL, NULL, NULL, NULL,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  ),
  (
    2, 3, 'EXAM', '考研规划检查清单', '一份精简的目标院校规划与复习节奏清单。', 'PUBLISHED', 1, 0, 0,
    0, NULL, NULL, NULL, NULL,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  ),
  (
    3, 2, 'ABROAD', '留学语言备考入门', '关于语言考试与申请时间规划的基础准备大纲。', 'PUBLISHED', 0, 0, 0,
    1, '雅思7.5分冲刺', '模考成绩从6.0提升至7.5',
    '第1个月打基础，第2个月限时训练，第3个月全套模考',
    '坚持整理错题本、每日口语练习、每周一次全套模考。',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  );

INSERT INTO t_community_comment (
  id, post_id, author_id, parent_comment_id, reply_to_user_id, content, status, created_at, updated_at
)
VALUES
  (
    1, 1, 2, NULL, NULL, '欢迎在此分享你的面试准备里程碑。', 'VISIBLE',
    TIMESTAMPADD(MINUTE, -2, CURRENT_TIMESTAMP), TIMESTAMPADD(MINUTE, -2, CURRENT_TIMESTAMP)
  ),
  (
    2, 1, 3, 1, 2, '我每周进行一次模拟面试，效果非常显著。', 'VISIBLE',
    TIMESTAMPADD(MINUTE, -1, CURRENT_TIMESTAMP), TIMESTAMPADD(MINUTE, -1, CURRENT_TIMESTAMP)
  );

INSERT INTO t_resource_item (
  id, title, category, summary, description, status, uploader_id, reviewed_by, reject_reason,
  file_name, file_ext, content_type, file_size, storage_key, download_count, favorite_count,
  published_at, reviewed_at, created_at, updated_at
)
VALUES
  (
    1, '2026年求职简历模板包', 'RESUME_TEMPLATE',
    '适用于实习和校招申请的精简简历入门礼包。',
    '包含一页纸简历结构、命名建议和投递自查清单。',
    'PUBLISHED', 3, 1, NULL,
    'resume-template-pack.pdf', 'pdf', 'application/pdf', 524288,
    'seed/2026/04/resume-template-pack.pdf', 12, 2,
    TIMESTAMPADD(DAY, -4, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -4, CURRENT_TIMESTAMP),
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  ),
  (
    2, '校招面试经验笔记合集', 'INTERVIEW_EXPERIENCE',
    '结构化的校招常见面试问题与应答套路合集。',
    '重点关注后端、产品和运营岗位的面试复盘模板。',
    'PUBLISHED', 2, 1, NULL,
    'interview-experience-notes.zip', 'zip', 'application/zip', 1048576,
    'seed/2026/04/interview-experience-notes.zip', 7, 1,
    TIMESTAMPADD(DAY, -2, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -2, CURRENT_TIMESTAMP),
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  ),
  (
    3, '雅思写作专项训练册', 'LANGUAGE_TEST',
    '已上传的训练册，等待审核后即可公开展示。',
    '包含写作练习的话题分组和每周训练页。',
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
    1, '后端开发实习生', '未来校园科技', '深圳', 'INTERNSHIP', 'BACHELOR', '官网',
    'https://jobs.example.com/future-campus-tech/backend-intern',
    '参与 Spring Boot 服务、后台工具和学生端模块建设。',
    '加入后端研发小组，负责学生成长平台的接口开发、数据整理和管理端能力支持。',
    TIMESTAMPADD(DAY, 20, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -3, CURRENT_TIMESTAMP), 'PUBLISHED', 1, 1,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  ),
  (
    2, '校园招聘产品助理', '北湾教育', '广州', 'CAMPUS', 'BACHELOR', '企业微信',
    'https://jobs.example.com/north-bay-education/campus-pm',
    '协助校招活动、学生调研和跨团队需求推进。',
    '负责校招项目排期、内容协调和基础数据分析，帮助团队优化招聘转化。',
    TIMESTAMPADD(DAY, 30, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP), 'PUBLISHED', 1, 1,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  ),
  (
    3, 'AI 研究助理', '启澜实验室', '深圳', 'FULL_TIME', 'MASTER', '内推',
    'https://jobs.example.com/delta-lab/ai-research-assistant',
    '待审核的 AI 研究助理岗位草稿。',
    '支持模型评测、实验记录整理和研究资料归档，协助团队形成阶段性结论。',
    TIMESTAMPADD(DAY, 45, CURRENT_TIMESTAMP), NULL, 'DRAFT', 1, 1,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  );

INSERT INTO t_decision_assessment_question (id, code, prompt, description, display_order, is_active, created_at, updated_at)
VALUES
  (1, 'DECISION_Q1', '目前哪种结果对你来说最重要？', '选择最符合你当前优先级的一项。', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 'DECISION_Q2', '你更喜欢如何衡量你的进度？', '选择让你觉得最受鼓舞且最现实的方式。', 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (3, 'DECISION_Q3', '你每周有多少可支配的专注时间？', '请诚实填写，以确保推荐计划具有实际可行性。', 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (4, 'DECISION_Q4', '你能够容忍哪种程度的不确定性？', '不同的发展路径伴随不同类型的风险与模糊性。', 4, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (5, 'DECISION_Q5', '哪种学习风格最适合你？', '你偏好的学习模式会影响你能坚持多久。', 5, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (6, 'DECISION_Q6', '你需要在什么时候确定清晰的下一步规划？', '明确的截止期限有助于选择合适节奏的发展路径。', 6, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO t_decision_assessment_option (
  id, question_id, code, label, description, display_order,
  career_score, exam_score, abroad_score, is_active, created_at, updated_at
)
VALUES
  (11, 1, 'Q1_A', '提升考试成绩', '优先考虑系统性学习和分数提升。', 1, 0, 3, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (12, 1, 'Q1_B', '尽快拿到工作录用通知', '优先考虑就业竞争力和面试表现。', 2, 3, 0, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (13, 1, 'Q1_C', '准备出国留学', '优先考虑语言考试和留学申请。', 3, 0, 0, 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (14, 1, 'Q1_D', '保持多种选择', '在探索过程中倾向于一份平衡的计划。', 4, 1, 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

  (21, 2, 'Q2_A', '构建作品集并交付项目', '进度通过产出和迭代清晰可见。', 1, 3, 0, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (22, 2, 'Q2_B', '达到明确的分数目标', '进度通过量化的分数展现。', 2, 0, 3, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (23, 2, 'Q2_C', '完成申请材料和文书', '进度通过已提交的里程碑体现。', 3, 0, 0, 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (24, 2, 'Q2_D', '小步快跑的综合成果', '进度来自产出和学习习惯的结合。', 4, 1, 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

  (31, 3, 'Q3_A', '10-15 小时', '时间充裕，适合制定系统化的复习计划。', 1, 0, 3, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (32, 3, 'Q3_B', '5-8 小时', '更适合进行针对性的面试练习和小项目。', 2, 3, 0, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (33, 3, 'Q3_C', '15-20 小时', '时间非常充裕，适合备考语言及准备申请材料。', 3, 0, 0, 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (34, 3, 'Q3_D', '每周情况不同', '需要更加灵活的学习安排。', 4, 1, 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

  (41, 4, 'Q4_A', '更倾向于可预测的里程碑', '在有清晰检查点和反馈的环境下效率最高。', 1, 0, 3, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (42, 4, 'Q4_B', '偏好模糊环境下的快速迭代', '能适应需求变化和快速迭代周期。', 2, 3, 0, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (43, 4, 'Q4_C', '偏好长线规划', '能妥善管理更长的时间跨度与文书流程。', 3, 0, 0, 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (44, 4, 'Q4_D', '可以接受一定的不确定性', '能够通过平衡的计划进行适应。', 4, 1, 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

  (51, 5, 'Q5_A', '通过历年真题和刷题来练习', '重复和反馈能有效帮助我提高。', 1, 0, 3, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (52, 5, 'Q5_B', '通过构建真实项目来学习', '实际项目和任务更有助于我掌握知识。', 2, 3, 0, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (53, 5, 'Q5_C', '通过阅读和写作来学习', '文档、论文和规划是我的强项。', 3, 0, 0, 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (54, 5, 'Q5_D', '混合搭配', '在不同的学习模式中寻求多样性。', 4, 1, 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

  (61, 6, 'Q6_A', '1-2 个月内', '时间较短；选择结构化的近期计划。', 1, 0, 3, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (62, 6, 'Q6_B', '3-6 个月内', '中期时间线；可容纳面试和项目实战。', 2, 3, 0, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (63, 6, 'Q6_C', '6-12 个月内', '长期时间线；可容纳申请和各类考试。', 3, 0, 0, 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (64, 6, 'Q6_D', '没有固定截止期限', '选择一份平衡的探索性计划。', 4, 1, 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO t_decision_timeline_milestone (
  id, track, phase_code, phase_label, title, summary,
  offset_months, offset_days, action_checklist, resource_hint,
  display_order, is_active, created_at, updated_at
)
VALUES
  (1001, 'EXAM', 'EXAM_P0', '基线评估', '确定目标与基线', '选择目标院校，明确分数目标，并测算当前的基线水平。', 0, 0,
   '挑选1-2所目标院校及考试科目\n收集最近2年的考试真题\n进行一次基线模考以评估薄弱环节',
   '建议从简单的周计划 and 2次模拟开始。', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1002, 'EXAM', 'EXAM_P1', '日常复习', '建立每周复习常规', '将基线评估转化为包含练习与复盘的、可重复的每周复习常规。', 0, 14,
   '制定每周时间表（学习模块+复盘）\n每日针对薄弱主题进行专项练习\n复习错题并更新笔记',
   '利用历年真题，并准备一个错题本。', 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1003, 'EXAM', 'EXAM_P2', '模拟测试', '增加模考频率', '提高模拟考试的频率，并稳定分数区间。', 1, 0,
   '每周进行一次完整的模拟考试\n按主题追踪分数情况\n根据上一次模考调整训练重点',
   '记录指标：主题正确率、每部分答题时间。', 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1004, 'EXAM', 'EXAM_P3', '冲刺复习', '最后冲刺与复盘', '在时间压力下，专注于高性价比的考点复盘与执行。', 2, 0,
   '优先复习高性价比主题\n在限时条件下进行练习\n保持充足睡眠并做好恢复规划',
   '减少新知识的学习，专注于临场发挥。', 4, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

  (1101, 'CAREER', 'CAREER_P0', '方向定位', '明确方向与岗位', '确定岗位范围，并结合个人背景制定切实可行的计划。', 0, 0,
   '选择1-2个重点关注的岗位类型\n列出3个示例招聘岗位\n写下自己目前存在的技能差距',
   '利用岗位描述来定义所需掌握的技能。', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1102, 'CAREER', 'CAREER_P1', '项目沉淀', '完成技能支撑项目', '交付一个能展现目标岗位核心技能的小型项目。', 0, 21,
   '定义一个小型的项目范围\n交付第一个可用版本\n撰写简短的README和演示笔记',
   '优先考虑与招聘需求相契合的项目。', 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1103, 'CAREER', 'CAREER_P2', '面试实战', '开始面试练习', '练习核心面试问题，并模拟真实的面试场景。', 1, 0,
   '准备一份自我介绍\n练习20个常见面试问题\n与同学/同行进行2次模拟面试',
   '记录错误并每周进行迭代。', 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1104, 'CAREER', 'CAREER_P3', '投递反馈', '持续投递与迭代', '保持稳定的投递节奏，并根据反馈和面试结果进行优化。', 2, 0,
   '提交10份简历投递\n跟进并追踪投递状态\n根据面试反馈精简修改简历',
   '使用简单的漏斗追踪工具。', 4, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

  (1201, 'ABROAD', 'ABROAD_P0', '初期规划', '选择地区并评估语言水平', '明确目标国家或地区，并测算语言能力的基线。', 0, 0,
   '挑选1-2个目标国家或地区\n选择心仪的专业项目\n进行一次语言能力基线测试',
   '尽早启动申请材料核对清单。', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1202, 'ABROAD', 'ABROAD_P1', '语言备考', '建立语言学习常规', '建立语言提升和熟悉题型的日常学习习惯。', 0, 28,
   '日常听力与阅读常规训练\n每周进行一次写作练习\n每2周进行一次完整的模拟测试',
   '习惯贵在坚持，持之以恒优于短期突击。', 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1203, 'ABROAD', 'ABROAD_P2', '文书筹备', '准备申请文书材料', '撰写关键申请材料并收集所需证明文件。', 1, 0,
   '撰写个人陈述(PS)大纲\n收集成绩单和各项证书\n联系推荐人撰写推荐信',
   '妥善整理各个材料的版本并做好复核。', 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1204, 'ABROAD', 'ABROAD_P3', '正式申请', '提交申请并跟进', '正式提交申请，并追踪审批状态及后续反馈。', 2, 0,
   '最终确定申请项目名单\n提交在线申请\n跟踪审核进度和截止日期',
   '制作一个专属的申请截止时间日历。', 4, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO t_decision_school_profile (id, name, track, region, tier_label, is_active, created_at, updated_at)
VALUES
  (5001, '广东考研大学', 'EXAM', '广东', 'Tier A', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (5002, '上海考研学院', 'EXAM', '上海', 'Tier S', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (5003, '北京考研高等专科学校', 'EXAM', '北京', 'Tier A', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (6001, '太平洋留学大学', 'ABROAD', '美西', '前50', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (6002, '北方留学学院', 'ABROAD', '加拿大', '前100', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (6003, '欧洲留学学校', 'ABROAD', '欧盟', '前80', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO t_decision_school_metric_definition (
  id, track, metric_code, metric_label, metric_unit, value_type, chartable, metric_order, is_active, created_at, updated_at
)
VALUES
  (7001, 'EXAM', 'COST_MONTHLY', '月度花费', '元', 'NUMBER', 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (7002, 'EXAM', 'REPUTATION', '院校声誉', '分', 'NUMBER', 1, 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (7003, 'EXAM', 'ADMISSION_RATE', '录取率', '%', 'PERCENT', 1, 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (7004, 'EXAM', 'NOTES', '备注', NULL, 'TEXT', 0, 4, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

  (8001, 'ABROAD', 'TUITION_YEARLY', '年度学费', '美元', 'NUMBER', 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (8002, 'ABROAD', 'RANKING', '排名', '名', 'NUMBER', 1, 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (8003, 'ABROAD', 'LANGUAGE_REQ', '语言要求', NULL, 'TEXT', 0, 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (8004, 'ABROAD', 'VISA_DIFFICULTY', '签证难度', '分', 'NUMBER', 1, 4, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO t_decision_school_metric (id, school_id, metric_code, metric_value, created_at, updated_at)
VALUES
  (9001, 5001, 'COST_MONTHLY', '3200', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9002, 5001, 'REPUTATION', '78', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9003, 5001, 'ADMISSION_RATE', '24', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9004, 5001, 'NOTES', '数学系实力雄厚。', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

  (9011, 5002, 'COST_MONTHLY', '5200', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9012, 5002, 'REPUTATION', '92', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9013, 5002, 'ADMISSION_RATE', '18', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9014, 5002, 'NOTES', '竞争激烈，但校友资源非常强大。', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

  (9021, 5003, 'COST_MONTHLY', '4100', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9022, 5003, 'REPUTATION', '85', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9023, 5003, 'NOTES', '各专业录取率差异较大。', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

  (9101, 6001, 'TUITION_YEARLY', '32000', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9102, 6001, 'RANKING', '35', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9103, 6001, 'LANGUAGE_REQ', '雅思 7.0', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9104, 6001, 'VISA_DIFFICULTY', '60', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

  (9111, 6002, 'TUITION_YEARLY', '24000', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9112, 6002, 'RANKING', '90', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9113, 6002, 'LANGUAGE_REQ', '雅思 6.5', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9114, 6002, 'VISA_DIFFICULTY', '40', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

  (9121, 6003, 'TUITION_YEARLY', '28000', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9122, 6003, 'RANKING', '65', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9123, 6003, 'VISA_DIFFICULTY', '55', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
