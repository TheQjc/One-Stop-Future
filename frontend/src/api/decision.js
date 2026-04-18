import http from "./http.js";

export async function listDecisionQuestions() {
  const { data } = await http.get("/decision/assessment/questions");
  return data.data;
}

export async function submitDecisionAnswers(payload) {
  const { data } = await http.post("/decision/assessment/submissions", payload);
  return data.data;
}

export async function getLatestDecisionResult() {
  const { data } = await http.get("/decision/assessment/latest");
  return data.data;
}

export async function getDecisionTimeline(params) {
  const { data } = await http.get("/decision/timeline", { params });
  return data.data;
}

