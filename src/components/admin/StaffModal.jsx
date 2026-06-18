import { useEffect, useState } from "react";

import {
  createStaff,
  updateStaff,
  getDepartments,
  getDivisions,
  getFloors,
  getRoles,
} from "@/services/adminStaffApi";

import { toast } from "sonner";

export default function StaffModal({
  isOpen,
  onClose,
  onSuccess,
  editStaff = null,
}) {
  const isEdit = !!editStaff;

  const [departments, setDepartments] = useState([]);
  const [divisions, setDivisions] = useState([]);
  const [floors, setFloors] = useState([]);
  const [roles, setRoles] = useState([]);

  const [formData, setFormData] = useState({
    name: "",
    email: "",
    phone: "",
    departmentId: "",
    divisionId: "",
    floorId: "",
    roleId: "",
  });

  useEffect(() => {
    if (isOpen) {
      loadDropdowns();
    }
  }, [isOpen]);

  useEffect(() => {
    if (editStaff) {
      setFormData({
        name: editStaff.name || "",
        email: editStaff.email || "",
        phone: editStaff.phone || "",
        departmentId: editStaff.departmentId || "",
        divisionId: editStaff.divisionId || "",
        floorId: editStaff.floorId || "",
        roleId: editStaff.roleIds?.[0] || "",
      });
    } else {
      setFormData({
        name: "",
        email: "",
        phone: "",
        departmentId: "",
        divisionId: "",
        floorId: "",
        roleId: "",
      });
    }
  }, [editStaff]);

  const loadDropdowns = async () => {
    try {
      const [
        departmentData,
        divisionData,
        floorData,
        roleData,
      ] = await Promise.all([
        getDepartments(),
        getDivisions(),
        getFloors(),
        getRoles(),
      ]);

      setDepartments(departmentData);
      setDivisions(divisionData);
      setFloors(floorData);
      setRoles(roleData);
    } catch (error) {
      toast.error("Failed to load dropdowns");
    }
  };

  const handleChange = (e) => {
    setFormData((prev) => ({
      ...prev,
      [e.target.name]: e.target.value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    try {
      const payload = {
        name: formData.name,
        phone: formData.phone,
        departmentId: formData.departmentId
          ? Number(formData.departmentId)
          : null,
        divisionId: formData.divisionId
          ? Number(formData.divisionId)
          : null,
        floorId: formData.floorId
          ? Number(formData.floorId)
          : null,
        roleIds: formData.roleId
          ? [Number(formData.roleId)]
          : [],
      };

      if (isEdit) {
        await updateStaff(editStaff.staffId, payload);

        toast.success("Staff updated successfully");
      } else {
        await createStaff({
          ...payload,
          email: formData.email,
        });

        toast.success("Staff created successfully");
      }

      onSuccess();
      onClose();
    } catch (error) {
      toast.error(
        error.response?.data?.message ||
          (isEdit
            ? "Failed to update staff"
            : "Failed to create staff")
      );
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm">
      <div className="w-full max-w-3xl rounded-3xl border border-white/10 bg-slate-900 p-6">

        <h2 className="mb-6 text-2xl font-bold text-white">
          {isEdit ? "Edit Staff" : "Add Staff"}
        </h2>

        <form
          onSubmit={handleSubmit}
          className="grid grid-cols-2 gap-4"
        >
          <input
            name="name"
            placeholder="Name"
            value={formData.name}
            onChange={handleChange}
            className="rounded-xl bg-slate-800 p-3"
            required
          />

          <input
            name="email"
            placeholder="Email"
            value={formData.email}
            onChange={handleChange}
            disabled={isEdit}
            className="rounded-xl bg-slate-800 p-3 disabled:opacity-50"
            required
          />

          <input
            name="phone"
            placeholder="Phone"
            value={formData.phone}
            onChange={handleChange}
            className="rounded-xl bg-slate-800 p-3"
          />

          <select
            name="departmentId"
            value={formData.departmentId}
            onChange={handleChange}
            className="rounded-xl bg-slate-800 p-3"
          >
            <option value="">Department</option>

            {departments.map((dept) => (
              <option
                key={dept.departmentId}
                value={dept.departmentId}
              >
                {dept.name}
              </option>
            ))}
          </select>

          <select
            name="divisionId"
            value={formData.divisionId}
            onChange={handleChange}
            className="rounded-xl bg-slate-800 p-3"
          >
            <option value="">Division</option>

            {divisions.map((division) => (
              <option
                key={division.divisionId}
                value={division.divisionId}
              >
                {division.name}
              </option>
            ))}
          </select>

          <select
            name="floorId"
            value={formData.floorId}
            onChange={handleChange}
            className="rounded-xl bg-slate-800 p-3"
          >
            <option value="">Floor</option>

            {floors.map((floor) => (
              <option
                key={floor.floorId}
                value={floor.floorId}
              >
                {floor.floorNumber}
              </option>
            ))}
          </select>

          <select
            name="roleId"
            value={formData.roleId}
            onChange={handleChange}
            className="rounded-xl bg-slate-800 p-3 col-span-2"
            required
          >
            <option value="">Select Role</option>

            {roles.map((role) => (
              <option
                key={role.roleId}
                value={role.roleId}
              >
                {role.roleName}
              </option>
            ))}
          </select>

          <div className="col-span-2 flex justify-end gap-3 mt-4">
            <button
              type="button"
              onClick={onClose}
              className="rounded-xl bg-slate-700 px-5 py-2"
            >
              Cancel
            </button>

            <button
              type="submit"
              className="rounded-xl bg-indigo-600 px-5 py-2"
            >
              {isEdit
                ? "Update Staff"
                : "Create Staff"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}