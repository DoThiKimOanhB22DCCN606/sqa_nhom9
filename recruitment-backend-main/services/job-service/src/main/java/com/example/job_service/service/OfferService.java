package com.example.job_service.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.job_service.dto.Meta;
import com.example.job_service.dto.PaginationDTO;
import com.example.job_service.dto.SingleResponseDTO;
import com.example.job_service.dto.offer.ApproveOfferDTO;
import com.example.job_service.dto.offer.CancelOfferDTO;
import com.example.job_service.dto.offer.CreateOfferDTO;
import com.example.job_service.dto.offer.OfferDetailDTO;
import com.example.job_service.dto.offer.OfferWithUserDTO;
import com.example.job_service.dto.offer.RejectOfferDTO;
import com.example.job_service.dto.offer.ReturnOfferDTO;
import com.example.job_service.dto.offer.UpdateOfferDTO;
import com.example.job_service.dto.offer.WithdrawOfferDTO;
import com.example.job_service.exception.IdInvalidException;
import com.example.job_service.exception.UserClientException;
import com.example.job_service.messaging.OfferWorkflowProducer;
import com.example.job_service.messaging.RecruitmentWorkflowEvent;
import com.example.job_service.model.Offer;
import com.example.job_service.repository.OfferRepository;
import com.example.job_service.utils.enums.OfferStatus;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class OfferService {
    private final OfferRepository offerRepository;
    private final UserClient userService;
    private final OfferWorkflowProducer workflowProducer;
    private final WorkflowClient workflowServiceClient;
    private final CandidateClient candidateClient;
    private final JobPositionService jobPositionService;

    public OfferService(
            OfferRepository offerRepository,
            UserClient userService,
            OfferWorkflowProducer workflowProducer,
            WorkflowClient workflowServiceClient,
            CandidateClient candidateClient,
            JobPositionService jobPositionService) {
        this.offerRepository = offerRepository;
        this.userService = userService;
        this.workflowProducer = workflowProducer;
        this.workflowServiceClient = workflowServiceClient;
        this.candidateClient = candidateClient;
        this.jobPositionService = jobPositionService;
    }

    @Transactional
    public Offer create(CreateOfferDTO dto) {
        Offer offer = new Offer();
        offer.setCandidateId(dto.getCandidateId());
        offer.setBasicSalary(dto.getBasicSalary());
        offer.setProbationSalaryRate(dto.getProbationSalaryRate());
        offer.setOnboardingDate(dto.getOnboardingDate());
        offer.setProbationPeriod(dto.getProbationPeriod());
        offer.setNotes(dto.getNotes());
        offer.setStatus(OfferStatus.DRAFT);
        offer.setIsActive(true);
        offer.setWorkflowId(dto.getWorkflowId());
        // requesterId sẽ được set từ controller

        return offerRepository.save(offer);
    }

    @Transactional
    public Offer update(Long id, UpdateOfferDTO dto) throws IdInvalidException {
        Offer offer = this.findById(id);
        // Workflow mới: không dùng trạng thái RETURNED cho offer nữa
        if (offer.getStatus() != OfferStatus.DRAFT) {
            throw new IllegalStateException("Chỉ có thể cập nhật khi offer ở trạng thái DRAFT");
        }

        if (dto.getCandidateId() != null) {
            offer.setCandidateId(dto.getCandidateId());
        }
        if (dto.getBasicSalary() != null) {
            offer.setBasicSalary(dto.getBasicSalary());
        }
        if (dto.getProbationSalaryRate() != null) {
            offer.setProbationSalaryRate(dto.getProbationSalaryRate());
        }
        if (dto.getOnboardingDate() != null) {
            offer.setOnboardingDate(dto.getOnboardingDate());
        }
        if (dto.getProbationPeriod() != null) {
            offer.setProbationPeriod(dto.getProbationPeriod());
        }
        if (dto.getNotes() != null) {
            offer.setNotes(dto.getNotes());
        }

        return offerRepository.save(offer);
    }

    @Transactional
    public Offer submit(Long id, Long actorId, String token) throws IdInvalidException {
        Offer offer = this.findById(id);
        // Workflow mới: không cho submit lại từ trạng thái RETURNED
        if (offer.getStatus() != OfferStatus.DRAFT) {
            throw new IllegalStateException("Chỉ có thể submit khi offer ở trạng thái DRAFT");
        }

        // Nếu chưa có workflowId, cần được truyền vào khi tạo offer
        if (offer.getWorkflowId() == null) {
            throw new IllegalStateException("WorkflowId là bắt buộc khi submit offer");
        }

        offer.setStatus(OfferStatus.PENDING);
        offer.setSubmittedAt(LocalDateTime.now());
        if (offer.getRequesterId() == null) {
            offer.setRequesterId(actorId);
        }
        if (offer.getOwnerUserId() == null) {
            offer.setOwnerUserId(actorId);
        }

        Offer saved = offerRepository.save(offer);
        publishWorkflowEvent("REQUEST_SUBMITTED", saved, actorId, null, null, null, null, token);
        return saved;
    }

    @Transactional
    public Offer approveStep(Long id, ApproveOfferDTO dto, Long actorId, String token) throws IdInvalidException {
        Offer offer = this.findById(id);
        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể phê duyệt offer ở trạng thái PENDING");
        }

        Offer saved = offerRepository.save(offer);
        publishWorkflowEvent("REQUEST_APPROVED", saved, actorId, dto.getApprovalNotes(), null, null, null,
                token);
        return saved;
    }

    @Transactional
    public Offer rejectStep(Long id, RejectOfferDTO dto, Long actorId, String token) throws IdInvalidException {
        Offer offer = this.findById(id);
        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể từ chối offer ở trạng thái PENDING");
        }

        offer.setStatus(OfferStatus.REJECTED);
        Offer saved = offerRepository.save(offer);
        publishWorkflowEvent("REQUEST_REJECTED", saved, actorId, null, dto.getReason(), null, null, token);
        return saved;
    }

    @Transactional
    public Offer returnOffer(Long id, ReturnOfferDTO dto, Long actorId, String token) throws IdInvalidException {
        // Theo nghiệp vụ mới, workflow của offer không còn bước "return"
        throw new IllegalStateException("Workflow offer không hỗ trợ trả về (return) nữa");
    }

    @Transactional
    public Offer cancel(Long id, CancelOfferDTO dto, Long actorId, String token) throws IdInvalidException {
        Offer offer = this.findById(id);
        if (offer.getStatus() == OfferStatus.CANCELLED) {
            return offer;
        }

        if (offer.getStatus() == OfferStatus.APPROVED || offer.getStatus() == OfferStatus.REJECTED) {
            throw new IllegalStateException("Không thể hủy offer đã được APPROVED hoặc REJECTED");
        }

        offer.setStatus(OfferStatus.CANCELLED);
        Offer saved = offerRepository.save(offer);
        publishWorkflowEvent("REQUEST_CANCELLED", saved, actorId, null, dto.getReason(), null, null, token);
        return saved;
    }

    @Transactional
    public Offer withdraw(Long id, WithdrawOfferDTO dto, Long actorId, String token) throws IdInvalidException {
        Offer offer = this.findById(id);

        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể rút lại offer đang PENDING");
        }

        if (!offer.getOwnerUserId().equals(actorId) && !offer.getRequesterId().equals(actorId)) {
            throw new IllegalStateException("Chỉ submitter hoặc owner mới có thể rút lại offer");
        }

        offer.setStatus(OfferStatus.WITHDRAWN);
        Offer saved = offerRepository.save(offer);
        publishWorkflowEvent("REQUEST_WITHDRAWN", saved, actorId, null, dto.getReason(), null, null, token);
        return saved;
    }

    public Offer findById(Long id) throws IdInvalidException {
        return offerRepository.findById(id)
                .orElseThrow(() -> new IdInvalidException("Offer không tồn tại"));
    }

    public OfferWithUserDTO getByIdWithUser(Long id, String token) throws IdInvalidException {
        Offer offer = this.findById(id);
        return convertToWithUserDTO(offer, token);
    }

    public SingleResponseDTO<OfferWithUserDTO> getByIdWithUserAndMetadata(Long id, String token)
            throws IdInvalidException {
        Offer offer = this.findById(id);
        OfferWithUserDTO dto = convertToWithUserDTO(offer, token);
        return new SingleResponseDTO<>(dto);
    }

    public OfferDetailDTO getByIdDetail(Long id, String token) throws IdInvalidException {
        Offer offer = this.findById(id);
        return convertToDetailDTO(offer, token);
    }

    // public PaginationDTO getAllByDepartmentIdWithUser(Long departmentId, String
    // token, Pageable pageable) {
    // // Use getAllWithFilters which handles pagination properly
    // return getAllWithFilters(departmentId, null, null, null, token, pageable);
    // }

    // public PaginationDTO getAllWithUser(String token, Pageable pageable) {
    // Page<Offer> offers = offerRepository.findByIsActiveTrue(pageable);
    // return convertToWithUserDTOList(offers, token);
    // }

    public PaginationDTO getAllWithFilters(String status, Long createdBy, String keyword,
            String token, Pageable pageable) {
        OfferStatus statusEnum = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                statusEnum = OfferStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid status, will be ignored
            }
        }

        Page<Offer> offers = offerRepository.findByFilters(statusEnum, createdBy, keyword, pageable);
        return convertToWithUserDTOList(offers, token);
    }

    public List<Offer> findAllWithFilters(String status, Long candidateId,
            Long workflowId, Long ownerUserId, Long minSalary, Long maxSalary,
            LocalDate onboardingDateFrom, LocalDate onboardingDateTo,
            String keyword) {
        OfferStatus statusEnum = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                statusEnum = OfferStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid status, will be ignored
            }
        }
        return offerRepository.findByFiltersList(statusEnum, candidateId, workflowId,
                ownerUserId, minSalary, maxSalary, onboardingDateFrom, onboardingDateTo,
                keyword);
    }

    @Transactional
    public boolean delete(Long id) throws IdInvalidException {
        Offer offer = this.findById(id);
        offer.setIsActive(false);
        offerRepository.save(offer);
        return true;
    }

    private OfferWithUserDTO convertToWithUserDTO(Offer offer, String token) {
        OfferWithUserDTO dto = OfferWithUserDTO.fromEntity(offer);

        if (offer.getRequesterId() != null) {
            ResponseEntity<JsonNode> requesterResponse = userService.getEmployeeById(offer.getRequesterId(), token);
            if (requesterResponse.getStatusCode().is2xxSuccessful()) {
                dto.setRequester(requesterResponse.getBody());
            } else {
                throw new UserClientException(requesterResponse);
            }
        }

        // Lấy thông tin candidate từ candidate-service
        if (offer.getCandidateId() != null) {
            ResponseEntity<JsonNode> candidateResponse = candidateClient.getCandidateById(offer.getCandidateId(),
                    token);
            if (candidateResponse.getStatusCode().is2xxSuccessful() && candidateResponse.getBody() != null) {
                dto.setCandidate(candidateResponse.getBody());

                // Lấy jobPositionId từ candidate
                JsonNode candidate = candidateResponse.getBody();
                if (candidate.has("jobPositionId") && !candidate.get("jobPositionId").isNull()) {
                    Long jobPositionId = candidate.get("jobPositionId").asLong();

                    try {
                        // Lấy thông tin JobPosition
                        var jobPosition = jobPositionService.findById(jobPositionId);
                        dto.setJobPositionTitle(jobPosition.getTitle());

                        // Lấy departmentId từ RecruitmentRequest
                        if (jobPosition.getRecruitmentRequest() != null
                                && jobPosition.getRecruitmentRequest().getDepartmentId() != null) {
                            Long departmentId = jobPosition.getRecruitmentRequest().getDepartmentId();
                            ResponseEntity<JsonNode> deptResponse = userService.getDepartmentById(departmentId, token);
                            if (deptResponse.getStatusCode().is2xxSuccessful() && deptResponse.getBody() != null) {
                                JsonNode department = deptResponse.getBody();
                                if (department.has("name")) {
                                    dto.setDepartmentName(department.get("name").asText());
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Ignore errors, các field sẽ là null
                    }
                }
            }
        }

        // Lấy level name từ employee (requester hoặc owner)
        // Level thường nằm trong position của employee
        try {
            Long employeeId = offer.getRequesterId() != null ? offer.getRequesterId() : offer.getOwnerUserId();
            if (employeeId != null) {
                ResponseEntity<JsonNode> employeeResponse = userService.getEmployeeById(employeeId, token);
                if (employeeResponse.getStatusCode().is2xxSuccessful() && employeeResponse.getBody() != null) {
                    JsonNode employee = employeeResponse.getBody();
                    // Level có thể nằm trong position.level hoặc position.name
                    if (employee.has("position")) {
                        JsonNode position = employee.get("position");
                        if (position.has("level")) {
                            dto.setLevelName(position.get("level").asText());
                        } else if (position.has("name")) {
                            dto.setLevelName(position.get("name").asText());
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors, levelName sẽ là null
        }

        // Lấy thông tin workflow
        dto.setWorkflowId(offer.getWorkflowId());
        dto.setSubmittedAt(offer.getSubmittedAt());
        dto.setOwnerUserId(offer.getOwnerUserId());

        JsonNode workflowInfo = workflowServiceClient.getWorkflowInfoByRequestId(offer.getId(), offer.getWorkflowId(),
                "OFFER", token);
        if (workflowInfo != null) {
            dto.setWorkflowInfo(workflowInfo);
        }

        return dto;
    }

    private OfferDetailDTO convertToDetailDTO(Offer offer, String token) {
        OfferDetailDTO dto = new OfferDetailDTO();

        // Thông tin offer cơ bản
        dto.setId(offer.getId());
        dto.setCandidateId(offer.getCandidateId());
        dto.setBasicSalary(offer.getBasicSalary());
        dto.setProbationSalaryRate(offer.getProbationSalaryRate());
        dto.setOnboardingDate(offer.getOnboardingDate());
        dto.setProbationPeriod(offer.getProbationPeriod());
        dto.setNotes(offer.getNotes());
        dto.setStatus(offer.getStatus());
        dto.setRequesterId(offer.getRequesterId());
        dto.setOwnerUserId(offer.getOwnerUserId());
        dto.setWorkflowId(offer.getWorkflowId());
        dto.setSubmittedAt(offer.getSubmittedAt());
        dto.setCreatedAt(offer.getCreatedAt());
        dto.setUpdatedAt(offer.getUpdatedAt());

        // Lấy thông tin requester (chỉ tên)
        if (offer.getRequesterId() != null) {
            try {
                ResponseEntity<JsonNode> requesterResponse = userService.getEmployeeById(offer.getRequesterId(), token);
                if (requesterResponse.getStatusCode().is2xxSuccessful() && requesterResponse.getBody() != null) {
                    JsonNode requester = requesterResponse.getBody();
                    if (requester.has("name")) {
                        dto.setRequesterName(requester.get("name").asText());
                    }
                }
            } catch (Exception e) {
                // Ignore errors
            }
        }

        // Lấy thông tin candidate (chỉ name, email, phone)
        if (offer.getCandidateId() != null) {
            try {
                ResponseEntity<JsonNode> candidateResponse = candidateClient.getCandidateById(offer.getCandidateId(),
                        token);
                if (candidateResponse.getStatusCode().is2xxSuccessful() && candidateResponse.getBody() != null) {
                    JsonNode candidate = candidateResponse.getBody();
                    if (candidate.has("name")) {
                        dto.setCandidateName(candidate.get("name").asText());
                    }
                    if (candidate.has("email")) {
                        dto.setCandidateEmail(candidate.get("email").asText());
                    }
                    if (candidate.has("phone")) {
                        dto.setCandidatePhone(candidate.get("phone").asText());
                    }

                    // Lấy jobPositionId từ candidate để lấy thông tin vị trí
                    if (candidate.has("jobPositionId") && !candidate.get("jobPositionId").isNull()) {
                        Long jobPositionId = candidate.get("jobPositionId").asLong();

                        try {
                            // Lấy thông tin JobPosition
                            var jobPosition = jobPositionService.findById(jobPositionId);
                            dto.setJobPositionTitle(jobPosition.getTitle());

                            // Lấy departmentId từ RecruitmentRequest
                            if (jobPosition.getRecruitmentRequest() != null
                                    && jobPosition.getRecruitmentRequest().getDepartmentId() != null) {
                                Long departmentId = jobPosition.getRecruitmentRequest().getDepartmentId();
                                ResponseEntity<JsonNode> deptResponse = userService.getDepartmentById(departmentId,
                                        token);
                                if (deptResponse.getStatusCode().is2xxSuccessful() && deptResponse.getBody() != null) {
                                    JsonNode department = deptResponse.getBody();
                                    if (department.has("name")) {
                                        dto.setDepartmentName(department.get("name").asText());
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Ignore errors
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore errors
            }
        }

        // Lấy level name từ employee (requester hoặc owner)
        try {
            Long employeeId = offer.getRequesterId() != null ? offer.getRequesterId() : offer.getOwnerUserId();
            if (employeeId != null) {
                ResponseEntity<JsonNode> employeeResponse = userService.getEmployeeById(employeeId, token);
                if (employeeResponse.getStatusCode().is2xxSuccessful() && employeeResponse.getBody() != null) {
                    JsonNode employee = employeeResponse.getBody();
                    // Level có thể nằm trong position.level hoặc position.name
                    if (employee.has("position")) {
                        JsonNode position = employee.get("position");
                        if (position.has("level")) {
                            dto.setLevelName(position.get("level").asText());
                        } else if (position.has("name")) {
                            dto.setLevelName(position.get("name").asText());
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors
        }

        // Lấy thông tin workflow
        if (offer.getWorkflowId() != null) {
            JsonNode workflowInfo = workflowServiceClient.getWorkflowInfoByRequestId(offer.getId(),
                    offer.getWorkflowId(), "OFFER", token);
            if (workflowInfo != null) {
                dto.setWorkflowInfo(workflowInfo);
            }
        }

        return dto;
    }

    private PaginationDTO convertToWithUserDTOList(Page<Offer> offers, String token) {
        PaginationDTO rs = new PaginationDTO();
        Meta mt = new Meta();
        mt.setPage(offers.getNumber() + 1);
        mt.setPageSize(offers.getSize());
        mt.setPages(offers.getTotalPages());
        mt.setTotal(offers.getTotalElements());
        rs.setMeta(mt);

        List<Long> employeeIds = offers.getContent().stream()
                .filter(o -> o.getRequesterId() != null)
                .map(Offer::getRequesterId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, JsonNode> employeeMap = userService.getEmployeesByIds(employeeIds, token);

        rs.setResult(offers.getContent().stream()
                .map(offer -> {
                    OfferWithUserDTO dto = convertToWithUserDTO(offer, token);
                    if (offer.getRequesterId() != null) {
                        dto.setRequester(employeeMap.get(offer.getRequesterId()));
                    }
                    return dto;
                })
                .toList());
        return rs;
    }

    private void publishWorkflowEvent(String eventType, Offer offer, Long actorId, String notes, String reason,
            Long currentStepId, Long returnedToStepId, String authToken) {
        RecruitmentWorkflowEvent event = RecruitmentWorkflowEvent.builder()
                .eventType(eventType)
                .requestType("OFFER")
                .requestId(offer.getId())
                .workflowId(offer.getWorkflowId())
                .candidateId(offer.getCandidateId())
                .currentStepId(currentStepId)
                .actorUserId(actorId)
                .notes(notes)
                .reason(reason)
                .requestStatus(offer.getStatus().name())
                .ownerUserId(offer.getOwnerUserId())
                .requesterId(offer.getRequesterId())
                // DepartmentId được suy ra tại workflow-service bằng candidateId
                .departmentId(2L)
                .occurredAt(LocalDateTime.now())
                .returnedToStepId(returnedToStepId)
                .build();
        event.setAuthToken(authToken);
        workflowProducer.publishEvent(event);
    }
}
