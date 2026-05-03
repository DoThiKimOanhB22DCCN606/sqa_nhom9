package com.example.job_service.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.job_service.dto.Response;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class CandidateClient {

    private final RestTemplate restTemplate;

    @Value("${candidate-service.url:http://localhost:8081}")
    private String candidateServiceUrl;

    public CandidateClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Đếm số lượng ứng viên theo jobPositionId
     * Sử dụng endpoint /count để tránh vòng lặp (không gọi lại job-service)
     * 
     * @param jobPositionId ID của vị trí tuyển dụng
     * @param token         JWT token (có thể null cho public API)
     * @return Số lượng ứng viên
     */
    public Integer countCandidatesByJobPositionId(Long jobPositionId, String token) {
        try {
            String url = candidateServiceUrl + "/api/v1/candidate-service/candidates/count?jobPositionId="
                    + jobPositionId;

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            if (token != null && !token.isEmpty()) {
                headers.setBearerAuth(token);
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Response<Long>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<Response<Long>>() {
                    });

            if (response.getBody() != null && response.getBody().getData() != null) {
                return response.getBody().getData().intValue();
            }
            return 0;
        } catch (Exception e) {
            // Nếu lỗi, trả về 0
            return 0;
        }
    }

    /**
     * Lấy số lượng ứng viên cho nhiều jobPositionIds cùng lúc
     * 
     * @param jobPositionIds Danh sách ID vị trí tuyển dụng
     * @param token          JWT token
     * @return Map với key là jobPositionId và value là số lượng ứng viên
     */
    public Map<Long, Integer> countCandidatesByJobPositionIds(List<Long> jobPositionIds, String token) {
        Map<Long, Integer> counts = new HashMap<>();

        if (jobPositionIds == null || jobPositionIds.isEmpty()) {
            return counts;
        }

        // Gọi song song cho từng jobPositionId
        for (Long jobPositionId : jobPositionIds) {
            try {
                Integer count = countCandidatesByJobPositionId(jobPositionId, token);
                counts.put(jobPositionId, count);
            } catch (Exception e) {
                // Nếu lỗi, set về 0
                counts.put(jobPositionId, 0);
            }
        }

        return counts;
    }

    /**
     * Lấy thông tin chi tiết ứng viên theo ID
     * 
     * @param candidateId ID của ứng viên
     * @param token       JWT token
     * @return Thông tin ứng viên dưới dạng JsonNode
     */
    public ResponseEntity<JsonNode> getCandidateById(Long candidateId, String token) {
        try {
            String url = candidateServiceUrl + "/api/v1/candidate-service/candidates/" + candidateId;

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            if (token != null && !token.isEmpty()) {
                headers.setBearerAuth(token);
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Response<JsonNode>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<Response<JsonNode>>() {
                    });

            if (response.getBody() != null && response.getBody().getData() != null) {
                return ResponseEntity.ok(response.getBody().getData());
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode errorNode = objectMapper.createObjectNode();
            errorNode.put("statusCode", 500);
            errorNode.put("error", "Internal Server Error");
            errorNode.put("message", "Không thể kết nối tới Candidate Service: " + e.getMessage());
            errorNode.putNull("data");
            return ResponseEntity.internalServerError().body(errorNode);
        }
    }
}
