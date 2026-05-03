package com.example.user_service.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.user_service.model.Department;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

        @Query("SELECT d FROM Department d WHERE " +
                        "(:isActive IS NULL OR d.is_active = :isActive) AND " +
                        "(:keyword IS NULL OR LOWER(d.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(d.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
        Page<Department> findByFilters(@Param("isActive") Boolean isActive,
                        @Param("keyword") String keyword,
                        Pageable pageable);

        Optional<Department> findByCode(String code);
}
