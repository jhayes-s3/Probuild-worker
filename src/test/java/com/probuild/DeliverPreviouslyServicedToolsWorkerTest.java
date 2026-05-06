package com.probuild;

import com.probuild.worker.DeliverPreviouslyServicedToolsWorker;
import com.probuild.worker.MessagePublisher;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.mockito.Mockito.*;

class DeliverPreviouslyServicedToolsWorkerTest {

    @Test
    void deliverPreviouslyServicedToolsPublishesMessageAndCompletesJob() {
        ActivatedJob job = mock(ActivatedJob.class);
        JobClient client = mock(JobClient.class, RETURNS_DEEP_STUBS);
        MessagePublisher messagePublisher = mock(MessagePublisher.class);

        when(job.getKey()).thenReturn(123L);
        when(job.getVariablesAsMap()).thenReturn(Map.of("purchaseOrderId", "PO-123"));

        DeliverPreviouslyServicedToolsWorker worker = new DeliverPreviouslyServicedToolsWorker();
        ReflectionTestUtils.setField(worker, "messagePublisher", messagePublisher);

        worker.deliverPreviouslyServicedTools(job, client);

        verify(messagePublisher).publish("Message-recieve-serviced-tools", "PO-123");
        verify(client.newCompleteCommand(123L)).send();
    }
}