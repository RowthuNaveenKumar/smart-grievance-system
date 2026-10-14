import { api } from "./api.js";

/**
 * Fetch all designations configured in the system.
 * Returns List<DesignationResponse>
 */
export const getAllDesignations = async () => {
  const res = await api.get("/admin/designations");
  return res.data;
};

/**
 * Fetch single designation by ID.
 * Returns DesignationResponse
 */
export const getDesignationById = async (id) => {
  const res = await api.get(`/admin/designations/${id}`);
  return res.data;
};

/**
 * Create a new dynamic designation.
 * @param {Object} data - { name: string, assignmentScope: 'GLOBAL' | 'DEPARTMENT' | 'DIVISION' | 'FLOOR' }
 * Returns DesignationResponse
 */
export const createDesignation = async (data) => {
  const res = await api.post("/admin/designations", data);
  return res.data;
};

/**
 * Update an existing dynamic designation.
 * Cannot be called for reserved platform system roles.
 * @param {number|string} id
 * @param {Object} data - { name: string, assignmentScope: string }
 * Returns DesignationResponse
 */
export const updateDesignation = async (id, data) => {
  const res = await api.put(`/admin/designations/${id}`, data);
  return res.data;
};

/**
 * Delete a designation.
 * Rejected with 400 if referenced by staff members or workflow steps.
 * @param {number|string} id
 */
export const deleteDesignation = async (id) => {
  const res = await api.delete(`/admin/designations/${id}`);
  return res.data;
};

/**
 * Fetch staff member's current designation assignments and admin status.
 * Returns StaffDesignationAssignmentResponse
 * @param {number|string} staffId
 */
export const getStaffDesignations = async (staffId) => {
  const res = await api.get(`/admin/designations/staff/${staffId}`);
  return res.data;
};

/**
 * Assign designations to staff member.
 * Preserves protected platform admin role identity if staff holds it.
 * @param {number|string} staffId
 * @param {Object} data - { designationIds: number[] }
 * Returns StaffDesignationAssignmentResponse
 */
export const assignStaffDesignations = async (staffId, data) => {
  const res = await api.put(`/admin/designations/staff/${staffId}`, data);
  return res.data;
};

/**
 * Remove a specific designation assignment from staff member.
 * Cannot remove reserved platform system role (ID 1).
 * @param {number|string} staffId
 * @param {number|string} designationId
 * Returns StaffDesignationAssignmentResponse
 */
export const removeStaffDesignation = async (staffId, designationId) => {
  const res = await api.delete(
    `/admin/designations/staff/${staffId}/${designationId}`
  );
  return res.data;
};
