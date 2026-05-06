package com.probuild.worker;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Fires from Fixpro's Event-deliver-previously-serviced-tools throw event.
 * Publishes a notification message back to ProBuild signalling that the
 * serviced tools have been delivered (catch event Event-recieve-serviced-tools
 * on Probuild side, message name Message-recieve-notification).
 */
@Component
public class DeliverPreviouslyServicedToolsWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeliverPreviouslyServicedToolsWorker.class);

    @Autowired
    private MessagePublisher messagePublisher;

    @JobWorker(type = "job-deliver-previously-serviced-tools")
    public void deliverPreviouslyServicedTools(final ActivatedJob job, final JobClient client) {
        LOGGER.info("job-deliver-previously-serviced-tools fired, Job ID: {}", job.getKey());
        Map<String, Object> vars = job.getVariablesAsMap();
        Object purchaseOrderId = vars.get("purchaseOrderId");
        LOGGER.info("Delivering serviced tools back to Probuild for PO={}", purchaseOrderId);

        if (purchaseOrderId != null) {
            messagePublisher.publish(
                    "Message-recieve-serviced-tools",
                    purchaseOrderId.toString());
        }

        client.newCompleteCommand(job.getKey())
                .send()
                .join();
    }
}
