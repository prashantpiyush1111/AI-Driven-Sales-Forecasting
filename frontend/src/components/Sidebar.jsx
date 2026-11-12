import { Link, NavLink } from "react-router-dom";

const links = [
  { path: "/", label: "Overview", icon: "◫" },
  { path: "/sales", label: "Sales records", icon: "▤" },
  { path: "/forecast", label: "Forecasts", icon: "⌁" },
  { path: "/analytics", label: "Analytics", icon: "▥" },
  { path: "/assistant", label: "AI assistant", icon: "✧", soon: true },
];

export default function Sidebar({ onNavigate }) {
  return (
    <aside className="sidebar">
      <Link className="brand" to="/" aria-label="Saleswise home">
        <span className="brand-mark">S</span>
        <span>saleswise<small>FORECASTING STUDIO</small></span>
      </Link>
      <div className="workspace-label">WORKSPACE</div>
      <nav aria-label="Main navigation">
        {links.map((link) => (
          <NavLink
            key={link.path}
            to={link.path}
            end={link.path === "/"}
            className={({ isActive }) => `nav-link${isActive ? " active" : ""}`}
            onClick={onNavigate}
          >
            <span className="nav-icon" aria-hidden="true">{link.icon}</span>
            {link.label}
            {link.soon && <span className="soon-pill">SOON</span>}
          </NavLink>
        ))}
      </nav>
      <div className="sidebar-bottom">
        <div className="data-status">
          <span className="status-dot" />
          <span><strong>Demo workspace</strong><small>Sample data · not connected</small></span>
        </div>
        <div className="user-avatar">PS</div>
      </div>
    </aside>
  );
}
