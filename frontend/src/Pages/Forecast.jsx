import { useMemo, useState } from "react";
import { Line } from "react-chartjs-2";
import { demoForecast, demoSales } from "../data/demoData";
import PageHeading from "../components/common/PageHeading";
import Notice from "../components/common/Notice";
import SelectFilter from "../components/common/SelectFilter";
import "../styles/Forecast.css";

const money = (amount) => new Intl.NumberFormat("en-IN", {
  style: "currency", currency: "INR", maximumFractionDigits: 0,
}).format(amount || 0);
const shortMoney = (amount) => new Intl.NumberFormat("en-IN", {
  notation: "compact", maximumFractionDigits: 1,
}).format(amount || 0);

function makeForecastChart(sales, forecasts) {
  const monthlyTotals = {};
  sales.slice(-8).forEach((row) => {
    const month = row.saleDate.slice(0, 7);
    monthlyTotals[month] = (monthlyTotals[month] || 0) + row.revenue;
  });

  const history = Object.entries(monthlyTotals).sort(([a], [b]) => a.localeCompare(b));
  const labels = [
    ...history.map(([month]) => new Date(`${month}-01T00:00:00`).toLocaleDateString("en", { month: "short" })),
    ...forecasts.map((row) => new Date(`${row.forecastDate}T00:00:00`).toLocaleDateString("en", { month: "short" })),
  ];
  const actualValues = history.map(([, revenue]) => revenue);
  const forecastValues = [
    ...Array(Math.max(actualValues.length - 1, 0)).fill(null),
    actualValues.at(-1) || null,
    ...forecasts.map((row) => row.predictedRevenue),
  ];

  return {
    labels,
    datasets: [
      {
        label: "Recorded revenue",
        data: [...actualValues, ...Array(forecasts.length).fill(null)],
        borderColor: "#4568e8", backgroundColor: "rgba(69,104,232,.12)",
        fill: true, tension: 0.35, pointRadius: 3, borderWidth: 2.5,
      },
      {
        label: "Forecast revenue",
        data: forecastValues,
        borderColor: "#9a72e8", backgroundColor: "rgba(154,114,232,.08)",
        fill: true, tension: 0.35, pointRadius: 3, borderWidth: 2.5, borderDash: [6, 5],
      },
    ],
  };
}

function makeChartOptions() {
  return {
    responsive: true,
    maintainAspectRatio: false,
    interaction: { mode: "index", intersect: false },
    plugins: {
      legend: { position: "bottom", labels: { usePointStyle: true, pointStyle: "circle", padding: 20, color: "#667085" } },
      tooltip: { callbacks: { label: (item) => ` ${item.dataset.label}: ${money(item.raw)}` } },
    },
    scales: {
      x: { grid: { display: false }, border: { display: false }, ticks: { color: "#8c96a8" } },
      y: { grid: { color: "#eef1f6" }, border: { display: false }, ticks: { color: "#8c96a8", callback: (value) => `₹${shortMoney(value)}` } },
    },
  };
}

function ForecastTable({ rows }) {
  return (
    <section className="panel table-panel forecast-table">
      <div className="panel-heading">
        <div><h2>Forecast detail</h2><p>Monthly revenue and quantity projections</p></div>
      </div>
      <div className="table-scroll">
        <table>
          <thead><tr><th>FORECAST MONTH</th><th>PREDICTED UNITS</th><th>CONFIDENCE</th><th className="align-right">PREDICTED REVENUE</th></tr></thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.forecastDate}>
                <td><strong>{new Date(`${row.forecastDate}T00:00:00`).toLocaleDateString("en", { month: "long", year: "numeric" })}</strong></td>
                <td>{row.predictedQuantity}</td>
                <td>
                  <span className="confidence-meter">
                    <span className="confidence-track"><i style={{ width: `${row.confidenceScore}%` }} /></span>
                    {row.confidenceScore.toFixed(1)}%
                  </span>
                </td>
                <td className="align-right revenue-cell">{money(row.predictedRevenue)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

export default function Forecast() {
  const [horizon, setHorizon] = useState("6");
  const rows = demoForecast.slice(0, Number(horizon));
  const chartData = useMemo(() => makeForecastChart(demoSales, rows), [rows]);
  const totalRevenue = rows.reduce((sum, row) => sum + row.predictedRevenue, 0);
  const averageConfidence = rows.reduce((sum, row) => sum + row.confidenceScore, 0) / rows.length;
  const finalMonth = new Date(`${rows.at(-1).forecastDate}T00:00:00`)
    .toLocaleDateString("en", { month: "long", year: "numeric" });

  return (
    <div className="page-content forecast-page">
      <PageHeading eyebrow="MACHINE LEARNING" title="Sales forecast"
        description="Review the projected revenue trend and forecast confidence.">
        <SelectFilter label="Forecast horizon" value={horizon === "3" ? "Next 3 months" : "Next 6 months"}
          options={["Next 3 months", "Next 6 months"]}
          onChange={(value) => setHorizon(value === "Next 3 months" ? "3" : "6")} />
      </PageHeading>

      <Notice label="MODEL PREVIEW">
        Illustrative forecast data. Live model output and confidence will appear when the ML API is connected.
      </Notice>

      <section className="forecast-kpis">
        <div className="forecast-kpi"><span>Projected revenue</span><strong>{money(totalRevenue)}</strong><small>For the selected forecast horizon</small></div>
        <div className="forecast-kpi"><span>Average model confidence</span><strong>{averageConfidence.toFixed(1)}%</strong><small>Preview confidence score</small></div>
        <div className="forecast-kpi"><span>Forecast through</span><strong>{finalMonth}</strong><small>{rows.length} monthly periods</small></div>
      </section>

      <section className="panel forecast-chart-panel">
        <div className="panel-heading">
          <div><h2>Revenue: actual vs forecast</h2><p>Historical sales and expected revenue</p></div>
          <span className="model-version">Demo model · illustrative</span>
        </div>
        <div className="forecast-chart"><Line data={chartData} options={makeChartOptions()} /></div>
      </section>
      <ForecastTable rows={rows} />
    </div>
  );
}
