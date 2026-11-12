import { useEffect, useState } from "react";
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
} from "chart.js";
import { Line } from "react-chartjs-2";

import {
  getOverview,
  getTrend,
  getTopProducts,
  getSalesByRegion,
} from "../services/dashboardService";

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend
);

const RUPEE = "\u20B9";

const formatMoney = (value) =>
  `${RUPEE}${Number(value ?? 0).toLocaleString("en-IN", {
    maximumFractionDigits: 0,
  })}`;

const formatNumber = (value) => Number(value ?? 0).toLocaleString("en-IN");

const chartOptions = {
  responsive: true,
  plugins: {
    legend: { display: true },
    title: { display: true, text: "Revenue Trend" },
  },
};

function Dashboard() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError("");
      try {
        const [overview, trend, topProducts, byRegion] = await Promise.all([
          getOverview(),
          getTrend(),
          getTopProducts(),
          getSalesByRegion(),
        ]);
        if (!cancelled) {
          setData({ overview, trend, topProducts, byRegion });
        }
      } catch (err) {
        if (!cancelled) {
          setError(
            err.response?.data?.message ||
              "Dashboard data load nahi ho paya. Backend check karo."
          );
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, []);

  if (loading) {
    return (
      <div className="dashboard">
        <div className="dashboard-header">
          <h1>Sales Dashboard</h1>
          <p>Loading...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="dashboard">
        <div className="dashboard-header">
          <h1>Sales Dashboard</h1>
          <p style={{ color: "#b91c1c" }}>{error}</p>
        </div>
      </div>
    );
  }

  const { overview, trend, topProducts, byRegion } = data;

  const totalRevenue = Number(overview?.totalRevenue ?? 0);
  const totalQuantity = Number(overview?.totalQuantity ?? 0);
  const avgRevenuePerUnit =
    totalQuantity > 0 ? totalRevenue / totalQuantity : 0;

  const trendChartData = {
    labels: trend.map((point) => point.period),
    datasets: [
      {
        label: "Revenue",
        data: trend.map((point) => Number(point.revenue)),
        borderWidth: 3,
        tension: 0.4,
      },
    ],
  };

  return (
    <div className="dashboard">
      <div className="dashboard-header">
        <h1>Sales Dashboard</h1>
        <p>Monitor your sales performance and business insights.</p>
      </div>

      {/* KPI Cards */}
      <div className="stats-container">
        <div className="stat-card">
          <h3>Total Revenue</h3>
          <p>{formatMoney(totalRevenue)}</p>
          <span>{formatNumber(overview?.recordCount)} sales records</span>
        </div>

        <div className="stat-card">
          <h3>Units Sold</h3>
          <p>{formatNumber(totalQuantity)}</p>
          <span>{formatNumber(overview?.productCount)} products</span>
        </div>

        <div className="stat-card">
          <h3>Forecasted Revenue</h3>
          <p>{formatMoney(overview?.upcomingForecastRevenue)}</p>
          <span>
            {formatNumber(overview?.forecastCount)} forecasts generated
          </span>
        </div>

        <div className="stat-card">
          <h3>Avg. Revenue per Unit</h3>
          <p>{formatMoney(avgRevenuePerUnit)}</p>
          <span>Revenue / units sold</span>
        </div>
      </div>

      {/* Trend Chart */}
      <div className="chart-container">
        {trend.length > 0 ? (
          <Line data={trendChartData} options={chartOptions} />
        ) : (
          <p>Abhi trend ke liye sales data nahi hai.</p>
        )}
      </div>

      {/* Summary Section */}
      <div className="summary-container">
        <div className="summary-card">
          <h2>Top Products</h2>
          {topProducts.length === 0 && <p>No data</p>}
          {topProducts.map((item) => (
            <div className="summary-item" key={item.name}>
              <span>{item.name}</span>
              <strong>{formatMoney(item.revenue)}</strong>
            </div>
          ))}
        </div>

        <div className="summary-card">
          <h2>Sales by Region</h2>
          {byRegion.length === 0 && <p>No data</p>}
          {byRegion.map((item) => (
            <div className="summary-item" key={item.name}>
              <span>{item.name}</span>
              <strong>{formatMoney(item.revenue)}</strong>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

export default Dashboard;