import { useQuery } from "@tanstack/react-query";
import { userServices } from "../services/userServices";

export const departmentKeys = {
  all: ["departments"],
  list: (filters) => [...departmentKeys.all, "list", filters],
};

export const useDepartments = (params = {}) => {
  return useQuery({
    queryKey: departmentKeys.list(params),
    queryFn: async () => {
      const response = await userServices.getDepartments(params);
      return response.data?.data || response.data || { meta: {}, result: [] };
    },
    staleTime: 5 * 60 * 1000,
  });
};

export const useAllDepartments = () => {
  return useQuery({
    queryKey: departmentKeys.all,
    queryFn: async () => {
      const response = await userServices.getDepartments();
      const data = response.data?.data || response.data;
      return data?.result || data || [];
    },
    staleTime: 10 * 60 * 1000,
  });
};
