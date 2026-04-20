import http from "./http.js";

const preferMock = import.meta.env.MODE === "test";
const APPLICATION_KEY = "one-stop-future-demo-verification-applications";
const USER_KEY = "one-stop-future-demo-users";
const PROFILE_KEY = "one-stop-future-profile";
const NOTIFICATION_KEY = "one-stop-future-demo-notifications";

const defaultApplications = [
  {
    id: 5001,
    userId: 2,
    realName: "Normal User",
    studentId: "20260009",
    status: "PENDING",
    rejectReason: null,
    createdAt: "2026-04-15T08:30:00",
    reviewedAt: null,
  },
  {
    id: 5002,
    userId: 3,
    realName: "Verified User",
    studentId: "20260001",
    status: "APPROVED",
    rejectReason: null,
    createdAt: "2026-04-14T10:00:00",
    reviewedAt: "2026-04-15T09:00:00",
  },
];

function readJson(key, fallback) {
  try {
    const raw = window.localStorage.getItem(key);
    return raw ? JSON.parse(raw) : fallback;
  } catch (error) {
    return fallback;
  }
}

function writeJson(key, value) {
  window.localStorage.setItem(key, JSON.stringify(value));
}

function readApplications() {
  const stored = readJson(APPLICATION_KEY, null);

  if (stored) {
    return stored;
  }

  writeJson(APPLICATION_KEY, defaultApplications);
  return [...defaultApplications];
}

function writeApplications(applications) {
  writeJson(APPLICATION_KEY, applications);
}

function readUsers() {
  return readJson(USER_KEY, []);
}

function writeUsers(users) {
  writeJson(USER_KEY, users);
}

function readProfile() {
  return readJson(PROFILE_KEY, null);
}

function writeProfile(profile) {
  writeJson(PROFILE_KEY, profile);
}

function readNotifications() {
  return readJson(NOTIFICATION_KEY, []);
}

function writeNotifications(notifications) {
  writeJson(NOTIFICATION_KEY, notifications);
}

function currentIsoString() {
  return new Date().toISOString();
}

function toSummary(application, users) {
  const applicant = users.find((item) => item.id === application.userId);

  return {
    id: application.id,
    userId: application.userId,
    applicantNickname: applicant?.nickname || "未知用户",
    realName: application.realName,
    studentId: application.studentId,
    status: application.status,
    createdAt: application.createdAt,
  };
}

function buildDashboardPayload(applications, users) {
  const pendingApplications = applications
    .filter((item) => item.status === "PENDING")
    .sort((left, right) => new Date(right.createdAt) - new Date(left.createdAt));
  const reviewedToday = applications.filter((item) => {
    if (!item.reviewedAt) {
      return false;
    }

    return new Date(item.reviewedAt).toDateString() === new Date().toDateString();
  }).length;

  return {
    pendingCount: pendingApplications.length,
    reviewedToday,
    latestPendingApplications: pendingApplications.slice(0, 5).map((item) => toSummary(item, users)),
  };
}

function syncApplicantProfile(user) {
  const profile = readProfile();

  if (!profile || profile.id !== user.id) {
    return;
  }

  writeProfile({
    ...profile,
    realName: user.realName || "",
    studentId: user.studentId || null,
    verificationStatus: user.verificationStatus,
  });
}

function appendNotification(userId, type, title, content) {
  const notifications = readNotifications();

  notifications.push({
    id: Date.now(),
    userId,
    type,
    title,
    content,
    read: false,
    createdAt: currentIsoString(),
    readAt: null,
  });

  writeNotifications(notifications);
}

export async function getVerificationDashboard() {
  if (preferMock) {
    const applications = readApplications();
    const users = readUsers();
    return buildDashboardPayload(applications, users);
  }

  const { data } = await http.get("/admin/verifications/dashboard");
  return data.data;
}

export async function getAdminDashboardSummary() {
  const { data } = await http.get("/admin/dashboard/summary");
  return data.data;
}

export async function getAdminApplications() {
  const { data } = await http.get("/admin/applications");
  return data.data;
}

export async function downloadAdminApplicationResume(id) {
  const response = await http.get(`/admin/applications/${id}/resume/download`, {
    responseType: "blob",
  });

  const filename = extractFilename(response.headers["content-disposition"]) || `application-resume-${id}`;
  const objectUrl = window.URL.createObjectURL(response.data);
  const anchor = document.createElement("a");
  anchor.href = objectUrl;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  window.URL.revokeObjectURL(objectUrl);
  return filename;
}

export async function getVerificationApplications() {
  if (preferMock) {
    const applications = readApplications();
    const users = readUsers();

    return applications
      .slice()
      .sort((left, right) => new Date(right.createdAt) - new Date(left.createdAt))
      .map((item) => toSummary(item, users));
  }

  const { data } = await http.get("/admin/verifications");
  return data.data;
}

export async function reviewVerification(id, payload) {
  if (preferMock) {
    const applications = readApplications();
    const index = applications.findIndex((item) => item.id === id);

    if (index < 0) {
      throw new Error("认证申请不存在");
    }

    if (applications[index].status !== "PENDING") {
      throw new Error("当前申请不在待审核状态");
    }

    const action = payload.action?.trim().toUpperCase();
    const users = readUsers();
    const userIndex = users.findIndex((item) => item.id === applications[index].userId);

    if (userIndex < 0) {
      throw new Error("申请人不存在");
    }

    if (action === "APPROVE") {
      applications[index] = {
        ...applications[index],
        status: "APPROVED",
        rejectReason: null,
        reviewedAt: currentIsoString(),
      };

      users[userIndex] = {
        ...users[userIndex],
        realName: applications[index].realName,
        studentId: applications[index].studentId,
        verificationStatus: "VERIFIED",
      };

      appendNotification(
        users[userIndex].id,
        "VERIFICATION_APPROVED",
        "认证已通过",
        "你的学生认证已通过审核。",
      );
    } else if (action === "REJECT") {
      if (!payload.reason?.trim()) {
        throw new Error("驳回时必须填写原因");
      }

      applications[index] = {
        ...applications[index],
        status: "REJECTED",
        rejectReason: payload.reason.trim(),
        reviewedAt: currentIsoString(),
      };

      users[userIndex] = {
        ...users[userIndex],
        studentId: null,
        verificationStatus: "UNVERIFIED",
      };

      appendNotification(
        users[userIndex].id,
        "VERIFICATION_REJECTED",
        "认证已驳回",
        `你的学生认证已被驳回：${payload.reason.trim()}`,
      );
    } else {
      throw new Error("无效的审核动作");
    }

    writeApplications(applications);
    writeUsers(users);
    syncApplicantProfile(users[userIndex]);
    return true;
  }

  const { data } = await http.post(`/admin/verifications/${id}/review`, payload);
  return data.data;
}

export async function getAdminCommunityPosts() {
  const { data } = await http.get("/admin/community/posts");
  return data.data;
}

export async function hideCommunityPost(id) {
  const { data } = await http.post(`/admin/community/posts/${id}/hide`);
  return data.data;
}

export async function deleteCommunityPost(id) {
  const { data } = await http.post(`/admin/community/posts/${id}/delete`);
  return data.data;
}

export async function getAdminJobs() {
  const { data } = await http.get("/admin/jobs");
  return data.data;
}

export async function createAdminJob(payload) {
  const { data } = await http.post("/admin/jobs", payload);
  return data.data;
}

export async function updateAdminJob(id, payload) {
  const { data } = await http.put(`/admin/jobs/${id}`, payload);
  return data.data;
}

export async function publishAdminJob(id) {
  const { data } = await http.post(`/admin/jobs/${id}/publish`);
  return data.data;
}

export async function offlineAdminJob(id) {
  const { data } = await http.post(`/admin/jobs/${id}/offline`);
  return data.data;
}

export async function deleteAdminJob(id) {
  const { data } = await http.post(`/admin/jobs/${id}/delete`);
  return data.data;
}

export async function getAdminResources() {
  const { data } = await http.get("/admin/resources");
  return data.data;
}

export async function publishAdminResource(id) {
  const { data } = await http.post(`/admin/resources/${id}/publish`);
  return data.data;
}

export async function rejectAdminResource(id, payload) {
  const { data } = await http.post(`/admin/resources/${id}/reject`, payload);
  return data.data;
}

export async function offlineAdminResource(id) {
  const { data } = await http.post(`/admin/resources/${id}/offline`);
  return data.data;
}

export async function getAdminUsers() {
  const { data } = await http.get("/admin/users");
  return data.data;
}

export async function banAdminUser(id) {
  const { data } = await http.post(`/admin/users/${id}/ban`);
  return data.data;
}

export async function unbanAdminUser(id) {
  const { data } = await http.post(`/admin/users/${id}/unban`);
  return data.data;
}

function extractFilename(contentDisposition = "") {
  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1]);
  }

  const plainMatch = contentDisposition.match(/filename="?([^"]+)"?/i);
  return plainMatch?.[1] || "";
}
