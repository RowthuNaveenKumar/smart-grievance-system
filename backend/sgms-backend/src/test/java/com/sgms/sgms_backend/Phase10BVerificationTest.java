package com.sgms.sgms_backend;

import com.sgms.sgms_backend.dto.Department.CreateDepartmentRequest;
import com.sgms.sgms_backend.dto.Department.DepartmentResponse;
import com.sgms.sgms_backend.dto.Department.DepartmentStatusRequest;
import com.sgms.sgms_backend.dto.Department.UpdateDepartmentRequest;
import com.sgms.sgms_backend.exception.ValidationException;
import com.sgms.sgms_backend.model.Department;
import com.sgms.sgms_backend.model.Workflow;
import com.sgms.sgms_backend.repository.DepartmentRepository;
import com.sgms.sgms_backend.repository.WorkflowRepository;
import com.sgms.sgms_backend.repository.WorkflowStepRepository;
import com.sgms.sgms_backend.service.DepartmentAdminService;
import com.sgms.sgms_backend.service.resolution.CategoryResolutionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
class Phase10BVerificationTest {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private WorkflowStepRepository workflowStepRepository;

    @Autowired
    private DepartmentAdminService departmentAdminService;

    @Autowired
    private CategoryResolutionService categoryResolutionService;

    @Test
    @DisplayName("Test A & B: All 14 legacy departments have exact assigned stable codes (including AI_DS)")
    void testLegacyDepartmentCodes() {
        Map<Long, String> expectedCodes = Map.ofEntries(
                Map.entry(1L, "CSE"),
                Map.entry(2L, "IT"),
                Map.entry(3L, "AI_DS"),
                Map.entry(4L, "MECH"),
                Map.entry(5L, "CIVIL"),
                Map.entry(6L, "EEE"),
                Map.entry(7L, "ECE"),
                Map.entry(8L, "ADMIN"),
                Map.entry(9L, "EXAM"),
                Map.entry(10L, "LIBRARY"),
                Map.entry(11L, "HOSTEL"),
                Map.entry(12L, "TRANSPORT"),
                Map.entry(13L, "SPORTS"),
                Map.entry(14L, "MEDICAL")
        );

        for (Map.Entry<Long, String> entry : expectedCodes.entrySet()) {
            Department dept = departmentRepository.findById(entry.getKey()).orElse(null);
            assertThat(dept).isNotNull();
            assertThat(dept.getCode()).isEqualTo(entry.getValue());
            assertThat(dept.isActive()).isTrue();
        }

        // Specifically assert department 3 is AI_DS (not AIDS)
        Department aidsDept = departmentRepository.findById(3L).orElseThrow();
        assertThat(aidsDept.getCode()).isEqualTo("AI_DS");
    }

    @Test
    @DisplayName("Test C, L, M, N: Create dynamic department with draft workflow and verify operational readiness")
    @Transactional
    void testCreateDynamicDepartment() {
        CreateDepartmentRequest req = CreateDepartmentRequest.builder()
                .code("FINANCE_TEST")
                .name("Finance & Accounts Test")
                .description("Handles tuition and student accounts")
                .build();

        DepartmentResponse resp = departmentAdminService.createDepartment(req);

        assertThat(resp).isNotNull();
        assertThat(resp.getCode()).isEqualTo("FINANCE_TEST");
        assertThat(resp.getName()).isEqualTo("Finance & Accounts Test");
        assertThat(resp.getActive()).isTrue();
        assertThat(resp.getHasActiveWorkflow()).isFalse();
        assertThat(resp.getIsOperationallyReady()).isFalse(); // Draft workflow, no steps

        // Verify draft workflow in repository
        Optional<Workflow> draftWf = workflowRepository.findByDepartmentDepartmentIdAndVersion(resp.getDepartmentId(), 1);
        assertThat(draftWf).isPresent();
        assertThat(draftWf.get().isActive()).isFalse();
        assertThat(draftWf.get().getVersion()).isEqualTo(1);
        assertThat(workflowStepRepository.countByWorkflowWorkflowId(draftWf.get().getWorkflowId())).isEqualTo(0);

        // Verify it appears in Admin list
        List<DepartmentResponse> adminList = departmentAdminService.getAllDepartments();
        assertThat(adminList.stream().anyMatch(d -> "FINANCE_TEST".equals(d.getCode()))).isTrue();

        // Verify it is EXCLUDED from public operational readiness list
        List<DepartmentResponse> publicList = departmentAdminService.getOperationallyReadyDepartments();
        assertThat(publicList.stream().noneMatch(d -> "FINANCE_TEST".equals(d.getCode()))).isTrue();
    }

    @Test
    @DisplayName("Test D, E, F: Validation errors for duplicate code, duplicate name, and invalid code format")
    @Transactional
    void testDepartmentValidationConstraints() {
        // Duplicate code
        CreateDepartmentRequest dupCodeReq = CreateDepartmentRequest.builder()
                .code("CSE")
                .name("New CSE Copy")
                .build();
        assertThatThrownBy(() -> departmentAdminService.createDepartment(dupCodeReq))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Department code already exists: CSE");

        // Duplicate name
        CreateDepartmentRequest dupNameReq = CreateDepartmentRequest.builder()
                .code("NEW_CSE")
                .name("CSE")
                .build();
        assertThatThrownBy(() -> departmentAdminService.createDepartment(dupNameReq))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Department name already exists: CSE");

        // Invalid code format (lowercase / spaces / special chars)
        CreateDepartmentRequest invalidCodeReq = CreateDepartmentRequest.builder()
                .code("finance-dept")
                .name("Finance Dept")
                .build();
        assertThatThrownBy(() -> departmentAdminService.createDepartment(invalidCodeReq))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("Test G & H: Update department name and description while code remains strictly immutable")
    @Transactional
    void testDepartmentUpdateAndCodeImmutability() {
        Department dept = departmentRepository.findById(1L).orElseThrow();
        String originalCode = dept.getCode();

        UpdateDepartmentRequest updateReq = UpdateDepartmentRequest.builder()
                .name("Computer Science and Engineering")
                .description("Main academic department for computing")
                .build();

        DepartmentResponse updated = departmentAdminService.updateDepartment(1L, updateReq);

        assertThat(updated.getName()).isEqualTo("Computer Science and Engineering");
        assertThat(updated.getDescription()).isEqualTo("Main academic department for computing");
        assertThat(updated.getCode()).isEqualTo(originalCode); // Immutable

        Department reloaded = departmentRepository.findById(1L).orElseThrow();
        assertThat(reloaded.getCode()).isEqualTo("CSE");
    }

    @Test
    @DisplayName("Test I & J: Soft deactivation and exclusion from ingress")
    @Transactional
    void testDepartmentSoftDeactivation() {
        DepartmentStatusRequest deactivateReq = new DepartmentStatusRequest(false);
        DepartmentResponse deactivated = departmentAdminService.updateDepartmentStatus(13L, deactivateReq); // Sports

        assertThat(deactivated.getActive()).isFalse();
        assertThat(deactivated.getIsOperationallyReady()).isFalse();

        // Verify exclusion from public list
        List<DepartmentResponse> publicList = departmentAdminService.getOperationallyReadyDepartments();
        assertThat(publicList.stream().noneMatch(d -> d.getDepartmentId().equals(13L))).isTrue();

        // Reactivate
        departmentAdminService.updateDepartmentStatus(13L, new DepartmentStatusRequest(true));
    }

    @Test
    @DisplayName("Test U: Existing Academic student-division routing regression across all 7 branches")
    void testAcademicStudentDivisionRoutingRegression() {
        for (long deptId = 1L; deptId <= 7L; deptId++) {
            Optional<com.sgms.sgms_backend.model.ComplaintCategory> categoryOpt =
                    categoryResolutionService.resolveCategoryFromMlClass("ACADEMIC", deptId);
            assertThat(categoryOpt).isPresent();
            assertThat(categoryOpt.get().getMlClass()).isEqualTo("ACADEMIC");
            assertThat(categoryOpt.get().getDepartment().getDepartmentId()).isEqualTo(deptId);
        }
    }
}
