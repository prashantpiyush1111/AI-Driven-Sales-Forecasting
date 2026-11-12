import { Link } from "react-router-dom";
import SalesTable from "./SalesTable";

export default function RecentSales({ rows }) {
  return (
    <section className="panel table-panel">
      <div className="panel-heading">
        <div><h2>Recent sales</h2><p>Latest records in the selected workspace</p></div>
        <Link className="text-link" to="/sales">All sales <span>→</span></Link>
      </div>
      <SalesTable rows={rows} />
    </section>
  );
}
