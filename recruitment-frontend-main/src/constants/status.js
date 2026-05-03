import i18n from "../config/translation";

export const STATUS_CONFIG = {
  // Candidate Application Statuses
  SUBMITTED: {
    label: () => i18n.t("statuses.submitted"),
    bgColor: "bg-blue-100",
    textColor: "text-blue-800",
    borderColor: "border-blue-300",
  },
  REVIEWING: {
    label: () => i18n.t("statuses.reviewing"),
    bgColor: "bg-indigo-100",
    textColor: "text-indigo-800",
    borderColor: "border-indigo-300",
  },
  INTERVIEW: {
    label: () => i18n.t("statuses.interview"),
    bgColor: "bg-yellow-100",
    textColor: "text-yellow-800",
    borderColor: "border-yellow-300",
  },
  OFFER: {
    label: () => i18n.t("statuses.offer"),
    bgColor: "bg-purple-100",
    textColor: "text-purple-800",
    borderColor: "border-purple-300",
  },
  HIRED: {
    label: () => i18n.t("statuses.hired"),
    bgColor: "bg-green-100",
    textColor: "text-green-800",
    borderColor: "border-green-300",
  },
  ARCHIVED: {
    label: () => i18n.t("statuses.archived"),
    bgColor: "bg-gray-100",
    textColor: "text-gray-800",
    borderColor: "border-gray-300",
  },
  REJECTED: {
    label: () => i18n.t("statuses.rejected"),
    bgColor: "bg-red-100",
    textColor: "text-red-800",
    borderColor: "border-red-300",
  },

  // Job Position Statuses
  DRAFT: {
    label: () => i18n.t("statuses.draft"),
    bgColor: "bg-gray-100",
    textColor: "text-gray-800",
    borderColor: "border-gray-300",
  },
  PUBLISHED: {
    label: () => i18n.t("statuses.published"),
    bgColor: "bg-green-100",
    textColor: "text-green-800",
    borderColor: "border-green-300",
  },
  CLOSED: {
    label: () => i18n.t("statuses.closed"),
    bgColor: "bg-red-100",
    textColor: "text-red-800",
    borderColor: "border-red-300",
  },

  // Offer Statuses
  PENDING: {
    label: () => i18n.t("statuses.pending"),
    bgColor: "bg-yellow-100",
    textColor: "text-yellow-800",
    borderColor: "border-yellow-300",
  },
  RETURNED: {
    label: () => i18n.t("statuses.returned"),
    bgColor: "bg-orange-100",
    textColor: "text-orange-800",
    borderColor: "border-orange-300",
  },
  WITHDRAWN: {
    label: () => i18n.t("statuses.withdrawn"),
    bgColor: "bg-gray-100",
    textColor: "text-gray-800",
    borderColor: "border-gray-300",
  },
  APPROVED: {
    label: () => i18n.t("statuses.approved"),
    bgColor: "bg-green-100",
    textColor: "text-green-800",
    borderColor: "border-green-300",
  },
  CANCELLED: {
    label: () => i18n.t("statuses.cancelled"),
    bgColor: "bg-gray-100",
    textColor: "text-gray-800",
    borderColor: "border-gray-300",
  },

  // Recruitment Request Statuses (includes SUBMITTED, PENDING, RETURNED, WITHDRAWN, APPROVED, REJECTED, CANCELLED from above)
  COMPLETED: {
    label: () => i18n.t("statuses.completed"),
    bgColor: "bg-green-100",
    textColor: "text-green-800",
    borderColor: "border-green-300",
  },

  // Approval Statuses (uses PENDING, APPROVED, REJECTED, CANCELLED, RETURNED from above)
};

export const getStatusConfig = (status) => {
  const config = STATUS_CONFIG[status];
  if (!config) {
    return {
      label: status,
      bgColor: "bg-gray-200",
      textColor: "text-gray-900",
      borderColor: "border-gray-400",
    };
  }
  
  // Call label function to get translated text
  return {
    ...config,
    label: typeof config.label === 'function' ? config.label() : config.label,
  };
};
