package com.probuild;

import com.probuild.worker.CheckToolRentalDetailsWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class CheckToolRentalDetailsWorkerTest {

    @Test
    void checkToolRentalDetailsCompletesJobWithCalculatedRentalDetails() {
        ActivatedJob job = mock(ActivatedJob.class);
        JobClient client = mock(JobClient.class, RETURNS_DEEP_STUBS);

        when(job.getKey()).thenReturn(123L);
        when(job.getVariablesAsMap()).thenReturn(Map.of(
                "selectedToolName", "Drill",
                "selectedToolId", 5,
                "quantityBooked", 2,
                "customerName", "James",
                "customerID", "CUST-123",
                "contactEmail", "james@example.com",
                "contactPhone", "07123456789"
        ));

        when(job.getVariable("selectedToolName")).thenReturn("Drill");
        when(job.getVariable("selectedToolId")).thenReturn(5);
        when(job.getVariable("quantityBooked")).thenReturn(2);
        when(job.getVariable("customerName")).thenReturn("James");
        when(job.getVariable("customerID")).thenReturn("CUST-123");
        when(job.getVariable("contactEmail")).thenReturn("james@example.com");
        when(job.getVariable("contactPhone")).thenReturn("07123456789");

        CheckToolRentalDetailsWorker worker = new CheckToolRentalDetailsWorker();

        worker.checkToolRentalDetails(job, client);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);

        verify(client.newCompleteCommand(123L)).variables(captor.capture());

        Map<String, Object> result = captor.getValue();

        assertEquals("Drill", result.get("toolName"));
        assertEquals("5", result.get("toolID"));
        assertEquals(2, result.get("quantityBooked"));
        assertEquals("pendingPayment", result.get("bookingStatus"));
        assertEquals(15.00, result.get("rentalRate"));
        assertEquals(50.00, result.get("depositRequired"));
        assertEquals(12.00, result.get("taxes"));
        assertEquals(122.00, result.get("totalCost"));
        assertEquals(122.00, result.get("balanceDue"));
        assertTrue(result.get("bookingReferenceNumber").toString().startsWith("PB-"));
    }
}