package com.example.user_service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.user_service.model.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
        @Query("SELECT r FROM Role r WHERE " +
                        "(:isActive IS NULL OR r.is_active = :isActive) AND " +
                        "(:keyword IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
        Page<Role> findByFilters(@Param("isActive") Boolean isActive,
                        @Param("keyword") String keyword,
                        Pageable pageable);

        boolean existsByName(String name);
}
