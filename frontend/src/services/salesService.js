import apiClient from "./apiClient";

export const getSalesRecords = (params) => apiClient.get("/sales", { params });
export const createSalesRecord = (record) => apiClient.post("/sales", record);
