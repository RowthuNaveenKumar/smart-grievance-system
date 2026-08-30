package com.sgms.sgms_backend.repository;

import com.sgms.sgms_backend.model.WorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, Long> {

    Optional<WorkflowStep> findByWorkflowWorkflowIdAndLevel(Long workflowId, Integer level);

    List<WorkflowStep> findByWorkflowWorkflowIdOrderByLevelAsc(Long workflowId);

    long countByWorkflowWorkflowId(Long workflowId);

    boolean existsByWorkflowWorkflowIdAndLevel(Long workflowId, Integer level);

    boolean existsByWorkflowWorkflowIdAndLevelAndStepIdNot(Long workflowId, Integer level, Long stepId);
}
