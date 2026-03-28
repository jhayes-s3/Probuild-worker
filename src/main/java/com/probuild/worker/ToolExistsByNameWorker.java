package com.probuild.worker;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

@Component
public class ToolExistsByNameWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToolAvailabilityWorker.class);

    @Autowired
    private RestTemplate restTemplate;

    // Your API endpoint
    private static final String API_URL = "http://localhost:8081/tools";

    @JobWorker(type = "checkToolTypeAvailability")
    public void checkToolTypeAvailability(final ActivatedJob job, final JobClient client) {

//        LOGGER.info("Processing tool selection, Job ID: {}", job.getKey());
        LOGGER.info("All variables: {}", job.getVariablesAsMap());

        String requestedToolName = (String) job.getVariable("toolName");
        LOGGER.info("Requested tool: {}", requestedToolName);

        List<Map<String, Object>> tools =
                restTemplate.getForObject(API_URL, List.class);

        Map<String, Object> result = new HashMap<>();

        boolean found = false;
        Integer foundId = null;
        String foundName = null;

        if (tools != null) {
            for (Map<String, Object> tool : tools) {

                String name = (String) tool.get("name");

                if (name != null && name.equalsIgnoreCase(requestedToolName)) {

                    found = true;

                    // Safe ID conversion (avoids Integer/Double issues)
                    Number idNumber = (Number) tool.get("id");
                    foundId = idNumber != null ? idNumber.intValue() : null;

                    foundName = name;

                    LOGGER.info("Match found: {} (ID: {})", foundName, foundId);
                    break;
                }
            }
        }

        if (!found) {
            LOGGER.warn("No matching tool found for name: {}", requestedToolName);
        }

        result.put("toolFound", found);
//        result.put("selectedToolId", foundId);
//        result.put("selectedToolName", foundName != null ? foundName : "none");

        client.newCompleteCommand(job.getKey())
                .variables(result)
                .send()
                .join();
    }
}