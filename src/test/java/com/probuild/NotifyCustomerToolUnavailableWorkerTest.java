package com.probuild;

import com.probuild.worker.NotifyCustomerToolUnavailableWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class NotifyCustomerToolUnavailableWorkerTest {

    @Test
    void notifyCustomerToolUnavailableCompletesJobWithNotificationMessage() {
        ActivatedJob job = mock(ActivatedJob.class);
        JobClient client = mock(JobClient.class, RETURNS_DEEP_STUBS);

        when(job.getKey()).thenReturn(123L);
        when(job.getVariablesAsMap()).thenReturn(Map.of("toolName", "Drill"));
        when(job.getVariable("selectedToolName")).thenReturn("none");
        when(job.getVariable("toolName")).thenReturn("Drill");

        NotifyCustomerToolUnavailableWorker worker = new NotifyCustomerToolUnavailableWorker();

        worker.notifyCustomerToolUnavailable(job, client);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);

        verify(client.newCompleteCommand(123L)).variables(captor.capture());

        assertEquals(true, captor.getValue().get("customerNotified"));
        assertEquals("toolUnavailable", captor.getValue().get("notificationType"));
        assertEquals(
                "Sorry, Drill is currently unavailable. Please choose a different tool or try again later.",
                captor.getValue().get("notificationMessage")
        );
    }
}