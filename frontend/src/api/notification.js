import http from "./http.js";

export async function getNotifications() {
  const { data } = await http.get("/notifications");
  return data.data;
}

export async function markNotificationRead(id) {
  const { data } = await http.post(`/notifications/${id}/read`);
  return data.data;
}

export async function markAllNotificationsRead() {
  const { data } = await http.post("/notifications/read-all");
  return data.data;
}
