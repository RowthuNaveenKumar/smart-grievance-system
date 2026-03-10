package com.sgms.sgms_backend.enums;

public enum ComplaintAction {

    SUBMITTED,
    AUTO_ASSIGNED,
    UPDATED,
    RESOLVED,
    ESCALATED;

    public ComplaintStatus toStatus() {

        return switch (this) {

            case SUBMITTED -> ComplaintStatus.OPEN;

            case AUTO_ASSIGNED -> ComplaintStatus.IN_PROGRESS;

            case UPDATED -> ComplaintStatus.IN_PROGRESS;

            case RESOLVED -> ComplaintStatus.RESOLVED;

            case ESCALATED -> ComplaintStatus.ESCALATED;
        };
    }
}