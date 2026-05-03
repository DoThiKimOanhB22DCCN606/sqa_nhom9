import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "react-toastify";
import { useTranslation } from "react-i18next";
import { formatNumber, parseFormattedNumber } from "../../utils/utils";
import ContentHeader from "../../components/ui/ContentHeader";
import Button from "../../components/ui/Button";
import TextInput from "../../components/ui/TextInput";
import SelectDropdown from "../../components/ui/SelectDropdown";
import RichTextEditor from "../../components/ui/RichTextEditor";
import {
  useCreateJobPosition,
  useUpdateJobPosition,
  useJobPosition,
} from "./hooks/useJobPositions";
import { useRecruitmentRequests } from "../recruitment-requests/hooks/useRecruitmentRequests";
import LoadingContent from "../../components/ui/LoadingContent";

export default function JobPositionAdd() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { t } = useTranslation();
  const isEditMode = !!id;

  const [formData, setFormData] = useState({
    title: "",
    description: "",
    requirements: "",
    benefits: "",
    salaryMin: 0,
    salaryMax: 0,
    currency: "VND",
    employmentType: "Full-time",
    experienceLevel: "Mid-level",
    location: "",
    isRemote: false,
    yearsOfExperience: "",
    quantity: 1,
    deadline: "",
    recruitmentRequestId: null,
  });

  const {
    data: existingPosition,
    isLoading: positionLoading,
    isError: positionError,
  } = useJobPosition(id);

  const { data: requestsData } = useRecruitmentRequests();
  const allRecruitmentRequests = requestsData?.data?.result || [];
  const currentRequestId = existingPosition?.recruitmentRequest?.id || existingPosition?.recruitmentRequestId;
  const recruitmentRequests = allRecruitmentRequests.filter((req) => {
    if (
      isEditMode &&
      currentRequestId &&
      req.id === currentRequestId
    ) {
      return true;
    }
    return req.status === "APPROVED";
  });

  const createMutation = useCreateJobPosition();
  const updateMutation = useUpdateJobPosition();

  useEffect(() => {
    if (positionError) {
      toast.error(t("toasts.cannotLoadPositionInfo"));
    }
  }, [positionError]);

  useEffect(() => {
    if (existingPosition && isEditMode) {
      if (existingPosition.status && existingPosition.status.toUpperCase() !== 'DRAFT') {
        navigate(-1);
        return;
      }

      const requestId = existingPosition.recruitmentRequest?.id || existingPosition.recruitmentRequestId || null;
      setFormData({
        title: existingPosition.title || "",
        description: existingPosition.description || "",
        requirements: existingPosition.requirements || "",
        benefits: existingPosition.benefits || "",
        salaryMin: existingPosition.salaryMin || 0,
        salaryMax: existingPosition.salaryMax || 0,
        currency: existingPosition.currency || "VND",
        employmentType: existingPosition.employmentType || "Full-time",
        experienceLevel: existingPosition.experienceLevel || "Mid-level",
        location: existingPosition.location || "",
        isRemote: existingPosition.isRemote || false,
        yearsOfExperience: existingPosition.yearsOfExperience || "",
        quantity: existingPosition.quantity || 1,
        deadline: existingPosition.deadline || "",
        recruitmentRequestId: requestId,
      });
    }
  }, [existingPosition, isEditMode, navigate, t]);

  const employmentTypes = [
    { id: "Full-time", name: t("employmentTypes.fullTime") },
    { id: "Part-time", name: t("employmentTypes.partTime") },
    { id: "Contract", name: t("employmentTypes.contract") },
    { id: "Intern", name: t("employmentTypes.internship") },
  ];

  const experienceLevels = [
    { id: "Entry-level", name: t("experienceLevels.entry") },
    { id: "Mid-level", name: t("experienceLevels.mid") },
    { id: "Senior-level", name: t("experienceLevels.senior") },
    { id: "Lead", name: t("experienceLevels.lead") },
    { id: "Manager", name: t("experienceLevels.manager") },
  ];

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const handleRecruitmentRequestChange = (requestId) => {
    const selectedRequest = recruitmentRequests.find(
      (req) => req.id === requestId
    );

    if (selectedRequest && !isEditMode) {
      setFormData((prev) => ({
        ...prev,
        recruitmentRequestId: requestId,
        title: selectedRequest.title || prev.title,
        salaryMin: selectedRequest.salaryMin || prev.salaryMin,
        salaryMax: selectedRequest.salaryMax || prev.salaryMax,
        quantity: selectedRequest.numberOfPositions || prev.quantity,
      }));

    } else {
      setFormData((prev) => ({
        ...prev,
        recruitmentRequestId: requestId,
      }));
    }
  };

  const onSubmit = () => {
    if (!formData.title) {
      toast.error(t("toasts.pleaseEnterPositionTitle"));
      return;
    }

    if (!formData.recruitmentRequestId) {
      toast.error(t("toasts.pleaseChooseRecruitmentRequest"));
      return;
    }

    if (isEditMode) {
      updateMutation.mutate(
        { id, data: formData },
        {
          onSuccess: () => {
            toast.success(t("updateSuccess"));
            navigate(-1);
          },
          onError: (error) => {
            const errorMessage =
              error.response?.data?.message ||
              t("errorUpdatePosition");
            toast.error(errorMessage);
          },
        }
      );
    } else {
      createMutation.mutate(formData, {
        onSuccess: () => {
          toast.success(t("toasts.createSuccess"));
          navigate(-1);
        },
        onError: (error) => {
          const errorMessage =
            error.response?.data?.message || t("errorCreatePosition");
          toast.error(errorMessage);
        },
      });
    }
  };

  const isPending = createMutation.isPending || updateMutation.isPending;

  if (positionLoading && isEditMode) {
    return (
      <div className="flex flex-col h-full">
        <ContentHeader
          title={t(isEditMode ? "editJobPosition" : "createJobPosition")}
          actions={
            <>
              <Button onClick={() => onSubmit()} disabled={true}>
                {t("loading")}
              </Button>
              <Button
                variant="outline"
                onClick={() => navigate(-1)}
                disabled={true}
              >
                {t("cancel")}
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
        title={t(isEditMode ? "editJobPosition" : "createJobPosition")}
        actions={
          <>
            <Button onClick={() => onSubmit()} disabled={isPending}>
              {isPending
                ? isEditMode
                  ? t("updating")
                  : t("saving")
                : isEditMode
                ? t("update")
                : t("savePosition")}
            </Button>
            <Button
              variant="outline"
              onClick={() => navigate(-1)}
              disabled={isPending}
            >
              {t("cancel")}
            </Button>
          </>
        }
      />

      <div className="flex-1 flex flex-col mt-4 overflow-y-auto p-6 gap-4 bg-white rounded-xl shadow">
        
        <div className="flex flex-col gap-4">
          
          <div className="rounded-md border border-gray-300 p-4 gap-4 flex flex-col">
            <h2>{t("generalInformation")}</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <SelectDropdown
                label={t("recruitmentRequestLabel")}
                name="recruitmentRequestId"
                value={formData.recruitmentRequestId}
                onChange={handleRecruitmentRequestChange}
                options={recruitmentRequests.map((req) => ({
                  id: req.id,
                  name: req.title,
                }))}
                disabled={isPending || isEditMode}
                placeholder={t("chooseRecruitmentRequest")}
                required
              />

              <TextInput
                label={t("jobPositionTitle")}
                name="title"
                value={formData.title}
                onChange={handleChange}
                placeholder="VD: Senior Backend Developer"
                required
                disabled={isPending}
              />

              <TextInput
                label={t("positionQuantity")}
                name="quantity"
                type="text"
                value={formatNumber(formData.quantity)}
                onChange={(e) => {
                  const parsed = parseFormattedNumber(e.target.value);
                  if (parsed !== null || e.target.value === "") {
                    setFormData(prev => ({ ...prev, quantity: parsed || 1 }));
                  }
                }}
                disabled={isPending}
                required
              />

              <SelectDropdown
                label={t("employmentTypeLabel")}
                name="employmentType"
                value={formData.employmentType}
                onChange={(value) =>
                  setFormData((prev) => ({ ...prev, employmentType: value }))
                }
                options={employmentTypes}
                disabled={isPending}
                required
              />

              <SelectDropdown
                label={t("experienceLevel")}
                name="experienceLevel"
                value={formData.experienceLevel}
                onChange={(value) =>
                  setFormData((prev) => ({ ...prev, experienceLevel: value }))
                }
                options={experienceLevels}
                disabled={isPending}
                required
              />

              <TextInput
                label={t("yearsExperience")}
                name="yearsOfExperience"
                value={formData.yearsOfExperience}
                onChange={handleChange}
                placeholder="VD: 1 năm, 2-3 năm..."
                disabled={isPending}
              />

              <TextInput
                label={t("workLocation")}
                name="location"
                value={formData.location}
                onChange={handleChange}
                placeholder="Hà Nội, Hồ Chí Minh..."
                disabled={isPending}
                required
              />

              <TextInput
                label={t("applicationDeadline")}
                name="deadline"
                type="date"
                value={formData.deadline}
                onChange={handleChange}
                disabled={isPending}
                required
              />
            </div>

            <div className="flex items-center">
              <input
                type="checkbox"
                id="isRemote"
                name="isRemote"
                checked={formData.isRemote}
                onChange={handleChange}
                disabled={isPending}
                className="h-4 w-4 text-red-600 focus:ring-red-500 border-gray-300 rounded"
              />
              <label htmlFor="isRemote" className="ml-2 text-sm text-gray-700">
                {t("remoteWork")}
              </label>
            </div>
          </div>

          
          <div className="rounded-md border border-gray-300 p-4 gap-4 flex flex-col">
            <h2>{t("salaryInfo")}</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <TextInput
                label={t("minSalary")}
                placeholder="0"
                type="text"
                name="salaryMin"
                value={formatNumber(formData.salaryMin)}
                onChange={(e) => {
                  const parsed = parseFormattedNumber(e.target.value);
                  if (parsed !== null || e.target.value === "") {
                    setFormData(prev => ({ ...prev, salaryMin: parsed }));
                  }
                }}
                required
                disabled={isPending}
              />
              <TextInput
                label={t("maxSalary")}
                placeholder="0"
                type="text"
                name="salaryMax"
                value={formatNumber(formData.salaryMax)}
                onChange={(e) => {
                  const parsed = parseFormattedNumber(e.target.value);
                  if (parsed !== null || e.target.value === "") {
                    setFormData(prev => ({ ...prev, salaryMax: parsed }));
                  }
                }}
                required
                disabled={isPending}
              />
            </div>
          </div>

          
          <div className="rounded-md border border-gray-300 p-4 gap-4 flex flex-col">
            <h2>{t("jobDescriptionSection")}</h2>
            <div className="gap-4 flex flex-col">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  {t("jobDescription")}
                </label>
                <RichTextEditor
                  value={formData.description}
                  onChange={(value) =>
                    setFormData((prev) => ({ ...prev, description: value }))
                  }
                  placeholder={t("enterJobDescription")}
                  disabled={isPending}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  {t("requirements")}
                </label>
                <RichTextEditor
                  value={formData.requirements}
                  onChange={(value) =>
                    setFormData((prev) => ({ ...prev, requirements: value }))
                  }
                  placeholder={t("enterJobRequirements")}
                  disabled={isPending}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  {t("benefits")}
                </label>
                <RichTextEditor
                  value={formData.benefits}
                  onChange={(value) =>
                    setFormData((prev) => ({ ...prev, benefits: value }))
                  }
                  placeholder={t("enterBenefits")}
                  disabled={isPending}
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
