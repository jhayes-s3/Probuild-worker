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
 * Fires from Event-give-digital-handover (Probuild throw event). Publishes the
 * "Message-receive-digital-handover-checklist" message which has a message
 * start event subscription on the Fixpro process, spawning a Fixpro instance
 * to service the damaged tools.
 */
@Component
public class GiveDigitalHandoverWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(GiveDigitalHandoverWorker.class);

    @Autowired
    private MessagePublisher messagePublisher;

    @JobWorker(type = "job-give-digital-handover")
    public void giveDigitalHandover(final ActivatedJob job, final JobClient client) {
        LOGGER.info("job-give-digital-handover fired, Job ID: {}", job.getKey());

        Map<String, Object> vars = job.getVariablesAsMap();
        Object purchaseOrderId = vars.get("purchaseOrderId");
        if (purchaseOrderId == null) {
            throw new IllegalStateException(
                    "purchaseOrderId not in variables; cannot trigger Fixpro service instance");
        }

        LOGGER.info("Triggering Fixpro service instance for PO={}", purchaseOrderId);
        Map<String, Object> fixproVars = new java.util.HashMap<>();
        fixproVars.put("purchaseOrderId", purchaseOrderId);
        fixproVars.put("serviceClassification", "repair");
        fixproVars.put("lessThan50", true);
        fixproVars.put("authorisationApproved", true);
        messagePublisher.publish(
                "Message-receive-digital-handover-checklist",
                purchaseOrderId.toString(),
                fixproVars);

        client.newCompleteCommand(job.getKey())
                .send()
                .join();
    }
}
