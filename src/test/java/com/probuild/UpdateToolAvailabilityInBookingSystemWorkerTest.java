package com.probuild;

import com.probuild.worker.UpdateToolAvailabilityInBookingSystemWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

class UpdateToolAvailabilityInBookingSystemWorkerTest {

    @Test
    void updateToolAvailabilityMarksToolAvailableAndCompletesJob() {
        ActivatedJob job = mock(ActivatedJob.class);
        JobClient client = mock(JobClient.class, RETURNS_DEEP_STUBS);
        RestTemplate restTemplate = mock(RestTemplate.class);

        when(job.getKey()).thenReturn(123L);
        when(job.getVariablesAsMap()).thenReturn(Map.of("toolId", 10));

        UpdateToolAvailabilityInBookingSystemWorker worker =
                new UpdateToolAvailabilityInBookingSystemWorker();

        ReflectionTestUtils.setField(worker, "restTemplate", restTemplate);

        worker.updateToolAvailabilityInBookingSystem(job, client);

        verify(restTemplate).put(contains("/tools/10/availability"), isNull());

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(client.newCompleteCommand(123L)).variables(captor.capture());

        assertEquals(true, captor.getValue().get("toolAvailabilityUpdated"));
    }

    @Test
    void updateToolAvailabilityCompletesJobWhenToolIdMissing() {
        ActivatedJob job = mock(ActivatedJob.class);
        JobClient client = mock(JobClient.class, RETURNS_DEEP_STUBS);
        RestTemplate restTemplate = mock(RestTemplate.class);

        when(job.getKey()).thenReturn(123L);
        when(job.getVariablesAsMap()).thenReturn(Map.of());

        UpdateToolAvailabilityInBookingSystemWorker worker =
                new UpdateToolAvailabilityInBookingSystemWorker();

        ReflectionTestUtils.setField(worker, "restTemplate", restTemplate);

        worker.updateToolAvailabilityInBookingSystem(job, client);

        verify(restTemplate, never()).put(anyString(), any());

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(client.newCompleteCommand(123L)).variables(captor.capture());

        assertEquals(true, captor.getValue().get("toolAvailabilityUpdated"));
    }
}