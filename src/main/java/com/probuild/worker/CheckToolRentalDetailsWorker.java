package com.probuild.worker;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
public class CheckToolRentalDetailsWorker {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CheckToolRentalDetailsWorker.class);

    @JobWorker(type = "checkToolRentalDetails")
    public void checkToolRentalDetails(final ActivatedJob job, final JobClient client) {

        LOGGER.info("Checking tool rental details. Job ID: {}", job.getKey());
        LOGGER.info("All variables: {}", job.getVariablesAsMap());

        String toolName = getString(job, "selectedToolName");

        if (toolName == null || toolName.equalsIgnoreCase("none")) {
            toolName = getString(job, "toolName");
        }

        if (toolName == null || toolName.isBlank()) {
            toolName = "Unknown Tool";
        }

        Integer selectedToolId = getInteger(job, "selectedToolId");
        Integer quantityBooked = getInteger(job, "quantityBooked");

        if (quantityBooked == null || quantityBooked < 1) {
            quantityBooked = 1;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime rentalStart = now.plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime rentalEnd = rentalStart.plusDays(2);

        long rentalDays = Math.max(1, Duration.between(rentalStart, rentalEnd).toDays());

        double rentalRate = getRentalRateForTool(toolName);
        double depositRequired = getDepositForTool(toolName);
        double additionalFees = 0.00;

        double subtotal = rentalRate * rentalDays * quantityBooked;
        double taxes = subtotal * 0.20;
        double totalCost = subtotal + depositRequired + taxes + additionalFees;
        double amountPaid = 0.00;
        double balanceDue = totalCost - amountPaid;

        String bookingStatus = balanceDue > 0 ? "pendingPayment" : "confirmed";

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        Map<String, Object> result = new HashMap<>();

        result.put("bookingReferenceNumber", "PB-" + System.currentTimeMillis());
        result.put("confirmationDate", now.format(formatter));
        result.put("bookingStatus", bookingStatus);

        result.put("customerName", getOrDefault(job, "customerName", "Test Customer"));
        result.put("customerID", getOrDefault(job, "customerID", "CUST-001"));
        result.put("contactEmail", getOrDefault(job, "contactEmail", "customer@example.com"));
        result.put("contactPhone", getOrDefault(job, "contactPhone", "07123456789"));

        result.put("toolName", toolName);
        result.put("toolID", selectedToolId != null ? selectedToolId.toString() : "N/A");
        result.put("quantityBooked", quantityBooked);

        result.put("rentalStart", rentalStart.format(formatter));
        result.put("rentalEnd", rentalEnd.format(formatter));
        result.put("totalRentalDuration", rentalDays + " days");

        result.put("deliveryMethod", getOrDefault(job, "deliveryMethod", "pickup"));
        result.put("pickupReturnAddress", "Probuild Tool Hire, Bristol Branch");
        result.put("deliveryAddress", "");
        result.put("pickupReturnInstructions", "Bring valid ID and booking reference number.");

        result.put("rentalRate", round2(rentalRate));
        result.put("depositRequired", round2(depositRequired));
        result.put("taxes", round2(taxes));
        result.put("additionalFees", round2(additionalFees));
        result.put("totalCost", round2(totalCost));
        result.put("amountPaid", round2(amountPaid));
        result.put("balanceDue", round2(balanceDue));

        LOGGER.info("Generated rental details: {}", result);

        client.newCompleteCommand(job.getKey())
                .variables(result)
                .send()
                .join();
    }

    private double getRentalRateForTool(String toolName) {
        String name = toolName.toLowerCase();

        if (name.contains("drill")) return 15.00;
        if (name.contains("saw")) return 20.00;
        if (name.contains("hammer")) return 10.00;
        if (name.contains("ladder")) return 12.00;
        if (name.contains("mixer")) return 35.00;
        if (name.contains("excavator")) return 150.00;

        return 25.00;
    }

    private double getDepositForTool(String toolName) {
        String name = toolName.toLowerCase();

        if (name.contains("excavator")) return 500.00;
        if (name.contains("mixer")) return 100.00;
        if (name.contains("drill")) return 50.00;
        if (name.contains("saw")) return 60.00;
        if (name.contains("ladder")) return 40.00;

        return 50.00;
    }

    private String getString(ActivatedJob job, String key) {
        Object value = job.getVariable(key);
        return value != null ? value.toString() : null;
    }

    private String getOrDefault(ActivatedJob job, String key, String defaultValue) {
        String value = getString(job, key);
        return value != null && !value.isBlank() ? value : defaultValue;
    }

    private Integer getInteger(ActivatedJob job, String key) {
        Object value = job.getVariable(key);

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}