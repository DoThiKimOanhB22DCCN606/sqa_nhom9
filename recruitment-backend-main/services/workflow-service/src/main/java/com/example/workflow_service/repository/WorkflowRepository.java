package com.example.workflow_service.repository;

import com.example.workflow_service.model.Workflow;
import com.example.workflow_service.utils.enums.WorkflowType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, Long> {

        Optional<Workflow> findByName(String name);

        List<Workflow> findByTypeAndIsActiveTrue(WorkflowType type);

        List<Workflow> findByIsActiveTrue();

        Page<Workflow> findByIsActiveTrue(Pageable pageable);

        @Query(value = "SELECT * FROM workflows w WHERE " +
                        "(:type IS NULL OR w.type = :type) AND " +
                        "(:isActive IS NULL OR w.is_active = :isActive) AND " +
                        "(:keyword IS NULL OR LOWER(w.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(w.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND "
                        +
                        "(:departmentId IS NULL OR w.department_id = :departmentId)", countQuery = "SELECT COUNT(*) FROM workflows w WHERE "
                                        +
                                        "(:type IS NULL OR w.type = :type) AND " +
                                        "(:isActive IS NULL OR w.is_active = :isActive) AND " +
                                        "(:keyword IS NULL OR LOWER(w.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(w.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND "
                                        +
                                        "(:departmentId IS NULL OR w.department_id = :departmentId)", nativeQuery = true)
        Page<Workflow> findByFilters(
                        @Param("type") String type,
                        @Param("isActive") Boolean isActive,
                        @Param("keyword") String keyword,
                        @Param("departmentId") Long departmentId,
                        Pageable pageable);

        // Tìm workflow phù hợp dựa trên department_id
        @Query(value = "SELECT * FROM workflows w WHERE " +
                        "w.is_active = true AND " +
                        "w.department_id = :departmentId " +
                        "LIMIT 1", nativeQuery = true)
        Optional<Workflow> findMatchingWorkflow(
                        @Param("departmentId") Long departmentId);
}
