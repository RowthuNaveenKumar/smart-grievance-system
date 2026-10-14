import { describe, it } from "node:test";
import assert from "node:assert/strict";
import * as staffApi from "../adminStaffApi.js";

/**
 * Unit Test Suite: SGMS Staff Management UX & Account Lifecycle Correction
 */

// Helper: Form payload builder mirroring StaffModal.jsx
function buildStaffPayload(formData) {
  return {
    name: formData.name ? formData.name.trim() : "",
    phone: formData.phone ? formData.phone.trim() : "",
    departmentId: formData.departmentId ? Number(formData.departmentId) : null,
    divisionId: formData.divisionId ? Number(formData.divisionId) : null,
    floorId: formData.floorId ? Number(formData.floorId) : null,
    roleIds: formData.roleId ? [Number(formData.roleId)] : [],
  };
}

// Helper: Scope constraint validator mirroring StaffModal.jsx
function validateStaffForm(formData, role) {
  if (!formData.name || !formData.name.trim()) {
    return { valid: false, error: "Name is required" };
  }
  if (!formData.roleId) {
    return { valid: false, error: "Please select a designation" };
  }

  const roleName = role?.roleName?.toUpperCase() || "";
  const roleScope =
    role?.assignmentScope ||
    (roleName === "HOD"
      ? "DEPARTMENT"
      : roleName === "MFT"
      ? "DIVISION"
      : roleName === "WARDEN"
      ? "FLOOR"
      : roleName === "SYSTEM COORDINATOR" || roleName === "DEAN" || roleName === "ADMIN"
      ? "GLOBAL"
      : null);

  const isDepartmentRequired = roleScope === "DEPARTMENT" || roleScope === "DIVISION";
  const isDivisionRequired = roleScope === "DIVISION";
  const isFloorRequired = roleScope === "FLOOR";

  if (isDepartmentRequired && !formData.departmentId) {
    return {
      valid: false,
      error: `Department is required for designation '${role?.roleName || "selected"}'`,
    };
  }

  if (isDivisionRequired && !formData.divisionId) {
    return {
      valid: false,
      error: `Academic division is required for designation '${role?.roleName || "selected"}'`,
    };
  }

  if (isFloorRequired && !formData.floorId) {
    return {
      valid: false,
      error: `Hostel floor is required for designation '${role?.roleName || "selected"}'`,
    };
  }

  if (formData.divisionId && !formData.departmentId) {
    return {
      valid: false,
      error: "Cannot assign academic division without selecting its parent department",
    };
  }

  return { valid: true };
}

// Helper: Dependent dropdown filtering
function filterDivisionsByDepartment(divisions, departmentId) {
  if (!departmentId) return divisions;
  return divisions.filter(
    (d) => !d.departmentId || String(d.departmentId) === String(departmentId)
  );
}

// Helper: Directory filter logic mirroring StaffManagement.jsx
function filterStaffList(staffList, search = "", statusFilter = "ALL") {
  const query = search.toLowerCase();
  return staffList.filter((member) => {
    const matchesSearch =
      member.name?.toLowerCase().includes(query) ||
      member.email?.toLowerCase().includes(query) ||
      member.phone?.toLowerCase().includes(query) ||
      member.department?.toLowerCase().includes(query) ||
      member.roles?.some((r) => r.toLowerCase().includes(query));

    if (!matchesSearch) return false;

    if (statusFilter === "ACTIVE") return member.enabled;
    if (statusFilter === "INACTIVE") return !member.enabled;
    return true;
  });
}

describe("SGMS Staff Management UX & Account Lifecycle Test Suite", () => {
  describe("1. API Module Exports", () => {
    it("exports all required staff and organizational API client methods", () => {
      assert.equal(typeof staffApi.getAllStaff, "function");
      assert.equal(typeof staffApi.getStaffById, "function");
      assert.equal(typeof staffApi.createStaff, "function");
      assert.equal(typeof staffApi.updateStaff, "function");
      assert.equal(typeof staffApi.disableStaff, "function");
      assert.equal(typeof staffApi.enableStaff, "function");
      assert.equal(typeof staffApi.getDepartments, "function");
      assert.equal(typeof staffApi.getDivisions, "function");
      assert.equal(typeof staffApi.getFloors, "function");
      assert.equal(typeof staffApi.getRoles, "function");
    });
  });

  describe("2. Conditional Organizational Fields & Scope Rules", () => {
    const coordinatorRole = {
      roleId: 5,
      roleName: "SYSTEM COORDINATOR",
      assignmentScope: "GLOBAL",
    };
    const deanRole = {
      roleId: 6,
      roleName: "DEAN",
      assignmentScope: "GLOBAL",
    };
    const hodRole = {
      roleId: 2,
      roleName: "HOD",
      assignmentScope: "DEPARTMENT",
    };
    const mftRole = {
      roleId: 3,
      roleName: "MFT",
      assignmentScope: "DIVISION",
    };
    const wardenRole = {
      roleId: 4,
      roleName: "WARDEN",
      assignmentScope: "FLOOR",
    };

    it("SYSTEM COORDINATOR is valid without academic division or hostel floor", () => {
      const formData = {
        name: "Coordinator Alice",
        roleId: "5",
        departmentId: "",
        divisionId: "",
        floorId: "",
      };
      const result = validateStaffForm(formData, coordinatorRole);
      assert.equal(result.valid, true);

      const payload = buildStaffPayload(formData);
      assert.equal(payload.departmentId, null);
      assert.equal(payload.divisionId, null);
      assert.equal(payload.floorId, null);
    });

    it("DEAN is valid without academic division or hostel floor", () => {
      const formData = {
        name: "Dean Bob",
        roleId: "6",
        departmentId: "10", // legitimate department assignment preserved
        divisionId: "",
        floorId: "",
      };
      const result = validateStaffForm(formData, deanRole);
      assert.equal(result.valid, true);

      const payload = buildStaffPayload(formData);
      assert.equal(payload.departmentId, 10);
      assert.equal(payload.divisionId, null);
      assert.equal(payload.floorId, null);
    });

    it("HOD requires department and rejects unassigned department", () => {
      const invalidForm = {
        name: "Dr. Charlie",
        roleId: "2",
        departmentId: "",
        divisionId: "",
        floorId: "",
      };
      const result = validateStaffForm(invalidForm, hodRole);
      assert.equal(result.valid, false);
      assert.match(result.error, /Department is required/);

      const validForm = {
        name: "Dr. Charlie",
        roleId: "2",
        departmentId: "10",
        divisionId: "",
        floorId: "",
      };
      assert.equal(validateStaffForm(validForm, hodRole).valid, true);
    });

    it("MFT requires department and academic division", () => {
      const missingDivision = {
        name: "Prof. Diana",
        roleId: "3",
        departmentId: "10",
        divisionId: "",
        floorId: "",
      };
      assert.equal(validateStaffForm(missingDivision, mftRole).valid, false);

      const validForm = {
        name: "Prof. Diana",
        roleId: "3",
        departmentId: "10",
        divisionId: "101",
        floorId: "",
      };
      assert.equal(validateStaffForm(validForm, mftRole).valid, true);
    });

    it("WARDEN requires hostel floor", () => {
      const missingFloor = {
        name: "Warden Edward",
        roleId: "4",
        departmentId: "",
        divisionId: "",
        floorId: "",
      };
      assert.equal(validateStaffForm(missingFloor, wardenRole).valid, false);

      const validForm = {
        name: "Warden Edward",
        roleId: "4",
        departmentId: "",
        divisionId: "",
        floorId: "1",
      };
      assert.equal(validateStaffForm(validForm, wardenRole).valid, true);
    });
  });

  describe("3. Safe Assignment Removal & Payload Serialization", () => {
    it("serializes unassigned/cleared dropdown values to null in API payload", () => {
      const formData = {
        name: "General Staff",
        phone: "+91 9876543210",
        departmentId: "",
        divisionId: "",
        floorId: "",
        roleId: "5",
      };

      const payload = buildStaffPayload(formData);
      assert.equal(payload.name, "General Staff");
      assert.equal(payload.phone, "+91 9876543210");
      assert.equal(payload.departmentId, null);
      assert.equal(payload.divisionId, null);
      assert.equal(payload.floorId, null);
      assert.deepEqual(payload.roleIds, [5]);
    });

    it("preserves legitimate department when clearing division and floor", () => {
      const formData = {
        name: "Lab Staff",
        phone: "",
        departmentId: "10",
        divisionId: "",
        floorId: "",
        roleId: "5",
      };

      const payload = buildStaffPayload(formData);
      assert.equal(payload.departmentId, 10);
      assert.equal(payload.divisionId, null);
      assert.equal(payload.floorId, null);
    });
  });

  describe("4. Dependent Dropdown Cascading & Child Reset", () => {
    const mockDivisions = [
      { divisionId: 101, name: "CSE-A", departmentId: 10 },
      { divisionId: 102, name: "CSE-B", departmentId: 10 },
      { divisionId: 201, name: "ME-A", departmentId: 20 },
    ];

    it("filters academic divisions by selected department", () => {
      const cseDivisions = filterDivisionsByDepartment(mockDivisions, "10");
      assert.equal(cseDivisions.length, 2);
      assert.deepEqual(
        cseDivisions.map((d) => d.divisionId),
        [101, 102]
      );

      const meDivisions = filterDivisionsByDepartment(mockDivisions, "20");
      assert.equal(meDivisions.length, 1);
      assert.equal(meDivisions[0].divisionId, 201);
    });

    it("prevents selecting academic division without a parent department", () => {
      const orphanDivision = {
        name: "Staff Frank",
        roleId: "5",
        departmentId: "",
        divisionId: "101",
        floorId: "",
      };
      const result = validateStaffForm(orphanDivision, {
        roleId: 5,
        roleName: "SYSTEM COORDINATOR",
        assignmentScope: "GLOBAL",
      });
      assert.equal(result.valid, false);
      assert.match(result.error, /Cannot assign academic division without selecting its parent department/);
    });
  });

  describe("5. Account Lifecycle & Directory Filtering", () => {
    const mockStaff = [
      {
        staffId: 1,
        name: "Admin User",
        email: "admin@sgms.test",
        enabled: true,
        department: "Administration",
        roles: ["ADMIN"],
      },
      {
        staffId: 2,
        name: "Coordinator Alice",
        email: "alice@sgms.test",
        enabled: true,
        department: null,
        roles: ["SYSTEM COORDINATOR"],
      },
      {
        staffId: 3,
        name: "Former Staff Bob",
        email: "bob@sgms.test",
        enabled: false,
        department: "Computer Science",
        roles: ["MFT"],
      },
      {
        staffId: 4,
        name: "Retired Warden Charlie",
        email: "charlie@sgms.test",
        enabled: false,
        department: null,
        roles: ["WARDEN"],
      },
    ];

    it("correctly filters staff by ALL, ACTIVE, and INACTIVE status", () => {
      const all = filterStaffList(mockStaff, "", "ALL");
      assert.equal(all.length, 4);

      const active = filterStaffList(mockStaff, "", "ACTIVE");
      assert.equal(active.length, 2);
      assert.deepEqual(
        active.map((s) => s.staffId),
        [1, 2]
      );

      const inactive = filterStaffList(mockStaff, "", "INACTIVE");
      assert.equal(inactive.length, 2);
      assert.deepEqual(
        inactive.map((s) => s.staffId),
        [3, 4]
      );
    });

    it("correctly filters staff by search keyword combined with status", () => {
      const activeCoordinator = filterStaffList(
        mockStaff,
        "coordinator",
        "ACTIVE"
      );
      assert.equal(activeCoordinator.length, 1);
      assert.equal(activeCoordinator[0].name, "Coordinator Alice");

      const inactiveCoordinator = filterStaffList(
        mockStaff,
        "coordinator",
        "INACTIVE"
      );
      assert.equal(inactiveCoordinator.length, 0);

      const inactiveWarden = filterStaffList(mockStaff, "warden", "INACTIVE");
      assert.equal(inactiveWarden.length, 1);
      assert.equal(inactiveWarden[0].name, "Retired Warden Charlie");
    });
  });
});
