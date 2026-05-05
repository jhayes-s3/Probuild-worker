package com.probuild.worker;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Fires from Event-give-digital-handover (throw event, terminal end of the
 * damaged-items service-bay branch). The cross-pool message flow points at the
 * Fixpro pool, which is non-executable, so no real publish is needed -- the
 * worker just completes so the token retires and the branch ends.
 */
@Component
public class GiveDigitalHandoverWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(GiveDigitalHandoverWorker.class);

    @JobWorker(type = "job-give-digital-handover")
    public void giveDigitalHandover(final ActivatedJob job, final JobClient client) {
        LOGGER.info("job-give-digital-handover fired, Job ID: {}", job.getKey());
        LOGGER.info("Digital handover delivered to Fixpro for PO={}",
                job.getVariablesAsMap().get("purchaseOrderId"));

        client.newCompleteCommand(job.getKey())
                .send()
                .join();
    }
}
