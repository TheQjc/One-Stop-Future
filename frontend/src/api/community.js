import http from "./http.js";

export async function getCommunityPosts(params = {}) {
  const { data } = await http.get("/community/posts", { params });
  return data.data;
}

export async function getCommunityPostDetail(id) {
  const { data } = await http.get(`/community/posts/${id}`);
  return data.data;
}

export async function createCommunityPost(payload) {
  const { data } = await http.post("/community/posts", payload);
  return data.data;
}

export async function getMyCommunityPosts() {
  const { data } = await http.get("/community/posts/mine");
  return data.data;
}

export async function createCommunityComment(id, payload) {
  const { data } = await http.post(`/community/posts/${id}/comments`, payload);
  return data.data;
}

export async function likeCommunityPost(id) {
  const { data } = await http.post(`/community/posts/${id}/like`);
  return data.data;
}

export async function unlikeCommunityPost(id) {
  const { data } = await http.delete(`/community/posts/${id}/like`);
  return data.data;
}

export async function favoriteCommunityPost(id) {
  const { data } = await http.post(`/community/posts/${id}/favorite`);
  return data.data;
}

export async function unfavoriteCommunityPost(id) {
  const { data } = await http.delete(`/community/posts/${id}/favorite`);
  return data.data;
}

export async function getMyPostFavorites() {
  const { data } = await http.get("/users/me/favorites", {
    params: { type: "POST" },
  });
  return data.data;
}
