package com.probuild.worker;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ArchivePurchaseOrderWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchivePurchaseOrderWorker.class);

    @JobWorker(type = "job-archive-purchase-order")
    public void archivePurchaseOrder(final ActivatedJob job, final JobClient client) {
        LOGGER.info("job-archive-purchase-order fired, Job ID: {}", job.getKey());
        LOGGER.info("All variables: {}", job.getVariablesAsMap());

        Object purchaseOrderId = job.getVariablesAsMap().get("purchaseOrderId");
        LOGGER.info("Archiving purchase order. PO={}", purchaseOrderId);

        Map<String, Object> result = new HashMap<>();
        result.put("purchaseOrderArchived", true);

        client.newCompleteCommand(job.getKey())
                .variables(result)
                .send()
                .join();
    }
}
