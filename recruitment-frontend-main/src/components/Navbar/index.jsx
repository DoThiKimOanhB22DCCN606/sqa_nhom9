import { useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { AutoTextSize } from "auto-text-size";
import UserDropdown from "./UserDropdown";
import NotificationsDropdown from "./NotificationsDropdown";
import { useNavigate } from "react-router-dom";
const Navbar = ({ isSidebarVisible, title, sidebarWidth }) => {
  const location = useLocation();
  const navigate = useNavigate();
  const { t } = useTranslation();

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
      case "offers":
        return t("offersPage");
      case "email":
        return t("emailPage");
      default:
        return t("dashboard");
    }
  };

  const cameFromMenu = location.state && location.state.from === "menu";
  const canGoBack =
    (!cameFromMenu && Boolean(location.state && location.state.from)) ||
    (typeof window !== "undefined" && window.history.length > 1);

  return (
    <div
      className="flex justify-between fixed
      pr-4 py-4
      transition-all duration-300 ease-in-out z-20
    "
      style={{
        width: `calc(100% - ${sidebarWidth}px)`,
        left: sidebarWidth,
      }}
    >
      <div
        className="flex items-center h-[60px] w-full justify-between bg-white
      rounded-xl shadow-lg border border-[#f3f3f3] px-4 py-2"
      >
        <div className="flex items-center flex-1">
          <div className="text-2xl font-bold">
            <AutoTextSize minFontSizePx={20} maxFontSizePx={30}>
              {title ?? getTitle()}
            </AutoTextSize>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <NotificationsDropdown />
          <UserDropdown />
        </div>
      </div>
    </div>
  );
};

export default Navbar;
