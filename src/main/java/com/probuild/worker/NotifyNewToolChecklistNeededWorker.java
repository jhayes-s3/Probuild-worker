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
 * Fires from Event-notfiy-that-new-tool-checklist-needed (throw event).
 * Publishes the response message that the immediately-downstream catch event
 * (receive-new-tool-checklist-needed) waits on, correlated by purchaseOrderId.
 */
@Component
public class NotifyNewToolChecklistNeededWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotifyNewToolChecklistNeededWorker.class);

    @Autowired
    private MessagePublisher messagePublisher;

    @JobWorker(type = "job-notfiy-that-new-tool-checklist-needed")
    public void notifyNewToolChecklistNeeded(final ActivatedJob job, final JobClient client) {
        LOGGER.info("job-notfiy-that-new-tool-checklist-needed fired, Job ID: {}", job.getKey());

        Map<String, Object> vars = job.getVariablesAsMap();
        Object purchaseOrderId = vars.get("purchaseOrderId");
        if (purchaseOrderId == null) {
            throw new IllegalStateException(
                    "purchaseOrderId not in process variables; cannot correlate new-tool-checklist message");
        }

        LOGGER.info("Publishing Message-receive-new-tool-checklist-needed for PO={}", purchaseOrderId);
        messagePublisher.publish(
                "Message-receive-new-tool-checklist-needed",
                purchaseOrderId.toString());

        client.newCompleteCommand(job.getKey())
                .send()
                .join();
    }
}
