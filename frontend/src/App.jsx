import { useState } from "react";
import { BrowserRouter, Routes, Route } from "react-router-dom";

import Sidebar from "./components/Sidebar";
import Navbar from "./components/Navbar";

import "./styles/App.css";

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

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend
);

const salesData = [
  { month: "Jan", region: "North", product: "Laptop", sales: 20000, orders: 200 },
  { month: "Feb", region: "North", product: "Smartphone", sales: 25000, orders: 250 },
  { month: "Mar", region: "South", product: "Laptop", sales: 30000, orders: 300 },
  { month: "Apr", region: "East", product: "Headphones", sales: 28000, orders: 280 },
  { month: "May", region: "West", product: "Monitor", sales: 32000, orders: 320 },
  { month: "Jun", region: "North", product: "Laptop", sales: 40000, orders: 400 },
];

const salesChartData = {
  labels: [
    "Jan",
    "Feb",
    "Mar",
    "Apr",
    "May",
    "Jun",
  ],

  datasets: [
    {
      label: "Monthly Sales",
      data: [85000, 92000, 105000, 98000, 115000, 125000],
      borderWidth: 3,
      tension: 0.4,
    },
  ],
};

const salesChartOptions = {
  responsive: true,

  plugins: {
    legend: {
      display: true,
    },

    title: {
      display: true,
      text: "Monthly Sales Trend",
    },
  },
};

function Dashboard() {
  const [timePeriod, setTimePeriod] = useState("Last 30 Days");
  const [region, setRegion] = useState("All Regions");
  const [product, setProduct] = useState("All Products");

  const filteredData = salesData.filter((item) => {
  const regionMatch =
    region === "All Regions" || item.region === region;

  const productMatch =
    product === "All Products" || item.product === product;

  return regionMatch && productMatch;
});

const totalSales = filteredData.reduce(
  (total, item) => total + item.sales,
  0
);

const totalOrders = filteredData.reduce(
  (total, item) => total + item.orders,
  0
);

  return (

    <div className="dashboard">

      <div className="dashboard-header">
        <h1>Sales Dashboard</h1>
        <p>
          Monitor your sales performance and business insights.
        </p>
      </div>

      <div className="filter-container">

  <div className="filter-group">
    <label>Time Period</label>
    <select
  value={timePeriod}
  onChange={(e) => setTimePeriod(e.target.value)}
>
  <option>Last 30 Days</option>
  <option>Last 3 Months</option>
  <option>Last 6 Months</option>
  <option>This Year</option>
</select>
  </div>

  <div className="filter-group">
    <label>Region</label>
    <select
  value={region}
  onChange={(e) => setRegion(e.target.value)}
>
  <option>All Regions</option>
  <option>North</option>
  <option>South</option>
  <option>East</option>
  <option>West</option>
</select>
  </div>

  <div className="filter-group">
    <label>Product</label>
    <select
  value={product}
  onChange={(e) => setProduct(e.target.value)}
>
  <option>All Products</option>
  <option>Laptop</option>
  <option>Smartphone</option>
  <option>Headphones</option>
  <option>Monitor</option>
</select>
  </div>
</div>

      {/* KPI Cards */}
      <div className="stats-container">

        <div className="stat-card">
          <h3>Total Sales</h3>
          <p>₹{totalSales.toLocaleString("en-IN")}</p>
          <span>+12.5% from last month</span>
        </div>

        <div className="stat-card">
          <h3>Total Orders</h3>
          <p>{totalOrders.toLocaleString("en-IN")}</p>
          <span>+8.2% from last month</span>
        </div>

        <div className="stat-card">
          <h3>Forecasted Sales</h3>
          <p>₹1,50,000</p>
          <span>Next month prediction</span>
        </div>

        <div className="stat-card">
          <h3>Average Order Value</h3>
          <p>₹1,000</p>
          <span>+5.4% from last month</span>
        </div>

      </div>

      {/* Sales Chart */}
      <div className="chart-container">
        <Line
          data={salesChartData}
          options={salesChartOptions}
        />
      </div>

      {/* Summary Section */}
      <div className="summary-container">

        <div className="summary-card">
          <h2>Top Products</h2>

          <div className="summary-item">
            <span>Laptop</span>
            <strong>₹45,000</strong>
          </div>

          <div className="summary-item">
            <span>Smartphone</span>
            <strong>₹32,500</strong>
          </div>

          <div className="summary-item">
            <span>Headphones</span>
            <strong>₹18,200</strong>
          </div>

          <div className="summary-item">
            <span>Monitor</span>
            <strong>₹15,800</strong>
          </div>
        </div>

        <div className="summary-card">
          <h2>Sales by Region</h2>

          <div className="summary-item">
            <span>North</span>
            <strong>₹42,000</strong>
          </div>

          <div className="summary-item">
            <span>South</span>
            <strong>₹35,000</strong>
          </div>

          <div className="summary-item">
            <span>East</span>
            <strong>₹28,000</strong>
          </div>

          <div className="summary-item">
            <span>West</span>
            <strong>₹20,000</strong>
          </div>

        </div>

      </div>

    </div>
  );
}

function Sales() {
  return (
    <div className="dashboard">
      <h1>Sales</h1>
      <p>Sales data and transactions will appear here.</p>
    </div>
  );
}

function Forecast() {
  return (
    <div className="dashboard">
      <h1>Sales Forecast</h1>
      <p>AI-based sales predictions will appear here.</p>
    </div>
  );
}

function Analytics() {
  return (
    <div className="dashboard">
      <h1>Analytics</h1>
      <p>Business intelligence analytics will appear here.</p>
    </div>
  );
}

function Chat() {
  return (
    <div className="dashboard">
      <h1>AI Assistant</h1>
      <p>Ask questions about your sales data.</p>
    </div>
  );
}

function App() {
  return (
    <BrowserRouter>
      <div className="app-layout">
        <Sidebar />

        <div className="main-content">
          <Navbar />

          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/sales" element={<Sales />} />
            <Route path="/forecast" element={<Forecast />} />
            <Route path="/analytics" element={<Analytics />} />
            <Route path="/chat" element={<Chat />} />
          </Routes>
        </div>
      </div>
    </BrowserRouter>
  );
}

export default App;