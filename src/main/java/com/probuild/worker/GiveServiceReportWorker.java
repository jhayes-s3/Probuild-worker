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
 * Fires from Fixpro's Event-give-service-report throw event. Publishes the
 * service report message back to Probuild's Event-receive-service-report
 * catch, correlated by purchaseOrderId.
 */
@Component
public class GiveServiceReportWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(GiveServiceReportWorker.class);

    @Autowired
    private MessagePublisher messagePublisher;

    @JobWorker(type = "job-give-service-report")
    public void giveServiceReport(final ActivatedJob job, final JobClient client) {
        LOGGER.info("job-give-service-report fired, Job ID: {}", job.getKey());
        Map<String, Object> vars = job.getVariablesAsMap();
        Object purchaseOrderId = vars.get("purchaseOrderId");
        LOGGER.info("Sending service report back to Probuild for PO={}", purchaseOrderId);

        if (purchaseOrderId != null) {
            messagePublisher.publish(
                    "Message-receive-service-report",
                    purchaseOrderId.toString());
        }

        client.newCompleteCommand(job.getKey())
                .send()
                .join();
    }
}
