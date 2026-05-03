import api from "../../../config/axios";

export const calendarServices = {
  getCalendar: async (params = {}) => {
    const response = await api.get("/schedule-service/schedules", {
      params,
    });
    return response.data;
  },

  createSchedule: async (data) => {
    const response = await api.post("/schedule-service/schedules", data);
    return response.data;
  },

  updateSchedule: async (id, data) => {
    const response = await api.put(
      `/schedule-service/schedules/${id}`,
      data
    );
    return response.data;
  },
};
