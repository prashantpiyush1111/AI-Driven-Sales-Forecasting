import apiClient from "./apiClient";

export async function login(username, password) {
  const { data } = await apiClient.post("/auth/login", { username, password });
  return data;
}

export async function register(username, email, password) {
  const { data } = await apiClient.post("/auth/register", { username, email, password });
  return data;
}

export async function getProfile() {
  const { data } = await apiClient.get("/auth/me");
  return data;
}
