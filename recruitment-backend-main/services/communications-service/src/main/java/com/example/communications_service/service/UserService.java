package com.example.communications_service.service;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.communications_service.dto.Response;
import com.example.communications_service.dto.PaginationDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class UserService {

    @Value("${user-service.url:http://localhost:8082}")
    private String userServiceUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public UserService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    public ResponseEntity<JsonNode> getEmployeeName(Long employeeId, String token) {
        try {
            String url = userServiceUrl + "/api/v1/user-service/employees/" + employeeId;

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            if (token != null && !token.isEmpty()) {
                headers.setBearerAuth(token);
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Response<Map<String, Object>>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<Response<Map<String, Object>>>() {
                    });

            if (response.getBody() != null && response.getBody().getData() != null) {
                Map<String, Object> data = response.getBody().getData();
                Object name = data.get("name");
                if (name != null) {
                    ObjectNode result = objectMapper.createObjectNode();
                    result.put("name", name.toString());
                    return ResponseEntity.ok(result);
                }
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            ObjectNode errorNode = objectMapper.createObjectNode();
            errorNode.put("statusCode", 500);
            errorNode.put("error", "Internal Server Error");
            errorNode.put("message", "Không thể kết nối tới User Service: " + e.getMessage());
            errorNode.putNull("data");
            return ResponseEntity.internalServerError().body(errorNode);
        }
    }

    public ResponseEntity<JsonNode> getEmployeeNames(List<Long> employeeIds, String token) {
        try {
            String url = userServiceUrl + "/api/v1/user-service/employees?ids="
                    + employeeIds.stream().map(String::valueOf).collect(Collectors.joining(","));

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            if (token != null && !token.isEmpty()) {
                headers.setBearerAuth(token);
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Response<List<Map<String, Object>>>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<Response<List<Map<String, Object>>>>() {
                    });

            ObjectNode idToName = objectMapper.createObjectNode();
            if (response.getBody() != null && response.getBody().getData() != null) {
                List<Map<String, Object>> employees = response.getBody().getData();
                for (Map<String, Object> employee : employees) {
                    Object id = employee.get("id");
                    Object name = employee.get("name");
                    if (id != null && name != null) {
                        idToName.put(String.valueOf(id), name.toString());
                    }
                }
            }
            return ResponseEntity.ok(idToName);
        } catch (Exception e) {
            ObjectNode errorNode = objectMapper.createObjectNode();
            errorNode.put("statusCode", 500);
            errorNode.put("error", "Internal Server Error");
            errorNode.put("message", "Không thể kết nối tới User Service: " + e.getMessage());
            errorNode.putNull("data");
            return ResponseEntity.internalServerError().body(errorNode);
        }
    }

    public ResponseEntity<JsonNode> getEmployeeNamesAndDepartmentNames(List<Long> employeeIds, String token) {
        try {
            String url = userServiceUrl + "/api/v1/user-service/employees?ids="
                    + employeeIds.stream().map(String::valueOf).collect(Collectors.joining(","));

            HttpHeaders headers = new HttpHeaders();
            if (token != null && !token.isEmpty()) {
                headers.set("Authorization", "Bearer " + token);
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Response<List<Map<String, Object>>>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<Response<List<Map<String, Object>>>>() {
                    });

            ObjectNode result = objectMapper.createObjectNode();
            if (response.getBody() != null && response.getBody().getData() != null) {
                List<Map<String, Object>> employees = response.getBody().getData();
                for (Map<String, Object> employee : employees) {
                    Object id = employee.get("id");
                    if (id != null) {
                        ObjectNode employeeInfo = objectMapper.createObjectNode();
                        Object name = employee.get("name");
                        employeeInfo.put("name", name != null ? name.toString() : "Unknown");

                        Object department = employee.get("department");
                        if (department instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> deptMap = (Map<String, Object>) department;
                            Object deptName = deptMap.get("name");
                            employeeInfo.put("departmentName", deptName != null ? deptName.toString() : "Unknown");
                        } else {
                            employeeInfo.put("departmentName", "Unknown");
                        }

                        result.set(String.valueOf(id), employeeInfo);
                    }
                }
            }
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            ObjectNode errorNode = objectMapper.createObjectNode();
            errorNode.put("statusCode", 500);
            errorNode.put("error", "Internal Server Error");
            errorNode.put("message", "Không thể kết nối tới User Service: " + e.getMessage());
            errorNode.putNull("data");
            return ResponseEntity.internalServerError().body(errorNode);
        }
    }

    /**
     * Lấy userId (employeeId) từ email
     * Gọi API user-service để tìm employee theo email
     */
    public Long getUserIdByEmail(String email) {
        try {
            String url = userServiceUrl + "/api/v1/user-service/users/email/" + email;

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Response<Map<String, Object>>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<Response<Map<String, Object>>>() {
                    });

            if (response.getBody() != null && response.getBody().getData() != null) {
                Map<String, Object> data = response.getBody().getData();
                Object employeeId = data.get("employeeId");
                if (employeeId instanceof Number) {
                    return ((Number) employeeId).longValue();
                }
                Object id = data.get("id");
                if (id instanceof Number) {
                    return ((Number) id).longValue();
                }
            }
            return null;
        } catch (Exception e) {
            // Nếu không tìm thấy hoặc lỗi, trả về null (có thể là email bên ngoài)
            return null;
        }
    }

    public List<Long> getEmployeeIdsByFilters(Long departmentId, Long positionId, String authToken) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(userServiceUrl + "/api/v1/user-service/employees")
                    .queryParam("page", 1)
                    .queryParam("limit", 1000);

            if (departmentId != null) {
                builder.queryParam("departmentId", departmentId);
            }
            if (positionId != null) {
                builder.queryParam("positionId", positionId);
            }

            String url = builder.toUriString();
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            if (authToken != null && !authToken.isEmpty()) {
                headers.setBearerAuth(authToken);
            }
            ResponseEntity<Response<PaginationDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<Response<PaginationDTO>>() {
                    });

            List<Long> result = new ArrayList<>();
            if (response.getBody() != null && response.getBody().getData() != null) {
                PaginationDTO pagination = response.getBody().getData();
                Object resultObj = pagination.getResult();
                if (resultObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> resultList = (List<Object>) resultObj;
                    for (Object item : resultList) {
                        if (item instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> employee = (Map<String, Object>) item;
                            Object id = employee.get("id");
                            if (id instanceof Number) {
                                result.add(((Number) id).longValue());
                            }
                        }
                    }
                }
            }
            return result;
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<Long> getAllEmployeeIds(String authToken) {
        return getEmployeeIdsByFilters(null, null, authToken);
    }

}
