import { ChevronRight, X, AlertTriangle } from "lucide-react";
import Button from "../../components/ui/Button";
import ContentHeader from "../../components/ui/ContentHeader";
import TextInput from "../../components/ui/TextInput";
import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import SelectDropdown from "../../components/ui/SelectDropdown";
import { useAuth } from "../../context/AuthContext";
import { useTranslation } from "react-i18next";
import { toast } from "react-toastify";
import { formatNumber, parseFormattedNumber } from "../../utils/utils";
import Modal from "../../components/ui/Modal";
import {
  useCreateRecruitmentRequest,
  useUpdateRecruitmentRequest,
  useRecruitmentRequest,
  useApproveRecruitmentRequest,
  useRejectRecruitmentRequest,
  useSubmitRecruitmentRequest,
  useReturnRecruitmentRequest,
  useCancelRecruitmentRequest,
  useWithdrawRecruitmentRequest,
} from "./hooks/useRecruitmentRequests";
import LoadingContent from "../../components/ui/LoadingContent";
import { useWorkflows } from "../../hooks/useWorkflows";
import { useAllDepartments } from "../../hooks/useDepartments";

export default function RecruitmentRequestAdd() {
  const { id } = useParams();
  const { user } = useAuth();
  const { t } = useTranslation();
  const navigate = useNavigate();
  const isViewMode = !!id;
  const [isEditing, setIsEditing] = useState(false);
  const [showRejectDialog, setShowRejectDialog] = useState(false);
  const [rejectionReason, setRejectionReason] = useState("");
  const [showSubmitDialog, setShowSubmitDialog] = useState(false);
  const [showReturnDialog, setShowReturnDialog] = useState(false);
  const [returnReason, setReturnReason] = useState("");
  const [showCancelDialog, setShowCancelDialog] = useState(false);
  const [cancelReason, setCancelReason] = useState("");
  const [showWithdrawDialog, setShowWithdrawDialog] = useState(false);

  const [formData, setFormData] = useState({
    title: "",
    quantity: 1,
    reason: "",
    requesterId: user?.userId || null,
    departmentId: user?.department?.id || null,
    workflowId: null,
  });

  const {
    data: existingRequest,
    isLoading: requestLoading,
    isError: requestError,
  } = useRecruitmentRequest(id);

  useEffect(() => {
    if (requestError) {
      toast.error(t("errorLoadRequest"));
    }
  }, [requestError, t]);

  useEffect(() => {
    if (existingRequest && isViewMode) {
      setFormData({
        title: existingRequest.title || "",
        quantity: existingRequest.quantity || 1,
        reason: existingRequest.reason || "",
        requesterId: existingRequest.requesterId || user?.id || null,
        departmentId: existingRequest.department?.id || null,
        workflowId: existingRequest.workflowId || null,
      });
    }
  }, [existingRequest, isViewMode, user]);

  const { data: departmentsData } = useAllDepartments();
  const departments = departmentsData || [];

  const selectedDepartmentId = formData.departmentId || user?.department?.id;

  const { data: workflowsData } = useWorkflows(
    {  type: "REQUEST", isActive: true },
    { enabled: !!selectedDepartmentId }
  );

  const workflows = workflowsData?.data?.result || [];

  const createMutation = useCreateRecruitmentRequest();
  const updateMutation = useUpdateRecruitmentRequest();
  const approveMutation = useApproveRecruitmentRequest();
  const rejectMutation = useRejectRecruitmentRequest();
  const submitMutation = useSubmitRecruitmentRequest();
  const returnMutation = useReturnRecruitmentRequest();
  const cancelMutation = useCancelRecruitmentRequest();
  const withdrawMutation = useWithdrawRecruitmentRequest();

  // Removed workplace locations: temporarily not needed

  const isGeneralInfoField = (fieldName) => {
    const generalFields = [
      "title", "requesterId", "departmentId", "workflowId" 
    ];
    return generalFields.includes(fieldName);
  };

  const handleChange = (e) => {
    const { name, value, files } = e.target;

    // Prevent editing general info fields during edit mode
    if (isEditing && isGeneralInfoField(name)) {
      toast.warning(t("recruitmentRequests.generalInfoEditRestricted"));
      return;
    }

    setFormData((prev) => ({
      ...prev,
      [name]: files ? files[0] : value,
    }));
  };

  // Removed workplace selection handler: temporarily not needed

  const onSubmit = () => {
    if (!formData.title) {
      toast.error(t("errorPositionRequired"));
      return;
    }
    if (!formData.departmentId) {
      toast.error(t("errorDepartmentRequired"));
      return;
    }
    if (!formData.quantity || formData.quantity < 1) {
      toast.error(t("errorPositionCountInvalid"));
      return;
    }
    if (!formData.reason) {
      toast.error(t("errorReasonRequired"));
      return;
    }

    const payload = {
      title: formData.title,
      quantity: formData.quantity,
      reason: formData.reason,
      requesterId: formData.requesterId,
      departmentId: formData.departmentId,
      workflowId: formData.workflowId,
    };

    if (isViewMode && isEditing) {
      updateMutation.mutate(
        { id, data: payload },
        {
          onSuccess: () => {
            setIsEditing(false);
            toast.success(t("updateSuccess"));
          },
          onError: (error) => {
            console.error("Error updating request:", error);
            const errorMessage =
              error.response?.data?.message || t("errorUpdateRequest");
            toast.error(errorMessage);
          },
        }
      );
    } else if (!isViewMode) {
      createMutation.mutate(payload, {
        onSuccess: () => {
          toast.success(t("toasts.createSuccess"));
          navigate(-1);
        },
        onError: (error) => {
          const errorMessage =
            error.response?.data?.message || t("errorCreateRequest");
          toast.error(errorMessage);
        },
      });
    }
  };

  const handleApprove = () => {
    if (!id) return;

    const approvalData = {
      approvalNotes: "_",
    };

    approveMutation.mutate(
      { id, data: approvalData },
      {
        onSuccess: () => {
          toast.success(t("approveSuccess"));
          // Refetch để cập nhật UI
        },
      }
    );
  };

  const handleReject = () => {
    if (!id) return;
    setShowRejectDialog(true);
  };

  const confirmReject = () => {
    if (rejectMutation.isPending) return;

    if (!rejectionReason || !rejectionReason.trim()) {
      toast.error(t("toasts.pleaseEnterRejectionReason"));
      return;
    }

    const rejectData = {
      reason: rejectionReason.trim(),
    };

    rejectMutation.mutate(
      { id, data: rejectData },
      {
        onSuccess: () => {
          setShowRejectDialog(false);
          setRejectionReason("");
        },
      }
    );
  };

  const handleSubmit = () => {
    if (!id) return;
    setShowSubmitDialog(true);
  };

  const confirmSubmit = () => {
    submitMutation.mutate(
      { id, data: {} },
      {
        onSuccess: () => {
          setShowSubmitDialog(false);
        },
      }
    );
  };

  const handleReturn = () => {
    if (!id) return;
    setShowReturnDialog(true);
  };

  const confirmReturn = () => {
    if (returnMutation.isPending) return;
    if (!returnReason || !returnReason.trim()) {
      toast.error(t("toasts.pleaseEnterReturnReason"));
      return;
    }
    returnMutation.mutate(
      { id, data: { reason: returnReason.trim() } },
      {
        onSuccess: () => {
          setShowReturnDialog(false);
          setReturnReason("");
        },
      }
    );
  };

  const handleCancel = () => {
    if (!id) return;
    setShowCancelDialog(true);
  };

  const confirmCancel = () => {
    if (cancelMutation.isPending) return;
    if (!cancelReason || !cancelReason.trim()) {
      toast.error(t("toasts.pleaseEnterCancellationReason"));
      return;
    }
    cancelMutation.mutate(
      { id, data: { reason: cancelReason.trim() } },
      {
        onSuccess: () => {
          setShowCancelDialog(false);
          setCancelReason("");
        },
      }
    );
  };

  const handleWithdraw = () => {
    if (!id) return;
    setShowWithdrawDialog(true);
  };

  const confirmWithdraw = () => {
    withdrawMutation.mutate(
      { id, data: {} },
      {
        onSuccess: () => {
          setShowWithdrawDialog(false);
        },
      }
    );
  };

  const isPending =
    createMutation.isPending ||
    updateMutation.isPending ||
    approveMutation.isPending ||
    rejectMutation.isPending ||
    submitMutation.isPending ||
    returnMutation.isPending ||
    cancelMutation.isPending ||
    withdrawMutation.isPending;

  const isCEO = user?.role?.name === "CEO";
  const isManager = user?.role?.name === "MANAGER";
  const isDraft = existingRequest?.status === "DRAFT";
  const isPendingStatus = existingRequest?.status === "PENDING";
  const isApproved = existingRequest?.status === "APPROVED";
  const isRejected = existingRequest?.status === "REJECTED";
  const isReturned = existingRequest?.status === "RETURNED";
  
  const isRequester = existingRequest?.requesterId === user?.userId;
  
  // Check if current approval tracking action user is the logged-in user
  const currentApprovalTracking = existingRequest?.workflowInfo?.approvalTrackings?.find(
    (t) => t.stepId === existingRequest?.workflowInfo?.currentStepId
  );
  const isCurrentActionUser = currentApprovalTracking?.actionUserId === user?.userId;
  
  const canSubmit = isViewMode && isRequester && (isDraft || isReturned);
  const canWithdraw = isViewMode && isRequester && isPendingStatus;
  const canCancelRequest = isViewMode && isRequester && (isDraft || isPendingStatus || isReturned);
  
  const canApprove = isViewMode && isPendingStatus && isCurrentActionUser;
  const canRejectRequest = isViewMode && isPendingStatus && isCurrentActionUser;
  const canReturn = isViewMode && isPendingStatus && isCurrentActionUser;
  
  const canEdit = isViewMode && isRequester && (isDraft || isReturned) && !isEditing;

  const getCurrentStatus = () => {
    if (!isViewMode) return "DRAFT";
    return existingRequest?.status || "DRAFT";
  };

  const currentStatus = getCurrentStatus();

  const progressSteps = [
    { key: "DRAFT", label: t("statuses.draft") },
    { key: "PENDING", label: t("statuses.pending") },
    { key: "APPROVED", label: t("statuses.approved") },
    { key: "REJECTED", label: t("statuses.rejected") },
    { key: "RETURNED", label: t("statuses.returned") },
    { key: "CANCELLED", label: t("statuses.cancelled") },
  ];

  const getStepStyle = (stepKey) => {
    if (currentStatus === stepKey) {
      return "text-[#DC2626] text-sm border-b-2 border-[#DC2626] pb-1 font-semibold";
    }
    return "text-[#A3A3A3] text-sm";
  };

  if (requestLoading && isViewMode) {
    return (
      <div className="flex flex-col h-full">
        <ContentHeader
          title={isViewMode ? t("requestDetail") : t("addRecruitmentRequest")}
          actions={
            <Button
              variant="outline"
              onClick={() => navigate(-1)}
              disabled={true}
            >
              {t("close")}
            </Button>
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
        title={isViewMode ? t("requestDetail") : t("addRecruitmentRequest")}
        actions={
          <>
            {isViewMode && isEditing ? (
              <>
                {/* Edit Mode: Only Update and Cancel */}
                <Button onClick={() => onSubmit()} disabled={isPending}>
                  {isPending ? t("updating") : t("update")}
                </Button>
                <Button
                  variant="outline"
                  onClick={() => setIsEditing(false)}
                  disabled={isPending}
                >
                  {t("cancel")}
                </Button>
              </>
            ) : (
              <>
                {/* Requester Actions */}
                {canSubmit && (
                  <Button onClick={handleSubmit} disabled={isPending}>
                    {submitMutation.isPending ? t("buttons.submitting") : t("recruitmentRequests.submitRequest")}
                  </Button>
                )}
                
                {canWithdraw && (
                  <Button 
                    variant="outline" 
                    onClick={handleWithdraw} 
                    disabled={isPending}
                    className="border-orange-600 text-orange-600 hover:bg-orange-50"
                  >
                    {withdrawMutation.isPending ? t("buttons.withdrawing") : t("buttons.withdraw")}
                  </Button>
                )}

                {/* Approver Actions */}
                {canApprove && (
                  <Button onClick={handleApprove} disabled={isPending}>
                    {approveMutation.isPending ? t("approving") : t("approve")}
                  </Button>
                )}
                
                {canRejectRequest && (
                  <Button
                    variant="outline"
                    onClick={handleReject}
                    disabled={isPending}
                    className="border-red-600 text-red-600 hover:bg-red-50"
                  >
                    {rejectMutation.isPending ? t("rejecting") : t("reject")}
                  </Button>
                )}
                
                {canReturn && (
                  <Button
                    variant="outline"
                    onClick={handleReturn}
                    disabled={isPending}
                    className="border-yellow-600 text-yellow-600 hover:bg-yellow-50"
                  >
                    {returnMutation.isPending ? t("buttons.returning") : t("buttons.return")}
                  </Button>
                )}

                {/* Edit Action */}
                {canEdit && (
                  <Button onClick={() => setIsEditing(true)}>{t("edit")}</Button>
                )}

                {/* Create Mode */}
                {!isViewMode && (
                  <Button onClick={() => onSubmit()} disabled={isPending}>
                    {isPending ? t("saving") : t("saveRequest")}
                  </Button>
                )}

                {/* Close/Cancel Button */}
                <Button
                  variant="outline"
                  onClick={() => navigate(-1)}
                  disabled={isPending}
                >
                  {isViewMode ? t("close") : t("cancel")}
                </Button>
              </>
            )}
          </>
        }
      />
      <div
        className="flex-1 flex flex-col mt-4 overflow-y-auto
        p-6 gap-4 bg-white rounded-xl shadow"
      >
        {/* progress */}
        <div className="flex flex-wrap items-center gap-2 w-full md:w-auto">
          {progressSteps.map((step, index) => (
            <div key={step.key} className="flex items-center">
              <div className="flex items-center">
                <span className={getStepStyle(step.key)}>{step.label}</span>
              </div>
              {index < progressSteps.length - 1 && (
                <ChevronRight className="text-[#E0E0E0] ml-2" size={20} />
              )}
            </div>
          ))}
        </div>
        {/* Steps & Tracking Table - only in view mode and has workflow */}
        {isViewMode && existingRequest?.workflowInfo?.workflow && (
          <div className="mb-6 bg-white rounded-xl shadow overflow-hidden">
            <div className="p-6 border-b border-gray-200">
              <h3 className="text-lg font-semibold text-gray-900">{t("recruitmentRequests.approvalStepsTitle")}</h3>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-red-50">
                  <tr>
                    <th className="p-4 text-left text-sm font-medium text-gray-600 whitespace-nowrap">{t("recruitmentRequests.stepOrder")}</th>
                    <th className="p-4 text-left text-sm font-medium text-gray-600 whitespace-nowrap">{t("recruitmentRequests.stepName")}</th>
                    <th className="p-4 text-left text-sm font-medium text-gray-600 whitespace-nowrap">{t("recruitmentRequests.processingStatus")}</th>
                    <th className="p-4 text-left text-sm font-medium text-gray-600 whitespace-nowrap">{t("recruitmentRequests.assignedPerson")}</th>
                    <th className="p-4 text-left text-sm font-medium text-gray-600 whitespace-nowrap">{t("recruitmentRequests.time")}</th>
                    <th className="p-4 text-left text-sm font-medium text-gray-600 whitespace-nowrap">{t("notes")}</th>
                  </tr>
                </thead>
                <tbody>
                  {existingRequest.workflowInfo.workflow.steps.map((step, idx) => {
                    // Tìm approvalTrackings cho step này
                    const approvalTrackings = Array.isArray(existingRequest.workflowInfo.approvalTrackings)
                      ? existingRequest.workflowInfo.approvalTrackings.filter(tr => tr.stepId === step.id)
                      : [];
                    
                    let trackingStatus = t("recruitmentRequests.notProcessed");
                    let actionUserName = "-";
                    let actionTime = "-";
                    let notes = "-";
                    
                    // Kiểm tra xem step này có phải là step hiện tại không
                    const isCurrentStep = existingRequest.workflowInfo.currentStepId === step.id;
                    
                    if (approvalTrackings.length > 0) {
                      // Lấy tracking gần nhất (có thể sắp xếp theo actionAt hoặc lấy phần tử cuối)
                      const latestTracking = approvalTrackings[approvalTrackings.length - 1];
                      const status = latestTracking.status;
                      
                      if (status === "APPROVED") trackingStatus = t("statuses.approved");
                      else if (status === "REJECTED") trackingStatus = t("statuses.rejected");
                      else if (status === "RETURNED") trackingStatus = t("statuses.returned");
                      else if (status === "CANCELLED") trackingStatus = t("statuses.cancelled");
                      else if (status === "PENDING") trackingStatus = t("statuses.pending");
                      else trackingStatus = status;
                      
                      // Lấy tên người thao tác
                      actionUserName = latestTracking.actionUserName || "-";
                      
                      // Lấy thời gian thao tác
                      if (latestTracking.actionAt) {
                        actionTime = new Date(latestTracking.actionAt).toLocaleString("vi-VN");
                      }
                      
                      // Lấy ghi chú
                      notes = latestTracking.notes || "-";
                    }
                    
                    return (
                      <tr 
                        key={step.id} 
                        className={`border-b last:border-b-0 border-gray-200 ${isCurrentStep ? "bg-yellow-50" : "hover:bg-gray-50"}`}
                      >
                        <td className="p-4 text-sm whitespace-nowrap">{step.stepOrder}</td>
                        <td className={`p-4 text-sm whitespace-nowrap ${isCurrentStep ? "font-semibold" : ""}`}>{step.approverPositionName}</td>
                        <td className="p-4 text-sm whitespace-nowrap">{trackingStatus}</td>
                        <td className="p-4 text-sm whitespace-nowrap max-w-[150px] truncate">{actionUserName}</td>
                        <td className="p-4 text-sm whitespace-nowrap">{actionTime}</td>
                        <td className="p-4 text-sm max-w-[200px] truncate" title={notes}>{notes}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        )}
        {/* form */}
        <div className="flex flex-col gap-4">
          {/* general info */}
          <div className="rounded-md border border-gray-300 p-4 gap-4 flex flex-col">
            <h2>{t("generalInfo")}</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-2 gap-4">
              <TextInput
                label={t("requester")}
                placeholder={user.name || t("requesterPlaceholder")}
                readOnly={true}
              />

              {user?.department?.id ? (
                <TextInput
                  label={t("department")}
                  placeholder={user?.department?.name || t("department")}
                  readOnly={true}
                  value={user?.department?.name || ""}
                />
              ) : (
                <SelectDropdown
                  label={t("department")}
                  placeholder={t("recruitmentRequests.selectDepartment")}
                  options={departments.map(d => ({ id: d.id, name: d.name }))}
                  value={formData.departmentId}
                  onChange={(value) => {
                    setFormData((prev) => ({ 
                      ...prev, 
                      departmentId: value,
                      workflowId: null
                    }));
                  }}
                  required={true}
                  disabled={isPending || (isViewMode ||isEditing)}
                />
              )}

              <TextInput
                label={t("positionLabel")}
                placeholder={t("positionPlaceholder")}
                type="text"
                name="title"
                value={formData.title}
                onChange={handleChange}
                required={true}
                disabled={isPending || (isViewMode ||isEditing)}
              />

              {/* Priority removed temporarily */}

              <SelectDropdown
                label={t("recruitmentRequests.workflow")}
                placeholder={selectedDepartmentId ? t("recruitmentRequests.selectWorkflow") : t("recruitmentRequests.selectDepartmentFirst")}
                options={workflows.map(w => ({ id: w.id, name: w.name }))}
                value={formData.workflowId}
                onChange={(value) => {
                  setFormData((prev) => ({ ...prev, workflowId: value }));
                }}
                required={true}
                disabled={!selectedDepartmentId || isPending || (isViewMode ||isEditing)}
              />
            </div>

            {/* Workflow Steps Preview - only show when workflow is selected and in create mode */}
            {!isViewMode && formData.workflowId && (
              <div className="mt-4 bg-blue-50 border border-blue-200 rounded-lg p-4">
                <h3 className="text-sm font-semibold text-blue-900 mb-3">{t("recruitmentRequests.selectedWorkflowSteps")}</h3>
                <div className="space-y-2">
                  {(() => {
                    const selectedWorkflow = workflows.find(w => w.id === formData.workflowId);
                    if (!selectedWorkflow || !selectedWorkflow.steps || selectedWorkflow.steps.length === 0) {
                      return <p className="text-sm text-blue-700">{t("recruitmentRequests.noApprovalSteps")}</p>;
                    }
                    return selectedWorkflow.steps
                      .sort((a, b) => a.stepOrder - b.stepOrder)
                      .map((step, idx) => (
                        <div key={step.id} className="flex items-start gap-3 text-sm">
                          <span className="flex-shrink-0 w-6 h-6 bg-blue-600 text-white rounded-full flex items-center justify-center font-semibold text-xs">
                            {step.stepOrder}
                          </span>
                          <div className="flex-1">
                            <p className="text-blue-900 font-medium">{`${t("recruitmentRequests.step")} ${step.stepOrder}`}</p>
                            <p className="text-blue-700 text-xs mt-0.5">
                              {t("recruitmentRequests.approver")}: {step.approverPositionName || t("recruitmentRequests.notDetermined")}
                            </p>
                          </div>
                        </div>
                      ));
                  })()}
                </div>
              </div>
            )}
          </div>
          {/* recruitment details */}
          <div className="rounded-md border border-gray-300 p-4 gap-4 flex flex-col">
            <h2>{t("recruitmentInfo")}</h2>
            <div className="gap-4 flex flex-col">
              <TextInput
                label={t("numberOfPositionsLabel")}
                placeholder={t("numberOfPositionsPlaceholder")}
                type="text"
                name="quantity"
                value={formatNumber(formData.quantity)}
                onChange={(e) => {
                  const parsed = parseFormattedNumber(e.target.value);
                  if (parsed !== null || e.target.value === "") {
                    setFormData(prev => ({ ...prev, quantity: parsed || 1 }));
                  }
                }}
                required={true}
                disabled={isPending || (isViewMode && !isEditing)}
              />

              {/* Workplace removed temporarily */}

              <TextInput
                isRow={true}
                label={t("recruitmentReason")}
                placeholder={t("recruitmentReasonPlaceholder")}
                type="text"
                name="reason"
                value={formData.reason}
                onChange={handleChange}
                required={true}
                disabled={isPending || (isViewMode && !isEditing)}
              />
            </div>
          </div>
          {/* Job description section removed temporarily */}
        </div>
      </div>

      {/* Submit Dialog */}
      <Modal
        isOpen={showSubmitDialog}
        onClose={() => !submitMutation.isPending && setShowSubmitDialog(false)}
        size="md"
      >
        <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md relative">
          {!submitMutation.isPending && (
            <div
              onClick={() => setShowSubmitDialog(false)}
              className="absolute top-4 right-4 p-1.5 hover:bg-gray-100 rounded-lg transition-colors cursor-pointer z-10"
            >
              <X className="h-4 w-4 text-gray-500" />
            </div>
          )}
          <div className="p-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-2">{t("recruitmentRequests.submitDialogTitle")}</h3>
            <p className="text-sm text-gray-600 mb-4">{t("recruitmentRequests.submitDialogMessage")}</p>
            <div className="flex gap-3">
              <Button variant="outline" onClick={() => setShowSubmitDialog(false)} disabled={submitMutation.isPending} className="flex-1">
                {t("cancel")}
              </Button>
              <Button onClick={confirmSubmit} disabled={submitMutation.isPending} className="flex-1">
                {submitMutation.isPending ? t("buttons.processing") : t("common.confirm")}
              </Button>
            </div>
          </div>
        </div>
      </Modal>

      {/* Reject Dialog */}
      <Modal
        isOpen={showRejectDialog}
        onClose={() => {
          if (!rejectMutation.isPending) {
            setShowRejectDialog(false);
            setRejectionReason("");
          }
        }}
        size="md"
      >
        <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md relative">
          {!rejectMutation.isPending && (
            <div
              onClick={() => {
                setShowRejectDialog(false);
                setRejectionReason("");
              }}
              className="absolute top-4 right-4 p-1.5 hover:bg-gray-100 rounded-lg transition-colors cursor-pointer z-10"
            >
              <X className="h-4 w-4 text-gray-500" />
            </div>
          )}
          <div className="p-6 pr-12">
            <div className="flex gap-4 mb-4">
              <div className="flex-shrink-0 w-12 h-12 bg-red-50 text-red-600 rounded-xl flex items-center justify-center">
                <AlertTriangle className="h-6 w-6" />
              </div>
              <div className="flex-1">
                <h3 className="text-lg font-semibold text-gray-900 mb-2">{t("recruitmentRequests.rejectDialogTitle")}</h3>
                <p className="text-sm text-gray-600 leading-relaxed">{t("recruitmentRequests.rejectDialogMessage")}</p>
              </div>
            </div>
          </div>
          <div className="px-6 pb-6">
            <textarea
              placeholder={t("recruitmentRequests.rejectPlaceholder")}
              value={rejectionReason}
              onChange={(e) => setRejectionReason(e.target.value)}
              disabled={rejectMutation.isPending}
              rows={3}
              className="w-full border border-gray-300 p-2 rounded-md text-gray-700
                focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent
                disabled:opacity-50 disabled:cursor-not-allowed resize-y"
            />
          </div>
          <div className="px-6 pb-6 flex gap-3 border-t border-gray-100">
            <Button
              variant="outline"
              onClick={() => {
                setShowRejectDialog(false);
                setRejectionReason("");
              }}
              disabled={rejectMutation.isPending}
              className="flex-1"
            >
              {t("cancel")}
            </Button>
            <Button
              onClick={confirmReject}
              disabled={rejectMutation.isPending || !rejectionReason.trim()}
              className="flex-1"
            >
              {rejectMutation.isPending ? t("buttons.processing") : t("reject")}
            </Button>
          </div>
        </div>
      </Modal>

      {/* Return Dialog */}
      <Modal
        isOpen={showReturnDialog}
        onClose={() => {
          if (!returnMutation.isPending) {
            setShowReturnDialog(false);
            setReturnReason("");
          }
        }}
        size="md"
      >
        <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md relative">
          {!returnMutation.isPending && (
            <div
              onClick={() => {
                setShowReturnDialog(false);
                setReturnReason("");
              }}
              className="absolute top-4 right-4 p-1.5 hover:bg-gray-100 rounded-lg transition-colors cursor-pointer z-10"
            >
              <X className="h-4 w-4 text-gray-500" />
            </div>
          )}
          <div className="p-6 pr-12">
            <div className="flex gap-4 mb-4">
              <div className="flex-shrink-0 w-12 h-12 bg-yellow-50 text-yellow-600 rounded-xl flex items-center justify-center">
                <AlertTriangle className="h-6 w-6" />
              </div>
              <div className="flex-1">
                <h3 className="text-lg font-semibold text-gray-900 mb-2">{t("recruitmentRequests.returnDialogTitle")}</h3>
                <p className="text-sm text-gray-600 leading-relaxed">{t("recruitmentRequests.returnDialogMessage")}</p>
              </div>
            </div>
          </div>
          <div className="px-6 pb-6">
            <textarea
              placeholder={t("recruitmentRequests.returnPlaceholder")}
              value={returnReason}
              onChange={(e) => setReturnReason(e.target.value)}
              disabled={returnMutation.isPending}
              rows={3}
              className="w-full border border-gray-300 p-2 rounded-md text-gray-700
                focus:outline-none focus:ring-2 focus:ring-yellow-500 focus:border-transparent
                disabled:opacity-50 disabled:cursor-not-allowed resize-y"
            />
          </div>
          <div className="px-6 pb-6 flex gap-3 border-t border-gray-100">
            <Button
              variant="outline"
              onClick={() => {
                setShowReturnDialog(false);
                setReturnReason("");
              }}
              disabled={returnMutation.isPending}
              className="flex-1"
            >
              {t("cancel")}
            </Button>
            <Button
              onClick={confirmReturn}
              disabled={returnMutation.isPending || !returnReason.trim()}
              className="flex-1"
            >
              {returnMutation.isPending ? t("buttons.processing") : t("buttons.return")}
            </Button>
          </div>
        </div>
      </Modal>

      {/* Cancel Dialog */}
      <Modal
        isOpen={showCancelDialog}
        onClose={() => {
          if (!cancelMutation.isPending) {
            setShowCancelDialog(false);
            setCancelReason("");
          }
        }}
        size="md"
      >
        <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md relative">
          {!cancelMutation.isPending && (
            <div
              onClick={() => {
                setShowCancelDialog(false);
                setCancelReason("");
              }}
              className="absolute top-4 right-4 p-1.5 hover:bg-gray-100 rounded-lg transition-colors cursor-pointer z-10"
            >
              <X className="h-4 w-4 text-gray-500" />
            </div>
          )}
          <div className="p-6 pr-12">
            <div className="flex gap-4 mb-4">
              <div className="flex-shrink-0 w-12 h-12 bg-red-50 text-red-600 rounded-xl flex items-center justify-center">
                <AlertTriangle className="h-6 w-6" />
              </div>
              <div className="flex-1">
                <h3 className="text-lg font-semibold text-gray-900 mb-2">{t("recruitmentRequests.cancelDialogTitle")}</h3>
                <p className="text-sm text-gray-600 leading-relaxed">{t("recruitmentRequests.cancelDialogMessage")}</p>
              </div>
            </div>
          </div>
          <div className="px-6 pb-6">
            <textarea
              placeholder={t("recruitmentRequests.cancelPlaceholder")}
              value={cancelReason}
              onChange={(e) => setCancelReason(e.target.value)}
              disabled={cancelMutation.isPending}
              rows={3}
              className="w-full border border-gray-300 p-2 rounded-md text-gray-700
                focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent
                disabled:opacity-50 disabled:cursor-not-allowed resize-y"
            />
          </div>
          <div className="px-6 pb-6 flex gap-3 border-t border-gray-100">
            <Button
              variant="outline"
              onClick={() => {
                setShowCancelDialog(false);
                setCancelReason("");
              }}
              disabled={cancelMutation.isPending}
              className="flex-1"
            >
              {t("cancel")}
            </Button>
            <Button
              onClick={confirmCancel}
              disabled={cancelMutation.isPending || !cancelReason.trim()}
              className="flex-1"
            >
              {cancelMutation.isPending ? t("buttons.processing") : t("common.confirm")}
            </Button>
          </div>
        </div>
      </Modal>

      {/* Withdraw Dialog */}
      <Modal
        isOpen={showWithdrawDialog}
        onClose={() => !withdrawMutation.isPending && setShowWithdrawDialog(false)}
        size="md"
      >
        <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md relative">
          {!withdrawMutation.isPending && (
            <div
              onClick={() => setShowWithdrawDialog(false)}
              className="absolute top-4 right-4 p-1.5 hover:bg-gray-100 rounded-lg transition-colors cursor-pointer z-10"
            >
              <X className="h-4 w-4 text-gray-500" />
            </div>
          )}
          <div className="p-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-2">{t("recruitmentRequests.withdrawDialogTitle")}</h3>
            <p className="text-sm text-gray-600 mb-4">{t("recruitmentRequests.withdrawDialogMessage")}</p>
            <div className="flex gap-3">
              <Button variant="outline" onClick={() => setShowWithdrawDialog(false)} disabled={withdrawMutation.isPending} className="flex-1">
                {t("cancel")}
              </Button>
              <Button onClick={confirmWithdraw} disabled={withdrawMutation.isPending} className="flex-1">
                {withdrawMutation.isPending ? t("buttons.processing") : t("buttons.withdraw")}
              </Button>
            </div>
          </div>
        </div>
      </Modal>
    </div>
  );
}
