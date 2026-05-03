import React, { useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { useParams, useNavigate } from "react-router-dom";
import ContentHeader from "../../components/ui/ContentHeader";
import TextInput from "../../components/ui/TextInput";
import SelectDropdown from "../../components/ui/SelectDropdown";
import Button from "../../components/ui/Button";
import LoadingContent from "../../components/ui/LoadingContent";
import { useAllRoles } from "../../hooks/useRoles";
import { useCreateUser, useUpdateUser, useUser } from "../../hooks/useUsers";
import { useEmployees } from "../../hooks/useEmployees";
import { toast } from "react-toastify";
import useConfirmDialog from "../../hooks/useConfirmDialog";

export default function UserForm() {
  const { t } = useTranslation();
  const { id } = useParams();
  const navigate = useNavigate();
  const formRef = useRef(null);

  const isAddMode = !id;
  const [isEditMode, setIsEditMode] = useState(isAddMode);
  const { ConfirmDialogComponent, showConfirm } = useConfirmDialog();

  const [form, setForm] = useState({
    email: "",
    password: "",
    roleId: null,
    employeeId: null,
  });

  const { data: userData, isLoading: userLoading } = useUser(id, { enabled: !!id });
  const { data: rolesData = [] } = useAllRoles();
  const { data: employeesResp } = useEmployees({ page: 1, size: 1000 });
  const employees = Array.isArray(employeesResp?.data?.result)
    ? employeesResp.data.result
    : [];

  const createUser = useCreateUser();
  const updateUser = useUpdateUser();

  useEffect(() => {
    if (userData) {
      setForm({
        email: userData.email || "",
        password: "",
        roleId: userData.role?.id || null,
        employeeId: userData.employee?.id || null,
      });
    }
  }, [userData]);

  const onChange = (field) => (e) => {
    const value = e?.target ? e.target.value : e;
    setForm((s) => ({ ...s, [field]: value }));
  };

  const handleSubmit = (e) => {
    e?.preventDefault();

    const requiredFieldsMap = isAddMode
      ? {
          email: t("email"),
          password: t("password"),
          roleId: t("role"),
          employeeId: t("chooseEmployee"),
        }
      : {
          email: t("email"),
          roleId: t("role"),
          employeeId: t("chooseEmployee"),
        };

    for (const [field, label] of Object.entries(requiredFieldsMap)) {
      const value = form[field];
      if (
        value === null ||
        value === undefined ||
        String(value).trim() === ""
      ) {
        toast.error(
          `${label} ${t("isRequired")}`
        );
        return;
      }
    }

    const payload = {
      email: form.email,
      roleId: Number(form.roleId),
      employeeId: Number(form.employeeId),
    };

    if (isAddMode) {
      payload.password = form.password;
    } else if (form.password && form.password.trim() !== "") {
      payload.password = form.password;
    }

    if (isAddMode) {
      createUser.mutate(payload, {
        onSuccess: () => {
          navigate("/users");
        },
      });
    } else {
      updateUser.mutate(
        { id, data: payload },
        {
          onSuccess: () => {
            setIsEditMode(false);
          },
        }
      );
    }
  };

  const handleToggleActive = () => {
    if (!userData) return;
    const newActiveState = !userData.active;
    
    showConfirm({
      title: newActiveState
        ? t("confirmActivate")
        : t("confirmDeactivate"),
      message: newActiveState
        ? t("confirmActivateMessage")
        : t("confirmDeactivateMessage"),
      onConfirm: () => {
        updateUser.mutate(
          { id, data: { isActive: newActiveState } },
          {
            onSuccess: () => {
              toast.success(
                newActiveState
                  ? t("accountActivated")
                  : t("accountDeactivated")
              );
            },
            onError: (error) => {
              const message = error.response?.data?.message || t("error");
              toast.error(message);
            },
          }
        );
      },
    });
  };

  const isPending = isAddMode ? createUser.isPending : updateUser.isPending;

  if ((userLoading && !isAddMode) || isPending) {
    return (
      <div className="flex flex-col h-full">
        <ContentHeader
          title={
            isAddMode
              ? t("addAccount")
              : t("accountDetail")
          }
          actions={
            <>
              <Button variant="outline" onClick={() => navigate(-1)}>
                {t("cancel")}
              </Button>
              {isAddMode ? (
                <Button onClick={() => formRef.current?.requestSubmit()}>
                  {t("save")}
                </Button>
              ) : isEditMode ? (
                <Button onClick={() => formRef.current?.requestSubmit()}>
                  {t("save")}
                </Button>
              ) : (
                <Button onClick={() => setIsEditMode(true)}>
                  {t("edit")}
                </Button>
              )}
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
        title={
          isAddMode
            ? t("addAccount")
            : t("accountDetail")
        }
        actions={
          <>
            {isAddMode ? (
              <>
                <Button variant="outline" onClick={() => navigate(-1)}>
                  {t("cancel")}
                </Button>
                <Button
                  onClick={() => formRef.current?.requestSubmit()}
                  disabled={isPending}
                >
                  {t("save")}
                </Button>
              </>
            ) : isEditMode ? (
              <>
                <Button
                  variant="outline"
                  onClick={() => {
                    setIsEditMode(false);
                    if (userData) {
                      setForm({
                        email: userData.email || "",
                        password: "",
                        roleId: userData.role?.id || null,
                        employeeId: userData.employee?.id || null,
                      });
                    }
                  }}
                >
                  {t("cancel")}
                </Button>
                <Button
                  onClick={() => formRef.current?.requestSubmit()}
                  disabled={isPending}
                >
                  {t("save")}
                </Button>
              </>
            ) : (
              <>
                <Button variant="outline" onClick={() => navigate(-1)}>
                  {t("cancel")}
                </Button>
                <Button
                  variant={userData?.active ? "outline" : "primary"}
                  onClick={handleToggleActive}
                  disabled={isPending}
                >
                  {userData?.active
                    ? t("inactive")
                    : t("activate")}
                </Button>
                <Button onClick={() => setIsEditMode(true)}>
                  {t("edit")}
                </Button>
              </>
            )}
          </>
        }
      />

      <div className="flex-1 mt-4">
        <div className="bg-white rounded-xl shadow p-6">
          <form ref={formRef} onSubmit={handleSubmit}>
            <div className="grid grid-cols-1 gap-6">
              <div className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <SelectDropdown
                    label={t("chooseEmployee")}
                    options={employees.map((e) => ({ id: e.id, name: e.name }))}
                    value={form.employeeId}
                    onChange={(v) => {
                      const selectedEmployee = employees.find(e => e.id === v);
                      setForm((s) => ({ 
                        ...s, 
                        employeeId: v,
                        email: selectedEmployee?.email || s.email
                      }));
                    }}
                    placeholder={t("chooseEmployee")}
                    disabled={!isEditMode}
                  />

                  <SelectDropdown
                    label={t("role")}
                    options={rolesData.map((r) => ({ id: r.id, name: r.name }))}
                    value={form.roleId}
                    onChange={(v) => setForm((s) => ({ ...s, roleId: v }))}
                    placeholder={t("chooseRole")}
                    disabled={!isEditMode}
                  />
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <TextInput
                    label={t("email")}
                    type="email"
                    value={form.email}
                    onChange={onChange("email")}
                    required
                    disabled={!isEditMode}
                  />
                  <TextInput
                    label={t("password")}
                    type="password"
                    value={form.password}
                    onChange={onChange("password")}
                    required={isAddMode}
                    disabled={!isEditMode}
                    placeholder={
                      !isAddMode && isEditMode
                        ? t("passwordOptional")
                        : ""
                    }
                  />
                </div>
              </div>
            </div>
          </form>
        </div>
      </div>
      {ConfirmDialogComponent}
    </div>
  );
}
