package com.example.statistics_service.service.client;

import com.example.statistics_service.dto.PaginationDTO;
import com.example.statistics_service.dto.Response;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JobServiceClient {
    private final RestTemplate restTemplate;

    @Value("${job-service.url:http://localhost:8083}")
    private String jobServiceBaseUrl;

    public PaginationDTO getJobPositions(String token, Long departmentId, int page, int limit) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(jobServiceBaseUrl)
                    .path("/api/v1/job-service/job-positions")
                    .queryParam("page", page)
                    .queryParam("limit", limit)
                    .queryParam("status", "PUBLISHED");

            if (departmentId != null) {
                builder.queryParam("departmentId", departmentId);
            }

            String url = builder.build().toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            if (token != null && !token.isEmpty()) {
                headers.setBearerAuth(token);
            }

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<Response<PaginationDTO>> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity,
                    new ParameterizedTypeReference<Response<PaginationDTO>>() {
                    });

            if (response.getBody() != null && response.getBody().getData() != null) {
                return response.getBody().getData();
            }
            return null;
        } catch (Exception ex) {
            System.err.println("Error fetching job positions: " + ex.getMessage());
            return null;
        }
    }
}
