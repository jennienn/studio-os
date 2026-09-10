export function StatStrip({
  items,
}: {
  items: {
    label: string;
    value: string;
    note?: string;
  }[];
}) {
  return (
    <div className="stat-strip">
      {items.map((item) => (
        <div className="stat-item" key={item.label}>
          <span>{item.label}</span>
          <strong>{item.value}</strong>
          {item.note && <small>{item.note}</small>}
        </div>
      ))}
    </div>
  );
}
