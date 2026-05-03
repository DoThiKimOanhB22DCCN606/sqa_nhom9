package com.example.job_service.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.example.job_service.dto.Meta;
import com.example.job_service.dto.PaginationDTO;
import com.example.job_service.dto.SingleResponseDTO;
import com.example.job_service.dto.jobposition.CreateJobPositionDTO;
import com.example.job_service.dto.jobposition.GetAllJobPositionDTO;
import com.example.job_service.dto.jobposition.JobPositionResponseDTO;
import com.example.job_service.dto.jobposition.UpdateJobPositionDTO;
import com.example.job_service.utils.TextTruncateUtil;
import com.example.job_service.exception.IdInvalidException;
import com.example.job_service.model.JobPosition;
import com.example.job_service.model.RecruitmentRequest;
import com.example.job_service.repository.JobPositionRepository;
import com.example.job_service.utils.enums.JobPositionStatus;
import com.example.job_service.utils.enums.RecruitmentRequestStatus;

@Service
public class JobPositionService {
    private final JobPositionRepository jobPositionRepository;
    private final RecruitmentRequestService recruitmentRequestService;
    private final UserClient userService;
    private final CandidateClient candidateClient;

    public JobPositionService(JobPositionRepository jobPositionRepository,
            RecruitmentRequestService recruitmentRequestService, UserClient userService,
            CandidateClient candidateClient) {
        this.jobPositionRepository = jobPositionRepository;
        this.recruitmentRequestService = recruitmentRequestService;
        this.userService = userService;
        this.candidateClient = candidateClient;
    }

    @Transactional
    public JobPosition create(CreateJobPositionDTO dto) throws IdInvalidException {
        RecruitmentRequest rr = recruitmentRequestService.findById(dto.getRecruitmentRequestId());

        JobPosition position = new JobPosition();
        position.setTitle(dto.getTitle());
        position.setDescription(dto.getDescription());
        position.setRequirements(dto.getRequirements());
        position.setBenefits(dto.getBenefits());

        // Fill from RecruitmentRequest if not provided in DTO
        // Lấy salary từ DTO nếu có, nếu không thì lấy từ RecruitmentRequest
        position.setSalaryMin(dto.getSalaryMin() != null ? dto.getSalaryMin() : rr.getSalaryMin());
        position.setSalaryMax(dto.getSalaryMax() != null ? dto.getSalaryMax() : rr.getSalaryMax());
        position.setEmploymentType(dto.getEmploymentType());
        position.setExperienceLevel(dto.getExperienceLevel());
        position.setLocation(dto.getLocation());
        position.setRemote(dto.getIsRemote() != null ? dto.getIsRemote() : false);
        position.setYearsOfExperience(dto.getYearsOfExperience());

        position.setQuantity(dto.getQuantity());
        position.setDeadline(dto.getDeadline());
        position.setStatus(JobPositionStatus.DRAFT);
        position.setRecruitmentRequest(rr);

        position = jobPositionRepository.save(position);

        recruitmentRequestService.changeStatus(rr.getId(), RecruitmentRequestStatus.COMPLETED);
        return position;
    }

    public JobPosition findById(Long id) throws IdInvalidException {
        return jobPositionRepository.findById(id)
                .orElseThrow(() -> new IdInvalidException("Vị trí tuyển dụng không tồn tại"));
    }

    /**
     * Lấy job position đơn giản theo ID, không gọi service khác
     * Dùng cho service-to-service calls
     */
    public JobPosition getByIdSimple(Long id) throws IdInvalidException {
        return findById(id);
    }

    public JobPositionResponseDTO getByIdWithDepartmentName(Long id, String token) throws IdInvalidException {
        JobPosition position = this.findById(id);
        JobPositionResponseDTO dto = JobPositionResponseDTO.fromEntity(position);
        Long deptId = position.getRecruitmentRequest().getDepartmentId();
        String deptName = userService.getDepartmentById(deptId, token).getBody().get("name").asText();
        dto.setDepartmentName(deptName);
        // Lấy applicationCount từ candidate-service
        Integer applicationCount = candidateClient.countCandidatesByJobPositionId(id, token);
        dto.setApplicationCount(applicationCount);
        return dto;
    }

    /**
     * Lấy nhiều job positions theo IDs và trả về DTO với department info
     */
    public List<JobPositionResponseDTO> getByIdsWithDepartmentName(List<Long> ids, String token) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<JobPosition> positions = jobPositionRepository.findByIdIn(ids);
        if (positions.isEmpty()) {
            return List.of();
        }

        // Lấy tất cả department IDs
        List<Long> departmentIds = positions.stream()
                .map(position -> position.getRecruitmentRequest().getDepartmentId())
                .distinct()
                .toList();

        // Lấy department names một lần
        Map<Long, String> departmentNames = userService.getDepartmentsByIds(departmentIds, token);

        // Convert to DTO
        return positions.stream()
                .map(position -> {
                    JobPositionResponseDTO dto = JobPositionResponseDTO.fromEntity(position, false);
                    Long deptId = position.getRecruitmentRequest().getDepartmentId();
                    dto.setDepartmentName(departmentNames.getOrDefault(deptId, "Unknown Department"));
                    return dto;
                })
                .toList();
    }

    // public SingleResponseDTO<JobPositionResponseDTO>
    // getByIdWithDepartmentNameAndMetadata(Long id, String token)
    // throws IdInvalidException {
    // JobPosition position = this.findById(id);
    // JobPositionResponseDTO dto = JobPositionResponseDTO.fromEntity(position);
    // Long deptId = position.getRecruitmentRequest().getDepartmentId();
    // String deptName = userService.getDepartmentById(deptId,
    // token).getBody().get("name").asText();
    // dto.setDepartmentName(deptName);
    // return new SingleResponseDTO<>(dto,
    // TextTruncateUtil.getJobPositionCharacterLimits());
    // }

    public JobPosition getByIdWithPublished(Long id) throws IdInvalidException {
        JobPosition position = this.findById(id);
        if (position.getStatus() != JobPositionStatus.PUBLISHED) {
            throw new IdInvalidException("Vị trí tuyển dụng chưa được xuất bản hoặc không khả dụng");
        }
        return position;
    }

    // public SingleResponseDTO<JobPositionResponseDTO>
    // getByIdWithPublishedAndMetadata(Long id)
    // throws IdInvalidException {
    // JobPosition position = this.findById(id);
    // if (position.getStatus() != JobPositionStatus.PUBLISHED) {
    // throw new IdInvalidException("Vị trí tuyển dụng chưa được xuất bản hoặc không
    // khả dụng");
    // }
    // JobPositionResponseDTO dto = JobPositionResponseDTO.fromEntity(position);
    // Long deptId = position.getRecruitmentRequest().getDepartmentId();
    // String deptName =
    // userService.getPublicDepartmentById(deptId).getBody().get("name").asText();
    // dto.setDepartmentName(deptName);
    // return new SingleResponseDTO<>(dto,
    // TextTruncateUtil.getJobPositionCharacterLimits());
    // }

    // public PaginationDTO findAll(Pageable pageable) {
    // Page<JobPosition> pageJobPosition = jobPositionRepository.findAll(pageable);
    // PaginationDTO rs = new PaginationDTO();
    // Meta mt = createMetaWithCharacterLimits(pageJobPosition);
    // rs.setMeta(mt);
    // rs.setResult(pageJobPosition.getContent().stream()
    // .map(position -> JobPositionResponseDTO.fromEntity(position, true))
    // .toList());
    // return rs;
    // }

    // public List<JobPosition> findByStatus(JobPositionStatus status) {
    // return jobPositionRepository.findByStatus(status);
    // }

    // public List<JobPosition> findPublished() {
    // return jobPositionRepository.findByStatus(JobPositionStatus.PUBLISHED);
    // }

    // public PaginationDTO findByDepartmentId(Long departmentId, Pageable pageable)
    // {
    // Page<JobPosition> pageJobPosition =
    // jobPositionRepository.findByDepartmentId(departmentId, pageable);
    // PaginationDTO rs = new PaginationDTO();
    // Meta mt = createMetaWithCharacterLimits(pageJobPosition);
    // rs.setMeta(mt);
    // rs.setResult(pageJobPosition.getContent().stream()
    // .map(position -> JobPositionResponseDTO.fromEntity(position, true))
    // .toList());
    // return rs;
    // }

    // public List<JobPosition> findByDepartmentIdAndStatus(Long departmentId,
    // JobPositionStatus status) {
    // return jobPositionRepository.findByDepartmentIdAndStatus(departmentId,
    // status);
    // }

    /**
     * Lấy danh sách JobPosition với filter, không gọi service khác
     */
    public List<JobPosition> findAllWithFiltersSimple(Long departmentId, JobPositionStatus status,
            Boolean published, String keyword, String ids) {
        // Nếu có ids, lấy theo IDs trước
        if (ids != null && !ids.trim().isEmpty()) {
            try {
                List<Long> idList = List.of(ids.split(","))
                        .stream()
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Long::valueOf)
                        .toList();
                if (!idList.isEmpty()) {
                    return jobPositionRepository.findByIdIn(idList);
                }
            } catch (Exception e) {
                System.err.println("Error parsing ids: " + e.getMessage());
            }
        }

        // Use repository findByFilters with unlimited pageable
        Pageable unlimitedPageable = PageRequest.of(0, Integer.MAX_VALUE);
        Page<JobPosition> pageResult = jobPositionRepository.findByFilters(
                departmentId, status, published, keyword, unlimitedPageable);
        return pageResult.getContent();
    }

    /**
     * Lấy danh sách JobPosition với filter và phân trang, không gọi service khác
     */
    public PaginationDTO findAllWithFiltersSimplePaged(Long departmentId, JobPositionStatus status,
            Boolean published, String keyword, String ids, Pageable pageable) {
        Page<JobPosition> pageJobPosition;

        // Nếu có ids, lấy theo IDs trước
        if (ids != null && !ids.trim().isEmpty()) {
            try {
                List<Long> idList = List.of(ids.split(","))
                        .stream()
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Long::valueOf)
                        .toList();
                if (!idList.isEmpty()) {
                    // Lấy tất cả và phân trang thủ công
                    List<JobPosition> allPositions = jobPositionRepository.findByIdIn(idList);
                    int start = (int) pageable.getOffset();
                    int end = Math.min((start + pageable.getPageSize()), allPositions.size());
                    List<JobPosition> pagedContent = start < allPositions.size()
                            ? allPositions.subList(start, end)
                            : List.of();

                    pageJobPosition = new PageImpl<>(pagedContent, pageable, allPositions.size());
                } else {
                    pageJobPosition = Page.empty(pageable);
                }
            } catch (Exception e) {
                System.err.println("Error parsing ids: " + e.getMessage());
                pageJobPosition = Page.empty(pageable);
            }
        } else {
            pageJobPosition = jobPositionRepository.findByFilters(departmentId, status,
                    published, keyword, pageable);
        }

        PaginationDTO rs = new PaginationDTO();
        Meta mt = new Meta();
        mt.setPage(pageJobPosition.getNumber() + 1);
        mt.setPageSize(pageJobPosition.getSize());
        mt.setPages(pageJobPosition.getTotalPages());
        mt.setTotal(pageJobPosition.getTotalElements());
        rs.setMeta(mt);
        rs.setResult(pageJobPosition.getContent());
        return rs;
    }

    public PaginationDTO findAllWithFilters(Long departmentId, JobPositionStatus status,
            Boolean published, String keyword, Pageable pageable, String token) {
                if (departmentId != null && departmentId == 1) {
                    departmentId = null;
                }

        Page<JobPosition> pageJobPosition = jobPositionRepository.findByFilters(departmentId, status,
                published, keyword, pageable);

        // Get unique department IDs
        List<Long> departmentIds = pageJobPosition.getContent().stream()
                .map(position -> position.getRecruitmentRequest().getDepartmentId())
                .distinct()
                .toList();

        // Get unique job position IDs for application count
        List<Long> jobPositionIds = pageJobPosition.getContent().stream()
                .map(JobPosition::getId)
                .distinct()
                .toList();

        // Fetch departments by IDs from UserService in one call
        Map<Long, String> departmentNames = userService.getDepartmentsByIds(departmentIds, token);

        // Fetch application counts by job position IDs from CandidateService
        Map<Long, Integer> applicationCounts = candidateClient.countCandidatesByJobPositionIds(jobPositionIds, token);

        PaginationDTO rs = new PaginationDTO();
        Meta mt = createMetaWithCharacterLimits(pageJobPosition);
        rs.setMeta(mt);

        // Convert to JobPositionResponseDTO with department names and application
        // counts
        List<JobPositionResponseDTO> result = pageJobPosition.getContent().stream()
                .map(position -> {
                    JobPositionResponseDTO dto = JobPositionResponseDTO.fromEntity(position, true);
                    Long deptId = position.getRecruitmentRequest().getDepartmentId();
                    dto.setDepartmentName(departmentNames.getOrDefault(deptId, "Unknown Department"));
                    // Set applicationCount từ candidate-service
                    Integer count = applicationCounts.getOrDefault(position.getId(), 0);
                    dto.setApplicationCount(count);
                    return dto;
                })
                .toList();

        rs.setResult(result);
        return rs;
    }

    public PaginationDTO findAllWithFiltersSimplified(Long departmentId, JobPositionStatus status, Long categoryId,
            Boolean published, String keyword, Pageable pageable, String token) {
        Page<JobPosition> pageJobPosition = jobPositionRepository.findByFilters(departmentId, status,
                published, keyword, pageable);

        // Get unique department IDs
        List<Long> departmentIds = pageJobPosition.getContent().stream()
                .map(position -> position.getRecruitmentRequest().getDepartmentId())
                .distinct()
                .toList();

        // Get unique job position IDs for application count
        List<Long> jobPositionIds = pageJobPosition.getContent().stream()
                .map(JobPosition::getId)
                .distinct()
                .toList();

        // Fetch departments by IDs from UserService in one call
        Map<Long, String> departmentNames = userService.getDepartmentsByIds(departmentIds, token);

        // Fetch application counts by job position IDs from CandidateService
        Map<Long, Integer> applicationCounts = candidateClient.countCandidatesByJobPositionIds(jobPositionIds, token);

        PaginationDTO rs = new PaginationDTO();
        Meta mt = createMetaWithCharacterLimits(pageJobPosition);
        rs.setMeta(mt);

        // Convert to GetAllJobPositionDTO with department names and application counts
        List<GetAllJobPositionDTO> result = pageJobPosition.getContent().stream()
                .map(position -> {
                    GetAllJobPositionDTO dto = GetAllJobPositionDTO.fromEntity(position);
                    Long deptId = position.getRecruitmentRequest().getDepartmentId();
                    dto.setDepartmentName(departmentNames.getOrDefault(deptId, "Unknown Department"));
                    // Set applicationCount từ candidate-service
                    Integer count = applicationCounts.getOrDefault(position.getId(), 0);
                    dto.setApplicationCount(count);
                    return dto;
                })
                .toList();

        rs.setResult(result);
        return rs;
    }

    @Transactional
    public JobPosition update(Long id, UpdateJobPositionDTO dto) throws IdInvalidException {
        JobPosition position = this.findById(id);

        if (dto.getTitle() != null) {
            position.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            position.setDescription(dto.getDescription());
        }
        if (dto.getRequirements() != null) {
            position.setRequirements(dto.getRequirements());
        }
        if (dto.getBenefits() != null) {
            position.setBenefits(dto.getBenefits());
        }
        if (dto.getSalaryMin() != null) {
            position.setSalaryMin(dto.getSalaryMin());
        }
        if (dto.getSalaryMax() != null) {
            position.setSalaryMax(dto.getSalaryMax());
        }
        if (dto.getEmploymentType() != null) {
            position.setEmploymentType(dto.getEmploymentType());
        }
        if (dto.getExperienceLevel() != null) {
            position.setExperienceLevel(dto.getExperienceLevel());
        }
        if (dto.getLocation() != null) {
            position.setLocation(dto.getLocation());
        }
        if (dto.getIsRemote() != null) {
            position.setRemote(dto.getIsRemote());
        }
        if (dto.getQuantity() != null) {
            position.setQuantity(dto.getQuantity());
        }

        if (dto.getDeadline() != null) {
            position.setDeadline(dto.getDeadline());
        }
        if (dto.getYearsOfExperience() != null) {
            position.setYearsOfExperience(dto.getYearsOfExperience());
        }

        return jobPositionRepository.save(position);
    }

    @Transactional
    public boolean delete(Long id) throws IdInvalidException {
        JobPosition position = this.findById(id);
        jobPositionRepository.delete(position);
        return true;
    }

    @Transactional
    public JobPosition publish(Long id) throws IdInvalidException {
        JobPosition position = this.findById(id);
        if (position.getStatus() != JobPositionStatus.DRAFT) {
            throw new IdInvalidException("Chỉ có thể publish vị trí ở trạng thái DRAFT");
        }
        position.setStatus(JobPositionStatus.PUBLISHED);
        position.setPublishedAt(LocalDateTime.now());
        return jobPositionRepository.save(position);
    }

    @Transactional
    public JobPosition close(Long id) throws IdInvalidException {
        JobPosition position = this.findById(id);
        if (position.getStatus() != JobPositionStatus.PUBLISHED) {
            throw new IdInvalidException("Chỉ có thể đóng vị trí ở trạng thái PUBLISHED");
        }
        position.setStatus(JobPositionStatus.CLOSED);
        return jobPositionRepository.save(position);
    }

    @Transactional
    public JobPosition reopen(Long id) throws IdInvalidException {
        JobPosition position = this.findById(id);
        if (position.getStatus() != JobPositionStatus.CLOSED) {
            throw new IdInvalidException("Chỉ có thể mở lại vị trí ở trạng thái CLOSED");
        }
        position.setStatus(JobPositionStatus.PUBLISHED);
        return jobPositionRepository.save(position);
    }

    /**
     * Tạo Meta object với metadata về giới hạn ký tự
     */
    private Meta createMetaWithCharacterLimits(Page<JobPosition> pageJobPosition) {
        Meta mt = new Meta();
        mt.setPage(pageJobPosition.getNumber() + 1);
        mt.setPageSize(pageJobPosition.getSize());
        mt.setPages(pageJobPosition.getTotalPages());
        mt.setTotal(pageJobPosition.getTotalElements());

        // Thêm metadata về giới hạn ký tự cho Frontend
        mt.setCharacterLimits(TextTruncateUtil.getJobPositionCharacterLimits());

        return mt;
    }
}
