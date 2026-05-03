package com.example.schedule_service.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class CandidateService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    @Value("${candidate-service.url:http://localhost:8084}")
    private String candidateServiceUrl;

    public CandidateService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    public ResponseEntity<JsonNode> getCandidateName(Long candidateId, String token) {
        try {
            String url = candidateServiceUrl + "/api/v1/candidate-service/candidates/" + candidateId;

            HttpHeaders headers = new HttpHeaders();
            if (token != null && !token.isEmpty()) {
                headers.set("Authorization", "Bearer " + token);
            }

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.get("data");
            return ResponseEntity.ok(data.get("name"));

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            try {
                JsonNode errorBody = objectMapper.readTree(ex.getResponseBodyAsString());
                return ResponseEntity.status(ex.getStatusCode()).body(errorBody);
            } catch (Exception parseEx) {
                ObjectNode fallback = objectMapper.createObjectNode();
                fallback.put("statusCode", ex.getStatusCode().value());
                fallback.put("error", ex.getStatusText());
                fallback.put("message", "Không thể parse phản hồi lỗi từ User Service");
                fallback.putNull("data");
                return ResponseEntity.status(ex.getStatusCode()).body(fallback);
            }
        } catch (Exception e) {
            ObjectNode errorNode = objectMapper.createObjectNode();
            errorNode.put("statusCode", 500);
            errorNode.put("error", "Internal Server Error");
            errorNode.put("message", "Không thể kết nối tới User Service: " + e.getMessage());
            errorNode.putNull("data");
            return ResponseEntity.internalServerError().body(errorNode);
        }
    }

    public ResponseEntity<JsonNode> getCandidateNames(List<Long> candidateIds, String token) {
        try {
            String url = candidateServiceUrl + "/api/v1/candidate-service/candidates?ids="
                    + candidateIds.stream().map(String::valueOf).collect(Collectors.joining(","));

            HttpHeaders headers = new HttpHeaders();
            if (token != null && !token.isEmpty()) {
                headers.set("Authorization", "Bearer " + token);
            }

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.get("data"); // expected array of candidates
            ObjectNode idToName = objectMapper.createObjectNode();
            if (data != null && data.isArray()) {
                for (JsonNode cand : data) {
                    if (cand.has("id") && cand.has("name")) {
                        idToName.put(String.valueOf(cand.get("id").asLong()), cand.get("name").asText());
                    }
                }
            }
            return ResponseEntity.ok(idToName);

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            try {
                JsonNode errorBody = objectMapper.readTree(ex.getResponseBodyAsString());
                return ResponseEntity.status(ex.getStatusCode()).body(errorBody);
            } catch (Exception parseEx) {
                ObjectNode fallback = objectMapper.createObjectNode();
                fallback.put("statusCode", ex.getStatusCode().value());
                fallback.put("error", ex.getStatusText());
                fallback.put("message", "Không thể parse phản hồi lỗi từ User Service");
                fallback.putNull("data");
                return ResponseEntity.status(ex.getStatusCode()).body(fallback);
            }
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
     * Tìm candidate theo email để xác định nhân viên phụ trách
     * 
     * @param email Email của candidate
     * @return employeeId (nhân viên phụ trách) hoặc null nếu không tìm thấy
     */
    public Long getEmployeeIdFromCandidateEmail(String email) {
        try {
            // Tìm candidate bằng keyword search (email)
            String candidateUrl = candidateServiceUrl + "/api/v1/candidate-service/candidates?keyword="
                    + java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8)
                    + "&page=1&limit=1";
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> candidateResponse = restTemplate.exchange(
                    candidateUrl, HttpMethod.GET, entity, String.class);

            JsonNode candidateRoot = objectMapper.readTree(candidateResponse.getBody());
            JsonNode candidateData = candidateRoot.path("data").path("result");

            if (candidateData == null || !candidateData.isArray() || candidateData.size() == 0) {
                return null;
            }

            // Tìm candidate có email khớp chính xác
            Long candidateId = null;
            for (JsonNode candidate : candidateData) {
                if (candidate.has("email") && email.equalsIgnoreCase(candidate.get("email").asText())) {
                    candidateId = candidate.get("id").asLong();
                    break;
                }
            }

            if (candidateId == null) {
                return null;
            }

            // Lấy candidate này (đã gộp application vào candidate)
            String candidateDetailUrl = candidateServiceUrl + "/api/v1/candidate-service/candidates?candidateId="
                    + candidateId + "&page=1&limit=1&sortBy=id&sortOrder=desc";

            ResponseEntity<String> candidateDetailResponse = restTemplate.exchange(
                    candidateDetailUrl, HttpMethod.GET, entity, String.class);

            JsonNode candidateDetailRoot = objectMapper.readTree(candidateDetailResponse.getBody());
            JsonNode candidateDetailData = candidateDetailRoot.path("data").path("result");

            if (candidateDetailData != null && candidateDetailData.isArray() && candidateDetailData.size() > 0) {
                JsonNode firstCandidate = candidateDetailData.get(0);
                // Ưu tiên updatedBy (người cập nhật gần nhất), nếu không có thì dùng createdBy
                if (firstCandidate.has("updatedBy") && !firstCandidate.get("updatedBy").isNull()) {
                    return firstCandidate.get("updatedBy").asLong();
                }
                if (firstCandidate.has("createdBy") && !firstCandidate.get("createdBy").isNull()) {
                    return firstCandidate.get("createdBy").asLong();
                }
            }

            return null;
        } catch (Exception e) {
            // Nếu không tìm thấy hoặc lỗi, trả về null
            return null;
        }
    }
}
