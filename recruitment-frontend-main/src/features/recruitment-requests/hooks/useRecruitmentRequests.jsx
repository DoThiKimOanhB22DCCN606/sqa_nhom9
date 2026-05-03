import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { reqServices } from "../services/reqServices";
import { toast } from "react-toastify";
import i18n from "../../../config/translation";

export const recruitmentRequestKeys = {
  all: ["recruitment-requests"],
  list: (params) => ["recruitment-requests", "list", params],
};

export const useRecruitmentRequests = (params = {}) => {
  return useQuery({
    queryKey: recruitmentRequestKeys.list(params),
    queryFn: async () => {
      const response = await reqServices.getRequests(params);
      return response.data;
    },
    staleTime: 5 * 60 * 1000,
  });
};

export const useRecruitmentRequest = (id) => {
  return useQuery({
    queryKey: [...recruitmentRequestKeys.all, id],
    queryFn: async () => {
      const response = await reqServices.getRequestById(id);
      return response.data?.data || response.data;
    },
    enabled: !!id,
    staleTime: 5 * 60 * 1000,
  });
};

export const useCreateRecruitmentRequest = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data) => {
      const response = await reqServices.createRequest(data);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: recruitmentRequestKeys.all });
    },
    onError: (error) => {
      const errorMessage =
        error.response?.data?.message || i18n.t("recruitmentRequests.errorCreate");
      toast.error(errorMessage);
    },
  });
};

export const useUpdateRecruitmentRequest = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, data }) => {
      const response = await reqServices.updateRequest(id, data);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: recruitmentRequestKeys.all });
    },
    onError: (error) => {
      const errorMessage =
        error.response?.data?.message || i18n.t("recruitmentRequests.errorUpdate");
      toast.error(errorMessage);
    },
  });
};

export const useDeleteRecruitmentRequest = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id) => {
      const response = await reqServices.deleteRequest(id);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: recruitmentRequestKeys.all });
    },
    onError: (error) => {
      const errorMessage =
        error.response?.data?.message || i18n.t("recruitmentRequests.errorDelete");
      toast.error(errorMessage);
    },
  });
};

export const useApproveRecruitmentRequest = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, data }) => {
      const response = await reqServices.approveRequest(id, data);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: recruitmentRequestKeys.all });
    },
    onError: (error) => {
      const errorMessage =
        error.response?.data?.message || i18n.t("recruitmentRequests.errorApprove");
      toast.error(errorMessage);
    },
  });
};

export const useRejectRecruitmentRequest = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, data }) => {
      const response = await reqServices.rejectRequest(id, data);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: recruitmentRequestKeys.all });
    },
    onError: (error) => {
      const errorMessage =
        error.response?.data?.message || i18n.t("recruitmentRequests.errorReject");
      toast.error(errorMessage);
    },
  });
};

export const useSubmitRecruitmentRequest = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, data }) => {
      const response = await reqServices.submitRequest(id, data);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: recruitmentRequestKeys.all });
    },
    onError: (error) => {
      const errorMessage =
        error.response?.data?.message || i18n.t("recruitmentRequests.errorSubmit");
      toast.error(errorMessage);
    },
  });
};

export const useReturnRecruitmentRequest = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, data }) => {
      const response = await reqServices.returnRequest(id, data);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: recruitmentRequestKeys.all });
    },
    onError: (error) => {
      const errorMessage =
        error.response?.data?.message || i18n.t("recruitmentRequests.errorReturn");
      toast.error(errorMessage);
    },
  });
};

export const useCancelRecruitmentRequest = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, data }) => {
      const response = await reqServices.cancelRequest(id, data);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: recruitmentRequestKeys.all });
    },
    onError: (error) => {
      const errorMessage =
        error.response?.data?.message || i18n.t("recruitmentRequests.errorCancel");
      toast.error(errorMessage);
    },
  });
};

export const useWithdrawRecruitmentRequest = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, data }) => {
      const response = await reqServices.withdrawRequest(id, data);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: recruitmentRequestKeys.all });
    },
    onError: (error) => {
      const errorMessage =
        error.response?.data?.message || i18n.t("recruitmentRequests.errorWithdraw");
      toast.error(errorMessage);
    },
  });
};

