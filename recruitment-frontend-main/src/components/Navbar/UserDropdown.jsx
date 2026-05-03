import React from "react";
import { useTranslation } from "react-i18next";
import LanguageSwitcher from "./LanguageSwitcher";
import { useAuth } from "../../context/AuthContext";
import { CircleUserRound } from "lucide-react";

const UserDropdown = () => {
  const { t } = useTranslation();
  const { user, logout } = useAuth();
  return (
    <div className="relative group">
      <div
        className="flex items-center justify-center rounded-lg
      transition bg-white hover:bg-gray-200 p-2 gap-2 cursor-pointer"
      >
        <CircleUserRound />
        <div className="flex flex-col items-start">
          <div className="text-sm font-medium text-gray-900">{user?.name}</div>
          <div className="text-xs text-gray-600">
            {user?.department?.name || t("common.noDepartment")}
          </div>
        </div>
      </div>

      {/* menu */}
      <div
        className="
          absolute right-0 mt-2
          opacity-0 -translate-y-1 pointer-events-none
          group-hover:opacity-100 group-hover:translate-y-0 group-hover:pointer-events-auto
          transition duration-200
          bg-white shadow-xl rounded-lg z-10 border border-gray-200
          min-w-[180px]
          [&::before]:content-[''] [&::before]:absolute [&::before]:-top-2 [&::before]:left-0
          [&::before]:w-full [&::before]:h-2
        "
      >
        <ul className="py-1.5">
          {/* Language Switcher */}
          <li className="px-3 py-2 hover:bg-gray-50 transition-colors">
            <div className="flex items-center justify-between gap-3">
              <span className="text-xs text-gray-700">{t("language")}</span>
              <LanguageSwitcher />
            </div>
          </li>

          {/* Logout */}
          <li
            className="px-3 py-2 hover:bg-red-50 cursor-pointer transition-colors border-t border-gray-100"
            onClick={logout}
          >
            <span className="text-xs text-red-600 font-medium">
              {t("logout")}
            </span>
          </li>
        </ul>
      </div>
    </div>
  );
};

export default UserDropdown;
