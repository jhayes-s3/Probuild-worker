package com.probuild;

import com.probuild.worker.MessagePublisher;
import com.probuild.worker.NotifyToolsAreReadyToProcessWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.mockito.Mockito.*;

class NotifyToolsAreReadyToProcessWorkerTest {

    @Test
    void notifyToolsAreReadyToProcessPublishesMessageAndCompletesJob() {
        ActivatedJob job = mock(ActivatedJob.class);
        JobClient client = mock(JobClient.class, RETURNS_DEEP_STUBS);
        MessagePublisher messagePublisher = mock(MessagePublisher.class);

        when(job.getKey()).thenReturn(123L);
        when(job.getVariablesAsMap()).thenReturn(Map.of("purchaseOrderId", "PO-123"));

        NotifyToolsAreReadyToProcessWorker worker = new NotifyToolsAreReadyToProcessWorker();
        ReflectionTestUtils.setField(worker, "messagePublisher", messagePublisher);

        worker.notifyToolsAreReadyToProcess(job, client);

        verify(messagePublisher).publish("Message-recieve-notification", "PO-123");
        verify(client.newCompleteCommand(123L)).send();
    }
}