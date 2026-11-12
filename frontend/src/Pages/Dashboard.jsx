import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Line } from "react-chartjs-2";
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Filler,
  Tooltip,
  Legend,
} from "chart.js";
import {
  getFilterOptions,
  getOverview,
  getSalesByRegion,
  getSalesSummary,
  getTopProducts,
  getTrend,
} from "../services/dashboardService";
import { getSalesRecords } from "../services/salesService";
import { getErrorMessage } from "../services/apiError";
import PageHeading from "../components/common/PageHeading";
import Notice from "../components/common/Notice";
import SelectFilter from "../components/common/SelectFilter";
import KpiCard from "../components/common/KpiCard";
import RecentSales from "../components/common/RecentSales";

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Filler, Tooltip, Legend);

const money = (amount) => new Intl.NumberFormat("en-IN", {
  style: "currency", currency: "INR", maximumFractionDigits: 0,
}).format(Number(amount) || 0);

const shortMoney = (amount) => new Intl.NumberFormat("en-IN", {
  notation: "compact", maximumFractionDigits: 1,
}).format(Number(amount) || 0);

function formatDate(date) {
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return localDate.toISOString().slice(0, 10);
}

function getDateRange(monthCount) {
  const toDate = new Date();
  const fromDate = new Date(toDate);
  fromDate.setMonth(fromDate.getMonth() - Number(monthCount));
  return { from: formatDate(fromDate), to: formatDate(toDate) };
}

function makeChartOptions() {
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

function makeChartData(trend) {
  return {
    labels: trend.map((point) => {
      const date = new Date(`${point.period}-01T00:00:00`);
      return date.toLocaleDateString("en", { month: "short", year: "2-digit" });
    }),
    datasets: [{
      label: "Revenue",
      data: trend.map((point) => Number(point.revenue) || 0),
      borderColor: "#4568e8",
      backgroundColor: "rgba(69,104,232,.12)",
      fill: true,
      tension: 0.38,
      pointRadius: 3,
      borderWidth: 2.5,
    }],
  };
}

function RegionBreakdown({ regions }) {
  if (!regions.length) return <p className="empty-message">No region data for this period.</p>;
  return (
    <div className="region-list">
      {regions.map((region, index) => (
        <div className="region-analytics-row" key={region.name}>
          <div className="region-analytics-label">
            <span className={`region-dot region-dot-${index % 4}`} />
            <strong>{region.name}</strong>
            <span>{Number(region.revenue || 0).toLocaleString("en-IN", { notation: "compact" })}</span>
          </div>
          <small>{money(region.revenue)}</small>
        </div>
      ))}
    </div>
  );
}

export default function Dashboard() {
  const [region, setRegion] = useState("All regions");
  const [period, setPeriod] = useState("12 months");
  const [regions, setRegions] = useState([]);
  const [dashboard, setDashboard] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;

    async function loadDashboard() {
      setLoading(true);
      setError("");
      const dateRange = getDateRange(period.split(" ")[0]);
      const filters = {
        ...dateRange,
        ...(region !== "All regions" ? { region } : {}),
      };

      try {
        const [overview, summary, trend, products, regionTotals, filterOptions, salesPage] = await Promise.all([
          getOverview(),
          getSalesSummary(filters),
          getTrend({ ...filters, granularity: "MONTHLY" }),
          getTopProducts(dateRange),
          getSalesByRegion(dateRange),
          getFilterOptions(),
          getSalesRecords({ ...filters, page: 0, size: 5, sort: "saleDate,desc" }),
        ]);

        if (cancelled) return;
        setDashboard({
          overview,
          summary,
          trend,
          products,
          regionTotals,
          recentSales: salesPage.data?.content ?? [],
        });
        setRegions(filterOptions.regions);
      } catch (requestError) {
        if (!cancelled) setError(getErrorMessage(requestError, "Dashboard data could not be loaded."));
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    loadDashboard();
    return () => { cancelled = true; };
  }, [region, period]);

  const chartData = useMemo(() => makeChartData(dashboard?.trend ?? []), [dashboard]);
  const regionOptions = ["All regions", ...regions];
  const salesSummary = dashboard?.summary ?? {};
  const overview = dashboard?.overview ?? {};
  const products = dashboard?.products ?? [];
  const regionTotals = dashboard?.regionTotals ?? [];
  const recentSales = dashboard?.recentSales ?? [];
  const monthCount = Number(period.split(" ")[0]);
  const averageMonthlyRevenue = Number(salesSummary.totalRevenue || 0) / monthCount;

  return (
    <div className="page-content">
      <PageHeading title="Sales overview"
        description="A clear view of your sales performance and what may come next.">
        <SelectFilter label="Filter by region" value={region} options={regionOptions} onChange={setRegion} />
        <SelectFilter label="Select time period" value={period}
          options={["12 months", "6 months", "3 months"]} onChange={setPeriod} />
      </PageHeading>

      <Notice label={error ? "API ERROR" : loading ? "LOADING" : "LIVE API"}>
        {error || (loading
          ? "Loading dashboard data from the backend..."
          : "Dashboard figures are loaded from the Spring Boot API.")}
      </Notice>

      <section className="kpi-grid" aria-label="Key performance indicators">
        <KpiCard label="Total revenue" value={money(salesSummary.totalRevenue)} detail="For selected period and region" trend="SALES API" tone="blue" icon="₹" />
        <KpiCard label="Units sold" value={Number(salesSummary.totalQuantity || 0).toLocaleString("en-IN")} detail="Recorded sales quantity" trend="SALES API" tone="violet" icon="▤" />
        <KpiCard label="Forecast revenue" value={money(overview.upcomingForecastRevenue)} detail="Upcoming saved forecasts" trend={`${overview.forecastCount || 0} FORECASTS`} tone="mint" icon="⌁" />
        <KpiCard label="Average per month" value={money(averageMonthlyRevenue)} detail="Selected period average" trend={`${monthCount} MONTHS`} tone="amber" icon="◷" />
      </section>

      {error ? (
        <section className="panel dashboard-error">
          <h2>Could not load dashboard data</h2>
          <p>Check that the backend is running and sign in again if your session has expired.</p>
        </section>
      ) : (
        <>
          <section className="content-grid dashboard-grid">
            <div className="panel trend-panel">
              <div className="panel-heading">
                <div><h2>Revenue trend</h2><p>Monthly recorded sales</p></div>
                <span className="chart-legend"><i /> Actual revenue</span>
              </div>
              <div className="chart-wrap">
                {dashboard?.trend?.length
                  ? <Line data={chartData} options={makeChartOptions()} />
                  : <p className="empty-message">No trend data for this period.</p>}
              </div>
            </div>

            <div className="panel products-panel">
              <div className="panel-heading">
                <div><h2>Top products</h2><p>By revenue in selected period</p></div>
                <Link className="text-link" to="/analytics">View all <span>→</span></Link>
              </div>
              <div className="product-list">
                {products.length ? products.slice(0, 4).map((product, index) => (
                  <div className="product-row" key={product.name}>
                    <span className={`rank rank-${index + 1}`}>{String(index + 1).padStart(2, "0")}</span>
                    <span className="product-name">{product.name}</span>
                    <strong>{money(product.revenue)}</strong>
                  </div>
                )) : <p className="empty-message">No product data for this period.</p>}
              </div>
              <div className="panel-footnote">Live data from the sales analytics API</div>
            </div>
          </section>

          <section className="content-grid analytics-grid">
            <div className="panel">
              <div className="panel-heading">
                <div><h2>Sales by region</h2><p>Revenue in selected period</p></div>
              </div>
              <RegionBreakdown regions={regionTotals} />
            </div>
            <RecentSales rows={recentSales} />
          </section>
        </>
      )}
    </div>
  );
}
