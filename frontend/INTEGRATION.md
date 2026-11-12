# Frontend and API integration

The dashboard now reads live data from the Spring Boot API. The API base URL is `VITE_API_URL`; in local development the frontend calls `/api` and Vite forwards that to `http://localhost:8080`.

## Connected frontend flows

- Sign in, register, and profile verification use `/api/auth/login`, `/api/auth/register`, and `/api/auth/me`.
- Dashboard KPI, trend, top products, regional breakdown, filter options, and recent sales use the analytics and sales endpoints documented in `shared/api-contracts/backend.yaml`.
- `apiClient.js` attaches the saved JWT and clears an expired session after a 401 response.

The Sales, Analytics, Forecast, and AI Assistant screens still have demo or placeholder content where the page-specific API flow is not wired yet. The backend should remain the browser's single API boundary; it owns calls to the ML service.

## Backend and ML handoff

The backend API contract is maintained in `shared/api-contracts/backend.yaml`. The backend should keep its response fields stable for the frontend. Forecasts currently use `productName`, `forecastDate`, `predictedQuantity`, `predictedRevenue`, `confidenceScore`, and `modelVersion`.

Before wiring the remaining forecast and assistant screens, confirm:

- Whether `confidenceScore` is a 0–1 fraction or 0–100 percentage.
- Supported forecast horizon and granularity, and whether lower/upper prediction bounds are available.
- The RAG chat endpoint, request/response format, source context shape, and error behavior.

The UI should not call internal ML or RAG service endpoints directly.
