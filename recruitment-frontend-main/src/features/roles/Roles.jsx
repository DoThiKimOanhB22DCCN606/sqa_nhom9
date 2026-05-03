import React from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import ContentHeader from "../../components/ui/ContentHeader";
import LoadingContent from "../../components/ui/LoadingContent";
import { useAllRoles } from "../../hooks/useRoles";
import Button from "../../components/ui/Button";
import { Plus } from "lucide-react";
import EmptyState from "../../components/ui/EmptyState";

export default function Roles() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { data: roles = [], isLoading } = useAllRoles();

  if (isLoading) {
    return (
      <div className="flex flex-col h-full">
        <ContentHeader title={t("roles")} actions={<Button onClick={() => navigate("/roles/new")}><Plus className="h-4 w-4 mr-2" />{t("addRole")}</Button>} />
        <div className="flex-1 flex items-center justify-center mt-4">
          <LoadingContent />
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full">
      <ContentHeader title={t("roleManagement", { defaultValue: "Role Management" })} actions={<Button onClick={() => navigate("/roles/new")}><Plus className="h-4 w-4 mr-2" />{t("addRole", { defaultValue: "Add Role" })}</Button>} />
      <div className="flex-1 flex flex-col mt-4 min-h-0">
        <div className="flex-1 bg-white rounded-xl shadow overflow-hidden">
          <div className="overflow-auto">
            <table className="w-full">
              <thead className="bg-gray-50 border-b border-gray-200 sticky top-0 z-10">
                <tr>
                  <th className="p-4 text-left text-sm font-medium text-gray-600">{t("id")}</th>
                  <th className="p-4 text-left text-sm font-medium text-gray-600">{t("role")}</th>
                  <th className="p-4 text-left text-sm font-medium text-gray-600">{t("permissions.label") || "Permissions"}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {roles.length === 0 ? (
                  <tr>
                    <td colSpan="3" className="p-8">
                      <EmptyState title={t("noData")} />
                    </td>
                  </tr>
                ) : (
                  roles.map((r) => (
                    <tr key={r.id} className="hover:bg-gray-50 cursor-pointer" onClick={() => navigate(`/roles/${r.id}`)}>
                      <td className="p-4 text-sm text-gray-900">{r.id}</td>
                      <td className="p-4 text-sm text-gray-900">{r.name}</td>
                      <td className="p-4 text-sm text-gray-600">{Array.isArray(r.permissions) ? r.permissions.length : r.permissionCount ?? "-"}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
