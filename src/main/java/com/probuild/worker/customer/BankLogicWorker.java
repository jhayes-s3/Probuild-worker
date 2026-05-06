package com.probuild.worker.payment;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class BankLogicWorker {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(BankLogicWorker.class);

    @JobWorker(type = "job-bank-logic")
    public void bankLogic(final ActivatedJob job, final JobClient client) {

        LOGGER.info("job-bank-logic fired, Job ID: {}", job.getKey());
        LOGGER.info("All variables: {}", job.getVariablesAsMap());

        Map<String, Object> vars = job.getVariablesAsMap();

        String paymentReference = getString(vars, "paymentReference");
        double finalPrice = getDoubleOrDefault(vars, "finalPrice", 0.00);

        boolean paymentAccepted = true;

        Map<String, Object> result = new HashMap<>();
        result.put("paymentReference", paymentReference);
        result.put("paymentAccepted", paymentAccepted);
        result.put("bankTransactionId", "BANK-" + job.getKey());
        result.put("bankResponseMessage", "Payment approved");
        result.put("bankAmountChecked", finalPrice);
        result.put("bankLogicCompleted", true);

        LOGGER.info(
                "Bank logic completed. paymentReference={}, paymentAccepted={}, amount={}",
                paymentReference,
                paymentAccepted,
                finalPrice
        );

        client.newCompleteCommand(job.getKey())
                .variables(result)
                .send()
                .join();
    }

    private String getString(Map<String, Object> variables, String key) {
        Object value = variables.get(key);

        if (value == null) {
            return "";
        }

        return value.toString().trim();
    }

    private double getDoubleOrDefault(
            Map<String, Object> variables,
            String key,
            double defaultValue
    ) {
        Object value = variables.get(key);

        if (value == null) {
            return defaultValue;
        }

        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}