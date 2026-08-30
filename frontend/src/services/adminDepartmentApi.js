import { api } from "./api";

export const getAllAdminDepartments = async () => {
  const res = await api.get("/admin/departments");
  return res.data;
};

export const getAdminDepartmentById = async (id) => {
  const res = await api.get(`/admin/departments/${id}`);
  return res.data;
};

export const createAdminDepartment = async (payload) => {
  const res = await api.post("/admin/departments", payload);
  return res.data;
};

export const updateAdminDepartment = async (id, payload) => {
  const res = await api.put(`/admin/departments/${id}`, payload);
  return res.data;
};

export const updateDepartmentStatus = async (id, active) => {
  const res = await api.patch(`/admin/departments/${id}/status`, { active });
  return res.data;
};

export const getPublicDepartments = async () => {
  const res = await api.get("/departments");
  return res.data;
};
