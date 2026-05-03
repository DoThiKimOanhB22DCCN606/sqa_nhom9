import api from "../config/axios";

export const statisticsServices = {
  getSummary: async (period = "WEEKLY", dateRange = null) => {
    const params = { period };
    if (dateRange && period === "CUSTOM") {
      params.from = dateRange.from;
      params.to = dateRange.to;
    }
    return api.get("/statistics-service/statistics/summary", { params });
  },

  getUpcomingSchedules: async () => {
    return api.get("/statistics-service/statistics/upcoming-schedules");
  },

  getJobOpenings: async () => {
    return api.get("/statistics-service/statistics/job-openings");
  },
};
