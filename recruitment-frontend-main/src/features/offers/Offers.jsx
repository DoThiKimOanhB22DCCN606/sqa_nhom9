import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import ContentHeader from "../../components/ui/ContentHeader";
import Button from "../../components/ui/Button";
import Pagination from "../../components/ui/Pagination";
import OfferStatus from "./components/OfferStatus";
import { useOffers } from "./hooks/useOffers";
import { toast } from "react-toastify";
import { useTranslation } from "react-i18next";
import LoadingContent from "../../components/ui/LoadingContent";
import SelectDropdown from "../../components/ui/SelectDropdown";
import { useJobPositions } from "../job-positions/hooks/useJobPositions";
import EmptyState from "../../components/ui/EmptyState";
import { useAuth } from "../../context/AuthContext";
import { Plus } from "lucide-react";

export default function Offers() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [currentPage, setCurrentPage] = useState(1);
  const [selectedJobPositionId, setSelectedJobPositionId] = useState(null);
  const [selectedStatus, setSelectedStatus] = useState(null);
  const itemsPerPage = 10;

  const { data, isLoading, isError, error, refetch } = useOffers();

  const { data: jobPositionsData } = useJobPositions();
  const jobPositions = Array.isArray(jobPositionsData?.data?.result)
    ? jobPositionsData.data.result
    : [];

  const allOffers = Array.isArray(data?.data?.result) ? data.data.result : [];

  const offers = allOffers.filter((o) => {
    const matchesPosition =
      !selectedJobPositionId ||
      o.position?.id === selectedJobPositionId ||
      o.positionId === selectedJobPositionId;
    const matchesStatus = !selectedStatus || o.status === selectedStatus;
    return matchesPosition && matchesStatus;
  });

  // Show toast notification when there's an error
  useEffect(() => {
    if (isError) {
      const errorMessage =
        error?.response?.data?.message || t("offers.errorLoadingOffers");
      toast.error(errorMessage);
    }
  }, [isError, error, t]);

  // Calculate pagination
  const totalPages = Math.ceil(offers.length / itemsPerPage);
  const startIndex = (currentPage - 1) * itemsPerPage;
  const currentOffers = offers.slice(startIndex, startIndex + itemsPerPage);

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
    } catch (e) {
      return dateString;
    }
  };

  const formatSalary = (salary) => {
    if (!salary) return "-";
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
      maximumFractionDigits: 0,
    }).format(salary);
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
          title={t("offers.title")}
        actions={
          <>
            
            <SelectDropdown
              value={selectedJobPositionId}
              onChange={setSelectedJobPositionId}
              options={[
                { id: null, name: t("offers.allPositions") },
                ...jobPositions.map((jp) => ({ id: jp.id, name: jp.title })),
              ]}
              placeholder={t("offers.allPositions")}
              hideLabel
              compact
              className="min-w-[200px]"
            />
            <SelectDropdown
              value={selectedStatus}
              onChange={setSelectedStatus}
              options={[
                { id: null, name: t("offers.allStatuses") },
                { id: "OFFER", name: t("statuses.offer") },
                { id: "HIRED", name: t("statuses.hired") },
                { id: "REJECTED", name: t("statuses.rejected") },
                { id: "PENDING", name: t("statuses.pending") },
              ]}
              placeholder={t("offers.allStatuses")}
              hideLabel
              compact
              className="min-w-[200px]"
            />
            <Button onClick={() => navigate("/offers/new")}>
              <Plus className="h-4 w-4 mr-2" /> {t("offers.newOfferButton")}
            </Button>
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
        title={t("offers.title")}
        actions={
          <>
         
            <SelectDropdown
              value={selectedJobPositionId}
              onChange={setSelectedJobPositionId}
              options={[
                { id: null, name: t("offers.allPositions") },
                ...jobPositions.map((jp) => ({ id: jp.id, name: jp.title })),
              ]}
              placeholder={t("offers.allPositions")}
              hideLabel
              compact
              className="min-w-[200px]"
            />
            <SelectDropdown
              value={selectedStatus}
              onChange={setSelectedStatus}
              options={[
                { id: null, name: t("offers.allStatuses") },
                { id: "OFFER", name: t("statuses.offer") },
                { id: "HIRED", name: t("statuses.hired") },
                { id: "REJECTED", name: t("statuses.rejected") },
                { id: "PENDING", name: t("statuses.pending") },
              ]}
              placeholder={t("offers.allStatuses")}
              hideLabel
              compact
              className="min-w-[200px]"
            />
            <Button onClick={() => navigate("/offers/new")}>
              <Plus className="h-4 w-4 mr-2" /> {t("offers.newOfferButton")}
            </Button>
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
                  {tableHeaders(t("offers.candidateName"))}
                  {tableHeaders(t("position"))}
                  {tableHeaders(t("department"))}
                  {tableHeaders(t("salary"))}
                  {tableHeaders(t("createdDate"))}
                  {tableHeaders(t("status"))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {currentOffers.length === 0 ? (
                  <tr>
                    <td colSpan="7" className="p-8">
                      <EmptyState
                        title={t("offers.noOffersFound")}
                      />
                    </td>
                  </tr>
                ) : (
                  currentOffers.map((offer, idx) => (
                    <tr
                      key={`${offer.id}-${idx}`}
                      className="hover:bg-gray-50 transition-colors cursor-pointer"
                      onClick={() =>
                        navigate(`/offers/${offer.id}`, {
                          state: { offer },
                        })
                      }
                    >
                      <td className="p-4 text-sm text-gray-900">
                        {offer.id}
                      </td>
                      <td className="p-4 text-sm text-gray-900 font-medium">
                        {offer.candidate?.name || "-"}
                      </td>
                      <td className="p-4 text-sm text-gray-900">
                        {offer.position?.name || "-"}
                      </td>
                      <td className="p-4 text-sm text-gray-600">
                        {offer.department?.name || "-"}
                      </td>
                      <td className="p-4 text-sm text-gray-900 font-medium">
                        {formatSalary(offer.position?.salary)}
                      </td>
                      <td className="p-4 text-sm text-gray-600">
                        {formatDate(offer.createdAt)}
                      </td>
                      <td className="p-4">
                        <OfferStatus status={offer.status} />
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {offers.length > 0 && (
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
