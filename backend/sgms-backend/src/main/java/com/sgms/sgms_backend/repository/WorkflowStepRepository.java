package com.sgms.sgms_backend.repository;

import com.sgms.sgms_backend.model.WorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkflowStepRepository extends JpaRepository<WorkflowStep,Long> {
    Optional<WorkflowStep> findByWorkflowWorkflowIdAndLevel(Long workflowId, Integer level);
}
