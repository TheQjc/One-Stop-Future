import http from "./http.js";

export async function getMyApplications() {
  const { data } = await http.get("/applications/mine");
  return data.data;
}
