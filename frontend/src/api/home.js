import http from "./http.js";

export async function getHomeSummary() {
  const { data } = await http.get("/home/summary");
  return data.data;
}
