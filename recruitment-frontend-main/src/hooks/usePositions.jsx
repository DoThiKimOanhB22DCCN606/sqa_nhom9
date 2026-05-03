import { useQuery } from "@tanstack/react-query";
import { userServices } from "../services/userServices";

export const positionKeys = {
  all: ["positions"],
  list: (filters) => [...positionKeys.all, "list", filters],
};

export const usePositions = (params = {}) => {
  return useQuery({
    queryKey: positionKeys.list(params),
    queryFn: async () => {
      const response = await userServices.getPositions(params);
      return response.data?.data || response.data || { meta: {}, result: [] };
    },
    staleTime: 5 * 60 * 1000,
  });
};

export const useAllPositions = () => {
  return useQuery({
    queryKey: positionKeys.all,
    queryFn: async () => {
      const response = await userServices.getPositions();
      const data = response.data?.data || response.data;
      return data?.result || data || [];
    },
    staleTime: 10 * 60 * 1000,
  });
};
