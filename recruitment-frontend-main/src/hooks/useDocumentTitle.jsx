import { useEffect } from "react";
import { useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";

export const useDocumentTitle = () => {
  const location = useLocation();
  const { t } = useTranslation();

  useEffect(() => {
    const getTitle = () => {
      const pathSegments = location.pathname
        .split("/")
        .filter((segment) => segment !== "");
      const mainRoute = pathSegments[0] || "";

      switch (mainRoute) {
      case "":
        return t("dashboard");
      case "employees":
        return t("employees");
      case "users":
        return t("users");
      case "roles":
        return t("roles");
      case "workflows":
        return t("workflows");
      case "recruitment-requests":
        return t("recruitmentRequestsPage");
      case "job-positions":
        return t("jobPositionsPage");
      case "calendar":
        return t("calendar");
      case "candidates":
        return t("candidatesPage");
      case "email":
        return t("emailPage");
      case "offers":
        return t("offersPage");
      default:
        return t("dashboard");
    }
    };

    const title = getTitle();
    document.title = `${title} | MelodySoft System`;
  }, [location, t]);
};
