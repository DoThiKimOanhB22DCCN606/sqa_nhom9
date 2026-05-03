import { useAuth } from '../context/AuthContext';
import { hasPermission, hasAnyPermission, hasAllPermissions } from '../constants/permissions';
export const usePermission = () => {
  const { user } = useAuth();

  const userPermissions = user?.role?.permissions || [];

  const can = (permission) => {
    return hasPermission(userPermissions, permission);
  };

  const canAny = (permissions) => {
    return hasAnyPermission(userPermissions, permissions);
  };

  const canAll = (permissions) => {
    return hasAllPermissions(userPermissions, permissions);
  };

  return {
    can,
    canAny,
    canAll,
    permissions: userPermissions,
  };
};
