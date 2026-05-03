import ScheduleBlock from "./ScheduleBlock";

export default function DayTimeline({
  schedules,
  getSchedulePosition,
  paddingTop = 8,
  handleScheduleClick,
}) {
  const hours = Array.from({ length: 24 }, (_, i) => i);
  const hourHeight = 60; // Fixed 60px per hour
  const timelineHeight = 24 * hourHeight + paddingTop; // Total height for all hours

  return (
    <div className="relative flex" style={{ minHeight: `${timelineHeight}px` }}>
      {/* Time labels */}
      <div className="w-16 flex-shrink-0 border-r border-gray-200">
        {hours.map((hour) => (
          <div
            key={hour}
            className="border-t border-gray-200 flex items-start justify-end pr-2"
            style={{
              height: `${hourHeight}px`,
              paddingTop: `${paddingTop}px`,
              minHeight: `${hourHeight}px`,
            }}
          >
            <span className="text-xs text-gray-500 mt-1">
              {hour.toString().padStart(2, "0")}:00
            </span>
          </div>
        ))}
      </div>

      {/* Timeline grid */}
      <div
        className="flex-1 relative"
        style={{ minHeight: `${timelineHeight}px` }}
      >
        {/* Hour lines */}
        {hours.map((hour) => (
          <div
            key={hour}
            className="absolute border-t border-gray-200 w-full"
            style={{
              top: `${hour * hourHeight + paddingTop}px`,
              height: `${hourHeight}px`,
            }}
          >
            {/* Half hour line */}
            <div
              className="absolute left-0 right-0 border-t border-gray-100 border-dashed"
              style={{ top: `${hourHeight / 2}px` }}
            />
          </div>
        ))}

        {/* Schedule blocks */}
        {schedules.map((schedule) => {
          const position = getSchedulePosition(schedule);

          return (
            <ScheduleBlock
              key={schedule.id}
              schedule={schedule}
              position={position}
              paddingTop={paddingTop}
              onClick={() => handleScheduleClick?.(schedule)}
            />
          );
        })}
      </div>
    </div>
  );
}
