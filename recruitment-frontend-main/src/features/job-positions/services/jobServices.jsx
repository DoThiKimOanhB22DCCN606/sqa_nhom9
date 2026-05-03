import api from "../../../config/axios";

export const jobServices = {
  getJobPositions: async (params = {}) => {
    return api.get("/job-service/job-positions", { params });
  },

  getJobPositionById: async (id) => {
    return api.get(`/job-service/job-positions/${id}`);
  },

  createJobPosition: async (data) => {
    return api.post("/job-service/job-positions", data);
  },

  updateJobPosition: async (id, data) => {
    return api.put(`/job-service/job-positions/${id}`, data);
  },

  deleteJobPosition: async (id) => {
    return api.delete(`/job-service/job-positions/${id}`);
  },

  publishJobPosition: async (id) => {
    return api.post(`/job-service/job-positions/${id}/publish`);
  },

  closeJobPosition: async (id) => {
    return api.post(`/job-service/job-positions/${id}/close`);
  },

  reopenJobPosition: async (id) => {
    return api.post(`/job-service/job-positions/${id}/reopen`);
  },
};
