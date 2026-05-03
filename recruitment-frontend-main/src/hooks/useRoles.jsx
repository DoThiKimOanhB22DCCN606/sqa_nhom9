import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { userServices } from "../services/userServices";
import { toast } from "react-toastify";

export const roleKeys = {
  all: ["roles"],
  list: (params) => [...roleKeys.all, "list", params],
  detail: (id) => [...roleKeys.all, "detail", id],
};

export const useAllRoles = (params = {}) => {
  return useQuery({
    queryKey: roleKeys.all,
    queryFn: async () => {
      const response = await userServices.getRoles(params);
      const data = response.data?.data || response.data;
      return data?.result || data || [];
    },
    staleTime: 10 * 60 * 1000,
  });
};

export const useRole = (id, options = {}) => {
  return useQuery({
    queryKey: roleKeys.detail(id),
    queryFn: async () => {
      const response = await userServices.getRole(id);
      return response.data?.data || response.data;
    },
    enabled: !!id && (options.enabled ?? true),
    staleTime: 5 * 60 * 1000,
  });
};

export const useUpdateRolePermissions = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, permissionIds }) => {
      const response = await userServices.updateRolePermissions(id, permissionIds);
      return response.data;
    },
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({ queryKey: roleKeys.detail(variables?.id) });
      qc.invalidateQueries({ queryKey: roleKeys.all });
    },
    onError: (error) => {
      const message = error.response?.data?.message || t("toasts.errorUpdatePermissions");
      toast.error(message);
    },
  });
};

export const useCreateRole = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (data) => {
      const res = await userServices.createRole(data);
      return res.data;
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: roleKeys.all });
    },
    onError: (error) => {
      const message = error.response?.data?.message || t("toasts.errorCreateRole");
      toast.error(message);
    },
  });
};

export const useUpdateRole = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, data }) => {
      const res = await userServices.updateRole(id, data);
      return res.data;
    },
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({ queryKey: roleKeys.detail(variables?.id) });
      qc.invalidateQueries({ queryKey: roleKeys.all });
    },
    onError: (error) => {
      const message = error.response?.data?.message || t("toasts.errorUpdateRole");
      toast.error(message);
    },
  });
};
