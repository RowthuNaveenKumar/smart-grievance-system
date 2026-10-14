import { describe, it } from "node:test";
import assert from "node:assert/strict";

/**
 * Unit Test Suite: SGMS Workflow-Aware Escalation Control
 * Tests escalation availability, hierarchy levels, final authority controls,
 * incomplete configurations, and resolution independence.
 */

// Helper evaluating escalation control logic matching ComplaintDetails.jsx
function getEscalationControlState(complaint, user) {
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

  const canAct = (isStaff || (isAdmin && isAssignedStaff)) &&
    (complaint.status === "OPEN" || complaint.status === "IN_PROGRESS");

  if (!canAct) {
    return {
      canAct: false,
      showEscalateButton: false,
      showFinalAuthorityBadge: false,
      showDisabledReason: false,
      showResolveButton: false,
      showMarkInProgressButton: false,
    };
  }

  const showMarkInProgressButton = complaint.status === "OPEN";
  const showResolveButton = complaint.status === "IN_PROGRESS";

  const showEscalateButton = Boolean(complaint.canEscalate);
  const showFinalAuthorityBadge = !showEscalateButton && Boolean(complaint.isFinalAuthority);
  const showDisabledReason = !showEscalateButton && !showFinalAuthorityBadge && Boolean(complaint.escalationDisabledReason);

  const buttonLabel = complaint.nextEscalationRole
    ? `Escalate to ${complaint.nextEscalationRole}`
    : "Escalate";

  const finalAuthorityText = complaint.isFinalAuthority
    ? `Final Authority (Level ${complaint.currentLevel || 1}/${complaint.maxLevel || 1})`
    : null;

  return {
    canAct: true,
    showEscalateButton,
    showFinalAuthorityBadge,
    showDisabledReason,
    showResolveButton,
    showMarkInProgressButton,
    buttonLabel,
    finalAuthorityText,
    disabledReason: complaint.escalationDisabledReason,
  };
}

describe("SGMS Workflow-Aware Escalation Control Test Suite", () => {
  const mockStaffUser = {
    accountType: "STAFF",
    role: "STAFF",
    staff: { staffId: 20 },
    email: "staff@sgms.com",
  };

  const mockAdminUser = {
    accountType: "STAFF",
    role: "ADMIN",
    staff: { staffId: 1 },
    email: "admin@sgms.com",
  };

  const mockStudentUser = {
    accountType: "STUDENT",
    role: "STUDENT",
    email: "student@sgms.com",
  };

  describe("1. Staff with valid next escalation authority", () => {
    it("displays Escalate button with target role when next authority exists", () => {
      const complaint = {
        complaintId: 101,
        status: "IN_PROGRESS",
        currentLevel: 1,
        maxLevel: 2,
        canEscalate: true,
        isFinalAuthority: false,
        nextEscalationRole: "Warden",
        assignedStaffId: 20,
      };

      const state = getEscalationControlState(complaint, mockStaffUser);
      assert.equal(state.canAct, true);
      assert.equal(state.showEscalateButton, true);
      assert.equal(state.buttonLabel, "Escalate to Warden");
      assert.equal(state.showFinalAuthorityBadge, false);
      assert.equal(state.showResolveButton, true);
    });

    it("displays Escalate button when complaint is OPEN and canEscalate is true", () => {
      const complaint = {
        complaintId: 102,
        status: "OPEN",
        currentLevel: 1,
        maxLevel: 3,
        canEscalate: true,
        isFinalAuthority: false,
        nextEscalationRole: "Supervisor",
        assignedStaffId: 20,
      };

      const state = getEscalationControlState(complaint, mockStaffUser);
      assert.equal(state.canAct, true);
      assert.equal(state.showEscalateButton, true);
      assert.equal(state.buttonLabel, "Escalate to Supervisor");
      assert.equal(state.showMarkInProgressButton, true);
      assert.equal(state.showResolveButton, false);
    });
  });

  describe("2. Department Head / Mid-level Authority with higher destination", () => {
    it("displays Escalate to Dean for level 2 department head when maxLevel is 3", () => {
      const complaint = {
        complaintId: 103,
        status: "IN_PROGRESS",
        currentLevel: 2,
        maxLevel: 3,
        canEscalate: true,
        isFinalAuthority: false,
        nextEscalationRole: "Dean",
        assignedStaffId: 20,
      };

      const state = getEscalationControlState(complaint, mockStaffUser);
      assert.equal(state.canAct, true);
      assert.equal(state.showEscalateButton, true);
      assert.equal(state.buttonLabel, "Escalate to Dean");
      assert.equal(state.showFinalAuthorityBadge, false);
    });
  });

  describe("3. Final Authority with no higher step", () => {
    it("hides Escalate button and displays Final Authority badge when currentLevel equals maxLevel", () => {
      const complaint = {
        complaintId: 104,
        status: "IN_PROGRESS",
        currentLevel: 2,
        maxLevel: 2,
        canEscalate: false,
        isFinalAuthority: true,
        nextEscalationRole: null,
        escalationDisabledReason: "Complaint is at the final authority level (2 of 2)",
        assignedStaffId: 20,
      };

      const state = getEscalationControlState(complaint, mockStaffUser);
      assert.equal(state.canAct, true);
      assert.equal(state.showEscalateButton, false);
      assert.equal(state.showFinalAuthorityBadge, true);
      assert.equal(state.finalAuthorityText, "Final Authority (Level 2/2)");
      // Crucial: Resolve button MUST remain available for final authority
      assert.equal(state.showResolveButton, true);
    });

    it("displays Final Authority badge for assigned admin at final authority level", () => {
      const complaint = {
        complaintId: 343,
        status: "OPEN",
        currentLevel: 2,
        maxLevel: 2,
        canEscalate: false,
        isFinalAuthority: true,
        nextEscalationRole: null,
        assignedStaffId: 1, // matches mockAdminUser
      };

      const state = getEscalationControlState(complaint, mockAdminUser);
      assert.equal(state.canAct, true);
      assert.equal(state.showEscalateButton, false);
      assert.equal(state.showFinalAuthorityBadge, true);
      assert.equal(state.showMarkInProgressButton, true);
    });
  });

  describe("4. Incomplete workflow configuration", () => {
    it("hides Escalate button and displays explanation when no workflow is configured", () => {
      const complaint = {
        complaintId: 105,
        status: "IN_PROGRESS",
        currentLevel: 1,
        maxLevel: null,
        canEscalate: false,
        isFinalAuthority: false,
        escalationDisabledReason: "No active workflow configured for this department",
        assignedStaffId: 20,
      };

      const state = getEscalationControlState(complaint, mockStaffUser);
      assert.equal(state.canAct, true);
      assert.equal(state.showEscalateButton, false);
      assert.equal(state.showFinalAuthorityBadge, false);
      assert.equal(state.showDisabledReason, true);
      assert.equal(state.disabledReason, "No active workflow configured for this department");
      assert.equal(state.showResolveButton, true);
    });

    it("hides Escalate button when next role has no staff configured", () => {
      const complaint = {
        complaintId: 106,
        status: "IN_PROGRESS",
        currentLevel: 1,
        maxLevel: 2,
        canEscalate: false,
        isFinalAuthority: false,
        escalationDisabledReason: "No staff member configured for next authority role 'Chief Warden'",
        assignedStaffId: 20,
      };

      const state = getEscalationControlState(complaint, mockStaffUser);
      assert.equal(state.showEscalateButton, false);
      assert.equal(state.showDisabledReason, true);
      assert.equal(state.disabledReason, "No staff member configured for next authority role 'Chief Warden'");
    });
  });

  describe("5. Terminal states and unauthorized users", () => {
    it("hides all actions when complaint is RESOLVED", () => {
      const complaint = {
        complaintId: 107,
        status: "RESOLVED",
        canEscalate: false,
        assignedStaffId: 20,
      };

      const state = getEscalationControlState(complaint, mockStaffUser);
      assert.equal(state.canAct, false);
      assert.equal(state.showEscalateButton, false);
    });

    it("hides all actions when complaint is CLOSED", () => {
      const complaint = {
        complaintId: 108,
        status: "CLOSED",
        canEscalate: false,
        assignedStaffId: 20,
      };

      const state = getEscalationControlState(complaint, mockStaffUser);
      assert.equal(state.canAct, false);
      assert.equal(state.showEscalateButton, false);
    });

    it("strictly hides all actions and escalate controls from students", () => {
      const complaint = {
        complaintId: 109,
        status: "IN_PROGRESS",
        canEscalate: true,
        assignedStaffId: 20,
      };

      const state = getEscalationControlState(complaint, mockStudentUser);
      assert.equal(state.canAct, false);
      assert.equal(state.showEscalateButton, false);
    });
  });
});
