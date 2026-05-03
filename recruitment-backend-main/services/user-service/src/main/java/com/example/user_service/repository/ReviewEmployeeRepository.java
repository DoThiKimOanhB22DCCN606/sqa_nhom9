package com.example.user_service.repository;

import com.example.user_service.model.ReviewEmployee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReviewEmployeeRepository extends JpaRepository<ReviewEmployee, Long> {

        List<ReviewEmployee> findByEmployee_Id(Long employeeId);

        List<ReviewEmployee> findByReviewerId(Long reviewerId);

        List<ReviewEmployee> findByEmployee_IdAndReviewerId(Long employeeId, Long reviewerId);

        @Query("SELECT r FROM ReviewEmployee r WHERE " +
                        "(:employeeId IS NULL OR r.employee.id = :employeeId) AND " +
                        "(:reviewerId IS NULL OR r.reviewerId = :reviewerId) AND " +
                        "(:startDate IS NULL OR r.createdAt >= :startDate) AND " +
                        "(:endDate IS NULL OR r.createdAt <= :endDate)")
        Page<ReviewEmployee> findByFilters(@Param("employeeId") Long employeeId,
                        @Param("reviewerId") Long reviewerId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        Pageable pageable);
}
