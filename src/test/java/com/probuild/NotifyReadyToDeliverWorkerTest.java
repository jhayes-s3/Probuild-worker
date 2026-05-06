package com.probuild;

import com.probuild.worker.NotifyReadyToDeliverWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class NotifyReadyToDeliverWorkerTest {

    @Test
    void notifyReadyToDeliverCompletesJobWithReadyToDeliverNotifiedTrue() {
        ActivatedJob job = mock(ActivatedJob.class);
        JobClient client = mock(JobClient.class, RETURNS_DEEP_STUBS);

        when(job.getKey()).thenReturn(123L);
        when(job.getVariablesAsMap()).thenReturn(Map.of(
                "deliveryOrderId", "DEL-123",
                "deliveryLocation", "Bristol"
        ));

        NotifyReadyToDeliverWorker worker = new NotifyReadyToDeliverWorker();

        worker.notifyReadyToDeliver(job, client);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);

        verify(client.newCompleteCommand(123L)).variables(captor.capture());

        assertEquals(true, captor.getValue().get("readyToDeliverNotified"));
    }
}