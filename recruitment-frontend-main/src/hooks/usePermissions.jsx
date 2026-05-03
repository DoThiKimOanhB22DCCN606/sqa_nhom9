import { useQuery } from "@tanstack/react-query";
import { userServices } from "../services/userServices";

export const permissionKeys = {
  all: ["permissions"],
};

export const useAllPermissions = (params = {}) => {
  return useQuery({
    queryKey: permissionKeys.all,
    queryFn: async () => {
      const response = await userServices.getPermissions(params);
      const data = response.data?.data ?? response.data;
      return data || [];
    },
    staleTime: 10 * 60 * 1000,
  });
};
