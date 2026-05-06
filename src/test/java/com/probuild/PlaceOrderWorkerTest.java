package com.probuild;

import com.probuild.worker.MessagePublisher;
import com.probuild.worker.PlaceOrderWorker;
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

class PlaceOrderWorkerTest {

    @Test
    void placeOrderPersistsRecordsPublishesConfirmationAndCompletesJob() {
        ActivatedJob job = mock(ActivatedJob.class);
        JobClient client = mock(JobClient.class, RETURNS_DEEP_STUBS);
        RestTemplate restTemplate = mock(RestTemplate.class);
        MessagePublisher messagePublisher = mock(MessagePublisher.class);

        when(job.getKey()).thenReturn(123L);
        when(job.getVariablesAsMap()).thenReturn(Map.of(
                "purchaseOrderId", "PO-123",
                "customerName", "James",
                "customerEmail", "james@example.com",
                "toolName", "Drill",
                "toolCategory", "Power Tools",
                "deliveryLocation", "Bristol"
        ));

        when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenReturn(
                        Map.of("id", 10),
                        Map.of("id", 20),
                        Map.of("id", 30)
                );

        PlaceOrderWorker worker = new PlaceOrderWorker();
        ReflectionTestUtils.setField(worker, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(worker, "messagePublisher", messagePublisher);

        worker.placeOrder(job, client);

        verify(messagePublisher).publish("Message-order-confirmation", "order-PO-123");

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);

        verify(client.newCompleteCommand(123L)).variables(captor.capture());

        assertEquals("order-PO-123", captor.getValue().get("orderId"));
        assertEquals(10, captor.getValue().get("customerId"));
        assertEquals(20, captor.getValue().get("toolId"));
        assertEquals(30, captor.getValue().get("apiPurchaseOrderId"));
    }
}