import axios from "axios";
import { AUTH_LOGOUT_EVENT, clearSession, getToken } from "../auth/session";

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "/api",
  headers: { "Content-Type": "application/json" },
  timeout: 15000,
});

apiClient.interceptors.request.use((config) => {
  const token = getToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && getToken()) {
      clearSession();
      window.dispatchEvent(new Event(AUTH_LOGOUT_EVENT));
    }
    return Promise.reject(error);
  }
);

export default apiClient;
