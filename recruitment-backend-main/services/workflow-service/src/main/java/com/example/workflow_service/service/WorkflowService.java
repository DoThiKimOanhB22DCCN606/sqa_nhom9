package com.example.workflow_service.service;

import com.example.workflow_service.dto.Meta;
import com.example.workflow_service.dto.PaginationDTO;
import com.example.workflow_service.dto.workflow.CreateWorkflowDTO;
import com.example.workflow_service.dto.workflow.CreateStepDTO;
import com.example.workflow_service.dto.workflow.UpdateWorkflowDTO;
import com.example.workflow_service.dto.workflow.WorkflowResponseDTO;
import com.example.workflow_service.dto.workflow.WorkflowStepResponseDTO;
import com.example.workflow_service.exception.CustomException;
import com.example.workflow_service.exception.IdInvalidException;
import com.example.workflow_service.model.Workflow;
import com.example.workflow_service.model.WorkflowStep;
import com.example.workflow_service.repository.WorkflowRepository;
import com.example.workflow_service.repository.WorkflowStepRepository;
import com.example.workflow_service.utils.SecurityUtil;
import com.example.workflow_service.utils.enums.WorkflowType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowStepRepository workflowStepRepository;
    private final UserService userService;

    @Transactional
    public WorkflowResponseDTO create(CreateWorkflowDTO dto) {
        // Kiểm tra tên workflow đã tồn tại
        if (workflowRepository.findByName(dto.getName()).isPresent()) {
            throw new CustomException("Tên workflow đã tồn tại: " + dto.getName());
        }

        // Validate hierarchyOrder của các steps nếu có
        if (dto.getSteps() != null && !dto.getSteps().isEmpty()) {
            // Thu thập tất cả position IDs từ steps
            List<Long> positionIds = dto.getSteps().stream()
                    .map(CreateStepDTO::getApproverPositionId)
                    .filter(positionId -> positionId != null)
                    .distinct()
                    .collect(Collectors.toList());
            System.out.println("positionIds: " + positionIds);
            if (!positionIds.isEmpty()) {
                // Lấy token từ SecurityContext
                String token = SecurityUtil.getCurrentUserJWT().orElse(null);

                // Gọi user-service để lấy hierarchyOrder của các positions
                Map<Long, Integer> positionHierarchyOrders = userService.getPositionHierarchyOrdersByIds(positionIds,
                        token);
                System.out.println("positionHierarchyOrders: " + positionHierarchyOrders);
                // Validate: các steps phải có hierarchyOrder tăng dần theo stepOrder
                // Sắp xếp steps theo stepOrder
                List<CreateStepDTO> sortedSteps = dto.getSteps().stream()
                        .sorted((s1, s2) -> Integer.compare(s1.getStepOrder(), s2.getStepOrder()))
                        .collect(Collectors.toList());

                Integer previousHierarchyOrder = null;
                for (CreateStepDTO stepDTO : sortedSteps) {
                    Long positionId = stepDTO.getApproverPositionId();
                    Integer hierarchyOrder = positionHierarchyOrders.get(positionId);

                    if (hierarchyOrder == null) {
                        throw new CustomException("Không tìm thấy hierarchyOrder cho position ID: " + positionId);
                    }

                    // Kiểm tra hierarchyOrder phải giảm dần (step sau phải có level >= step trước)
                    // hierarchyOrder thấp hơn = level cao hơn (CEO=1, Staff=4)
                    // Để đi từ level thấp lên cao, hierarchyOrder phải giảm dần
                    if (previousHierarchyOrder != null && hierarchyOrder > previousHierarchyOrder) {
                        throw new CustomException(
                                "Thứ tự hierarchy không hợp lệ: Step " + stepDTO.getStepOrder() +
                                        " có hierarchyOrder (" + hierarchyOrder + ") cao hơn step trước (" +
                                        previousHierarchyOrder
                                        + "). Workflow phải đi từ level thấp lên level cao (hierarchyOrder phải giảm dần).");
                    }

                    previousHierarchyOrder = hierarchyOrder;
                }
            }
        }

        Workflow workflow = new Workflow();
        workflow.setName(dto.getName());
        workflow.setDescription(dto.getDescription());
        workflow.setType(dto.getType());
        workflow.setDepartmentId(dto.getDepartmentId());
        workflow.setIsActive(true);
        workflow.setCreatedBy(SecurityUtil.extractEmployeeId());

        final Workflow savedWorkflow = workflowRepository.save(workflow);

        // Tạo các steps nếu có
        if (dto.getSteps() != null && !dto.getSteps().isEmpty()) {
            List<WorkflowStep> steps = dto.getSteps().stream()
                    .map(stepDTO -> {
                        WorkflowStep step = new WorkflowStep();
                        step.setWorkflow(savedWorkflow);
                        step.setStepOrder(stepDTO.getStepOrder());
                        step.setApproverPositionId(stepDTO.getApproverPositionId());
                        step.setIsActive(true);
                        return step;
                    })
                    .collect(Collectors.toList());
            workflowStepRepository.saveAll(steps);
        }

        return toResponseDTO(savedWorkflow);
    }

    @Transactional(readOnly = true)
    public WorkflowResponseDTO getById(Long id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy workflow với ID: " + id));
        return toResponseDTO(workflow);
    }

    @Transactional(readOnly = true)
    public PaginationDTO getAll(
            WorkflowType type,
            Boolean isActive,
            String keyword,
            Long departmentId,
            Pageable pageable) {
        String typeString = type != null ? type.name() : null;
        Page<Workflow> workflowPage = workflowRepository.findByFilters(
                typeString, isActive, keyword, departmentId, pageable);

        List<Workflow> workflows = workflowPage.getContent();

        // Thu thập tất cả position IDs từ tất cả workflows
        Set<Long> allPositionIds = workflows.stream()
                .filter(w -> w.getSteps() != null)
                .flatMap(w -> w.getSteps().stream())
                .map(WorkflowStep::getApproverPositionId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        // Lấy token từ SecurityContext
        String token = SecurityUtil.getCurrentUserJWT().orElse(null);
        // Gọi user-service một lần để lấy tất cả position names
        Map<Long, String> positionNamesMap = userService.getPositionNamesByIds(
                allPositionIds.stream().collect(Collectors.toList()),
                token);
        // Convert workflows sang DTOs với position names
        List<WorkflowResponseDTO> content = workflows.stream()
                .map(workflow -> toResponseDTO(workflow, positionNamesMap))
                .collect(Collectors.toList());

        PaginationDTO paginationDTO = new PaginationDTO();
        Meta meta = new Meta();
        meta.setPage(workflowPage.getNumber() + 1);
        meta.setPageSize(workflowPage.getSize());
        meta.setPages(workflowPage.getTotalPages());
        meta.setTotal(workflowPage.getTotalElements());
        paginationDTO.setMeta(meta);
        paginationDTO.setResult(content);

        return paginationDTO;
    }

    @Transactional
    public WorkflowResponseDTO update(Long id, UpdateWorkflowDTO dto) {
        // Load workflow với steps để đảm bảo relationship được load
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy workflow với ID: " + id));

        // Load steps để đảm bảo Set được khởi tạo
        if (workflow.getSteps() == null) {
            workflow.setSteps(new java.util.HashSet<>());
        }

        // Kiểm tra tên workflow nếu thay đổi
        if (dto.getName() != null && !dto.getName().equals(workflow.getName())) {
            if (workflowRepository.findByName(dto.getName()).isPresent()) {
                throw new CustomException("Tên workflow đã tồn tại: " + dto.getName());
            }
            workflow.setName(dto.getName());
        }

        if (dto.getDescription() != null) {
            workflow.setDescription(dto.getDescription());
        }
        if (dto.getType() != null) {
            workflow.setType(dto.getType());
        }
        if (dto.getDepartmentId() != null) {
            workflow.setDepartmentId(dto.getDepartmentId());
        }
        if (dto.getIsActive() != null) {
            workflow.setIsActive(dto.getIsActive());
        }
        workflow.setUpdatedBy(SecurityUtil.extractEmployeeId());

        // Validate hierarchyOrder của các steps nếu có
        if (dto.getSteps() != null && !dto.getSteps().isEmpty()) {
            // Thu thập tất cả position IDs từ steps
            List<Long> positionIds = dto.getSteps().stream()
                    .map(CreateStepDTO::getApproverPositionId)
                    .filter(positionId -> positionId != null)
                    .distinct()
                    .collect(Collectors.toList());

            if (!positionIds.isEmpty()) {
                // Lấy token từ SecurityContext
                String token = SecurityUtil.getCurrentUserJWT().orElse(null);

                // Gọi user-service để lấy hierarchyOrder của các positions
                Map<Long, Integer> positionHierarchyOrders = userService.getPositionHierarchyOrdersByIds(positionIds,
                        token);

                // Validate: các steps phải có hierarchyOrder tăng dần theo stepOrder
                // Sắp xếp steps theo stepOrder
                List<CreateStepDTO> sortedSteps = dto.getSteps().stream()
                        .sorted((s1, s2) -> Integer.compare(s1.getStepOrder(), s2.getStepOrder()))
                        .collect(Collectors.toList());

                Integer previousHierarchyOrder = null;
                for (CreateStepDTO stepDTO : sortedSteps) {
                    Long positionId = stepDTO.getApproverPositionId();
                    Integer hierarchyOrder = positionHierarchyOrders.get(positionId);

                    if (hierarchyOrder == null) {
                        throw new CustomException("Không tìm thấy hierarchyOrder cho position ID: " + positionId);
                    }

                    // Kiểm tra hierarchyOrder phải giảm dần (step sau phải có level >= step trước)
                    // hierarchyOrder thấp hơn = level cao hơn (CEO=1, Staff=4)
                    // Để đi từ level thấp lên cao, hierarchyOrder phải giảm dần
                    if (previousHierarchyOrder != null && hierarchyOrder > previousHierarchyOrder) {
                        throw new CustomException(
                                "Thứ tự hierarchy không hợp lệ: Step " + stepDTO.getStepOrder() +
                                        " có hierarchyOrder (" + hierarchyOrder + ") cao hơn step trước (" +
                                        previousHierarchyOrder
                                        + "). Workflow phải đi từ level thấp lên level cao (hierarchyOrder phải giảm dần).");
                    }

                    previousHierarchyOrder = hierarchyOrder;
                }
            }
        }

        // Cập nhật steps nếu có
        if (dto.getSteps() != null) {
            // Xóa tất cả steps cũ bằng cách clear Set
            // orphanRemoval = true sẽ tự động xóa các steps bị remove khỏi Set
            workflow.getSteps().clear();

            // Tạo các steps mới và thêm vào Set
            if (!dto.getSteps().isEmpty()) {
                for (CreateStepDTO stepDTO : dto.getSteps()) {
                    WorkflowStep step = new WorkflowStep();
                    step.setWorkflow(workflow);
                    step.setStepOrder(stepDTO.getStepOrder());
                    step.setApproverPositionId(stepDTO.getApproverPositionId());
                    step.setIsActive(true);
                    workflow.getSteps().add(step);
                }
            }
        }

        workflow = workflowRepository.save(workflow);
        return toResponseDTO(workflow);
    }

    @Transactional
    public void delete(Long id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy workflow với ID: " + id));

        // Soft delete
        workflow.setIsActive(false);
        workflowRepository.save(workflow);
    }

    private WorkflowResponseDTO toResponseDTO(Workflow workflow) {
        // Thu thập position IDs từ workflow này
        Set<Long> positionIds = workflow.getSteps() != null
                ? workflow.getSteps().stream()
                        .map(WorkflowStep::getApproverPositionId)
                        .filter(id -> id != null)
                        .collect(Collectors.toSet())
                : Set.of();

        // Lấy token từ SecurityContext
        String token = SecurityUtil.getCurrentUserJWT().orElse(null);

        // Gọi user-service để lấy position names
        Map<Long, String> positionNamesMap = userService.getPositionNamesByIds(
                positionIds.stream().collect(Collectors.toList()),
                token);

        return toResponseDTO(workflow, positionNamesMap);
    }

    private WorkflowResponseDTO toResponseDTO(Workflow workflow, Map<Long, String> positionNamesMap) {
        WorkflowResponseDTO dto = new WorkflowResponseDTO();
        dto.setId(workflow.getId());
        dto.setName(workflow.getName());
        dto.setDescription(workflow.getDescription());
        dto.setType(workflow.getType());
        dto.setDepartmentId(workflow.getDepartmentId());
        dto.setIsActive(workflow.getIsActive());
        dto.setCreatedBy(workflow.getCreatedBy());
        dto.setUpdatedBy(workflow.getUpdatedBy());
        dto.setCreatedAt(workflow.getCreatedAt());
        dto.setUpdatedAt(workflow.getUpdatedAt());

        if (workflow.getSteps() != null) {
            List<WorkflowStepResponseDTO> stepDTOs = workflow.getSteps().stream()
                    .sorted(java.util.Comparator.comparing(WorkflowStep::getStepOrder))
                    .map(step -> toStepResponseDTO(step, positionNamesMap))
                    .collect(Collectors.toList());
            dto.setSteps(stepDTOs);
        }

        return dto;
    }

    private WorkflowStepResponseDTO toStepResponseDTO(WorkflowStep step, Map<Long, String> positionNamesMap) {
        WorkflowStepResponseDTO dto = new WorkflowStepResponseDTO();
        dto.setId(step.getId());
        dto.setStepOrder(step.getStepOrder());
        dto.setApproverPositionId(step.getApproverPositionId());
        System.out.println("step.getApproverPositionId(): " + step.getApproverPositionId());
        System.out.println("positionNamesMap: " + positionNamesMap);

        // Lấy position name từ map
        if (step.getApproverPositionId() != null) {
            String positionName = positionNamesMap.get(step.getApproverPositionId());
            dto.setApproverPositionName(positionName);
        }

        dto.setIsActive(step.getIsActive());
        dto.setCreatedAt(step.getCreatedAt());
        dto.setUpdatedAt(step.getUpdatedAt());
        return dto;
    }
}
