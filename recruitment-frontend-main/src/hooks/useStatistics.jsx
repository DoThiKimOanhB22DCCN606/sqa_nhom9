import { useQuery } from "@tanstack/react-query";
import { statisticsServices } from "../services/statisticsServices";

const statisticsKeys = {
  all: ["statistics"],
  summary: (period, dateRange) => [...statisticsKeys.all, "summary", period, dateRange],
  upcomingSchedules: () => [...statisticsKeys.all, "upcoming-schedules"],
  jobOpenings: () => [...statisticsKeys.all, "job-openings"],
};

export const useSummaryStatistics = (period = "WEEKLY", dateRange = null) => {
  return useQuery({
    queryKey: statisticsKeys.summary(period, dateRange),
    queryFn: async () => {
      const response = await statisticsServices.getSummary(period, dateRange);
      return response.data;
    },
    staleTime: 1000 * 60 * 5, // 5 minutes
    enabled: period !== "CUSTOM" || Boolean(dateRange?.from && dateRange?.to), // Only fetch if custom dates are provided
  });
};

export const useUpcomingSchedules = () => {
  return useQuery({
    queryKey: statisticsKeys.upcomingSchedules(),
    queryFn: async () => {
      const response = await statisticsServices.getUpcomingSchedules();
      return response.data;
    },
    staleTime: 1000 * 60 * 5, // 5 minutes
  });
};

export const useJobOpenings = () => {
  return useQuery({
    queryKey: statisticsKeys.jobOpenings(),
    queryFn: async () => {
      const response = await statisticsServices.getJobOpenings();
      return response.data;
    },
    staleTime: 1000 * 60 * 5, // 5 minutes
  });
};
