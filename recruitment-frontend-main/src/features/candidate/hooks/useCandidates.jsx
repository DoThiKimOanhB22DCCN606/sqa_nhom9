import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { candidateServices } from "../services/candidateServices";
import { toast } from "react-toastify";
import { useTranslation } from "react-i18next";

// Query Keys
export const candidateKeys = {
  all: ["candidates"],
  list: (params) => ["candidates", "list", params],
  detail: (id) => ["candidates", "detail", id],
};

// Custom hook to fetch candidates with optional filters
export const useCandidates = (params = {}) => {
  return useQuery({
    queryKey: candidateKeys.list(params),
    queryFn: async () => {
      const response = await candidateServices.getCandidates(params);
      return response.data;
    },
    staleTime: 5 * 60 * 1000, // Data is fresh for 5 minutes
  });
};

// Custom hook to fetch a single candidate by id
export const useCandidate = (id, options = {}) => {
  return useQuery({
    queryKey: candidateKeys.detail(id),
    queryFn: async () => {
      const response = await candidateServices.getCandidateById(id);
      return response.data;
    },
    enabled: !!id, // Only run if id exists
    staleTime: 5 * 60 * 1000,
    ...options, // Allow passing additional options
  });
};

// Custom hook to update candidate status
export const useUpdateCandidateStatus = () => {
  const queryClient = useQueryClient();
  const { t } = useTranslation();

  return useMutation({
    mutationFn: async ({ id, status }) => {
      const response = await candidateServices.updateCandidateStatus(
        id,
        status
      );
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: candidateKeys.all });
    },
    onError: (error) => {
      const errorMessage =
        error.response?.data?.message || t("candidates.updateStatusError");
      toast.error(errorMessage);
    },
  });
};

// Custom hook to create a new candidate
export const useCreateCandidate = () => {
  const queryClient = useQueryClient();
  const { t } = useTranslation();

  return useMutation({
    mutationFn: async (data) => {
      const response = await candidateServices.newCandidate(data);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: candidateKeys.all });
    },
    onError: (error) => {
      const errorMessage =
        error.response?.data?.message || t("candidates.addCandidateError");
      toast.error(errorMessage);
    },
  });
};

// Custom hook to delete a candidate
export const useDeleteCandidate = () => {
  const queryClient = useQueryClient();
  const { t } = useTranslation();

  return useMutation({
    mutationFn: async (id) => {
      const response = await candidateServices.deleteCandidate(id);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: candidateKeys.all });
    },
    onError: (error) => {
      const errorMessage =
        error.response?.data?.message || t("candidates.deleteCandidateError");
      toast.error(errorMessage);
    },
  });
};

// Custom hook to add a comment to an application
export const useAddCandidateComment = () => {
  const queryClient = useQueryClient();
  const { t } = useTranslation();

  return useMutation({
    mutationFn: async ({ applicationId, content }) => {
      const payload = { applicationId, content };
      const response = await candidateServices.commentCandidate(
        applicationId,
        payload
      );
      return response.data;
    },
    onSuccess: (_data, variables) => {
      // Refresh the specific candidate detail
      queryClient.invalidateQueries({
        queryKey: candidateKeys.detail(variables.applicationId),
      });
    },
    onError: (error) => {
      const errorMessage =
        error.response?.data?.message || t("candidates.submitFeedbackError");
      toast.error(errorMessage);
    },
  });
};

// Custom hook to change candidate stage (for drag and drop)
export const useChangeStageCandidate = () => {
  const queryClient = useQueryClient();
  const { t } = useTranslation();

  return useMutation({
    mutationFn: async ({ id, stage }) => {
      const response = await candidateServices.changeStageCandidate(id, stage);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: candidateKeys.all });
    },
    onError: (error) => {
      const errorMessage =
        error.response?.data?.message || t("candidates.updateStatusError");
      toast.error(errorMessage);
    },
  });
};
