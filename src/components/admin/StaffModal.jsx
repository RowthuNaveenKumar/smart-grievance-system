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
import { X, Shield, Building, Layers, Home } from "lucide-react";

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
  const [submitting, setSubmitting] = useState(false);

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
        departmentId:
          editStaff.departmentId != null ? String(editStaff.departmentId) : "",
        divisionId:
          editStaff.divisionId != null ? String(editStaff.divisionId) : "",
        floorId:
          editStaff.floorId != null ? String(editStaff.floorId) : "",
        roleId:
          editStaff.roleIds?.[0] != null ? String(editStaff.roleIds[0]) : "",
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
  }, [editStaff, isOpen]);

  const loadDropdowns = async () => {
    try {
      const [departmentData, divisionData, floorData, roleData] =
        await Promise.all([
          getDepartments(),
          getDivisions(),
          getFloors(),
          getRoles(),
        ]);

      setDepartments(departmentData || []);
      setDivisions(divisionData || []);
      setFloors(floorData || []);
      setRoles(roleData || []);
    } catch (error) {
      toast.error("Failed to load organizational options");
    }
  };

  const selectedRole = roles.find(
    (r) => String(r.roleId) === String(formData.roleId)
  );

  const roleName = selectedRole?.roleName?.toUpperCase() || "";
  const roleScope =
    selectedRole?.assignmentScope ||
    (roleName === "HOD"
      ? "DEPARTMENT"
      : roleName === "MFT"
      ? "DIVISION"
      : roleName === "WARDEN"
      ? "FLOOR"
      : roleName === "SYSTEM COORDINATOR" ||
        roleName === "DEAN" ||
        roleName === "ADMIN"
      ? "GLOBAL"
      : null);

  const isDepartmentRequired =
    roleScope === "DEPARTMENT" || roleScope === "DIVISION";
  const isDivisionRequired = roleScope === "DIVISION";
  const isFloorRequired = roleScope === "FLOOR";

  // Visibility: Hide irrelevant selectors or present them as clearly optional.
  // Never clear existing assignments merely because a field is hidden.
  const showDivision =
    isDivisionRequired ||
    roleScope === "DEPARTMENT" ||
    (!roleScope && !["SYSTEM COORDINATOR", "DEAN", "ADMIN"].includes(roleName)) ||
    (isEdit && Boolean(formData.divisionId));

  const showFloor =
    isFloorRequired ||
    (!roleScope &&
      !["SYSTEM COORDINATOR", "DEAN", "HOD", "MFT", "ADMIN"].includes(
        roleName
      )) ||
    (isEdit && Boolean(formData.floorId));

  // Dependent dropdown: Filter divisions by selected department
  const filteredDivisions = formData.departmentId
    ? divisions.filter(
        (d) =>
          !d.departmentId ||
          String(d.departmentId) === String(formData.departmentId)
      )
    : divisions;

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleDepartmentChange = (e) => {
    const newDeptId = e.target.value;
    setFormData((prev) => {
      const next = { ...prev, departmentId: newDeptId };
      // If department is cleared or changed, ensure division belongs to new department
      if (prev.divisionId) {
        if (!newDeptId) {
          next.divisionId = "";
        } else {
          const selectedDiv = divisions.find(
            (d) => String(d.divisionId) === String(prev.divisionId)
          );
          if (
            selectedDiv?.departmentId &&
            String(selectedDiv.departmentId) !== String(newDeptId)
          ) {
            next.divisionId = "";
          }
        }
      }
      return next;
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!formData.name.trim()) {
      toast.error("Name is required");
      return;
    }

    if (!isEdit && !formData.email.trim()) {
      toast.error("Email is required");
      return;
    }

    if (!formData.roleId) {
      toast.error("Please select a designation");
      return;
    }

    // Role scope constraints
    if (isDepartmentRequired && !formData.departmentId) {
      toast.error(
        `Department is required for designation '${selectedRole?.roleName}'`
      );
      return;
    }

    if (isDivisionRequired && !formData.divisionId) {
      toast.error(
        `Academic division is required for designation '${selectedRole?.roleName}'`
      );
      return;
    }

    if (isFloorRequired && !formData.floorId) {
      toast.error(
        `Hostel floor is required for designation '${selectedRole?.roleName}'`
      );
      return;
    }

    if (formData.divisionId && !formData.departmentId) {
      toast.error(
        "Cannot assign academic division without selecting its parent department"
      );
      return;
    }

    setSubmitting(true);
    try {
      const payload = {
        name: formData.name.trim(),
        phone: formData.phone.trim(),
        departmentId: formData.departmentId
          ? Number(formData.departmentId)
          : null,
        divisionId: formData.divisionId
          ? Number(formData.divisionId)
          : null,
        floorId: formData.floorId
          ? Number(formData.floorId)
          : null,
        roleIds: formData.roleId ? [Number(formData.roleId)] : [],
      };

      if (isEdit) {
        await updateStaff(editStaff.staffId, payload);
        toast.success("Staff profile updated successfully");
      } else {
        await createStaff({
          ...payload,
          email: formData.email.trim(),
        });
        toast.success("Staff member created successfully");
      }

      onSuccess();
      onClose();
    } catch (error) {
      toast.error(
        error.response?.data?.message ||
          (isEdit ? "Failed to update staff" : "Failed to create staff")
      );
    } finally {
      setSubmitting(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4 overflow-y-auto">
      <div className="w-full max-w-2xl rounded-3xl border border-white/10 bg-slate-900 p-6 shadow-2xl">
        <div className="flex items-center justify-between pb-4 border-b border-white/10 mb-6">
          <div>
            <h2 className="text-2xl font-bold text-white">
              {isEdit ? "Edit Staff Member" : "Add New Staff Member"}
            </h2>
            <p className="text-sm text-slate-400 mt-1">
              Configure profile information, primary designation, and organizational scopes
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-2 rounded-xl text-slate-400 hover:text-white hover:bg-white/10 transition"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Section 1: Basic Information */}
          <div>
            <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-3">
              Account Information
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1.5">
                  Full Name <span className="text-rose-400">*</span>
                </label>
                <input
                  name="name"
                  placeholder="e.g. Dr. John Doe"
                  value={formData.name}
                  onChange={handleChange}
                  className="w-full rounded-xl bg-slate-800 border border-white/10 p-3 text-white placeholder-slate-500 outline-none focus:border-indigo-500"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1.5">
                  Email Address <span className="text-rose-400">*</span>
                </label>
                <input
                  name="email"
                  type="email"
                  placeholder="e.g. jdoe@university.edu"
                  value={formData.email}
                  onChange={handleChange}
                  disabled={isEdit}
                  className="w-full rounded-xl bg-slate-800 border border-white/10 p-3 text-white placeholder-slate-500 outline-none focus:border-indigo-500 disabled:opacity-50"
                  required
                />
              </div>

              <div className="md:col-span-2">
                <label className="block text-xs font-medium text-slate-300 mb-1.5">
                  Phone Number
                </label>
                <input
                  name="phone"
                  placeholder="e.g. +91 9876543210"
                  value={formData.phone}
                  onChange={handleChange}
                  className="w-full rounded-xl bg-slate-800 border border-white/10 p-3 text-white placeholder-slate-500 outline-none focus:border-indigo-500"
                />
              </div>
            </div>
          </div>

          {/* Section 2: Designation */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
                <Shield className="h-3.5 w-3.5 text-indigo-400" />
                Primary Designation <span className="text-rose-400">*</span>
              </h3>
              {roleScope && (
                <span className="text-xs px-2.5 py-0.5 rounded-full bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                  Scope: {roleScope}
                </span>
              )}
            </div>

            <select
              name="roleId"
              value={formData.roleId}
              onChange={handleChange}
              className="w-full rounded-xl bg-slate-800 border border-white/10 p-3 text-white outline-none focus:border-indigo-500"
              required
            >
              <option value="">-- Select Designation --</option>
              {roles.map((role) => (
                <option key={role.roleId} value={role.roleId}>
                  {role.roleName}
                  {role.assignmentScope ? ` (${role.assignmentScope})` : ""}
                </option>
              ))}
            </select>
          </div>

          {/* Section 3: Organizational Assignments */}
          <div>
            <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-3 flex items-center gap-1.5">
              <Building className="h-3.5 w-3.5 text-indigo-400" />
              Organizational Scope & Assignments
            </h3>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* Department */}
              <div className={!showDivision && !showFloor ? "md:col-span-2" : ""}>
                <div className="flex items-center justify-between mb-1.5">
                  <label className="text-xs font-medium text-slate-300">
                    Department
                  </label>
                  <span
                    className={`text-[10px] px-2 py-0.5 rounded-full font-semibold ${
                      isDepartmentRequired
                        ? "bg-amber-500/20 text-amber-300 border border-amber-500/30"
                        : "bg-slate-700/50 text-slate-400"
                    }`}
                  >
                    {isDepartmentRequired ? "Required *" : "Optional"}
                  </span>
                </div>
                <select
                  name="departmentId"
                  value={formData.departmentId}
                  onChange={handleDepartmentChange}
                  className="w-full rounded-xl bg-slate-800 border border-white/10 p-3 text-white outline-none focus:border-indigo-500"
                >
                  <option value="">-- Unassigned --</option>
                  {departments.map((dept) => (
                    <option key={dept.departmentId} value={dept.departmentId}>
                      {dept.name}
                    </option>
                  ))}
                </select>
              </div>

              {/* Division */}
              {showDivision && (
                <div>
                  <div className="flex items-center justify-between mb-1.5">
                    <label className="text-xs font-medium text-slate-300 flex items-center gap-1">
                      <Layers className="h-3 w-3 text-slate-400" />
                      Academic Division
                    </label>
                    <span
                      className={`text-[10px] px-2 py-0.5 rounded-full font-semibold ${
                        isDivisionRequired
                          ? "bg-amber-500/20 text-amber-300 border border-amber-500/30"
                          : "bg-slate-700/50 text-slate-400"
                      }`}
                    >
                      {isDivisionRequired ? "Required *" : "Optional"}
                    </span>
                  </div>
                  <select
                    name="divisionId"
                    value={formData.divisionId}
                    onChange={handleChange}
                    className="w-full rounded-xl bg-slate-800 border border-white/10 p-3 text-white outline-none focus:border-indigo-500 disabled:opacity-40"
                    disabled={!formData.departmentId && filteredDivisions.length > 0}
                  >
                    <option value="">-- Unassigned --</option>
                    {filteredDivisions.map((division) => (
                      <option
                        key={division.divisionId}
                        value={division.divisionId}
                      >
                        {division.name}
                      </option>
                    ))}
                  </select>
                  {!formData.departmentId && (
                    <p className="text-[11px] text-slate-500 mt-1">
                      Select a department first to filter academic divisions
                    </p>
                  )}
                </div>
              )}

              {/* Floor */}
              {showFloor && (
                <div className={!showDivision ? "md:col-span-2" : ""}>
                  <div className="flex items-center justify-between mb-1.5">
                    <label className="text-xs font-medium text-slate-300 flex items-center gap-1">
                      <Home className="h-3 w-3 text-slate-400" />
                      Hostel Floor
                    </label>
                    <span
                      className={`text-[10px] px-2 py-0.5 rounded-full font-semibold ${
                        isFloorRequired
                          ? "bg-amber-500/20 text-amber-300 border border-amber-500/30"
                          : "bg-slate-700/50 text-slate-400"
                      }`}
                    >
                      {isFloorRequired ? "Required *" : "Optional"}
                    </span>
                  </div>
                  <select
                    name="floorId"
                    value={formData.floorId}
                    onChange={handleChange}
                    className="w-full rounded-xl bg-slate-800 border border-white/10 p-3 text-white outline-none focus:border-indigo-500"
                  >
                    <option value="">-- Unassigned --</option>
                    {floors.map((floor) => (
                      <option key={floor.floorId} value={floor.floorId}>
                        {floor.floorNumber}
                      </option>
                    ))}
                  </select>
                </div>
              )}
            </div>
          </div>

          {/* Form Actions */}
          <div className="flex justify-end gap-3 pt-4 border-t border-white/10">
            <button
              type="button"
              onClick={onClose}
              disabled={submitting}
              className="rounded-xl bg-slate-800 border border-white/10 px-5 py-2.5 text-sm font-medium text-slate-300 hover:text-white hover:bg-slate-700 transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="rounded-xl bg-gradient-to-r from-indigo-500 to-blue-600 px-6 py-2.5 text-sm font-semibold text-white shadow-lg shadow-indigo-500/25 hover:from-indigo-400 hover:to-blue-500 transition disabled:opacity-50"
            >
              {submitting
                ? "Saving..."
                : isEdit
                ? "Update Staff"
                : "Create Staff"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}