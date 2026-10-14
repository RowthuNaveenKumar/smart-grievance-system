import { api } from "./api.js";

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

/**
 * Fetch paginated platform-wide complaints for Admin.
 * Endpoint: GET /complaints/page
 *
 * @param {object} [params] - Query parameters
 * @param {number} [params.page=0] - Zero-based page index
 * @param {number} [params.size=10] - Number of items per page
 * @param {string} [params.sortBy="createdAt"] - Field to sort by
 * @param {string} [params.direction="DESC"] - Sort direction ('ASC' or 'DESC')
 * @param {string} [params.status] - Optional status filter
 * @param {string} [params.priority] - Optional priority filter
 * @param {number|string} [params.departmentId] - Optional department ID filter
 * @returns {Promise<object>} Spring Page response: { content, totalElements, totalPages, number, size, ... }
 */
export const getAdminComplaintsPage = async ({
  page = 0,
  size = 10,
  sortBy = "createdAt",
  direction = "DESC",
  status,
  priority,
  departmentId,
} = {}) => {
  const params = { page, size, sortBy, direction };
  if (status && status !== "ALL") params.status = status;
  if (priority && priority !== "ALL") params.priority = priority;
  if (departmentId && departmentId !== "ALL") params.departmentId = Number(departmentId);

  const res = await api.get("/complaints/page", { params });
  return res.data;
};

/**
 * Fetch Admin dashboard aggregate statistics.
 * Endpoint: GET /admin/dashboard/stats
 *
 * @returns {Promise<object>} Dashboard stats: { totalStudents, totalStaff, totalComplaints, activeComplaints, resolvedComplaints, closedComplaints }
 */
export const getAdminDashboardStats = async () => {
  const res = await api.get("/admin/dashboard/stats");
  return res.data;
};
