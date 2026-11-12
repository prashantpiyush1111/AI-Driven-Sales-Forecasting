import { useState } from "react";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import Sidebar from "./components/Sidebar";
import Navbar from "./components/Navbar";
import Dashboard from "./Pages/Dashboard";
import SalesPage from "./Pages/SalesPage";
import Forecast from "./Pages/Forecast";
import AnalyticsPage from "./Pages/AnalyticsPage";
import AssistantPage from "./Pages/AssistantPage";
import "./styles/App.css";

function AppLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(() =>
    window.matchMedia("(min-width: 761px)").matches
  );

  function toggleSidebar() {
    setSidebarOpen((isOpen) => !isOpen);
  }

  function closeSidebar() {
    setSidebarOpen(false);
  }

  return (
    <div className={`app-layout${sidebarOpen ? "" : " sidebar-collapsed"}`}>
      <Navbar onMenuClick={toggleSidebar} sidebarOpen={sidebarOpen} />
      <div className="workspace-body">
        <Sidebar onNavigate={closeSidebar} />
        {sidebarOpen && (
          <button
            className="sidebar-backdrop"
            onClick={closeSidebar}
            aria-label="Close navigation menu"
          />
        )}
        <main className="main-content">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/sales" element={<SalesPage />} />
            <Route path="/forecast" element={<Forecast />} />
            <Route path="/analytics" element={<AnalyticsPage />} />
            <Route path="/assistant" element={<AssistantPage />} />
          </Routes>
          <footer className="page-footer">
            Saleswise <span>·</span> AI driven sales forecasting <span>·</span> Demo environment
          </footer>
        </main>
      </div>
    </div>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AppLayout />
    </BrowserRouter>
  );
}
