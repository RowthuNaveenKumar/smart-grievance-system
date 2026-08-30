package com.sgms.sgms_backend;

import com.sgms.sgms_backend.model.Complaint;
import com.sgms.sgms_backend.model.ComplaintCategory;
import com.sgms.sgms_backend.model.Department;
import com.sgms.sgms_backend.model.MlClassConfig;
import com.sgms.sgms_backend.repository.ComplaintCategoryRepository;
import com.sgms.sgms_backend.repository.ComplaintRepository;
import com.sgms.sgms_backend.repository.DepartmentRepository;
import com.sgms.sgms_backend.repository.MlClassConfigRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class Phase2VerificationTest {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private ComplaintCategoryRepository complaintCategoryRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private MlClassConfigRepository mlClassConfigRepository;

    @Test
    @Transactional(readOnly = true)
    void verifyPhase2DatabaseAndEntities() {
        // 1. Verify MlClassConfigRepository reads all 8 rows
        List<MlClassConfig> configs = mlClassConfigRepository.findAll();
        System.out.println("ML Class Config count: " + configs.size());
        assertThat(configs).hasSize(8);
        for (MlClassConfig cfg : configs) {
            System.out.println(" - " + cfg.getMlClass() + " -> " + cfg.getResolutionType() + " (active=" + cfg.isActive() + ")");
            assertThat(cfg.isActive()).isTrue();
            assertThat(cfg.getResolutionType()).isNotNull();
        }

        // 2. Verify all 14 legacy departments load and are active
        List<Department> departments = departmentRepository.findAll();
        System.out.println("Department count: " + departments.size());
        assertThat(departments).hasSizeGreaterThanOrEqualTo(14);
        for (Department d : departments) {
            if (d.getDepartmentId() <= 14) {
                assertThat(d.isActive()).isTrue();
            }
        }

        // 3. Verify all 14 complaint categories load, are active, have mlClass
        List<ComplaintCategory> categories = complaintCategoryRepository.findAllWithDepartment();
        System.out.println("Category count: " + categories.size());
        assertThat(categories).hasSize(14);
        for (ComplaintCategory c : categories) {
            assertThat(c.isActive()).isTrue();
            assertThat(c.getMlClass()).isNotNull().isNotBlank();
            assertThat(c.getDepartment()).isNotNull();
            assertThat(c.getDepartment().getName()).isNotBlank();
        }

        // 4. Verify all complaints load with relationships and ml audit metadata
        List<Complaint> complaints = complaintRepository.findAll();

        System.out.println("Complaint count: " + complaints.size());

        assertThat(complaints).isNotEmpty();

        int countWithCategory = 0;
        int countWithDepartment = 0;
        int countWithMlPredictedClass = 0;

        for (Complaint comp : complaints) {

            // Every complaint must have a student
            assertThat(comp.getStudent()).isNotNull();

            if (comp.getCategory() != null) {
                countWithCategory++;
            }

            if (comp.getDepartment() != null) {
                countWithDepartment++;
            }

            if (comp.getMlPredictedClass() != null) {
                countWithMlPredictedClass++;

                System.out.println("Complaint #" + comp.getComplaintId() + " mlPredictedClass: " + comp.getMlPredictedClass());
            }
        }

// Every complaint must have category and department
        assertThat(countWithCategory).isEqualTo(complaints.size());
        assertThat(countWithDepartment).isEqualTo(complaints.size());

// Current baseline expectation: 2 complaints have ML prediction metadata
        assertThat(countWithMlPredictedClass).isEqualTo(2);
        // 5. Verify findAllActiveWithDepartment() returns only active categories with active departments
        List<ComplaintCategory> activeCategories = complaintCategoryRepository.findAllActiveWithDepartment();
        System.out.println("Active categories with active department count: " + activeCategories.size());
        assertThat(activeCategories).hasSize(14);
        for (ComplaintCategory ac : activeCategories) {
            assertThat(ac.isActive()).isTrue();
            assertThat(ac.getDepartment().isActive()).isTrue();
        }

        System.out.println("Backend entity, repository, and resolution verification completed successfully!");
    }
}
