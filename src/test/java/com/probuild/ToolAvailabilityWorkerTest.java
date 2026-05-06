package com.probuild;

import com.probuild.worker.ToolAvailabilityWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ToolAvailabilityWorkerTest {

    @Test
    void processToolSelectionCompletesJobWithFirstAvailableTool() {
        ActivatedJob job = mock(ActivatedJob.class);
        JobClient client = mock(JobClient.class, RETURNS_DEEP_STUBS);

        when(job.getKey()).thenReturn(123L);
        when(job.getVariablesAsMap()).thenReturn(Map.of());

        when(job.getVariable("availableTools")).thenReturn(Map.of(
                "body",
                List.of(Map.of(
                        "id", 10,
                        "name", "Drill"
                ))
        ));

        ToolAvailabilityWorker worker = new ToolAvailabilityWorker();

        worker.processToolSelection(job, client);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(client.newCompleteCommand(123L)).variables(captor.capture());

        assertEquals(true, captor.getValue().get("toolFound"));
        assertEquals(10, captor.getValue().get("selectedToolId"));
        assertEquals("Drill", captor.getValue().get("selectedToolName"));
    }

    @Test
    void processToolSelectionCompletesJobWithNoToolFoundWhenListEmpty() {
        ActivatedJob job = mock(ActivatedJob.class);
        JobClient client = mock(JobClient.class, RETURNS_DEEP_STUBS);

        when(job.getKey()).thenReturn(123L);
        when(job.getVariablesAsMap()).thenReturn(Map.of());

        when(job.getVariable("availableTools")).thenReturn(Map.of(
                "body",
                List.of()
        ));

        ToolAvailabilityWorker worker = new ToolAvailabilityWorker();

        worker.processToolSelection(job, client);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(client.newCompleteCommand(123L)).variables(captor.capture());

        assertEquals(false, captor.getValue().get("toolFound"));
        assertEquals(null, captor.getValue().get("selectedToolId"));
        assertEquals("none", captor.getValue().get("selectedToolName"));
    }
}