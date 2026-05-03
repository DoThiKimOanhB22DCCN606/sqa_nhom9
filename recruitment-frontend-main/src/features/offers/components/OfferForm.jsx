import { useEffect } from "react";
import { useForm, Controller } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useCandidates } from "../../candidate/hooks/useCandidates";
import { useCandidate } from "../../candidate/hooks/useCandidates";
import { useWorkflows } from "../../../hooks/useWorkflows";
import SelectDropdown from "../../../components/ui/SelectDropdown";
import TextInput from "../../../components/ui/TextInput";
import TextArea from "../../../components/ui/TextArea";
import Button from "../../../components/ui/Button";
import LoadingSpinner from "../../../components/ui/LoadingSpinner";

export default function OfferForm({ initialData, onSubmit, isLoading = false, mode = "create" }) {
  const { t } = useTranslation();
  const isViewMode = mode === "view";
  const isEditMode = mode === "edit";
  const isCreateMode = mode === "create";

  const {
    control,
    handleSubmit,
    watch,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm({
    defaultValues: initialData || {
      candidateId: null,
      positionId: null,
      departmentName: "",
      positionLevel: "",
      workflowId: null,
      isOverBudget: false,
      salary: "",
      probationPeriod: "2 tháng",
      contractType: "DEFINITE",
      probationStartDate: "",
      notes: "",
    },
  });

  const selectedCandidateId = watch("candidateId");

  const { data: candidatesData, isLoading: isLoadingCandidates } = useCandidates({
    status: "INTERVIEW",
  });
  const candidates = candidatesData?.data?.result || [];

  const { data: selectedCandidateData, isLoading: isLoadingSelectedCandidate } =
    useCandidate(selectedCandidateId, { enabled: !!selectedCandidateId && isCreateMode });

  useEffect(() => {
    if (selectedCandidateData) {
      const candidate = selectedCandidateData.data;
      const position = candidate.jobPosition;
      if (position) {
        setValue("positionId", position.id);
        setValue("positionName", position.title);
        setValue("departmentName", position.department?.name || "");
        setValue("positionLevel", position.level || "");
        setValue("salary", position.salary || "");
      }
    }
  }, [selectedCandidateData, setValue]);

  const contractTypes = [
    { id: "DEFINITE", name: "Hợp đồng xác định thời hạn" },
    { id: "INDEFINITE", name: "Hợp đồng không xác định thời hạn" },
  ];

  const { data: workflowsData } = useWorkflows({ type: "OFFER" });
  const workflows = Array.isArray(workflowsData?.data?.result)
    ? workflowsData.data.result
    : Array.isArray(workflowsData?.result)
    ? workflowsData.result
    : [];

  const budgetOptions = [
    { id: false, name: "Trong ngân sách" },
    { id: true, name: "Vượt ngân sách" },
  ];

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Controller
          name="candidateId"
          control={control}
          rules={{ required: t("validation.required", { field: "Ứng viên" }) }}
          render={({ field }) => (
            <SelectDropdown
              {...field}
              label="Ứng viên"
              options={candidates.map((c) => ({ id: c.id, name: c.fullName }))}
              placeholder={t("common.selectCandidate")}
              error={errors.candidateId?.message}
              disabled={isViewMode || isLoadingCandidates || (isEditMode && !!initialData) || isLoading}
            />
          )}
        />

        <Controller
          name="workflowId"
          control={control}
          rules={{ required: t("validation.required", { field: "Luồng duyệt" }) }}
          render={({ field }) => (
            <SelectDropdown
              {...field}
              label="Luồng duyệt"
              options={workflows.map((w) => ({ id: w.id, name: w.name }))}
              placeholder={t("common.selectWorkflow")}
              error={errors.workflowId?.message}
              disabled={isViewMode || isLoading}
            />
          )}
        />

        {isLoadingSelectedCandidate ? (
          <div className="flex items-center justify-center h-10">
            <LoadingSpinner />
          </div>
        ) : (
          <>
            <TextInput
              label="Vị trí"
              value={watch("positionName") || ""}
              disabled
            />
            <TextInput
              label="Phòng ban"
              value={watch("departmentName") || ""}
              disabled
            />
            <TextInput
              label="Cấp bậc"
              value={watch("positionLevel") || ""}
              disabled
            />
          </>
        )}

        <Controller
          name="salary"
          control={control}
          rules={{ required: t("validation.required", { field: "Mức lương" }) }}
          render={({ field }) => (
            <TextInput
              {...field}
              label="Mức lương chính thức"
              type="number"
              placeholder="Nhập mức lương"
              error={errors.salary?.message}
              disabled={isViewMode || isLoading}
            />
          )}
        />

        <Controller
          name="isOverBudget"
          control={control}
          render={({ field }) => (
            <SelectDropdown
              {...field}
              label="Tình trạng ngân sách"
              options={budgetOptions}
              error={errors.isOverBudget?.message}
              disabled={isViewMode || isLoading}
            />
          )}
        />

        <Controller
          name="probationPeriod"
          control={control}
          render={({ field }) => (
            <TextInput
              {...field}
              label="Thời gian thử việc"
              placeholder="VD: 2 tháng"
              error={errors.probationPeriod?.message}
              disabled={isViewMode || isLoading}
            />
          )}
        />

        <Controller
          name="contractType"
          control={control}
          render={({ field }) => (
            <SelectDropdown
              {...field}
              label="Loại hợp đồng"
              options={contractTypes}
              error={errors.contractType?.message}
              disabled={isViewMode || isLoading}
            />
          )}
        />

        <Controller
          name="probationStartDate"
          control={control}
          rules={{ required: t("validation.required", { field: "Ngày bắt đầu" }) }}
          render={({ field }) => (
            <TextInput
              {...field}
              label="Ngày bắt đầu thử việc"
              type="date"
              error={errors.probationStartDate?.message}
              disabled={isViewMode || isLoading}
            />
          )}
        />
      </div>

      <Controller
        name="notes"
        control={control}
        render={({ field }) => (
          <TextArea
            {...field}
            label="Ghi chú"
            rows={4}
            placeholder="Thêm ghi chú về offer..."
            disabled={isViewMode || isLoading}
          />
        )}
      />

      {!isViewMode && (
        <div className="flex justify-end gap-4">
          <Button type="button" variant="secondary" onClick={() => window.history.back()} disabled={isLoading}>
            {t("actions.cancel")}
          </Button>
          <Button type="submit" disabled={isSubmitting || isLoading}>
            {isSubmitting || isLoading ? (
              <LoadingSpinner />
            ) : isCreateMode ? (
              t("actions.create")
            ) : (
              t("actions.update")
            )}
          </Button>
        </div>
      )}
    </form>
  );
}
