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
public class PlaceOrderWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlaceOrderWorker.class);

    @Autowired
    private MessagePublisher messagePublisher;

    @JobWorker(type = "PlaceOrder")
    public void placeOrder(final ActivatedJob job, final JobClient client) {
        LOGGER.info("PlaceOrder fired, Job ID: {}", job.getKey());
        LOGGER.info("All variables: {}", job.getVariablesAsMap());

        Object purchaseOrderId = job.getVariablesAsMap().get("purchaseOrderId");
        String orderId = "order-" + purchaseOrderId;
        LOGGER.info("Placing order with supplier (black box). orderId={}", orderId);

        messagePublisher.publish("Message-order-confirmation", orderId);

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);

        client.newCompleteCommand(job.getKey())
                .variables(result)
                .send()
                .join();
    }
}
