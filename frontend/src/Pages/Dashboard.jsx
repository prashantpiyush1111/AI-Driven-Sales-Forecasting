import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Line } from "react-chartjs-2";
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, Filler, Tooltip, Legend } from "chart.js";
import { demoSales, demoForecast } from "../data/demoData";
import PageHeading from "../components/common/PageHeading";
import Notice from "../components/common/Notice";
import SelectFilter from "../components/common/SelectFilter";
import KpiCard from "../components/common/KpiCard";
import RecentSales from "../components/common/RecentSales";

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Filler, Tooltip, Legend);

const money = (amount) => new Intl.NumberFormat("en-IN", {
  style: "currency", currency: "INR", maximumFractionDigits: 0,
}).format(amount || 0);
const shortMoney = (amount) => new Intl.NumberFormat("en-IN", {
  notation: "compact", maximumFractionDigits: 1,
}).format(amount || 0);

function createChartOptions() {
  return {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: { callbacks: { label: (item) => ` ${money(item.raw)}` } },
    },
    scales: {
      x: { grid: { display: false }, border: { display: false }, ticks: { color: "#8c96a8" } },
      y: { grid: { color: "#eef1f6" }, border: { display: false }, ticks: {
        color: "#8c96a8", callback: (value) => `₹${shortMoney(value)}`,
      } },
    },
  };
}

function getMonthlySales(rows) {
  const totals = {};
  rows.forEach((row) => {
    const month = row.saleDate.slice(0, 7);
    totals[month] = (totals[month] || 0) + row.revenue;
  });
  return Object.entries(totals).sort(([first], [second]) => first.localeCompare(second));
}

function getTopProducts(rows) {
  const totals = {};
  rows.forEach((row) => {
    totals[row.productName] = (totals[row.productName] || 0) + row.revenue;
  });
  return Object.entries(totals).sort((a, b) => b[1] - a[1]).slice(0, 4);
}

export default function Dashboard() {
  const [region, setRegion] = useState("All regions");
  const [period, setPeriod] = useState("12 months");
  const regions = ["All regions", ...new Set(demoSales.map((row) => row.region))];

  const monthlySales = useMemo(() => {
    const rows = region === "All regions"
      ? demoSales
      : demoSales.filter((row) => row.region === region);
    const monthCount = Number(period.split(" ")[0]);
    return getMonthlySales(rows).slice(-monthCount);
  }, [region, period]);

  const revenue = monthlySales.reduce((sum, [, amount]) => sum + amount, 0);
  const visibleMonths = new Set(monthlySales.map(([month]) => month));
  const units = demoSales
    .filter((row) => (region === "All regions" || row.region === region)
      && visibleMonths.has(row.saleDate.slice(0, 7)))
    .reduce((sum, row) => sum + row.quantity, 0);
  const forecastRevenue = demoForecast.reduce((sum, row) => sum + row.predictedRevenue, 0);
  const products = getTopProducts(demoSales);

  const chartData = {
    labels: monthlySales.map(([month]) => new Date(`${month}-01T00:00:00`).toLocaleDateString("en", { month: "short" })),
    datasets: [{
      label: "Revenue", data: monthlySales.map(([, amount]) => amount),
      borderColor: "#4568e8", backgroundColor: "rgba(69,104,232,.12)",
      fill: true, tension: 0.38, pointRadius: 3, borderWidth: 2.5,
    }],
  };

  return (
    <div className="page-content">
      <PageHeading
        title="Sales overview"
        description="A clear view of your sales performance and what may come next."
      >
        <SelectFilter label="Filter by region" value={region} options={regions} onChange={setRegion} />
        <SelectFilter label="Select time period" value={period}
          options={["12 months", "6 months", "3 months"]} onChange={setPeriod} />
      </PageHeading>

      <Notice>These figures use sample data while the API integration is in progress.</Notice>

      <section className="kpi-grid" aria-label="Key performance indicators">
        <KpiCard label="Total revenue" value={money(revenue)} detail="Across selected period" trend="↗ 12.8%" tone="blue" icon="₹" />
        <KpiCard label="Units sold" value={units.toLocaleString("en-IN")} detail="Recorded sales quantity" trend="↗ 8.4%" tone="violet" icon="▤" />
        <KpiCard label="Forecast revenue" value={money(forecastRevenue)} detail="Next 6 months · preview" trend="MODEL PREVIEW" tone="mint" icon="⌁" />
        <KpiCard label="Average per month" value={money(monthlySales.length ? revenue / monthlySales.length : 0)} detail="Selected period average" trend="↗ 4.2%" tone="amber" icon="◷" />
      </section>

      <section className="content-grid dashboard-grid">
        <div className="panel trend-panel">
          <div className="panel-heading">
            <div><h2>Revenue trend</h2><p>Monthly recorded sales</p></div>
            <span className="chart-legend"><i /> Actual revenue</span>
          </div>
          <div className="chart-wrap"><Line data={chartData} options={createChartOptions()} /></div>
        </div>

        <div className="panel products-panel">
          <div className="panel-heading">
            <div><h2>Top products</h2><p>By total revenue</p></div>
            <Link className="text-link" to="/analytics">View all <span>→</span></Link>
          </div>
          <div className="product-list">
            {products.map(([name, amount], index) => (
              <div className="product-row" key={name}>
                <span className={`rank rank-${index + 1}`}>{String(index + 1).padStart(2, "0")}</span>
                <span className="product-name">{name}</span>
                <strong>{money(amount)}</strong>
              </div>
            ))}
          </div>
          <div className="panel-footnote">Based on all available sample records</div>
        </div>
      </section>

      <RecentSales rows={[...demoSales].slice(-5).reverse()} />
    </div>
  );
}
