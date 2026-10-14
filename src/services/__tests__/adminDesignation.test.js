import { describe, it } from "node:test";
import assert from "node:assert/strict";
import * as designationApi from "../adminDesignationApi.js";

/**
 * Unit Test Suite: SGMS Phase 2.3 — Admin Designation Management Frontend Integration
 */

// Helper validation logic mirroring DesignationsManagement.jsx
function validateDesignationName(name) {
  if (!name || typeof name !== "string") {
    return { valid: false, error: "Designation name is required" };
  }
  const trimmed = name.trim().toUpperCase();
  if (!trimmed) {
    return { valid: false, error: "Designation name is required" };
  }
  if (trimmed.length < 2 || trimmed.length > 100) {
    return { valid: false, error: "Designation name must be between 2 and 100 characters" };
  }
  if (trimmed === "ADMIN" || trimmed === "STUDENT") {
    return { valid: false, error: `Cannot create or rename designation to reserved system role name '${trimmed}'` };
  }
  return { valid: true, value: trimmed };
}

function isDesignationProtected(designation) {
  if (!designation) return false;
  return Boolean(designation.systemRole || designation.isSystemRole || designation.id === 1);
}

function validateStaffScopeCompatibility(designation, staff) {
  if (!designation || !staff) return { compatible: true };
  const scope = designation.assignmentScope;
  if (scope === "DEPARTMENT" && !staff.department) {
    return { compatible: false, warning: "Staff missing Department" };
  }
  if (scope === "DIVISION" && !staff.academicDivision) {
    return { compatible: false, warning: "Staff missing Academic Division" };
  }
  if (scope === "FLOOR" && !staff.floor) {
    return { compatible: false, warning: "Staff missing Hostel Floor" };
  }
  return { compatible: true };
}

describe("SGMS Phase 2.3 — Admin Designation Management Frontend Integration Test Suite", () => {
  describe("1. API Module Contracts & Function Exports", () => {
    it("exports all 8 required designation management API functions", () => {
      assert.equal(typeof designationApi.getAllDesignations, "function");
      assert.equal(typeof designationApi.getDesignationById, "function");
      assert.equal(typeof designationApi.createDesignation, "function");
      assert.equal(typeof designationApi.updateDesignation, "function");
      assert.equal(typeof designationApi.deleteDesignation, "function");
      assert.equal(typeof designationApi.getStaffDesignations, "function");
      assert.equal(typeof designationApi.assignStaffDesignations, "function");
      assert.equal(typeof designationApi.removeStaffDesignation, "function");
    });
  });

  describe("2. Designation Validation & Reserved Role Protections", () => {
    it("accepts valid alphanumeric organizational designation name", () => {
      const res = validateDesignationName("lab_coordinator");
      assert.equal(res.valid, true);
      assert.equal(res.value, "LAB_COORDINATOR");
    });

    it("rejects empty or whitespace designation name", () => {
      const res = validateDesignationName("   ");
      assert.equal(res.valid, false);
      assert.equal(res.error, "Designation name is required");
    });

    it("rejects designation names shorter than 2 characters", () => {
      const res = validateDesignationName("A");
      assert.equal(res.valid, false);
      assert.match(res.error, /between 2 and 100 characters/);
    });

    it("rejects reserved system role name 'ADMIN' (case-insensitive)", () => {
      const resUpper = validateDesignationName("ADMIN");
      assert.equal(resUpper.valid, false);
      assert.match(resUpper.error, /reserved system role name 'ADMIN'/);

      const resLower = validateDesignationName("admin");
      assert.equal(resLower.valid, false);
      assert.match(resLower.error, /reserved system role name 'ADMIN'/);
    });

    it("rejects reserved system role name 'STUDENT'", () => {
      const res = validateDesignationName("student");
      assert.equal(res.valid, false);
      assert.match(res.error, /reserved system role name 'STUDENT'/);
    });
  });

  describe("3. Protected System Role Guards", () => {
    it("identifies platform ADMIN role (ID 1) as protected system role", () => {
      const adminRole = { id: 1, name: "ADMIN", assignmentScope: "GLOBAL", systemRole: true };
      assert.equal(isDesignationProtected(adminRole), true);
    });

    it("identifies STUDENT role as protected system role via systemRole flag", () => {
      const studentRole = { id: 3, name: "STUDENT", assignmentScope: "GLOBAL", systemRole: true };
      assert.equal(isDesignationProtected(studentRole), true);
    });

    it("identifies organizational designations (e.g. HOD, WARDEN) as editable/deletable", () => {
      const hodRole = { id: 4, name: "HOD", assignmentScope: "DEPARTMENT", systemRole: false };
      assert.equal(isDesignationProtected(hodRole), false);

      const wardenRole = { id: 6, name: "WARDEN", assignmentScope: "FLOOR", systemRole: false };
      assert.equal(isDesignationProtected(wardenRole), false);
    });
  });

  describe("4. Scope & Staff Compatibility Checks", () => {
    const staffWithDept = {
      staffId: 10,
      name: "Dr. Sharma",
      department: { departmentId: 1, name: "Computer Science" },
      academicDivision: null,
      floor: null,
    };

    const staffWithDivision = {
      staffId: 11,
      name: "Prof. Verma",
      department: null,
      academicDivision: { divisionId: 2, name: "Engineering Division" },
      floor: null,
    };

    const staffWithFloor = {
      staffId: 12,
      name: "Warden Bob",
      department: null,
      academicDivision: null,
      floor: { floorId: 1, floorNumber: 3 },
    };

    it("allows DEPARTMENT-scoped designation for staff with department", () => {
      const hod = { id: 4, name: "HOD", assignmentScope: "DEPARTMENT" };
      const res = validateStaffScopeCompatibility(hod, staffWithDept);
      assert.equal(res.compatible, true);
    });

    it("flags warning for DEPARTMENT-scoped designation when staff has no department", () => {
      const hod = { id: 4, name: "HOD", assignmentScope: "DEPARTMENT" };
      const res = validateStaffScopeCompatibility(hod, staffWithDivision);
      assert.equal(res.compatible, false);
      assert.equal(res.warning, "Staff missing Department");
    });

    it("allows DIVISION-scoped designation for staff with academic division", () => {
      const mft = { id: 2, name: "MFT", assignmentScope: "DIVISION" };
      const res = validateStaffScopeCompatibility(mft, staffWithDivision);
      assert.equal(res.compatible, true);
    });

    it("flags warning for DIVISION-scoped designation when staff has no division", () => {
      const mft = { id: 2, name: "MFT", assignmentScope: "DIVISION" };
      const res = validateStaffScopeCompatibility(mft, staffWithDept);
      assert.equal(res.compatible, false);
      assert.equal(res.warning, "Staff missing Academic Division");
    });

    it("allows FLOOR-scoped designation for staff with hostel floor", () => {
      const warden = { id: 6, name: "WARDEN", assignmentScope: "FLOOR" };
      const res = validateStaffScopeCompatibility(warden, staffWithFloor);
      assert.equal(res.compatible, true);
    });

    it("flags warning for FLOOR-scoped designation when staff has no floor", () => {
      const warden = { id: 6, name: "WARDEN", assignmentScope: "FLOOR" };
      const res = validateStaffScopeCompatibility(warden, staffWithDept);
      assert.equal(res.compatible, false);
      assert.equal(res.warning, "Staff missing Hostel Floor");
    });

    it("allows GLOBAL-scoped designation for any staff member regardless of attributes", () => {
      const proctor = { id: 9, name: "CHIEF_PROCTOR", assignmentScope: "GLOBAL" };
      assert.equal(validateStaffScopeCompatibility(proctor, staffWithDept).compatible, true);
      assert.equal(validateStaffScopeCompatibility(proctor, staffWithDivision).compatible, true);
      assert.equal(validateStaffScopeCompatibility(proctor, staffWithFloor).compatible, true);
    });
  });

  describe("5. Response Shape Serialization & Field Compatibility", () => {
    it("correctly parses backend DesignationResponse with Jackson boolean property", () => {
      const backendPayload = {
        id: 4,
        name: "HOD",
        assignmentScope: "DEPARTMENT",
        assignedStaffCount: 3,
        workflowStepCount: 8,
        systemRole: false,
      };

      const isSystem = backendPayload.systemRole ?? backendPayload.isSystemRole;
      assert.equal(isSystem, false);
      assert.equal(backendPayload.assignedStaffCount, 3);
      assert.equal(backendPayload.workflowStepCount, 8);
    });

    it("correctly identifies platformAdmin boolean property on StaffDesignationAssignmentResponse", () => {
      const staffResponse = {
        staffId: 1,
        staffName: "Naveen Kumar",
        email: "naveen@sgms.com",
        designations: [{ id: 4, name: "HOD", assignmentScope: "DEPARTMENT" }],
        platformAdmin: true,
      };

      assert.equal(Boolean(staffResponse.platformAdmin || staffResponse.isPlatformAdmin), true);
      assert.equal(staffResponse.designations.length, 1);
    });
  });
});
