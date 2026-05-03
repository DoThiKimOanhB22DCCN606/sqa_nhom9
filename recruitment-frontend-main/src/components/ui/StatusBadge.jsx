import { getStatusConfig } from "../../constants/status";

export default function StatusBadge({ status }) {
  const config = getStatusConfig(status);

  return (
    <span
      className={`px-3 py-1 rounded-full text-xs font-medium whitespace-nowrap border ${config.bgColor} ${config.textColor} ${config.borderColor}`}
    >
      {config.label}
    </span>
  );
}
