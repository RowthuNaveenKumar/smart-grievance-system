import { api } from "./api";

export const getAllComplaints = async () => {
  const res = await api.get("/complaints");
  return res.data;
};

export const getComplaintsByStatus = async (status) => {
  const res = await api.get(`/complaints/status/${status}`);
  return res.data;
};

export const getComplaintsByPriority = async (priority) => {
  const res = await api.get(`/complaints/priority/${priority}`);
  return res.data;
};

export const getComplaintsByDepartment = async (departmentId) => {
  const res = await api.get(`/complaints/department/${departmentId}`);
  return res.data;
};

export const overrideDepartment = async (complaintId, data) => {
  const res = await api.patch(`/complaints/${complaintId}/override-department`, data);
  return res.data;
};

export const reassignStaff = async (complaintId, data) => {
  const res = await api.patch(`/complaints/${complaintId}/reassign-staff`, data);
  return res.data;
};

export const getStaffByDepartment = async (departmentId) => {
  const res = await api.get(`/admin/staff/by-department/${departmentId}`);
  return res.data;
};

export const getDepartments = async () => {
  const res = await api.get("/departments");
  return res.data;
};
