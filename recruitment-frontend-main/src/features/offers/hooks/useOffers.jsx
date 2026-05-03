import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { offerServices } from "../services/offerServices";
import { toast } from "react-toastify";

export const offerKeys = {
  all: ["offers"],
  list: (params) => ["offers", "list", params],
  detail: (id) => ["offers", "detail", id],
};

export const useOffers = (params = {}) => {
  return useQuery({
    queryKey: offerKeys.list(params),
    queryFn: async () => {
      const response = await offerServices.getOffers(params);
      return response.data;
    },
    staleTime: 5 * 60 * 1000,
  });
};

export const useOffer = (id, options = {}) => {
  return useQuery({
    queryKey: offerKeys.detail(id),
    queryFn: async () => {
      const response = await offerServices.getOfferById(id);
      return response.data;
    },
    enabled: !!id,
    staleTime: 5 * 60 * 1000,
    ...options,
  });
};

export const useUpdateOfferStatus = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, status }) => {
      const response = await offerServices.updateOfferStatus(id, status);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: offerKeys.all });
    },
    onError: (error) => {
      const errorMessage =
        error.response?.data?.message || "Không thể cập nhật offer";
      toast.error(errorMessage);
    },
  });
};

export const useCreateOffer = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (offerData) => {
      const response = await offerServices.createOffer(offerData);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: offerKeys.all });
    },
    onError: (error) => {
      const errorMessage =
        error.response?.data?.message || "Không thể tạo offer";
      toast.error(errorMessage);
    },
  });
};

export const useApproveOffer = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, action }) => {
      const response = await offerServices.approveOffer(id, action);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: offerKeys.all });
    },
    onError: (error) => {
      const errorMessage =
        error.response?.data?.message || "Không thể phê duyệt offer";
      toast.error(errorMessage);
    },
  });
};

export const useRejectOffer = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, notes }) => {
      const response = await offerServices.rejectOffer(id, notes);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: offerKeys.all });
    },
    onError: (error) => {
      const errorMessage =
        error.response?.data?.message || "Không thể từ chối offer";
      toast.error(errorMessage);
    },
  });
};
