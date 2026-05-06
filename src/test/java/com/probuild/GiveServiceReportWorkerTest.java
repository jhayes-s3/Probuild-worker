package com.probuild;

import com.probuild.worker.GiveServiceReportWorker;
import com.probuild.worker.MessagePublisher;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.mockito.Mockito.*;

class GiveServiceReportWorkerTest {

    @Test
    void giveServiceReportPublishesMessageAndCompletesJob() {
        ActivatedJob job = mock(ActivatedJob.class);
        JobClient client = mock(JobClient.class, RETURNS_DEEP_STUBS);
        MessagePublisher messagePublisher = mock(MessagePublisher.class);

        when(job.getKey()).thenReturn(123L);
        when(job.getVariablesAsMap()).thenReturn(Map.of("purchaseOrderId", "PO-123"));

        GiveServiceReportWorker worker = new GiveServiceReportWorker();
        ReflectionTestUtils.setField(worker, "messagePublisher", messagePublisher);

        worker.giveServiceReport(job, client);

        verify(messagePublisher).publish("Message-receive-service-report", "PO-123");
        verify(client.newCompleteCommand(123L)).send();
    }
}