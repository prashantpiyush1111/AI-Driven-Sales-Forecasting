# Frontend integration handoff

The UI currently runs on clearly labeled sample data in `src/data/demoData.js`. Replace those demo imports with the service functions in `src/services/` only after the backend routes and payloads are agreed. The browser should call the Spring Boot API; Spring Boot owns persistence, authorization, KPI aggregation, and calls into the ML and RAG services.

## Backend team: API decisions needed

Please publish or confirm the following before frontend wiring:

- Base path, local port, CORS policy, and whether the Vite `/api` proxy is the expected local setup.
- Authentication flow: login/register paths, token lifetime and refresh/logout behavior, role names, and permitted routes.
- Sales list/create path, page numbering convention, sort syntax, filter names, and response envelope. The current entity fields are `id`, `productName`, `category`, `region`, `quantity`, `revenue`, `saleDate`, and `createdAt`.
- Forecast list/latest paths, filtering by product and date range, and whether results are paginated.
- Dashboard/KPI and analytics aggregation endpoints, including the comparison period used for trend percentages.
- Standard error response shape and validation behavior.

Suggested query filters are `from`, `to`, `region`, `productName`, `page`, `size`, and `sort`; these are proposals, not an implemented backend contract.

## ML team: forecast payload decisions needed

The existing forecast entity supports `productName`, `forecastDate`, `predictedQuantity`, `predictedRevenue`, `confidenceScore`, `modelVersion`, and `createdAt`. Please confirm:

- Whether confidence is a 0–1 fraction or 0–100 percentage.
- Whether forecast dates are points or period start/end dates, and the supported horizon/granularity.
- Whether outputs include lower/upper prediction bounds, model status, run ID, and generation timestamp.
- How missing/invalid input history and model failures are reported.

Spring Boot should expose a stable response contract to the UI and translate any ML service payloads; the UI should not depend on internal ML endpoints.

## Candidate UI response shapes

These shapes make the current screens straightforward to wire; adjust them jointly before freezing OpenAPI:

```json
{
  "summary": {
    "totalRevenue": 0,
    "unitsSold": 0,
    "averageMonthlyRevenue": 0,
    "forecastRevenue": 0,
    "revenueChangePercent": null,
    "unitsChangePercent": null
  },
  "salesSeries": [{ "period": "2026-11", "actualRevenue": 0, "forecastRevenue": null }],
  "forecasts": [{
    "forecastDate": "2026-12-01",
    "predictedQuantity": 0,
    "predictedRevenue": 0,
    "confidenceScore": 0,
    "modelVersion": ""
  }]
}
```

The dashboard trend, product/region breakdown, sales table, forecast detail, and assistant screens already have demo states. The assistant stays visibly unavailable until the RAG/backend chat endpoint can answer with source context and errors in a defined format.
