package com.probuild.worker;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ToolAvailabilityWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToolAvailabilityWorker.class);

    @JobWorker(type = "processToolSelection")
    public void processToolSelection(final ActivatedJob job, final JobClient client) {
        LOGGER.info("Processing tool selection, Job ID: {}", job.getKey());
        LOGGER.info("All variables: {}", job.getVariablesAsMap());

        // availableTools is the full response object, body is the list inside it
        Map<String, Object> availableTools = (Map<String, Object>) job.getVariable("availableTools");
        List<Map<String, Object>> tools = (List<Map<String, Object>>) availableTools.get("body");

        Map<String, Object> result = new HashMap<>();

        if (tools == null || tools.isEmpty()) {
            result.put("toolFound", false);
            result.put("selectedToolId", null);
            result.put("selectedToolName", "none");
            LOGGER.warn("No available tools found");
        } else {
            Map<String, Object> selectedTool = tools.get(0);
            Integer toolId = (Integer) selectedTool.get("id");
            String toolName = (String) selectedTool.get("name");

            result.put("toolFound", true);
            result.put("selectedToolId", toolId);
            result.put("selectedToolName", toolName);
            LOGGER.info("Selected tool: {} (ID: {})", toolName, toolId);
            LOGGER.info("selectedToolId value and type: {} - {}", toolId, toolId.getClass().getName());
        }

        client.newCompleteCommand(job.getKey())
                .variables(result)
                .send()
                .join();
    }
}