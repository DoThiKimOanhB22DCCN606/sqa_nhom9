import { useState, useEffect } from "react";
import { X, UserPlus, Upload, FileText, Trash2, Loader2 } from "lucide-react";
import Modal from "../../../components/ui/Modal";
import Button from "../../../components/ui/Button";
import TextInput from "../../../components/ui/TextInput";
import { toast } from "react-toastify";
import { useCreateCandidate } from "../hooks/useCandidates";
import { useTranslation } from "react-i18next";
import { uploadServices } from "../../../services/uploadServices";

export default function AddCandidateModal({
  isOpen,
  onClose,
  jobPosition,
  onSuccess,
}) {
  const { t } = useTranslation();
  const createMutation = useCreateCandidate();

  const [formData, setFormData] = useState({
    fullName: "",
    email: "",
    phone: "",
    notes: "",
    jobPositionId: jobPosition?.id || null,
  });

  const [cvFile, setCvFile] = useState(null);
  const [cvFileData, setCvFileData] = useState(null); // Dữ liệu trả về từ server
  const [uploading, setUploading] = useState(false);

  // Update jobPositionId when jobPosition changes
  useEffect(() => {
    if (jobPosition?.id) {
      setFormData((prev) => ({
        ...prev,
        jobPositionId: jobPosition.id,
      }));
    }
  }, [jobPosition]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleFileChange = async (e) => {
    const file = e.target.files[0];

    if (file) {
      // Validate file type
      if (file.type !== "application/pdf") {
        toast.error(t("toasts.pleaseSelectPdfFile"));
        e.target.value = ""; // Reset input
        return;
      }

      // Validate file size (max 5MB)
      const maxSize = 5 * 1024 * 1024; // 5MB in bytes
      if (file.size > maxSize) {
        toast.error(t("toasts.fileSizeExceeded"));
        e.target.value = ""; // Reset input
        return;
      }

      // Lưu thông tin file để hiển thị
      setCvFile(file);

      // Upload file lên server
      setUploading(true);
      try {
        const response = await uploadServices.uploadFile(file);
        setCvFileData(response.data); // Lưu dữ liệu trả về từ server
      } catch (error) {
        console.error("Error uploading CV file:", error);
        toast.error(error.response?.data?.message || t("toasts.uploadFailed"));
        // Xóa file nếu upload thất bại
        setCvFile(null);
        setCvFileData(null);
        e.target.value = "";
      } finally {
        setUploading(false);
      }
    }
  };

  const handleRemoveFile = () => {
    setCvFile(null);
    setCvFileData(null);
    // Reset file input
    const fileInput = document.getElementById("cv-file-input");
    if (fileInput) {
      fileInput.value = "";
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    // Validation
    if (!formData.fullName.trim()) {
      toast.error(t("toasts.pleaseEnterFullName"));
      return;
    }
    if (!formData.email.trim()) {
      toast.error(t("toasts.pleaseEnterEmail"));
      return;
    }
    if (!formData.phone.trim()) {
      toast.error(t("toasts.pleaseEnterPhone"));
      return;
    }

    // Email validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(formData.email)) {
      toast.error(t("invalidEmail"));
      return;
    }

    // Phone validation (basic)
    const phoneRegex = /^[0-9]{10,11}$/;
    if (!phoneRegex.test(formData.phone.replace(/\s/g, ""))) {
      toast.error(t("toasts.invalidPhoneNumber"));
      return;
    }

    // CV file validation
    if (!cvFileData) {
      toast.error(t("toasts.pleaseUploadCV"));
      return;
    }

    // Prepare data với CV file đã upload
    const submitData = {
      name: formData.fullName,
      email: formData.email,
      phone: formData.phone,
      jobPositionId: formData.jobPositionId,
      cvUrl: cvFileData.url || cvFileData,
      notes: formData.notes,
    };

    createMutation.mutate(submitData, {
      onSuccess: () => {
        toast.success(t("toasts.addCandidateSuccess"));
        // Reset form
        setFormData({
          fullName: "",
          email: "",
          phone: "",
          notes: "",
          jobPositionId: jobPosition?.id || null,
        });
        setCvFile(null);
        setCvFileData(null);

        if (onSuccess) {
          onSuccess();
        }

        onClose();
      },
    });
  };

  const handleClose = () => {
    if (!createMutation.isPending) {
      // Reset form on close
      setFormData({
        fullName: "",
        email: "",
        phone: "",
        notes: "",
        jobPositionId: jobPosition?.id || null,
      });
      setCvFile(null);
      setCvFileData(null);
      onClose();
    }
  };

  return (
    <Modal isOpen={isOpen} size="xl" onClose={handleClose}>
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-[1100px] mx-auto max-h-[85vh] flex flex-col">
        {/* Close button */}
        {!createMutation.isPending && (
          <div
            onClick={handleClose}
            className="absolute top-4 right-4 p-1.5 hover:bg-gray-100 rounded-lg transition-colors cursor-pointer z-10"
            aria-label="Close"
          >
            <X className="h-5 w-5 text-gray-500" />
          </div>
        )}

        {/* Header */}
        <div className="px-8 py-6 pr-14 border-b border-gray-200">
          <div className="flex gap-4 items-start">
            {/* Icon */}
            <div className="flex-shrink-0 w-12 h-12 bg-blue-50 text-blue-600 rounded-xl flex items-center justify-center">
              <UserPlus className="h-6 w-6" />
            </div>

            {/* Title & Subtitle */}
            <div className="flex-1 pt-1">
              <h2 className="text-xl font-semibold text-gray-900 mb-1">
                {t("modals.addCandidateTitle")}
              </h2>
              {jobPosition && (
                <p className="text-sm text-gray-600">
                  {t("modals.position")}:{" "}
                  <span className="font-medium text-gray-900">
                    {jobPosition.title}
                  </span>
                  {" • "}
                  <span className="text-gray-500">
                    {jobPosition.department}
                  </span>
                </p>
              )}
            </div>
          </div>
        </div>

        {/* Body - Scrollable Form */}
        <form onSubmit={handleSubmit} className="flex flex-col flex-1 min-h-0">
          <div className="flex-1 overflow-y-auto px-8 py-6">
            <div className="grid grid-cols-3 gap-8">
              {/* Left Column - Form Fields */}
              <div className="col-span-2 space-y-4">
                {/* Full Name */}
                <TextInput
                  label={t("modals.fullName")}
                  placeholder={t("modals.fullNamePlaceholder")}
                  name="fullName"
                  value={formData.fullName}
                  onChange={handleChange}
                  required={true}
                  disabled={createMutation.isPending}
                />

                {/* Email & Phone - Grid */}
                <div className="grid grid-cols-2 gap-4">
                  <TextInput
                    label={t("modals.email")}
                    type="email"
                    placeholder={t("modals.emailPlaceholder")}
                    name="email"
                    value={formData.email}
                    onChange={handleChange}
                    required={true}
                    disabled={createMutation.isPending}
                  />

                  <TextInput
                    label={t("modals.phone")}
                    type="tel"
                    placeholder={t("modals.phonePlaceholder")}
                    name="phone"
                    value={formData.phone}
                    onChange={handleChange}
                    required={true}
                    disabled={createMutation.isPending}
                  />
                </div>

                {/* Notes */}
                <div className="flex flex-col gap-2">
                  <label className="block text-gray-700">
                    {t("modals.notes")}
                  </label>
                  <textarea
                    placeholder={t("modals.notesPlaceholder")}
                    name="notes"
                    value={formData.notes}
                    onChange={handleChange}
                    disabled={createMutation.isPending}
                    rows={3}
                    className="border border-gray-300 px-3 py-2 rounded-lg text-sm text-gray-900
                      focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent
                      disabled:opacity-50 disabled:cursor-not-allowed
                      placeholder:text-gray-400
                      resize-none"
                  />
                </div>
              </div>

              {/* Right Column - CV Upload */}
              <div className="col-span-1 flex flex-col">
                <div className="flex flex-col gap-2 flex-1">
                  <label className="block text-gray-700">
                    {t("modals.cvFileRequired")}
                  </label>

                  {uploading ? (
                    <div className="flex-1 border-2 border-dashed border-blue-300 bg-blue-50 rounded-lg p-4 flex flex-col items-center justify-center gap-3">
                      <div className="relative">
                        <div className="w-16 h-16 rounded-full bg-blue-100 flex items-center justify-center">
                          <Loader2 className="h-8 w-8 text-blue-600 animate-spin" />
                        </div>
                      </div>
                      <div className="text-center">
                        <p className="text-sm font-semibold text-blue-900">
                          {t("modals.uploading")}
                        </p>
                        <p className="text-xs text-blue-600 mt-1">
                          {t("modals.pleaseWait")}
                        </p>
                      </div>
                    </div>
                  ) : !cvFile ? (
                    <label
                      htmlFor="cv-file-input"
                      className={`
                        flex-1
                        border-2 border-dashed border-gray-300 rounded-lg p-4
                        flex flex-col items-center justify-center gap-2
                        cursor-pointer hover:border-red-400 hover:bg-red-50/50
                        transition-colors
                        ${createMutation.isPending ? 'opacity-50 cursor-not-allowed' : ''}
                      `}
                    >
                      <Upload className="h-10 w-10 text-gray-400" />
                      <div className="text-center">
                        <p className="text-sm font-medium text-gray-700">
                          {t("modals.clickToSelectFile")}
                        </p>
                        <p className="text-xs text-gray-500 mt-1">
                          {t("modals.pdfMaxSize")}
                        </p>
                      </div>
                      <input
                        id="cv-file-input"
                        type="file"
                        accept=".pdf,application/pdf"
                        onChange={handleFileChange}
                        disabled={createMutation.isPending}
                        className="hidden"
                      />
                    </label>
                  ) : (
                    <div className="flex-1 border border-gray-300 rounded-lg p-4 bg-gray-50 flex flex-col">
                      <div className="flex items-start gap-3 mb-4">
                        <div className="flex-shrink-0 w-10 h-10 bg-red-100 text-red-600 rounded-lg flex items-center justify-center">
                          <FileText className="h-5 w-5" />
                        </div>
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-medium text-gray-900 truncate">
                            {cvFile.name}
                          </p>
                          <p className="text-xs text-gray-500 mt-1">
                            {(cvFile.size / 1024).toFixed(2)} KB
                          </p>
                        </div>
                      </div>
                      <button
                        type="button"
                        onClick={handleRemoveFile}
                        disabled={createMutation.isPending}
                        className="w-full mt-auto p-2 text-sm text-red-600 hover:bg-red-100 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed border border-red-200"
                      >
                        <Trash2 className="h-4 w-4 mx-auto" />
                      </button>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>

          {/* Footer */}
          <div className="px-8 py-5 border-t border-gray-100 flex gap-4">
            <Button
              type="button"
              variant="outline"
              onClick={handleClose}
              disabled={createMutation.isPending}
              className="flex-1"
            >
              {t("common.cancel")}
            </Button>
            <Button
              type="submit"
              disabled={createMutation.isPending}
              onClick={handleSubmit}
              className="flex-1"
            >
              {createMutation.isPending ? t("modals.adding") : t("modals.addCandidate")}
            </Button>
          </div>
        </form>
      </div>
    </Modal>
  );
}
