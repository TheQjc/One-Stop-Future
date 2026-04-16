import http from "./http.js";

export async function getSearchResults(params = {}) {
  const { data } = await http.get("/search", { params });
  return data.data;
}
