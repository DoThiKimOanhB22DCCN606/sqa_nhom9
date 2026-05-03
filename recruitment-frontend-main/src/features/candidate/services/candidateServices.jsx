import api from "../../../config/axios";

export const candidateServices = {
  getCandidates: async (params = {}) => {
    return api.get("/candidate-service/candidates", { params });
  },

  getCandidateById: async (id) => {
    return api.get(`/candidate-service/candidates/${id}`);
  },

  newCandidate: async (data) => {
    return api.post("/candidate-service/candidates", data);
  },

  updateCandidateStatus: async (id, status) => {
    return api.put(`/candidate-service/candidates/status/${id}?status=${status}`);
  },

  deleteCandidate: async (id) => {
    return api.delete(`/candidate-service/candidates/${id}`);
  },

  commentCandidate: async (id, data) => {
    return api.post(`/candidate-service/comments`, data);
  },

  changeStageCandidate: async (id, stage) => {
    return api.put(`/candidate-service/candidates/status/${id}?status=${stage}`);
  },
};
