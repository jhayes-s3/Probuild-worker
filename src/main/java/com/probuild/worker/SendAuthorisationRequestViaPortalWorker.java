package com.probuild.worker;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Fires from Fixpro's Event-send-authorisation-request-via-portal throw event.
 * In production this would post the authorisation request to ProBuild's auth
 * portal and wait async. For test setup we just complete the job; the user
 * (or an external script) then publishes Message-receive-authorisation-decision
 * with correlationKey = purchaseOrderId to advance past the receive event.
 */
@Component
public class SendAuthorisationRequestViaPortalWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendAuthorisationRequestViaPortalWorker.class);

    @JobWorker(type = "job-send-authorisation-request-via-portal")
    public void sendAuthorisationRequestViaPortal(final ActivatedJob job, final JobClient client) {
        LOGGER.info("job-send-authorisation-request-via-portal fired, Job ID: {}", job.getKey());
        Map<String, Object> vars = job.getVariablesAsMap();
        LOGGER.info("Auth request submitted to portal for PO={}; awaiting decision",
                vars.get("purchaseOrderId"));

        client.newCompleteCommand(job.getKey())
                .send()
                .join();
    }
}
