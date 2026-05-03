import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import ContentHeader from "../../components/ui/ContentHeader";
import Card from "../../components/ui/Card";
import TextButton from "../../components/ui/TextButton";
import {
  useSummaryStatistics,
  useUpcomingSchedules,
  useJobOpenings,
} from "../../hooks/useStatistics";
import LoadingSpinner from "../../components/ui/LoadingSpinner";
import EmptyState from "../../components/ui/EmptyState";
import { useTranslation } from "react-i18next";
import { Briefcase, Calendar } from "lucide-react";
import LoadingOverlay from "../../components/ui/LoadingOverlay";
import LoadingContent from "../../components/ui/LoadingContent";

export default function Home() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [customDateRange, setCustomDateRange] = useState({ from: "", to: "" });

  const { data: summaryData, isLoading: isLoadingSummary } =
    useSummaryStatistics();
  const { data: schedulesData, isLoading: isLoadingSchedules } =
    useUpcomingSchedules();
  const { data: jobOpeningsData, isLoading: isLoadingJobOpenings } =
    useJobOpenings();

  const [weeklyData] = useState([
    { day: "T2", applications: 200, interviews: 180 },
    { day: "T3", applications: 250, interviews: 220 },
    { day: "T4", applications: 400, interviews: 350 },
    { day: "T5", applications: 250, interviews: 230 },
    { day: "T6", applications: 220, interviews: 200 },
    { day: "T7", applications: 268, interviews: 240 },
    { day: "CN", applications: 340, interviews: 310 },
  ]);

  const colorMap = {
    purple: "#8B5CF6",
    green: "#84CC16",
    pink: "#EC4899",
    darkgreen: "#10B981",
    blue: "#3B82F6",
    red: "#EF4444",
    yellow: "#F59E0B",
    orange: "#F97316",
  };

  const jobPositions = Array.isArray(jobOpeningsData?.data)
    ? jobOpeningsData.data.map((job) => ({
        id: job.id,
        title: job.title,
        type: job.employmentType,
        level: job.workLocation,
        applications: job.applicantCount,
        range: job.salaryDisplay,
        color: colorMap[job.iconColor] || "#8B5CF6",
      }))
    : [];

  const upcomingEvents = Array.isArray(schedulesData?.data?.schedules)
    ? schedulesData.data.schedules.map((schedule) => ({
        scheduleId: schedule.scheduleId,
        time: schedule.time,
        title: `${schedule.jobTitle} - ${schedule.candidateName}`,
        type: schedule.type === "INTERVIEW" ? "Phỏng vấn" : schedule.type,
        status: schedule.status,
        date: schedule.date,
        color:
          schedule.priority === "HIGH"
            ? "#EF4444"
            : schedule.status === "SCHEDULED"
            ? "#FBBF24"
            : "#86EFAC",
      }))
    : [];

  const stats = {
    applications: summaryData?.data?.applications || {
      value: 0,
      changePercent: 0,
      isIncrease: null,
      changeText: "So với tuần trước",
    },
    hired: summaryData?.data?.hired || {
      value: 0,
      changePercent: 0,
      isIncrease: null,
      changeText: "So với tuần trước",
    },
    interviews: summaryData?.data?.interviews || {
      value: 0,
      changePercent: 0,
      isIncrease: null,
      changeText: "So với tuần trước",
    },
    rejected: summaryData?.data?.rejected || {
      value: 0,
      changePercent: 0,
      isIncrease: null,
      changeText: "So với tuần trước",
    },
  };

  const StatCard = ({
    label,
    value,
    iconBg,
    iconType,
    change,
    isIncrease,
    subtitle,
  }) => (
    <div className="bg-white rounded-xl shadow p-4">
      <div className="flex items-center gap-3 mb-3">
        <div
          className="w-10 h-10 rounded-lg flex items-center justify-center"
          style={{ backgroundColor: iconBg + "15" }}
        >
          {iconType === "users" && (
            <svg
              className="w-5 h-5"
              style={{ color: iconBg }}
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"
              />
            </svg>
          )}
          {iconType === "briefcase" && (
            <svg
              className="w-5 h-5"
              style={{ color: iconBg }}
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M21 13.255A23.931 23.931 0 0112 15c-3.183 0-6.22-.62-9-1.745M16 6V4a2 2 0 00-2-2h-4a2 2 0 00-2 2v2m4 6h.01M5 20h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"
              />
            </svg>
          )}
          {iconType === "star" && (
            <svg
              className="w-5 h-5"
              style={{ color: iconBg }}
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z"
              />
            </svg>
          )}
          {iconType === "x" && (
            <svg
              className="w-5 h-5"
              style={{ color: iconBg }}
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          )}
        </div>
        <p className="text-gray-600 text-sm font-medium">{label}</p>
      </div>
      <p className="text-2xl font-bold text-gray-900 mb-1">{value}</p>
      <div className="flex items-center gap-2">
        {change !== undefined && change !== 0 && isIncrease !== null && (
          <span
            className={`text-xs font-medium flex items-center gap-1 ${
              isIncrease ? "text-green-500" : "text-red-500"
            }`}
          >
            <svg
              className="w-3 h-3"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              {isIncrease ? (
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"
                />
              ) : (
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M13 17h8m0 0V9m0 8l-8-8-4 4-6-6"
                />
              )}
            </svg>
            {change}%
          </span>
        )}
        <span className="text-gray-500 text-xs">{subtitle}</span>
      </div>
    </div>
  );

  const JobPositionCard = ({ position }) => (
    <div className="flex items-center justify-between p-4 hover:bg-gray-50 rounded-lg transition-colors border-b border-gray-100 last:border-0">
      <div className="flex items-center gap-3 flex-1">
        <div
          className="w-12 h-12 rounded-xl flex items-center justify-center"
          style={{ backgroundColor: position.color + "15" }}
        >
          <div
            className="w-6 h-6 rounded-full"
            style={{ backgroundColor: position.color }}
          ></div>
        </div>
        <div className="flex-1">
          <h3 className="font-semibold text-gray-900 mb-1">{position.title}</h3>
          <div className="flex items-center gap-2">
            <span className="text-xs px-2 py-1 bg-gray-100 text-gray-600 rounded-md">
              {position.type}
            </span>
            <span className="text-xs px-2 py-1 bg-gray-100 text-gray-600 rounded-md">
              {position.level}
            </span>
          </div>
        </div>
      </div>
      <div className="text-right">
        <p className="text-xs text-gray-500 mb-1">
          <svg
            className="w-3 h-3 inline mr-1"
            fill="currentColor"
            viewBox="0 0 20 20"
          >
            <path d="M9 6a3 3 0 11-6 0 3 3 0 016 0zM17 6a3 3 0 11-6 0 3 3 0 016 0zM12.93 17c.046-.327.07-.66.07-1a6.97 6.97 0 00-1.5-4.33A5 5 0 0119 16v1h-6.07zM6 11a5 5 0 015 5v1H1v-1a5 5 0 015-5z" />
          </svg>
          {position.applications} {t("applicants")}
        </p>
        <p className="text-sm font-semibold text-gray-900">{position.range}</p>
      </div>
    </div>
  );

  const EventCard = ({ event }) => (
    <div
      className="flex items-start gap-3 p-3 rounded-lg"
      style={{
        backgroundColor: event.color + "20",
        borderLeft: `3px solid ${event.color}`,
      }}
    >
      <div className="flex-1">
        <p className="text-xs font-medium" style={{ color: event.color }}>
          {event.time}
        </p>
        <p className="text-sm font-semibold text-gray-900 mt-1">
          {event.title}
        </p>
        <p className="text-xs text-gray-500 mt-1">{event.type}</p>
      </div>
    </div>
  );

  const maxValue = Math.max(
    ...weeklyData.map((d) => Math.max(d.applications, d.interviews))
  );

  if (isLoadingSummary) {
    return (
      <div className="flex items-center justify-center h-full">
        <LoadingContent />
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full">
      {/* Period Selector */}
      <div className="mb-4 bg-white rounded-xl shadow p-4">
        <div className="flex items-center justify-between gap-4 flex-wrap">
          <h2 className="text-lg font-semibold text-gray-900">
            {t("overviewStatistics")}
          </h2>
          <div className="flex items-center gap-3">
            <label className="text-sm text-gray-600">{t("fromDate")}:</label>
            <input
              type="date"
              value={customDateRange.from}
              onChange={(e) =>
                setCustomDateRange((prev) => ({
                  ...prev,
                  from: e.target.value,
                }))
              }
              className="text-sm border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
            <label className="text-sm text-gray-600">{t("toDate")}:</label>
            <input
              type="date"
              value={customDateRange.to}
              onChange={(e) =>
                setCustomDateRange((prev) => ({ ...prev, to: e.target.value }))
              }
              className="text-sm border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
          </div>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-4">
        <StatCard
          label={t("applicationsLabel")}
          value={stats.applications.value}
          iconBg="#3B82F6"
          iconType="users"
          change={stats.applications.changePercent}
          isIncrease={stats.applications.isIncrease}
          subtitle={stats.applications.changeText}
        />
        <StatCard
          label={t("hiredLabel")}
          value={stats.hired.value}
          iconBg="#10B981"
          iconType="briefcase"
          change={stats.hired.changePercent}
          isIncrease={stats.hired.isIncrease}
          subtitle={stats.hired.changeText}
        />
        <StatCard
          label={t("interviewsLabel")}
          value={stats.interviews.value}
          iconBg="#8B5CF6"
          iconType="star"
          change={stats.interviews.changePercent}
          isIncrease={stats.interviews.isIncrease}
          subtitle={stats.interviews.changeText}
        />
        <StatCard
          label={t("rejectedLabel")}
          value={stats.rejected.value}
          iconBg="#EF4444"
          iconType="x"
          change={stats.rejected.changePercent}
          isIncrease={stats.rejected.isIncrease}
          subtitle={stats.rejected.changeText}
        />
      </div>

      {/* Main Content Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        {/* Left Column - Schedules and Applications */}
        <div className="lg:col-span-2 space-y-4">
          {/* Upcoming Events */}
          <div className="bg-white rounded-xl shadow p-4">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-semibold text-gray-900">
                {t("upcomingSchedule")}
              </h2>
              <TextButton onClick={() => navigate("/calendar")}>{t("viewAll")}</TextButton>
            </div>
            <div className="space-y-2">
              {upcomingEvents.length > 0 ? (
                upcomingEvents.map((event, index) => (
                  <EventCard key={index} event={event} />
                ))
              ) : (
                <EmptyState title={t("noUpcomingSchedules")} icon={Calendar} />
              )}
            </div>
          </div>
        </div>

        {/* Right Column - Job Positions */}
        <div className="lg:col-span-1">
          <div className="bg-white rounded-xl shadow p-4">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-semibold text-gray-900">
                {t("recruitmentPositions")} ({jobPositions.length})
              </h2>
              <TextButton onClick={() => navigate("/job-positions")}>
                {t("viewAll")}
              </TextButton>
            </div>
            <div className="space-y-2">
              {jobPositions.length > 0 ? (
                jobPositions.map((position) => (
                  <JobPositionCard key={position.id} position={position} />
                ))
              ) : (
                <EmptyState title={t("noPositionsFound")} icon={Briefcase} />
              )}
            </div>
          </div>
        </div>
      </div>
      {/* </div> */}
    </div>
  );
}
