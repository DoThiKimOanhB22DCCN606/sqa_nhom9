package com.example.candidate_service.service;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.candidate_service.dto.Response;
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

    /**
     * Tạo Employee từ Candidate bằng cách gọi user-service
     */
    public ResponseEntity<JsonNode> createEmployeeFromCandidate(
            Long candidateId,
            String name,
            String email,
            String phone,
            String dateOfBirth,
            String gender,
            String nationality,
            String idNumber,
            String address,
            String avatarUrl,
            Long departmentId,
            Long positionId,
            String status,
            String token) {
        try {
            String url = userServiceUrl + "/api/v1/user-service/employees/from-candidate";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            if (token != null && !token.isEmpty()) {
                headers.setBearerAuth(token);
            }

            // Tạo request body
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("candidateId", candidateId);
            requestBody.put("name", name);
            requestBody.put("email", email);
            requestBody.put("phone", phone);
            if (dateOfBirth != null) {
                requestBody.put("dateOfBirth", dateOfBirth);
            }
            if (gender != null) {
                requestBody.put("gender", gender);
            }
            if (nationality != null) {
                requestBody.put("nationality", nationality);
            }
            if (idNumber != null) {
                requestBody.put("idNumber", idNumber);
            }
            if (address != null) {
                requestBody.put("address", address);
            }
            if (avatarUrl != null) {
                requestBody.put("avatarUrl", avatarUrl);
            }
            requestBody.put("departmentId", departmentId);
            requestBody.put("positionId", positionId);
            if (status != null) {
                requestBody.put("status", status);
            }

            HttpEntity<ObjectNode> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Response<JsonNode>> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity,
                    new ParameterizedTypeReference<Response<JsonNode>>() {
                    });

            if (response.getBody() != null && response.getBody().getData() != null) {
                return ResponseEntity.ok(response.getBody().getData());
            }
            return ResponseEntity.status(response.getStatusCode()).build();
        } catch (Exception e) {
            ObjectNode errorNode = objectMapper.createObjectNode();
            errorNode.put("statusCode", 500);
            errorNode.put("error", "Internal Server Error");
            errorNode.put("message", "Không thể kết nối tới User Service: " + e.getMessage());
            errorNode.putNull("data");
            return ResponseEntity.internalServerError().body(errorNode);
        }
    }

}
