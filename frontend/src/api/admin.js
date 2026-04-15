import http from "./http.js";

export async function getVerificationDashboard() {
  const { data } = await http.get("/admin/verifications/dashboard");
  return data.data;
}

export async function getVerificationApplications() {
  const { data } = await http.get("/admin/verifications");
  return data.data;
}

export async function reviewVerification(id, payload) {
  const { data } = await http.post(`/admin/verifications/${id}/review`, payload);
  return data.data;
}
