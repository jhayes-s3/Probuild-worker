package com.probuild;

import com.probuild.worker.SendInvoiceToProbuildWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SendInvoiceToProbuildWorkerTest {

    @Test
    void sendInvoiceToProbuildPostsInvoiceAndCompletesJob() {
        ActivatedJob job = mock(ActivatedJob.class);
        JobClient client = mock(JobClient.class, RETURNS_DEEP_STUBS);
        RestTemplate restTemplate = mock(RestTemplate.class);

        when(job.getKey()).thenReturn(123L);
        when(job.getVariablesAsMap()).thenReturn(Map.of("purchaseOrderId", "PO-123"));

        when(restTemplate.postForObject(anyString(), isNull(), eq(Map.class)))
                .thenReturn(Map.of("id", 1));

        SendInvoiceToProbuildWorker worker = new SendInvoiceToProbuildWorker();
        ReflectionTestUtils.setField(worker, "restTemplate", restTemplate);

        worker.sendInvoiceToProbuild(job, client);

        verify(restTemplate).postForObject(contains("/invoices"), isNull(), eq(Map.class));
        verify(client.newCompleteCommand(123L)).send();
    }
}