import axios from "axios";

const TOKEN_KEY = "one-stop-future-token";
const PROFILE_KEY = "one-stop-future-profile";

const http = axios.create({
  baseURL: "/api",
  timeout: 8000,
});

const DEFAULT_ERROR_MESSAGE = "请求失败，请稍后重试";

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

function shouldForceLogout(code, message) {
  return code === 401 || (code === 403 && ["account is banned", "账号已被封禁"].includes(message));
}

function shouldRedirectOnAuthFailure(config) {
  return config?.skipAuthRedirect !== true;
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
      const requestError = new Error(payload.message || DEFAULT_ERROR_MESSAGE);
      requestError.code = payload.code;
      requestError.data = payload.data ?? null;

      if (shouldForceLogout(payload.code, payload.message) && shouldRedirectOnAuthFailure(response.config)) {
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
    const requestError = new Error(payload.message || DEFAULT_ERROR_MESSAGE);
    requestError.code = payload.code ?? status ?? 500;
    requestError.data = payload.data ?? null;

    if (shouldForceLogout(requestError.code, requestError.message) && shouldRedirectOnAuthFailure(error.config)) {
      clearPersistedAuth();
      redirectToLogin();
    }

    return Promise.reject(requestError);
  },
);

export default http;
