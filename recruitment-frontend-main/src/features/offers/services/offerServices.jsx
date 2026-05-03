import api from "../../../config/axios";

export const offerServices = {
  getOffers: async (params = {}) => {
    return api.get("/job-service/offers", { params });
  },

  getOfferById: async (id) => {
    return api.get(`/compensation-service/offers/${id}`);
  },

  updateOfferStatus: async (id, status) => {
    return api.patch(`/compensation-service/offers/${id}/status`, { status });
  },

  createOffer: async (offerData) => {
    return api.post("/compensation-service/offers", offerData);
  },

  approveOffer: async (id, action) => {
    return api.post(`/compensation-service/offers/${id}/approve`, { action });
  },

  rejectOffer: async (id, notes) => {
    return api.post(`/compensation-service/offers/${id}/reject`, { notes });
  },
};
