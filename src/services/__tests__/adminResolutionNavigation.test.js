import { describe, it } from "node:test";
import assert from "node:assert/strict";

/**
 * Unit Test Suite: SGMS Admin Resolution & Back Navigation Regression Fix
 */

// Pure function replicating getBackDestination from ComplaintDetails.jsx
function getBackDestination(locationState, user) {
  const from = locationState?.from;

  if (from === "submission") return "/student-dashboard";
  if (from === "student-dashboard") return "/student-dashboard";
  if (from === "staff-dashboard") return "/staff-dashboard";
  if (from === "admin-dashboard") return "/admin-dashboard";
  if (from === "admin-complaints") return "/admin/complaints";

  // Safe role-based fallback when state is absent (direct URL / refresh)
  if (user?.role === "ADMIN") return "/admin-dashboard";
  if (user?.accountType === "STUDENT") return "/student-dashboard";
  if (user?.accountType === "STAFF") return "/staff-dashboard";

  // Last resort: login (should not be reached for authenticated users)
  return "/login";
}

// Pure function replicating canAct from ComplaintDetails.jsx
function canActOnComplaint(user, complaint) {
  const isAdmin = user?.role === "ADMIN";
  const isStaff = user?.accountType === "STAFF" && !isAdmin;

  const isAssignedStaff =
    Boolean(
      (user?.staff?.staffId &&
        complaint?.assignedStaffId &&
        Number(user.staff.staffId) === Number(complaint.assignedStaffId)) ||
      (user?.email &&
        complaint?.assignedStaffEmail &&
        user.email.toLowerCase() === complaint.assignedStaffEmail.toLowerCase())
    );

  const canAct = isStaff || (isAdmin && isAssignedStaff);
  return canAct && (complaint.status === "OPEN" || complaint.status === "IN_PROGRESS");
}

describe("SGMS Admin Resolution & Back Navigation Regression Test Suite", () => {
  describe("1. Back Navigation Destination Resolution", () => {
    it("returns /admin/complaints when originating from admin-complaints", () => {
      const dest = getBackDestination({ from: "admin-complaints" }, { role: "ADMIN" });
      assert.equal(dest, "/admin/complaints");
    });

    it("returns /admin-dashboard when originating from admin-dashboard", () => {
      const dest = getBackDestination({ from: "admin-dashboard" }, { role: "ADMIN" });
      assert.equal(dest, "/admin-dashboard");
    });

    it("falls back to /admin-dashboard for direct URL or refresh when user is ADMIN", () => {
      const dest = getBackDestination(null, { role: "ADMIN" });
      assert.equal(dest, "/admin-dashboard");
    });

    it("falls back to /student-dashboard for direct URL when user is STUDENT", () => {
      const dest = getBackDestination(null, { accountType: "STUDENT" });
      assert.equal(dest, "/student-dashboard");
    });

    it("falls back to /staff-dashboard for direct URL when user is STAFF", () => {
      const dest = getBackDestination(null, { accountType: "STAFF" });
      assert.equal(dest, "/staff-dashboard");
    });
  });

  describe("2. Admin Resolution Actions Visibility", () => {
    const adminUser = {
      userId: 7,
      email: "naveen@sgms.com",
      accountType: "STAFF",
      role: "ADMIN",
      staff: { staffId: 1, name: "Naveen Kumar" },
    };

    it("allows ADMIN to see actions when complaint is assigned to that admin (matching staffId) in OPEN status", () => {
      const complaint = {
        complaintId: 343,
        status: "OPEN",
        assignedStaffId: 1,
        assignedStaffEmail: "naveen@sgms.com",
      };
      assert.equal(canActOnComplaint(adminUser, complaint), true);
    });

    it("allows ADMIN to see actions when complaint is assigned to that admin in IN_PROGRESS status", () => {
      const complaint = {
        complaintId: 343,
        status: "IN_PROGRESS",
        assignedStaffId: 1,
        assignedStaffEmail: "naveen@sgms.com",
      };
      assert.equal(canActOnComplaint(adminUser, complaint), true);
    });

    it("hides actions for ADMIN when complaint is assigned to a different staff member", () => {
      const complaint = {
        complaintId: 100,
        status: "OPEN",
        assignedStaffId: 99,
        assignedStaffEmail: "other@sgms.com",
      };
      assert.equal(canActOnComplaint(adminUser, complaint), false);
    });

    it("hides actions for ADMIN when complaint is already RESOLVED", () => {
      const complaint = {
        complaintId: 343,
        status: "RESOLVED",
        assignedStaffId: 1,
        assignedStaffEmail: "naveen@sgms.com",
      };
      assert.equal(canActOnComplaint(adminUser, complaint), false);
    });

    it("hides actions for ADMIN when complaint is CLOSED", () => {
      const complaint = {
        complaintId: 343,
        status: "CLOSED",
        assignedStaffId: 1,
        assignedStaffEmail: "naveen@sgms.com",
      };
      assert.equal(canActOnComplaint(adminUser, complaint), false);
    });

    it("allows regular STAFF to see actions for active complaints", () => {
      const staffUser = {
        userId: 20,
        email: "staff@sgms.com",
        accountType: "STAFF",
        role: "WARDEN",
        staff: { staffId: 2, name: "Warden John" },
      };
      const complaint = {
        complaintId: 200,
        status: "IN_PROGRESS",
        assignedStaffId: 2,
      };
      assert.equal(canActOnComplaint(staffUser, complaint), true);
    });

    it("strictly hides staff/admin actions from STUDENT users", () => {
      const studentUser = {
        userId: 10,
        email: "student@sgms.com",
        accountType: "STUDENT",
      };
      const complaint = {
        complaintId: 343,
        status: "OPEN",
        assignedStaffId: 1,
      };
      assert.equal(canActOnComplaint(studentUser, complaint), false);
    });
  });
});
