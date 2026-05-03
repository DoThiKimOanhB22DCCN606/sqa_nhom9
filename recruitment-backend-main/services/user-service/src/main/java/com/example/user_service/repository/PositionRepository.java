package com.example.user_service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.user_service.model.Position;

public interface PositionRepository extends JpaRepository<Position, Long> {

    @Query("""
            SELECT p FROM Position p
            WHERE (:isActive IS NULL OR p.isActive = :isActive)
            AND (
                :keyword IS NULL OR
                LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                (p.level IS NOT NULL AND LOWER(p.level) LIKE LOWER(CONCAT('%', :keyword, '%')))
            )
            """)
    Page<Position> findByFilters(@Param("isActive") Boolean isActive,
            @Param("keyword") String keyword,
            Pageable pageable);
}
