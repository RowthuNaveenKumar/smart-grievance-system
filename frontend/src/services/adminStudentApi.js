import { api } from "./api";

export const getAllStudents = async () => {
  const res = await api.get("/admin/students");
  return res.data;
};

export const getStudentById = async (id) => {
  const res = await api.get(`/admin/students/${id}`);
  return res.data;
};

export const updateStudent = async (id, data) => {
  const res = await api.put(`/admin/students/${id}`, data);
  return res.data;
};

export const disableStudent = async (id) => {
  const res = await api.patch(`/admin/students/${id}/disable`);
  return res.data;
};

export const createStudent = async (payload) => {
  const res = await api.post("/admin/students", payload);
  return res.data;
};

export const getDivisions = async () => {
  const res = await api.get("/divisions");
  return res.data;
};

export const getRooms = async () => {
  const res = await api.get("/rooms");
  return res.data;
};
