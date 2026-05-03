import api from "../config/axios";

const base = "/user-service/employees";

export const employeeServices = {
  getEmployees: (params = {}) => api.get(base, { params }),
  getEmployee: (id) => api.get(`${base}/${id}`),
  createEmployee: (data) => api.post(base, data),
  updateEmployee: (id, data) => api.put(`${base}/${id}`, data),
  deleteEmployee: (id) => api.delete(`${base}/${id}`),
};
