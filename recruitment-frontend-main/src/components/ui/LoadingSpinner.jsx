import React from "react";
import { useTranslation } from "react-i18next";

export default function LoadingSpinner({
  size = "md",
  color = "blue",
  text,
}) {
  const { t } = useTranslation();
  const displayText = text !== undefined ? text : t("loading");
  const sizes = {
    sm: "w-8 h-8 border-2",
    md: "w-16 h-16 border-4",
    lg: "w-24 h-24 border-4",
  };

  const colors = {
    blue: "border-blue-500",
    red: "border-red-500",
    green: "border-green-500",
    white: "border-white",
  };

  return (
    <div className="flex flex-col items-center gap-4">
      {/* Circular Spinner */}
      <div className="relative">
        {/* Outer ring */}
        <div className={`${sizes[size]} border-gray-200 rounded-full`}></div>
        {/* Spinning ring */}
        <div
          className={`absolute top-0 left-0 ${sizes[size]} ${colors[color]} rounded-full border-t-transparent animate-spin`}
        ></div>
      </div>

      {/* Optional loading text */}
      {displayText && <div className="font-medium text-lg">{displayText}</div>}
    </div>
  );
}
