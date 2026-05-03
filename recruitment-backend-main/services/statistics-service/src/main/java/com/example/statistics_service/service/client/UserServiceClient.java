package com.example.statistics_service.service.client;

import com.example.statistics_service.dto.PaginationDTO;
import com.example.statistics_service.dto.Response;
import com.fasterxml.jackson.databind.JsonNode;
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
public class UserServiceClient {
    private final RestTemplate restTemplate;

    @Value("${user-service.url:http://localhost:8082}")
    private String userServiceBaseUrl;

    public PaginationDTO getUsers(String token, String role, Boolean isActive, int page, int limit) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(userServiceBaseUrl)
                    .path("/api/v1/user-service/users")
                    .queryParam("page", page)
                    .queryParam("limit", limit);
            
            if (role != null) {
                builder.queryParam("role", role);
            }
            if (isActive != null) {
                builder.queryParam("isActive", isActive);
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
                    new ParameterizedTypeReference<Response<PaginationDTO>>() {});

            if (response.getBody() != null && response.getBody().getData() != null) {
                return response.getBody().getData();
            }
            return null;
        } catch (Exception ex) {
            System.err.println("Error fetching users: " + ex.getMessage());
            return null;
        }
    }

    public PaginationDTO getDepartments(String token, int page, int limit) {
        try {
            String url = UriComponentsBuilder.fromUriString(userServiceBaseUrl)
                    .path("/api/v1/user-service/departments")
                    .queryParam("page", page)
                    .queryParam("limit", limit)
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            if (token != null && !token.isEmpty()) {
                headers.setBearerAuth(token);
            }

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<Response<PaginationDTO>> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity,
                    new ParameterizedTypeReference<Response<PaginationDTO>>() {});

            if (response.getBody() != null && response.getBody().getData() != null) {
                return response.getBody().getData();
            }
            return null;
        } catch (Exception ex) {
            System.err.println("Error fetching departments: " + ex.getMessage());
            return null;
        }
    }

    public JsonNode getDepartmentById(String token, Long departmentId) {
        try {
            String url = UriComponentsBuilder.fromUriString(userServiceBaseUrl)
                    .path("/api/v1/user-service/departments/{id}")
                    .buildAndExpand(departmentId)
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            if (token != null && !token.isEmpty()) {
                headers.setBearerAuth(token);
            }

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<Response<JsonNode>> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity,
                    new ParameterizedTypeReference<Response<JsonNode>>() {});

            if (response.getBody() != null && response.getBody().getData() != null) {
                return response.getBody().getData();
            }
            return null;
        } catch (Exception ex) {
            System.err.println("Error fetching department: " + ex.getMessage());
            return null;
        }
    }

    public PaginationDTO getEmployees(String token, Long departmentId, int page, int limit) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(userServiceBaseUrl)
                    .path("/api/v1/user-service/employees")
                    .queryParam("page", page)
                    .queryParam("limit", limit);

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
                    new ParameterizedTypeReference<Response<PaginationDTO>>() {});

            if (response.getBody() != null && response.getBody().getData() != null) {
                return response.getBody().getData();
            }
            return null;
        } catch (Exception ex) {
            System.err.println("Error fetching employees: " + ex.getMessage());
            return null;
        }
    }
}

