import React, { useRef, useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import ContentHeader from "../../components/ui/ContentHeader";
import TextInput from "../../components/ui/TextInput";
import SelectDropdown from "../../components/ui/SelectDropdown";
import Button from "../../components/ui/Button";
import { toast } from "react-toastify";
import { useCreateEmployee, useUpdateEmployee, useEmployee, useDeleteEmployee } from "../../hooks/useEmployees";
import { useAllDepartments } from "../../hooks/useDepartments";
import { useAllPositions } from "../../hooks/usePositions";
import FileUploader from "../../components/ui/FileUploader";
import LoadingContent from "../../components/ui/LoadingContent";
import LoadingOverlay from "../../components/ui/LoadingOverlay";
import { uploadServices } from "../../services/uploadServices";

export default function EmployeeForm() {
  const { id } = useParams();
  const isEditPage = !!id;
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { data: departmentsData = [] } = useAllDepartments();
  const { data: positionsData = [] } = useAllPositions();
  const createEmployee = useCreateEmployee();
  const updateEmployee = useUpdateEmployee();
  const deleteEmployee = useDeleteEmployee();
  const { data: employeeData, isLoading: isLoadingEmployee } = useEmployee(id, { enabled: isEditPage });
  const formRef = useRef(null);

  const [isEditMode, setIsEditMode] = useState(!isEditPage);
  const [uploading, setUploading] = useState(false);
  const [form, setForm] = useState({
    name: "",
    phone: "",
    email: "",
    gender: "",
    address: "",
    nationality: "",
    dateOfBirth: "",
    idNumber: "",
    departmentId: null,
    positionId: "",
  });

  const [avatarUrl, setAvatarUrl] = useState(null);
  const [avatarPreview, setAvatarPreview] = useState(null);

  const employee = employeeData?.data || {};

  useEffect(() => {
    if (isEditPage && employee && Object.keys(employee).length > 0) {
      setForm({
        name: employee.name || "",
        phone: employee.phone || "",
        email: employee.email || "",
        gender: employee.gender || "",
        address: employee.address || "",
        nationality: employee.nationality || "",
        dateOfBirth: employee.dateOfBirth || "",
        idNumber: employee.idNumber || "",
        departmentId: employee.department?.id || null,
        positionId: employee.position?.id || "",
      });
      if (employee.avatar) {
        setAvatarPreview(employee.avatar);
      }
    }
  }, [employee, isEditPage]);

  const onChange = (field) => (e) => {
    const value = e?.target ? e.target.value : e;
    setForm((s) => ({ ...s, [field]: value }));
  };

  const handleAvatarChange = async (fileData) => {
    if (!fileData) {
      setAvatarUrl(null);
      return;
    }

    if (fileData instanceof File) {
      setUploading(true);
      try {
        const response = await uploadServices.uploadFile(fileData);
        const url = response.data.url || response.data;
        setAvatarUrl(url);
        setAvatarPreview(url);
      } catch (error) {
        console.error("Failed to upload avatar:", error);
        toast.error(t("toasts.uploadFailed"));
      } finally {
        setUploading(false);
      }
    } else {
      setAvatarUrl(fileData);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const requiredFieldsMap = {
      name: t("employeeName"),
      phone: t("phone"),
      email: t("email"),
      gender: t("gender"),
      address: t("address"),
      nationality: t("nationality"),
      dateOfBirth: t("dateOfBirth"),
      idNumber: t("idNumber"),
      departmentId: t("department"),
      positionId: t("position"),
    };

    for (const [field, label] of Object.entries(requiredFieldsMap)) {
      const value = form[field];
      if (value === null || value === undefined || String(value).trim() === "") {
        toast.error(`${label} ${t("isRequired")}`);
        return;
      }
    }

    const payload = {
      name: form.name?.trim(),
      phone: form.phone,
      email: form.email,
      gender: form.gender,
      address: form.address,
      nationality: form.nationality,
      dateOfBirth: form.dateOfBirth,
      idNumber: form.idNumber,
      departmentId: form.departmentId,
      positionId: form.positionId ? Number(form.positionId) : null,
    };

    if (avatarUrl) {
      payload.avatar = avatarUrl;
    }

    if (isEditPage) {
      updateEmployee.mutate(
        { id, data: payload },
        {
          onSuccess: () => {
            setIsEditMode(false);
            setAvatarUrl(null);
          },
        }
      );
    } else {
      createEmployee.mutate(payload, {
        onSuccess: () => navigate("/employees"),
      });
    }
  };

  const handleDelete = () => {
    if (window.confirm(t("delete") + "?")) {
      deleteEmployee.mutate(id, { onSuccess: () => navigate("/employees") });
    }
  };

  const handleCancelEdit = () => {
    setIsEditMode(false);
    setAvatarUrl(null);
    if (employee && Object.keys(employee).length > 0) {
      setForm({
        name: employee.name || "",
        phone: employee.phone || "",
        email: employee.email || "",
        gender: employee.gender || "",
        address: employee.address || "",
        nationality: employee.nationality || "",
        dateOfBirth: employee.dateOfBirth || "",
        idNumber: employee.idNumber || "",
        departmentId: employee.department?.id || null,
        positionId: employee.position?.id || "",
      });
      setAvatarPreview(employee.avatar || null);
    }
  };

  const isPending = isEditPage ? updateEmployee.isPending : createEmployee.isPending;
  const title = isEditPage ? (employee.name || t("employeeDetail")) : t("addEmployee");

  if (isPending || (isEditPage && isLoadingEmployee)) {
    return (
      <div className="flex flex-col h-full">
        <ContentHeader
          title={title}
          actions={
            <>
              <Button variant="outline" onClick={() => navigate(-1)}>{t("cancel")}</Button>
              {(isEditMode || !isEditPage) && (
                <Button onClick={() => formRef.current?.requestSubmit()}>{t("save")}</Button>
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
        title={title}
        actions={
          <>
            <Button variant="outline" onClick={() => navigate(-1)}>{t("cancel")}</Button>
            {isEditPage && !isEditMode ? (
              <>
                <Button variant="outline" onClick={() => setIsEditMode(true)}>{t("edit")}</Button>
                <Button variant="outline" onClick={handleDelete} disabled={deleteEmployee.isPending}>{t("delete")}</Button>
              </>
            ) : (
              <>
                {isEditPage && (
                  <Button variant="outline" onClick={handleCancelEdit}>{t("cancel")}</Button>
                )}
                <Button onClick={() => formRef.current?.requestSubmit()}>{t("save")}</Button>
              </>
            )}
          </>
        }
      />
      <div className="flex-1 mt-4">
        <div className="bg-white rounded-xl shadow p-6">
          <form ref={formRef} onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-3 gap-6">
              <div className="col-span-2 space-y-4">
                <TextInput 
                  label={t("employeeName")} 
                  value={form.name} 
                  onChange={onChange("name")} 
                  required 
                  disabled={isEditPage && !isEditMode}
                />
                <TextInput 
                  label={t("email")} 
                  type="email" 
                  value={form.email} 
                  onChange={onChange("email")} 
                  required 
                  disabled={isEditPage && !isEditMode}
                />
                <TextInput 
                  label={t("phone")} 
                  value={form.phone} 
                  onChange={onChange("phone")} 
                  required 
                  disabled={isEditPage && !isEditMode}
                />
                <div className="grid grid-cols-2 gap-4">
                  <SelectDropdown
                    label={t("gender")}
                    options={[
                      { id: "Male", name: t("male") },
                      { id: "Female", name: t("female") },
                      { id: "Other", name: t("other") },
                    ]}
                    value={form.gender}
                    onChange={(v) => setForm((s) => ({ ...s, gender: v }))}
                    placeholder={t("gender")}
                    required
                    disabled={isEditPage && !isEditMode}
                  />
                  <TextInput 
                    label={t("dateOfBirth", { defaultValue: "Date of Birth" })} 
                    type="date" 
                    value={form.dateOfBirth} 
                    onChange={onChange("dateOfBirth")} 
                    required 
                    disabled={isEditPage && !isEditMode}
                  />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <TextInput 
                    label={t("idNumber", { defaultValue: "ID Number" })} 
                    value={form.idNumber} 
                    onChange={onChange("idNumber")} 
                    required 
                    disabled={isEditPage && !isEditMode}
                  />
                  <TextInput 
                    label={t("nationality", { defaultValue: "Nationality" })} 
                    value={form.nationality} 
                    onChange={onChange("nationality")} 
                    required 
                    disabled={isEditPage && !isEditMode}
                  />
                </div>
                <TextInput 
                  label={t("address")} 
                  value={form.address} 
                  onChange={onChange("address")} 
                  required 
                  disabled={isEditPage && !isEditMode}
                />
                <div className="grid grid-cols-2 gap-4">
                  <SelectDropdown
                    label={t("department", { defaultValue: "Department" })}
                    options={(departmentsData || []).map((d) => ({ id: d.id, name: d.name }))}
                    value={form.departmentId}
                    onChange={(v) => setForm((s) => ({ ...s, departmentId: v }))}
                    placeholder={t("chooseDepartment", { defaultValue: "Choose department" })}
                    required
                    disabled={isEditPage && !isEditMode}
                  />
                  <SelectDropdown
                    label={t("position", { defaultValue: "Position" })}
                    options={(positionsData || []).map((p) => ({ id: p.id, name: `${p.name}${p.level ? ` (${p.level})` : ""}` }))}
                    value={form.positionId}
                    onChange={(v) => setForm((s) => ({ ...s, positionId: v }))}
                    placeholder={t("choosePosition", { defaultValue: "Choose position" })}
                    required
                    disabled={isEditPage && !isEditMode}
                  />
                </div>
              </div>
              <div className="col-span-1 flex flex-col relative">
                {uploading && <LoadingOverlay show={true} />}
                <FileUploader
                  label={t("avatar")}
                  onFileChange={handleAvatarChange}
                  preview={avatarPreview}
                  setPreview={setAvatarPreview}
                  disabled={(isEditPage && !isEditMode) || isPending || uploading}
                />
              </div>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
