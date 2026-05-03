import ScheduleCard from "./ScheduleCard";

export default function MonthView({
  currentDate,
  selectedDate,
  schedules,
  isScheduleOnDate,
  isToday,
  isSameDate,
  getSchedulesForDate,
  handleDateClick,
  handleScheduleClick,
  getDaysInMonth,
  getFirstDayOfMonth,
}) {
  const daysInMonth = getDaysInMonth(currentDate);
  const firstDay = getFirstDayOfMonth(currentDate);
  const days = [];

  // Get previous month info
  const prevMonth = currentDate.getMonth() - 1;
  const prevMonthDate = new Date(currentDate.getFullYear(), prevMonth, 1);
  const daysInPrevMonth = getDaysInMonth(prevMonthDate);

  // Days from previous month
  for (let i = firstDay - 1; i >= 0; i--) {
    const day = daysInPrevMonth - i;
    const isSelected = isSameDate(day, prevMonth);
    const date = new Date(currentDate.getFullYear(), prevMonth, day);
    const daySchedules = getSchedulesForDate(date);

    days.push(
      <div
        key={`prev-${day}`}
        onClick={() => handleDateClick(day, prevMonth)}
        className={`
          aspect-square p-2 border cursor-pointer transition-all hover:bg-gray-50
          flex flex-col overflow-hidden
          ${isSelected ? "bg-red-100 border-red-500" : "border-gray-50"}
        `}
      >
        <div
          className={`text-sm font-medium ${
            isSelected ? "text-red-700" : "text-gray-400"
          }`}
        >
          {day}
        </div>
        <div className="flex-1 overflow-y-auto mt-1">
          {daySchedules.slice(0, 3).map((schedule) => (
            <ScheduleCard
              key={schedule.id}
              schedule={schedule}
              onClick={(e) => {
                e.stopPropagation();
                handleScheduleClick?.(schedule);
              }}
            />
          ))}
          {daySchedules.length > 3 && (
            <div className="text-xs text-gray-500 mt-1">
              +{daySchedules.length - 3} khác
            </div>
          )}
        </div>
      </div>
    );
  }

  // Days of the current month
  for (let day = 1; day <= daysInMonth; day++) {
    const isCurrentDay = isToday(day);
    const isSelected = isSameDate(day);
    const date = new Date(
      currentDate.getFullYear(),
      currentDate.getMonth(),
      day
    );
    const daySchedules = getSchedulesForDate(date);

    days.push(
      <div
        key={`current-${day}`}
        onClick={() => handleDateClick(day)}
        className={`
          aspect-square p-2 border border-gray-100 cursor-pointer
          transition-all hover:bg-gray-50 flex flex-col overflow-hidden
          ${isCurrentDay ? "bg-red-50 border-red-300" : ""}
          ${isSelected ? "bg-red-100 border-red-500" : ""}
        `}
      >
        <div
          className={`
            text-sm font-medium
            ${isCurrentDay ? "text-red-600" : "text-gray-700"}
            ${isSelected ? "text-red-700" : ""}
          `}
        >
          {day}
        </div>
        <div className="flex-1 overflow-y-auto mt-1">
          {daySchedules.slice(0, 3).map((schedule) => (
            <ScheduleCard
              key={schedule.id}
              schedule={schedule}
              onClick={(e) => {
                e.stopPropagation();
                handleScheduleClick?.(schedule);
              }}
            />
          ))}
          {daySchedules.length > 3 && (
            <div className="text-xs text-gray-500 mt-1">
              +{daySchedules.length - 3} khác
            </div>
          )}
        </div>
      </div>
    );
  }

  // Days from next month (to fill the grid)
  const totalCells = days.length;
  const remainingCells = totalCells % 7 === 0 ? 0 : 7 - (totalCells % 7);
  const nextMonth = currentDate.getMonth() + 1;

  for (let day = 1; day <= remainingCells; day++) {
    const isSelected = isSameDate(day, nextMonth);
    const date = new Date(currentDate.getFullYear(), nextMonth, day);
    const daySchedules = getSchedulesForDate(date);

    days.push(
      <div
        key={`next-${day}`}
        onClick={() => handleDateClick(day, nextMonth)}
        className={`
          aspect-square p-2 border cursor-pointer transition-all hover:bg-gray-50
          flex flex-col overflow-hidden
          ${isSelected ? "bg-red-100 border-red-500" : "border-gray-50"}
        `}
      >
        <div
          className={`text-sm font-medium ${
            isSelected ? "text-red-700" : "text-gray-400"
          }`}
        >
          {day}
        </div>
        <div className="flex-1 overflow-y-auto mt-1">
          {daySchedules.slice(0, 3).map((schedule) => (
            <ScheduleCard
              key={schedule.id}
              schedule={schedule}
              onClick={(e) => {
                e.stopPropagation();
                handleScheduleClick?.(schedule);
              }}
            />
          ))}
          {daySchedules.length > 3 && (
            <div className="text-xs text-gray-500 mt-1">
              +{daySchedules.length - 3} khác
            </div>
          )}
        </div>
      </div>
    );
  }

  return <>{days}</>;
}
