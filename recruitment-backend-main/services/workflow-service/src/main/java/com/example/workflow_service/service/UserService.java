package com.example.workflow_service.service;

import com.example.workflow_service.dto.Response;
import com.example.workflow_service.dto.PaginationDTO;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final RestTemplate restTemplate;

    @Value("${user-service.url:http://localhost:8082}")
    private String userServiceBaseUrl;

    public Map<Long, String> getUserNamesByIds(List<Long> userIds, String token) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }
        try {
            // Tạo query string với các IDs
            String idsParam = userIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

            String url = UriComponentsBuilder.fromUriString(userServiceBaseUrl)
                    .path("/api/v1/user-service/employees")
                    .queryParam("ids", idsParam)
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            if (token != null && !token.isEmpty()) {
                headers.setBearerAuth(token);
            }

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            // User-service trả về Response<List<EmployeeDTO>> với field data
            ResponseEntity<Response<List<EmployeeDTO>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<Response<List<EmployeeDTO>>>() {
                    });

            Response<List<EmployeeDTO>> responseBody = response.getBody();
            if (responseBody == null || responseBody.getData() == null) {
                return new HashMap<>();
            }

            List<EmployeeDTO> employees = responseBody.getData();

            // Tạo Map từ userId -> userName
            return employees.stream()
                    .filter(e -> e.getId() != null && e.getName() != null)
                    .collect(Collectors.toMap(
                            EmployeeDTO::getId,
                            EmployeeDTO::getName,
                            (existing, replacement) -> existing // Nếu trùng key, giữ giá trị cũ
                    ));
        } catch (Exception ex) {
            // Log error và trả về map rỗng
            System.err.println("Error fetching user names: " + ex.getMessage());
            return new HashMap<>();
        }
    }

    public Map<Long, String> getPositionNamesByIds(List<Long> positionIds, String token) {
        if (positionIds == null || positionIds.isEmpty()) {
            return new HashMap<>();
        }

        try {
            // Tạo query string với các IDs
            String idsParam = positionIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

            String url = UriComponentsBuilder.fromUriString(userServiceBaseUrl)
                    .path("/api/v1/user-service/positions")
                    .queryParam("ids", idsParam)
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            if (token != null && !token.isEmpty()) {
                headers.setBearerAuth(token);
            }

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            // User-service trả về Response<List<PositionDTO>> với field data
            ResponseEntity<Response<List<PositionDTO>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<Response<List<PositionDTO>>>() {
                    });

            Response<List<PositionDTO>> responseBody = response.getBody();
            if (responseBody == null || responseBody.getData() == null) {
                return new HashMap<>();
            }

            List<PositionDTO> positions = responseBody.getData();

            // Tạo Map từ positionId -> positionName
            return positions.stream()
                    .filter(p -> p.getId() != null && p.getName() != null)
                    .collect(Collectors.toMap(
                            PositionDTO::getId,
                            PositionDTO::getName,
                            (existing, replacement) -> existing // Nếu trùng key, giữ giá trị cũ
                    ));
        } catch (Exception ex) {
            // Log error và trả về map rỗng
            System.err.println("Error fetching position names: " + ex.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Lấy hierarchyOrder của các positions theo IDs
     * 
     * @param positionIds Danh sách position IDs
     * @param token       JWT token để xác thực
     * @return Map từ positionId -> hierarchyOrder
     */
    public Map<Long, Integer> getPositionHierarchyOrdersByIds(List<Long> positionIds, String token) {
        if (positionIds == null || positionIds.isEmpty()) {
            return new HashMap<>();
        }

        try {
            // Tạo query string với các IDs
            String idsParam = positionIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

            String url = UriComponentsBuilder.fromUriString(userServiceBaseUrl)
                    .path("/api/v1/user-service/positions")
                    .queryParam("ids", idsParam)
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            if (token != null && !token.isEmpty()) {
                headers.setBearerAuth(token);
            }

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            // User-service trả về Response<List<PositionDTO>> với field data
            ResponseEntity<Response<List<PositionDTO>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<Response<List<PositionDTO>>>() {
                    });

            Response<List<PositionDTO>> responseBody = response.getBody();
            if (responseBody == null || responseBody.getData() == null) {
                return new HashMap<>();
            }

            List<PositionDTO> positions = responseBody.getData();

            // Tạo Map từ positionId -> hierarchyOrder
            return positions.stream()
                    .filter(p -> p.getId() != null && p.getHierarchyOrder() != null)
                    .collect(Collectors.toMap(
                            PositionDTO::getId,
                            PositionDTO::getHierarchyOrder,
                            (existing, replacement) -> existing // Nếu trùng key, giữ giá trị cũ
                    ));
        } catch (Exception ex) {
            // Log error và trả về map rỗng
            System.err.println("Error fetching position hierarchy orders: " + ex.getMessage());
            return new HashMap<>();
        }
    }

    public Map<Long, String> getPositionNamesByUserIds(List<Long> userIds, String token) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }

        try {
            // Tạo query string với các IDs
            String idsParam = userIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

            String url = UriComponentsBuilder.fromUriString(userServiceBaseUrl)
                    .path("/api/v1/user-service/employees")
                    .queryParam("ids", idsParam)
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            if (token != null && !token.isEmpty()) {
                headers.setBearerAuth(token);
            }

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            // User-service trả về Response<List<EmployeeWithPositionDTO>> với field data
            ResponseEntity<Response<List<EmployeeWithPositionDTO>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<Response<List<EmployeeWithPositionDTO>>>() {
                    });

            Response<List<EmployeeWithPositionDTO>> responseBody = response.getBody();
            if (responseBody == null || responseBody.getData() == null) {
                return new HashMap<>();
            }

            List<EmployeeWithPositionDTO> employees = responseBody.getData();

            // Tạo Map từ userId -> positionName
            return employees.stream()
                    .filter(e -> e.getId() != null && e.getPosition() != null && e.getPosition().getName() != null)
                    .collect(Collectors.toMap(
                            EmployeeWithPositionDTO::getId,
                            e -> e.getPosition().getName(),
                            (existing, replacement) -> existing // Nếu trùng key, giữ giá trị cũ
                    ));
        } catch (Exception ex) {
            // Log error và trả về map rỗng
            System.err.println("Error fetching position names by user ids: " + ex.getMessage());
            return new HashMap<>();
        }
    }

    @Getter
    @Setter
    private static class PositionDTO {
        private Long id;
        private String name;
        private String level;
        private Integer hierarchyOrder;
        private Boolean active;
    }

    @Getter
    @Setter
    private static class EmployeeDTO {
        private Long id;
        private String name;
    }

    /**
     * Tìm user dựa trên levelId (positionId) và departmentId
     * Gọi user-service để tìm employee có positionId và departmentId tương ứng
     * 
     * @param levelId      Position ID (level ID)
     * @param departmentId Department ID
     * @param token        JWT token để xác thực
     * @return User ID (employee ID) hoặc null nếu không tìm thấy
     */
    public Long findUserByPositionIdAndDepartmentId(Long positionId, Long departmentId, String token) {
        if (positionId == null || departmentId == null) {
            return null;
        }

        try {
            String url = UriComponentsBuilder.fromUriString(userServiceBaseUrl)
                    .path("/api/v1/user-service/employees")
                    .queryParam("positionId", positionId)
                    .queryParam("departmentId", departmentId)
                    .queryParam("status", "ACTIVE")
                    .queryParam("limit", 1)
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            if (token != null && !token.isEmpty()) {
                headers.setBearerAuth(token);
            }

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            // User-service trả về Response<PaginationDTO> với field data chứa danh sách
            // employees
            ResponseEntity<Response<PaginationDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<Response<PaginationDTO>>() {
                    });

            Response<PaginationDTO> responseBody = response.getBody();
            if (responseBody == null || responseBody.getData() == null) {
                return null;
            }

            PaginationDTO paginationDTO = responseBody.getData();
            Object resultObj = paginationDTO.getResult();

            if (resultObj == null) {
                return null;
            }

            // Parse result thành List
            List<Object> resultList = null;
            if (resultObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) resultObj;
                resultList = list;
            }

            if (resultList == null || resultList.isEmpty()) {
                return null;
            }

            // Lấy employee đầu tiên từ kết quả
            Object firstResult = resultList.get(0);
            if (firstResult instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> employeeMap = (Map<String, Object>) firstResult;
                Object idObj = employeeMap.get("id");
                if (idObj instanceof Number) {
                    return ((Number) idObj).longValue();
                }
            }

            return null;
        } catch (Exception ex) {
            // Log error và trả về null
            System.err.println("Error finding user by levelId and departmentId: " + ex.getMessage());
            return null;
        }
    }

    /**
     * Lấy thông tin department theo ID
     */
    public ResponseEntity<com.fasterxml.jackson.databind.JsonNode> getDepartmentById(Long departmentId, String token) {
        try {
            String url = userServiceBaseUrl + "/api/v1/user-service/departments/" + departmentId;
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
                return ResponseEntity.ok(response.getBody().getData());
            }
        } catch (Exception e) {
            // Log error
        }
        return ResponseEntity.notFound().build();
    }

    @Getter
    @Setter
    private static class EmployeeWithPositionDTO {
        private Long id;
        private String name;
        private PositionDTO position;
    }

}
