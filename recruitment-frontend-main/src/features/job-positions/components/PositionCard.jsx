import {
  Edit,
  Trash2,
  Eye,
  Users,
  UserPlus,
  RefreshCw,
  CheckCircle,
  PauseCircle,
  XCircle,
} from "lucide-react";
import DropdownMenu from "../../../components/ui/DropdownMenu";
import StatusBadge from "../../../components/ui/StatusBadge";

export default function PositionCard({
  position,
  onClicked,
  onClickedCandidates,
  onAddCandidate,
  isCompact = false,
  isSelected = false,
  onEdit,
  onDelete,
  onView,
  onUpdateStatus,
}) {
  const currentStatusValue = String(position.status || "").toUpperCase();
  
  const menuOptions = [
    onView && {
      label: "Xem chi tiết",
      icon: Eye,
      onClick: () => onView(position),
    },
    onClickedCandidates && {
      label: "Xem ứng viên",
      icon: Users,
      onClick: () => onClickedCandidates(position),
    },
    onAddCandidate && {
      label: "Thêm ứng viên",
      icon: UserPlus,
      onClick: () => onAddCandidate(position),
    },
    onUpdateStatus && currentStatusValue === "DRAFT" && {
      label: "Đăng tuyển",
      icon: CheckCircle,
      onClick: () => onUpdateStatus(position, "PUBLISHED"),
    },
    onUpdateStatus && currentStatusValue === "PUBLISHED" && {
      label: "Đóng tuyển",
      icon: XCircle,
      onClick: () => onUpdateStatus(position, "CLOSED"),
    },
    onEdit && {
      label: "Chỉnh sửa",
      icon: Edit,
      onClick: () => onEdit(position),
    },
    onDelete && {
      divider: true,
    },
    onDelete && {
      label: "Xóa",
      icon: Trash2,
      variant: "danger",
      onClick: () => onDelete(position),
    },
  ].filter(Boolean);

  if (isCompact) {
    // Compact list view
    return (
      <div
        className={`bg-white rounded-lg p-3 border transition-all cursor-pointer relative
          ${
            isSelected
              ? "border-[#DC2626] bg-red-50 shadow-md"
              : "border-gray-200 hover:border-gray-300 hover:shadow-sm"
          }
        `}
        onClick={onClicked}
      >
        {/* Menu button */}
        <div className="absolute top-2 right-2">
          <DropdownMenu options={menuOptions} />
        </div>

        {/* Header: Title, Department, Status */}
        <div className="flex items-start justify-between gap-2 mb-2 pr-8">
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-1">
              <span className="text-xs text-gray-500">ID: {position.id}</span>
              <StatusBadge status={position.status} />
            </div>
            <h3
              className={`text-sm font-medium mb-0.5 truncate ${
                isSelected ? "text-[#DC2626]" : "text-gray-900"
              }`}
              title={position.title}
            >
              {position.title}
            </h3>
            <p className="text-xs text-gray-500 truncate">
              {position.department}
            </p>
          </div>
        </div>

        {/* Level, Experience & Tags in one row */}
        <div className="flex items-center gap-2 mb-2 text-xs text-gray-600 flex-wrap">
          <span className="capitalize">{position.level}</span>
          <span className="text-gray-300">•</span>
          <span>{position.experience}</span>
          <span className="text-gray-300">•</span>
          <span className="px-2 py-0.5 bg-gray-50 rounded-full capitalize">
            {position.type}
          </span>
          {position.mode && (
            <span className="px-2 py-0.5 bg-gray-50 rounded-full">
              {position.mode}
            </span>
          )}
        </div>

        {/* Salary and Applicants */}
        <div className="flex items-center justify-between pt-2 border-t border-gray-100">
          <span className="text-[#DC2626] font-medium text-sm">
            {position.salary}
          </span>
          <span
            className="text-xs text-gray-500 hover:text-[#DC2626] transition-colors cursor-pointer"
            onClick={(e) => {
              e.stopPropagation();
              onClickedCandidates(position);
            }}
          >
            {position.applicants} ứng viên
          </span>
        </div>
      </div>
    );
  }

  // Original card view
  return (
    <div
      className={`bg-white rounded-[10px] p-4 border
        transition-all cursor-pointer relative
        border-gray-200 hover:border-gray-300 hover:shadow-md
      `}
      onClick={onClicked}
    >
      {/* Status badge and Menu button */}
      <div className="absolute top-2 right-2 flex items-center gap-2">
        <StatusBadge status={position.status} />
        <DropdownMenu options={menuOptions} />
      </div>

      <>
        <div className="text-xs text-gray-500 mb-2 pr-32">
          ID: {position.id}
        </div>
        <div className="flex items-start justify-between mb-4">
          <div className="flex-1 pr-32">
            <h3
              className="text-base font-medium text-gray-900 mb-1 truncate"
              title={position.title}
            >
              {position.title}
            </h3>
            <p className="text-sm text-gray-500 truncate">
              {position.department}
            </p>
          </div>
        </div>

        {/* Level and Experience */}
        <div className="flex flex-col gap-2 mb-4">
          <div className="flex items-center gap-2">
            <span className="text-sm text-gray-600 capitalize">
              {position.level}
            </span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-sm text-gray-600">{position.experience}</span>
          </div>
        </div>
        <div className="flex gap-2 mb-4">
          <span className="px-2 py-1 bg-gray-50 rounded-full text-xs text-gray-600 capitalize">
            {position.type}
          </span>
          {position.mode && (
            <span className="px-2 py-1 bg-gray-50 rounded-full text-xs text-gray-600">
              {position.mode}
            </span>
          )}
        </div>

        {/* Salary and Applicants */}
        <div className="flex flex-col gap-2">
          <div className="text-[#DC2626] font-medium">{position.salary}</div>
          <div
            className="text-sm text-gray-500 cursor-pointer hover:text-[#DC2626] applicants-count flex items-center gap-1"
            onClick={(e) => {
              e.stopPropagation();
              onClickedCandidates(position);
            }}
          >
            {position.applicants} ứng viên
          </div>
        </div>
      </>
    </div>
  );
}
