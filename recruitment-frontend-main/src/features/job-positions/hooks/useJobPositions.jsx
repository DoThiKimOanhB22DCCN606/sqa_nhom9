import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { jobServices } from "../services/jobServices";
import { toast } from "react-toastify";

// Query Keys
export const jobPositionKeys = {
  all: ["job-positions"],
  list: (params) => ["job-positions", "list", params],
  detail: (id) => ["job-positions", "detail", id],
};

// Custom hook to fetch job positions with optional filters
export const useJobPositions = (params = {}) => {
  return useQuery({
    queryKey: jobPositionKeys.list(params),
    queryFn: async () => {
      const response = await jobServices.getJobPositions(params);
      return response.data;
    },
    staleTime: 5 * 60 * 1000, // Data is fresh for 5 minutes
  });
};

// Custom hook to fetch a single job position by id
export const useJobPosition = (id) => {
  return useQuery({
    queryKey: jobPositionKeys.detail(id),
    queryFn: async () => {
      const response = await jobServices.getJobPositionById(id);
      return response.data?.data || response.data;
    },
    enabled: !!id, // Only run if id exists
    staleTime: 5 * 60 * 1000,
  });
};

// Custom hook to create a job position
export const useCreateJobPosition = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data) => {
      const response = await jobServices.createJobPosition(data);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: jobPositionKeys.all });
    },
    onError: (error) => {
      const errorMessage =
        error.response?.data?.message || "Không thể tạo vị trí tuyển dụng";
      toast.error(errorMessage);
    },
  });
};

// Custom hook to update a job position
export const useUpdateJobPosition = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, data }) => {
      const response = await jobServices.updateJobPosition(id, data);
      return response.data;
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: jobPositionKeys.all });
      queryClient.invalidateQueries({
        queryKey: jobPositionKeys.detail(variables.id),
      });
    },
    onError: (error) => {
      const errorMessage =
        error.response?.data?.message || "Không thể cập nhật vị trí tuyển dụng";
      toast.error(errorMessage);
    },
  });
};

// Custom hook to delete a job position
export const useDeleteJobPosition = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id) => {
      const response = await jobServices.deleteJobPosition(id);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: jobPositionKeys.all });
    },
    onError: (error) => {
      const errorMessage =
        error.response?.data?.message || "Không thể xóa vị trí tuyển dụng";
      toast.error(errorMessage);
    },
  });
};
