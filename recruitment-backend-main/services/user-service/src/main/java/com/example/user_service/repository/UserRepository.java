package com.example.user_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.user_service.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
        User findByEmail(String email);

        User findByRefreshTokenAndEmail(String token, String email);

        Optional<User> findById(Long id);

        @Query("""
                SELECT DISTINCT u FROM User u
                LEFT JOIN u.employee e
                LEFT JOIN e.department d
                WHERE (:departmentId IS NULL OR d.id = :departmentId)
                AND (:role IS NULL OR u.role.name = :role)
                AND (:isActive IS NULL OR u.is_active = :isActive)
                AND (
                        :keyword IS NULL OR
                        LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                        (e.name IS NOT NULL AND LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR
                        (e.phone IS NOT NULL AND LOWER(e.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR
                        (e.email IS NOT NULL AND LOWER(e.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR
                        (e.idNumber IS NOT NULL AND LOWER(e.idNumber) LIKE LOWER(CONCAT('%', :keyword, '%')))
                )
                """)
        Page<User> findByFilters(@Param("departmentId") Long departmentId,
                        @Param("role") String role,
                        @Param("isActive") Boolean isActive,
                        @Param("keyword") String keyword,
                        Pageable pageable);

        @Query("""
                SELECT DISTINCT u FROM User u
                LEFT JOIN u.employee e
                LEFT JOIN e.department d
                WHERE d.id IN :departmentIds
                """)
        List<User> findByDepartmentIds(@Param("departmentIds") List<Long> departmentIds);
}
