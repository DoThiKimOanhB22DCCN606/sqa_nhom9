import { Users } from "lucide-react";
import { forwardRef } from "react";
import { parseMarkdown } from "../../../utils/utils";
import Button from "../../../components/ui/Button";
import StatusBadge from "../../../components/ui/StatusBadge";
import { useTranslation } from "react-i18next";

const PositionDetail = forwardRef(
  ({ position, onClose, onNavigateToCandidates, isFixed, dimensions }, ref) => {
    const { t } = useTranslation();

    return (
      <div
        ref={ref}
        className={`bg-white rounded-xl shadow flex flex-col transition-none ${
          isFixed ? "fixed z-20" : "flex-1"
        }`}
        style={
          isFixed
            ? {
                top: "92px",
                left: `${dimensions.left}px`,
                width: `${dimensions.width}px`,
                height: "calc(100vh - 108px)", // 92px top + 16px bottom
                overflow: "hidden",
              }
            : {
                maxHeight: "calc(100vh - 108px)",
                overflow: "hidden",
              }
        }
      >
        {/* Header - Fixed */}
        <div className="flex-shrink-0 p-6 pb-4 border-b border-gray-200">
          <div className="flex justify-between items-start gap-4">
            {/* Left: Title and Department */}
            <div className="flex-1 min-w-0">
              <h2 className="text-2xl font-bold text-gray-900 mb-2">
                {position.title}
              </h2>
              <p className="text-gray-600">{position.department}</p>
            </div>

            {/* Right: Status and Close Button */}
            <div className="flex items-center gap-3">
              <StatusBadge status={position.status} />
              <Button variant="outline" onClick={onClose}>
                {t("close")}
              </Button>
            </div>
          </div>
        </div>

        {/* Scrollable Content */}
        <div className="flex-1 overflow-y-auto p-6 min-h-0">
          {/* Details Grid */}
          <div className="grid grid-cols-2 gap-6 mb-6">
            <div>
              <h3 className="text-sm font-medium text-gray-500 mb-2">
                {t("location")}
              </h3>
              <p className="text-gray-900">{position.location}</p>
            </div>
            <div>
              <h3 className="text-sm font-medium text-gray-500 mb-2">
                {t("employmentType")}
              </h3>
              <p className="text-gray-900">{position.employmentType}</p>
            </div>
            <div>
              <h3 className="text-sm font-medium text-gray-500 mb-2">
                {t("level")}
              </h3>
              <p className="text-gray-900 capitalize">{position.level}</p>
            </div>
            <div>
              <h3 className="text-sm font-medium text-gray-500 mb-2">
                {t("experience")}
              </h3>
              <p className="text-gray-900">{position.experience}</p>
            </div>
            <div>
              <h3 className="text-sm font-medium text-gray-500 mb-2">
                {t("salary")}
              </h3>
              <p className="text-[#DC2626] font-semibold">{position.salary}</p>
            </div>
            <div>
              <h3 className="text-sm font-medium text-gray-500 mb-2">
                {t("numberOfPositions")}
              </h3>
              <p className="text-gray-900">
                {position.numberOfPositions || position.quantity}
              </p>
            </div>
            <div>
              <h3 className="text-sm font-medium text-gray-500 mb-2">
                {t("deadline")}
              </h3>
              <p className="text-gray-900">{position.deadline || t("common.notAvailable")}</p>
            </div>
            <div>
              <h3 className="text-sm font-medium text-gray-500 mb-2">
                {t("remoteWork")}
              </h3>
              <p className="text-gray-900">
                {position.remoteWorkAllowed ? t("yes") : t("no")}
              </p>
            </div>
          </div>

          {/* Description */}
          {position.description && (
            <div className="border-t border-gray-200 pt-6">
              <h3 className="text-sm font-medium text-gray-500 mb-2">
                {t("jobDescription")}
              </h3>
              <div 
                className="text-gray-900"
                dangerouslySetInnerHTML={{ __html: parseMarkdown(position.description) }}
              />
            </div>
          )}

          {/* Responsibilities */}
          {position.responsibilities && (
            <div className="border-t border-gray-200 pt-6 mt-6">
              <h3 className="text-sm font-medium text-gray-500 mb-2">
                {t("responsibilities")}
              </h3>
              <div 
                className="text-gray-900"
                dangerouslySetInnerHTML={{ __html: parseMarkdown(position.responsibilities) }}
              />
            </div>
          )}

          {/* Requirements */}
          {position.requirements && (
            <div className="border-t border-gray-200 pt-6 mt-6">
              <h3 className="text-sm font-medium text-gray-500 mb-2">
                {t("requirements")}
              </h3>
              <div 
                className="text-gray-900"
                dangerouslySetInnerHTML={{ __html: parseMarkdown(position.requirements) }}
              />
            </div>
          )}

          {/* Preferred Qualifications */}
          {position.preferredQualifications && (
            <div className="border-t border-gray-200 pt-6 mt-6">
              <h3 className="text-sm font-medium text-gray-500 mb-2">
                {t("preferredQualifications")}
              </h3>
              <div 
                className="text-gray-900"
                dangerouslySetInnerHTML={{ __html: parseMarkdown(position.preferredQualifications) }}
              />
            </div>
          )}

          {/* Benefits */}
          {position.benefits && (
            <div className="border-t border-gray-200 pt-6 mt-6">
              <h3 className="text-sm font-medium text-gray-500 mb-2">
                {t("benefits")}
              </h3>
              <div 
                className="text-gray-900"
                dangerouslySetInnerHTML={{ __html: parseMarkdown(position.benefits) }}
              />
            </div>
          )}

          {/* Applicants */}
          <div className="border-t border-gray-200 pt-6 mt-6">
            <h3 className="text-sm font-medium text-gray-500 mb-2">
              {t("applicants")}
            </h3>
            <div className="flex items-center gap-2">
              <Users className="h-5 w-5 text-gray-400" />
              <span className="text-gray-900 font-medium">
                {position.applicants} {t("applicantsApplied")}
              </span>
            </div>
            <Button className="mt-4" onClick={onNavigateToCandidates}>
              {t("viewCandidateList")}
            </Button>
          </div>
        </div>
      </div>
    );
  }
);

PositionDetail.displayName = "PositionDetail";

export default PositionDetail;
