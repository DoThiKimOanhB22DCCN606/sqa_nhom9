package com.example.workflow_service.service;

import com.example.workflow_service.dto.Response;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CandidateService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${services.candidate-service.url:http://localhost:8084}")
    private String candidateServiceBaseUrl;

    /**
     * Lấy departmentId từ candidate bằng cách gọi API chuyên biệt ở
     * candidate-service:
     * GET /candidates/{id}/department
     * 
     * @param candidateId ID của candidate
     * @param token       JWT token để xác thực
     * @return Department ID hoặc null nếu không tìm thấy
     */
    public Long getDepartmentIdFromCandidate(Long candidateId, String token) {
        if (candidateId == null) {
            return null;
        }
        try {
            String url = candidateServiceBaseUrl + "/api/v1/candidate-service/candidates/" + candidateId
                    + "/department";
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
                return response.getBody().getData();
            }
        } catch (Exception e) {
            System.err.println("Error fetching departmentId from candidate-service for candidate " + candidateId + ": "
                    + e.getMessage());
        }
        return null;
    }

}
