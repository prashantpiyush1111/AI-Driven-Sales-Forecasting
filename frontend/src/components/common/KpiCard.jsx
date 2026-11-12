export default function KpiCard({ label, value, detail, trend, tone, icon }) {
  return (
    <article className="kpi-card">
      <div className="kpi-top">
        <span>{label}</span>
        <span className={`kpi-icon ${tone}`}>{icon}</span>
      </div>
      <strong className="kpi-value">{value}</strong>
      <div className="kpi-bottom">
        <span>{detail}</span>
        <small className={tone === "mint" ? "model-note" : "trend-positive"}>{trend}</small>
      </div>
    </article>
  );
}
