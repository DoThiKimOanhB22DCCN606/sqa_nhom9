import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import ContentHeader from "../../components/ui/ContentHeader";
import Button from "../../components/ui/Button";
import TextInput from "../../components/ui/TextInput";
import { Plus, Search, UserRound } from "lucide-react";
import { useNavigate } from "react-router-dom";
import LoadingContent from "../../components/ui/LoadingContent";
import Pagination from "../../components/ui/Pagination";
import { toast } from "react-toastify";
import { useEmployees } from "../../hooks/useEmployees";
import { useAllDepartments, useDepartments } from "../../hooks/useDepartments";
import { usePositions } from "../../hooks/usePositions";
import SelectDropdown from "../../components/ui/SelectDropdown";
import EmptyState from "../../components/ui/EmptyState";

export default function Employees() {
  const { t } = useTranslation();
  const [currentPage, setCurrentPage] = useState(1);
  const [selectedDepartmentId, setSelectedDepartmentId] = useState(null);
  const [selectedPositionId, setSelectedPositionId] = useState(null);
  const [keyword, setKeyword] = useState("");
  const itemsPerPage = 10;
  const navigate = useNavigate();

  const { data, isLoading, isError, error } = useEmployees({
    page: currentPage,
    size: itemsPerPage,
    departmentId: selectedDepartmentId,
    positionId: selectedPositionId,
    keyword: keyword || undefined,
  });
  const employees = Array.isArray(data?.data?.result) ? data.data.result : [];
  const meta = data?.data?.meta;

  const { data: departmentsData } = useAllDepartments();
    const departments = Array.isArray(departmentsData) ? departmentsData : [];

  const { data: positionsData } = usePositions();
  const positions = Array.isArray(positionsData)
    ? positionsData
    : Array.isArray(positionsData?.data?.result)
    ? positionsData.data.result
    : Array.isArray(positionsData?.result)
    ? positionsData.result
    : [];

  useEffect(() => {
    if (isError) {
      const errorMessage = error?.response?.data?.message || t("error");
      toast.error(errorMessage);
    }
  }, [isError, error, t]);

  const totalPages =
    meta?.pages ??
    Math.max(1, Math.ceil((meta?.total ?? employees.length) / itemsPerPage));

  const goToPage = (page) => {
    if (page >= 1 && page <= totalPages) setCurrentPage(page);
  };

  if (isLoading) {
    return (
      <div className="flex flex-col h-full">
        <ContentHeader
          title={t("employees")}
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
              <Button onClick={() => navigate("/employees/new")}>
                {" "}
                <Plus className="h-4 w-4 mr-2" /> {t("addEmployee")}
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
        title={t("employeeManagement")}
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
              value={selectedDepartmentId}
              onChange={setSelectedDepartmentId}
              options={[
                { id: null, name: t("common.allDepartments") },
                ...departments.map((d) => ({ id: d.id, name: d.name })),
              ]}
              placeholder={t("common.allDepartments")}
              hideLabel
              compact
              className="min-w-[200px]"
            />
            <SelectDropdown
              value={selectedPositionId}
              onChange={setSelectedPositionId}
              options={[
                { id: null, name: t("common.allPositions") },
                ...positions.map((p) => ({ id: p.id, name: p.name })),
              ]}
              placeholder={t("common.allPositions")}
              hideLabel
              compact
              className="min-w-[200px]"
            />
            <Button onClick={() => navigate("/employees/new")}>
              <Plus className="h-4 w-4 mr-2" /> {t("addEmployee")}
            </Button>
          </>
        }
      />
      <div className="flex-1 flex flex-col mt-4 min-h-0">
        <div className="flex-1 flex flex-col bg-white rounded-xl shadow overflow-hidden">
          <div className="flex-1 overflow-auto">
            <table className="w-full">
              <thead className="bg-gray-50 border-b border-gray-200 sticky top-0 z-10">
                <tr>
                  <th className="p-4 text-left text-sm font-medium text-gray-600">
                    {t("id")}
                  </th>
                  <th className="p-4 text-left text-sm font-medium text-gray-600">
                    {t("employeeName")}
                  </th>
                  <th className="p-4 text-left text-sm font-medium text-gray-600">
                    {t("email")}
                  </th>
                  <th className="p-4 text-left text-sm font-medium text-gray-600">
                    {t("phone")}
                  </th>
                  <th className="p-4 text-left text-sm font-medium text-gray-600">
                    {t("department")}
                  </th>
                  <th className="p-4 text-left text-sm font-medium text-gray-600">
                    {t("position")}
                  </th>
                  {/* removed position level column as requested */}
                  <th className="p-4 text-left text-sm font-medium text-gray-600">
                    {t("status")}
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {employees.length === 0 ? (
                  <tr>
                    <td colSpan="7" className="p-8">
                      <EmptyState title={t("noData")} icon={UserRound} />
                    </td>
                  </tr>
                ) : (
                  employees.map((e) => {
                    const isActive = e.status === "ACTIVE";
                    return (
                      <tr
                        key={e.id}
                        className="hover:bg-gray-50 cursor-pointer"
                        onClick={() => navigate(`/employees/${e.id}`)}
                      >
                        <td className="p-4 text-sm text-gray-900">{e.id}</td>
                        <td className="p-4 text-sm text-gray-900">
                          {e.name || "-"}
                        </td>
                        <td className="p-4 text-sm text-gray-600">
                          {e.email || "-"}
                        </td>
                        <td className="p-4 text-sm text-gray-600">
                          {e.phone || "-"}
                        </td>
                        <td className="p-4 text-sm text-gray-600">
                          {e.department?.name || "-"}
                        </td>
                        <td className="p-4 text-sm text-gray-600">
                          {e.position?.name || "-"}
                        </td>
                        {/* removed position level cell */}
                        <td className="p-4 text-sm text-gray-600">
                          {isActive ? t("active") : t("inactive")}
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
          {employees.length > 0 && (
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
