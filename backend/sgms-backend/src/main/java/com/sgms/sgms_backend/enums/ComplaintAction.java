package com.sgms.sgms_backend.enums;

public enum ComplaintAction {

    SUBMITTED,
    MARK_IN_PROGRESS,
    RESOLVE,
    ESCALATE,
    UPDATE_NOTE;

    public ComplaintStatus toStatus() {

        return switch (this) {

            case SUBMITTED -> ComplaintStatus.OPEN;

            case MARK_IN_PROGRESS -> ComplaintStatus.IN_PROGRESS;

            case UPDATE_NOTE -> ComplaintStatus.IN_PROGRESS;

            case RESOLVE -> ComplaintStatus.RESOLVED;

            case ESCALATE -> ComplaintStatus.ESCALATED;
        };
    }
}