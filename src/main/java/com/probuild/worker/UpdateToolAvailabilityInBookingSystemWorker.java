package com.probuild.worker;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Fires from update-tool-availability-in-booking-system service task. Final
 * step in the post-service flow: marks the just-returned tools as available
 * for hire again in the booking system. Stubbed for the demo (real impl would
 * call the Booking entity / API).
 */
@Component
public class UpdateToolAvailabilityInBookingSystemWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateToolAvailabilityInBookingSystemWorker.class);

    @JobWorker(type = "job-update-tool-availability-in-booking-system")
    public void updateToolAvailabilityInBookingSystem(final ActivatedJob job, final JobClient client) {
        LOGGER.info("job-update-tool-availability-in-booking-system fired, Job ID: {}", job.getKey());
        LOGGER.info("Marking tools as available-for-hire in booking system for PO={}",
                job.getVariablesAsMap().get("purchaseOrderId"));

        Map<String, Object> result = new HashMap<>();
        result.put("toolAvailabilityUpdated", true);

        client.newCompleteCommand(job.getKey())
                .variables(result)
                .send()
                .join();
    }
}
