import { useLocation } from "react-router-dom";

const pageNames = {
  "/": "Overview",
  "/sales": "Sales records",
  "/forecast": "Forecasts",
  "/analytics": "Analytics",
  "/assistant": "AI assistant",
};

export default function Navbar({ onMenuClick, sidebarOpen }) {
  const { pathname } = useLocation();
  const pageName = pageNames[pathname] || "Overview";

  return (
    <header className="topbar">
      <div className="topbar-left">
        <button
          className="menu-toggle"
          type="button"
          onClick={onMenuClick}
          aria-label={sidebarOpen ? "Hide sidebar" : "Show sidebar"}
          aria-expanded={sidebarOpen}
        >
          <span /><span /><span />
        </button>
        <div className="breadcrumb">Workspace <span>/</span> <strong>{pageName}</strong></div>
      </div>
      <div className="topbar-right">
        <span className="demo-badge"><span /> DEMO DATA</span>
        <button className="avatar-button" title="Signed in as Prashant">P</button>
      </div>
    </header>
  );
}
