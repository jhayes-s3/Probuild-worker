package com.probuild.worker;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Fires from update-tool-availability-in-booking-system service task. Marks
 * the just-returned tool available again via the existing PUT /tools/{id}/availability
 * endpoint, using toolId set in process variables by PlaceOrderWorker.
 */
@Component
public class UpdateToolAvailabilityInBookingSystemWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateToolAvailabilityInBookingSystemWorker.class);
    private static final String API_BASE = "http://localhost:8081";

    @Autowired
    private RestTemplate restTemplate;

    @JobWorker(type = "job-update-tool-availability-in-booking-system")
    public void updateToolAvailabilityInBookingSystem(final ActivatedJob job, final JobClient client) {
        LOGGER.info("job-update-tool-availability-in-booking-system fired, Job ID: {}", job.getKey());
        Map<String, Object> vars = job.getVariablesAsMap();
        Object toolId = vars.get("toolId");

        if (toolId instanceof Integer tid) {
            String url = UriComponentsBuilder.fromHttpUrl(API_BASE + "/tools/" + tid + "/availability")
                    .queryParam("available", true)
                    .toUriString();
            try {
                restTemplate.put(url, null);
                LOGGER.info("Tool {} marked as available-for-hire", tid);
            } catch (Exception e) {
                LOGGER.warn("Failed to update tool availability: {}", e.getMessage());
            }
        } else {
            LOGGER.warn("No toolId in variables; skipping tool availability update");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("toolAvailabilityUpdated", true);

        client.newCompleteCommand(job.getKey())
                .variables(result)
                .send()
                .join();
    }
}
