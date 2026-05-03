import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { notificationServices } from "../../../services/notificationServices";

export const useNotifications = () => {
  return useQuery({
    queryKey: ["notifications"],
    queryFn: async () => {
      const res = await notificationServices.getNotifications();
      return res.data?.data?.result || [];
    },
  });
};

export const useMarkNotificationAsRead = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id) => notificationServices.markAsRead(id),
    onSuccess: (_, id) => {
      queryClient.setQueryData(["notifications"], (oldData) => {
        if (!oldData) return oldData;
        return oldData.map(notification => 
          notification.id === id 
            ? { ...notification, read: true, readAt: new Date().toISOString() }
            : notification
        );
      });
    },
  });
};

export const useMarkAllNotificationsAsRead = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => notificationServices.markAllAsRead(),
    onSuccess: () => {
      queryClient.setQueryData(["notifications"], (oldData) => {
        if (!oldData) return oldData;
        return oldData.map(notification => ({
          ...notification,
          read: true,
          readAt: new Date().toISOString()
        }));
      });
    },
  });
};

