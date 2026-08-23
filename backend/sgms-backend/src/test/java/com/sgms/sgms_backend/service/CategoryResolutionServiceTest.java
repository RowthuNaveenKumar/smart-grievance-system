package com.sgms.sgms_backend.service;

import com.sgms.sgms_backend.dto.CategorySuggestionResponse;
import com.sgms.sgms_backend.dto.MLResponse;
import com.sgms.sgms_backend.enums.MlResolutionType;
import com.sgms.sgms_backend.exception.NotFoundException;
import com.sgms.sgms_backend.exception.ValidationException;
import com.sgms.sgms_backend.model.AcademicDivision;
import com.sgms.sgms_backend.model.ComplaintCategory;
import com.sgms.sgms_backend.model.Department;
import com.sgms.sgms_backend.model.MlClassConfig;
import com.sgms.sgms_backend.model.StudentInfo;
import com.sgms.sgms_backend.repository.ComplaintCategoryRepository;
import com.sgms.sgms_backend.repository.MlClassConfigRepository;
import com.sgms.sgms_backend.service.resolution.CategoryResolutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryResolutionServiceTest {

    @Mock
    private ComplaintCategoryRepository categoryRepo;

    @Mock
    private MlClassConfigRepository mlClassConfigRepo;

    @InjectMocks
    private CategoryResolutionService resolutionService;

    private Department cseDept;
    private Department eceDept;
    private Department hostelDept;
    private ComplaintCategory cseAcademicCat;
    private ComplaintCategory eceAcademicCat;
    private ComplaintCategory hostelCat;
    private MlClassConfig academicConfig;
    private MlClassConfig hostelConfig;
    private StudentInfo cseStudent;
    private StudentInfo eceStudent;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(resolutionService, "mlConfidenceThreshold", 0.60);

        cseDept = new Department();
        cseDept.setDepartmentId(1L);
        cseDept.setName("CSE");
        cseDept.setActive(true);

        eceDept = new Department();
        eceDept.setDepartmentId(2L);
        eceDept.setName("ECE");
        eceDept.setActive(true);

        hostelDept = new Department();
        hostelDept.setDepartmentId(11L);
        hostelDept.setName("Hostel");
        hostelDept.setActive(true);

        cseAcademicCat = new ComplaintCategory();
        cseAcademicCat.setCategoryId(1L);
        cseAcademicCat.setName("ACADEMIC");
        cseAcademicCat.setMlClass("ACADEMIC");
        cseAcademicCat.setDepartment(cseDept);
        cseAcademicCat.setActive(true);

        eceAcademicCat = new ComplaintCategory();
        eceAcademicCat.setCategoryId(2L);
        eceAcademicCat.setName("ACADEMIC");
        eceAcademicCat.setMlClass("ACADEMIC");
        eceAcademicCat.setDepartment(eceDept);
        eceAcademicCat.setActive(true);

        hostelCat = new ComplaintCategory();
        hostelCat.setCategoryId(11L);
        hostelCat.setName("HOSTEL");
        hostelCat.setMlClass("HOSTEL");
        hostelCat.setDepartment(hostelDept);
        hostelCat.setActive(true);

        academicConfig = new MlClassConfig();
        academicConfig.setMlClass("ACADEMIC");
        academicConfig.setResolutionType(MlResolutionType.STUDENT_DEPT);
        academicConfig.setActive(true);

        hostelConfig = new MlClassConfig();
        hostelConfig.setMlClass("HOSTEL");
        hostelConfig.setResolutionType(MlResolutionType.DIRECT_SINGLE);
        hostelConfig.setActive(true);

        AcademicDivision cseDiv = new AcademicDivision();
        cseDiv.setDivisionId(1L);
        cseDiv.setName("CSE-A");
        cseDiv.setDepartment(cseDept);
        cseStudent = new StudentInfo();
        cseStudent.setStudentId(101);
        cseStudent.setAcademicDivision(cseDiv);

        AcademicDivision eceDiv = new AcademicDivision();
        eceDiv.setDivisionId(2L);
        eceDiv.setName("ECE-A");
        eceDiv.setDepartment(eceDept);
        eceStudent = new StudentInfo();
        eceStudent.setStudentId(102);
        eceStudent.setAcademicDivision(eceDiv);
    }

    // A. CSE student + ACADEMIC ML -> CSE ACADEMIC category
    @Test
    void testA_cseStudentWithAcademicMlResolvesCseAcademicCategory() {
        when(mlClassConfigRepo.findByMlClassAndActiveTrue("ACADEMIC")).thenReturn(Optional.of(academicConfig));
        when(categoryRepo.findByMlClassAndDepartmentIdAndActiveTrue("ACADEMIC", 1L))
                .thenReturn(List.of(cseAcademicCat));

        Optional<ComplaintCategory> result = resolutionService.resolveCategoryFromMlClass("ACADEMIC", 1L);

        assertThat(result).isPresent();
        assertThat(result.get().getCategoryId()).isEqualTo(1L);
        assertThat(result.get().getDepartment().getName()).isEqualTo("CSE");
    }

    // B. ECE student + ACADEMIC ML -> ECE ACADEMIC category
    @Test
    void testB_eceStudentWithAcademicMlResolvesEceAcademicCategory() {
        when(mlClassConfigRepo.findByMlClassAndActiveTrue("ACADEMIC")).thenReturn(Optional.of(academicConfig));
        when(categoryRepo.findByMlClassAndDepartmentIdAndActiveTrue("ACADEMIC", 2L))
                .thenReturn(List.of(eceAcademicCat));

        Optional<ComplaintCategory> result = resolutionService.resolveCategoryFromMlClass("ACADEMIC", 2L);

        assertThat(result).isPresent();
        assertThat(result.get().getCategoryId()).isEqualTo(2L);
        assertThat(result.get().getDepartment().getName()).isEqualTo("ECE");
    }

    // C. CSE student + HOSTEL ML -> HOSTEL category (DIRECT_SINGLE)
    @Test
    void testC_cseStudentWithHostelMlResolvesHostelCategory() {
        when(mlClassConfigRepo.findByMlClassAndActiveTrue("HOSTEL")).thenReturn(Optional.of(hostelConfig));
        when(categoryRepo.findByMlClassAndActiveTrue("HOSTEL")).thenReturn(List.of(hostelCat));

        Optional<ComplaintCategory> result = resolutionService.resolveCategoryFromMlClass("HOSTEL", 1L);

        assertThat(result).isPresent();
        assertThat(result.get().getCategoryId()).isEqualTo(11L);
        assertThat(result.get().getName()).isEqualTo("HOSTEL");
    }

    // E. ML class unknown -> no categoryId + manual selection
    @Test
    void testE_unknownMlClassReturnsEmptyAndSuggestionRequiresManual() {
        when(mlClassConfigRepo.findByMlClassAndActiveTrue("INFRASTRUCTURE")).thenReturn(Optional.empty());

        MLResponse ml = new MLResponse();
        ml.setPredictedClass("INFRASTRUCTURE");
        ml.setConfidence(0.85);
        ml.setHighConfidence(true);

        CategorySuggestionResponse resp = resolutionService.buildSuggestionResponse(ml, cseStudent);

        assertThat(resp.getCategoryId()).isNull();
        assertThat(resp.getHighConfidence()).isFalse();
        assertThat(resp.getSuggestionNote()).contains("cannot be resolved");
    }

    // F. ML confidence below threshold -> no auto-selection (categoryId = null, highConfidence = false)
    @Test
    void testF_lowConfidenceReturnsNullCategoryIdAndFalseHighConfidence() {
        when(mlClassConfigRepo.findByMlClassAndActiveTrue("HOSTEL")).thenReturn(Optional.of(hostelConfig));
        when(categoryRepo.findByMlClassAndActiveTrue("HOSTEL")).thenReturn(List.of(hostelCat));

        MLResponse ml = new MLResponse();
        ml.setPredictedClass("HOSTEL");
        ml.setConfidence(0.45); // Below 0.60
        ml.setHighConfidence(false);

        CategorySuggestionResponse resp = resolutionService.buildSuggestionResponse(ml, cseStudent);

        assertThat(resp.getCategoryId()).isNull();
        assertThat(resp.getHighConfidence()).isFalse();
        assertThat(resp.getConfidenceScore()).isEqualTo(0.45);
        assertThat(resp.getSuggestionNote()).contains("confidence is low");
    }

    // G. ML service unavailable -> controlled manual-selection response
    @Test
    void testG_nullMlResponseReturnsControlledManualSelectionResponse() {
        CategorySuggestionResponse resp = resolutionService.buildSuggestionResponse(null, cseStudent);

        assertThat(resp.getCategoryId()).isNull();
        assertThat(resp.getHighConfidence()).isFalse();
        assertThat(resp.getSuggestionNote()).contains("manual");
    }

    // H. Inactive category selected manually -> rejected (ValidationException)
    @Test
    void testH_inactiveCategoryManuallySelectedThrowsValidationException() {
        ComplaintCategory inactiveCat = new ComplaintCategory();
        inactiveCat.setCategoryId(99L);
        inactiveCat.setActive(false);
        inactiveCat.setDepartment(cseDept);

        when(categoryRepo.findById(99L)).thenReturn(Optional.of(inactiveCat));

        assertThatThrownBy(() -> resolutionService.validateAndResolveCategoryById(99L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("inactive");
    }

    // I. Inactive department category -> rejected (ValidationException)
    @Test
    void testI_categoryWithInactiveDepartmentThrowsValidationException() {
        Department inactiveDept = new Department();
        inactiveDept.setDepartmentId(99L);
        inactiveDept.setActive(false);

        ComplaintCategory cat = new ComplaintCategory();
        cat.setCategoryId(88L);
        cat.setActive(true);
        cat.setDepartment(inactiveDept);

        when(categoryRepo.findById(88L)).thenReturn(Optional.of(cat));

        assertThatThrownBy(() -> resolutionService.validateAndResolveCategoryById(88L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Department is inactive");
    }

    // J. DIRECT_SINGLE class with zero active categories -> controlled empty result
    @Test
    void testJ_directSingleWithZeroActiveCategoriesReturnsEmpty() {
        when(mlClassConfigRepo.findByMlClassAndActiveTrue("HOSTEL")).thenReturn(Optional.of(hostelConfig));
        when(categoryRepo.findByMlClassAndActiveTrue("HOSTEL")).thenReturn(Collections.emptyList());

        Optional<ComplaintCategory> result = resolutionService.resolveCategoryFromMlClass("HOSTEL", 1L);

        assertThat(result).isEmpty();
    }

    // K. DIRECT_SINGLE class with multiple active categories -> data integrity error (IllegalStateException)
    @Test
    void testK_directSingleWithMultipleActiveCategoriesThrowsIllegalStateException() {
        ComplaintCategory duplicateHostel = new ComplaintCategory();
        duplicateHostel.setCategoryId(12L);
        duplicateHostel.setName("HOSTEL_2");
        duplicateHostel.setMlClass("HOSTEL");
        duplicateHostel.setActive(true);

        when(mlClassConfigRepo.findByMlClassAndActiveTrue("HOSTEL")).thenReturn(Optional.of(hostelConfig));
        when(categoryRepo.findByMlClassAndActiveTrue("HOSTEL")).thenReturn(List.of(hostelCat, duplicateHostel));

        assertThatThrownBy(() -> resolutionService.resolveCategoryFromMlClass("HOSTEL", 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Data integrity error: Multiple active categories");
    }

    // L. STUDENT_DEPT class with no category for student's department -> controlled empty result
    @Test
    void testL_studentDeptWithNoCategoryForDepartmentReturnsEmpty() {
        when(mlClassConfigRepo.findByMlClassAndActiveTrue("ACADEMIC")).thenReturn(Optional.of(academicConfig));
        when(categoryRepo.findByMlClassAndDepartmentIdAndActiveTrue("ACADEMIC", 999L))
                .thenReturn(Collections.emptyList());

        Optional<ComplaintCategory> result = resolutionService.resolveCategoryFromMlClass("ACADEMIC", 999L);

        assertThat(result).isEmpty();
    }

    // STUDENT_DEPT with multiple active categories -> data integrity error
    @Test
    void testStudentDeptWithMultipleActiveCategoriesThrowsIllegalStateException() {
        ComplaintCategory duplicateCat = new ComplaintCategory();
        duplicateCat.setCategoryId(99L);
        duplicateCat.setMlClass("ACADEMIC");
        duplicateCat.setActive(true);

        when(mlClassConfigRepo.findByMlClassAndActiveTrue("ACADEMIC")).thenReturn(Optional.of(academicConfig));
        when(categoryRepo.findByMlClassAndDepartmentIdAndActiveTrue("ACADEMIC", 1L))
                .thenReturn(List.of(cseAcademicCat, duplicateCat));

        assertThatThrownBy(() -> resolutionService.resolveCategoryFromMlClass("ACADEMIC", 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Data integrity error: Multiple active categories");
    }
}
