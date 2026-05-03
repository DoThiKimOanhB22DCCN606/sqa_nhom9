import React from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

const RoleBasedGuard = ({ children, requiredRoles = [], requiredDepartmentIds = [], exemptRoles = [] }) => {
  const { user } = useAuth();

  // If no roles and no departments required, allow access
  if ((!requiredRoles || requiredRoles.length === 0) && (!requiredDepartmentIds || requiredDepartmentIds.length === 0)) {
    return children;
  }

  const userRole = user?.role?.name;
  const userDepartmentId = user?.department?.id;

  // Check if user has exempt role (e.g., ADMIN, CEO don't need department check)
  const isExempt = exemptRoles.length > 0 && exemptRoles.includes(userRole);

  // Check role access
  const hasRoleAccess = requiredRoles.length === 0 || requiredRoles.includes(userRole);
  
  // Check department access (skip if user is exempt)
  const hasDepartmentAccess = isExempt || requiredDepartmentIds.length === 0 || requiredDepartmentIds.includes(userDepartmentId);

  // User must satisfy both conditions (if specified)
  const hasAccess = hasRoleAccess && hasDepartmentAccess;

  // If user doesn't have required access, redirect to home
  if (!hasAccess) {
    return <Navigate to="/" replace />;
  }

  return children;
};

export default RoleBasedGuard;
