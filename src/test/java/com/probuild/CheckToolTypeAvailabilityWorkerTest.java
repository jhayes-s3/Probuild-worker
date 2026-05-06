package com.probuild;

import com.probuild.worker.CheckToolTypeAvailabilityWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CheckToolTypeAvailabilityWorkerTest {

    @Test
    void checkToolTypeAvailabilityCompletesJobWithMatchingTool() {
        ActivatedJob job = mock(ActivatedJob.class);
        JobClient client = mock(JobClient.class, RETURNS_DEEP_STUBS);
        RestTemplate restTemplate = mock(RestTemplate.class);

        when(job.getKey()).thenReturn(123L);
        when(job.getVariable("toolName")).thenReturn("Drill");
        when(job.getVariablesAsMap()).thenReturn(Map.of("toolName", "Drill"));

        when(restTemplate.getForObject(anyString(), eq(List.class)))
                .thenReturn(List.of(Map.of("id", 7, "name", "Drill")));

        CheckToolTypeAvailabilityWorker worker = new CheckToolTypeAvailabilityWorker();
        ReflectionTestUtils.setField(worker, "restTemplate", restTemplate);

        worker.checkToolTypeAvailability(job, client);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);

        verify(client.newCompleteCommand(123L)).variables(captor.capture());

        assertEquals(true, captor.getValue().get("toolAvailable"));
        assertEquals(7, captor.getValue().get("selectedToolId"));
        assertEquals("Drill", captor.getValue().get("selectedToolName"));
    }
}