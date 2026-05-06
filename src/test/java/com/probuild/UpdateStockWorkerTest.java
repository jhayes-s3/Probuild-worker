package com.probuild;

import com.probuild.worker.UpdateStockWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UpdateStockWorkerTest {

    @Test
    void updateStockCreatesStockRecordUpdatesPurchaseOrderAndCompletesJob() {
        ActivatedJob job = mock(ActivatedJob.class);
        JobClient client = mock(JobClient.class, RETURNS_DEEP_STUBS);
        RestTemplate restTemplate = mock(RestTemplate.class);

        when(job.getKey()).thenReturn(123L);
        when(job.getVariablesAsMap()).thenReturn(Map.of(
                "purchaseOrderId", "PO-123",
                "toolId", 10,
                "quantityMoved", 4,
                "destinationLocation", "A1-B2",
                "apiPurchaseOrderId", 99,
                "supplierName", "Supplier Ltd",
                "documentReferenceNumber", "DOC-123"
        ));

        when(restTemplate.postForObject(anyString(), isNull(), eq(Map.class)))
                .thenReturn(Map.of("id", 1));

        UpdateStockWorker worker = new UpdateStockWorker();
        ReflectionTestUtils.setField(worker, "restTemplate", restTemplate);

        worker.updateStock(job, client);

        verify(restTemplate).postForObject(contains("/stock"), isNull(), eq(Map.class));
        verify(restTemplate).put(contains("/purchaseorders/99"), isNull());

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(client.newCompleteCommand(123L)).variables(captor.capture());

        assertEquals(true, captor.getValue().get("stockUpdated"));
    }

    @Test
    void updateStockCompletesJobEvenWhenToolIdMissing() {
        ActivatedJob job = mock(ActivatedJob.class);
        JobClient client = mock(JobClient.class, RETURNS_DEEP_STUBS);
        RestTemplate restTemplate = mock(RestTemplate.class);

        when(job.getKey()).thenReturn(123L);
        when(job.getVariablesAsMap()).thenReturn(Map.of(
                "purchaseOrderId", "PO-123"
        ));

        UpdateStockWorker worker = new UpdateStockWorker();
        ReflectionTestUtils.setField(worker, "restTemplate", restTemplate);

        worker.updateStock(job, client);

        verify(restTemplate, never()).postForObject(contains("/stock"), isNull(), eq(Map.class));

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(client.newCompleteCommand(123L)).variables(captor.capture());

        assertEquals(true, captor.getValue().get("stockUpdated"));
    }
}