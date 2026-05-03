import ScheduleCard from "./ScheduleCard";

export default function WeekView({
  currentDate,
  selectedDate,
  schedules,
  isToday,
  getSchedulesForDate,
  handleDateClick,
  handleScheduleClick,
}) {
  const startOfWeek = new Date(currentDate);
  const day = startOfWeek.getDay();
  const diff = startOfWeek.getDate() - day;
  startOfWeek.setDate(diff);

  const weekDays = [];
  for (let i = 0; i < 7; i++) {
    const date = new Date(startOfWeek);
    date.setDate(startOfWeek.getDate() + i);
    const dayNum = date.getDate();
    const isCurrentDay = isToday(dayNum);
    const isSelected =
      selectedDate &&
      date.getDate() === selectedDate.getDate() &&
      date.getMonth() === selectedDate.getMonth() &&
      date.getFullYear() === selectedDate.getFullYear();
    const daySchedules = getSchedulesForDate(date);

    weekDays.push(
      <div
        key={i}
        onClick={() => handleDateClick(date.getDate(), date.getMonth())}
        className={`
          flex-1 border border-gray-100 p-4 cursor-pointer
          transition-all hover:bg-gray-50 flex flex-col overflow-hidden
          ${isCurrentDay ? "bg-red-50 border-red-300" : ""}
          ${isSelected ? "bg-red-100 border-red-500" : ""}
        `}
      >
        <div className="text-center mb-2">
          <div className="text-xs text-gray-500 mb-1">
            {["CN", "T2", "T3", "T4", "T5", "T6", "T7"][i]}
          </div>
          <div
            className={`
              text-lg font-semibold
              ${isCurrentDay ? "text-red-600" : "text-gray-700"}
              ${isSelected ? "text-red-700" : ""}
            `}
          >
            {dayNum}
          </div>
        </div>
        <div className="flex-1 overflow-y-auto">
          {daySchedules.map((schedule) => (
            <ScheduleCard
              key={schedule.id}
              schedule={schedule}
              onClick={(e) => {
                e.stopPropagation();
                handleScheduleClick?.(schedule);
              }}
            />
          ))}
        </div>
      </div>
    );
  }

  return <>{weekDays}</>;
}
