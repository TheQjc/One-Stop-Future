import axios from "axios";

const TOKEN_KEY = "one-stop-future-token";
const PROFILE_KEY = "one-stop-future-profile";

const http = axios.create({
  baseURL: "/api",
  timeout: 8000,
});

function clearPersistedAuth() {
  window.localStorage.removeItem(TOKEN_KEY);
  window.localStorage.removeItem(PROFILE_KEY);
}

function redirectToLogin() {
  if (typeof window === "undefined") {
    return;
  }

  if (window.location.pathname !== "/login") {
    window.history.replaceState({}, "", "/login");
  }
}

http.interceptors.request.use((config) => {
  const token = window.localStorage.getItem(TOKEN_KEY);

  if (token) {
    config.headers = config.headers || {};
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

http.interceptors.response.use(
  (response) => {
    const payload = response.data;

    if (payload && typeof payload.code === "number" && payload.code !== 200) {
      const requestError = new Error(payload.message || "request failed");
      requestError.code = payload.code;

      if (payload.code === 401) {
        clearPersistedAuth();
        redirectToLogin();
      }

      return Promise.reject(requestError);
    }

    return response;
  },
  (error) => {
    const status = error.response?.status;
    const payload = error.response?.data || {};
    const requestError = new Error(payload.message || error.message || "request failed");
    requestError.code = payload.code ?? status ?? 500;

    if (requestError.code === 401) {
      clearPersistedAuth();
      redirectToLogin();
    }

    return Promise.reject(requestError);
  },
);

export default http;
