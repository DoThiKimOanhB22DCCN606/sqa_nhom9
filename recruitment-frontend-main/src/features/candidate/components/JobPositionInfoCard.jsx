import { Briefcase } from "lucide-react";
import { formatSalary } from "../../../utils/utils";
import { useTranslation } from "react-i18next";

export default function JobPositionInfoCard({
  jobPositionTitle,
  departmentName,
  salaryMin,
  salaryMax,
  currency,
  experienceLevel,
  yearsOfExperience,
}) {
  const { t } = useTranslation();
  const salaryText =
    salaryMin || salaryMax
      ? `${currency === "VND" ? "₫" : ""} ${
          salaryMin ? formatSalary(salaryMin, t("common.notAvailable")) : "?"
        } - ${salaryMax ? formatSalary(salaryMax, t("common.notAvailable")) : "?"}`
      : "-";

  return (
    <div className="bg-white rounded-xl shadow p-6">
      <div className="space-y-3">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-lg bg-gray-100 flex items-center justify-center text-gray-700 flex-shrink-0">
            <Briefcase size={24} />
          </div>
          <div className="min-w-0">
            <div className="font-medium text-gray-900 truncate">
              {jobPositionTitle || "-"}
            </div>
            <div className="text-gray-600 mt-0.5 truncate">
              {departmentName || "-"}
            </div>
          </div>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <div className="text-sm text-gray-500 mb-1">{t("experienceLevel")}</div>
            <div className="text-gray-900">{experienceLevel || "-"}</div>
          </div>
          <div>
            <div className="text-sm text-gray-500 mb-1">{t("yearsExperience")}</div>
            <div className="text-gray-900">{yearsOfExperience || "-"}</div>
          </div>
        </div>
        <div>
          <div className="text-sm text-gray-500 mb-1">{t("salary")}</div>
          <div className="text-gray-900">{salaryText}</div>
        </div>
      </div>
    </div>
  );
}
