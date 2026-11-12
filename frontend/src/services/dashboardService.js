import apiClient from "./apiClient";

const asList = (data) => {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.value)) return data.value;
  return [];
};

export async function getOverview() {
  const { data } = await apiClient.get("/analytics/overview");
  return data;
}

export async function getTrend(params = {}) {
  const { data } = await apiClient.get("/analytics/trend", { params });
  return asList(data);
}

export async function getTopProducts(params = {}) {
  const { data } = await apiClient.get("/analytics/top-products", { params });
  return asList(data);
}

export async function getSalesByCategory(params = {}) {
  const { data } = await apiClient.get("/analytics/by-category", { params });
  return asList(data);
}

export async function getSalesByRegion(params = {}) {
  const { data } = await apiClient.get("/analytics/by-region", { params });
  return asList(data);
}

export async function getSalesSummary(params = {}) {
  const { data } = await apiClient.get("/sales/summary", { params });
  return data;
}

export async function getFilterOptions() {
  const { data } = await apiClient.get("/sales/filters");
  return {
    products: data?.products ?? [],
    categories: data?.categories ?? [],
    regions: data?.regions ?? [],
  };
}
