import { useState } from "react";
import {
  Plus,
  ChevronLeft,
  ChevronRight,
  Calendar as CalendarIcon,
} from "lucide-react";
import { useTranslation } from "react-i18next";
import Button from "../../components/ui/Button";
import { useCalendar as useCalendarQuery } from "./hooks/useCalendar";
import CreateEventModal from "./components/CreateEventModal";
import ScheduleDetailModal from "./components/ScheduleDetailModal";
import ScheduleCard from "./components/ScheduleCard";
import DayTimeline from "./components/DayTimeline";
import MonthView from "./components/MonthView";
import WeekView from "./components/WeekView";
import LoadingContent from "../../components/ui/LoadingContent";

export default function Calendar() {
  const { t } = useTranslation();
  const [currentDate, setCurrentDate] = useState(new Date());
  const [viewMode, setViewMode] = useState("month"); // day, week, month
  const [selectedDate, setSelectedDate] = useState(null);
  const [showCreateEventModal, setShowCreateEventModal] = useState(false);
  const [selectedSchedule, setSelectedSchedule] = useState(null);
  const [showScheduleDetailModal, setShowScheduleDetailModal] = useState(false);

  const toYMD = (date) => {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, "0");
    const d = String(date.getDate()).padStart(2, "0");
    return `${y}-${m}-${d}`;
  };

  const getWeekStartEnd = (date) => {
    const start = new Date(date);
    const day = start.getDay();
    start.setDate(start.getDate() - day);
    const end = new Date(start);
    end.setDate(start.getDate() + 6);
    return { start, end };
  };

  const calendarParams = (() => {
    if (viewMode === "day") {
      const day = selectedDate || currentDate;
      return { day: toYMD(day) };
    }
    if (viewMode === "week") {
      const { start, end } = getWeekStartEnd(currentDate);
      return { startDate: toYMD(start), endDate: toYMD(end) };
    }
    // month
    return {
      month: currentDate.getMonth() + 1,
      year: currentDate.getFullYear(),
    };
  })();

  const { data: calendarData, isLoading } = useCalendarQuery(calendarParams);

  // Extract schedules from API response
  const schedules = Array.isArray(calendarData?.data?.result)
    ? calendarData.data.result
    : Array.isArray(calendarData?.data)
    ? calendarData.data
    : Array.isArray(calendarData?.result)
    ? calendarData.result
    : [];

  // Helper functions
  const getDaysInMonth = (date) => {
    const year = date.getFullYear();
    const month = date.getMonth();
    return new Date(year, month + 1, 0).getDate();
  };

  const getFirstDayOfMonth = (date) => {
    const year = date.getFullYear();
    const month = date.getMonth();
    return new Date(year, month, 1).getDay();
  };

  const formatDate = (date, format = "full") => {
    const options = {
      full: { weekday: "long", year: "numeric", month: "long", day: "numeric" },
      monthYear: { year: "numeric", month: "long" },
      short: { month: "short", day: "numeric" },
    };
    return date.toLocaleDateString("vi-VN", options[format] || options.full);
  };

  const isToday = (day) => {
    const today = new Date();
    return (
      day === today.getDate() &&
      currentDate.getMonth() === today.getMonth() &&
      currentDate.getFullYear() === today.getFullYear()
    );
  };

  const isSameDate = (day, month = currentDate.getMonth()) => {
    if (!selectedDate) return false;
    return (
      day === selectedDate.getDate() &&
      month === selectedDate.getMonth() &&
      currentDate.getFullYear() === selectedDate.getFullYear()
    );
  };

  // Helper: Check if a schedule falls on a specific date
  const isScheduleOnDate = (schedule, date) => {
    if (!schedule.startTime) return false;
    const scheduleDate = new Date(schedule.startTime);
    return (
      scheduleDate.getDate() === date.getDate() &&
      scheduleDate.getMonth() === date.getMonth() &&
      scheduleDate.getFullYear() === date.getFullYear()
    );
  };

  // Helper: Format time from ISO string
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

  // Helper: Get schedules for a specific date
  const getSchedulesForDate = (date) => {
    return schedules.filter((schedule) => isScheduleOnDate(schedule, date));
  };

  // Helper: Get hour and minute from time string
  const getTimeFromString = (timeString) => {
    if (!timeString) return { hour: 0, minute: 0 };
    try {
      const date = new Date(timeString);
      return { hour: date.getHours(), minute: date.getMinutes() };
    } catch {
      return { hour: 0, minute: 0 };
    }
  };

  // Helper: Calculate position and height for schedule in timeline
  const getSchedulePosition = (schedule) => {
    const startTime = getTimeFromString(schedule.startTime);
    const endTime = getTimeFromString(schedule.endTime);

    // Each hour is 60px, each minute is 1px
    const startMinutes = startTime.hour * 60 + startTime.minute;
    const endMinutes = endTime
      ? endTime.hour * 60 + endTime.minute
      : startMinutes + 60;
    const durationMinutes = endMinutes - startMinutes;

    return {
      top: startMinutes, // in minutes from 0:00
      height: Math.max(durationMinutes, 30), // minimum 30 minutes height
    };
  };

  const handlePrevMonth = () => {
    setCurrentDate(
      new Date(currentDate.getFullYear(), currentDate.getMonth() - 1)
    );
  };

  const handleNextMonth = () => {
    setCurrentDate(
      new Date(currentDate.getFullYear(), currentDate.getMonth() + 1)
    );
  };

  const handlePrevDay = () => {
    const newDate = new Date(currentDate);
    newDate.setDate(currentDate.getDate() - 1);
    setCurrentDate(newDate);
    setSelectedDate(newDate);
  };

  const handleNextDay = () => {
    const newDate = new Date(currentDate);
    newDate.setDate(currentDate.getDate() + 1);
    setCurrentDate(newDate);
    setSelectedDate(newDate);
  };

  const handlePrevWeek = () => {
    const newDate = new Date(currentDate);
    newDate.setDate(currentDate.getDate() - 7);
    setCurrentDate(newDate);
  };

  const handleNextWeek = () => {
    const newDate = new Date(currentDate);
    newDate.setDate(currentDate.getDate() + 7);
    setCurrentDate(newDate);
  };

  const handleDateClick = (day, month = currentDate.getMonth()) => {
    const clickedDate = new Date(currentDate.getFullYear(), month, day);

    // Toggle: if clicking on the same date, hide the details panel
    if (
      selectedDate &&
      clickedDate.getDate() === selectedDate.getDate() &&
      clickedDate.getMonth() === selectedDate.getMonth() &&
      clickedDate.getFullYear() === selectedDate.getFullYear()
    ) {
      setSelectedDate(null);
    } else {
      setSelectedDate(clickedDate);
    }

    // If clicking on a day from previous or next month, change the current month
    if (month !== currentDate.getMonth()) {
      setCurrentDate(new Date(currentDate.getFullYear(), month, 1));
    }
  };

  const handleCreateEvent = () => {
    setShowCreateEventModal(true);
  };

  const handleScheduleClick = (schedule) => {
    setSelectedSchedule(schedule);
    setShowScheduleDetailModal(true);
  };

  const handleCloseScheduleDetail = () => {
    setShowScheduleDetailModal(false);
    setSelectedSchedule(null);
  };

  if (isLoading) {
    return (
      <div className="flex flex-col h-full overflow-hidden">
        <div className="flex-1 flex items-center justify-center">
          <LoadingContent />
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full overflow-hidden">
      {/* Main Content */}
      <div className="flex-1 flex gap-4 min-h-0 overflow-hidden">
        {/* Left Side - Calendar */}
        <div className="flex-1 flex flex-col bg-white rounded-xl shadow overflow-hidden min-h-0">
          {/* Calendar Header */}
          <div className="flex-shrink-0 p-4 border-b border-gray-200">
            <div className="flex items-center justify-between">
              {/* Left: Navigation Buttons */}
              <div className="flex items-center gap-2">
                <div
                  onClick={
                    viewMode === "day"
                      ? handlePrevDay
                      : viewMode === "week"
                      ? handlePrevWeek
                      : handlePrevMonth
                  }
                  className="p-2 hover:bg-gray-100 rounded-lg transition-colors cursor-pointer"
                >
                  <ChevronLeft className="h-5 w-5 text-gray-600" />
                </div>

                <div className="flex items-center gap-2 flex-1 justify-center">
                  {/* Current Date Display - Centered */}
                  <div className="flex items-center gap-2">
                    <CalendarIcon className="h-5 w-5 text-gray-600" />
                    <input
                      type={viewMode === "month" ? "month" : "date"}
                      value={
                        viewMode === "day"
                          ? toYMD(currentDate)
                          : viewMode === "month"
                          ? `${currentDate.getFullYear()}-${String(
                              currentDate.getMonth() + 1
                            ).padStart(2, "0")}`
                          : `${currentDate.getFullYear()}-${String(
                              currentDate.getMonth() + 1
                            ).padStart(2, "0")}-01`
                      }
                      onChange={(e) => {
                        const selectedValue = e.target.value;
                        if (viewMode === "month") {
                          // For month input, value is "YYYY-MM"
                          const [year, month] = selectedValue.split("-");
                          const selectedDate = new Date(
                            parseInt(year),
                            parseInt(month) - 1,
                            1
                          );
                          setCurrentDate(selectedDate);
                        } else {
                          const selectedDate = new Date(selectedValue);
                          if (viewMode === "day") {
                            setCurrentDate(selectedDate);
                            setSelectedDate(selectedDate);
                          } else {
                            setCurrentDate(selectedDate);
                          }
                        }
                      }}
                      className="text-xl font-semibold text-gray-400 border-none outline-none bg-transparent cursor-pointer hover:bg-gray-50 hover:text-gray-900 rounded px-2 py-1 focus:text-gray-900 [&::-webkit-calendar-picker-indicator]:opacity-0 [&::-webkit-calendar-picker-indicator]:absolute [&::-webkit-calendar-picker-indicator]:cursor-pointer"
                      style={{
                        position: "relative",
                      }}
                      onCopy={(e) => {
                        e.preventDefault();
                        return false;
                      }}
                      onContextMenu={(e) => {
                        e.preventDefault();
                        return false;
                      }}
                      title={
                        viewMode === "month"
                          ? t("selectMonthQuick")
                          : t("selectDateQuick")
                      }
                    />
                  </div>

                  <div
                    onClick={
                      viewMode === "day"
                        ? handleNextDay
                        : viewMode === "week"
                        ? handleNextWeek
                        : handleNextMonth
                    }
                    className="p-2 hover:bg-gray-100 rounded-lg transition-colors cursor-pointer"
                  >
                    <ChevronRight className="h-5 w-5 text-gray-600" />
                  </div>
                </div>
              </div>

              {/* Right: View Mode Toggle and Create Button */}
              <div className="flex items-center gap-3">
                <div className="flex gap-1 bg-gray-100 rounded-lg p-1">
                  <div
                    onClick={() => {
                      setViewMode("day");
                      if (!selectedDate) {
                        setSelectedDate(currentDate);
                      }
                    }}
                    className={`
                      px-3 py-1.5 rounded-md text-sm font-medium transition-colors cursor-pointer
                      ${
                        viewMode === "day"
                          ? "bg-white text-red-600 shadow-sm"
                          : "text-gray-600 hover:text-gray-900"
                      }
                    `}
                  >
                    {t("day")}
                  </div>
                  <div
                    onClick={() => setViewMode("week")}
                    className={`
                      px-3 py-1.5 rounded-md text-sm font-medium transition-colors cursor-pointer
                      ${
                        viewMode === "week"
                          ? "bg-white text-red-600 shadow-sm"
                          : "text-gray-600 hover:text-gray-900"
                      }
                    `}
                  >
                    {t("week")}
                  </div>
                  <div
                    onClick={() => setViewMode("month")}
                    className={`
                      px-3 py-1.5 rounded-md text-sm font-medium transition-colors cursor-pointer
                      ${
                        viewMode === "month"
                          ? "bg-white text-red-600 shadow-sm"
                          : "text-gray-600 hover:text-gray-900"
                      }
                    `}
                  >
                    {t("month")}
                  </div>
                </div>

                {/* Create Button */}
                <Button onClick={handleCreateEvent} className="text-sm">
                  <Plus className="h-4 w-4 mr-2" />
                  {t("createSchedule")}
                </Button>
              </div>
            </div>
          </div>

          {/* Calendar Grid */}
          <div
            className={`flex-1 min-h-0 ${
              viewMode === "day" ? "overflow-hidden" : "overflow-auto"
            } p-4`}
          >
            {viewMode === "month" && (
              <div className="h-full">
                {/* Day Headers */}
                <div className="grid grid-cols-7 gap-0 mb-2">
                  {[t("sunday"), t("monday"), t("tuesday"), t("wednesday"), t("thursday"), t("friday"), t("saturday")].map((day) => (
                    <div
                      key={day}
                      className="text-center text-sm font-semibold text-gray-600 py-2"
                    >
                      {day}
                    </div>
                  ))}
                </div>

                {/* Calendar Days */}
                <div className="grid grid-cols-7 gap-0 auto-rows-fr">
                  <MonthView
                    currentDate={currentDate}
                    selectedDate={selectedDate}
                    schedules={schedules}
                    isScheduleOnDate={isScheduleOnDate}
                    isToday={isToday}
                    isSameDate={isSameDate}
                    getSchedulesForDate={getSchedulesForDate}
                    handleDateClick={handleDateClick}
                    handleScheduleClick={handleScheduleClick}
                    getDaysInMonth={getDaysInMonth}
                    getFirstDayOfMonth={getFirstDayOfMonth}
                  />
                </div>
              </div>
            )}

            {viewMode === "week" && (
              <div className="h-full">
                <div className="flex h-full gap-0">
                  <WeekView
                    currentDate={currentDate}
                    selectedDate={selectedDate}
                    schedules={schedules}
                    isToday={isToday}
                    getSchedulesForDate={getSchedulesForDate}
                    handleDateClick={handleDateClick}
                    handleScheduleClick={handleScheduleClick}
                  />
                </div>
              </div>
            )}

            {viewMode === "day" && (
              <div className="flex-1 min-h-0 overflow-hidden flex flex-col">
                {getSchedulesForDate(selectedDate || currentDate).length ===
                0 ? (
                  <div className="flex-1 flex items-center justify-center text-center text-gray-400">
                    <div>
                      <CalendarIcon className="h-12 w-12 mx-auto mb-3 text-gray-300" />
                      <p className="text-sm">
                        {t("noEventsToday")}
                      </p>
                    </div>
                  </div>
                ) : (
                  <div className="flex-1 min-h-0 overflow-y-auto">
                    <DayTimeline
                      schedules={getSchedulesForDate(
                        selectedDate || currentDate
                      )}
                      getSchedulePosition={getSchedulePosition}
                      paddingTop={8}
                      handleScheduleClick={handleScheduleClick}
                    />
                  </div>
                )}
              </div>
            )}
          </div>
        </div>

        {/* Right Side - Event Details */}
        {selectedDate && viewMode !== "day" && (
          <div className="w-80 bg-white rounded-xl shadow p-6 flex flex-col">
            <div className="mb-4">
              <h3 className="text-lg font-semibold text-gray-900 mb-1">
                {t("dayDetails")}
              </h3>
              <p className="text-sm text-gray-600">
                {formatDate(selectedDate, "full")}
              </p>
            </div>

            <div className="flex-1 overflow-auto">
              <div className="space-y-2">
                {selectedDate &&
                  getSchedulesForDate(selectedDate).map((schedule) => (
                    <ScheduleCard
                      key={schedule.id}
                      schedule={schedule}
                      onClick={() => handleScheduleClick(schedule)}
                    />
                  ))}
                {(!selectedDate ||
                  getSchedulesForDate(selectedDate).length === 0) && (
                  <div className="text-center text-gray-400 py-10">
                    <CalendarIcon className="h-12 w-12 mx-auto mb-3 text-gray-300" />
                    <p className="text-sm">{t("noEvents")}</p>
                  </div>
                )}
              </div>
            </div>

            <Button onClick={handleCreateEvent} className="mt-4 w-full text-sm">
              {t("addEvent")}
            </Button>
          </div>
        )}
      </div>

      {/* Create Event Modal */}
      <CreateEventModal
        isOpen={showCreateEventModal}
        onClose={() => setShowCreateEventModal(false)}
        defaultDate={selectedDate || currentDate}
      />

      {/* Schedule Detail Modal */}
      <ScheduleDetailModal
        schedule={selectedSchedule}
        isOpen={showScheduleDetailModal}
        onClose={handleCloseScheduleDetail}
      />
    </div>
  );
}
