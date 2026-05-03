import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import ContentHeader from "../../components/ui/ContentHeader";
import Button from "../../components/ui/Button";
import TextInput from "../../components/ui/TextInput";
import { Plus, Search, UserRound } from "lucide-react";
import { useNavigate } from "react-router-dom";
import LoadingContent from "../../components/ui/LoadingContent";
import { useUsers } from "../../hooks/useUsers";
import Pagination from "../../components/ui/Pagination";
import { toast } from "react-toastify";
import EmptyState from "../../components/ui/EmptyState";

export default function Users() {
  const { t } = useTranslation();
  const [currentPage, setCurrentPage] = useState(1);
  const [keyword, setKeyword] = useState("");
  const itemsPerPage = 10;

  // Fetch users from server with paging params
  const { data, isLoading, isError, error } = useUsers({ 
    page: currentPage, 
    size: itemsPerPage,
    keyword: keyword || undefined,
  });

  const navigate = useNavigate();

  const users = Array.isArray(data?.data?.result) ? data.data.result : [];
  const meta = data?.data?.meta;

  useEffect(() => {
    if (isError) {
      const errorMessage = error?.response?.data?.message || t("error");
      toast.error(errorMessage);
    }
  }, [isError, error, t]);

  const totalPages = meta?.pages ?? Math.max(1, Math.ceil((meta?.total ?? users.length) / itemsPerPage));

  const goToPage = (page) => {
    if (page >= 1 && page <= totalPages) setCurrentPage(page);
  };

  if (isLoading) {
    return (
      <div className="flex flex-col h-full">
        <ContentHeader
          title={t("users")}
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
              <Button onClick={() => navigate("/users/new")}>
                <Plus className="h-4 w-4 mr-2" />
                {t("addAccount")}
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
        title={t("accountManagement")}
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
            <Button onClick={() => navigate("/users/new")}>
              <Plus className="h-4 w-4 mr-2" />
              {t("addAccount")}
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
                  <th className="p-4 text-left text-sm font-medium text-gray-600">{t("id")}</th>
                  <th className="p-4 text-left text-sm font-medium text-gray-600">{t("employeeName")}</th>
                  <th className="p-4 text-left text-sm font-medium text-gray-600">{t("email")}</th>
                  <th className="p-4 text-left text-sm font-medium text-gray-600">{t("department")}</th>
                  <th className="p-4 text-left text-sm font-medium text-gray-600">{t("role")}</th>
                  <th className="p-4 text-left text-sm font-medium text-gray-600">{t("status")}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {users.length === 0 ? (
                  <tr>
                    <td colSpan="6" className="p-8">
                      <EmptyState title={t("noData")} icon={UserRound} />
                    </td>
                  </tr>
                ) : (
                  users.map((u) => {
                    const isActive = u.active !== undefined ? u.active : true;
                    return (
                      <tr key={u.id} className="hover:bg-gray-50 cursor-pointer" onClick={() => navigate(`/users/${u.id}`)}>
                        <td className="p-4 text-sm text-gray-900">{u.id}</td>
                        <td className="p-4 text-sm text-gray-900">{u.employee?.name || "-"}</td>
                        <td className="p-4 text-sm text-gray-600">{u.email || "-"}</td>
                        <td className="p-4 text-sm text-gray-600">{u.employee?.department?.name || "-"}</td>
                        <td className="p-4 text-sm text-gray-600">{u.employee?.position?.name || "-"}</td>
                        <td className="p-4 text-sm text-gray-600">{isActive ? t("active") : t("inactive")}</td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>

          {users.length > 0 && (
            <div className="flex-shrink-0 flex justify-end items-center p-4 border-t border-gray-200 bg-white">
              <Pagination currentPage={currentPage} totalPages={totalPages} onPageChange={goToPage} />
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
