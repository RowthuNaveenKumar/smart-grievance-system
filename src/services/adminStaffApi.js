import { api } from "./api";

export const getAllStaff = async () => {
  const res = await api.get("/admin/staff");
  return res.data;
};

export const getStaffById = async (id) => {
  const res = await api.get(`/admin/staff/${id}`);
  return res.data;
};

export const createStaff = async (data) => {
  const res = await api.post("/admin/staff", data);
  return res.data;
};

export const updateStaff = async (id, data) => {
  const res = await api.put(`/admin/staff/${id}`, data);
  return res.data;
};

export const disableStaff = async (id) => {
  const res = await api.patch(`/admin/staff/${id}/disable`);
  return res.data;
};

export const getDepartments = async () => {
  const res = await api.get("/departments");
  return res.data;
};

export const getDivisions = async () => {
  const res = await api.get("/divisions");
  return res.data;
};

export const getFloors = async () => {
  const res = await api.get("/floors");
  return res.data;
};

export const getRoles = async () => {
  const res = await api.get("/roles/staff");
  return res.data;
};