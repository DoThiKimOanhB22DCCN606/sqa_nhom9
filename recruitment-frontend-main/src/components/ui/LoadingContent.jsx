import React from "react";
import { useTranslation } from "react-i18next";

export default function LoadingContent() {
  const { t } = useTranslation();
  return (
    <div className="flex flex-col items-center justify-center h-full">
      <div className="relative">
        {/* Outer ring */}
        <div className="w-16 h-16 border-4 border-gray-200 rounded-full"></div>
        {/* Spinning ring */}
        <div className="absolute top-0 left-0 w-16 h-16 border-4 border-red-500 rounded-full border-t-transparent animate-spin"></div>
      </div>
      <div className="text-gray-700 font-medium text-lg animate-pulse mt-4">
        {t("loading")}
      </div>
    </div>
  );
}
