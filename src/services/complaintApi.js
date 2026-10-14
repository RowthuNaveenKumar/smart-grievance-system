import { api } from "./api.js";

/**
 * Fetch paginated complaints for a student.
 * Endpoint: GET /complaints/student/{studentId}/page
 *
 * @param {number|string} studentId - Student ID
 * @param {object} [params] - Query parameters
 * @param {number} [params.page=0] - Zero-based page index
 * @param {number} [params.size=10] - Number of items per page
 * @param {string} [params.sortBy="createdAt"] - Field to sort by
 * @param {string} [params.direction="desc"] - Sort direction ('asc' or 'desc')
 * @returns {Promise<object>} Spring Page response: { content, totalElements, totalPages, number, size, ... }
 */
export const getStudentComplaintsPage = async (
  studentId,
  { page = 0, size = 10, sortBy = "createdAt", direction = "desc" } = {}
) => {
  const res = await api.get(`/complaints/student/${studentId}/page`, {
    params: { page, size, sortBy, direction },
  });
  return res.data;
};

/**
 * Fetch paginated complaints assigned to a staff member.
 * Endpoint: GET /complaints/staff/{staffId}/page
 *
 * @param {number|string} staffId - Staff ID
 * @param {object} [params] - Query parameters
 * @param {number} [params.page=0] - Zero-based page index
 * @param {number} [params.size=10] - Number of items per page
 * @param {string} [params.sortBy="createdAt"] - Field to sort by
 * @param {string} [params.direction="desc"] - Sort direction ('asc' or 'desc')
 * @returns {Promise<object>} Spring Page response: { content, totalElements, totalPages, number, size, ... }
 */
export const getStaffComplaintsPage = async (
  staffId,
  { page = 0, size = 10, sortBy = "createdAt", direction = "desc" } = {}
) => {
  const res = await api.get(`/complaints/staff/${staffId}/page`, {
    params: { page, size, sortBy, direction },
  });
  return res.data;
};

/**
 * Fetch summary/unpaginated complaints for the current student.
 * Preserves existing array-based statistics and counts.
 * Endpoint: GET /complaints/my
 *
 * @returns {Promise<Array>} Array of ComplaintResponse objects
 */
export const getMyComplaints = async () => {
  const res = await api.get("/complaints/my");
  return res.data;
};

/**
 * Fetch summary/unpaginated complaints assigned to the current staff member.
 * Preserves existing array-based statistics and badge counts.
 * Endpoint: GET /complaints/assigned
 *
 * @returns {Promise<Array>} Array of ComplaintResponse objects
 */
export const getAssignedComplaints = async () => {
  const res = await api.get("/complaints/assigned");
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
