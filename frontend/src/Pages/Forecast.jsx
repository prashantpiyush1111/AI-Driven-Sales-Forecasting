
import { Line } from "react-chartjs-2";
import "../styles/Forecast.css";

function Forecast() {
  const chartData = {
    labels: ["Jan", "Feb", "Mar", "Apr", "May", "Jun"],
    datasets: [
      {
        label: "Predicted Sales",
        data: [120000, 135000, 128000, 150000, 165000, 180000],
        borderColor: "#6366f1",
        backgroundColor: "rgba(99, 102, 241, 0.15)",
        fill: true,
        tension: 0.4,
      },
    ],
  };

  return (
    <div className="forecast-page">
      <h1>Sales Forecast</h1>
      <p>View predicted sales trends for upcoming periods.</p>

      <div className="forecast-card">
        <h3>Forecast Overview</h3>
        <p>Forecast period: Next 6 months</p>
        <p>Forecast status: Sample data</p>
      </div>

      <div className="forecast-chart">
        <h3>Predicted Sales Trend</h3>
        <Line
          data={chartData}
          options={{
            responsive: true,
            maintainAspectRatio: false,
          }}
        />
      </div>
    </div>
  );
}

export default Forecast;
