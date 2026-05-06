package com.probuild.worker;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class NotifyCustomerToolUnavailableWorker {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(NotifyCustomerToolUnavailableWorker.class);

    @JobWorker(type = "notifyCustomerToolUnavailable")
    public void notifyCustomerToolUnavailable(final ActivatedJob job, final JobClient client) {

        LOGGER.info("Notify customer tool unavailable worker started. Job ID: {}", job.getKey());
        LOGGER.info("All variables: {}", job.getVariablesAsMap());

        String selectedToolName = (String) job.getVariable("selectedToolName");
        String requestedToolName = (String) job.getVariable("toolName");

        String toolNameToUse;

        if (selectedToolName != null && !selectedToolName.equalsIgnoreCase("none")) {
            toolNameToUse = selectedToolName;
        } else if (requestedToolName != null && !requestedToolName.isBlank()) {
            toolNameToUse = requestedToolName;
        } else {
            toolNameToUse = "the requested tool";
        }

        String message = "Sorry, " + toolNameToUse
                + " is currently unavailable. Please choose a different tool or try again later.";

        Map<String, Object> result = new HashMap<>();
        result.put("customerNotified", true);
        result.put("notificationType", "toolUnavailable");
        result.put("notificationMessage", message);

        LOGGER.info("Customer notification created: {}", message);

        client.newCompleteCommand(job.getKey())
                .variables(result)
                .send()
                .join();
    }
}