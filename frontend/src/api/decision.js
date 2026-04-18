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

export async function listDecisionSchools(params) {
  const { data } = await http.get("/decision/schools", { params });
  return data.data;
}

export async function compareDecisionSchools(payload) {
  const { data } = await http.post("/decision/schools/compare", payload);
  return data.data;
}
