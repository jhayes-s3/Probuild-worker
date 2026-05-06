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
 * Fires from Probuild's Event-notify-tools-are-ready-to-process throw event,
 * which sits downstream of receiving serviced tools back from Fixpro and
 * tells the warehouse staff to process them. Publishes the notification
 * message that the immediately-downstream catch event Event-recieve-notification
 * waits on.
 */
@Component
public class NotifyToolsAreReadyToProcessWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotifyToolsAreReadyToProcessWorker.class);

    @Autowired
    private MessagePublisher messagePublisher;

    @JobWorker(type = "job-notify-tools-are-ready-to-process")
    public void notifyToolsAreReadyToProcess(final ActivatedJob job, final JobClient client) {
        LOGGER.info("job-notify-tools-are-ready-to-process fired, Job ID: {}", job.getKey());
        Map<String, Object> vars = job.getVariablesAsMap();
        Object purchaseOrderId = vars.get("purchaseOrderId");
        LOGGER.info("Warehouse staff notified that tools are ready to process for PO={}", purchaseOrderId);

        if (purchaseOrderId != null) {
            messagePublisher.publish(
                    "Message-recieve-notification",
                    purchaseOrderId.toString());
        }

        client.newCompleteCommand(job.getKey())
                .send()
                .join();
    }
}
