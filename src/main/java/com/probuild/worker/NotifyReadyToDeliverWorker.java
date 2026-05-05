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
public class NotifyReadyToDeliverWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotifyReadyToDeliverWorker.class);

    @JobWorker(type = "job-notify-ready-to-deliver")
    public void notifyReadyToDeliver(final ActivatedJob job, final JobClient client) {
        LOGGER.info("job-notify-ready-to-deliver fired, Job ID: {}", job.getKey());
        LOGGER.info("All variables: {}", job.getVariablesAsMap());

        Map<String, Object> vars = job.getVariablesAsMap();
        Object deliveryOrderId = vars.get("deliveryOrderId");
        Object deliveryLocation = vars.get("deliveryLocation");
        LOGGER.info("Notifying delivery line: deliveryOrderId={}, deliveryLocation={}",
                deliveryOrderId, deliveryLocation);

        Map<String, Object> result = new HashMap<>();
        result.put("readyToDeliverNotified", true);

        client.newCompleteCommand(job.getKey())
                .variables(result)
                .send()
                .join();
    }
}
