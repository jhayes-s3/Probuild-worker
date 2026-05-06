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
 * Fires from Fixpro's record-service-batch-in-fixpro-client-portal service
 * task. Creates a ServiceJob row in the Probuild API db so the service work
 * is auditable from the system of record.
 */
@Component
public class RecordServiceBatchInFixproClientPortalWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecordServiceBatchInFixproClientPortalWorker.class);
    private static final String API_BASE = "http://localhost:8081";

    @Autowired
    private RestTemplate restTemplate;

    @JobWorker(type = "job-record-service-batch-in-fixpro-client-portal")
    public void recordServiceBatch(final ActivatedJob job, final JobClient client) {
        LOGGER.info("job-record-service-batch-in-fixpro-client-portal fired, Job ID: {}", job.getKey());
        Object purchaseOrderId = job.getVariablesAsMap().get("purchaseOrderId");
        String description = "Fixpro service batch for " + purchaseOrderId;

        String url = UriComponentsBuilder.fromHttpUrl(API_BASE + "/servicejobs")
                .queryParam("description", description)
                .toUriString();
        try {
            Map<?, ?> created = restTemplate.postForObject(url, null, Map.class);
            LOGGER.info("ServiceJob persisted: {}", created);
        } catch (Exception e) {
            LOGGER.warn("Failed to persist ServiceJob: {}", e.getMessage());
        }

        client.newCompleteCommand(job.getKey())
                .send()
                .join();
    }
}
