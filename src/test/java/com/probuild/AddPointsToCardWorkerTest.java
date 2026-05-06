package com.probuild;

import com.probuild.worker.AddPointsToCardWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AddPointsToCardWorkerTest {

    @Test
    void addPointsToCardCompletesJobWithPointsAddedTrue() {
        ActivatedJob job = mock(ActivatedJob.class);
        JobClient client = mock(JobClient.class, RETURNS_DEEP_STUBS);

        when(job.getKey()).thenReturn(123L);
        when(job.getVariablesAsMap()).thenReturn(Map.of(
                "purchaseOrderId", "PO-123",
                "tradeCardBeingUsed", true
        ));

        AddPointsToCardWorker worker = new AddPointsToCardWorker();

        worker.addPointsToCard(job, client);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);

        verify(client.newCompleteCommand(123L)).variables(captor.capture());

        assertEquals(true, captor.getValue().get("pointsAdded"));
    }
}