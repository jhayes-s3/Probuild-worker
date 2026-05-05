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
public class NotifyTeamOfNewOrderWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotifyTeamOfNewOrderWorker.class);

    @Autowired
    private MessagePublisher messagePublisher;

    @JobWorker(type = "job-notify-team-of-new-order")
    public void notifyTeamOfNewOrder(final ActivatedJob job, final JobClient client) {
        LOGGER.info("job-notify-team-of-new-order fired, Job ID: {}", job.getKey());
        LOGGER.info("All variables: {}", job.getVariablesAsMap());

        Map<String, Object> vars = job.getVariablesAsMap();
        Object purchaseOrderId = vars.get("purchaseOrderId");
        Object orderId = vars.get("orderId");
        LOGGER.info("Notifying logistics team and warehouse of new order. PO={}, orderId={}", purchaseOrderId, orderId);

        Object deliveryOrderId = vars.get("deliveryOrderId");
        Object deliveryLocation = vars.get("deliveryLocation");
        if (deliveryOrderId != null) {
            messagePublisher.publish("Message-recieve-delivery-order", deliveryOrderId.toString());
        }
        if (deliveryLocation != null) {
            messagePublisher.publish("Message-recieve-location-to-deliver", deliveryLocation.toString());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("teamNotified", true);

        client.newCompleteCommand(job.getKey())
                .variables(result)
                .send()
                .join();
    }
}
