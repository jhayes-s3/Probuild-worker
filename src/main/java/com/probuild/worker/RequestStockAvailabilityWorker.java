package com.probuild.worker;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class RequestStockAvailabilityWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestStockAvailabilityWorker.class);

    @Autowired
    private MessagePublisher messagePublisher;

    @JobWorker(type = "job-request-stock-availability")
    public void requestStockAvailability(final ActivatedJob job, final JobClient client) {
        LOGGER.info("job-request-stock-availability fired, Job ID: {}", job.getKey());
        LOGGER.info("All variables: {}", job.getVariablesAsMap());

        Object purchaseOrderId = job.getVariablesAsMap().get("purchaseOrderId");
        String stockRequestId = "stock-req-" + purchaseOrderId;
        LOGGER.info("Sending stock request to IMS with stockRequestId: {}", stockRequestId);

        messagePublisher.publish("Message-recieve-stock-availability-request", stockRequestId);

        Map<String, Object> result = new HashMap<>();
        result.put("stockRequestId", stockRequestId);

        client.newCompleteCommand(job.getKey())
                .variables(result)
                .send()
                .join();
    }
}
