package com.example.candidate_service.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.candidate_service.dto.Response;
import com.example.candidate_service.dto.PaginationDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class JobService {
    @Value("${job-service.url:http://localhost:8083}")
    private String jobServiceUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public JobService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public ResponseEntity<JsonNode> getJobPositionById(Long id, String token) {
        try {
            String url = jobServiceUrl + "/api/v1/job-service/job-positions/" + id;
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
            ObjectNode errorNode = objectMapper.createObjectNode();
            errorNode.put("statusCode", 500);
            errorNode.put("error", "Internal Server Error");
            errorNode.put("message", "Không thể kết nối tới Job Service: " + e.getMessage());
            errorNode.putNull("data");
            return ResponseEntity.internalServerError().body(errorNode);
        }
    }

    public List<Long> getJobPositionIdsByDepartmentId(Long departmentId, String token) {
        List<Long> jobPositionIds = new ArrayList<>();
        try {
            String baseUrl = jobServiceUrl + "/api/v1/job-service/job-positions";
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            if (token != null && !token.isEmpty()) {
                headers.setBearerAuth(token);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Fetch first page with departmentId filter
            String firstPageUrl = baseUrl + "?departmentId=" + departmentId + "&page=1&limit=100";
            ResponseEntity<Response<PaginationDTO>> firstResponse = restTemplate.exchange(
                    firstPageUrl, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<Response<PaginationDTO>>() {
                    });

            if (firstResponse.getBody() != null && firstResponse.getBody().getData() != null) {
                PaginationDTO pagination = firstResponse.getBody().getData();
                Object resultObj = pagination.getResult();
                if (resultObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> resultList = (List<Object>) resultObj;
                    for (Object item : resultList) {
                        if (item instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> jobPosition = (Map<String, Object>) item;
                            Object id = jobPosition.get("id");
                            if (id instanceof Number) {
                                jobPositionIds.add(((Number) id).longValue());
                            }
                        }
                    }
                }

                // Get total pages from meta and fetch remaining pages
                if (pagination.getMeta() != null) {
                    int totalPages = pagination.getMeta().getPages();

                    // Fetch remaining pages if any
                    for (int page = 2; page <= totalPages; page++) {
                        try {
                            String pageUrl = baseUrl + "?departmentId=" + departmentId + "&page=" + page + "&limit=100";
                            ResponseEntity<Response<PaginationDTO>> pageResponse = restTemplate.exchange(
                                    pageUrl, HttpMethod.GET, entity,
                                    new ParameterizedTypeReference<Response<PaginationDTO>>() {
                                    });
                            if (pageResponse.getBody() != null && pageResponse.getBody().getData() != null) {
                                PaginationDTO pagePagination = pageResponse.getBody().getData();
                                Object pageResultObj = pagePagination.getResult();
                                if (pageResultObj instanceof List) {
                                    @SuppressWarnings("unchecked")
                                    List<Object> pageResultList = (List<Object>) pageResultObj;
                                    for (Object item : pageResultList) {
                                        if (item instanceof Map) {
                                            @SuppressWarnings("unchecked")
                                            Map<String, Object> jobPosition = (Map<String, Object>) item;
                                            Object id = jobPosition.get("id");
                                            if (id instanceof Number) {
                                                jobPositionIds.add(((Number) id).longValue());
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Log error but continue with other pages
                            System.err.println("Error fetching page " + page + ": " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Log error and return empty list
            System.err.println("Error fetching job positions by department ID: " + e.getMessage());
        }
        return jobPositionIds;
    }

    /**
     * Lấy job positions theo departmentId và trả về đầy đủ thông tin (Map với
     * JsonNode)
     * Sử dụng API /simple để không gọi user service
     */
    public Map<Long, JsonNode> getJobPositionsByDepartmentId(Long departmentId, String token) {
        Map<Long, JsonNode> jobPositionMap = new HashMap<>();
        try {
            // Gọi API /simple với departmentId filter - API này trả về PaginationDTO
            String url = jobServiceUrl + "/api/v1/job-service/job-positions/simple?departmentId=" + departmentId
                    + "&limit=1000";
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            if (token != null && !token.isEmpty()) {
                headers.setBearerAuth(token);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Response<PaginationDTO>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<Response<PaginationDTO>>() {
                    });

            if (response.getBody() != null && response.getBody().getData() != null) {
                PaginationDTO pagination = response.getBody().getData();
                Object resultObj = pagination.getResult();
                if (resultObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> resultList = (List<Object>) resultObj;
                    for (Object item : resultList) {
                        JsonNode jpNode = objectMapper.valueToTree(item);
                        if (jpNode != null && jpNode.has("id")) {
                            Long jpId = jpNode.get("id").asLong();
                            jobPositionMap.put(jpId, jpNode);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Log error and return empty map
            System.err.println("Error fetching job positions by department ID: " + e.getMessage());
        }
        return jobPositionMap;
    }

    /**
     * Lấy nhiều job positions theo IDs một lần - sử dụng API /simple (không gọi
     * user service)
     */
    public Map<Long, JsonNode> getJobPositionsByIdsSimple(List<Long> ids, String token) {
        Map<Long, JsonNode> jobPositionMap = new HashMap<>();

        if (ids == null || ids.isEmpty()) {
            return jobPositionMap;
        }

        try {
            // Tạo query string với các IDs
            String idsParam = ids.stream()
                    .map(String::valueOf)
                    .collect(java.util.stream.Collectors.joining(","));

            // Gọi API /simple để lấy JobPosition entities (không có departmentName, không
            // gọi user service) - API này trả về PaginationDTO
            String url = jobServiceUrl + "/api/v1/job-service/job-positions/simple?ids=" + idsParam + "&limit=1000";

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            if (token != null && !token.isEmpty()) {
                headers.setBearerAuth(token);
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Response<PaginationDTO>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<Response<PaginationDTO>>() {
                    });

            if (response.getBody() != null && response.getBody().getData() != null) {
                PaginationDTO pagination = response.getBody().getData();
                Object resultObj = pagination.getResult();
                if (resultObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> resultList = (List<Object>) resultObj;
                    for (Object item : resultList) {
                        JsonNode jpNode = objectMapper.valueToTree(item);
                        if (jpNode != null && jpNode.has("id")) {
                            Long jpId = jpNode.get("id").asLong();
                            jobPositionMap.put(jpId, jpNode);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching job positions by IDs: " + e.getMessage());
        }

        return jobPositionMap;
    }

    /**
     * Lấy nhiều job positions theo IDs một lần với thông tin department (deprecated
     * - dùng getJobPositionsByIdsSimple)
     */
    public Map<Long, JsonNode> getJobPositionsByIds(List<Long> ids, String token) {
        return getJobPositionsByIdsSimple(ids, token);
    }

    public ResponseEntity<JsonNode> getJobPositionByIdSimple(Long id, String token) {
        try {
            String url = jobServiceUrl + "/api/v1/job-service/job-positions/simple/" + id;
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            if (token != null && !token.isEmpty()) {
                headers.setBearerAuth(token);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Gọi API simple mới, response sẽ được wrap trong Response<JobPosition>
            // Parse response thành JsonNode
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, JsonNode.class);

            if (response.getBody() != null) {
                // Response có format: {"statusCode": 200, "data": {...}, ...}
                JsonNode responseBody = response.getBody();
                if (responseBody.has("data") && !responseBody.get("data").isNull()) {
                    return ResponseEntity.ok(responseBody.get("data"));
                }
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            ObjectNode errorNode = objectMapper.createObjectNode();
            errorNode.put("statusCode", 500);
            errorNode.put("error", "Internal Server Error");
            errorNode.put("message", "Không thể kết nối tới Job Service: " + e.getMessage());
            errorNode.putNull("data");
            return ResponseEntity.internalServerError().body(errorNode);
        }
    }

}
