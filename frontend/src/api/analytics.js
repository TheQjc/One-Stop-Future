import http from "./http.js";

export async function getAnalyticsSummary(params = {}) {
  const { data } = await http.get("/analytics/summary", { params });
  return data.data;
}
