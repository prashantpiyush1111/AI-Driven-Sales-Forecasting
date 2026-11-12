import { useLocation } from "react-router-dom";
import useAuth from "../auth/useAuth";

const pageNames = {
  "/": "Overview",
  "/sales": "Sales records",
  "/forecast": "Forecasts",
  "/analytics": "Analytics",
  "/assistant": "AI assistant",
};

export default function Navbar({ onMenuClick, sidebarOpen }) {
  const { pathname } = useLocation();
  const { user, logout } = useAuth();
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
        {user?.role && <span className="role-badge">{user.role}</span>}
        <span className="welcome-user">{user?.username ?? "User"}</span>
        <button className="logout-button" type="button" onClick={logout}>Log out</button>
      </div>
    </header>
  );
}
