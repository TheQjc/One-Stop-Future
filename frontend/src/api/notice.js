import http from "./http.js";

const preferMock = import.meta.env.MODE === "test";

const seedNotices = [
  {
    id: 1,
    title: "关于本周教学楼开放时间调整的通知",
    category: "教学通知",
    author: "教务处",
    authorRole: "ADMIN",
    publishAt: "2026-04-15 08:30",
    status: "APPROVED",
    summary: "因周末设备巡检，周五晚间与周六上午部分教学楼开放时间有所调整，请师生提前规划。",
    content:
      "因周末设备巡检，主教学楼 A 区与实验楼 B 区的开放时间将调整为周五 21:00 关闭、周六 13:00 重新开放。请有课程或自习安排的师生提前调整计划，并关注现场指引。",
    isTop: true,
  },
  {
    id: 2,
    title: "春季社团展演志愿者招募启动",
    category: "校园活动",
    author: "学生工作部",
    authorRole: "TEACHER",
    publishAt: "2026-04-14 15:20",
    status: "PENDING",
    summary: "面向全校学生招募舞台协助、摄影记录与秩序维护志愿者，报名截至本周日。",
    content:
      "春季社团展演将于下周三晚在文体中心举行，现面向全校学生开放志愿者报名。岗位包括舞台协助、摄影记录、观众引导与秩序维护，欢迎有兴趣的同学报名参与。",
    isTop: false,
  },
  {
    id: 3,
    title: "校园网络维护窗口公告",
    category: "服务公告",
    author: "信息中心",
    authorRole: "ADMIN",
    publishAt: "2026-04-13 10:00",
    status: "APPROVED",
    summary: "本周四凌晨进行网络设备维护，宿舍区和图书馆部分网络服务将短时中断。",
    content:
      "为提升校园网络稳定性，信息中心将于周四 00:30 至 02:30 开展核心交换设备维护。期间宿舍区、图书馆和教学楼部分区域网络将出现短时中断。",
    isTop: false,
  },
  {
    id: 4,
    title: "青年教师公开课申报提醒",
    category: "教师事务",
    author: "教师发展中心",
    authorRole: "TEACHER",
    publishAt: "2026-04-12 14:10",
    status: "REJECTED",
    summary: "青年教师公开课申报本周截止，请尽快补充教学设计与课程说明材料。",
    content:
      "请有意参与本学期青年教师公开课的老师在周五 17:00 前完成在线申报，并上传课程说明、教学设计与课堂组织方案。",
    isTop: false,
  },
];

function readNotices() {
  const raw = window.localStorage.getItem("campus-demo-notices");

  if (!raw) {
    window.localStorage.setItem("campus-demo-notices", JSON.stringify(seedNotices));
    return [...seedNotices];
  }

  return JSON.parse(raw);
}

function writeNotices(notices) {
  window.localStorage.setItem("campus-demo-notices", JSON.stringify(notices));
}

function buildSummary(content) {
  return (content || "").replace(/\s+/g, " ").slice(0, 72);
}

function normalizeNotice(item) {
  return {
    id: item.id,
    title: item.title,
    category: item.category || "未分类",
    author: item.author || item.realName || (item.authorId ? `用户 #${item.authorId}` : "未知发布人"),
    authorRole: item.authorRole || item.role || "",
    publishAt: item.publishAt || item.createdAt || "",
    status: item.status || "PENDING",
    summary: item.summary || buildSummary(item.content),
    content: item.content || "",
    isTop: Boolean(item.isTop),
  };
}

function normalizeNoticePage(payload, params = {}) {
  const records = payload?.list || payload?.records || [];
  const page = Number(payload?.page || params.page || 1);
  const pageSize = Number(payload?.pageSize || payload?.size || params.pageSize || params.size || 3);

  return {
    list: records.map(normalizeNotice),
    total: Number(payload?.total || records.length),
    page,
    pageSize,
  };
}

export function categoryOptions() {
  return ["全部", "教学通知", "校园活动", "服务公告", "教师事务"];
}

export async function getNoticeList(params = {}) {
  if (preferMock) {
    const page = Number(params.page || 1);
    const pageSize = Number(params.pageSize || 3);
    const category = params.category || "全部";
    const keyword = (params.keyword || "").trim();
    const role = params.role || "";
    const notices = readNotices()
      .filter((item) => category === "全部" || item.category === category)
      .filter((item) => !keyword || item.title.includes(keyword) || item.summary.includes(keyword))
      .filter((item) => !role || params.includeAll || item.status === "APPROVED")
      .sort((a, b) => Number(b.isTop) - Number(a.isTop) || b.id - a.id);

    const start = (page - 1) * pageSize;
    return {
      list: notices.slice(start, start + pageSize),
      total: notices.length,
      page,
      pageSize,
    };
  }

  try {
    const backendParams = {
      page: params.page || 1,
      size: params.pageSize || params.size || 3,
    };

    if (params.category && params.category !== "全部") {
      backendParams.category = params.category;
    }

    const { data } = await http.get("/notices", { params: backendParams });
    return normalizeNoticePage(data.data, params);
  } catch (error) {
    const page = Number(params.page || 1);
    const pageSize = Number(params.pageSize || 3);
    const category = params.category || "全部";
    const keyword = (params.keyword || "").trim();
    const role = params.role || "";
    const notices = readNotices()
      .filter((item) => category === "全部" || item.category === category)
      .filter((item) => !keyword || item.title.includes(keyword) || item.summary.includes(keyword))
      .filter((item) => !role || params.includeAll || item.status === "APPROVED")
      .sort((a, b) => Number(b.isTop) - Number(a.isTop) || b.id - a.id);

    const start = (page - 1) * pageSize;
    return {
      list: notices.slice(start, start + pageSize),
      total: notices.length,
      page,
      pageSize,
    };
  }
}

export async function getNoticeDetail(id) {
  if (preferMock) {
    const notice = readNotices().find((item) => item.id === Number(id));

    if (!notice) {
      throw new Error("通知不存在");
    }

    return notice;
  }

  try {
    const { data } = await http.get(`/notices/${id}`);
    return normalizeNotice(data.data);
  } catch (error) {
    const notice = readNotices().find((item) => item.id === Number(id));

    if (!notice) {
      throw new Error("通知不存在");
    }

    return notice;
  }
}

export async function createNotice(payload, actor) {
  if (preferMock) {
    const notices = readNotices();
    const next = {
      id: Date.now(),
      title: payload.title,
      category: payload.category,
      author: actor?.realName || actor?.username || "教师用户",
      authorRole: actor?.role || "TEACHER",
      publishAt: new Date().toISOString().slice(0, 16).replace("T", " "),
      status: "PENDING",
      summary: payload.summary,
      content: payload.content,
      isTop: Boolean(payload.isTop),
    };

    notices.unshift(next);
    writeNotices(notices);
    return next;
  }

  try {
    const { data } = await http.post("/notices", payload);
    return data.data;
  } catch (error) {
    const notices = readNotices();
    const next = {
      id: Date.now(),
      title: payload.title,
      category: payload.category,
      author: actor?.realName || actor?.username || "教师用户",
      authorRole: actor?.role || "TEACHER",
      publishAt: new Date().toISOString().slice(0, 16).replace("T", " "),
      status: "PENDING",
      summary: payload.summary,
      content: payload.content,
      isTop: Boolean(payload.isTop),
    };

    notices.unshift(next);
    writeNotices(notices);
    return next;
  }
}

export async function updateNotice(id, payload) {
  if (preferMock) {
    const notices = readNotices();
    const index = notices.findIndex((item) => item.id === Number(id));

    if (index < 0) {
      throw new Error("通知不存在");
    }

    notices[index] = {
      ...notices[index],
      ...payload,
      status: "PENDING",
    };
    writeNotices(notices);
    return notices[index];
  }

  try {
    const { data } = await http.put(`/notices/${id}`, payload);
    return data.data;
  } catch (error) {
    const notices = readNotices();
    const index = notices.findIndex((item) => item.id === Number(id));

    if (index < 0) {
      throw new Error("通知不存在");
    }

    notices[index] = {
      ...notices[index],
      ...payload,
      status: "PENDING",
    };
    writeNotices(notices);
    return notices[index];
  }
}

export async function deleteNotice(id) {
  if (preferMock) {
    const notices = readNotices().filter((item) => item.id !== Number(id));
    writeNotices(notices);
    return true;
  }

  try {
    const { data } = await http.delete(`/notices/${id}`);
    return data.data;
  } catch (error) {
    const notices = readNotices().filter((item) => item.id !== Number(id));
    writeNotices(notices);
    return true;
  }
}

export async function reviewNotice(id, status) {
  if (preferMock) {
    const notices = readNotices();
    const index = notices.findIndex((item) => item.id === Number(id));

    if (index < 0) {
      throw new Error("通知不存在");
    }

    notices[index].status = status;
    writeNotices(notices);
    return notices[index];
  }

  try {
    const { data } = await http.post(`/notices/${id}/review`, { status });
    return data.data;
  } catch (error) {
    const notices = readNotices();
    const index = notices.findIndex((item) => item.id === Number(id));

    if (index < 0) {
      throw new Error("通知不存在");
    }

    notices[index].status = status;
    writeNotices(notices);
    return notices[index];
  }
}
