import api from "../config/axios";

export const notificationServices = {
  getNotifications: async (params = {}) => {
    return await api.get("/notification-service/notifications", { params });
  },

  getNotification: async (id) => {
    return await api.get(`/notification-service/notifications/${id}`);
  },

  markAsRead: async (id) => {
    return await api.put(`/notification-service/notifications/${id}/read`);
  },

  markAllAsRead: async () => {
    return await api.put("/notification-service/notifications/read-all");
  },

  deleteNotification: async (id) => {
    return await api.delete(`/notification-service/notifications/${id}`);
  },
};