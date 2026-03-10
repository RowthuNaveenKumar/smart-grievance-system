package com.sgms.sgms_backend.repository;

import com.sgms.sgms_backend.enums.ComplaintCategory;
import com.sgms.sgms_backend.model.EscalationMatrix;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EscalationMatrixRepository extends JpaRepository<EscalationMatrix, Long> {

    List<EscalationMatrix> findByCategoryOrderByLevelAsc(ComplaintCategory category);

    Optional<EscalationMatrix> findByCategoryAndLevel(ComplaintCategory  category, Integer level);
}