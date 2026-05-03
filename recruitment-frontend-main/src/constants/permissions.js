// Permission constants for the application
export const PERMISSIONS = {
  // User Service - Permissions
  PERMISSIONS_READ: 'user-service:permissions:read',
  PERMISSIONS_CREATE: 'user-service:permissions:create',
  PERMISSIONS_UPDATE: 'user-service:permissions:update',
  PERMISSIONS_DELETE: 'user-service:permissions:delete',

  // User Service - Roles
  ROLES_READ: 'user-service:roles:read',
  ROLES_CREATE: 'user-service:roles:create',
  ROLES_UPDATE: 'user-service:roles:update',
  ROLES_DELETE: 'user-service:roles:delete',

  // User Service - Departments
  DEPARTMENTS_READ: 'user-service:departments:read',
  DEPARTMENTS_CREATE: 'user-service:departments:create',
  DEPARTMENTS_UPDATE: 'user-service:departments:update',
  DEPARTMENTS_DELETE: 'user-service:departments:delete',

  // User Service - Employees
  EMPLOYEES_READ: 'user-service:employees:read',
  EMPLOYEES_CREATE: 'user-service:employees:create',
  EMPLOYEES_UPDATE: 'user-service:employees:update',
  EMPLOYEES_DELETE: 'user-service:employees:delete',

  // User Service - Positions
  POSITIONS_READ: 'user-service:positions:read',
  POSITIONS_CREATE: 'user-service:positions:create',
  POSITIONS_UPDATE: 'user-service:positions:update',
  POSITIONS_DELETE: 'user-service:positions:delete',

  // User Service - Users
  USERS_READ: 'user-service:users:read',
  USERS_CREATE: 'user-service:users:create',
  USERS_UPDATE: 'user-service:users:update',
  USERS_DELETE: 'user-service:users:delete',

  // User Service - Workflows
  WORKFLOWS_READ: 'user-service:workflows:read',
  WORKFLOWS_CREATE: 'user-service:workflows:create',
  WORKFLOWS_UPDATE: 'user-service:workflows:update',
  WORKFLOWS_DELETE: 'user-service:workflows:delete',

  // Job Service - Job Positions
  JOB_POSITIONS_READ: 'job-service:job-positions:read',
  JOB_POSITIONS_CREATE: 'job-service:job-positions:create',
  JOB_POSITIONS_UPDATE: 'job-service:job-positions:update',
  JOB_POSITIONS_DELETE: 'job-service:job-positions:delete',
  JOB_POSITIONS_PUBLISH: 'job-service:job-positions:publish',
  JOB_POSITIONS_CLOSE: 'job-service:job-positions:close',
  JOB_POSITIONS_REOPEN: 'job-service:job-positions:reopen',

  // Job Service - Recruitment Requests
  RECRUITMENT_REQUESTS_READ: 'job-service:recruitment-requests:read',
  RECRUITMENT_REQUESTS_CREATE: 'job-service:recruitment-requests:create',
  RECRUITMENT_REQUESTS_UPDATE: 'job-service:recruitment-requests:update',
  RECRUITMENT_REQUESTS_DELETE: 'job-service:recruitment-requests:delete',
  RECRUITMENT_REQUESTS_APPROVE: 'job-service:recruitment-requests:approve',
  RECRUITMENT_REQUESTS_REJECT: 'job-service:recruitment-requests:reject',

  // Job Service - Skills & Categories
  JOB_SKILLS_READ: 'job-service:job-skills:read',
  JOB_CATEGORIES_READ: 'job-service:job-categories:read',

  // Candidate Service - Applications
  APPLICATIONS_READ: 'candidate-service:applications:read',
  APPLICATIONS_CREATE: 'candidate-service:applications:create',
  APPLICATIONS_UPDATE: 'candidate-service:applications:update',
  APPLICATIONS_DELETE: 'candidate-service:applications:delete',
  APPLICATIONS_STATUS: 'candidate-service:applications:status',
  APPLICATIONS_ACCEPT: 'candidate-service:applications:accept',
  APPLICATIONS_REJECT: 'candidate-service:applications:reject',

  // Candidate Service - Candidates
  CANDIDATES_READ: 'candidate-service:candidates:read',
  CANDIDATES_CREATE: 'candidate-service:candidates:create',
  CANDIDATES_UPDATE: 'candidate-service:candidates:update',
  CANDIDATES_DELETE: 'candidate-service:candidates:delete',
  CANDIDATES_CHANGE_STAGE: 'candidate-service:candidates:change-stage',

  // Candidate Service - Comments
  COMMENTS_READ: 'candidate-service:comments:read',
  COMMENTS_CREATE: 'candidate-service:comments:create',
  COMMENTS_UPDATE: 'candidate-service:comments:update',
  COMMENTS_DELETE: 'candidate-service:comments:delete',

  // Communications Service - Schedules
  SCHEDULES_READ: 'communications-service:schedules:read',
  SCHEDULES_CREATE: 'communications-service:schedules:create',
  SCHEDULES_UPDATE: 'communications-service:schedules:update',
  SCHEDULES_DELETE: 'communications-service:schedules:delete',
  SCHEDULES_UPDATE_STATUS: 'communications-service:schedules:update-status',
  SCHEDULES_CALENDAR: 'communications-service:schedules:calendar',
};

// Helper function to check if user has permission
export const hasPermission = (userPermissions, requiredPermission) => {
  if (!userPermissions || !Array.isArray(userPermissions)) return false;
  return userPermissions.some(
    (perm) => perm.name === requiredPermission && perm.active === true
  );
};

// Helper function to check if user has any of the required permissions
export const hasAnyPermission = (userPermissions, requiredPermissions) => {
  if (!userPermissions || !Array.isArray(userPermissions)) return false;
  return requiredPermissions.some((permission) =>
    hasPermission(userPermissions, permission)
  );
};

// Helper function to check if user has all required permissions
export const hasAllPermissions = (userPermissions, requiredPermissions) => {
  if (!userPermissions || !Array.isArray(userPermissions)) return false;
  return requiredPermissions.every((permission) =>
    hasPermission(userPermissions, permission)
  );
};
