import http from "./http.js";

export async function getJobs(params = {}) {
  const { data } = await http.get("/jobs", { params });
  return data.data;
}

export async function getJobDetail(id) {
  const { data } = await http.get(`/jobs/${id}`);
  return data.data;
}

export async function favoriteJob(id) {
  const { data } = await http.post(`/jobs/${id}/favorite`);
  return data.data;
}

export async function unfavoriteJob(id) {
  const { data } = await http.delete(`/jobs/${id}/favorite`);
  return data.data;
}

export async function getMyJobFavorites() {
  const { data } = await http.get("/users/me/favorites", {
    params: { type: "JOB" },
  });
  return data.data;
}
