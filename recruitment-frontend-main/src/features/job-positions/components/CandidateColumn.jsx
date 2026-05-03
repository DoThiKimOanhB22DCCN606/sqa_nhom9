import { SortableContext, useSortable } from "@dnd-kit/sortable";
import CandidateCard from "./CandidateCard";

export default function CandidateColumn({
  status,
  candidates,
  onCandidateClick,
  onChangeStatus,
  allStatuses = [],
  canChangeStatus,
}) {
  const { setNodeRef } = useSortable({
    id: status.id,
    data: {
      type: "column",
      status: status.id,
    },
  });

  return (
    <div className="flex-shrink-0 w-[280px] bg-white rounded-xl border border-gray-200 shadow-sm p-4 flex flex-col max-h-full overflow-visible">
      {/* Column Header */}
      <div
        className="flex items-center justify-between mb-3 pb-2.5 border-b-2 flex-shrink-0"
        style={{ borderColor: status.color }}
      >
        <div className="flex items-center gap-2">
          <div
            className="w-3 h-3 rounded-full shadow-sm"
            style={{ backgroundColor: status.color }}
          />
          <h3 className="font-semibold text-gray-900 text-sm">
            {status.label}
          </h3>
        </div>
        <span
          className="px-2.5 py-1 rounded-full text-xs font-semibold shadow-sm"
          style={{
            backgroundColor: status.bgColor,
            color: status.color,
          }}
        >
          {candidates.length}
        </span>
      </div>

      {/* Cards Container - Droppable */}
      <div
        ref={setNodeRef}
        className="flex-1 space-y-0 overflow-y-auto custom-scrollbar min-h-0"
        style={{ overflowX: 'visible' }}
      >
        <SortableContext items={candidates.map((c) => c.id)}>
          {candidates.length === 0 ? (
            <div className="text-center py-12 text-gray-400 text-sm">
              <div className="w-16 h-16 mx-auto mb-3 rounded-full bg-gray-50 flex items-center justify-center">
                <div
                  className="w-8 h-8 rounded-full"
                  style={{ backgroundColor: status.bgColor }}
                />
              </div>
              <p>Chưa có ứng viên</p>
            </div>
          ) : (
            candidates.map((candidate) => {
              // Create status options excluding current status and invalid transitions
              const statusOptions = allStatuses
                .filter((s) => s.id !== candidate.status && (!canChangeStatus || canChangeStatus(candidate.status, s.id)))
                .map((s) => ({
                  label: s.label,
                  icon: null,
                  onClick: () => onChangeStatus(candidate, s.id),
                }));

              return (
                <CandidateCard
                  key={candidate.id}
                  candidate={candidate}
                  onClick={onCandidateClick}
                  statusOptions={statusOptions}
                  onChangeStatus={onChangeStatus}
                />
              );
            })
          )}
        </SortableContext>
      </div>
    </div>
  );
}
