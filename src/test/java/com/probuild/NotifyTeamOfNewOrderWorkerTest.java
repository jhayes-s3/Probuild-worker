package com.probuild;

import com.probuild.worker.MessagePublisher;
import com.probuild.worker.NotifyTeamOfNewOrderWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class NotifyTeamOfNewOrderWorkerTest {

    @Test
    void notifyTeamOfNewOrderPublishesMessagesAndCompletesJob() {
        ActivatedJob job = mock(ActivatedJob.class);
        JobClient client = mock(JobClient.class, RETURNS_DEEP_STUBS);
        MessagePublisher messagePublisher = mock(MessagePublisher.class);

        when(job.getKey()).thenReturn(123L);
        when(job.getVariablesAsMap()).thenReturn(Map.of(
                "purchaseOrderId", "PO-123",
                "orderId", "ORD-123",
                "deliveryOrderId", "DEL-123",
                "deliveryLocation", "Bristol"
        ));

        NotifyTeamOfNewOrderWorker worker = new NotifyTeamOfNewOrderWorker();
        ReflectionTestUtils.setField(worker, "messagePublisher", messagePublisher);

        worker.notifyTeamOfNewOrder(job, client);

        verify(messagePublisher).publish("Message-recieve-delivery-order", "DEL-123");
        verify(messagePublisher).publish("Message-recieve-location-to-deliver", "Bristol");

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);

        verify(client.newCompleteCommand(123L)).variables(captor.capture());

        assertEquals(true, captor.getValue().get("teamNotified"));
    }
}