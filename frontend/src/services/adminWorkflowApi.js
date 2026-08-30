import { api } from "./api";

export const getWorkflowsByDepartment = async (departmentId) => {
  const res = await api.get(`/admin/workflows/department/${departmentId}`);
  return res.data;
};

export const getWorkflowById = async (id) => {
  const res = await api.get(`/admin/workflows/${id}`);
  return res.data;
};

export const createWorkflowVersion = async (departmentId, payload = {}) => {
  const res = await api.post(`/admin/workflows/department/${departmentId}`, payload);
  return res.data;
};

export const addWorkflowStep = async (workflowId, payload) => {
  const res = await api.post(`/admin/workflows/${workflowId}/steps`, payload);
  return res.data;
};

export const updateWorkflowStep = async (stepId, payload) => {
  const res = await api.put(`/admin/workflows/steps/${stepId}`, payload);
  return res.data;
};

export const deleteWorkflowStep = async (stepId) => {
  const res = await api.delete(`/admin/workflows/steps/${stepId}`);
  return res.data;
};

export const activateWorkflow = async (workflowId) => {
  const res = await api.post(`/admin/workflows/${workflowId}/activate`);
  return res.data;
};

export const getAllRoles = async () => {
  const res = await api.get("/admin/workflows/roles");
  return res.data;
};
