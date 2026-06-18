package com.sgms.sgms_backend.enums;

public enum ComplaintAction {

    SUBMITTED,
    MARK_IN_PROGRESS,
    RESOLVE,
    ESCALATE,
    STUDENT_ACCEPT,
    STUDENT_REJECT,
    UPDATE_NOTE;

    public ComplaintStatus toStatus() {

        return switch (this) {

            case SUBMITTED -> ComplaintStatus.OPEN;

            case MARK_IN_PROGRESS -> ComplaintStatus.IN_PROGRESS;

            case UPDATE_NOTE -> null;

            case RESOLVE -> ComplaintStatus.RESOLVED;

            case ESCALATE -> ComplaintStatus.ESCALATED;

            case STUDENT_ACCEPT -> ComplaintStatus.CLOSED;

            case STUDENT_REJECT -> ComplaintStatus.OPEN;
        };
    }
}