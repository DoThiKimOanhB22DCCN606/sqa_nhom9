import React, { useState } from "react";
import { User, X, Loader2 } from "lucide-react";
import { toast } from "react-toastify";
import { useTranslation } from "react-i18next";
import { uploadServices } from "../../services/uploadServices";

export default function FileUploader({
  label,
  onFileChange,
  preview,
  setPreview,
  disabled,
  maxSize = 5 * 1024 * 1024,
}) {
  const { t } = useTranslation();
  const [uploading, setUploading] = useState(false);

  const handleFileChange = async (e) => {
    const file = e.target.files[0];
    if (file) {
      if (!file.type.startsWith("image/")) {
        toast.error(t("toasts.pleaseSelectImageFile"));
        e.target.value = "";
        return;
      }
      if (file.size > maxSize) {
        toast.error(t("toasts.imageSizeExceeded"));
        e.target.value = "";
        return;
      }

      // Hiển thị preview ngay lập tức
      const reader = new FileReader();
      reader.onloadend = () => setPreview(reader.result);
      reader.readAsDataURL(file);

      // Upload file lên server
      setUploading(true);
      try {
        const response = await uploadServices.uploadFile(file);
        onFileChange(response.data); // Trả về data từ server (URL, ID, etc.)
      } catch (error) {
        console.error("Error uploading file:", error);
        toast.error(error.response?.data?.message || t("toasts.uploadFailed"));
        // Xóa preview nếu upload thất bại
        setPreview(null);
        e.target.value = "";
      } finally {
        setUploading(false);
      }
    }
  };

  const handleRemoveFile = () => {
    onFileChange(null);
    setPreview(null);
    const fileInput = document.getElementById("file-input");
    if (fileInput) fileInput.value = "";
  };

  return (
    <div className="flex flex-col gap-2">
      <label className="block font-medium text-gray-700">{label}</label>
      {!preview ? (
        <label
          htmlFor="file-input"
          className={`flex-1 min-h-[180px] border-2 border-dashed border-gray-300 rounded-lg p-4 flex flex-col items-center justify-center gap-2 cursor-pointer hover:border-blue-400 hover:bg-blue-50/50 transition-colors ${disabled || uploading ? "opacity-50 cursor-not-allowed" : ""}`}
        >
          {uploading ? (
            <>
              <Loader2 className="h-7 w-7 text-blue-500 animate-spin" />
              <div className="text-center">
                <p className="text-xs font-medium text-gray-700">Uploading...</p>
                <p className="text-[10px] text-gray-500 mt-1">Please wait</p>
              </div>
            </>
          ) : (
            <>
              <div className="w-14 h-14 bg-gray-100 rounded-full flex items-center justify-center">
                <User className="h-7 w-7 text-gray-400" />
              </div>
              <div className="text-center">
                <p className="text-xs font-medium text-gray-700">Click to upload</p>
                <p className="text-[10px] text-gray-500 mt-1">JPG, PNG • Max 5MB</p>
              </div>
            </>
          )}
          <input
            id="file-input"
            type="file"
            accept="image/*"
            onChange={handleFileChange}
            disabled={disabled || uploading}
            className="hidden"
          />
        </label>
      ) : (
        <div className="flex-1 min-h-[180px] border border-gray-300 rounded-lg p-4 bg-gray-50 flex flex-col">
          <div className="flex-1 flex items-center justify-center mb-4">
            <div className="relative w-28 h-28 rounded-full overflow-hidden border-2 border-gray-300">
              <img src={preview} alt="Preview" className="w-full h-full object-cover" />
            </div>
          </div>
          <button
            type="button"
            onClick={handleRemoveFile}
            disabled={disabled || uploading}
            className="w-full p-2 text-xs text-red-600 hover:bg-red-100 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed border border-red-200 flex items-center justify-center gap-2"
          >
            <X className="h-4 w-4" />
            <span>Remove</span>
          </button>
        </div>
      )}
    </div>
  );
}