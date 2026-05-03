package com.example.statistics_service.service.client;

import com.example.statistics_service.dto.PaginationDTO;
import com.example.statistics_service.dto.Response;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CandidateServiceClient {
    private final RestTemplate restTemplate;

    @Value("${candidate-service.url:http://localhost:8084}")
    private String candidateServiceBaseUrl;

    /**
     * Lấy dữ liệu candidates cho thống kê - API tối ưu chỉ trả về dữ liệu cần
     * thiết
     */
    public List<JsonNode> getApplicationsForStatistics(String token, String status, String startDate,
            String endDate, Long jobPositionId, Long departmentId) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(candidateServiceBaseUrl)
                    .path("/api/v1/candidate-service/candidates/statistics");

            if (status != null) {
                builder.queryParam("status", status);
            }
            if (startDate != null) {
                builder.queryParam("startDate", startDate);
            }
            if (endDate != null) {
                builder.queryParam("endDate", endDate);
            }
            if (jobPositionId != null) {
                builder.queryParam("jobPositionId", jobPositionId);
            }
            if (departmentId != null) {
                builder.queryParam("departmentId", departmentId);
            }

            String url = builder.build().toUriString();
            System.out.println("url: " + url);
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            if (token != null && !token.isEmpty()) {
                headers.setBearerAuth(token);
            }

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<Response<List<JsonNode>>> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity,
                    new ParameterizedTypeReference<Response<List<JsonNode>>>() {
                    });

            if (response.getBody() != null && response.getBody().getData() != null) {
                System.out.println("response.getBody().getData(): " + response.getBody().getData());
                return response.getBody().getData();
            }

            return List.of();
        } catch (Exception ex) {
            System.err.println("Error fetching candidates for statistics: " + ex.getMessage());
            ex.printStackTrace();
            return List.of();
        }
    }
}
