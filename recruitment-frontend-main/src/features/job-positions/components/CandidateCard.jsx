import { Mail, Calendar } from "lucide-react";
import { useSortable } from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import DropdownMenu from "../../../components/ui/DropdownMenu";

// Candidate Card Component (Base)
function CandidateCardBase({ candidate, onClick, isDragging = false, statusOptions = [], onChangeStatus }) {
  const getInitials = (name) => {
    return name
      .split(" ")
      .map((n) => n[0])
      .join("")
      .toUpperCase()
      .slice(0, 2);
  };

  return (
    <div
      className={`bg-white rounded-lg border border-gray-200 p-2.5 mb-1.5 transition-all cursor-pointer group relative ${
        isDragging
          ? "opacity-50 shadow-lg"
          : "hover:shadow-md hover:border-gray-300"
      }`}
      onClick={onClick ? () => onClick(candidate) : undefined}
    >
      {/* Dropdown Menu */}
      {statusOptions.length > 0 && onChangeStatus && (
        <div className="absolute top-2 right-2 z-10 opacity-0 group-hover:opacity-100 transition-opacity" onClick={(e) => e.stopPropagation()}>
          <DropdownMenu options={statusOptions} position="left" />
        </div>
      )}

      <div className="flex items-start gap-2.5">
        {/* Avatar */}
        <div className="flex-shrink-0 w-8 h-8 bg-gradient-to-br from-blue-500 to-purple-600 rounded-lg flex items-center justify-center text-white font-semibold text-xs shadow-sm">
          {getInitials(candidate.name)}
        </div>

        {/* Content */}
        <div className="flex-1 min-w-0 pr-6">
          {/* Name */}
          <h4 className="font-semibold text-gray-900 text-sm truncate mb-1">
            {candidate.name}
          </h4>

          {/* Info - Compact */}
          <div className="space-y-0.5">
            <div className="flex items-center gap-1.5 text-xs text-gray-600">
              <Mail className="h-3 w-3 flex-shrink-0" />
              <span className="truncate">{candidate.email}</span>
            </div>
            <div className="flex items-center gap-1.5 text-xs text-gray-500">
              <Calendar className="h-3 w-3 flex-shrink-0" />
              <span>Nộp: {candidate.appliedDate}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

// Draggable Candidate Card
export default function CandidateCard({ candidate, onClick, statusOptions = [], onChangeStatus }) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: candidate.id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
  };

  return (
    <div ref={setNodeRef} style={style} {...attributes} {...listeners}>
      <CandidateCardBase
        candidate={candidate}
        onClick={onClick}
        isDragging={isDragging}
        statusOptions={statusOptions}
        onChangeStatus={onChangeStatus}
      />
    </div>
  );
}

// Export base component for DragOverlay
export { CandidateCardBase };
