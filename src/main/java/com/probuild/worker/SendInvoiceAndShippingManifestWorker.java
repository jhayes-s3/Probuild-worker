package com.probuild.worker;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Fires from the supplier process's "send invoice and shipping manifest" throw event.
 * Publishes the delivery-notice message back to Probuild's intermediate catch event,
 * correlated by purchaseOrderId.
 */
@Component
public class SendInvoiceAndShippingManifestWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendInvoiceAndShippingManifestWorker.class);

    @Autowired
    private MessagePublisher messagePublisher;

    @JobWorker(type = "job-send-invoice-and-shipping-manifest")
    public void sendInvoiceAndShippingManifest(final ActivatedJob job, final JobClient client) {
        LOGGER.info("job-send-invoice-and-shipping-manifest fired, Job ID: {}", job.getKey());

        Map<String, Object> vars = job.getVariablesAsMap();
        Object purchaseOrderId = vars.get("purchaseOrderId");
        if (purchaseOrderId == null) {
            throw new IllegalStateException(
                    "purchaseOrderId not found in supplier process variables; cannot correlate back to Probuild");
        }

        LOGGER.info("Supplier sending invoice + manifest for PO={} back to Probuild", purchaseOrderId);
        messagePublisher.publish(
                "Message-receive-delivery-notice-with-shipment-manifest",
                purchaseOrderId.toString());

        client.newCompleteCommand(job.getKey())
                .send()
                .join();
    }
}
