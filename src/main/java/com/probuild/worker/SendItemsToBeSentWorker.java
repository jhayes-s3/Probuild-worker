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
public class SendItemsToBeSentWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendItemsToBeSentWorker.class);

    @Autowired
    private MessagePublisher messagePublisher;

    @JobWorker(type = "SendItemsToBeSent")
    public void sendItemsToBeSent(final ActivatedJob job, final JobClient client) {
        LOGGER.info("SendItemsToBeSent fired, Job ID: {}", job.getKey());
        LOGGER.info("All variables: {}", job.getVariablesAsMap());

        Object purchaseOrderId = job.getVariablesAsMap().get("purchaseOrderId");
        String itemsToBeSent = "items-" + purchaseOrderId;
        LOGGER.info("Notifying warehouse to dispatch items for PO {} (itemsToBeSent={})", purchaseOrderId, itemsToBeSent);

        messagePublisher.publish("Message-recieve-items-to-be-sent", itemsToBeSent);

        Map<String, Object> result = new HashMap<>();
        result.put("itemsDispatched", true);
        result.put("itemsToBeSent", itemsToBeSent);

        client.newCompleteCommand(job.getKey())
                .variables(result)
                .send()
                .join();
    }
}
