import { useState } from "react";
import { demoSales } from "../data/demoData";
import PageHeading from "../components/common/PageHeading";
import Notice from "../components/common/Notice";
import SalesTable from "../components/common/SalesTable";

export default function SalesPage() {
  const [search, setSearch] = useState("");
  const rows = [...demoSales].reverse().filter((row) => {
    const query = search.toLowerCase();
    return row.productName.toLowerCase().includes(query)
      || row.region.toLowerCase().includes(query);
  });

  return (
    <div className="page-content">
      <PageHeading eyebrow="REVENUE DATA" title="Sales records"
        description="Browse the transactions currently available in the demo workspace.">
        <input className="search-input" value={search}
          onChange={(event) => setSearch(event.target.value)}
          placeholder="Search product or region" aria-label="Search product or region" />
      </PageHeading>
      <Notice label={`${rows.length} RECORDS`}>
        Sample records only. Upload and live data sync will be added with the backend API.
      </Notice>
      <section className="panel table-panel"><SalesTable rows={rows} /></section>
    </div>
  );
}
