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
  return (
  <div className="stats-container">

  <div className="chart-container">
  <Line
    data={salesChartData}
    options={salesChartOptions}
  />
  </div>

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

  <div className="stat-card">
    <h3>Total Sales</h3>
    <p>₹1,25,000</p>
    <span>+12.5% from last month</span>
  </div>

  <div className="stat-card">
    <h3>Total Orders</h3>
    <p>1,250</p>
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