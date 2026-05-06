package com.probuild;

import com.probuild.worker.MessagePublisher;
import com.probuild.worker.RequestStockAvailabilityWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class RequestStockAvailabilityWorkerTest {

    @Test
    void requestStockAvailabilityPublishesMessageAndCompletesJobWithRequestId() {
        ActivatedJob job = mock(ActivatedJob.class);
        JobClient client = mock(JobClient.class, RETURNS_DEEP_STUBS);
        MessagePublisher messagePublisher = mock(MessagePublisher.class);

        when(job.getKey()).thenReturn(123L);
        when(job.getVariablesAsMap()).thenReturn(Map.of("purchaseOrderId", "PO-123"));

        RequestStockAvailabilityWorker worker = new RequestStockAvailabilityWorker();
        ReflectionTestUtils.setField(worker, "messagePublisher", messagePublisher);

        worker.requestStockAvailability(job, client);

        verify(messagePublisher).publish(
                "Message-recieve-stock-availability-request",
                "stock-req-PO-123"
        );

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);

        verify(client.newCompleteCommand(123L)).variables(captor.capture());

        assertEquals("stock-req-PO-123", captor.getValue().get("stockRequestId"));
    }
}