import apiClient from "./apiClient";

// The backend should proxy requests to the ML service; the browser should never call ML directly.
export const getForecasts = (params) => apiClient.get("/forecasts", { params });
export const getLatestForecast = (params) => apiClient.get("/forecasts/latest", { params });
