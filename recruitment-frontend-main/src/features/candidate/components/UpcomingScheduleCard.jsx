import { Clock, Calendar } from "lucide-react";
import { useTranslation } from "react-i18next";

export default function UpcomingScheduleCard({ schedules = [] }) {
  const { t } = useTranslation();
  const formatDate = (date) => {
    if (!date) return "-";
    try {
      return new Date(date).toLocaleDateString("vi-VN");
    } catch {
      return date;
    }
  };

  const formatTimeRange = (start, end) => {
    if (!start) return "-";
    try {
      const fmt = (d) =>
        new Date(d)
          .toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" })
          .replace(":00", ":00");
      return end ? `${fmt(start)} - ${fmt(end)}` : fmt(start);
    } catch {
      return start;
    }
  };

  const renderParticipants = (participants = []) => {
    const names = participants.map((p) => p.name).filter(Boolean);
    return names.length ? names.join(", ") : "-";
  };

  const palette = [
    { bg: "bg-blue-50", border: "border-blue-200", text: "text-blue-800" },
    { bg: "bg-green-50", border: "border-green-200", text: "text-green-800" },
    { bg: "bg-amber-50", border: "border-amber-200", text: "text-amber-800" },
    {
      bg: "bg-purple-50",
      border: "border-purple-200",
      text: "text-purple-800",
    },
    { bg: "bg-pink-50", border: "border-pink-200", text: "text-pink-800" },
    { bg: "bg-teal-50", border: "border-teal-200", text: "text-teal-800" },
  ];

  return (
    <div className="bg-white rounded-xl shadow p-6">
      <h3 className="font-semibold text-gray-900 mb-4 flex items-center gap-2">
        <Clock size={18} />
        {t("candidates.upcomingSchedules")}
      </h3>
      {!schedules || schedules.length === 0 ? (
        <div className="text-center py-8 text-gray-400">
          <Calendar size={32} className="mx-auto mb-2 opacity-50" />
          <p className="text-sm">{t("candidates.noSchedules")}</p>
        </div>
      ) : (
        <div className="space-y-3">
          {schedules.map((sch, idx) => {
            const color = palette[idx % palette.length];
            return (
              <div
                key={sch.id}
                className={`border rounded-lg p-4 ${color.bg} ${color.border}`}
              >
                <div className={`font-medium mb-1 truncate ${color.text}`}>
                  {sch.title || t("common.noTitle")}
                </div>
                <div className="text-sm text-gray-700 mb-1">
                  <span className="text-gray-500">{t("candidates.date")}: </span>
                  {formatDate(sch.startTime)}
                </div>
                <div className="text-sm text-gray-700">
                  <span className="text-gray-500">{t("candidates.time")}: </span>
                  {formatTimeRange(sch.startTime, sch.endTime)}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
