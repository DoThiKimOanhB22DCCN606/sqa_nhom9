import React from 'react';
import { usePermission } from '../hooks/usePermission';

/**
 * Component for permission-based rendering
 * @param {Object} props
 * @param {React.ReactNode} props.children - Content to render if permission check passes
 * @param {string} [props.permission] - Single permission to check
 * @param {string[]} [props.permissions] - Multiple permissions to check
 * @param {boolean} [props.requireAll=false] - If true, requires all permissions; if false, requires any permission
 * @param {React.ReactNode} [props.fallback=null] - Content to render if permission check fails
 */
const Can = ({ 
  children, 
  permission, 
  permissions, 
  requireAll = false, 
  fallback = null 
}) => {
  const { can, canAny, canAll } = usePermission();

  let isAllowed = false;

  if (permission) {
    // Check single permission
    isAllowed = can(permission);
  } else if (permissions && permissions.length > 0) {
    // Check multiple permissions
    isAllowed = requireAll ? canAll(permissions) : canAny(permissions);
  } else {
    // No permissions specified, deny access
    return fallback;
  }

  return isAllowed ? <>{children}</> : fallback;
};

export default Can;