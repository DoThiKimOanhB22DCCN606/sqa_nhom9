package com.example.job_service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.job_service.model.JobPosition;
import com.example.job_service.utils.enums.JobPositionStatus;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface JobPositionRepository extends JpaRepository<JobPosition, Long> {
        List<JobPosition> findByStatus(JobPositionStatus status);

        List<JobPosition> findByStatusAndDeadlineAfter(JobPositionStatus status, LocalDate date);

        @Query("SELECT jp FROM JobPosition jp WHERE jp.recruitmentRequest.departmentId = :departmentId")
        Page<JobPosition> findByDepartmentId(@Param("departmentId") Long departmentId, Pageable pageable);

        @Query("SELECT jp FROM JobPosition jp WHERE jp.recruitmentRequest.departmentId = :departmentId AND jp.status = :status")
        List<JobPosition> findByDepartmentIdAndStatus(@Param("departmentId") Long departmentId,
                        @Param("status") JobPositionStatus status);

        Page<JobPosition> findByStatus(JobPositionStatus status, Pageable pageable);

        @Query("SELECT jp FROM JobPosition jp WHERE " +
                        "(:departmentId IS NULL OR jp.recruitmentRequest.departmentId = :departmentId) AND " +
                        "(:status IS NULL OR jp.status = :status) AND " +
                        "(:published IS NULL OR jp.status = 'PUBLISHED') AND " +
                        "(:keyword IS NULL OR LOWER(jp.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(jp.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(jp.requirements) LIKE LOWER(CONCAT('%', :keyword, '%')))")
        Page<JobPosition> findByFilters(@Param("departmentId") Long departmentId,
                        @Param("status") JobPositionStatus status,
                        @Param("published") Boolean published,
                        @Param("keyword") String keyword,
                        Pageable pageable);

        List<JobPosition> findByIdIn(List<Long> ids);
}
