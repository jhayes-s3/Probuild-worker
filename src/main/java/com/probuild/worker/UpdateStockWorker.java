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
public class UpdateStockWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateStockWorker.class);

    @JobWorker(type = "job-update-stock")
    public void updateStock(final ActivatedJob job, final JobClient client) {
        LOGGER.info("job-update-stock fired, Job ID: {}", job.getKey());
        LOGGER.info("All variables: {}", job.getVariablesAsMap());

        Object purchaseOrderId = job.getVariablesAsMap().get("purchaseOrderId");
        Object inStock = job.getVariablesAsMap().get("inStock");
        LOGGER.info("Updating stock for PO {} (was inStock={}). " +
                "Stub: would decrement stock counts here once /stock has a PUT/PATCH endpoint.",
                purchaseOrderId, inStock);

        Map<String, Object> result = new HashMap<>();
        result.put("stockUpdated", true);

        client.newCompleteCommand(job.getKey())
                .variables(result)
                .send()
                .join();
    }
}
