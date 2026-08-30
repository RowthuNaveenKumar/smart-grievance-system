import { api } from "./api";

export const getAllAdminCategories = async () => {
  const res = await api.get("/admin/categories");
  return res.data;
};

export const getAdminCategoriesByDepartment = async (departmentId) => {
  const res = await api.get(`/admin/categories/by-department/${departmentId}`);
  return res.data;
};

export const getAdminCategoryById = async (id) => {
  const res = await api.get(`/admin/categories/${id}`);
  return res.data;
};

export const createAdminCategory = async (payload) => {
  const res = await api.post("/admin/categories", payload);
  return res.data;
};

export const updateAdminCategory = async (id, payload) => {
  const res = await api.put(`/admin/categories/${id}`, payload);
  return res.data;
};

export const updateCategoryStatus = async (id, active) => {
  const res = await api.patch(`/admin/categories/${id}/status`, { active });
  return res.data;
};
