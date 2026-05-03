package com.example.user_service.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.user_service.model.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    @Query("""
            SELECT DISTINCT e FROM Employee e
            LEFT JOIN e.department d
            LEFT JOIN e.position p
            WHERE (:departmentId IS NULL OR d.id = :departmentId)
            AND (:positionId IS NULL OR p.id = :positionId)
            AND (:status IS NULL OR e.status = :status)
            AND (
                :keyword IS NULL OR
                LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(e.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                (e.phone IS NOT NULL AND LOWER(e.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR
                (e.idNumber IS NOT NULL AND LOWER(e.idNumber) LIKE LOWER(CONCAT('%', :keyword, '%')))
            )
            """)
    Page<Employee> findByFilters(@Param("departmentId") Long departmentId,
            @Param("positionId") Long positionId,
            @Param("status") String status,
            @Param("keyword") String keyword,
            Pageable pageable);
    @Query("""
            SELECT DISTINCT e FROM Employee e
            LEFT JOIN e.department d
            WHERE d.id IN :departmentIds
            """)
    List<Employee> findByDepartmentIds(@Param("departmentIds") List<Long> departmentIds);
}
