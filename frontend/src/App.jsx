import { BrowserRouter, Routes, Route, Outlet } from "react-router-dom";

import Sidebar from "./components/Sidebar";
import Navbar from "./components/Navbar";
import ProtectedRoute from "./auth/ProtectedRoute";
import Login from "./Pages/Login";
import Dashboard from "./Pages/Dashboard";
import Forecast from "./Pages/Forecast";

import "./styles/App.css";

function Sales() {
  return (
    <div className="dashboard">
      <h1>Sales</h1>
      <p>Sales data and transactions will appear here.</p>
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

// Shell shown around every page that requires login.
function Layout() {
  return (
    <div className="app-layout">
      <Sidebar />

      <div className="main-content">
        <Navbar />
        <Outlet />
      </div>
    </div>
  );
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />

        <Route
          element={
            <ProtectedRoute>
              <Layout />
            </ProtectedRoute>
          }
        >
          <Route path="/" element={<Dashboard />} />
          <Route path="/sales" element={<Sales />} />
          <Route path="/forecast" element={<Forecast />} />
          <Route path="/analytics" element={<Analytics />} />
          <Route path="/chat" element={<Chat />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;