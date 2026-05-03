package com.example.workflow_service.service;

import com.example.workflow_service.dto.PaginationDTO;
import com.example.workflow_service.dto.Response;
import com.example.workflow_service.dto.approval.ApprovalTrackingResponseDTO;
import com.example.workflow_service.dto.approval.ApproveStepDTO;
import com.example.workflow_service.dto.approval.CreateApprovalTrackingDTO;
import com.example.workflow_service.dto.approval.RequestWorkflowInfoDTO;
import com.example.workflow_service.dto.workflow.WorkflowResponseDTO;
import com.example.workflow_service.dto.workflow.WorkflowStepResponseDTO;
import com.example.workflow_service.exception.CustomException;
import com.example.workflow_service.exception.IdInvalidException;
import com.example.workflow_service.model.ApprovalTracking;
import com.example.workflow_service.model.Workflow;
import com.example.workflow_service.model.WorkflowStep;
import com.example.workflow_service.messaging.RecruitmentWorkflowEvent;
import com.example.workflow_service.messaging.NotificationProducer;
import com.example.workflow_service.messaging.RecruitmentWorkflowProducer;
import com.example.workflow_service.repository.ApprovalTrackingRepository;
import com.example.workflow_service.repository.WorkflowRepository;
import com.example.workflow_service.repository.WorkflowStepRepository;
import com.example.workflow_service.utils.SecurityUtil;
import com.example.workflow_service.utils.enums.ApprovalStatus;
import com.example.workflow_service.utils.enums.WorkflowType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApprovalTrackingService {

    private final ApprovalTrackingRepository approvalTrackingRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowStepRepository workflowStepRepository;
    private final RecruitmentWorkflowProducer workflowProducer;
    private final NotificationProducer notificationProducer;
    private final ObjectMapper objectMapper;
    private final UserService userService;
    private final CandidateService candidateService;
    private static final Logger log = LoggerFactory.getLogger(ApprovalTrackingService.class);

    private final RestTemplate restTemplate;

    @Value("${services.user-service.url:http://localhost:8081}")
    private String userServiceBaseUrl;

    @Value("${services.job-service.url:http://localhost:8083}")
    private String jobServiceBaseUrl;

    /**
     * Bước 1-4: Tạo yêu cầu và khởi tạo bước đầu tiên
     * Tự động tìm workflow phù hợp, tìm người duyệt và tạo ApprovalTracking
     */
    @Transactional
    public ApprovalTrackingResponseDTO initializeApproval(CreateApprovalTrackingDTO dto) {
        // Bước 2: Tìm workflow phù hợp
        Workflow workflow = workflowRepository.findMatchingWorkflow(
                dto.getDepartmentId())
                .orElseThrow(() -> new CustomException(
                        "Không tìm thấy workflow phù hợp với department_id: " + dto.getDepartmentId()));

        // Bước 3: Lấy bước đầu tiên
        WorkflowStep firstStep = workflowStepRepository
                .findByWorkflowIdAndStepOrder(workflow.getId(), 1)
                .orElseThrow(() -> new CustomException(
                        "Workflow không có bước đầu tiên"));

        // Bước 4: Tìm người duyệt dựa trên position_id
        Long assignedUserId = findUserByPositionId(firstStep.getApproverPositionId());
        if (assignedUserId == null) {
            throw new CustomException(
                    "Không tìm thấy người dùng nào giữ vị trí position_id: " + firstStep.getApproverPositionId());
        }

        // Tìm actionUserId dựa trên levelId và departmentId
        String token = SecurityUtil.getCurrentUserJWT().orElse(null);
        Long actionUserId = userService.findUserByPositionIdAndDepartmentId(dto.getLevelId(), dto.getDepartmentId(),
                token);

        // Tạo ApprovalTracking
        ApprovalTracking tracking = new ApprovalTracking();
        tracking.setRequestId(dto.getRequestId());
        tracking.setStep(firstStep);
        tracking.setStatus(ApprovalStatus.PENDING);
        tracking.setApproverPositionId(assignedUserId);
        tracking.setActionUserId(actionUserId); // Lưu actionUserId ngay khi tạo
        tracking.setActionAt(null);

        tracking = approvalTrackingRepository.save(tracking);

        notifyNextApprovers(dto.getDepartmentId(), firstStep, dto.getRequestId(), null);

        // Lấy user name nếu có actionUserId (đã được lưu vào DB)
        Map<Long, String> userNamesMap = null;
        if (tracking.getActionUserId() != null) {
            userNamesMap = userService.getUserNamesByIds(
                    List.of(tracking.getActionUserId()),
                    token);
        }

        // Lấy position name nếu có assignedUserId
        Map<Long, String> positionNamesByUserIdMap = null;
        if (tracking.getApproverPositionId() != null) {
            positionNamesByUserIdMap = userService.getPositionNamesByIds(
                    List.of(tracking.getApproverPositionId()),
                    token);
        }

        return toResponseDTO(tracking, null, userNamesMap, positionNamesByUserIdMap);
    }

    /**
     * Bước 5: Người dùng phê duyệt/từ chối
     */
    @Transactional
    public ApprovalTrackingResponseDTO approve(Long id, ApproveStepDTO dto) {
        ApprovalTracking tracking = approvalTrackingRepository.findById(id)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy approval tracking với ID: " + id));

        // Kiểm tra quyền
        Long currentUserId = SecurityUtil.extractEmployeeId();
        if (!tracking.getApproverPositionId().equals(currentUserId)) {
            throw new CustomException("Bạn không có quyền phê duyệt bước này");
        }

        // Kiểm tra trạng thái
        if (tracking.getStatus() != ApprovalStatus.PENDING) {
            throw new CustomException("Bước này đã được xử lý");
        }

        if (dto.getApproved()) {
            // Approve
            tracking.setStatus(ApprovalStatus.APPROVED);
            tracking.setActionUserId(currentUserId);
            tracking.setActionAt(LocalDateTime.now());
            tracking.setNotes(dto.getApprovalNotes());
            approvalTrackingRepository.save(tracking);

            // Bước 6: Chuyển sang bước tiếp theo
            moveToNextStep(tracking, null, null);
        } else {
            // Reject
            tracking.setStatus(ApprovalStatus.REJECTED);
            tracking.setActionUserId(currentUserId);
            tracking.setActionAt(LocalDateTime.now());
            tracking.setNotes(dto.getApprovalNotes());
            approvalTrackingRepository.save(tracking);
        }

        String token = SecurityUtil.getCurrentUserJWT().orElse(null);

        // Lấy user name nếu có actionUserId
        Map<Long, String> userNamesMap = null;
        if (tracking.getActionUserId() != null) {
            userNamesMap = userService.getUserNamesByIds(
                    List.of(tracking.getActionUserId()),
                    token);
        }

        // Lấy position name nếu có assignedUserId
        Map<Long, String> positionNamesByUserIdMap = null;
        if (tracking.getApproverPositionId() != null) {
            positionNamesByUserIdMap = userService.getPositionNamesByIds(
                    List.of(tracking.getApproverPositionId()),
                    token);
        }

        return toResponseDTO(tracking, null, userNamesMap, positionNamesByUserIdMap);
    }

    /**
     * Tìm user_id dựa trên position_id (gọi RBAC service)
     */
    private Long findUserByPositionId(Long positionId) {
        try {
            String url = userServiceBaseUrl + "/api/v1/user-service/user-positions/by-position/" + positionId;

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            // Có thể thêm JWT token nếu cần

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<Response<List<UserPositionDTO>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<Response<List<UserPositionDTO>>>() {
                    });

            if (response.getBody() != null && response.getBody().getData() != null) {
                List<UserPositionDTO> userPositions = response.getBody().getData();
                if (!userPositions.isEmpty()) {
                    // Lấy user đầu tiên (có thể cần logic phức tạp hơn)
                    return userPositions.get(0).getUserId();
                }
            }
            return null;
        } catch (Exception e) {
            // Log error
            return null;
        }
    }

    @Transactional(readOnly = true)
    public ApprovalTrackingResponseDTO getById(Long id) {
        ApprovalTracking tracking = approvalTrackingRepository.findById(id)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy approval tracking với ID: " + id));

        String token = SecurityUtil.getCurrentUserJWT().orElse(null);

        // Lấy user name nếu có actionUserId
        Map<Long, String> userNamesMap = null;
        if (tracking.getActionUserId() != null) {
            userNamesMap = userService.getUserNamesByIds(
                    List.of(tracking.getActionUserId()),
                    token);
        }

        // Lấy position name nếu có assignedUserId
        Map<Long, String> positionNamesByUserIdMap = null;
        if (tracking.getApproverPositionId() != null) {
            positionNamesByUserIdMap = userService.getPositionNamesByIds(
                    List.of(tracking.getApproverPositionId()),
                    token);
        }

        return toResponseDTO(tracking, null, userNamesMap, positionNamesByUserIdMap);
    }

    @Transactional(readOnly = true)
    public PaginationDTO getAll(
            Long requestId,
            ApprovalStatus status,
            Long approverPositionId,
            Pageable pageable) {
        Page<ApprovalTracking> trackingPage = approvalTrackingRepository.findByFilters(
                requestId, status, approverPositionId, pageable);

        List<ApprovalTracking> trackings = trackingPage.getContent();

        // Thu thập tất cả user IDs từ trackings (actionUserId)
        Set<Long> allActionUserIds = trackings.stream()
                .map(ApprovalTracking::getActionUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        // Thu thập tất cả assigned user IDs từ trackings
        Set<Long> allAssignedUserIds = trackings.stream()
                .map(ApprovalTracking::getApproverPositionId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        // Lấy token từ SecurityContext
        String token = SecurityUtil.getCurrentUserJWT().orElse(null);
        // Gọi user-service một lần để lấy tất cả user names
        Map<Long, String> userNamesMap = userService.getUserNamesByIds(
                allActionUserIds.stream().collect(Collectors.toList()),
                token);

        // Gọi user-service một lần để lấy tất cả position names từ assigned user IDs
        Map<Long, String> positionNamesByUserIdMap = userService.getPositionNamesByIds(
                allAssignedUserIds.stream().collect(Collectors.toList()),
                token);

        List<ApprovalTrackingResponseDTO> content = trackings.stream()
                .map(tracking -> toResponseDTO(tracking, null, userNamesMap, positionNamesByUserIdMap))
                .collect(Collectors.toList());

        PaginationDTO paginationDTO = new PaginationDTO();
        com.example.workflow_service.dto.Meta meta = new com.example.workflow_service.dto.Meta();
        meta.setPage(trackingPage.getNumber() + 1);
        meta.setPageSize(trackingPage.getSize());
        meta.setPages(trackingPage.getTotalPages());
        meta.setTotal(trackingPage.getTotalElements());
        paginationDTO.setMeta(meta);
        paginationDTO.setResult(content);

        return paginationDTO;
    }

    @Transactional(readOnly = true)
    public List<ApprovalTrackingResponseDTO> getPendingApprovalsForUser(Long userId) {
        List<ApprovalTracking> trackings = approvalTrackingRepository
                .findByApproverPositionIdAndStatus(userId, ApprovalStatus.PENDING);

        // Thu thập tất cả user IDs từ trackings (actionUserId)
        Set<Long> allActionUserIds = trackings.stream()
                .map(ApprovalTracking::getActionUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        // Thu thập tất cả assigned user IDs từ trackings
        Set<Long> allAssignedUserIds = trackings.stream()
                .map(ApprovalTracking::getApproverPositionId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        // Lấy token từ SecurityContext
        String token = SecurityUtil.getCurrentUserJWT().orElse(null);
        // Gọi user-service một lần để lấy tất cả user names
        Map<Long, String> userNamesMap = userService.getUserNamesByIds(
                allActionUserIds.stream().collect(Collectors.toList()),
                token);

        // Gọi user-service một lần để lấy tất cả position names từ assigned user IDs
        Map<Long, String> positionNamesByUserIdMap = userService.getPositionNamesByIds(
                allAssignedUserIds.stream().collect(Collectors.toList()),
                token);

        return trackings.stream()
                .map(tracking -> toResponseDTO(tracking, null, userNamesMap, positionNamesByUserIdMap))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RequestWorkflowInfoDTO getWorkflowInfoByRequestId(
            Long requestId, Long workflowId, String requestType) {
        // Lấy tất cả approval tracking của request này
        List<ApprovalTracking> allTrackings = approvalTrackingRepository.findByRequestId(requestId);

        // Filter theo workflow type nếu có requestType để tránh trùng requestId giữa
        // RECRUITMENT_REQUEST và OFFER
        List<ApprovalTracking> trackings = allTrackings;
        if (requestType != null && !requestType.trim().isEmpty()) {
            String normalizedType = requestType.toUpperCase();
            final WorkflowType workflowType;
            if ("RECRUITMENT_REQUEST".equals(normalizedType) || "REQUEST".equals(normalizedType)) {
                workflowType = WorkflowType.REQUEST;
            } else if ("OFFER".equals(normalizedType)) {
                workflowType = WorkflowType.OFFER;
            } else {
                workflowType = null;
            }

            if (workflowType != null) {
                trackings = allTrackings.stream()
                        .filter(tracking -> {
                            WorkflowStep step = tracking.getStep();
                            if (step != null && step.getWorkflow() != null) {
                                return workflowType.equals(step.getWorkflow().getType());
                            }
                            return false;
                        })
                        .collect(Collectors.toList());

                if (trackings.isEmpty()) {
                    log.warn("Không tìm thấy tracking nào cho requestId: {} với type: {}", requestId, requestType);
                } else {
                    log.debug("Filtered {} trackings từ {} total trackings cho requestId: {} với type: {}",
                            trackings.size(), allTrackings.size(), requestId, requestType);
                }
            }
        }

        // Tìm workflowId từ tracking hoặc từ parameter
        Long actualWorkflowId = workflowId;
        Long currentStepId = null;

        if (!trackings.isEmpty()) {
            ApprovalTracking firstTracking = trackings.get(0);
            WorkflowStep firstStep = firstTracking.getStep();
            if (firstStep != null && actualWorkflowId == null) {
                actualWorkflowId = firstStep.getWorkflow().getId();
            }

            // Tìm bước hiện tại đang pending
            ApprovalTracking currentTracking = trackings.stream()
                    .filter(t -> t.getStatus() == ApprovalStatus.PENDING)
                    .findFirst()
                    .orElse(null);
            if (currentTracking != null && currentTracking.getStep() != null) {
                currentStepId = currentTracking.getStep().getId();
            }
        }

        // Lấy thông tin workflow trước để thu thập position IDs từ tất cả các steps
        Workflow workflow = null;
        if (actualWorkflowId != null) {
            workflow = workflowRepository.findById(actualWorkflowId).orElse(null);
        }

        // Thu thập tất cả position IDs từ trackings
        Set<Long> allPositionIdsFromTrackings = trackings.stream()
                .map(tracking -> {
                    WorkflowStep step = tracking.getStep();
                    return step != null ? step.getApproverPositionId() : null;
                })
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        // Thu thập tất cả position IDs từ workflow steps (nếu có workflow)
        Set<Long> allPositionIdsFromWorkflow = new java.util.HashSet<>();
        if (workflow != null && workflow.getSteps() != null) {
            allPositionIdsFromWorkflow = workflow.getSteps().stream()
                    .map(WorkflowStep::getApproverPositionId)
                    .filter(id -> id != null)
                    .collect(Collectors.toSet());
        }

        // Kết hợp position IDs từ cả trackings và workflow steps
        Set<Long> allPositionIds = new java.util.HashSet<>(allPositionIdsFromTrackings);
        allPositionIds.addAll(allPositionIdsFromWorkflow);

        // Thu thập tất cả user IDs từ trackings (actionUserId)
        Set<Long> allActionUserIds = trackings.stream()
                .map(ApprovalTracking::getActionUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        // Thu thập tất cả assigned user IDs từ trackings
        Set<Long> allAssignedUserIds = trackings.stream()
                .map(ApprovalTracking::getApproverPositionId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        // Lấy token từ SecurityContext
        String token = SecurityUtil.getCurrentUserJWT().orElse(null);
        // Gọi user-service một lần để lấy tất cả position names (từ positionIds)
        System.out.println("allPositionIds: " + allPositionIds);
        Map<Long, String> positionNamesMap = userService.getPositionNamesByIds(
                allPositionIds.stream().collect(Collectors.toList()),
                token);
        System.out.println("positionNamesMap: " + positionNamesMap);

        // Gọi user-service một lần để lấy tất cả user names
        Map<Long, String> userNamesMap = userService.getUserNamesByIds(
                allActionUserIds.stream().collect(Collectors.toList()),
                token);
        System.out.println("userNamesMap: " + userNamesMap);
        // Gọi user-service một lần để lấy tất cả position names từ assigned user IDs
        Map<Long, String> positionNamesByUserIdMap = userService.getPositionNamesByIds(
                allAssignedUserIds.stream().collect(Collectors.toList()),
                token);
        System.out.println("positionNamesByUserIdMap: " + positionNamesByUserIdMap);
        RequestWorkflowInfoDTO result = new RequestWorkflowInfoDTO();

        // Convert workflow với position names
        if (workflow != null) {
            WorkflowResponseDTO workflowDTO = convertWorkflowToDTO(workflow, positionNamesMap);
            result.setWorkflow(workflowDTO);
        }

        // Convert approval trackings với position names và user names
        List<ApprovalTrackingResponseDTO> trackingDTOs = trackings.stream()
                .map(tracking -> toResponseDTO(tracking, positionNamesMap, userNamesMap, positionNamesByUserIdMap))
                .collect(Collectors.toList());
        result.setApprovalTrackings(trackingDTOs);
        result.setCurrentStepId(currentStepId);

        return result;
    }

    private com.example.workflow_service.dto.workflow.WorkflowResponseDTO convertWorkflowToDTO(Workflow workflow) {
        return convertWorkflowToDTO(workflow, null);
    }

    private com.example.workflow_service.dto.workflow.WorkflowResponseDTO convertWorkflowToDTO(Workflow workflow,
            Map<Long, String> positionNamesMap) {
        com.example.workflow_service.dto.workflow.WorkflowResponseDTO dto = new com.example.workflow_service.dto.workflow.WorkflowResponseDTO();
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

        // Convert steps nếu có
        if (workflow.getSteps() != null) {
            List<com.example.workflow_service.dto.workflow.WorkflowStepResponseDTO> stepDTOs = workflow.getSteps()
                    .stream()
                    .sorted(java.util.Comparator.comparing(WorkflowStep::getStepOrder))
                    .map(step -> convertStepToDTO(step, positionNamesMap))
                    .collect(Collectors.toList());
            dto.setSteps(stepDTOs);
        }

        return dto;
    }

    private WorkflowStepResponseDTO convertStepToDTO(WorkflowStep step) {
        return convertStepToDTO(step, null);
    }

    private WorkflowStepResponseDTO convertStepToDTO(WorkflowStep step, Map<Long, String> positionNamesMap) {
        WorkflowStepResponseDTO dto = new WorkflowStepResponseDTO();
        dto.setId(step.getId());
        dto.setStepOrder(step.getStepOrder());
        dto.setApproverPositionId(step.getApproverPositionId());
        dto.setIsActive(step.getIsActive());
        dto.setCreatedAt(step.getCreatedAt());
        dto.setUpdatedAt(step.getUpdatedAt());

        // Set approverPositionName từ map nếu có
        if (positionNamesMap != null && step.getApproverPositionId() != null) {
            String positionName = positionNamesMap.get(step.getApproverPositionId());
            dto.setApproverPositionName(positionName);
        }

        return dto;
    }

    @Transactional
    public void handleWorkflowEvent(RecruitmentWorkflowEvent event) {
        if (event == null || event.getEventType() == null || event.getRequestId() == null) {
            return;
        }
        // Resolve departmentId nếu chưa có
        if (event.getDepartmentId() == null && "OFFER".equalsIgnoreCase(event.getRequestType())) {
            Long candidateId = event.getCandidateId();
            System.out.println("candidateId: " + candidateId);
            if (candidateId != null) {
                Long departmentId = candidateService.getDepartmentIdFromCandidate(candidateId, event.getAuthToken());
                System.out.println("departmentId: " + departmentId);
                if (departmentId != null) {
                    event.setDepartmentId(departmentId);
                } else {
                    log.warn("Offer {} không có departmentId sau khi cố gắng suy ra từ candidate {}",
                            event.getRequestId(), candidateId);
                }
            } else {
                log.warn("Offer {} không có candidateId", event.getRequestId());
            }
        }
        String eventType = event.getEventType().toUpperCase();
        switch (eventType) {
            case "REQUEST_SUBMITTED":
                handleRequestSubmitted(event);
                break;

            case "REQUEST_RETURNED":
                handleRequestReturned(event);
                break;
            case "REQUEST_CANCELLED":
                handleRequestCancelled(event);
                break;
            case "REQUEST_WITHDRAWN":
                handleRequestWithdrawn(event);
                break;
            // Backward compatibility
            case "REQUEST_APPROVED":
                handleStepApproved(event);
                break;
            case "REQUEST_REJECTED":
                handleStepRejected(event);
                break;
            default:
                log.debug("Ignore unknown workflow event type: {}", eventType);
        }
    }

    private ApprovalTrackingResponseDTO toResponseDTO(ApprovalTracking tracking) {
        return toResponseDTO(tracking, null, null, null);
    }

    private ApprovalTrackingResponseDTO toResponseDTO(ApprovalTracking tracking, Map<Long, String> positionNamesMap,
            Map<Long, String> userNamesMap) {
        return toResponseDTO(tracking, positionNamesMap, userNamesMap, null);
    }

    private ApprovalTrackingResponseDTO toResponseDTO(ApprovalTracking tracking, Map<Long, String> positionNamesMap,
            Map<Long, String> userNamesMap, Map<Long, String> positionNamesByUserIdMap) {
        ApprovalTrackingResponseDTO dto = new ApprovalTrackingResponseDTO();
        dto.setId(tracking.getId());
        dto.setRequestId(tracking.getRequestId());
        dto.setStepId(tracking.getStep() != null ? tracking.getStep().getId() : null);
        dto.setStatus(tracking.getStatus());
        dto.setApproverPositionId(tracking.getApproverPositionId());
        dto.setActionUserId(tracking.getActionUserId());
        dto.setActionAt(tracking.getActionAt());
        dto.setNotes(tracking.getNotes());
        dto.setCreatedAt(tracking.getCreatedAt());
        dto.setUpdatedAt(tracking.getUpdatedAt());

        // Set actionUserName từ map nếu có
        if (userNamesMap != null && tracking.getActionUserId() != null) {
            String userName = userNamesMap.get(tracking.getActionUserId());
            dto.setActionUserName(userName);
        }

        // Set approverPositionName từ positionNamesMap (có đầy đủ từ workflow steps và
        // trackings)
        if (positionNamesMap != null && tracking.getApproverPositionId() != null) {
            String positionName = positionNamesMap.get(tracking.getApproverPositionId());
            dto.setApproverPositionName(positionName);
        }

        return dto;
    }

    // DTO cho UserPosition từ user-service
    @SuppressWarnings("unused")
    private static class UserPositionDTO {
        private Long userId;
        private Long positionId;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getPositionId() {
            return positionId;
        }

        public void setPositionId(Long positionId) {
            this.positionId = positionId;
        }
    }

    private void moveToNextStep(ApprovalTracking currentTracking, Long departmentId, String authToken) {
        WorkflowStep currentStep = currentTracking.getStep();
        if (currentStep == null) {
            throw new CustomException("Không tìm thấy workflow step");

        }

        Workflow workflow = currentStep.getWorkflow();
        WorkflowStep nextStep = workflowStepRepository
                .findByWorkflowIdAndStepOrder(workflow.getId(), currentStep.getStepOrder() + 1)
                .orElse(null);

        if (nextStep != null) {
            Long effectiveDepartmentId = departmentId;
            createTrackingForStep(currentTracking.getRequestId(), nextStep, effectiveDepartmentId, authToken);
        }
    }

    private void handleRequestSubmitted(RecruitmentWorkflowEvent event) {
        if (event.getWorkflowId() == null) {
            log.warn("REQUEST_SUBMITTED missing workflowId for request/offer {}", event.getRequestId());
            return;
        }

        // Đảm bảo departmentId đã được resolve cho offer
        if ("OFFER".equalsIgnoreCase(event.getRequestType()) && event.getDepartmentId() == null) {
            log.warn("Offer {} không có departmentId khi submit, không thể tạo tracking", event.getRequestId());
            return;
        }

        // Kiểm tra xem có tracking nào đang pending không (filter theo workflow type)
        List<ApprovalTracking> allPending = approvalTrackingRepository
                .findByRequestIdAndStatus(event.getRequestId(), ApprovalStatus.PENDING);
        WorkflowType workflowType = getWorkflowTypeFromRequestType(event.getRequestType());
        List<ApprovalTracking> pending = filterByWorkflowType(allPending, workflowType);

        if (!pending.isEmpty()) {
            boolean allReturnPlaceholders = pending.stream()
                    .allMatch(this::isReturnPlaceholderTracking);

            if (allReturnPlaceholders) {
                pending.forEach(tracking -> {
                    tracking.setStatus(ApprovalStatus.CANCELLED);
                    tracking.setActionType("RESUBMIT");
                    tracking.setActionUserId(event.getActorUserId());
                    tracking.setActionAt(LocalDateTime.now());
                    tracking.setNotes("Placeholder cancelled due to resubmit");
                    approvalTrackingRepository.save(tracking);
                });
            } else {
                // Cancel tất cả tracking cũ và tạo mới (đặc biệt cho offer khi resubmit)
                log.info("Request/Offer {} có tracking cũ đang pending, cancel và tạo mới", event.getRequestId());
                pending.forEach(tracking -> {
                    tracking.setStatus(ApprovalStatus.CANCELLED);
                    tracking.setActionType("RESUBMIT");
                    tracking.setActionUserId(event.getActorUserId());
                    tracking.setActionAt(LocalDateTime.now());
                    tracking.setNotes("Cancelled due to resubmit");
                    approvalTrackingRepository.save(tracking);
                });
            }
        }

        // Kiểm tra xem có tracking nào đã bị return không (submit lại sau return)
        // Filter theo workflow type để tránh nhầm lẫn
        List<ApprovalTracking> allTrackings = approvalTrackingRepository.findByRequestId(event.getRequestId());
        allTrackings = filterByWorkflowType(allTrackings, workflowType);
        ApprovalTracking returnedTracking = allTrackings.stream()
                .filter(t -> t.getReturnedToStepId() != null)
                .findFirst()
                .orElse(null);

        WorkflowStep targetStep;
        boolean isResubmitAfterReturn = false;
        if (returnedTracking != null && returnedTracking.getReturnedToStepId() != null) {
            // Submit lại sau return: tạo tracking từ bước được trả về
            targetStep = workflowStepRepository.findById(returnedTracking.getReturnedToStepId())
                    .orElseThrow(() -> new CustomException(
                            "Không tìm thấy bước được trả về: " + returnedTracking.getReturnedToStepId()));
            isResubmitAfterReturn = true;
            log.info("Request/Offer {} submit lại từ bước {}", event.getRequestId(), targetStep.getId());
        } else {
            // Submit lần đầu: tạo tracking từ bước đầu tiên
            Workflow workflow = workflowRepository.findById(event.getWorkflowId())
                    .orElseThrow(() -> new CustomException("Workflow không tồn tại: " + event.getWorkflowId()));
            targetStep = workflowStepRepository
                    .findByWorkflowIdAndStepOrder(workflow.getId(), 1)
                    .orElseThrow(() -> new CustomException("Workflow không có bước đầu tiên"));

            // Logic: Nếu người tạo yêu cầu trùng với người duyệt ở bước 1 (cùng positionId
            // VÀ departmentId),
            // tự động bỏ qua bước 1
            if (event.getRequesterId() != null && targetStep.getApproverPositionId() != null
                    && event.getDepartmentId() != null) {
                Long requesterPositionId = getRequesterPositionId(event.getRequesterId(), event.getAuthToken());
                Long requesterDepartmentId = getRequesterDepartmentId(event.getRequesterId(), event.getAuthToken());

                // Kiểm tra cả positionId VÀ departmentId phải trùng
                // departmentId lấy từ event (đã có sẵn cho cả RECRUITMENT_REQUEST và OFFER)
                boolean positionMatches = requesterPositionId != null
                        && requesterPositionId.equals(targetStep.getApproverPositionId());
                boolean departmentMatches = requesterDepartmentId != null
                        && requesterDepartmentId.equals(event.getDepartmentId());

                if (positionMatches && departmentMatches) {
                    // Requester trùng với approver của step 1 (cùng position và department):
                    // tự động approve step 1 và chuyển sang step 2
                    log.info(
                            "Requester {} (positionId: {}, departmentId: {}) trùng với approver của step 1 (positionId: {}, departmentId: {}), tự động bỏ qua step 1",
                            event.getRequesterId(), requesterPositionId, requesterDepartmentId,
                            targetStep.getApproverPositionId(), event.getDepartmentId());

                    // Tạo tracking cho step 1 với status APPROVED (tự động approve)
                    ApprovalTracking step1Tracking = createTrackingForStep(event.getRequestId(), targetStep,
                            event.getDepartmentId(), event.getAuthToken());
                    step1Tracking.setStatus(ApprovalStatus.APPROVED);
                    step1Tracking.setActionType("APPROVE");
                    step1Tracking.setActionUserId(event.getRequesterId());
                    step1Tracking.setActionAt(LocalDateTime.now());
                    step1Tracking.setNotes("Người tạo trùng với bước đầu");
                    approvalTrackingRepository.save(step1Tracking);

                    // Tạo tracking cho step 2 (nếu có)
                    WorkflowStep nextStep = workflowStepRepository
                            .findByWorkflowIdAndStepOrder(workflow.getId(), 2)
                            .orElse(null);
                    if (nextStep != null) {
                        createTrackingForStep(event.getRequestId(), nextStep, event.getDepartmentId(),
                                event.getAuthToken());
                    } else {
                        // Không còn bước nào: workflow đã hoàn thành
                        log.info("Request/Offer {} đã hoàn thành tất cả các bước workflow (auto-approve step 1)",
                                event.getRequestId());
                        sendWorkflowCompletedEvent(event);
                    }
                    return; // Đã xử lý xong, không cần tạo tracking thêm
                } else {
                    log.debug(
                            "Requester {} không trùng với approver của step 1 - requester (positionId: {}, departmentId: {}), step (positionId: {}, departmentId: {})",
                            event.getRequesterId(), requesterPositionId, requesterDepartmentId,
                            targetStep.getApproverPositionId(), event.getDepartmentId());
                }
            }
        }

        ApprovalTracking newTracking = createTrackingForStep(event.getRequestId(), targetStep, event.getDepartmentId(),
                event.getAuthToken());

        // Nếu là resubmit sau return, thêm ghi chú đã chỉnh sửa
        if (isResubmitAfterReturn && newTracking != null) {
            String existingNotes = newTracking.getNotes();
            String editedNote = "Đã chỉnh sửa";
            if (existingNotes != null && !existingNotes.trim().isEmpty()) {
                newTracking.setNotes(existingNotes + " - " + editedNote);
            } else {
                newTracking.setNotes(editedNote);
            }
            approvalTrackingRepository.save(newTracking);
        }
    }

    private void handleStepApproved(RecruitmentWorkflowEvent event) {
        ApprovalTracking current = markCurrentTracking(event, ApprovalStatus.APPROVED, "APPROVE", event.getNotes());
        if (current != null) {
            // Kiểm tra có bước tiếp theo không
            WorkflowStep currentStep = current.getStep();
            if (currentStep == null) {
                throw new CustomException("Không tìm thấy workflow step cho tracking: " + current.getId());
            }

            Workflow workflow = currentStep.getWorkflow();
            WorkflowStep nextStep = workflowStepRepository
                    .findByWorkflowIdAndStepOrder(workflow.getId(), currentStep.getStepOrder() + 1)
                    .orElse(null);

            // Thông báo cho requester và owner về việc bước hiện tại đã được phê duyệt
            notifyRequester(event,
                    "Yêu cầu #" + event.getRequestId() + " đã được phê duyệt",
                    "Yêu cầu đã được phê duyệt ở bước " + currentStep.getStepOrder());

            if (nextStep != null) {
                // Có bước tiếp theo: chuyển sang bước tiếp
                moveToNextStep(current, event.getDepartmentId(), event.getAuthToken());
            } else {
                // Không còn bước nào: đã hoàn thành workflow
                // Kiểm tra xem còn tracking nào pending không (filter theo workflow type)
                List<ApprovalTracking> allRemainingPending = approvalTrackingRepository
                        .findByRequestIdAndStatus(event.getRequestId(), ApprovalStatus.PENDING);
                WorkflowType eventWorkflowType = getWorkflowTypeFromRequestType(event.getRequestType());
                List<ApprovalTracking> remainingPending = filterByWorkflowType(allRemainingPending, eventWorkflowType);

                if (remainingPending.isEmpty()) {
                    // Không còn bước nào pending: workflow đã hoàn thành
                    log.info("Request/Offer {} đã hoàn thành tất cả các bước workflow", event.getRequestId());
                    // Gửi event WORKFLOW_COMPLETED để job-service cập nhật status = APPROVED
                    sendWorkflowCompletedEvent(event);
                }
            }
        }
    }

    private void handleStepRejected(RecruitmentWorkflowEvent event) {
        // Reject: Từ chối ở bước này, kết thúc luồng
        ApprovalTracking tracking = markCurrentTracking(event, ApprovalStatus.REJECTED, "REJECT", event.getNotes());
        if (tracking != null && tracking.getStep() != null) {
            // Invalidate tất cả các bước tiếp theo (nếu có)
            invalidateFutureSteps(event.getRequestId(), tracking.getStep().getId());

            notifyRequester(event,
                    "Yêu cầu #" + event.getRequestId() + " bị từ chối",
                    event.getNotes() != null ? event.getNotes() : "Yêu cầu đã bị từ chối.");
        }
    }

    private void handleRequestReturned(RecruitmentWorkflowEvent event) {
        // Đánh dấu bước hiện tại là RETURNED
        ApprovalTracking currentTracking = findCurrentPendingTracking(event.getRequestId(), event.getRequestType());
        if (currentTracking == null) {
            log.warn("Không tìm thấy approval tracking đang chờ cho request {}", event.getRequestId());
            return;
        }

        // Xác định bước cần trả về
        Long returnedToStepId = event.getReturnedToStepId();
        if (returnedToStepId == null) {
            // Nếu không chỉ định, trả về bước đầu tiên
            Workflow workflow = workflowRepository.findById(event.getWorkflowId())
                    .orElseThrow(() -> new CustomException("Workflow không tồn tại: " + event.getWorkflowId()));
            WorkflowStep firstStep = workflowStepRepository
                    .findByWorkflowIdAndStepOrder(workflow.getId(), 1)
                    .orElseThrow(() -> new CustomException("Workflow không có bước đầu tiên"));
            returnedToStepId = firstStep.getId();
        }

        // Đánh dấu bước hiện tại
        currentTracking.setStatus(ApprovalStatus.RETURNED);
        currentTracking.setActionType("RETURN");
        currentTracking.setActionUserId(event.getActorUserId());
        currentTracking.setActionAt(LocalDateTime.now());
        currentTracking.setNotes(event.getReason());
        currentTracking.setReturnedToStepId(returnedToStepId);
        approvalTrackingRepository.save(currentTracking);

        // Invalidate tất cả các bước đã qua (từ bước hiện tại trở về sau)
        if (currentTracking.getStep() != null) {
            invalidateFutureSteps(event.getRequestId(), currentTracking.getStep().getId());
        }
        // Không tạo pending mới khi return - sẽ tạo khi submit lại

        notifyRequester(event,
                "Yêu cầu #" + event.getRequestId() + " bị trả về",
                event.getReason() != null ? event.getReason()
                        : "Yêu cầu cần chỉnh sửa lại thông tin.");

        log.info("Request {} được trả về bước {}", event.getRequestId(), returnedToStepId);
    }

    private void handleRequestCancelled(RecruitmentWorkflowEvent event) {
        // Cancel: Hủy yêu cầu (có thể ở bất kỳ trạng thái nào)
        // Invalidate tất cả các tracking đang pending (filter theo workflow type)
        List<ApprovalTracking> allPendingTrackings = approvalTrackingRepository
                .findByRequestIdAndStatus(event.getRequestId(), ApprovalStatus.PENDING);
        WorkflowType workflowType = getWorkflowTypeFromRequestType(event.getRequestType());
        List<ApprovalTracking> pendingTrackings = filterByWorkflowType(allPendingTrackings, workflowType);

        for (ApprovalTracking tracking : pendingTrackings) {
            tracking.setStatus(ApprovalStatus.CANCELLED);
            tracking.setActionType("CANCEL");
            tracking.setActionUserId(event.getActorUserId());
            tracking.setActionAt(LocalDateTime.now());
            tracking.setNotes(event.getReason());
            approvalTrackingRepository.save(tracking);
        }

        notifyRequester(event,
                "Yêu cầu #" + event.getRequestId() + " đã bị hủy",
                event.getReason() != null ? event.getReason() : "Yêu cầu đã bị hủy.");

        log.info("Request {} đã bị hủy, đã invalidate {} tracking", event.getRequestId(), pendingTrackings.size());
    }

    private void handleRequestWithdrawn(RecruitmentWorkflowEvent event) {
        // Withdraw: Rút lại yêu cầu (chỉ submitter/owner mới có thể)
        // Invalidate tất cả các tracking đang pending (filter theo workflow type)
        List<ApprovalTracking> allPendingTrackings = approvalTrackingRepository
                .findByRequestIdAndStatus(event.getRequestId(), ApprovalStatus.PENDING);
        WorkflowType workflowType = getWorkflowTypeFromRequestType(event.getRequestType());
        List<ApprovalTracking> pendingTrackings = filterByWorkflowType(allPendingTrackings, workflowType);

        for (ApprovalTracking tracking : pendingTrackings) {
            tracking.setStatus(ApprovalStatus.CANCELLED);
            tracking.setActionType("WITHDRAW");
            tracking.setActionUserId(event.getActorUserId());
            tracking.setActionAt(LocalDateTime.now());
            tracking.setNotes(event.getReason());
            approvalTrackingRepository.save(tracking);
        }

        notifyRequester(event,
                "Yêu cầu #" + event.getRequestId() + " đã được rút lại",
                event.getReason() != null ? event.getReason() : "Yêu cầu đã được rút bởi người tạo.");

        log.info("Request {} đã bị rút lại, đã invalidate {} tracking", event.getRequestId(), pendingTrackings.size());
    }

    /**
     * Invalidate tất cả các bước đã qua (từ bước hiện tại trở về sau)
     * Dùng khi return hoặc reject
     */
    private void invalidateFutureSteps(Long requestId, Long currentStepId) {
        WorkflowStep currentStep = workflowStepRepository.findById(currentStepId)
                .orElseThrow(() -> new CustomException("Không tìm thấy workflow step: " + currentStepId));

        Workflow workflow = currentStep.getWorkflow();
        WorkflowType workflowType = workflow != null ? workflow.getType() : null;
        List<WorkflowStep> allSteps = workflowStepRepository.findByWorkflowIdOrderByStepOrderAsc(workflow.getId());

        // Tìm các bước có stepOrder >= currentStep.stepOrder
        List<Long> futureStepIds = allSteps.stream()
                .filter(step -> step.getStepOrder() > currentStep.getStepOrder())
                .map(WorkflowStep::getId)
                .collect(Collectors.toList());

        if (!futureStepIds.isEmpty()) {
            // Tìm và invalidate các tracking của các bước này (filter theo workflow type)
            List<ApprovalTracking> allFutureTrackings = approvalTrackingRepository.findByRequestId(requestId);
            List<ApprovalTracking> futureTrackings = filterByWorkflowType(allFutureTrackings, workflowType)
                    .stream()
                    .filter(t -> t.getStep() != null && futureStepIds.contains(t.getStep().getId())
                            && t.getStatus() == ApprovalStatus.PENDING)
                    .collect(Collectors.toList());

            for (ApprovalTracking tracking : futureTrackings) {
                tracking.setStatus(ApprovalStatus.CANCELLED);
                tracking.setNotes("Automatically cancelled due to return/reject at step " + currentStepId);
                approvalTrackingRepository.save(tracking);
            }

            log.info("Đã invalidate {} tracking cho các bước tương lai của request {}", futureTrackings.size(),
                    requestId);
        }
    }

    /**
     * Khi request bị return, tạo một tracking mới cho bước được trả về để người
     * tạo/owner
     * có thể chỉnh sửa và submit lại.
     */
    private void createTrackingForReturnedStep(RecruitmentWorkflowEvent event, Long returnedToStepId,
            boolean isReturned) {
        WorkflowStep returnedStep = workflowStepRepository.findById(returnedToStepId)
                .orElseThrow(() -> new CustomException("Không tìm thấy bước được trả về: " + returnedToStepId));

        ApprovalTracking tracking = new ApprovalTracking();
        tracking.setRequestId(event.getRequestId());
        tracking.setStep(returnedStep);
        tracking.setStatus(ApprovalStatus.PENDING);

        tracking.setApproverPositionId(returnedStep.getApproverPositionId());
        tracking.setNotes("Đang đợi cập nhật sau khi trả về");
        approvalTrackingRepository.save(tracking);
        if (!isReturned) {
            notifyNextApprovers(event.getDepartmentId(), returnedStep, event.getRequestId(), event.getAuthToken());
        }
    }

    private void notifyNextApprovers(Long departmentId, WorkflowStep step, Long requestId, String authToken) {
        if (step == null) {
            return;
        }
        Long effectiveDepartmentId = departmentId;
        if (effectiveDepartmentId == null) {
            log.warn("Không xác định được departmentId cho request {}", requestId);
            return;
        }

        String stepName = "Bước " + step.getStepOrder();
        String title = "Yêu cầu #" + requestId + " cần xử lý";
        String message = "Bước '" + stepName + "' đang chờ phê duyệt.";

        notificationProducer.sendNotificationToDepartment(
                effectiveDepartmentId,
                step.getApproverPositionId(),
                title,
                message,
                authToken);
    }

    /**
     * Gửi thông báo cho requester và owner (nếu khác nhau).
     * Nếu requesterId và ownerUserId trùng nhau thì chỉ gửi 1 thông báo.
     */
    private void notifyRequester(RecruitmentWorkflowEvent event, String title, String message) {
        Set<Long> recipients = new HashSet<>();
        if (event.getRequesterId() != null) {
            recipients.add(event.getRequesterId());
        }
        if (event.getOwnerUserId() != null) {
            recipients.add(event.getOwnerUserId());
        }
        if (recipients.isEmpty()) {
            return;
        }
        for (Long recipientId : recipients) {
            notificationProducer.sendNotification(recipientId, title, message, event.getAuthToken());
        }
    }

    private ApprovalTracking markCurrentTracking(RecruitmentWorkflowEvent event,
            ApprovalStatus status,
            String actionType,
            String notes) {
        ApprovalTracking tracking = findCurrentPendingTracking(event.getRequestId(), event.getRequestType());
        if (tracking == null) {
            log.warn("Không tìm thấy approval tracking đang chờ cho request {}", event.getRequestId());
            return null;
        }
        tracking.setStatus(status);
        tracking.setActionType(actionType);
        tracking.setActionUserId(event.getActorUserId());
        tracking.setActionAt(LocalDateTime.now());
        tracking.setNotes(notes);
        return approvalTrackingRepository.save(tracking);
    }

    /**
     * Helper method để filter tracking theo workflow type
     */
    private List<ApprovalTracking> filterByWorkflowType(List<ApprovalTracking> trackings, WorkflowType workflowType) {
        if (workflowType == null) {
            return trackings;
        }
        return trackings.stream()
                .filter(tracking -> {
                    WorkflowStep step = tracking.getStep();
                    if (step != null && step.getWorkflow() != null) {
                        return workflowType.equals(step.getWorkflow().getType());
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    /**
     * Lấy workflow type từ requestType string
     */
    private WorkflowType getWorkflowTypeFromRequestType(String requestType) {
        if (requestType == null || requestType.trim().isEmpty()) {
            return null;
        }
        String normalizedType = requestType.toUpperCase();
        if ("RECRUITMENT_REQUEST".equals(normalizedType) || "REQUEST".equals(normalizedType)) {
            return WorkflowType.REQUEST;
        } else if ("OFFER".equals(normalizedType)) {
            return WorkflowType.OFFER;
        }
        return null;
    }

    private ApprovalTracking findCurrentPendingTracking(Long requestId, String requestType) {
        List<ApprovalTracking> allPending = approvalTrackingRepository
                .findByRequestIdAndStatus(requestId, ApprovalStatus.PENDING);

        // Filter theo workflow type nếu có
        WorkflowType workflowType = getWorkflowTypeFromRequestType(requestType);
        List<ApprovalTracking> filtered = filterByWorkflowType(allPending, workflowType);

        return filtered.stream()
                .findFirst()
                .orElse(null);
    }

    private ApprovalTracking createTrackingForStep(Long requestId, WorkflowStep step, Long departmentId,
            String authToken) {
        log.info("Tạo tracking cho requestId: {}, stepId: {}, departmentId: {}", requestId, step.getId(), departmentId);
        log.info("step.getApproverPositionId(): {}", step.getApproverPositionId());

        // Kiểm tra departmentId trước khi tìm actionUserId
        if (departmentId == null) {
            log.warn("departmentId là null khi tạo tracking cho requestId: {}, stepId: {}", requestId, step.getId());
        }

        // Tìm actionUserId dựa trên positionId và departmentId
        Long actionUserId = null;
        if (departmentId != null && step.getApproverPositionId() != null) {
            actionUserId = userService.findUserByPositionIdAndDepartmentId(step.getApproverPositionId(), departmentId,
                    authToken);
            log.info("Tìm được actionUserId: {} cho positionId: {}, departmentId: {}",
                    actionUserId, step.getApproverPositionId(), departmentId);
        } else {
            log.warn("Không thể tìm actionUserId vì departmentId: {} hoặc approverPositionId: {} là null",
                    departmentId, step.getApproverPositionId());
        }
        // Long assignedUserId = findUserByPositionId(step.getApproverPositionId());
        // if (assignedUserId == null) {
        // log.warn("Không tìm thấy user cho vị trí {} khi tạo bước mới cho request {}",
        // step.getApproverPositionId(),
        // requestId);
        // return;
        // }
        ApprovalTracking tracking = new ApprovalTracking();
        tracking.setRequestId(requestId);
        tracking.setStep(step);
        tracking.setStatus(ApprovalStatus.PENDING);
        tracking.setApproverPositionId(step.getApproverPositionId());
        tracking.setActionUserId(actionUserId);
        ApprovalTracking saved = approvalTrackingRepository.save(tracking);

        notifyNextApprovers(departmentId, step, requestId, authToken);
        return saved;
    }

    /**
     * Placeholder tracking được tạo khi request bị return để chờ submitter/owner
     * chỉnh sửa. Nhận diện bằng ghi chú mặc định.
     */
    private boolean isReturnPlaceholderTracking(ApprovalTracking tracking) {
        String notes = tracking.getNotes();
        return notes != null && notes.startsWith("Waiting for update after return");
    }

    /**
     * Lấy positionId của requester từ user-service
     */
    private Long getRequesterPositionId(Long requesterId, String token) {
        if (requesterId == null) {
            return null;
        }
        try {
            String url = userServiceBaseUrl + "/api/v1/user-service/employees/" + requesterId;
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            if (token != null && !token.isEmpty()) {
                headers.setBearerAuth(token);
            }
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<Response<com.fasterxml.jackson.databind.JsonNode>> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity,
                    new ParameterizedTypeReference<Response<com.fasterxml.jackson.databind.JsonNode>>() {
                    });
            if (response.getBody() != null && response.getBody().getData() != null) {
                com.fasterxml.jackson.databind.JsonNode employee = response.getBody().getData();
                if (employee.has("position") && employee.get("position").has("id")) {
                    return employee.get("position").get("id").asLong();
                }
            }
        } catch (Exception e) {
            log.warn("Không thể lấy position của requester {}: {}", requesterId, e.getMessage());
        }
        return null;
    }

    /**
     * Lấy departmentId của requester từ user-service
     */
    private Long getRequesterDepartmentId(Long requesterId, String token) {
        if (requesterId == null) {
            return null;
        }
        try {
            String url = userServiceBaseUrl + "/api/v1/user-service/employees/" + requesterId;
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            if (token != null && !token.isEmpty()) {
                headers.setBearerAuth(token);
            }
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<Response<com.fasterxml.jackson.databind.JsonNode>> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity,
                    new ParameterizedTypeReference<Response<com.fasterxml.jackson.databind.JsonNode>>() {
                    });
            if (response.getBody() != null && response.getBody().getData() != null) {
                com.fasterxml.jackson.databind.JsonNode employee = response.getBody().getData();
                // Lấy departmentId từ department.id hoặc departmentId
                if (employee.has("department")) {
                    com.fasterxml.jackson.databind.JsonNode department = employee.get("department");
                    if (department.has("id") && !department.get("id").isNull()) {
                        return department.get("id").asLong();
                    }
                }
                if (employee.has("departmentId") && !employee.get("departmentId").isNull()) {
                    return employee.get("departmentId").asLong();
                }
            }
        } catch (Exception e) {
            log.warn("Không thể lấy departmentId của requester {}: {}", requesterId, e.getMessage());
        }
        return null;
    }

    /**
     * Gửi event WORKFLOW_COMPLETED khi workflow đã hoàn thành tất cả các bước
     */
    private void sendWorkflowCompletedEvent(RecruitmentWorkflowEvent originalEvent) {
        RecruitmentWorkflowEvent completedEvent = new RecruitmentWorkflowEvent();
        completedEvent.setEventType("WORKFLOW_COMPLETED");
        completedEvent.setRequestId(originalEvent.getRequestId());
        completedEvent.setWorkflowId(originalEvent.getWorkflowId());
        completedEvent.setActorUserId(originalEvent.getActorUserId());
        completedEvent.setNotes(originalEvent.getNotes());
        completedEvent.setRequestStatus("APPROVED");
        completedEvent.setOwnerUserId(originalEvent.getOwnerUserId());
        completedEvent.setRequesterId(originalEvent.getRequesterId());
        completedEvent.setDepartmentId(originalEvent.getDepartmentId());
        completedEvent.setOccurredAt(java.time.LocalDateTime.now());
        completedEvent.setAuthToken(originalEvent.getAuthToken());

        workflowProducer.publishEvent(completedEvent);
        log.info("Đã gửi event WORKFLOW_COMPLETED cho request/offer {}", originalEvent.getRequestId());

        notifyRequester(originalEvent,
                "Yêu cầu #" + originalEvent.getRequestId() + " đã được phê duyệt",
                "Tất cả các bước phê duyệt đã hoàn thành.");
    }
}
