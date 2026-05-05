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
public class AddPointsToCardWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddPointsToCardWorker.class);

    @JobWorker(type = "job-add-points-to-card")
    public void addPointsToCard(final ActivatedJob job, final JobClient client) {
        LOGGER.info("job-add-points-to-card fired, Job ID: {}", job.getKey());
        LOGGER.info("All variables: {}", job.getVariablesAsMap());

        Map<String, Object> vars = job.getVariablesAsMap();
        Object purchaseOrderId = vars.get("purchaseOrderId");
        Object tradeCardBeingUsed = vars.get("tradeCardBeingUsed");
        LOGGER.info("Adding points to trade card. PO={}, tradeCardBeingUsed={}",
                purchaseOrderId, tradeCardBeingUsed);

        Map<String, Object> result = new HashMap<>();
        result.put("pointsAdded", true);

        client.newCompleteCommand(job.getKey())
                .variables(result)
                .send()
                .join();
    }
}
