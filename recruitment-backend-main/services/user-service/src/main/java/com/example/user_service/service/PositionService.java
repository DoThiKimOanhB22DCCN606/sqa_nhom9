package com.example.user_service.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.user_service.dto.Meta;
import com.example.user_service.dto.PaginationDTO;
import com.example.user_service.dto.position.CreatePositionDTO;
import com.example.user_service.dto.position.UpdatePositionDTO;
import com.example.user_service.model.Position;
import com.example.user_service.repository.PositionRepository;

@Service
public class PositionService {
    private final PositionRepository positionRepository;

    public PositionService(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    public PaginationDTO getAll(Pageable pageable) {
        Page<Position> pagePosition = this.positionRepository.findAll(pageable);
        PaginationDTO rs = new PaginationDTO();
        Meta mt = new Meta();
        mt.setPage(pagePosition.getNumber() + 1);
        mt.setPageSize(pagePosition.getSize());
        mt.setPages(pagePosition.getTotalPages());
        mt.setTotal(pagePosition.getTotalElements());
        rs.setMeta(mt);
        rs.setResult(pagePosition.getContent());
        return rs;
    }

    public Position create(CreatePositionDTO createPositionDTO) {
        Position position = new Position();
        position.setName(createPositionDTO.getName());
        position.setLevel(createPositionDTO.getLevel());
        position.setHierarchyOrder(createPositionDTO.getHierarchyOrder());
        position.setActive(createPositionDTO.getIsActive() != null ? createPositionDTO.getIsActive() : true);
        return this.positionRepository.save(position);
    }

    public Position update(Long id, UpdatePositionDTO updatePositionDTO) {
        Position position = this.getById(id);

        if (updatePositionDTO.getName() != null) {
            position.setName(updatePositionDTO.getName());
        }
        if (updatePositionDTO.getLevel() != null) {
            position.setLevel(updatePositionDTO.getLevel());
        }
        if (updatePositionDTO.getHierarchyOrder() != null) {
            position.setHierarchyOrder(updatePositionDTO.getHierarchyOrder());
        }
        if (updatePositionDTO.getIsActive() != null) {
            position.setActive(updatePositionDTO.getIsActive());
        }

        return this.positionRepository.save(position);
    }

    public void delete(Long id) {
        this.positionRepository.deleteById(id);
    }

    public Position getById(Long id) {
        return this.positionRepository.findById(id)
                .orElse(null);
    }

    public PaginationDTO getAllWithFilters(Boolean isActive, String keyword, Pageable pageable) {
        Page<Position> pagePosition = this.positionRepository.findByFilters(isActive, keyword, pageable);
        PaginationDTO rs = new PaginationDTO();
        Meta mt = new Meta();
        mt.setPage(pagePosition.getNumber() + 1);
        mt.setPageSize(pagePosition.getSize());
        mt.setPages(pagePosition.getTotalPages());
        mt.setTotal(pagePosition.getTotalElements());
        rs.setMeta(mt);
        rs.setResult(pagePosition.getContent());
        return rs;
    }

    public List<Position> getByIds(List<Long> ids) {
        return this.positionRepository.findAllById(ids);
    }
}
