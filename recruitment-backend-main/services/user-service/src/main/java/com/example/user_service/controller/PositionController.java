package com.example.user_service.controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.user_service.dto.PaginationDTO;
import com.example.user_service.dto.position.CreatePositionDTO;
import com.example.user_service.dto.position.UpdatePositionDTO;
import com.example.user_service.model.Position;
import com.example.user_service.service.PositionService;
import com.example.user_service.utils.annotation.ApiMessage;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/user-service/positions")
public class PositionController {
    private final PositionService positionService;

    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }

    @GetMapping
    @ApiMessage("Lấy danh sách vị trí")
    public ResponseEntity<PaginationDTO> findAll(
            @RequestParam(name = "isActive", required = false) Boolean isActive,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "1", required = false) int page,
            @RequestParam(name = "limit", defaultValue = "10", required = false) int limit,
            @RequestParam(name = "sortBy", defaultValue = "id", required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = "desc", required = false) String sortOrder) {

        if (page < 1)
            page = 1;
        if (limit < 1 || limit > 100)
            limit = 10;

        Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page - 1, limit, sort);

        return ResponseEntity.status(HttpStatus.OK)
                .body(this.positionService.getAllWithFilters(isActive, keyword, pageable));
    }

    @PostMapping
    @ApiMessage("Tạo mới vị trí")
    public ResponseEntity<Position> create(@Valid @RequestBody CreatePositionDTO createPositionDTO) {
        Position position = this.positionService.create(createPositionDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(position);
    }

    @PutMapping("/{id}")
    @ApiMessage("Cập nhật vị trí")
    public ResponseEntity<Position> update(@PathVariable Long id,
            @RequestBody UpdatePositionDTO updatePositionDTO) {
        Position position = this.positionService.update(id, updatePositionDTO);
        return ResponseEntity.ok(position);
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Xóa vị trí")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        this.positionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @ApiMessage("Lấy vị trí theo ID")
    public ResponseEntity<Position> findById(@PathVariable Long id) {
        Position position = this.positionService.getById(id);
        return ResponseEntity.ok(position);
    }

    @GetMapping(params = "ids")
    @ApiMessage("Lấy danh sách vị trí theo IDs")
    public ResponseEntity<List<Position>> findByIds(@RequestParam("ids") String ids) {
        List<Long> positionIds = List.of(ids.split(","))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();
        return ResponseEntity.ok(this.positionService.getByIds(positionIds));
    }
}
