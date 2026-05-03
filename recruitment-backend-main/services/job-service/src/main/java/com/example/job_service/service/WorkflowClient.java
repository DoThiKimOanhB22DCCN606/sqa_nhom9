package com.example.job_service.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.example.job_service.dto.Response;
import com.fasterxml.jackson.databind.JsonNode;

@Component
public class WorkflowClient {

    private static final Logger log = LoggerFactory.getLogger(WorkflowClient.class);

    private final RestTemplate restTemplate;
    private final String workflowServiceBaseUrl;

    public WorkflowClient(
            RestTemplate restTemplate,
            @Value("${workflow-service.url:http://localhost:8086}") String workflowServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.workflowServiceBaseUrl = workflowServiceBaseUrl;
    }

    /**
     * Gọi sang workflow-service để tìm workflow phù hợp với departmentId/levelId.
     * Trả về workflowId hoặc null nếu không tìm thấy.
     */
    public Long findMatchingWorkflow(Long departmentId, Long levelId) {
        try {
            String url = String.format("%s/api/v1/workflow-service/workflows/match?departmentId=%d&levelId=%d",
                    workflowServiceBaseUrl, departmentId, levelId != null ? levelId : 0L);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Response<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<Response<Map<String, Object>>>() {
                    });

            if (response.getBody() != null && response.getBody().getData() != null) {
                Map<String, Object> data = response.getBody().getData();
                Object id = data.get("id");
                if (id instanceof Number number) {
                    return number.longValue();
                }
            }
        } catch (Exception ex) {
            log.error("Cannot call workflow-service match API", ex);
        }
        return null;
    }

    /**
     * Tìm workflow type OFFER phù hợp với departmentId
     * Trả về workflowId hoặc null nếu không tìm thấy
     */
    public Long findMatchingOfferWorkflow(Long departmentId) {
        try {
            String url = String.format(
                    "%s/api/v1/workflow-service/workflows?type=OFFER&departmentId=%d&isActive=true&page=1&limit=1",
                    workflowServiceBaseUrl, departmentId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Response<com.example.job_service.dto.PaginationDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<Response<com.example.job_service.dto.PaginationDTO>>() {
                    });

            if (response.getBody() != null && response.getBody().getData() != null) {
                com.example.job_service.dto.PaginationDTO pagination = response.getBody().getData();
                Object result = pagination.getResult();
                if (result instanceof java.util.List && !((java.util.List<?>) result).isEmpty()) {
                    Object firstItem = ((java.util.List<?>) result).get(0);
                    if (firstItem instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> workflow = (Map<String, Object>) firstItem;
                        Object id = workflow.get("id");
                        if (id instanceof Number number) {
                            return number.longValue();
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Cannot call workflow-service to find OFFER workflow", ex);
        }
        return null;
    }

    /**
     * Lấy thông tin workflow và approval tracking theo requestId
     */
    public JsonNode getWorkflowInfoByRequestId(Long requestId, Long workflowId,
            String token) {
        return getWorkflowInfoByRequestId(requestId, workflowId, null, token);
    }

    /**
     * Lấy thông tin workflow và approval tracking theo requestId với requestType
     */
    public JsonNode getWorkflowInfoByRequestId(Long requestId, Long workflowId, String requestType,
            String token) {
        try {
            String url = String.format("%s/api/v1/workflow-service/approval-trackings/by-request/%d",
                    workflowServiceBaseUrl, requestId);
            StringBuilder queryParams = new StringBuilder();
            if (workflowId != null) {
                queryParams.append("workflowId=").append(workflowId);
            }
            if (requestType != null && !requestType.trim().isEmpty()) {
                if (queryParams.length() > 0) {
                    queryParams.append("&");
                }
                queryParams.append("requestType=").append(requestType);
            }
            if (queryParams.length() > 0) {
                url += "?" + queryParams.toString();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (token != null && !token.isEmpty()) {
                headers.set("Authorization", "Bearer " + token);
            }

            ResponseEntity<Response<com.fasterxml.jackson.databind.JsonNode>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<Response<com.fasterxml.jackson.databind.JsonNode>>() {
                    });

            if (response.getBody() != null && response.getBody().getData() != null) {
                return response.getBody().getData();
            }
        } catch (Exception ex) {
            log.error("Cannot call workflow-service to get workflow info for request {}", requestId, ex);
        }
        return null;
    }
}
