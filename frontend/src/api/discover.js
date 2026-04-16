import http from "./http.js";

export async function getDiscoverResults(params) {
  const { data } = await http.get("/discover", { params });
  return data.data;
}
