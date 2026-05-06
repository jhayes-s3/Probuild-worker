package com.probuild;

import com.probuild.worker.MessagePublisher;
import com.probuild.worker.SendItemsToBeSentWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SendItemsToBeSentWorkerTest {

    @Test
    void sendItemsToBeSentPublishesMessagesAndCompletesJob() {
        ActivatedJob job = mock(ActivatedJob.class);
        JobClient client = mock(JobClient.class, RETURNS_DEEP_STUBS);
        MessagePublisher messagePublisher = mock(MessagePublisher.class);

        when(job.getKey()).thenReturn(123L);
        when(job.getVariablesAsMap()).thenReturn(Map.of("purchaseOrderId", "PO-123"));

        SendItemsToBeSentWorker worker = new SendItemsToBeSentWorker();
        ReflectionTestUtils.setField(worker, "messagePublisher", messagePublisher);

        worker.sendItemsToBeSent(job, client);

        verify(messagePublisher).publish("Message-recieve-items-to-be-sent", "items-PO-123");
        verify(messagePublisher).publish(
                eq("Message-supplier-receives-po"),
                eq("PO-123"),
                eq(Map.of("purchaseOrderId", "PO-123"))
        );

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(client.newCompleteCommand(123L)).variables(captor.capture());

        assertEquals(true, captor.getValue().get("itemsDispatched"));
        assertEquals("items-PO-123", captor.getValue().get("itemsToBeSent"));
    }
}