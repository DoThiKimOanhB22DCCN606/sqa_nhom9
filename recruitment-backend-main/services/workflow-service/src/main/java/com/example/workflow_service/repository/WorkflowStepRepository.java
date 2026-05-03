package com.example.workflow_service.repository;

import com.example.workflow_service.model.WorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, Long> {

    List<WorkflowStep> findByWorkflowIdOrderByStepOrderAsc(Long workflowId);

    List<WorkflowStep> findByWorkflowIdAndIsActiveTrueOrderByStepOrderAsc(Long workflowId);

    Optional<WorkflowStep> findByWorkflowIdAndStepOrder(Long workflowId, Integer stepOrder);

    void deleteByWorkflowId(Long workflowId);
}
