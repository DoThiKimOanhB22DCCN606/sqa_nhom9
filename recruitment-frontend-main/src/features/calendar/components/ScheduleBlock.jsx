import { useTranslation } from "react-i18next";

export default function ScheduleBlock({ schedule, position, paddingTop, onClick }) {
  const { t } = useTranslation();
  const formatTime = (timeString) => {
    if (!timeString) return "";
    try {
      const date = new Date(timeString);
      return date.toLocaleTimeString("vi-VN", {
        hour: "2-digit",
        minute: "2-digit",
      });
    } catch {
      return timeString;
    }
  };

  const endTime = schedule.endTime;

  return (
    <div
      className="absolute left-1 right-1 bg-red-500 text-white rounded-md p-2 cursor-pointer hover:bg-red-600 transition-colors shadow-sm border border-red-600 z-10 overflow-hidden"
      style={{
        top: `${position.top + paddingTop}px`,
        height: `${position.height}px`,
        minHeight: "30px",
      }}
      title={schedule.title || schedule.description}
      onClick={onClick}
    >
      <div className="font-medium text-sm truncate">
        {schedule.title || t("common.noTitle")}
      </div>
      <div className="text-xs opacity-90">
        {formatTime(schedule.startTime)}
        {endTime && ` - ${formatTime(schedule.endTime)}`}
      </div>
    </div>
  );
}
