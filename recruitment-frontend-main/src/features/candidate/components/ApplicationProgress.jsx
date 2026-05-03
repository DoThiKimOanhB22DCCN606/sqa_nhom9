import { FileText, CheckCircle2, MessageSquare, ChevronDown } from "lucide-react";
import { useTranslation } from "react-i18next";
import { useState, useRef, useEffect } from "react";

export default function ApplicationProgress({ status, isHR, onStatusChange, isUpdating, statuses }) {
  const { t } = useTranslation();
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef(null);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  const steps = [
    {
      key: "SUBMITTED",
      label: t("applicationSteps.submitted"),
      icon: FileText,
    },
    {
      key: "SCREENING",
      label: t("applicationSteps.screening"),
      icon: CheckCircle2,
    },
    {
      key: "INTERVIEW",
      label: t("applicationSteps.interview"),
      icon: MessageSquare,
    },
    // { key: "OFFER", label: t("applicationSteps.offer"), icon: FileText },
    { key: "HIRED", label: t("applicationSteps.hired"), icon: CheckCircle2 },
  ];

  const statusOrder = {
    SUBMITTED: 0,
    REVIEWING: 1,
    INTERVIEW: 2,
    HIRED: 3,
    REJECTED: -1,
    ARCHIVED: -2,
  };

  const currentStep = statusOrder[status] ?? 0;
  const isRejected = status === "REJECTED";
  const isArchived = status === "ARCHIVED";
  const isStatusLocked = isRejected || isArchived;

  const canTransitionTo = (newStatus) => {
    const currentOrder = statusOrder[status] ?? 0;
    const newOrder = statusOrder[newStatus] ?? 0;
    
    // Allow REJECTED and ARCHIVED from any status
    if (newStatus === "REJECTED" || newStatus === "ARCHIVED") {
      return true;
    }
    
    // Don't allow going back to lower status
    return newOrder >= currentOrder;
  };

  const currentStatusObj = statuses?.find(s => s.id === status);
  const availableStatuses = statuses?.filter(s => canTransitionTo(s.id)) || [];

  const handleStatusSelect = (newStatus) => {
    onStatusChange(newStatus);
    setIsOpen(false);
  };

  return (
    <div className="bg-white rounded-xl shadow p-6">
      <div className="flex items-center justify-between mb-6">
        <h3 className="font-semibold text-gray-900 flex items-center gap-2">
          <CheckCircle2 size={18} />
          {t("applicationProgress")}
        </h3>
        {isHR && statuses && !isStatusLocked && (
          <div className="relative" ref={dropdownRef}>
            <button
              onClick={() => setIsOpen(!isOpen)}
              disabled={isUpdating}
              className="flex items-center gap-2 text-sm font-semibold rounded-lg px-4 py-2 border border-gray-200 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:opacity-50 disabled:cursor-not-allowed bg-white cursor-pointer hover:border-gray-300 transition-colors"
            >
              <span
                className="w-2 h-2 rounded-full"
                style={{ backgroundColor: currentStatusObj?.color }}
              />
              <span>{currentStatusObj?.label}</span>
              <ChevronDown size={16} className={`transition-transform ${isOpen ? 'rotate-180' : ''}`} />
            </button>
            
            {isOpen && (
              <div className="absolute right-0 mt-2 w-56 bg-white rounded-lg shadow-lg border border-gray-200 py-1 z-50">
                {availableStatuses.map((s) => (
                  <button
                    key={s.id}
                    onClick={() => handleStatusSelect(s.id)}
                    className={`w-full text-left px-4 py-2.5 text-sm hover:bg-gray-50 transition-colors flex items-center gap-3 ${
                      s.id === status ? 'bg-blue-50' : ''
                    }`}
                  >
                    <span
                      className="w-2.5 h-2.5 rounded-full flex-shrink-0"
                      style={{ backgroundColor: s.color }}
                    />
                    <span className={`${s.id === status ? 'font-semibold text-gray-900' : 'text-gray-700'}`}>
                      {s.label}
                    </span>
                  </button>
                ))}
              </div>
            )}
          </div>
        )}
      </div>

      <div className="relative">
        {/* Progress Bar Background */}
        <div className="absolute top-6 left-6 right-6 h-0.5 bg-gray-200" />

        {/* Progress Bar Fill */}
        {!isRejected && currentStep > 0 && (
          <div
            className="absolute top-6 left-6 h-0.5 bg-red-600 transition-all duration-500"
            style={{ 
              width: `calc((100% - 48px) * ${currentStep / (steps.length - 1)})` 
            }}
          />
        )}

        {/* Steps */}
        <div className="relative flex justify-between">
          {steps.map((step, index) => {
            const Icon = step.icon;
            const isCompleted = index <= currentStep && !isRejected;
            const isCurrent = index === currentStep && !isRejected;

            return (
              <div key={step.key} className="flex flex-col items-center">
                {/* Step Circle */}
                <div
                  className={`w-12 h-12 rounded-full border-2 flex items-center justify-center transition-all ${
                    isCompleted
                      ? "bg-red-600 border-red-600 text-white"
                      : "bg-white border-gray-300 text-gray-400"
                  } ${isCurrent ? "ring-4 ring-red-100" : ""}`}
                >
                  <Icon size={20} />
                </div>

                {/* Step Label */}
                <div className="mt-3 text-center">
                  <div
                    className={`text-sm font-medium ${
                      isCompleted ? "text-gray-900" : "text-gray-500"
                    }`}
                  >
                    {step.label}
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        {/* Rejected Status */}
        {isRejected && (
          <div className="mt-6 p-4 bg-red-50 border border-red-200 rounded-lg">
            <div className="flex items-center gap-2 text-red-700">
              <div className="w-8 h-8 rounded-full bg-red-600 text-white flex items-center justify-center">
                ✕
              </div>
              <span className="font-medium">{t("applicationRejected")}</span>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
