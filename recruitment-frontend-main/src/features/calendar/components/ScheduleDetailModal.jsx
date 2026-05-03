import { useEffect } from "react";
import { createPortal } from "react-dom";
import { X, Calendar, Clock, MapPin, Users, Video, Monitor } from "lucide-react";
import Button from "../../../components/ui/Button";
import { useTranslation } from "react-i18next";

export default function ScheduleDetailModal({ schedule, isOpen, onClose }) {
  const { t } = useTranslation();
  // Handle ESC key
  useEffect(() => {
    if (!isOpen) return;

    const handleEsc = (e) => {
      if (e.key === "Escape") {
        onClose();
      }
    };

    document.addEventListener("keydown", handleEsc);
    return () => document.removeEventListener("keydown", handleEsc);
  }, [isOpen, onClose]);

  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = "hidden";
    } else {
      document.body.style.overflow = "unset";
    }

    return () => {
      document.body.style.overflow = "unset";
    };
  }, [isOpen]);

  if (!isOpen || !schedule) return null;

  const formatDateTime = (timeString) => {
    if (!timeString) return "-";
    try {
      const date = new Date(timeString);
      return {
        date: date.toLocaleDateString("vi-VN", {
          weekday: "long",
          year: "numeric",
          month: "long",
          day: "numeric",
        }),
        time: date.toLocaleTimeString("vi-VN", {
          hour: "2-digit",
          minute: "2-digit",
        }),
      };
    } catch {
      return { date: timeString, time: "" };
    }
  };

  const formatTime = (timeString) => {
    if (!timeString) return "-";
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

  const getFormatLabel = (format) => {
    const formatMap = {
      ONLINE: t("calendarSchedule.formats.online"),
      OFFLINE: t("calendarSchedule.formats.offline"),
    };
    return formatMap[format] || format;
  };

  const getMeetingTypeLabel = (meetingType) => {
    const typeMap = {
      INTERVIEW: t("calendarSchedule.meetingTypes.interview"),
      MEETING: t("calendarSchedule.meetingTypes.meeting"),
      OTHER: t("calendarSchedule.meetingTypes.other"),
    };
    return typeMap[meetingType] || meetingType;
  };

  const startDateTime = formatDateTime(schedule.startTime);
  const endTime = formatTime(schedule.endTime);

  const dialogContent = (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      onClick={(e) => {
        if (e.target === e.currentTarget) {
          onClose();
        }
      }}
    >
      {/* Backdrop */}
      <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" />

      {/* Dialog */}
      <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-2xl max-h-[90vh] overflow-hidden flex flex-col transform transition-all">
        {/* Close button */}
        <div
          onClick={onClose}
          className="absolute top-4 right-4 p-1.5 hover:bg-gray-100 rounded-lg transition-colors cursor-pointer z-10"
          aria-label="Close"
        >
          <X className="h-4 w-4 text-gray-500" />
        </div>

        {/* Header */}
        <div className="p-6 pr-12 border-b border-gray-200">
          <h2 className="text-2xl font-bold text-gray-900">
            {t("modals.scheduleDetail")}
          </h2>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-6 pr-12 space-y-6">
          {/* Title */}
          <div>
            <h3 className="text-xl font-semibold text-gray-900">
              {schedule.title || t("common.noTitle")}
            </h3>
          </div>

          {/* Description */}
          {schedule.description && (
            <div>
              <p className="text-sm text-gray-600 leading-relaxed whitespace-pre-wrap">
                {schedule.description}
              </p>
            </div>
          )}

          {/* Date and Time */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* Date */}
            <div className="flex items-start gap-3">
              <div className="flex-shrink-0 w-10 h-10 bg-red-50 text-red-600 rounded-xl flex items-center justify-center">
                <Calendar className="h-5 w-5" />
              </div>
              <div className="flex-1 pt-1">
                <div className="text-xs text-gray-500 mb-1">{t("calendarSchedule.date")}</div>
                <div className="text-sm text-gray-900 font-medium">{startDateTime.date}</div>
              </div>
            </div>

            {/* Time */}
            <div className="flex items-start gap-3">
              <div className="flex-shrink-0 w-10 h-10 bg-red-50 text-red-600 rounded-xl flex items-center justify-center">
                <Clock className="h-5 w-5" />
              </div>
              <div className="flex-1 pt-1">
                <div className="text-xs text-gray-500 mb-1">{t("calendarSchedule.time")}</div>
                <div className="text-sm text-gray-900 font-medium">
                  {startDateTime.time}
                  {schedule.endTime && ` - ${endTime}`}
                </div>
              </div>
            </div>
          </div>

          {/* Format and Meeting Type */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* Format */}
            <div className="flex items-start gap-3">
              <div className="flex-shrink-0 w-10 h-10 bg-red-50 text-red-600 rounded-xl flex items-center justify-center">
                {schedule.format === "ONLINE" ? (
                  <Video className="h-5 w-5" />
                ) : (
                  <Monitor className="h-5 w-5" />
                )}
              </div>
              <div className="flex-1 pt-1">
                <div className="text-xs text-gray-500 mb-1">{t("calendarSchedule.format")}</div>
                <div className="text-sm text-gray-900 font-medium">
                  {getFormatLabel(schedule.format)}
                </div>
              </div>
            </div>

            {/* Meeting Type */}
            <div className="flex items-start gap-3">
              <div className="flex-shrink-0 w-10 h-10 bg-red-50 text-red-600 rounded-xl flex items-center justify-center">
                <Calendar className="h-5 w-5" />
              </div>
              <div className="flex-1 pt-1">
                <div className="text-xs text-gray-500 mb-1">{t("calendarSchedule.type")}</div>
                <div className="text-sm text-gray-900 font-medium">
                  {getMeetingTypeLabel(schedule.meetingType)}
                </div>
              </div>
            </div>
          </div>

          {/* Location */}
          {schedule.location && (
            <div className="flex items-start gap-3">
              <div className="flex-shrink-0 w-10 h-10 bg-red-50 text-red-600 rounded-xl flex items-center justify-center">
                <MapPin className="h-5 w-5" />
              </div>
              <div className="flex-1 pt-1">
                <div className="text-xs text-gray-500 mb-1">{t("calendarSchedule.location")}</div>
                <div className="text-sm text-gray-900 font-medium">
                  {schedule.location}
                </div>
              </div>
            </div>
          )}

          {/* Participants */}
          {schedule.participants && schedule.participants.length > 0 && (
            <div className="flex items-start gap-3">
              <div className="flex-shrink-0 w-10 h-10 bg-red-50 text-red-600 rounded-xl flex items-center justify-center">
                <Users className="h-5 w-5" />
              </div>
              <div className="flex-1 pt-1">
                <div className="text-xs text-gray-500 mb-2">{t("calendarSchedule.participants")}</div>
                <div className="flex flex-wrap gap-2">
                  {schedule.participants.map((participant, index) => (
                    <div
                      key={participant.id || index}
                      className="px-3 py-1.5 bg-red-50 text-red-700 rounded-md text-sm font-medium"
                    >
                      {participant.name || participant.participantId}
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-6 pb-6 flex justify-end">
          <Button
            variant="outline"
            onClick={onClose}
            className="px-6"
          >
            {t("modals.close")}
          </Button>
        </div>
      </div>
    </div>
  );

  return createPortal(dialogContent, document.body);
}

