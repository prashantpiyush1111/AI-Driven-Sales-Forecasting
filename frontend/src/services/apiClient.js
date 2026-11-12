import axios from "axios";

import { AUTH_LOGOUT_EVENT, clearSession, getToken } from "../auth/session";

// In dev, "/api" goes through the Vite proxy to http://localhost:8080 (see vite.config.js).
// For a separately hosted backend, set VITE_API_URL (e.g. https://api.example.com/api).
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "/api",
  headers: {
    "Content-Type": "application/json",
  },
});

apiClient.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    // A 401 on a request that carried a token means the session expired or is invalid.
    // (A failed login has no token yet, so it does not trigger this.)
    if (error.response?.status === 401 && getToken()) {
      clearSession();
      window.dispatchEvent(new Event(AUTH_LOGOUT_EVENT));
    }
    return Promise.reject(error);
  }
);

export default apiClient;
