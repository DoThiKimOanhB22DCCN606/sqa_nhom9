import api from "../config/axios";

export const workflowServices = {
  getWorkflows: async (params = {}) => {
    return await api.get("/workflow-service/workflows", { params });
  },

  getWorkflow: async (id) => {
    return await api.get(`/workflow-service/workflows/${id}`);
  },

  createWorkflow: async (data) => {
    return await api.post("/workflow-service/workflows", data);
  },

  updateWorkflow: async (id, data) => {
    return await api.put(`/workflow-service/workflows/${id}`, data);
  },

  deleteWorkflow: async (id) => {
    return await api.delete(`/workflow-service/workflows/${id}`);
  },
};
