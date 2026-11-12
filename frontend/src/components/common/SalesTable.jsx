const formatMoney = (amount) => new Intl.NumberFormat("en-IN", {
  style: "currency", currency: "INR", maximumFractionDigits: 0,
}).format(amount || 0);

export default function SalesTable({ rows }) {
  return (
    <div className="table-scroll">
      <table>
        <thead>
          <tr>
            <th>PRODUCT</th><th>CATEGORY</th><th>REGION</th>
            <th>SALE DATE</th><th>UNITS</th><th className="align-right">REVENUE</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.id}>
              <td>
                <span className="product-cell-mark">{row.productName.slice(0, 1)}</span>
                <strong>{row.productName}</strong>
              </td>
              <td>{row.category}</td>
              <td><span className="region-chip">{row.region}</span></td>
              <td>{new Date(`${row.saleDate}T00:00:00`).toLocaleDateString("en-IN", {
                day: "2-digit", month: "short", year: "numeric",
              })}</td>
              <td>{row.quantity}</td>
              <td className="align-right revenue-cell">{formatMoney(row.revenue)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
