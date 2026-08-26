package com.sgms.sgms_backend.enums;

/**
 * Determines how a predicted ML class label is resolved to a specific
 * ComplaintCategory row in the database.
 *
 * STUDENT_DEPT  - Used for classes like ACADEMIC where one ML class maps to
 *                 multiple categories (one per academic department). The student's
 *                 department is used to select the correct active category row.
 *
 * DIRECT_SINGLE - Used for classes like HOSTEL, EXAM, TRANSPORT where exactly one
 *                 active category row exists for that ml_class across the system.
 *                 The service validates that exactly one result is found.
 */
public enum MlResolutionType {
    STUDENT_DEPT,
    DIRECT_SINGLE
}
