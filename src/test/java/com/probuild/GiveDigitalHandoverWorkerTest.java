package com.probuild;

import com.probuild.worker.GiveDigitalHandoverWorker;
import com.probuild.worker.MessagePublisher;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class GiveDigitalHandoverWorkerTest {

    @Test
    void giveDigitalHandoverPublishesFixproMessageAndCompletesJob() {
        ActivatedJob job = mock(ActivatedJob.class);
        JobClient client = mock(JobClient.class, RETURNS_DEEP_STUBS);
        MessagePublisher messagePublisher = mock(MessagePublisher.class);

        when(job.getKey()).thenReturn(123L);
        when(job.getVariablesAsMap()).thenReturn(Map.of("purchaseOrderId", "PO-123"));

        GiveDigitalHandoverWorker worker = new GiveDigitalHandoverWorker();
        ReflectionTestUtils.setField(worker, "messagePublisher", messagePublisher);

        worker.giveDigitalHandover(job, client);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);

        verify(messagePublisher).publish(
                eq("Message-receive-digital-handover-checklist"),
                eq("PO-123"),
                captor.capture()
        );

        assertEquals("PO-123", captor.getValue().get("purchaseOrderId"));
        assertEquals("repair", captor.getValue().get("serviceClassification"));
        assertEquals(true, captor.getValue().get("lessThan50"));
        assertEquals(true, captor.getValue().get("authorisationApproved"));

        verify(client.newCompleteCommand(123L)).send();
    }
}