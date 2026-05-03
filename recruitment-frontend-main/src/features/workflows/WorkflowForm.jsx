import React, { useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { useParams, useNavigate } from "react-router-dom";
import { Plus, Trash2, MoveUp, MoveDown } from "lucide-react";
import ContentHeader from "../../components/ui/ContentHeader";
import TextInput from "../../components/ui/TextInput";
import SelectDropdown from "../../components/ui/SelectDropdown";
import Button from "../../components/ui/Button";
import LoadingContent from "../../components/ui/LoadingContent";
import { useCreateWorkflow, useUpdateWorkflow, useWorkflow } from "../../hooks/useWorkflows";
import { useAllDepartments } from "../../hooks/useDepartments";
import { useAllPositions } from "../../hooks/usePositions";
import { toast } from "react-toastify";

const LEVEL_OPTIONS = [
  { id: "C-Level", name: "C-Level" },
  { id: "Manager", name: "Manager" },
  { id: "Senior", name: "Senior" },
  { id: "Junior", name: "Junior" },
  { id: "Intern", name: "Intern" },
];

const TYPE_OPTIONS = [
  { id: "REQUEST", name: "Yêu cầu tuyển dụng" },
  { id: "OFFER", name: "Phê duyệt Offer" },
];

export default function WorkflowForm() {
  const { t } = useTranslation();
  const { id } = useParams();
  const navigate = useNavigate();
  const formRef = useRef(null);

  const isAddMode = !id;
  const [isEditMode, setIsEditMode] = useState(isAddMode);

  const [form, setForm] = useState({
    name: "",
    description: "",
    type: "REQUEST",
    departmentId: null,
    steps: [],
  });

  const { data: workflowData, isLoading: workflowLoading } = useWorkflow(id, {
    enabled: !!id,
  });
  const { data: departmentsData = [] } = useAllDepartments();
  const { data: positionsData = [] } = useAllPositions();

  const createWorkflow = useCreateWorkflow();
  const updateWorkflow = useUpdateWorkflow();

  useEffect(() => {
    if (workflowData) {
      setForm({
        name: workflowData.name || "",
        description: workflowData.description || "",
        type: workflowData.type || "REQUEST",
        departmentId: workflowData.departmentId || null,
        steps: workflowData.steps
          ? workflowData.steps.map((s) => ({
              stepOrder: s.stepOrder,
              stepName: s.stepName || "",
              approverPositionId: s.approverPositionId || null,
            }))
          : [],
      });
    }
  }, [workflowData]);

  const onChange = (field) => (e) => {
    const value = e?.target ? e.target.value : e;
    setForm((s) => ({ ...s, [field]: value }));
  };

  const onDepartmentChange = (value) => {
    setForm((s) => ({
      ...s,
      departmentId: value,
    }));
  };

  const addStep = () => {
    setForm((s) => ({
      ...s,
      steps: [
        ...s.steps,
        {
          stepOrder: s.steps.length + 1,
          stepName: "",
          approverPositionId: null,
        },
      ],
    }));
  };

  const removeStep = (index) => {
    setForm((s) => {
      const newSteps = s.steps.filter((_, i) => i !== index);
      return {
        ...s,
        steps: newSteps.map((step, i) => ({ ...step, stepOrder: i + 1 })),
      };
    });
  };

  const moveStep = (index, direction) => {
    const newIndex = direction === "up" ? index - 1 : index + 1;
    if (newIndex < 0 || newIndex >= form.steps.length) return;

    setForm((s) => {
      const newSteps = [...s.steps];
      [newSteps[index], newSteps[newIndex]] = [newSteps[newIndex], newSteps[index]];
      return {
        ...s,
        steps: newSteps.map((step, i) => ({ ...step, stepOrder: i + 1 })),
      };
    });
  };

  const onStepChange = (index, field) => (e) => {
    const value = e?.target ? e.target.value : e;
    setForm((s) => {
      const newSteps = [...s.steps];
      newSteps[index] = { ...newSteps[index], [field]: value };
      return { ...s, steps: newSteps };
    });
  };

  const handleSubmit = (e) => {
    e?.preventDefault();

    const requiredFieldsMap = {
      name: t("workflowName"),
      description: t("description"),
    };

    for (const [field, label] of Object.entries(requiredFieldsMap)) {
      const value = form[field];
      if (value === null || value === undefined || String(value).trim() === "") {
        toast.error(`${label} ${t("isRequired")}`);
        return;
      }
    }

    if (form.steps.length === 0) {
      toast.error(
        t("stepsRequired")
      );
      return;
    }

    for (let i = 0; i < form.steps.length; i++) {
      const step = form.steps[i];
      if (!step.approverPositionId) {
        toast.error(
          `${t("step")} ${i + 1}: ${t("approverRequired")}`
        );
        return;
      }
    }

    const payload = {
      name: form.name,
      description: form.description,
      type: form.type,
      steps: form.steps.map((s) => ({
        stepOrder: s.stepOrder,
        approverPositionId: Number(s.approverPositionId),
      })),
    };

    if (form.departmentId) {
      payload.departmentId = Number(form.departmentId);
    }

    if (isAddMode) {
      createWorkflow.mutate(payload, {
        onSuccess: () => {
          toast.success(t("toasts.createSuccess"));
          navigate("/workflows");
        },
      });
    } else {
      updateWorkflow.mutate(
        { id, data: payload },
        {
          onSuccess: () => {
            toast.success(t("toasts.updateSuccess"));
            setIsEditMode(false);
          },
        }
      );
    }
  };

  const isPending = isAddMode ? createWorkflow.isPending : updateWorkflow.isPending;

  if ((workflowLoading && !isAddMode) || isPending) {
    return (
      <div className="flex flex-col h-full">
        <ContentHeader
          title={
            isAddMode
              ? t("addWorkflow")
              : t("workflowDetail")
          }
          actions={
            <>
              {isAddMode ? (
                <>
                  <Button variant="outline" onClick={() => navigate(-1)}>
                    {t("cancel")}
                  </Button>
                  <Button onClick={() => formRef.current?.requestSubmit()}>
                    {t("save")}
                  </Button>
                </>
              ) : isEditMode ? (
                <>
                  <Button
                    variant="outline"
                    onClick={() => {
                      setIsEditMode(false);
                      if (workflowData) {
                        setForm({
                          name: workflowData.name || "",
                          description: workflowData.description || "",
                          type: workflowData.type || "REQUEST",
                          applyConditions: {
                            department_id: workflowData.applyConditions?.department_id || null,
                          },
                          steps: workflowData.steps
                            ? workflowData.steps.map((s) => ({
                                stepOrder: s.stepOrder,
                                stepName: s.stepName || "",
                                approverPositionId: s.approverPositionId || null,
                              }))
                            : [],
                        });
                      }
                    }}
                  >
                    {t("cancel")}
                  </Button>
                  <Button onClick={() => formRef.current?.requestSubmit()}>
                    {t("save")}
                  </Button>
                </>
              ) : (
                <>
                  <Button variant="outline" onClick={() => navigate(-1)}>
                    {t("cancel")}
                  </Button>
                  <Button onClick={() => setIsEditMode(true)}>
                    {t("edit")}
                  </Button>
                </>
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
            ? t("addWorkflow")
            : t("workflowDetail")
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
                    if (workflowData) {
                      setForm({
                        name: workflowData.name || "",
                        description: workflowData.description || "",
                        type: workflowData.type || "REQUEST",
                        departmentId: workflowData.departmentId || null,
                        steps: workflowData.steps
                          ? workflowData.steps.map((s) => ({
                              stepOrder: s.stepOrder,
                              stepName: s.stepName || "",
                              approverPositionId: s.approverPositionId || null,
                            }))
                          : [],
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
                <Button onClick={() => setIsEditMode(true)}>
                  {t("edit")}
                </Button>
              </>
            )}
          </>
        }
      />

      <div className="flex-1 mt-4 overflow-auto">
        <div className="bg-white rounded-xl shadow p-6">
          <form ref={formRef} onSubmit={handleSubmit}>
            <div className="space-y-6">
              {/* Basic Info */}
              <div className="space-y-4">
                <h3 className="text-lg font-semibold text-gray-900">
                  {t("basicInfo")}
                </h3>
                <TextInput
                  label={t("workflowName")}
                  value={form.name}
                  onChange={onChange("name")}
                  required
                  disabled={!isEditMode}
                />
                <TextInput
                  label={t("description")}
                  value={form.description}
                  onChange={onChange("description")}
                  required
                  disabled={!isEditMode}
                />
                <SelectDropdown
                  label={t("workflowType")}
                  options={TYPE_OPTIONS}
                  value={form.type}
                  onChange={onChange("type")}
                  placeholder={t("selectType")}
                  required
                  disabled={!isEditMode}
                />
              </div>

              {/* Apply Conditions */}
              <div className="space-y-4">
                <h3 className="text-lg font-semibold text-gray-900">
                  {t("applyConditions")}
                </h3>
                <SelectDropdown
                  label={t("department")}
                  options={[{ id: null, name: t("allDepartments") }, ...departmentsData.map((d) => ({ id: d.id, name: d.name }))]}
                  value={form.departmentId}
                  onChange={onDepartmentChange}
                  placeholder={t("allDepartments")}
                  disabled={!isEditMode}
                />
              </div>

              {/* Approval Steps */}
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <h3 className="text-lg font-semibold text-gray-900">
                    {t("approvalSteps")}
                  </h3>
                  {isEditMode && (
                    <Button type="button" variant="outline" onClick={addStep}>
                      <Plus className="h-4 w-4 mr-2" />
                      {t("addStep")}
                    </Button>
                  )}
                </div>

                {form.steps.length === 0 ? (
                  <div className="text-center py-8 text-gray-500 border border-gray-200 rounded-lg">
                    {t("noSteps")}
                  </div>
                ) : (
                  <div className="border border-gray-200 rounded-lg overflow-hidden">
                    <table className="w-full">
                      <thead className="bg-gray-50 border-b border-gray-200">
                        <tr>
                          <th className="p-3 text-left text-sm font-medium text-gray-600 w-20">
                            {t("order")}
                          </th>
                          <th className="p-3 text-left text-sm font-medium text-gray-600">
                            {t("recruitmentRequests.approver")}
                          </th>
                          {isEditMode && (
                            <th className="p-3 text-center text-sm font-medium text-gray-600 w-32">
                              {t("actions")}
                            </th>
                          )}
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-gray-200">
                        {form.steps.map((step, index) => (
                          <tr key={index} className="hover:bg-gray-50">
                            <td className="p-3">
                              <div className="w-8 h-8 bg-blue-100 text-blue-800 rounded-full flex items-center justify-center font-semibold text-sm">
                                {step.stepOrder}
                              </div>
                            </td>
                            <td className="p-3">
                              {isEditMode ? (
                                <select
                                  value={step.approverPositionId || ""}
                                  onChange={(e) => {
                                    const value = e.target.value ? Number(e.target.value) : null;
                                    onStepChange(index, "approverPositionId")({ target: { value } });
                                  }}
                                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                >
                                  <option value="">
                                    {t("selectPosition")}
                                  </option>
                                  {positionsData.map((p) => (
                                    <option key={p.id} value={p.id}>
                                      {p.name}
                                    </option>
                                  ))}
                                </select>
                              ) : (
                                <span className="text-sm text-gray-900">
                                  {positionsData.find((p) => p.id === step.approverPositionId)?.name || "-"}
                                </span>
                              )}
                            </td>
                            {isEditMode && (
                              <td className="p-3">
                                <div className="flex items-center justify-center gap-1">
                                  <button
                                    type="button"
                                    onClick={() => moveStep(index, "up")}
                                    disabled={index === 0}
                                    className="p-1 text-gray-600 hover:text-gray-800 disabled:opacity-30 disabled:cursor-not-allowed"
                                    title={t("moveUp")}
                                  >
                                    <MoveUp className="h-4 w-4" />
                                  </button>
                                  <button
                                    type="button"
                                    onClick={() => moveStep(index, "down")}
                                    disabled={index === form.steps.length - 1}
                                    className="p-1 text-gray-600 hover:text-gray-800 disabled:opacity-30 disabled:cursor-not-allowed"
                                    title={t("moveDown")}
                                  >
                                    <MoveDown className="h-4 w-4" />
                                  </button>
                                  <button
                                    type="button"
                                    onClick={() => removeStep(index)}
                                    className="p-1 text-red-600 hover:text-red-800"
                                    title={t("delete")}
                                  >
                                    <Trash2 className="h-4 w-4" />
                                  </button>
                                </div>
                              </td>
                            )}
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
