import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useCandidates } from "../candidate/hooks/useCandidates";
import { useCandidate } from "../candidate/hooks/useCandidates";
import { useWorkflows } from "../../hooks/useWorkflows";
import ContentHeader from "../../components/ui/ContentHeader";
import SelectDropdown from "../../components/ui/SelectDropdown";
import TextInput from "../../components/ui/TextInput";
import TextArea from "../../components/ui/TextArea";
import Button from "../../components/ui/Button";
import LoadingSpinner from "../../components/ui/LoadingSpinner";
import LoadingContent from "../../components/ui/LoadingContent";
import { formatNumber, parseFormattedNumber } from "../../utils/utils";

export default function OfferForm({ initialData, onSubmit, isLoading = false, mode = "create" }) {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const isViewMode = mode === "view";
    const isEditMode = mode === "edit";
    const isCreateMode = mode === "create";
    const [form, setForm] = useState({
        candidateId: initialData?.candidateId ?? null,
        positionId: initialData?.positionId ?? null,
        positionName: initialData?.positionName ?? "",
        departmentName: initialData?.departmentName ?? "",
        positionLevel: initialData?.positionLevel ?? "",
        workflowId: initialData?.workflowId ?? null,
        isOverBudget: initialData?.isOverBudget ?? false,
        salary: initialData?.salary ?? "",
        probationPeriod: initialData?.probationPeriod ?? "",
        contractType: initialData?.contractType ?? "DEFINITE",
        probationStartDate: initialData?.probationStartDate ?? "",
        notes: initialData?.notes ?? "",
    });

    const setValue = (key, value) => setForm((prev) => ({ ...prev, [key]: value }));
    const watch = (key) => form[key];

    const selectedCandidateId = watch("candidateId");

    const { data: candidatesData, isLoading: isLoadingCandidates } = useCandidates({ status: "INTERVIEW" });
    const candidates = Array.isArray(candidatesData?.data?.result) ? candidatesData.data.result : [];

    const { data: selectedCandidateData, isLoading: isLoadingSelectedCandidate } = useCandidate(selectedCandidateId, {
        enabled: !!selectedCandidateId && isCreateMode,
    });

    useEffect(() => {
        if (selectedCandidateData?.data?.jobPosition) {
            const position = selectedCandidateData.data.jobPosition;
            setValue("positionId", position.id || null);
            setValue("positionName", position.title || "");
            setValue("departmentName", position.department?.name || "");
            setValue("positionLevel", position.level || "");
            if (!initialData?.salary && position.salary) setValue("salary", position.salary);
        }
    }, [selectedCandidateData, setValue, initialData]);

    const { data: workflowsData } = useWorkflows({ type: "OFFER" });
    const workflows = Array.isArray(workflowsData?.data?.result)
        ? workflowsData.data.result
        : Array.isArray(workflowsData?.result)
        ? workflowsData.result
        : [];

    const contractTypes = [
        { id: "DEFINITE", name: "Hợp đồng xác định thời hạn" },
        { id: "INDEFINITE", name: "Hợp đồng không xác định thời hạn" },
    ];

    const budgetOptions = [
        { id: false, name: "Trong ngân sách" },
        { id: true, name: "Vượt ngân sách" },
    ];

    const headerTitle = isEditMode
        ? "Chỉnh sửa offer"
        : isCreateMode
        ? "Tạo offer"
        : "Xem offer";

    const handleSubmit = () => {
        if (isLoading || isViewMode) return;
        const payload = {
            ...form,
            salary: form.salary ? Number(form.salary) : null,
        };
        onSubmit?.(payload);
    };

    const isPending = isLoading || isLoadingCandidates || (isEditMode && isLoadingSelectedCandidate);

    if (isEditMode && isLoadingSelectedCandidate) {
        return (
            <div className="flex flex-col h-full">
                <ContentHeader
                    title={headerTitle}
                    actions={
                        !isViewMode && (
                            <>
                                <Button onClick={handleSubmit} disabled={true}>
                                    {t("loading")}
                                </Button>
                                <Button variant="outline" onClick={() => navigate(-1)} disabled={true}>
                                    Hủy
                                </Button>
                            </>
                        )
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
                title={headerTitle}
                actions={
                    !isViewMode && (
                        <>
                            <Button onClick={handleSubmit} disabled={isLoading}>
                                {isLoading ? (isEditMode ? "Đang cập nhật..." : "Đang lưu...") : isEditMode ? "Cập nhật" : "Lưu offer"}
                            </Button>
                            <Button variant="outline" onClick={() => navigate(-1)} disabled={isLoading}>
                                Hủy
                            </Button>
                        </>
                    )
                }
            />

            <div className="flex-1 flex flex-col mt-4 overflow-y-auto p-6 gap-4 bg-white rounded-xl shadow">
                <div className="flex flex-col gap-4">
                    <div className="rounded-md border border-gray-300 p-4 gap-4 flex flex-col">
                        <h2>Thông tin cơ bản</h2>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <SelectDropdown
                                label="Ứng viên"
                                value={form.candidateId}
                                onChange={(val) => setValue("candidateId", val)}
                                options={candidates.map((c) => ({ id: c.id, name: c.fullName }))}
                                placeholder={t("common.selectCandidate")}
                                disabled={isViewMode || isLoadingCandidates || (isEditMode && !!initialData) || isLoading}
                                required
                            />

                            <SelectDropdown
                                label="Luồng duyệt"
                                value={form.workflowId}
                                onChange={(val) => setValue("workflowId", val)}
                                options={workflows.map((w) => ({ id: w.id, name: w.name }))}
                                placeholder={t("common.selectWorkflow")}
                                disabled={isViewMode || isLoading}
                                required
                            />

                            {isLoadingSelectedCandidate ? (
                                <div className="flex items-center justify-center h-10">
                                    <LoadingSpinner />
                                </div>
                            ) : (
                                <>
                                    <TextInput label="Vị trí" value={watch("positionName") || ""} disabled />
                                    <TextInput label="Phòng ban" value={watch("departmentName") || ""} disabled />
                                    <TextInput label="Cấp bậc" value={watch("positionLevel") || ""} disabled />
                                </>
                            )}
                        </div>
                    </div>

                    <div className="rounded-md border border-gray-300 p-4 gap-4 flex flex-col">
                        <h2>Thông tin lương thưởng</h2>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <TextInput
                                label="Mức lương chính thức (VNĐ)"
                                placeholder="0"
                                type="text"
                                value={formatNumber(form.salary)}
                                onChange={(e) => {
                                    const parsed = parseFormattedNumber(e.target.value);
                                    if (parsed !== null || e.target.value === "") {
                                        setValue("salary", parsed);
                                    }
                                }}
                                required
                                disabled={isViewMode || isLoading}
                            />
                            <SelectDropdown
                                label="Tình trạng ngân sách"
                                value={form.isOverBudget}
                                onChange={(val) => setValue("isOverBudget", val)}
                                options={budgetOptions}
                                disabled={isViewMode || isLoading}
                                required
                            />
                        </div>
                    </div>

                    <div className="rounded-md border border-gray-300 p-4 gap-4 flex flex-col">
                        <h2>Thông tin hợp đồng</h2>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <TextInput
                                label="Thời gian thử việc"
                                value={form.probationPeriod}
                                onChange={(e) => setValue("probationPeriod", e.target.value)}
                                placeholder="VD: 2 tháng"
                                disabled={isViewMode || isLoading}
                            />

                            <SelectDropdown
                                label="Loại hợp đồng"
                                value={form.contractType}
                                onChange={(val) => setValue("contractType", val)}
                                options={contractTypes}
                                disabled={isViewMode || isLoading}
                                required
                            />

                            <TextInput
                                label="Ngày bắt đầu thử việc"
                                type="date"
                                value={form.probationStartDate}
                                onChange={(e) => setValue("probationStartDate", e.target.value)}
                                disabled={isViewMode || isLoading}
                                required
                            />
                        </div>
                    </div>

                    <div className="rounded-md border border-gray-300 p-4 gap-4 flex flex-col">
                        <h2>Ghi chú</h2>
                        <TextArea
                            value={form.notes}
                            onChange={(e) => setValue("notes", e.target.value)}
                            rows={4}
                            placeholder="Thêm ghi chú về offer"
                            disabled={isViewMode || isLoading}
                        />
                    </div>
                </div>

            </div>
        </div>
    );
}