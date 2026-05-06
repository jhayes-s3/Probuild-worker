package com.probuild.worker;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * Fires from Fixpro's send-invoice-to-probuild service task. Creates an
 * Invoice row in the Probuild API db so the run-through actually persists
 * something downstream of the Fixpro→Probuild handover.
 */
@Component
public class SendInvoiceToProbuildWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendInvoiceToProbuildWorker.class);
    private static final String API_BASE = "http://localhost:8081";

    @Autowired
    private RestTemplate restTemplate;

    @JobWorker(type = "job-send-invoice-to-probuild")
    public void sendInvoiceToProbuild(final ActivatedJob job, final JobClient client) {
        LOGGER.info("job-send-invoice-to-probuild fired, Job ID: {}", job.getKey());
        Object purchaseOrderId = job.getVariablesAsMap().get("purchaseOrderId");
        String invoiceNumber = "INV-" + purchaseOrderId;

        String url = UriComponentsBuilder.fromHttpUrl(API_BASE + "/invoices")
                .queryParam("invoiceNumber", invoiceNumber)
                .queryParam("amount", 250.00)
                .toUriString();
        try {
            Map<?, ?> created = restTemplate.postForObject(url, null, Map.class);
            LOGGER.info("Invoice persisted: {}", created);
        } catch (Exception e) {
            LOGGER.warn("Failed to persist Invoice: {}", e.getMessage());
        }

        client.newCompleteCommand(job.getKey())
                .send()
                .join();
    }
}
