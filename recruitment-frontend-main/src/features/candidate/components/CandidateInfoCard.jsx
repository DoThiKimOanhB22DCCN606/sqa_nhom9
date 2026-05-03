import { Mail, Phone, Calendar } from "lucide-react";
import { useTranslation } from "react-i18next";

export default function CandidateInfoCard({
  displayName,
  displayEmail,
  displayPhone,
  appliedDate,
}) {
  const { t } = useTranslation();
  const formatDate = (dateString) => {
    if (!dateString) return "-";
    const date = new Date(dateString);
    return date.toLocaleDateString("vi-VN");
  };

  return (
    <div className="bg-white rounded-xl shadow p-6">
      <div className="flex items-center gap-4 mb-4">
        <div className="w-16 h-16 rounded-full bg-gradient-to-br from-red-400 to-red-600 flex items-center justify-center text-white text-xl font-bold flex-shrink-0">
          {displayName?.charAt(0)?.toUpperCase() || "?"}
        </div>
        <div className="flex-1 min-w-0">
          <h3 className="font-semibold text-lg text-gray-900 truncate">
            {displayName}
          </h3>
        </div>
      </div>

      <div className="space-y-3">
        <div className="flex items-center gap-2 text-sm text-gray-600">
          <Mail size={16} className="flex-shrink-0" />
          <span className="truncate">{displayEmail}</span>
        </div>
        {displayPhone !== "-" && (
          <div className="flex items-center gap-2 text-sm text-gray-600">
            <Phone size={16} className="flex-shrink-0" />
            <span>{displayPhone}</span>
          </div>
        )}
        <div className="flex items-center gap-2 text-sm text-gray-600">
          <Calendar size={16} className="flex-shrink-0" />
          <span>{t("appliedDate")}: {formatDate(appliedDate)}</span>
        </div>
      </div>
    </div>
  );
}
