package com.example.job_service.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.job_service.dto.Meta;
import com.example.job_service.dto.PaginationDTO;
import com.example.job_service.dto.SingleResponseDTO;
import com.example.job_service.utils.TextTruncateUtil;
import com.example.job_service.dto.recruitment.ApproveRecruitmentRequestDTO;
import com.example.job_service.dto.recruitment.CancelRecruitmentRequestDTO;
import com.example.job_service.dto.recruitment.CreateRecruitmentRequestDTO;
import com.example.job_service.dto.recruitment.RecruitmentRequestAllWithUserDTO;
import com.example.job_service.dto.recruitment.RecruitmentRequestWithUserDTO;
import com.example.job_service.dto.recruitment.RejectRecruitmentRequestDTO;
import com.example.job_service.dto.recruitment.ReturnRecruitmentRequestDTO;
import com.example.job_service.exception.IdInvalidException;
import com.example.job_service.exception.UserClientException;
import com.example.job_service.messaging.RecruitmentWorkflowEvent;
import com.example.job_service.messaging.RecruitmentWorkflowProducer;
import com.fasterxml.jackson.databind.JsonNode;
import com.example.job_service.model.RecruitmentRequest;
import com.example.job_service.repository.RecruitmentRequestRepository;
import com.example.job_service.utils.enums.RecruitmentRequestStatus;

@Service
public class RecruitmentRequestService {
    private final RecruitmentRequestRepository recruitmentRequestRepository;
    private final UserClient userService;
    private final RecruitmentWorkflowProducer workflowProducer;
    private final WorkflowClient workflowServiceClient;

    public RecruitmentRequestService(RecruitmentRequestRepository recruitmentRequestRepository,
            UserClient userService,
            RecruitmentWorkflowProducer workflowProducer,
            WorkflowClient workflowServiceClient) {
        this.recruitmentRequestRepository = recruitmentRequestRepository;
        this.userService = userService;
        this.workflowProducer = workflowProducer;
        this.workflowServiceClient = workflowServiceClient;
    }

    @Transactional
    public RecruitmentRequest create(CreateRecruitmentRequestDTO dto) {
        // JobCategory category = jobCategoryService.findById(dto.getJobCategoryId());
        RecruitmentRequest rr = new RecruitmentRequest();
        rr.setTitle(dto.getTitle());
        rr.setQuantity(dto.getQuantity());
        rr.setReason(dto.getReason());
        // Set salary nếu có
        rr.setSalaryMin(dto.getSalaryMin());
        rr.setSalaryMax(dto.getSalaryMax());

        rr.setStatus(RecruitmentRequestStatus.DRAFT);
        rr.setRequesterId(dto.getRequesterId());
        rr.setOwnerUserId(dto.getRequesterId());
        rr.setWorkflowId(dto.getWorkflowId());
        // rr.setJobCategory(category);
        rr.setDepartmentId(dto.getDepartmentId());
        rr.setActive(true);
        return recruitmentRequestRepository.save(rr);
    }

    @Transactional
    public RecruitmentRequest approveStep(Long id, ApproveRecruitmentRequestDTO dto, Long actorId, String token)
            throws IdInvalidException {
        RecruitmentRequest request = this.findById(id);
        if (request.getStatus() != RecruitmentRequestStatus.SUBMITTED
                && request.getStatus() != RecruitmentRequestStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể phê duyệt yêu cầu ở trạng thái SUBMITTED hoặc PENDING");
        }

        // Approve: Vẫn giữ SUBMITTED/PENDING, workflow-service sẽ xử lý chuyển bước
        // Chỉ khi workflow-service xác nhận đã hết bước thì mới chuyển sang APPROVED
        // request.setApprovalNotes(dto.getApprovalNotes());
        RecruitmentRequest saved = recruitmentRequestRepository.save(request);
        publishWorkflowEvent("REQUEST_APPROVED", saved, actorId, dto.getApprovalNotes(), null, null, null,
                token);
        return saved;
    }

    @Transactional
    public RecruitmentRequest rejectStep(Long id, RejectRecruitmentRequestDTO dto, Long actorId, String token)
            throws IdInvalidException {
        RecruitmentRequest request = this.findById(id);
        if (request.getStatus() != RecruitmentRequestStatus.SUBMITTED
                && request.getStatus() != RecruitmentRequestStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể từ chối yêu cầu ở trạng thái SUBMITTED hoặc PENDING");
        }

        // Reject: Từ chối ở bước này, kết thúc luồng
        request.setStatus(RecruitmentRequestStatus.REJECTED);
        // request.setApprovalNotes(dto.getReason());
        // request.setApprovedAt(LocalDateTime.now());
        RecruitmentRequest saved = recruitmentRequestRepository.save(request);
        publishWorkflowEvent("REQUEST_REJECTED", saved, actorId, null, dto.getReason(), null, null, token);
        return saved;
    }

    public RecruitmentRequest findById(Long id) throws IdInvalidException {
        return recruitmentRequestRepository.findById(id)
                .orElseThrow(() -> new IdInvalidException("Yêu cầu tuyển dụng không tồn tại"));
    }

    public boolean changeStatus(Long id, RecruitmentRequestStatus status) throws IdInvalidException {
        RecruitmentRequest rr = this.findById(id);
        rr.setStatus(status);
        return recruitmentRequestRepository.save(rr) != null;
    }

    public List<RecruitmentRequest> getAllByDepartmentId(Long departmentId) {
        return recruitmentRequestRepository.findByDepartmentId(departmentId);
    }

    public RecruitmentRequest getById(Long id) throws IdInvalidException {
        return recruitmentRequestRepository.findById(id)
                .orElseThrow(() -> new IdInvalidException("Yêu cầu tuyển dụng không tồn tại"));
    }

    public List<RecruitmentRequest> getAll() {
        return recruitmentRequestRepository.findAll().stream()
                .filter(request -> request.isActive())
                .collect(Collectors.toList());
    }

    public List<RecruitmentRequest> findAllWithFilters(Long departmentId, String status, Long createdBy,
            String keyword) {
        RecruitmentRequestStatus statusEnum = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                statusEnum = RecruitmentRequestStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid status, will be ignored (statusEnum remains null)
            }
        }
        return recruitmentRequestRepository.findByFiltersList(departmentId, statusEnum, createdBy, keyword);
    }

    // Methods with user information
    public RecruitmentRequestWithUserDTO getByIdWithUser(Long id, String token) throws IdInvalidException {
        RecruitmentRequest request = this.findById(id);
        return convertToWithUserDTO(request, token);
    }

    public SingleResponseDTO<RecruitmentRequestWithUserDTO> getByIdWithUserAndMetadata(Long id, String token)
            throws IdInvalidException {
        RecruitmentRequest request = this.findById(id);
        RecruitmentRequestWithUserDTO dto = convertToWithUserDTO(request, token);
        return new SingleResponseDTO<>(dto, TextTruncateUtil.getRecruitmentRequestCharacterLimits());
    }

    public PaginationDTO getAllWithFilters(Long departmentId, String status, Long createdBy, String keyword,
            String token,
            Pageable pageable) {
        RecruitmentRequestStatus statusEnum = null;
        if (departmentId != null && departmentId == 1) {
            departmentId = null;
        }
        if (status != null && !status.trim().isEmpty()) {
            try {
                statusEnum = RecruitmentRequestStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid status, will be ignored (statusEnum remains null)
            }
        }
        Page<RecruitmentRequest> requests = recruitmentRequestRepository.findByFilters(departmentId, statusEnum,
                createdBy,
                keyword,
                pageable);
        return convertToWithUserDTOList(requests, token);
    }

    private RecruitmentRequestWithUserDTO convertToWithUserDTO(RecruitmentRequest request, String token) {
        return convertToWithUserDTO(request, token, false, true);
    }

    private RecruitmentRequestWithUserDTO convertToWithUserDTO(RecruitmentRequest request, String token,
            boolean truncateText, boolean includeWorkflow) {
        RecruitmentRequestWithUserDTO dto = RecruitmentRequestWithUserDTO.fromEntity(request, truncateText);

        if (request.getRequesterId() != null) {
            // Lấy thông tin employee (requesterId lưu employeeId)
            ResponseEntity<JsonNode> requesterResponse = userService.getEmployeeById(request.getRequesterId(), token);
            if (requesterResponse.getStatusCode().is2xxSuccessful()) {
                dto.setRequester(requesterResponse.getBody());
            } else {
                // Nếu lỗi → ném lại exception có nội dung JSON lỗi
                throw new UserClientException(requesterResponse);
            }
        }

        // if (request.getApprovedId() != null) {
        // // Lấy thông tin employee (approvedId lưu employeeId)
        // ResponseEntity<JsonNode> approverResponse =
        // userService.getEmployeeById(request.getApprovedId(), token);
        // if (approverResponse.getStatusCode().is2xxSuccessful()) {
        // dto.setApprover(approverResponse.getBody());
        // } else {
        // throw new UserServiceException(approverResponse);
        // }
        // }

        if (request.getDepartmentId() != null) {
            ResponseEntity<JsonNode> departmentResponse = userService.getDepartmentById(request.getDepartmentId(),
                    token);
            if (departmentResponse.getStatusCode().is2xxSuccessful()) {
                dto.setDepartment(departmentResponse.getBody());
            } else {
                throw new UserClientException(departmentResponse);
            }
        }

        // Lấy thông tin workflow nếu có workflowId (chỉ khi includeWorkflow = true)
        dto.setWorkflowId(request.getWorkflowId());
        dto.setSubmittedAt(request.getSubmittedAt());
        dto.setOwnerUserId(request.getOwnerUserId());

        if (includeWorkflow) {
            // Luôn gọi để lấy workflow info (có thể có tracking ngay cả khi chưa có
            // workflowId trong request)
            JsonNode workflowInfo = workflowServiceClient.getWorkflowInfoByRequestId(
                    request.getId(), request.getWorkflowId(), "REQUEST", token);
            if (workflowInfo != null) {
                dto.setWorkflowInfo(workflowInfo);
            }
        }

        return dto;
    }

    private PaginationDTO convertToWithUserDTOList(Page<RecruitmentRequest> requests,
            String token) {
        PaginationDTO rs = new PaginationDTO();
        Meta mt = createMetaWithCharacterLimits(requests);
        rs.setMeta(mt);

        // Lấy tất cả employee IDs cần thiết (requesterId và approvedId lưu employeeId)
        List<Long> employeeIds = requests.getContent().stream()
                .flatMap(request -> {
                    if (request.getRequesterId() != null) {
                        return List.of(request.getRequesterId()).stream();
                    }
                    return List.<Long>of().stream();
                })
                .distinct()
                .collect(Collectors.toList());

        // Lấy thông tin tất cả employees một lần (API này trả về employee với
        // department đã có sẵn)
        Map<Long, JsonNode> employeeMap = userService.getEmployeesByIds(employeeIds, token);

        // Lấy department từ employees
        Map<Long, JsonNode> departmentMap = new HashMap<>();

        // Lấy department từ employees (nếu có)
        for (JsonNode employee : employeeMap.values()) {
            if (employee != null && employee.has("department")) {
                JsonNode dept = employee.get("department");
                if (dept != null && dept.has("id")) {
                    Long deptId = dept.get("id").asLong();
                    departmentMap.put(deptId, dept);
                }
            }
        }

        // Convert từng request sử dụng cached data (không lấy workflow info)
        rs.setResult(requests.getContent().stream()
                .map(request -> {
                    RecruitmentRequestAllWithUserDTO dto = RecruitmentRequestAllWithUserDTO.fromEntity(request);

                    // Set requester info (requesterId lưu employeeId)
                    if (request.getRequesterId() != null) {
                        dto.setRequester(employeeMap.get(request.getRequesterId()));
                    }

                    // // Set approver info (approvedId lưu employeeId)
                    // if (request.getApprovedId() != null) {
                    // dto.setApprover(employeeMap.get(request.getApprovedId()));
                    // }

                    // Set department info từ employee của requester
                    if (request.getRequesterId() != null) {
                        JsonNode employee = employeeMap.get(request.getRequesterId());
                        if (employee != null && employee.has("department")) {
                            dto.setDepartment(employee.get("department"));
                        }
                    }
                    return dto;
                })
                .toList());
        return rs;
    }

    @Transactional
    public RecruitmentRequest update(Long id, CreateRecruitmentRequestDTO dto) throws IdInvalidException {
        RecruitmentRequest rr = this.findById(id);
        // JobCategory category = jobCategoryService.findById(dto.getJobCategoryId());

        rr.setTitle(dto.getTitle());
        rr.setQuantity(dto.getQuantity());
        rr.setReason(dto.getReason());

        // Set salary nếu có
        rr.setSalaryMin(dto.getSalaryMin());
        rr.setSalaryMax(dto.getSalaryMax());

        // rr.setJobCategory(category);
        rr.setDepartmentId(dto.getDepartmentId());

        return recruitmentRequestRepository.save(rr);
    }

    @Transactional
    public boolean delete(Long id) throws IdInvalidException {
        RecruitmentRequest rr = this.findById(id);
        rr.setActive(false);
        recruitmentRequestRepository.save(rr);
        return true;
    }

    @Transactional
    public RecruitmentRequest submit(Long id, Long actorId, String token) throws IdInvalidException {
        RecruitmentRequest request = this.findById(id);
        if (request.getStatus() != RecruitmentRequestStatus.DRAFT
                && request.getStatus() != RecruitmentRequestStatus.RETURNED) {
            throw new IllegalStateException("Chỉ có thể submit khi yêu cầu ở trạng thái DRAFT hoặc RETURNED");
        }
        // request.setStatus(RecruitmentRequestStatus.SUBMITTED);
        request.setStatus(RecruitmentRequestStatus.PENDING);

        request.setSubmittedAt(LocalDateTime.now());
        if (request.getOwnerUserId() == null) {
            request.setOwnerUserId(actorId);
        }

        // Nếu submit lại sau return, workflow-service sẽ xử lý tạo tracking từ
        // returnedToStepId
        // Nếu là submit lần đầu, workflow-service sẽ tạo tracking từ bước đầu tiên

        RecruitmentRequest saved = recruitmentRequestRepository.save(request);

        publishWorkflowEvent("REQUEST_SUBMITTED", saved, actorId, null, null, null, null, token);
        return saved;
    }

    @Transactional
    public RecruitmentRequest returnRequest(Long id, ReturnRecruitmentRequestDTO dto, Long actorId, String token)
            throws IdInvalidException {
        RecruitmentRequest request = this.findById(id);
        if (request.getStatus() != RecruitmentRequestStatus.SUBMITTED
                && request.getStatus() != RecruitmentRequestStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể trả về yêu cầu đang SUBMITTED hoặc PENDING");
        }

        request.setStatus(RecruitmentRequestStatus.RETURNED);
        // request.setApprovalNotes(dto.getReason());
        RecruitmentRequest saved = recruitmentRequestRepository.save(request);
        publishWorkflowEvent("REQUEST_RETURNED", saved, actorId, null, dto.getReason(), null,
                dto.getReturnedToStepId(), token);
        return saved;
    }

    @Transactional
    public RecruitmentRequest cancel(Long id, CancelRecruitmentRequestDTO dto, Long actorId, String token)
            throws IdInvalidException {
        RecruitmentRequest request = this.findById(id);
        if (request.getStatus() == RecruitmentRequestStatus.CANCELLED) {
            return request;
        }

        // Cancel có thể thực hiện ở bất kỳ trạng thái nào (trừ đã
        // APPROVED/REJECTED/CANCELLED)
        if (request.getStatus() == RecruitmentRequestStatus.APPROVED
                || request.getStatus() == RecruitmentRequestStatus.REJECTED) {
            throw new IllegalStateException("Không thể hủy yêu cầu đã được APPROVED hoặc REJECTED");
        }

        request.setStatus(RecruitmentRequestStatus.CANCELLED);
        // request.setApprovalNotes(dto.getReason());
        RecruitmentRequest saved = recruitmentRequestRepository.save(request);
        publishWorkflowEvent("REQUEST_CANCELLED", saved, actorId, null, dto.getReason(), null, null, token);
        return saved;
    }

    @Transactional
    public RecruitmentRequest withdraw(Long id, Long actorId, String token)
            throws IdInvalidException {
        RecruitmentRequest request = this.findById(id);

        // Chỉ có thể withdraw khi đang SUBMITTED hoặc PENDING
        if (request.getStatus() != RecruitmentRequestStatus.SUBMITTED
                && request.getStatus() != RecruitmentRequestStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể rút lại yêu cầu đang SUBMITTED hoặc PENDING");
        }

        // Chỉ submitter/owner mới có thể withdraw
        if (!request.getOwnerUserId().equals(actorId) && !request.getRequesterId().equals(actorId)) {
            throw new IllegalStateException("Chỉ submitter hoặc owner mới có thể rút lại yêu cầu");
        }

        request.setStatus(RecruitmentRequestStatus.WITHDRAWN);
        RecruitmentRequest saved = recruitmentRequestRepository.save(request);

        publishWorkflowEvent("REQUEST_WITHDRAWN", saved, actorId, null, null, null, null, token);
        return saved;
    }

    private void publishWorkflowEvent(String eventType,
            RecruitmentRequest request,
            Long actorId,
            String notes,
            String reason,
            Long currentStepId,
            Long returnedToStepId,
            String authToken) {
        RecruitmentWorkflowEvent event = RecruitmentWorkflowEvent.builder()
                .eventType(eventType)
                .requestType("RECRUITMENT_REQUEST")
                .requestId(request.getId())
                .workflowId(request.getWorkflowId())
                .currentStepId(currentStepId)
                .actorUserId(actorId)
                .notes(notes)
                .reason(reason)
                .requestStatus(request.getStatus().name())
                .ownerUserId(request.getOwnerUserId())
                .requesterId(request.getRequesterId())
                .departmentId(request.getDepartmentId())
                .occurredAt(LocalDateTime.now())
                .returnedToStepId(returnedToStepId)
                .build();
        event.setAuthToken(authToken);
        workflowProducer.publishEvent(event);
    }

    /**
     * Tạo Meta object với metadata về giới hạn ký tự
     */
    private Meta createMetaWithCharacterLimits(Page<RecruitmentRequest> pageRequests) {
        Meta mt = new Meta();
        mt.setPage(pageRequests.getNumber() + 1);
        mt.setPageSize(pageRequests.getSize());
        mt.setPages(pageRequests.getTotalPages());
        mt.setTotal(pageRequests.getTotalElements());

        // Thêm metadata về giới hạn ký tự cho Frontend
        mt.setCharacterLimits(TextTruncateUtil.getRecruitmentRequestCharacterLimits());

        return mt;
    }

}
