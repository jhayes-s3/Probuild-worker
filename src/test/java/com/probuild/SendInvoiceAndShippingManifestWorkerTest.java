package com.probuild;

import com.probuild.worker.MessagePublisher;
import com.probuild.worker.SendInvoiceAndShippingManifestWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.mockito.Mockito.*;

class SendInvoiceAndShippingManifestWorkerTest {

    @Test
    void sendInvoiceAndShippingManifestPublishesMessageAndCompletesJob() {
        ActivatedJob job = mock(ActivatedJob.class);
        JobClient client = mock(JobClient.class, RETURNS_DEEP_STUBS);
        MessagePublisher messagePublisher = mock(MessagePublisher.class);

        when(job.getKey()).thenReturn(123L);
        when(job.getVariablesAsMap()).thenReturn(Map.of("purchaseOrderId", "PO-123"));

        SendInvoiceAndShippingManifestWorker worker =
                new SendInvoiceAndShippingManifestWorker();

        ReflectionTestUtils.setField(worker, "messagePublisher", messagePublisher);

        worker.sendInvoiceAndShippingManifest(job, client);

        verify(messagePublisher).publish(
                "Message-receive-delivery-notice-with-shipment-manifest",
                "PO-123"
        );

        verify(client.newCompleteCommand(123L)).send();
    }
}