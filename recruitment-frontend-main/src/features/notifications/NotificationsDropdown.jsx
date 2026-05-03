import { useNotifications, useMarkNotificationAsRead, useMarkAllNotificationsAsRead } from "./hooks/useNotifications";
import { Bell, CheckCheck, FileText, UserCheck, Calendar, Mail, Check } from "lucide-react";
import { useState, useRef, useEffect } from "react";
import { useSocket } from "../../context/SocketContext";
import { useTranslation } from "react-i18next";

const getNotificationIcon = (type) => {
  switch (type) {
    case "RECRUITMENT_REQUEST":
      return <FileText className="w-4 h-4 text-blue-500" />;
    case "CANDIDATE":
      return <UserCheck className="w-4 h-4 text-green-500" />;
    case "CALENDAR":
      return <Calendar className="w-4 h-4 text-purple-500" />;
    case "EMAIL":
      return <Mail className="w-4 h-4 text-orange-500" />;
    default:
      return <Bell className="w-4 h-4 text-gray-500" />;
  }
};

const getRelativeTime = (dateString, t) => {
  if (!dateString) return "";
  const date = new Date(dateString);
  const now = new Date();
  const diffInSeconds = Math.floor((now - date) / 1000);
  
  if (diffInSeconds < 60) return t("common.justNow");
  if (diffInSeconds < 3600) return `${Math.floor(diffInSeconds / 60)} ${t("common.minutesAgo")}`;
  if (diffInSeconds < 86400) return `${Math.floor(diffInSeconds / 3600)} ${t("common.hoursAgo")}`;
  if (diffInSeconds < 604800) return `${Math.floor(diffInSeconds / 86400)} ${t("common.daysAgo")}`;
  return date.toLocaleDateString("vi-VN");
};

export default function NotificationsDropdown() {
  const { t } = useTranslation();
  const { data: notifications = [], isLoading, refetch } = useNotifications();
  const markAsRead = useMarkNotificationAsRead();
  const markAllAsRead = useMarkAllNotificationsAsRead();
  const { isConnected } = useSocket();
  const unreadCount = notifications.filter((n) => !n.read).length;
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef(null);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  const toggleDropdown = () => {
    if (!isOpen) {
      refetch();
    }
    setIsOpen(!isOpen);
  };

  const handleNotificationClick = async (notification) => {
    setIsOpen(false);
  };

  const handleMarkAsRead = async (e, notificationId) => {
    e.stopPropagation();
    try {
      await markAsRead.mutateAsync(notificationId);
    } catch (error) {
      console.error("Failed to mark notification as read:", error);
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await markAllAsRead.mutateAsync();
    } catch (error) {
      console.error("Failed to mark all notifications as read:", error);
    }
  };

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        onClick={toggleDropdown}
        className="relative p-2 rounded-lg hover:bg-gray-100 focus:outline-none transition-colors duration-200"
        title={isConnected ? t("common.connectedRealtime") : t("common.notConnected")}
      >
        <Bell className="w-5 h-5 text-gray-700" />
        {unreadCount > 0 && (
          <span className="absolute top-0 right-0 bg-red-500 text-white text-[10px] font-bold rounded-full min-w-[16px] h-[16px] flex items-center justify-center px-1 shadow-sm">
            {unreadCount > 99 ? "99+" : unreadCount}
          </span>
        )}
      </button>
      {isOpen && (
        <div className="absolute right-0 mt-2 w-96 bg-white shadow-lg rounded-lg z-50 max-h-96 overflow-hidden border border-gray-200">
          <div className="px-4 py-3 bg-gray-50 border-b border-gray-200 rounded-t-lg">
            <div className="flex items-start justify-between gap-3">
              <div className="flex-1">
                <h3 className="text-base font-semibold text-gray-900">{t("common.notifications")}</h3>
                {unreadCount > 0 && (
                  <p className="text-sm text-gray-600 mt-1">{unreadCount} {t("common.unreadNotifications")}</p>
                )}
              </div>
              {unreadCount > 0 && (
                <button
                  onClick={handleMarkAllAsRead}
                  disabled={markAllAsRead.isPending}
                  className="p-1.5 rounded hover:bg-blue-100 transition-colors duration-150 disabled:opacity-50 disabled:cursor-not-allowed flex-shrink-0"
                  title={markAllAsRead.isPending ? t("common.processing") : t("common.markAllAsRead")}
                >
                  <CheckCheck className="w-4 h-4 text-blue-600" />
                </button>
              )}
            </div>
          </div>

          <div className="max-h-80 overflow-y-auto">
            {isLoading ? (
              <div className="px-4 py-6 text-center">
                <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-gray-900 mx-auto"></div>
                <p className="text-sm text-gray-500 mt-2">{t("loading")}</p>
              </div>
            ) : notifications.length === 0 ? (
              <div className="px-4 py-6 text-center">
                <Bell className="w-10 h-10 text-gray-300 mx-auto mb-2" />
                <p className="text-sm text-gray-500">{t("common.noNotifications")}</p>
              </div>
            ) : (
              <ul>
                {notifications.map((n) => (
                  <li
                    key={n.id}
                    onClick={() => handleNotificationClick(n)}
                    className={`px-4 py-2.5 hover:bg-gray-50 transition-colors duration-150 cursor-pointer border-b border-gray-100 last:border-b-0 ${!n.read ? "bg-blue-50/50" : ""}`}
                  >
                    <div className="flex items-start gap-3">
                      <div className="flex-shrink-0 mt-0.5">
                        {getNotificationIcon(n.type)}
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-start justify-between gap-2 mb-1">
                          <h4 className={`text-sm leading-5 ${!n.read ? "font-semibold text-gray-900" : "font-medium text-gray-700"}`}>
                            {n.title || n.message || t("common.noTitle")}
                          </h4>
                          {!n.read && (
                            <button
                              onClick={(e) => handleMarkAsRead(e, n.id)}
                              className="p-1 hover:bg-blue-100 rounded transition-colors flex-shrink-0"
                              title={t("common.markAsRead")}
                            >
                              <Check className="w-3.5 h-3.5 text-blue-600" />
                            </button>
                          )}
                        </div>
                        {n.message && n.title !== n.message && (
                          <p className="text-xs text-gray-600 mb-1.5 line-clamp-2 leading-relaxed">
                            {n.message}
                          </p>
                        )}
                        <span className="text-[11px] text-gray-400">
                          {getRelativeTime(n.createdAt, t)}
                        </span>
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
