package com.example.job_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.example.job_service.dto.PaginationDTO;
import com.example.job_service.dto.Response;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class WorkflowClientTest {

    @Mock
    private RestTemplate restTemplate;

    private WorkflowClient workflowClient;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        // Khởi tạo trực tiếp thay vì @InjectMocks để dễ dàng truyền baseUrl
        workflowClient = new WorkflowClient(restTemplate, "http://localhost:8086");
    }

    // ==================== findMatchingWorkflow ====================

    @Test
    void findMatchingWorkflow_Success_WithLevelId() {
        Response<Map<String, Object>> mockResponseObj = new Response<>();
        mockResponseObj.setData(Map.of("id", 100L));
        ResponseEntity<Response<Map<String, Object>>> responseEntity = new ResponseEntity<>(mockResponseObj, HttpStatus.OK);

        when(restTemplate.exchange(
                eq("http://localhost:8086/api/v1/workflow-service/workflows/match?departmentId=1&levelId=2"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        Long result = workflowClient.findMatchingWorkflow(1L, 2L);
        assertEquals(100L, result);
    }

    @Test
    void findMatchingWorkflow_Success_NullLevelId() {
        // levelId = null sẽ được chuyển thành 0L
        Response<Map<String, Object>> mockResponseObj = new Response<>();
        mockResponseObj.setData(Map.of("id", 50L));
        ResponseEntity<Response<Map<String, Object>>> responseEntity = new ResponseEntity<>(mockResponseObj, HttpStatus.OK);

        when(restTemplate.exchange(
                eq("http://localhost:8086/api/v1/workflow-service/workflows/match?departmentId=1&levelId=0"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        Long result = workflowClient.findMatchingWorkflow(1L, null);
        assertEquals(50L, result);
    }

    @Test
    void findMatchingWorkflow_DataNull_Or_IdNotNumber() {
        // Test 1: Data null
        Response<Map<String, Object>> mockResponseObj1 = new Response<>();
        ResponseEntity<Response<Map<String, Object>>> responseEntity1 = new ResponseEntity<>(mockResponseObj1, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity1);

        assertNull(workflowClient.findMatchingWorkflow(1L, 2L));

        // Test 2: ID không phải Number (VD: String)
        Response<Map<String, Object>> mockResponseObj2 = new Response<>();
        mockResponseObj2.setData(Map.of("id", "100")); // String
        ResponseEntity<Response<Map<String, Object>>> responseEntity2 = new ResponseEntity<>(mockResponseObj2, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity2);

        assertNull(workflowClient.findMatchingWorkflow(1L, 2L));
    }

    @Test
    void findMatchingWorkflow_ExceptionCaught() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenThrow(new RestClientException("API Error"));

        assertNull(workflowClient.findMatchingWorkflow(1L, 2L));
    }

    // ==================== findMatchingOfferWorkflow ====================

    @Test
    void findMatchingOfferWorkflow_Success() {
        Response<PaginationDTO> mockResponseObj = new Response<>();
        PaginationDTO paginationDTO = new PaginationDTO();
        paginationDTO.setResult(List.of(Map.of("id", 99L)));
        mockResponseObj.setData(paginationDTO);

        ResponseEntity<Response<PaginationDTO>> responseEntity = new ResponseEntity<>(mockResponseObj, HttpStatus.OK);

        when(restTemplate.exchange(
                eq("http://localhost:8086/api/v1/workflow-service/workflows?type=OFFER&departmentId=1&isActive=true&page=1&limit=1"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        Long result = workflowClient.findMatchingOfferWorkflow(1L);
        assertEquals(99L, result);
    }

    @Test
    void findMatchingOfferWorkflow_Branches() {
        Response<PaginationDTO> mockResponseObj = new Response<>();
        PaginationDTO paginationDTO = new PaginationDTO();
        mockResponseObj.setData(paginationDTO);

        // Branch 1: result is not a List
        paginationDTO.setResult("Not A List");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(mockResponseObj, HttpStatus.OK));
        assertNull(workflowClient.findMatchingOfferWorkflow(1L));

        // Branch 2: result is empty List
        paginationDTO.setResult(Collections.emptyList());
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(mockResponseObj, HttpStatus.OK));
        assertNull(workflowClient.findMatchingOfferWorkflow(1L));

        // Branch 3: firstItem is not a Map
        paginationDTO.setResult(List.of("Just A String"));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(mockResponseObj, HttpStatus.OK));
        assertNull(workflowClient.findMatchingOfferWorkflow(1L));

        // Branch 4: Map does not contain Number ID
        paginationDTO.setResult(List.of(Map.of("id", "Not a number")));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(mockResponseObj, HttpStatus.OK));
        assertNull(workflowClient.findMatchingOfferWorkflow(1L));
    }

    @Test
    void findMatchingOfferWorkflow_ExceptionCaught() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("Crash"));

        assertNull(workflowClient.findMatchingOfferWorkflow(1L));
    }

    // ==================== getWorkflowInfoByRequestId ====================

    @Test
    void getWorkflowInfoByRequestId_Delegation() {
        // Test hàm 3 tham số sẽ gọi hàm 4 tham số với requestType = null
        Response<JsonNode> mockResponseObj = new Response<>();
        mockResponseObj.setData(objectMapper.createObjectNode().put("success", true));
        ResponseEntity<Response<JsonNode>> responseEntity = new ResponseEntity<>(mockResponseObj, HttpStatus.OK);

        when(restTemplate.exchange(
                eq("http://localhost:8086/api/v1/workflow-service/approval-trackings/by-request/1?workflowId=2"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        JsonNode result = workflowClient.getWorkflowInfoByRequestId(1L, 2L, "token");
        assertNotNull(result);
        assertTrue(result.get("success").asBoolean());
    }

    @Test
    void getWorkflowInfoByRequestId_UrlBuilder_Branches() {
        Response<JsonNode> mockResponseObj = new Response<>();
        mockResponseObj.setData(objectMapper.createObjectNode());
        ResponseEntity<Response<JsonNode>> responseEntity = new ResponseEntity<>(mockResponseObj, HttpStatus.OK);

        // Branch 1: Không có workflowId, Không có requestType
        when(restTemplate.exchange(eq("http://localhost:8086/api/v1/workflow-service/approval-trackings/by-request/1"),
                eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);
        assertNotNull(workflowClient.getWorkflowInfoByRequestId(1L, null, null, "token"));

        // Branch 2: Có workflowId, requestType rỗng hoặc whitespace
        when(restTemplate.exchange(eq("http://localhost:8086/api/v1/workflow-service/approval-trackings/by-request/1?workflowId=2"),
                eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);
        assertNotNull(workflowClient.getWorkflowInfoByRequestId(1L, 2L, "   ", "token"));

        // Branch 3: Có requestType, Không có workflowId
        when(restTemplate.exchange(eq("http://localhost:8086/api/v1/workflow-service/approval-trackings/by-request/1?requestType=OFFER"),
                eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);
        assertNotNull(workflowClient.getWorkflowInfoByRequestId(1L, null, "OFFER", "token"));

        // Branch 4: Có cả workflowId và requestType
        when(restTemplate.exchange(eq("http://localhost:8086/api/v1/workflow-service/approval-trackings/by-request/1?workflowId=2&requestType=OFFER"),
                eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);
        assertNotNull(workflowClient.getWorkflowInfoByRequestId(1L, 2L, "OFFER", "token"));
    }

    @Test
    void getWorkflowInfoByRequestId_TokenBranches_And_DataNull() {
        // Token null hoặc rỗng
        Response<JsonNode> mockResponseObj = new Response<>();
        ResponseEntity<Response<JsonNode>> responseEntity = new ResponseEntity<>(mockResponseObj, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        assertNull(workflowClient.getWorkflowInfoByRequestId(1L, null, "OFFER", null)); // Null token
        assertNull(workflowClient.getWorkflowInfoByRequestId(1L, null, "OFFER", ""));   // Empty token
    }

    @Test
    void getWorkflowInfoByRequestId_ExceptionCaught() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenThrow(new RestClientException("Connection Refused"));

        assertNull(workflowClient.getWorkflowInfoByRequestId(1L, null, null, "token"));
    }
    // ==================== BỔ SUNG EDGE CASES (ĐẨY LÊN 100% BRANCHES) ====================

    @Test
    void findMatchingWorkflow_ResponseBodyNull() {
        // Test nhánh response.getBody() == null để thỏa mãn điều kiện && đầu tiên
        ResponseEntity<Response<Map<String, Object>>> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        assertNull(workflowClient.findMatchingWorkflow(1L, 2L));
    }

    @Test
    void findMatchingOfferWorkflow_ResponseBodyNull() {
        // Test nhánh response.getBody() == null cho hàm OFFER
        ResponseEntity<Response<PaginationDTO>> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        assertNull(workflowClient.findMatchingOfferWorkflow(1L));
    }

    @Test
    void getWorkflowInfoByRequestId_ResponseBodyNull() {
        // Test nhánh response.getBody() == null cho hàm lấy info
        ResponseEntity<Response<JsonNode>> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        assertNull(workflowClient.getWorkflowInfoByRequestId(1L, null, null, "token"));
    }
}