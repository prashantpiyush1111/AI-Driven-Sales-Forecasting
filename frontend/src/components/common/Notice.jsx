export default function Notice({ children, label = "PREVIEW" }) {
  return (
    <div className="notice">
      <span className="notice-icon">i</span>
      <span>{children}</span>
      <span className="notice-tag">{label}</span>
    </div>
  );
}
