package com.probuild;

import com.probuild.worker.NotifyNewToolChecklistNeededWorker;
import com.probuild.worker.MessagePublisher;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.mockito.Mockito.*;

class NotifyNewToolChecklistNeededWorkerTest {

    @Test
    void notifyNewToolChecklistNeededPublishesMessageAndCompletesJob() {
        ActivatedJob job = mock(ActivatedJob.class);
        JobClient client = mock(JobClient.class, RETURNS_DEEP_STUBS);
        MessagePublisher messagePublisher = mock(MessagePublisher.class);

        when(job.getKey()).thenReturn(123L);
        when(job.getVariablesAsMap()).thenReturn(Map.of("purchaseOrderId", "PO-123"));

        NotifyNewToolChecklistNeededWorker worker = new NotifyNewToolChecklistNeededWorker();
        ReflectionTestUtils.setField(worker, "messagePublisher", messagePublisher);

        worker.notifyNewToolChecklistNeeded(job, client);

        verify(messagePublisher).publish("Message-receive-new-tool-checklist-needed", "PO-123");
        verify(client.newCompleteCommand(123L)).send();
    }
}