import { NavLink } from "react-router-dom";

function Sidebar() {
  return (
    <aside className="sidebar">
      <div className="logo">
        SalesAI
      </div>

      <nav>
        <NavLink to="/">Dashboard</NavLink>

        <NavLink to="/sales">Sales</NavLink>

        <NavLink to="/forecast">Forecast</NavLink>

        <NavLink to="/analytics">Analytics</NavLink>

        <NavLink to="/chat">AI Assistant</NavLink>
      </nav>
    </aside>
  );
}

export default Sidebar;