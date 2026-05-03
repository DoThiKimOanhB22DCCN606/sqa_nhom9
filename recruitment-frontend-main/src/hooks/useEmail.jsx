import { useMutation, useQuery } from "@tanstack/react-query";
import { emailServices } from "../services/emailServices";
import { toast } from "react-toastify";
import { useTranslation } from "react-i18next";

export const useSendEmail = () => {
  const { t } = useTranslation();

  return useMutation({
    mutationFn: async (data) => {
      const response = await emailServices.sendEmail(data);
      return response.data;
    },
    onSuccess: () => {
    },
    onError: (error) => {
      const message = error.response?.data?.message || t("toasts.errorSendEmail");
      toast.error(message);
    },
  });
};

export const useInboxEmails = () => {
  return useQuery({
    queryKey: ["emails", "inbox"],
    queryFn: async () => {
      const response = await emailServices.getInbox();
      // Support different API formats: data.data.result or data.result or array
      const payload = response?.data;
      const items = Array.isArray(payload)
        ? payload
        : Array.isArray(payload?.data?.result)
        ? payload.data.result
        : Array.isArray(payload?.result)
        ? payload.result
        : [];
      return items;
    },
  });
};

export const useSentEmails = () => {
  return useQuery({
    queryKey: ["emails", "sent"],
    queryFn: async () => {
      const response = await emailServices.getSent();
      const payload = response?.data;
      const items = Array.isArray(payload)
        ? payload
        : Array.isArray(payload?.data?.result)
        ? payload.data.result
        : Array.isArray(payload?.result)
        ? payload.result
        : [];
      return items;
    },
  });
};
