import { useEffect } from "react";
import { createPortal } from "react-dom";
import Button from "./Button";
import { AlertTriangle, Info, CheckCircle, X } from "lucide-react";
import { useTranslation } from "react-i18next";

export default function ConfirmDialog({
  isOpen,
  onClose,
  onConfirm,
  title,
  message,
  confirmText,
  cancelText,
  variant = "default", // default, danger, success, info
  loading = false,
}) {
  const { t } = useTranslation();
  const displayConfirmText = confirmText || t("common.confirm");
  const displayCancelText = cancelText || t("common.cancel");
  // Handle ESC key
  useEffect(() => {
    if (!isOpen) return;

    const handleEsc = (e) => {
      if (e.key === "Escape" && !loading) {
        onClose();
      }
    };

    document.addEventListener("keydown", handleEsc);
    return () => document.removeEventListener("keydown", handleEsc);
  }, [isOpen, loading, onClose]);

  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = "hidden";
    } else {
      document.body.style.overflow = "unset";
    }

    return () => {
      document.body.style.overflow = "unset";
    };
  }, [isOpen]);

  if (!isOpen) return null;

  const getIcon = () => {
    switch (variant) {
      case "danger":
        return <AlertTriangle className="h-6 w-6" />;
      case "success":
        return <CheckCircle className="h-6 w-6" />;
      case "info":
        return <Info className="h-6 w-6" />;
      default:
        return null;
    }
  };

  const getColors = () => {
    switch (variant) {
      case "danger":
        return {
          icon: "text-red-600",
          iconBg: "bg-red-50",
        };
      case "success":
        return {
          icon: "text-green-600",
          iconBg: "bg-green-50",
        };
      case "info":
        return {
          icon: "text-blue-600",
          iconBg: "bg-blue-50",
        };
      default:
        return {
          icon: "text-gray-600",
          iconBg: "bg-gray-50",
        };
    }
  };

  const colors = getColors();

  const dialogContent = (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      onClick={(e) => {
        if (!loading && e.target === e.currentTarget) {
          onClose();
        }
      }}
    >
      {/* Backdrop */}
      <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" />

      {/* Dialog */}
      <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-md transform transition-all">
        {/* Close button */}
        {!loading && (
          <div
            onClick={onClose}
            className="absolute top-4 right-4 p-1.5 hover:bg-gray-100 rounded-lg transition-colors cursor-pointer"
            aria-label="Close"
          >
            <X className="h-4 w-4 text-gray-500" />
          </div>
        )}

        <div className="p-6 pr-12">
          {/* Icon & Content */}
          <div className="flex gap-4">
            {/* Icon */}
            {getIcon() && (
              <div
                className={`flex-shrink-0 w-12 h-12 ${colors.iconBg} ${colors.icon} rounded-xl flex items-center justify-center`}
              >
                {getIcon()}
              </div>
            )}

            {/* Text Content */}
            <div className="flex-1 pt-1">
              {/* Title */}
              {title && (
                <h3 className="text-lg font-semibold text-gray-900 mb-2">
                  {title}
                </h3>
              )}

              {/* Message */}
              <p className="text-sm text-gray-600 leading-relaxed">{message}</p>
            </div>
          </div>
        </div>

        {/* Actions */}
        <div className="px-6 pb-6 flex gap-3">
          <Button
            variant="outline"
            onClick={onClose}
            disabled={loading}
            className="flex-1"
          >
            {displayCancelText}
          </Button>
          <Button
            onClick={() => {
              onConfirm();
              if (!loading) {
                onClose();
              }
            }}
            disabled={loading}
            className="flex-1"
          >
            {loading ? t("common.processing") : displayConfirmText}
          </Button>
        </div>
      </div>
    </div>
  );

  return createPortal(dialogContent, document.body);
}
