import { Bar } from "react-chartjs-2";
import { Chart as ChartJS, CategoryScale, LinearScale, BarElement, Tooltip, Legend } from "chart.js";
import { demoSales } from "../data/demoData";
import PageHeading from "../components/common/PageHeading";
import Notice from "../components/common/Notice";
import RecentSales from "../components/common/RecentSales";

ChartJS.register(CategoryScale, LinearScale, BarElement, Tooltip, Legend);

const money = (amount) => new Intl.NumberFormat("en-IN", {
  style: "currency", currency: "INR", maximumFractionDigits: 0,
}).format(amount || 0);

function getRevenueByRegion(rows) {
  const totals = {};
  rows.forEach((row) => {
    totals[row.region] = (totals[row.region] || 0) + row.revenue;
  });
  return Object.entries(totals).sort((a, b) => b[1] - a[1]);
}

function getUnitsByProduct(rows) {
  const totals = {};
  rows.forEach((row) => {
    totals[row.productName] = (totals[row.productName] || 0) + row.quantity;
  });
  return Object.entries(totals).sort((a, b) => b[1] - a[1]);
}

function ProductChart({ products }) {
  const data = {
    labels: products.map(([name]) => name),
    datasets: [{
      label: "Units",
      data: products.map(([, units]) => units),
      backgroundColor: ["#4568e8", "#8499f1", "#b3c1fb", "#dae1ff"],
      borderRadius: 5,
      barThickness: 24,
    }],
  };
  const options = {
    responsive: true,
    maintainAspectRatio: false,
    indexAxis: "y",
    plugins: { legend: { display: false } },
    scales: {
      x: { grid: { color: "#eef1f6" }, border: { display: false }, ticks: { color: "#8c96a8" } },
      y: { grid: { display: false }, border: { display: false }, ticks: { color: "#596579" } },
    },
  };
  return <div className="analytics-chart"><Bar data={data} options={options} /></div>;
}

function RegionBreakdown({ regions }) {
  const total = regions.reduce((sum, [, revenue]) => sum + revenue, 0);
  const highestRevenue = Math.max(...regions.map(([, revenue]) => revenue));

  return (
    <div className="region-list">
      {regions.map(([name, revenue], index) => (
        <div className="region-analytics-row" key={name}>
          <div className="region-analytics-label">
            <span className={`region-dot region-dot-${index % 4}`} />
            <strong>{name}</strong>
            <span>{((revenue / total) * 100).toFixed(1)}%</span>
          </div>
          <div className="progress-track">
            <i style={{ width: `${(revenue / highestRevenue) * 100}%` }} />
          </div>
          <small>{money(revenue)}</small>
        </div>
      ))}
    </div>
  );
}

export default function AnalyticsPage() {
  const regions = getRevenueByRegion(demoSales);
  const products = getUnitsByProduct(demoSales);

  return (
    <div className="page-content">
      <PageHeading eyebrow="BUSINESS INTELLIGENCE" title="Analytics"
        description="Explore product and regional patterns across recorded sales." />
      <Notice>Insights below use sample records and will use live KPI endpoints once available.</Notice>

      <section className="content-grid analytics-grid">
        <div className="panel">
          <div className="panel-heading">
            <div><h2>Units by product</h2><p>Volume across the selected dataset</p></div>
          </div>
          <ProductChart products={products} />
        </div>
        <div className="panel">
          <div className="panel-heading">
            <div><h2>Revenue by region</h2><p>Share of total revenue</p></div>
          </div>
          <RegionBreakdown regions={regions} />
        </div>
      </section>

      <RecentSales rows={[...demoSales].reverse().slice(0, 6)} />
    </div>
  );
}
