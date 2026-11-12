import apiClient from "./apiClient";

// Endpoint names follow the proposed frontend integration contract in INTEGRATION.md.
export const getDashboardSummary = (params) => apiClient.get("/dashboard/summary", { params });
export const getSalesSeries = (params) => apiClient.get("/analytics/sales-series", { params });
export const getProductBreakdown = (params) => apiClient.get("/analytics/products", { params });
export const getRegionBreakdown = (params) => apiClient.get("/analytics/regions", { params });
