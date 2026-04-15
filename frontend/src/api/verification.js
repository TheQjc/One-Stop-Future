import http from "./http.js";

export async function submitVerification(payload) {
  const { data } = await http.post("/verifications", payload);
  return data.data;
}
