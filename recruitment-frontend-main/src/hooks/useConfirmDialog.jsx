import { useState } from "react";

/**
 * Custom hook to easily use ConfirmDialog
 *
 * @example
 * const { showConfirm, ConfirmDialogComponent } = useConfirmDialog();
 *
 * const handleDelete = () => {
 *   showConfirm({
 *     title: "Xóa item",
 *     message: "Bạn có chắc?",
 *     onConfirm: () => { // delete logic },
 *     variant: "danger"
 *   });
 * };
 *
 * return (
 *   <>
 *     <Button onClick={handleDelete}>Delete</Button>
 *     {ConfirmDialogComponent}
 *   </>
 * );
 */
export default function useConfirmDialog() {
  const [isOpen, setIsOpen] = useState(false);
  const [config, setConfig] = useState({
    title: "",
    message: "",
    confirmText: "Xác nhận",
    cancelText: "Hủy",
    variant: "default",
    onConfirm: () => {},
  });

  const showConfirm = ({
    title = "Xác nhận",
    message,
    confirmText = "Xác nhận",
    cancelText = "Hủy",
    variant = "default",
    onConfirm,
  }) => {
    setConfig({
      title,
      message,
      confirmText,
      cancelText,
      variant,
      onConfirm,
    });
    setIsOpen(true);
  };

  const hideConfirm = () => {
    setIsOpen(false);
  };

  const handleConfirm = () => {
    config.onConfirm();
    hideConfirm();
  };

  const ConfirmDialogComponent = isOpen ? (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      onClick={(e) => {
        if (e.target === e.currentTarget) {
          hideConfirm();
        }
      }}
    >
      <div className="absolute inset-0 bg-black/20 backdrop-blur-sm transition-opacity" />

      <div className="relative bg-white rounded-xl shadow-xl w-full max-w-md animate-in fade-in zoom-in-95 duration-200">
        <div className="p-6">
          <div className="text-center">
            {config.variant === "danger" && (
              <div className="mx-auto w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mb-4">
                <svg
                  className="h-12 w-12 text-red-600"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
                  />
                </svg>
              </div>
            )}

            <h3 className="text-lg font-semibold text-gray-900 mb-2">
              {config.title}
            </h3>

            <p className="text-gray-600 mb-6">{config.message}</p>

            <div className="flex gap-3 justify-center">
              <button
                onClick={hideConfirm}
                className="inline-flex items-center justify-center px-4 py-2 h-9 rounded-md font-medium shadow-sm border-2 border-[#DC2626] text-[#DC2626] bg-white hover:bg-red-50 transition-all duration-200 cursor-pointer select-none"
              >
                {config.cancelText}
              </button>
              <button
                onClick={handleConfirm}
                className="inline-flex items-center justify-center px-4 py-2 h-9 rounded-md font-medium shadow-sm hover:opacity-90 transition-opacity duration-200 cursor-pointer select-none"
                style={{ backgroundColor: "#DC2626", color: "#fff" }}
              >
                {config.confirmText}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  ) : null;

  return {
    showConfirm,
    hideConfirm,
    ConfirmDialogComponent,
    isOpen,
  };
}
