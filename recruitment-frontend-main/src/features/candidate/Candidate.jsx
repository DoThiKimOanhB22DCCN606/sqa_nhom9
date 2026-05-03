import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import ContentHeader from "../../components/ui/ContentHeader";
import Button from "../../components/ui/Button";
import TextInput from "../../components/ui/TextInput";
import { Search, UsersRound } from "lucide-react";
import Pagination from "../../components/ui/Pagination";
import CandidateStatus from "./components/CandidateStatus";
import { useCandidates } from "./hooks/useCandidates";
import { toast } from "react-toastify";
import { useTranslation } from "react-i18next";
import LoadingContent from "../../components/ui/LoadingContent";
import SelectDropdown from "../../components/ui/SelectDropdown";
import { useJobPositions } from "../job-positions/hooks/useJobPositions";
import EmptyState from "../../components/ui/EmptyState";
import { useAuth } from "../../context/AuthContext";

export default function Candidate() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [currentPage, setCurrentPage] = useState(1);
  const [selectedJobPositionId, setSelectedJobPositionId] = useState(null);
  const [selectedStatus, setSelectedStatus] = useState(null);
  const [keyword, setKeyword] = useState("");
  const itemsPerPage = 10;

  // Determine if user is HR or Director
  const isHR = user?.department?.id === 2;
  const isDirector = user?.department?.id === 1;
  
  // Prepare query params - filter by department if not HR or Director
  const queryParams = {};
  if (!isHR && !isDirector && user?.department?.id) {
    queryParams.departmentId = user.department.id;
  }
  if (keyword) {
    queryParams.keyword = keyword;
  }

  // Fetch candidates with department filtering
  const { data, isLoading, isError, error, refetch } = useCandidates(queryParams);

  // Fetch job positions for filter
  const { data: jobPositionsData } = useJobPositions();
  const jobPositions = Array.isArray(jobPositionsData?.data?.result) 
    ? jobPositionsData.data.result 
    : [];

  // Get candidates from the query data
  const allCandidates = Array.isArray(data?.data?.result) ? data.data.result : [];
  
  // Filter by selected job position and status
  const candidates = allCandidates.filter((c) => {
    const matchesPosition = !selectedJobPositionId || 
      c.jobPosition?.id === selectedJobPositionId || 
      c.jobPositionId === selectedJobPositionId;
    const matchesStatus = !selectedStatus || c.status === selectedStatus;
    return matchesPosition && matchesStatus;
  });

  // Show toast notification when there's an error
  useEffect(() => {
    if (isError) {
      const errorMessage =
        error?.response?.data?.message || t("errorLoadingCandidates");
      toast.error(errorMessage);
    }
  }, [isError, error, t]);

  const totalPages = Math.ceil(candidates.length / itemsPerPage);
  const startIndex = (currentPage - 1) * itemsPerPage;
  const endIndex = startIndex + itemsPerPage;
  const currentCandidates = candidates.slice(startIndex, endIndex);

  const goToPage = (page) => {
    if (page >= 1 && page <= totalPages) {
      setCurrentPage(page);
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return "-";
    try {
      const date = new Date(dateString);
      const day = String(date.getDate()).padStart(2, "0");
      const month = String(date.getMonth() + 1).padStart(2, "0");
      const year = date.getFullYear();
      return `${day}/${month}/${year}`;
    } catch {
      return dateString;
    }
  };

  const tableHeaders = (header) => {
    return (
      <th className="p-4 text-left text-sm font-medium text-gray-600 whitespace-nowrap">
        {header}
      </th>
    );
  };

  if (isLoading) {
    return (
      <div className="flex flex-col h-full">
        <ContentHeader
          title={t("listCandidate")}
          actions={
            <>
              <TextInput
                placeholder={t("common.search")}
                value={keyword}
                onChange={(e) => {
                  setKeyword(e.target.value);
                  setCurrentPage(1);
                }}
                icon={Search}
                hideLabel
                className="w-[300px]"
              />
              <SelectDropdown
                value={selectedJobPositionId}
                onChange={setSelectedJobPositionId}
                options={[
                  { id: null, name: t("candidates.allPositions") },
                  ...jobPositions.map((jp) => ({ id: jp.id, name: jp.title })),
                ]}
                placeholder={t("candidates.allPositions")}
                hideLabel
                compact
                className="min-w-[200px]"
              />
              <SelectDropdown
                value={selectedStatus}
                onChange={setSelectedStatus}
                options={[
                  { id: null, name: t("candidates.allStatuses") },
                  { id: "SUBMITTED", name: t("statuses.submitted") },
                  { id: "REVIEWING", name: t("statuses.reviewing") },
                  { id: "INTERVIEW", name: t("statuses.interview") },
                  { id: "OFFER", name: t("statuses.offer") },
                  { id: "HIRED", name: t("statuses.hired") },
                  { id: "REJECTED", name: t("statuses.rejected") },
                  { id: "ARCHIVED", name: t("statuses.archived") },
                ]}
                placeholder={t("candidates.allStatuses")}
                hideLabel
                compact
                className="min-w-[200px]"
              />
            </>
          }
        />
        <div className="flex-1 flex items-center justify-center mt-4">
          <LoadingContent />
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full">
      <ContentHeader
        title={t("listCandidate")}
        actions={
          <>
            <TextInput
              placeholder={t("common.search")}
              value={keyword}
              onChange={(e) => {
                setKeyword(e.target.value);
                setCurrentPage(1);
              }}
              icon={Search}
              hideLabel
              className="w-[300px]"
            />
            <SelectDropdown
              value={selectedJobPositionId}
              onChange={setSelectedJobPositionId}
              options={[
                { id: null, name: t("candidates.allPositions") },
                ...jobPositions.map((jp) => ({ id: jp.id, name: jp.title })),
              ]}
              placeholder={t("candidates.allPositions")}
              hideLabel
              compact
              className="min-w-[200px]"
            />
            <SelectDropdown
              value={selectedStatus}
              onChange={setSelectedStatus}
              options={[
                { id: null, name: t("candidates.allStatuses") },
                { id: "SUBMITTED", name: t("statuses.submitted") },
                { id: "REVIEWING", name: t("statuses.reviewing") },
                { id: "INTERVIEW", name: t("statuses.interview") },
                { id: "OFFER", name: t("statuses.offer") },
                { id: "HIRED", name: t("statuses.hired") },
                { id: "REJECTED", name: t("statuses.rejected") },
                { id: "ARCHIVED", name: t("statuses.archived") },
              ]}
              placeholder={t("candidates.allStatuses")}
              hideLabel
              compact
              className="min-w-[200px]"
            />
          </>
        }
      />

      <div className="flex-1 flex flex-col mt-4 min-h-0">
        <div className="flex-1 flex flex-col bg-white rounded-xl shadow overflow-hidden">
          {/* Table */}
          <div className="flex-1 overflow-auto">
            <table className="w-full">
              <thead className="bg-gray-50 border-b border-gray-200 sticky top-0 z-10">
                <tr>
                  {tableHeaders(t("id"))}
                  {tableHeaders(t("candidateName"))}
                  {tableHeaders(t("email"))}
                  {tableHeaders(t("phone"))}
                  {tableHeaders(t("appliedPosition"))}
                  {tableHeaders(t("appliedDate"))}
                  {tableHeaders(t("status"))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {currentCandidates.length === 0 ? (
                  <tr>
                    <td colSpan="8" className="p-8">
                      <EmptyState title={t("noCandidatesFound", { defaultValue: "Không có ứng viên" })} icon={UsersRound}/>
                    </td>
                  </tr>
                ) : (
                  currentCandidates.map((candidate) => (
                    <tr
                      key={candidate.id}
                      className="hover:bg-gray-50 transition-colors cursor-pointer"
                      onClick={() =>
                        navigate(`/candidates/${candidate.id}`, {
                          state: { candidate },
                        })
                      }
                    >
                      <td className="p-4 text-sm text-gray-900">
                        {candidate.id}
                      </td>
                      <td className="p-4 text-sm text-gray-900 font-medium">
                        {candidate.name || "-"}
                      </td>
                      <td className="p-4 text-sm text-gray-600">
                        {candidate.email || "-"}
                      </td>
                      <td className="p-4 text-sm text-gray-600">
                        {candidate.phone || "-"}
                      </td>
                      <td className="p-4 text-sm text-gray-900">
                        {candidate.jobPosition?.title ||
                          candidate.jobPositionTitle ||
                          "-"}
                      </td>
                      <td className="p-4 text-sm text-gray-600">
                        {formatDate(candidate.appliedDate)}
                      </td>
                      <td className="p-4">
                        <CandidateStatus status={candidate.status} />
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {candidates.length > 0 && (
            <div className="flex-shrink-0 flex justify-end items-center p-4 border-t border-gray-200 bg-white">
              <Pagination
                currentPage={currentPage}
                totalPages={totalPages}
                onPageChange={goToPage}
              />
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
