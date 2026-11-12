import apiClient from "./apiClient";

// Backend normally returns a plain array; tolerate a { value: [...] } wrapper too.
const asList = (data) => {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.value)) return data.value;
  return [];
};

export const getOverview = async () => {
  const { data } = await apiClient.get("/analytics/overview");
  return data;
};

export const getTrend = async () => {
  const { data } = await apiClient.get("/analytics/trend");
  return asList(data);
};

export const getTopProducts = async () => {
  const { data } = await apiClient.get("/analytics/top-products");
  return asList(data);
};

export const getSalesByCategory = async () => {
  const { data } = await apiClient.get("/analytics/by-category");
  return asList(data);
};

export const getSalesByRegion = async () => {
  const { data } = await apiClient.get("/analytics/by-region");
  return asList(data);
};

export const getFilterOptions = async () => {
  const { data } = await apiClient.get("/sales/filters");
  return {
    products: data?.products ?? [],
    categories: data?.categories ?? [],
    regions: data?.regions ?? [],
  };
};
