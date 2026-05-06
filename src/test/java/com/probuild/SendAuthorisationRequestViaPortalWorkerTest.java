package com.probuild;

import com.probuild.worker.SendAuthorisationRequestViaPortalWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.Mockito.*;

class SendAuthorisationRequestViaPortalWorkerTest {

    @Test
    void sendAuthorisationRequestViaPortalCompletesJob() {
        ActivatedJob job = mock(ActivatedJob.class);
        JobClient client = mock(JobClient.class, RETURNS_DEEP_STUBS);

        when(job.getKey()).thenReturn(123L);
        when(job.getVariablesAsMap()).thenReturn(Map.of("purchaseOrderId", "PO-123"));

        SendAuthorisationRequestViaPortalWorker worker =
                new SendAuthorisationRequestViaPortalWorker();

        worker.sendAuthorisationRequestViaPortal(job, client);

        verify(client.newCompleteCommand(123L)).send();
    }
}