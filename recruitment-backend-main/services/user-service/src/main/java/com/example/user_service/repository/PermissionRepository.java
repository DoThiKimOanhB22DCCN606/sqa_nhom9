package com.example.user_service.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.user_service.model.Permission;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
        boolean existsByRoles_IdAndNameAndActiveTrue(Long roleId, String name);

        boolean existsByName(String name);

        List<Permission> findByIdIn(List<Long> ids);

        @Query("SELECT p FROM Permission p WHERE "
                        + "(:isActive IS NULL OR p.active = :isActive) AND "
                        + "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
        Page<Permission> findByFilters(@Param("isActive") Boolean isActive,
                        @Param("keyword") String keyword,
                        Pageable pageable);

        @Query("SELECT p FROM Permission p WHERE "
                        + "(:isActive IS NULL OR p.active = :isActive) AND "
                        + "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
        List<Permission> findByFiltersNoPage(@Param("isActive") Boolean isActive,
                        @Param("keyword") String keyword);
}
